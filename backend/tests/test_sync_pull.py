import pytest
import pytest_asyncio
from sqlalchemy.ext.asyncio import create_async_engine, async_sessionmaker, AsyncSession
from app.database import Base
from app.models.user import ShopProfile
from app.api.v1.endpoints.sync import sync_push, sync_pull
from app.schemas.sync import SyncPushRequest
from app.schemas.party import PartyCreate
from app.schemas.deal import DealCreate
from app.schemas.revision import EntryRevisionCreate

USER = {"shop_id": "shop-a", "user_id": "u-a"}

@pytest_asyncio.fixture
async def db():
    engine = create_async_engine("sqlite+aiosqlite:///:memory:")
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)
    sm = async_sessionmaker(engine, class_=AsyncSession, expire_on_commit=False)
    async with sm() as s:
        s.add(ShopProfile(id="shop-a", shop_name="A", phone_number="9000000001"))
        await s.commit()
        yield s
    await engine.dispose()

@pytest.mark.asyncio
async def test_paging_by_seq_has_no_gaps_across_kinds(db):
    # 3 parties, then 3 deals, then 3 revisions: seq 1..9 across three tables.
    await sync_push(SyncPushRequest(parties=[PartyCreate(id=f"p{i}", name=f"n{i}", role="FARMER") for i in range(3)]), USER, db)
    await sync_push(SyncPushRequest(deals=[
        DealCreate(id=f"d{i}", farmer_id="p0", commodity="गेहूँ", gross_weight_grams=1, net_weight_grams=1,
                   net_farmer_payable_paisa=1, net_buyer_receivable_paisa=1) for i in range(3)]), USER, db)
    await sync_push(SyncPushRequest(revisions=[
        EntryRevisionCreate(id=f"r{i}", entry_id=f"d{i}", entry_kind="DEAL", revision=1, change_kind="CREATE",
                            snapshot_json="{}") for i in range(3)]), USER, db)

    seen, cursor, pages = [], 0, 0
    while True:
        page = await sync_pull(after_seq=cursor, limit=4, current_user=USER, db=db)
        items = page.parties + page.deals + page.transactions + page.revisions
        seen += sorted(i.server_seq for i in items)
        cursor, pages = page.next_seq, pages + 1
        if not page.has_more:
            break
    assert seen == list(range(1, 10)) and pages == 3

@pytest.mark.asyncio
async def test_empty_pull_keeps_cursor(db):
    page = await sync_pull(after_seq=7, limit=500, current_user=USER, db=db)
    assert page.next_seq == 7 and page.has_more is False

@pytest.mark.asyncio
async def test_pull_never_returns_rows_above_committed_last_seq(db):
    from app.models.party import Party
    await sync_push(SyncPushRequest(parties=[PartyCreate(id="p0", name="n0", role="FARMER")]), USER, db)  # seq 1, last_seq 1
    # A push still in flight elsewhere: its row is visible with seq 3 but last_seq was not committed past 1.
    db.add(Party(id="p-late", shop_id="shop-a", name="late", role="FARMER", server_seq=3))
    await db.commit()
    page = await sync_pull(after_seq=0, limit=500, current_user=USER, db=db)
    assert [p.id for p in page.parties] == ["p0"] and page.next_seq == 1 and page.has_more is False
