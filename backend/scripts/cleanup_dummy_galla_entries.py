import asyncio
import time
from sqlalchemy import select
from app.database import AsyncSessionLocal
from app.models.user import ShopProfile
from app.models.transaction import CashTransaction

SHOP_ID = "97f4b927-52dc-4601-b764-cc603a7ddeeb"

async def main():
    print(f"Cleaning up all dummy cash entries for shop: {SHOP_ID}")
    now_ms = int(time.time() * 1000)

    async with AsyncSessionLocal() as session:
        shop = (await session.execute(
            select(ShopProfile).where(ShopProfile.id == SHOP_ID).with_for_update()
        )).scalars().first()
        if not shop:
            raise RuntimeError(f"Shop {SHOP_ID} not found")

        current_seq = shop.last_seq or 0

        res = await session.execute(
            select(CashTransaction).where(
                CashTransaction.shop_id == SHOP_ID,
                CashTransaction.remarks.like("[DUMMY_TEST]%"),
                CashTransaction.is_deleted == 0
            )
        )
        dummy_txs = res.scalars().all()
        print(f"Found {len(dummy_txs)} dummy transactions to mark deleted.")

        for tx in dummy_txs:
            current_seq += 1
            tx.is_deleted = 1
            tx.server_seq = current_seq
            tx.updated_at = now_ms

        shop.last_seq = current_seq
        await session.commit()
        print(f"Cleaned up {len(dummy_txs)} transactions. New last_seq: {current_seq}")

if __name__ == "__main__":
    asyncio.run(main())
