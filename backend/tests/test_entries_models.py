import pytest
import pytest_asyncio
from sqlalchemy import select
from sqlalchemy.ext.asyncio import create_async_engine, async_sessionmaker, AsyncSession
from app.database import Base
from app.models.user import ShopProfile
from app.models.buy_entry import BuyEntry
from app.models.sell_entry import SellEntry
from app.models.lend_entry import LendEntry, LendSettlement
from app.models.expense_entry import ExpenseEntry


@pytest_asyncio.fixture
async def db_session():
    engine = create_async_engine("sqlite+aiosqlite:///:memory:")
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)
    sm = async_sessionmaker(engine, class_=AsyncSession, expire_on_commit=False)
    async with sm() as s:
        s.add(ShopProfile(id="s1", shop_name="A", phone_number="9000000001"))
        await s.commit()
        yield s
    await engine.dispose()


@pytest.mark.asyncio
async def test_buy_entry_roundtrip(db_session):
    db_session.add(BuyEntry(
        id="b1", shop_id="s1", farmer_id="f1", commodity_id="c1", entry_date=1,
        bags_count=10, weight_per_bag_grams=50000, extra_weight_grams=5000, total_weight_grams=505000,
        rate_paisa_per_quintal=250000, commission_bps=200, labour_per_bag_paisa=500, advance_paid_paisa=100000,
        gross_paisa=1262500, commission_paisa=25250, labour_paisa=5000, net_payable_paisa=1132250,
        paid_paisa=0, created_at=1, updated_at=1, revision=1,
    ))
    await db_session.commit()
    row = (await db_session.execute(select(BuyEntry).where(BuyEntry.id == "b1"))).scalar_one()
    assert row.net_payable_paisa == 1132250
    assert row.server_seq is None or isinstance(row.server_seq, int)


@pytest.mark.asyncio
async def test_sell_entry_roundtrip(db_session):
    db_session.add(SellEntry(
        id="s1", shop_id="s1", buyer_id="buyer-1", commodity_id="c1", entry_date=1,
        bill_mode="BILL_OF_SUPPLY", party_kind="MERCHANT", bags_count=20, weight_per_bag_grams=50000,
        total_weight_grams=1000000, rate_paisa_per_quintal=260000, arat_bps=100, tulak_paisa=1000,
        gross_paisa=2600000, arat_paisa=26000, mandi_shulk_paisa=39000, total_receivable_paisa=2666000,
        received_paisa=1000000, balance_due_paisa=1666000, created_at=1, updated_at=1, revision=1,
    ))
    await db_session.commit()
    row = (await db_session.execute(select(SellEntry).where(SellEntry.id == "s1"))).scalar_one()
    assert row.total_receivable_paisa == 2666000


@pytest.mark.asyncio
async def test_lend_entry_and_settlement_roundtrip(db_session):
    db_session.add(LendEntry(
        id="l1", shop_id="s1", party_id="party-1", lend_type="MONEY", entry_date=1,
        amount_paisa=1000000, status="OPEN", created_at=1, updated_at=1, revision=1,
    ))
    await db_session.commit()
    db_session.add(LendSettlement(
        id="set1", shop_id="s1", lend_id="l1", settled_at=2, amount_paisa=400000,
        created_at=2, updated_at=2, revision=1,
    ))
    await db_session.commit()
    lend_row = (await db_session.execute(select(LendEntry).where(LendEntry.id == "l1"))).scalar_one()
    settle_row = (await db_session.execute(select(LendSettlement).where(LendSettlement.lend_id == "l1"))).scalar_one()
    assert lend_row.amount_paisa == 1000000
    assert settle_row.amount_paisa == 400000


@pytest.mark.asyncio
async def test_expense_entry_roundtrip(db_session):
    db_session.add(ExpenseEntry(
        id="e1", shop_id="s1", entry_date=1, category="HAMMALI", amount_paisa=50000,
        payment_mode="CASH", created_at=1, updated_at=1, revision=1,
    ))
    await db_session.commit()
    row = (await db_session.execute(select(ExpenseEntry).where(ExpenseEntry.id == "e1"))).scalar_one()
    assert row.amount_paisa == 50000
