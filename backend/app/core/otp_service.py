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
        count = await self.redis.incr(rl)
        if count == 1:
            await self.redis.expire(rl, RATE_WINDOW_SECONDS)
        if count > RATE_MAX_SENDS:
            raise OtpRateLimited()
        code = self.static_code or f"{secrets.randbelow(10**6):06d}"
        key = f"otp:{phone}"
        await self.redis.hset(key, mapping={"code": code, "fails": 0})
        await self.redis.expire(key, settings.OTP_EXPIRE_SECONDS)
        return code

    async def verify(self, phone: str, otp: str) -> bool:
        key = f"otp:{phone}"
        saved = await self.redis.hget(key, "code")
        if saved is None:
            return False
        if secrets.compare_digest(saved.encode(), otp.strip().encode()):
            await self.redis.delete(key)
            return True
        if await self.redis.hincrby(key, "fails", 1) >= MAX_FAILS:
            await self.redis.delete(key)
        return False
