import secrets
from app.config import settings

RATE_WINDOW_SECONDS = 300
RATE_MAX_SENDS = 3
MAX_FAILS = 5


class OtpRateLimited(Exception):
    pass


class OtpService:
    def __init__(self, redis, static_code: str | None = None):
        self.redis = redis
        self.static_code = static_code

    async def issue(self, phone: str) -> str:
        rl = f"otp_rl:{phone}"
        async with self.redis.pipeline(transaction=True) as pipe:
            pipe.incr(rl)
            pipe.expire(rl, RATE_WINDOW_SECONDS, nx=True)
            count, _ = await pipe.execute()
        if count > RATE_MAX_SENDS:
            raise OtpRateLimited()
        code = self.static_code or f"{secrets.randbelow(10**6):06d}"
        key = f"otp:{phone}"
        await self.redis.hset(key, mapping={"code": code, "fails": 0})
        await self.redis.expire(key, settings.OTP_EXPIRE_SECONDS)
        return code

    async def verify(self, phone: str, otp: str) -> bool:
        # Count the attempt first (atomic), then compare, so concurrent
        # guesses cannot out-run the lockout.
        key = f"otp:{phone}"
        async with self.redis.pipeline(transaction=True) as pipe:
            pipe.hincrby(key, "fails", 1)
            pipe.hget(key, "code")
            fails, saved = await pipe.execute()
        if saved is None:
            # key was missing: HINCRBY recreated a TTL-less stub; remove it
            await self.redis.delete(key)
            return False
        if fails > MAX_FAILS:
            await self.redis.delete(key)
            return False
        if secrets.compare_digest(saved.encode(), otp.strip().encode()):
            # single use: only the caller whose DEL removed the key wins
            return await self.redis.delete(key) == 1
        if fails >= MAX_FAILS:
            await self.redis.delete(key)
        return False
