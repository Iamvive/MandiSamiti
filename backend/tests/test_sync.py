import pytest
from app.schemas.sync import SyncPushRequest, SyncPushResponse
from app.schemas.party import PartyCreate
from app.schemas.deal import DealCreate
from app.schemas.transaction import CashTransactionCreate
from app.schemas.revision import EntryRevisionCreate

def test_sync_push_payload_serialization():
    party = PartyCreate(name="अग्रवाल ट्रेडर्स", phone="9876543210", role="BUYER", village="मथुरा", monthly_interest_rate=1.5)
    deal = DealCreate(
        id="deal-1",
        farmer_id="farmer_1",
        buyer_id="buyer_1",
        commodity="गेहूँ",
        deal_status="SETTLED",
        bags_count=20,
        gross_weight_grams=1000000,
        cut_weight_grams=2000,
        net_weight_grams=998000,
        rate_paisa_per_unit=227550,
        gross_amount_paisa=2270949,
        farmer_commission_bps=250,
        farmer_commission_paisa=56774,
        net_farmer_payable_paisa=2214175,
        net_buyer_receivable_paisa=2327723
    )
    tx = CashTransactionCreate(
        id="tx-1",
        party_id="buyer_1",
        transaction_type="JAMA_RECEIVED",
        amount_paisa=1000000,
        payment_mode="CASH",
        category="TRADE_PAYMENT",
        remarks="नकद भुगतान"
    )
    rev = EntryRevisionCreate(
        id="rev-1",
        entry_id="deal-1",
        entry_kind="DEAL",
        revision=1,
        change_kind="CREATE",
        snapshot_json="{}",
        void_reason=None
    )
    
    payload = SyncPushRequest(
        parties=[party],
        deals=[deal],
        transactions=[tx],
        revisions=[rev]
    )
    assert len(payload.parties) == 1
    assert len(payload.deals) == 1
    assert len(payload.transactions) == 1
    assert len(payload.revisions) == 1
    assert payload.deals[0].rate_paisa_per_unit == 227550
    assert payload.deals[0].farmer_commission_bps == 250
    assert payload.transactions[0].amount_paisa == 1000000

