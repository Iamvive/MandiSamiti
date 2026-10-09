from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession
from app.models.user import ShopProfile

async def lock_shop(db: AsyncSession, shop_id: str) -> ShopProfile | None:
    """Row-locks the shop until commit (Postgres), so one shop's pushes take server_seq values in commit order."""
    res = await db.execute(select(ShopProfile).where(ShopProfile.id == shop_id).with_for_update())
    return res.scalars().first()

def next_seq(shop: ShopProfile) -> int:
    shop.last_seq = (shop.last_seq or 0) + 1
    return shop.last_seq
