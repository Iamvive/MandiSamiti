import re
import time
import uuid
from fastapi import APIRouter, Depends, HTTPException, Response, status
from sqlalchemy import select, update
from sqlalchemy.exc import IntegrityError
from sqlalchemy.ext.asyncio import AsyncSession
from app.database import get_db
from app.models.user import User, ShopProfile
from app.models.refresh_token import RefreshToken
from app.schemas.auth import (
    SendOtpRequest, SendOtpResponse, VerifyOtpRequest, VerifyOtpResponse,
    SignupRequest, LoginRequest, RefreshRequest, LogoutRequest,
    AuthSession, ShopOut, RefreshResponse,
)
from app.core.security import (
    create_access_token, create_refresh_token, create_pass_token,
    decode_token, get_password_hash, verify_password,
)
from app.core.otp_service import OtpService, OtpRateLimited
from app.core.otp_sender import OtpSender, get_otp_sender
from app.core.redis_client import get_redis
from app.config import settings

router = APIRouter()

PHONE_RE = re.compile(r"^[6-9]\d{9}$")
MAX_MPIN_FAILS = 5
MAX_MPIN_FAILS_PER_PHONE = 10
PHONE_LOCK_S = 86400
OTP_COOLDOWN_S = 30


def _unauthorized(detail="INVALID_PASS"):
    return HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail=detail)


def _clean_phone(raw: str) -> str:
    phone = raw.strip().replace(" ", "")
    if phone.startswith("+91"):
        phone = phone[3:]
    if not PHONE_RE.match(phone):
        raise HTTPException(status.HTTP_400_BAD_REQUEST, detail="INVALID_PHONE")
    return phone


def _otp_service(redis) -> OtpService:
    return OtpService(redis, settings.OTP_STATIC_CODE if settings.OTP_STATIC_ENABLED else None)


def _decode_pass(token: str, expected_type: str) -> dict:
    try:
        payload = decode_token(token)
    except HTTPException:
        raise _unauthorized()
    if payload.get("type") != expected_type or not payload.get("jti") or not payload.get("phone"):
        raise _unauthorized()
    return payload


async def _consume_pass(redis, jti: str) -> bool:
    """Atomic single-use: True only for the caller that sets the key."""
    ttl = settings.AUTH_PASS_EXPIRE_MINUTES * 60
    return bool(await redis.set(f"pass_used:{jti}", 1, nx=True, ex=ttl))


async def _issue_session(db: AsyncSession, user: User, shop: ShopProfile) -> AuthSession:
    refresh, jti, exp_ms = create_refresh_token(user.id)
    db.add(RefreshToken(jti=jti, user_id=user.id, expires_at=exp_ms))
    await db.commit()
    return AuthSession(
        access_token=create_access_token(subject=user.id, shop_id=user.shop_id, role=user.role),
        refresh_token=refresh,
        shop=ShopOut(
            id=shop.id, shop_name=shop.shop_name, owner_name=shop.owner_name,
            mandi_name=shop.mandi_name, phone_number=shop.phone_number,
        ),
    )


@router.post("/otp/send", response_model=SendOtpResponse)
async def send_otp(req: SendOtpRequest, redis=Depends(get_redis), sender: OtpSender = Depends(get_otp_sender)):
    phone = _clean_phone(req.phone)
    try:
        code = await _otp_service(redis).issue(phone)
    except OtpRateLimited:
        raise HTTPException(status.HTTP_429_TOO_MANY_REQUESTS, detail="OTP_RATE_LIMITED")
    await sender.send(phone, code)
    return SendOtpResponse(sent=True, cooldown_s=OTP_COOLDOWN_S)


@router.post("/otp/verify", response_model=VerifyOtpResponse)
async def verify_otp(req: VerifyOtpRequest, db: AsyncSession = Depends(get_db), redis=Depends(get_redis)):
    phone = _clean_phone(req.phone)
    if not await _otp_service(redis).verify(phone, req.otp):
        raise HTTPException(status.HTTP_400_BAD_REQUEST, detail="OTP_INVALID")
    exists = (await db.execute(select(User.id).where(User.phone_number == phone))).first()
    if exists:
        token, _ = create_pass_token("login_pass", phone)
        return VerifyOtpResponse(status="EXISTING", login_pass=token)
    token, _ = create_pass_token("signup_pass", phone)
    return VerifyOtpResponse(status="NEW", signup_pass=token)


@router.post("/signup", response_model=AuthSession)
async def signup(req: SignupRequest, db: AsyncSession = Depends(get_db), redis=Depends(get_redis)):
    payload = _decode_pass(req.signup_pass, "signup_pass")
    phone = payload["phone"]
    if not await _consume_pass(redis, payload["jti"]):
        raise _unauthorized()
    if (await db.execute(select(User.id).where(User.phone_number == phone))).first():
        raise HTTPException(status.HTTP_409_CONFLICT, detail="PHONE_EXISTS")
    shop = ShopProfile(
        id=str(uuid.uuid4()), shop_name=req.shop_name, owner_name=req.owner_name,
        mandi_name=req.mandi_name, phone_number=phone,
    )
    user = User(
        phone_number=phone, name=req.owner_name, role="OWNER", shop_id=shop.id,
        mpin_hash=get_password_hash(req.mpin),
    )
    db.add(shop)
    await db.flush()
    db.add(user)
    try:
        await db.flush()
    except IntegrityError:
        await db.rollback()
        raise HTTPException(status.HTTP_409_CONFLICT, detail="PHONE_EXISTS")
    return await _issue_session(db, user, shop)


@router.post("/login", response_model=AuthSession)
async def login(req: LoginRequest, db: AsyncSession = Depends(get_db), redis=Depends(get_redis)):
    payload = _decode_pass(req.login_pass, "login_pass")
    jti = payload["jti"]
    ttl = settings.AUTH_PASS_EXPIRE_MINUTES * 60
    if await redis.exists(f"pass_used:{jti}"):
        raise _unauthorized()

    # Count the attempt first (atomic), per pass AND per phone, then decide.
    # Per-phone counting stops brute force via fresh passes (static OTP).
    fail_key = f"mpin_fail:{jti}"
    user_fail_key = f"mpin_fail_user:{payload['phone']}"
    async with redis.pipeline(transaction=True) as pipe:
        pipe.incr(fail_key)
        pipe.expire(fail_key, ttl, nx=True)
        pipe.incr(user_fail_key)
        pipe.expire(user_fail_key, PHONE_LOCK_S, nx=True)
        attempts, _, user_attempts, _ = await pipe.execute()
    if user_attempts > MAX_MPIN_FAILS_PER_PHONE:
        raise _unauthorized({"code": "ACCOUNT_LOCKED"})
    if attempts > MAX_MPIN_FAILS:
        raise _unauthorized({"code": "PASS_BURNED"})

    user = (await db.execute(select(User).where(User.phone_number == payload["phone"]))).scalars().first()
    ok = bool(user and user.is_active and user.mpin_hash and verify_password(req.mpin, user.mpin_hash))
    if not ok:
        if attempts >= MAX_MPIN_FAILS:
            await _consume_pass(redis, jti)  # burn the pass
            raise _unauthorized({"code": "PASS_BURNED"})
        raise _unauthorized({"code": "MPIN_INVALID", "attempts_left": MAX_MPIN_FAILS - attempts})

    if not await _consume_pass(redis, jti):
        raise _unauthorized()
    await redis.delete(user_fail_key)
    shop = (await db.execute(select(ShopProfile).where(ShopProfile.id == user.shop_id))).scalars().first()
    if not shop:
        raise _unauthorized()
    return await _issue_session(db, user, shop)


@router.post("/refresh", response_model=RefreshResponse)
async def refresh(req: RefreshRequest, db: AsyncSession = Depends(get_db)):
    try:
        payload = decode_token(req.refresh_token)
    except HTTPException:
        raise _unauthorized("INVALID_REFRESH")
    if payload.get("type") != "refresh" or not payload.get("jti"):
        raise _unauthorized("INVALID_REFRESH")
    now = int(time.time() * 1000)
    # Atomic rotate: only one caller can flip revoked_at from NULL.
    res = await db.execute(
        update(RefreshToken)
        .where(RefreshToken.jti == payload["jti"], RefreshToken.revoked_at.is_(None), RefreshToken.expires_at > now)
        .values(revoked_at=now)
    )
    if res.rowcount != 1:
        await db.rollback()
        raise _unauthorized("INVALID_REFRESH")
    user = (await db.execute(select(User).where(User.id == payload["sub"]))).scalars().first()
    if not user or not user.is_active:
        await db.rollback()
        raise _unauthorized("INVALID_REFRESH")
    new_refresh, jti, exp_ms = create_refresh_token(user.id)
    db.add(RefreshToken(jti=jti, user_id=user.id, expires_at=exp_ms))
    await db.commit()
    return RefreshResponse(
        access_token=create_access_token(subject=user.id, shop_id=user.shop_id, role=user.role),
        refresh_token=new_refresh,
    )


@router.post("/logout", status_code=status.HTTP_204_NO_CONTENT)
async def logout(req: LogoutRequest, db: AsyncSession = Depends(get_db)):
    try:
        payload = decode_token(req.refresh_token)
    except HTTPException:
        return Response(status_code=status.HTTP_204_NO_CONTENT)
    if payload.get("type") == "refresh" and payload.get("jti"):
        await db.execute(
            update(RefreshToken)
            .where(RefreshToken.jti == payload["jti"], RefreshToken.revoked_at.is_(None))
            .values(revoked_at=int(time.time() * 1000))
        )
        await db.commit()
    return Response(status_code=status.HTTP_204_NO_CONTENT)
