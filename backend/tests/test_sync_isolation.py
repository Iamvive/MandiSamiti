import pytest
import pytest_asyncio
from sqlalchemy.ext.asyncio import create_async_engine, AsyncSession
from sqlalchemy.orm import sessionmaker
from app.database import Base
from app.models.user import ShopProfile, User
from app.models.party import Party
from app.models.deal import Deal
from app.models.transaction import CashTransaction
from app.models.revision import EntryRevision
from app.api.v1.endpoints.sync import sync_push, sync_pull
from app.schemas.sync import SyncPushRequest
from app.schemas.party import PartyCreate
from app.schemas.deal import DealCreate
from app.schemas.transaction import CashTransactionCreate
from app.schemas.revision import EntryRevisionCreate

@pytest_asyncio.fixture
async def test_db():
    engine = create_async_engine("sqlite+aiosqlite:///:memory:", echo=False)
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)
    
    async_session = sessionmaker(engine, class_=AsyncSession, expire_on_commit=False)
    async with async_session() as session:
        # Seed two shops
        shop_a = ShopProfile(id="shop-a", shop_name="Shop A", owner_name="Owner A", mandi_name="Mandi A", phone_number="9876543210", pin_hash="hash")
        shop_b = ShopProfile(id="shop-b", shop_name="Shop B", owner_name="Owner B", mandi_name="Mandi B", phone_number="9876543211", pin_hash="hash")
        session.add_all([shop_a, shop_b])
        await session.commit()
        yield session

@pytest.mark.asyncio
async def test_tenant_isolation_on_sync_push_and_pull(test_db):
    user_a = {"shop_id": "shop-a", "user_id": "user-a"}
    user_b = {"shop_id": "shop-b", "user_id": "user-b"}

    # 1. Shop A pushes a party, a deal, and a transaction
    party_a = PartyCreate(id="party-shared-id", name="रामवीर सिंह", phone="9800000001", role="FARMER", village="राया")
    deal_a = DealCreate(
        id="deal-shared-id",
        farmer_id="party-shared-id",
        commodity="सरसों",
        bags_count=10,
        gross_weight_grams=500000,
        net_weight_grams=500000,
        rate_paisa_per_unit=550000,
        net_farmer_payable_paisa=2750000,
        net_buyer_receivable_paisa=2800000
    )
    tx_a = CashTransactionCreate(
        id="tx-shared-id",
        party_id="party-shared-id",
        transaction_type="UDHAR_GIVEN",
        amount_paisa=500000
    )
    rev_a = EntryRevisionCreate(
        id="rev-1",
        entry_id="deal-shared-id",
        entry_kind="DEAL",
        revision=1,
        change_kind="CREATE",
        snapshot_json='{"amount": 2750000}'
    )

    push_req_a = SyncPushRequest(parties=[party_a], deals=[deal_a], transactions=[tx_a], revisions=[rev_a])
    res_a = await sync_push(push_req_a, current_user=user_a, db=test_db)
    assert res_a.success is True
    assert "party-shared-id" in res_a.synced_parties
    assert "deal-shared-id" in res_a.synced_deals
    assert "tx-shared-id" in res_a.synced_transactions

    # 2. Shop B pulls data -> must receive EMPTY lists (cannot see Shop A's records)
    pull_b = await sync_pull(after_seq=0, limit=500, current_user=user_b, db=test_db)
    assert len(pull_b.parties) == 0
    assert len(pull_b.deals) == 0
    assert len(pull_b.transactions) == 0
    assert len(pull_b.revisions) == 0

    # 3. Shop A pulls data -> must receive all its records
    pull_a = await sync_pull(after_seq=0, limit=500, current_user=user_a, db=test_db)
    assert len(pull_a.parties) == 1
    assert pull_a.parties[0].name == "रामवीर सिंह"
    assert len(pull_a.deals) == 1
    assert pull_a.deals[0].rate_paisa_per_unit == 550000
    assert len(pull_a.transactions) == 1
    assert pull_a.transactions[0].amount_paisa == 500000
    assert len(pull_a.revisions) == 1

@pytest.mark.asyncio
async def test_cross_tenant_overwrite_prevention(test_db):
    user_a = {"shop_id": "shop-a", "user_id": "user-a"}
    user_b = {"shop_id": "shop-b", "user_id": "user-b"}

    # Shop A creates a deal
    deal_a = DealCreate(
        id="target-deal-id",
        farmer_id="farmer-1",
        commodity="गेहूँ",
        rate_paisa_per_unit=250000,
        net_farmer_payable_paisa=2500000,
        net_buyer_receivable_paisa=2600000
    )
    await sync_push(SyncPushRequest(deals=[deal_a]), current_user=user_a, db=test_db)

    # Shop B pushes a deal with the same ID but modified values
    deal_b_malicious = DealCreate(
        id="target-deal-id",
        farmer_id="farmer-2",
        commodity="चना",
        rate_paisa_per_unit=999999,
        net_farmer_payable_paisa=9999999,
        net_buyer_receivable_paisa=9999999
    )
    # Shop B push creates/scopes under Shop B without mutating Shop A
    await sync_push(SyncPushRequest(deals=[deal_b_malicious]), current_user=user_b, db=test_db)

    # Verify Shop A's deal is unchanged
    pull_a = await sync_pull(after_seq=0, limit=500, current_user=user_a, db=test_db)
    assert len(pull_a.deals) == 1
    assert pull_a.deals[0].commodity == "गेहूँ"
    assert pull_a.deals[0].rate_paisa_per_unit == 250000

@pytest.mark.asyncio
async def test_voided_entry_and_revision_sync(test_db):
    user_a = {"shop_id": "shop-a", "user_id": "user-a"}

    # Record deal with void status & revision
    deal = DealCreate(
        id="deal-voided-1",
        farmer_id="farmer-1",
        commodity="बाजरा",
        rate_paisa_per_unit=180000,
        net_farmer_payable_paisa=1800000,
        net_buyer_receivable_paisa=1900000,
        is_void=1,
        void_reason="तौल त्रुटि",
        revision=2
    )
    rev1 = EntryRevisionCreate(
        id="rev-create",
        entry_id="deal-voided-1",
        entry_kind="DEAL",
        revision=1,
        change_kind="CREATE",
        snapshot_json='{"rate": 180000}'
    )
    rev2 = EntryRevisionCreate(
        id="rev-void",
        entry_id="deal-voided-1",
        entry_kind="DEAL",
        revision=2,
        change_kind="VOID",
        snapshot_json='{"rate": 180000, "is_void": 1}',
        void_reason="तौल त्रुटि"
    )

    res = await sync_push(SyncPushRequest(deals=[deal], revisions=[rev1, rev2]), current_user=user_a, db=test_db)
    assert res.success is True
    assert len(res.synced_revisions) == 2

    pull = await sync_pull(after_seq=0, limit=500, current_user=user_a, db=test_db)
    assert len(pull.deals) == 1
    assert pull.deals[0].is_void == 1
    assert pull.deals[0].void_reason == "तौल त्रुटि"
    assert len(pull.revisions) == 2
    assert {r.revision for r in pull.revisions} == {1, 2}

