from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.ext.asyncio import AsyncSession
from app.database import get_db
from app.core.security import get_current_user_payload
from app.schemas.billing import QuotaResponse
from app.services.quota_service import get_shop_quota, claim_emergency_extension

router = APIRouter()

@router.get("/quota", response_model=QuotaResponse, summary="Get current shop monthly billing quota status")
async def get_quota(
    current_user: dict = Depends(get_current_user_payload),
    db: AsyncSession = Depends(get_db)
):
    shop_id = current_user.get("shop_id")
    if not shop_id:
        raise HTTPException(status_code=400, detail="User is not associated with a Shop")
    return await get_shop_quota(db, shop_id)

@router.post("/claim-emergency-extension", response_model=QuotaResponse, summary="Claim emergency quota extension (+10 bills)")
async def claim_extension(
    current_user: dict = Depends(get_current_user_payload),
    db: AsyncSession = Depends(get_db)
):
    shop_id = current_user.get("shop_id")
    if not shop_id:
        raise HTTPException(status_code=400, detail="User is not associated with a Shop")
    return await claim_emergency_extension(db, shop_id)
