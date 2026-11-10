import time
from datetime import datetime, timezone
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, func, and_, or_
from fastapi import HTTPException
from app.models.user import ShopProfile
from app.models.deal import Deal
from app.models.buy_entry import BuyEntry
from app.models.sell_entry import SellEntry

MAX_EMERGENCY_EXTENSIONS = 2
BILLS_PER_EXTENSION = 10

def get_current_month_range_ms() -> tuple[int, int]:
    now = datetime.now(timezone.utc)
    start_of_month = datetime(now.year, now.month, 1, 0, 0, 0, tzinfo=timezone.utc)
    if now.month == 12:
        start_of_next_month = datetime(now.year + 1, 1, 1, 0, 0, 0, tzinfo=timezone.utc)
    else:
        start_of_next_month = datetime(now.year, now.month + 1, 1, 0, 0, 0, tzinfo=timezone.utc)
    return int(start_of_month.timestamp() * 1000), int(start_of_next_month.timestamp() * 1000)

async def count_monthly_bills_for_shop(db: AsyncSession, shop_id: str) -> int:
    start_ms, end_ms = get_current_month_range_ms()

    # Buy entries
    buy_q = select(func.count(BuyEntry.id)).where(
        BuyEntry.shop_id == shop_id,
        BuyEntry.is_deleted == 0,
        BuyEntry.is_void == 0,
        or_(
            and_(BuyEntry.entry_date >= start_ms, BuyEntry.entry_date < end_ms),
            and_(BuyEntry.entry_date.is_(None), BuyEntry.created_at >= start_ms, BuyEntry.created_at < end_ms),
        )
    )
    buy_count = (await db.execute(buy_q)).scalar() or 0

    # Sell entries
    sell_q = select(func.count(SellEntry.id)).where(
        SellEntry.shop_id == shop_id,
        SellEntry.is_deleted == 0,
        SellEntry.is_void == 0,
        or_(
            and_(SellEntry.entry_date >= start_ms, SellEntry.entry_date < end_ms),
            and_(SellEntry.entry_date.is_(None), SellEntry.created_at >= start_ms, SellEntry.created_at < end_ms),
        )
    )
    sell_count = (await db.execute(sell_q)).scalar() or 0

    # Deals
    deal_q = select(func.count(Deal.id)).where(
        Deal.shop_id == shop_id,
        Deal.is_deleted == 0,
        Deal.is_void == 0,
        or_(
            and_(Deal.deal_date >= start_ms, Deal.deal_date < end_ms),
            and_(Deal.deal_date.is_(None), Deal.created_at >= start_ms, Deal.created_at < end_ms),
        )
    )
    deal_count = (await db.execute(deal_q)).scalar() or 0

    return buy_count + sell_count + deal_count

async def get_shop_quota(db: AsyncSession, shop_id: str) -> dict:
    shop = await db.get(ShopProfile, shop_id)
    if not shop:
        raise HTTPException(status_code=404, detail="Shop not found")

    bills_used = await count_monthly_bills_for_shop(db, shop_id)
    effective_limit = shop.monthly_bill_limit + shop.emergency_extension_bills
    plan_tier = shop.plan_tier or "FREE"
    can_create = (plan_tier.upper() == "PRO") or (bills_used < effective_limit)
    is_extension_available = shop.emergency_extensions_used < MAX_EMERGENCY_EXTENSIONS

    return {
        "plan_tier": plan_tier,
        "bills_used_this_month": bills_used,
        "monthly_limit": shop.monthly_bill_limit,
        "emergency_extensions_used": shop.emergency_extensions_used,
        "emergency_extensions_max": MAX_EMERGENCY_EXTENSIONS,
        "emergency_bills_added": shop.emergency_extension_bills,
        "effective_limit": effective_limit,
        "can_create_bill": can_create,
        "is_emergency_extension_available": is_extension_available,
    }

async def claim_emergency_extension(db: AsyncSession, shop_id: str) -> dict:
    shop = await db.get(ShopProfile, shop_id)
    if not shop:
        raise HTTPException(status_code=404, detail="Shop not found")

    if shop.emergency_extensions_used >= MAX_EMERGENCY_EXTENSIONS:
        raise HTTPException(status_code=400, detail="Maximum emergency extensions reached")

    shop.emergency_extensions_used += 1
    shop.emergency_extension_bills += BILLS_PER_EXTENSION
    await db.commit()
    await db.refresh(shop)

    return await get_shop_quota(db, shop_id)
