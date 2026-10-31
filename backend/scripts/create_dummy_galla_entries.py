import asyncio
import time
import uuid
from datetime import datetime, timezone, timedelta
from sqlalchemy import select
from app.database import AsyncSessionLocal
from app.models.user import ShopProfile
from app.models.transaction import CashTransaction

SHOP_ID = "97f4b927-52dc-4601-b764-cc603a7ddeeb"
ist = timezone(timedelta(hours=5, minutes=30))

# 10 Dummy entries across 2 days (2026-10-29 and 2026-10-30)
ENTRIES = [
    # --- DAY 1: 2026-10-29 ---
    {
        "party_id": None,
        "type": "JAMA_RECEIVED",
        "amount_rs": 50000,
        "date_ist": datetime(2026, 10, 29, 10, 15, tzinfo=ist),
        "remarks": "[DUMMY_TEST] दुकान गल्ला प्रारंभिक रोकड़ (Opening Cash)",
        "category": "CASH_DEPOSIT"
    },
    {
        "party_id": "7d22ec86-312f-5d68-9af6-f039e8d8b303", # Agrawal Enterprises
        "type": "JAMA_RECEIVED",
        "amount_rs": 75000,
        "date_ist": datetime(2026, 10, 29, 11, 30, tzinfo=ist),
        "remarks": "[DUMMY_TEST] नकद आवक - Agrawal Enterprises",
        "category": "TRADE_PAYMENT"
    },
    {
        "party_id": "5aa73d9a-308c-5310-88db-5670c10f2957", # Dharmpal Singh
        "type": "UDHAR_GIVEN",
        "amount_rs": 25000,
        "date_ist": datetime(2026, 10, 29, 13, 0, tzinfo=ist),
        "remarks": "[DUMMY_TEST] नकद पेशगी भुगतान - Dharmpal Singh",
        "category": "TRADE_PAYMENT"
    },
    {
        "party_id": None,
        "type": "UDHAR_GIVEN",
        "amount_rs": 3500,
        "date_ist": datetime(2026, 10, 29, 14, 30, tzinfo=ist),
        "remarks": "[DUMMY_TEST] हम्माली व चाय खर्च (Labor & Tea)",
        "category": "EXPENSE"
    },
    {
        "party_id": "3f00306e-e6bd-4966-8b57-5ae84ccf8928", # Shaakir Khan
        "type": "JAMA_RECEIVED",
        "amount_rs": 20000,
        "date_ist": datetime(2026, 10, 29, 16, 45, tzinfo=ist),
        "remarks": "[DUMMY_TEST] नकद वसूली - Shaakir Khan",
        "category": "TRADE_PAYMENT"
    },

    # --- DAY 2: 2026-10-30 ---
    {
        "party_id": "0e31c34a-0e76-5d9f-8cd8-5d47da65aa6d", # Madanlal Gopaldas
        "type": "JAMA_RECEIVED",
        "amount_rs": 60000,
        "date_ist": datetime(2026, 10, 30, 10, 45, tzinfo=ist),
        "remarks": "[DUMMY_TEST] नकद आवक - Madanlal Gopaldas",
        "category": "TRADE_PAYMENT"
    },
    {
        "party_id": "4e24bc9b-f2c4-5567-85d9-ad19789b8d88", # Sunil Kumar
        "type": "UDHAR_GIVEN",
        "amount_rs": 40000,
        "date_ist": datetime(2026, 10, 30, 12, 15, tzinfo=ist),
        "remarks": "[DUMMY_TEST] नकद भुगतान - Sunil Kumar",
        "category": "TRADE_PAYMENT"
    },
    {
        "party_id": None,
        "type": "UDHAR_GIVEN",
        "amount_rs": 8000,
        "date_ist": datetime(2026, 10, 30, 14, 0, tzinfo=ist),
        "remarks": "[DUMMY_TEST] मंडी गाड़ी भाड़ा (Freight Expense)",
        "category": "EXPENSE"
    },
    {
        "party_id": "da0e07db-956e-523f-a043-64ddce418bf8", # Lalit Sales Corporation
        "type": "JAMA_RECEIVED",
        "amount_rs": 45000,
        "date_ist": datetime(2026, 10, 30, 15, 30, tzinfo=ist),
        "remarks": "[DUMMY_TEST] नकद आवक - Lalit Sales Corporation",
        "category": "TRADE_PAYMENT"
    },
    {
        "party_id": None,
        "type": "UDHAR_GIVEN",
        "amount_rs": 15000,
        "date_ist": datetime(2026, 10, 30, 17, 0, tzinfo=ist),
        "remarks": "[DUMMY_TEST] घर ले गए / गल्ला निकासी (Owner Withdrawal)",
        "category": "DRAWING"
    },
]

async def main():
    print(f"Creating 10 dummy cash entries across 2 days for shop: {SHOP_ID}")
    now_ms = int(time.time() * 1000)

    async with AsyncSessionLocal() as session:
        shop = (await session.execute(
            select(ShopProfile).where(ShopProfile.id == SHOP_ID).with_for_update()
        )).scalars().first()
        if not shop:
            raise RuntimeError(f"Shop {SHOP_ID} not found")

        current_seq = shop.last_seq or 0

        for item in ENTRIES:
            current_seq += 1
            tx_id = str(uuid.uuid4())
            tx_date_ms = int(item["date_ist"].timestamp() * 1000)
            amount_paisa = int(item["amount_rs"] * 100)

            tx = CashTransaction(
                id=tx_id,
                shop_id=SHOP_ID,
                party_id=item["party_id"],
                deal_id=None,
                transaction_type=item["type"],
                amount_paisa=amount_paisa,
                payment_mode="CASH",
                transaction_date=tx_date_ms,
                remarks=item["remarks"],
                category=item["category"],
                soundbox_broadcasted=0,
                is_void=0,
                revision=1,
                is_deleted=0,
                sync_version=1,
                server_seq=current_seq,
                created_at=now_ms,
                updated_at=now_ms,
            )
            session.add(tx)
            print(f"  Added: {item['remarks']} - ₹{item['amount_rs']} ({item['type']}) seq={current_seq}")

        shop.last_seq = current_seq
        await session.commit()
        print(f"Successfully committed all 10 dummy transactions! New last_seq: {current_seq}")

if __name__ == "__main__":
    asyncio.run(main())
