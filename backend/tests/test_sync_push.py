import pytest
import pytest_asyncio
from pydantic import ValidationError
from sqlalchemy import select
from sqlalchemy.ext.asyncio import create_async_engine, async_sessionmaker, AsyncSession
from app.database import Base
from app.models.user import ShopProfile
from app.models.deal import Deal
from app.models.party import Party
from app.models.revision import EntryRevision
from app.models.sync_conflict import SyncConflict
from app.api.v1.endpoints.sync import sync_push
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

def deal(revision=1, payable=1000):
    return DealCreate(id="d1", farmer_id="p1", commodity="गेहूँ", gross_weight_grams=1, net_weight_grams=1,
                      net_farmer_payable_paisa=payable, net_buyer_receivable_paisa=payable, revision=revision)

def rev(revision=1, snapshot="{}", rid="r1"):
    return EntryRevisionCreate(id=rid, entry_id="d1", entry_kind="DEAL", revision=revision,
                               change_kind="CREATE", snapshot_json=snapshot)

@pytest.mark.asyncio
async def test_duplicate_push_is_acked_again_without_new_seq(db):
    req = SyncPushRequest(parties=[PartyCreate(id="p1", name="रामवीर", role="FARMER", updated_at=10)],
                          deals=[deal()], revisions=[rev()])
    first = await sync_push(req, USER, db)
    second = await sync_push(req, USER, db)   # response of the first one was "lost"
    assert first.synced_revisions == second.synced_revisions == ["r1"]
    assert second.synced_deals == ["d1"] and second.synced_parties == ["p1"]
    assert first.server_seq == 3 and second.server_seq == 3   # nothing new, no new seq

@pytest.mark.asyncio
async def test_older_deal_revision_does_not_overwrite_newer(db):
    await sync_push(SyncPushRequest(deals=[deal(revision=2, payable=2000)]), USER, db)
    res = await sync_push(SyncPushRequest(deals=[deal(revision=1, payable=1000)]), USER, db)
    assert res.synced_deals == ["d1"]
    row = (await db.execute(select(Deal).where(Deal.id == "d1"))).scalars().one()
    assert row.net_farmer_payable_paisa == 2000 and row.revision == 2

@pytest.mark.asyncio
async def test_newer_deal_revision_applies_and_takes_next_seq(db):
    await sync_push(SyncPushRequest(deals=[deal(revision=1)]), USER, db)
    res = await sync_push(SyncPushRequest(deals=[deal(revision=2, payable=5)]), USER, db)
    row = (await db.execute(select(Deal).where(Deal.id == "d1"))).scalars().one()
    assert row.net_farmer_payable_paisa == 5 and row.server_seq == 2 == res.server_seq

@pytest.mark.asyncio
async def test_conflicting_revision_is_kept_flagged_and_acked(db):
    await sync_push(SyncPushRequest(revisions=[rev(snapshot='{"a":1}')]), USER, db)
    res = await sync_push(SyncPushRequest(revisions=[rev(snapshot='{"a":2}', rid="r1-other")]), USER, db)
    assert res.synced_revisions == ["r1-other"] and res.conflicts == ["r1-other"]
    kept = (await db.execute(select(EntryRevision))).scalars().all()
    assert [r.snapshot_json for r in kept] == ['{"a":1}']
    conflict = (await db.execute(select(SyncConflict))).scalars().one()
    assert conflict.snapshot_json == '{"a":2}' and conflict.shop_id == "shop-a"

@pytest.mark.asyncio
async def test_party_last_write_wins_by_updated_at(db):
    await sync_push(SyncPushRequest(parties=[PartyCreate(id="p1", name="नया", role="FARMER", updated_at=20)]), USER, db)
    res = await sync_push(SyncPushRequest(parties=[PartyCreate(id="p1", name="पुराना", role="FARMER", updated_at=10)]), USER, db)
    assert res.synced_parties == ["p1"]
    row = (await db.execute(select(Party).where(Party.id == "p1"))).scalars().one()
    assert row.name == "नया"

def test_more_than_100_revisions_is_rejected():
    with pytest.raises(ValidationError):
        SyncPushRequest(revisions=[rev(revision=i, rid=f"r{i}") for i in range(101)])

@pytest.mark.parametrize("raw,expected", [
    ("9876543210", "9876543210"),
    ("98765 43210", "9876543210"),
    ("+919876543210", "9876543210"),
    ("919876543210", "9876543210"),
    ("+91 98765 43210", "9876543210"),
    ("98765432101234567", None),   # longer than the String(15) column: must never fail a push
    ("12345", None),
    ("", None),
    (None, None),
])
def test_party_phone_is_normalised_or_dropped(raw, expected):
    assert PartyCreate(id="p1", name="n", role="FARMER", phone=raw).phone == expected

@pytest.mark.asyncio
async def test_party_with_overlong_phone_is_stored_without_phone(db):
    res = await sync_push(SyncPushRequest(parties=[PartyCreate(id="p1", name="n", role="FARMER", phone="1" * 20)]), USER, db)
    assert res.synced_parties == ["p1"]
    row = (await db.execute(select(Party).where(Party.id == "p1"))).scalars().one()
    assert row.phone is None

@pytest.mark.asyncio
async def test_older_deal_push_gives_held_row_a_new_seq(db):
    await sync_push(SyncPushRequest(deals=[deal(revision=2, payable=2000)]), USER, db)
    res = await sync_push(SyncPushRequest(deals=[deal(revision=1, payable=1000)]), USER, db)
    assert res.synced_deals == ["d1"]
    row = (await db.execute(select(Deal).where(Deal.id == "d1"))).scalars().one()
    assert row.revision == 2 and row.net_farmer_payable_paisa == 2000
    assert row.server_seq == 2 == res.server_seq   # re-delivered on the next pull

@pytest.mark.asyncio
async def test_older_party_push_gives_held_row_a_new_seq(db):
    await sync_push(SyncPushRequest(parties=[PartyCreate(id="p1", name="नया", role="FARMER", updated_at=20)]), USER, db)
    await sync_push(SyncPushRequest(parties=[PartyCreate(id="p1", name="पुराना", role="FARMER", updated_at=10)]), USER, db)
    row = (await db.execute(select(Party).where(Party.id == "p1"))).scalars().one()
    assert row.name == "नया" and row.server_seq == 2

@pytest.mark.asyncio
async def test_older_cash_push_gives_held_row_a_new_seq(db):
    from app.schemas.transaction import CashTransactionCreate
    from app.models.transaction import CashTransaction
    def tx(revision, amount):
        return CashTransactionCreate(id="t1", party_id="p1", transaction_type="PAYMENT", amount_paisa=amount,
                                     revision=revision)
    await sync_push(SyncPushRequest(transactions=[tx(2, 200)]), USER, db)
    await sync_push(SyncPushRequest(transactions=[tx(1, 100)]), USER, db)
    row = (await db.execute(select(CashTransaction).where(CashTransaction.id == "t1"))).scalars().one()
    assert row.amount_paisa == 200 and row.server_seq == 2
