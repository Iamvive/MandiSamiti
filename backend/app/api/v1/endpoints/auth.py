import time
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select
from app.database import get_db
from app.models.user import User, ShopProfile
from app.schemas.auth import (
    SendOTPRequest, SendOTPResponse,
    VerifyOTPRequest, TokenResponse,
    SetupMPINRequest, VerifyMPINRequest,
    RefreshTokenRequest
)
from app.core.security import (
    create_access_token, create_refresh_token,
    decode_token, get_password_hash, verify_password,
    get_current_user_payload
)
from app.core.otp_service import otp_service
from app.config import settings

router = APIRouter()

@router.post("/otp/send", response_model=SendOTPResponse, summary="Send 6-digit OTP to mobile")
async def send_otp(req: SendOTPRequest):
    phone = req.phone_number.strip().replace("+91", "").replace(" ", "")
    if len(phone) != 10 or not phone.isdigit():
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Please provide a valid 10-digit Indian mobile number"
        )
    
    try:
        otp = otp_service.generate_otp(phone)
    except ValueError as e:
        raise HTTPException(
            status_code=status.HTTP_429_TOO_MANY_REQUESTS,
            detail=str(e)
        )
    mock_val = otp if settings.OTP_MOCK_MODE else None

    return SendOTPResponse(
        success=True,
        message="OTP sent successfully",
        phone_number=phone,
        expires_in_seconds=settings.OTP_EXPIRE_SECONDS,
        mock_otp=mock_val
    )


@router.post("/otp/verify", response_model=TokenResponse, summary="Verify OTP and issue JWT access tokens")
async def verify_otp(req: VerifyOTPRequest, db: AsyncSession = Depends(get_db)):
    phone = req.phone_number.strip().replace("+91", "").replace(" ", "")
    if not otp_service.verify_otp(phone, req.otp):
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Invalid or expired OTP"
        )
    
    # Check if user exists
    result = await db.execute(select(User).where(User.phone_number == phone))
    user = result.scalars().first()

    if not user:
        # Create new Shop and User
        shop = ShopProfile(
            shop_name=req.shop_name or "मेरी मंडी दुकान",
            mandi_name="मथुरा कृषि उपज मंडी"
        )
        db.add(shop)
        await db.flush()

        user = User(
            phone_number=phone,
            name=req.user_name or "व्यापारी / मुनीम",
            role="OWNER",
            shop_id=shop.id
        )
        db.add(user)
        await db.commit()
        await db.refresh(user)
        await db.refresh(shop)
        shop_name = shop.shop_name
    else:
        # Load existing shop
        shop_result = await db.execute(select(ShopProfile).where(ShopProfile.id == user.shop_id))
        shop = shop_result.scalars().first()
        shop_name = shop.shop_name if shop else None

    access_token = create_access_token(subject=user.id, shop_id=user.shop_id, role=user.role)
    refresh_token = create_refresh_token(subject=user.id)

    return TokenResponse(
        access_token=access_token,
        refresh_token=refresh_token,
        token_type="bearer",
        user_id=user.id,
        shop_id=user.shop_id,
        role=user.role,
        user_name=user.name,
        shop_name=shop_name
    )

@router.post("/mpin/setup", summary="Set up local 4-digit MPIN")
async def setup_mpin(
    req: SetupMPINRequest,
    current_user: dict = Depends(get_current_user_payload),
    db: AsyncSession = Depends(get_db)
):
    user_id = current_user.get("sub")
    result = await db.execute(select(User).where(User.id == user_id))
    user = result.scalars().first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
    
    user.mpin_hash = get_password_hash(req.mpin)
    user.updated_at = time.time()
    await db.commit()

    return {"success": True, "message": "MPIN configured successfully"}

@router.post("/mpin/verify", response_model=TokenResponse, summary="Fast login with 4-digit MPIN")
async def verify_mpin(req: VerifyMPINRequest, db: AsyncSession = Depends(get_db)):
    phone = req.phone_number.strip().replace("+91", "").replace(" ", "")
    result = await db.execute(select(User).where(User.phone_number == phone))
    user = result.scalars().first()
    if not user or not user.mpin_hash:
        raise HTTPException(status_code=400, detail="Invalid phone number or MPIN not setup")
    
    if not verify_password(req.mpin, user.mpin_hash):
        raise HTTPException(status_code=400, detail="Incorrect MPIN")
    
    shop_result = await db.execute(select(ShopProfile).where(ShopProfile.id == user.shop_id))
    shop = shop_result.scalars().first()

    access_token = create_access_token(subject=user.id, shop_id=user.shop_id, role=user.role)
    refresh_token = create_refresh_token(subject=user.id)

    return TokenResponse(
        access_token=access_token,
        refresh_token=refresh_token,
        token_type="bearer",
        user_id=user.id,
        shop_id=user.shop_id,
        role=user.role,
        user_name=user.name,
        shop_name=shop.shop_name if shop else None
    )

@router.post("/refresh", summary="Refresh expired access token")
async def refresh_token(req: RefreshTokenRequest, db: AsyncSession = Depends(get_db)):
    payload = decode_token(req.refresh_token)
    if payload.get("type") != "refresh":
        raise HTTPException(status_code=400, detail="Invalid refresh token")
    
    user_id = payload.get("sub")
    result = await db.execute(select(User).where(User.id == user_id))
    user = result.scalars().first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
    
    access_token = create_access_token(subject=user.id, shop_id=user.shop_id, role=user.role)
    return {"access_token": access_token, "token_type": "bearer"}
