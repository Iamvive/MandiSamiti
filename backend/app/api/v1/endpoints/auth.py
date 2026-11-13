import re
import time
import uuid
from fastapi import APIRouter, Depends, HTTPException, Response, status
from sqlalchemy import select, update
from sqlalchemy.exc import IntegrityError
from sqlalchemy.ext.asyncio import AsyncSession
from starlette.concurrency import run_in_threadpool
from app.database import get_db
from app.models.user import User, ShopProfile
from app.models.refresh_token import RefreshToken
from app.schemas.auth import (
    CheckPhoneRequest, CheckPhoneResponse, SignupRequest, LoginRequest,
    RefreshRequest, LogoutRequest, AuthSession, ShopOut, RefreshResponse,
)
from app.core.security import (
    create_access_token, create_refresh_token, decode_token,
    get_password_hash, verify_password,
)
from app.core.redis_client import get_redis
from app.config import settings

router = APIRouter()

PHONE_RE = re.compile(r"^[6-9]\d{9}$")
MAX_MPIN_FAILS_PER_PHONE = 10
PHONE_LOCK_S = 86400


def _unauthorized(detail="INVALID_MPIN"):
    return HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail=detail)


def _clean_phone(raw: str) -> str:
    phone = raw.strip().replace(" ", "")
    if phone.startswith("+91"):
        phone = phone[3:]
    if not PHONE_RE.match(phone):
        raise HTTPException(status.HTTP_400_BAD_REQUEST, detail="INVALID_PHONE")
    return phone


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


def _is_bcrypt(mpin_hash: str | None) -> bool:
    return bool(mpin_hash) and mpin_hash.startswith("$2")


def _legacy_reset_allowed(user: User) -> bool:
    """Only an active OWNER may re-set a legacy MPIN; staff re-onboarding comes with the roles feature."""
    return bool(user.is_active) and user.role == "OWNER"


def _account_disabled() -> HTTPException:
    return _unauthorized({"code": "ACCOUNT_DISABLED"})


async def _reset_legacy_user(db: AsyncSession, user: User, req: SignupRequest) -> AuthSession:
    """Legacy (SHA-256 / missing) hash: set a bcrypt MPIN in place and KEEP the shop_id so server data stays attached."""
    if not _legacy_reset_allowed(user):
        raise _account_disabled()
    user.mpin_hash = await run_in_threadpool(get_password_hash, req.mpin)
    user.name = req.owner_name
    shop = None
    if user.shop_id:
        shop = (await db.execute(select(ShopProfile).where(ShopProfile.id == user.shop_id))).scalars().first()
    if shop is None:
        shop = ShopProfile(id=user.shop_id or str(uuid.uuid4()), phone_number=user.phone_number, shop_name=req.shop_name)
        db.add(shop)
        user.shop_id = shop.id
    shop.shop_name = req.shop_name
    shop.owner_name = req.owner_name
    shop.mandi_name = req.mandi_name
    await db.flush()
    return await _issue_session(db, user, shop)


@router.post("/check-phone", response_model=CheckPhoneResponse)
async def check_phone(req: CheckPhoneRequest, db: AsyncSession = Depends(get_db)):
    phone = _clean_phone(req.phone)
    user = (await db.execute(select(User).where(User.phone_number == phone))).scalars().first()
    is_new = user is None or not _is_bcrypt(user.mpin_hash)
    return CheckPhoneResponse(is_new_user=is_new)


@router.post("/signup", response_model=AuthSession)
async def signup(req: SignupRequest, db: AsyncSession = Depends(get_db)):
    phone = _clean_phone(req.phone)
    existing = (await db.execute(select(User).where(User.phone_number == phone))).scalars().first()
    if existing:
        if _is_bcrypt(existing.mpin_hash):
            raise HTTPException(status.HTTP_409_CONFLICT, detail="PHONE_EXISTS")
        return await _reset_legacy_user(db, existing, req)
    shop = ShopProfile(
        id=str(uuid.uuid4()), shop_name=req.shop_name, owner_name=req.owner_name,
        mandi_name=req.mandi_name, phone_number=phone,
    )
    user = User(
        phone_number=phone, name=req.owner_name, role="OWNER", shop_id=shop.id,
        mpin_hash=await run_in_threadpool(get_password_hash, req.mpin),
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
    phone = _clean_phone(req.phone)

    # Check if account is already locked
    fail_key = f"mpin_fail:{phone}"
    current_fails = int(await redis.get(fail_key) or 0)
    if current_fails >= MAX_MPIN_FAILS_PER_PHONE:
        raise _unauthorized({"code": "ACCOUNT_LOCKED"})

    user = (await db.execute(select(User).where(User.phone_number == phone))).scalars().first()
    if not user or not user.is_active or not user.mpin_hash:
        raise _unauthorized({"code": "MPIN_INVALID", "attempts_left": max(0, MAX_MPIN_FAILS_PER_PHONE - current_fails)})

    is_valid = await run_in_threadpool(verify_password, req.mpin, user.mpin_hash)
    if not is_valid:
        user_fails = await redis.incr(fail_key)
        await redis.expire(fail_key, PHONE_LOCK_S, nx=True)
        if user_fails >= MAX_MPIN_FAILS_PER_PHONE:
            raise _unauthorized({"code": "ACCOUNT_LOCKED"})
        raise _unauthorized({"code": "MPIN_INVALID", "attempts_left": max(0, MAX_MPIN_FAILS_PER_PHONE - user_fails)})

    await redis.delete(fail_key)
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
