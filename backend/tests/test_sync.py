import pytest
from app.schemas.sync import SyncPushRequest, SyncPushResponse
from app.schemas.party import PartyCreate
from app.schemas.deal import DealCreate
from app.schemas.transaction import CashTransactionCreate

def test_sync_push_payload_serialization():
    party = PartyCreate(name="अग्रवाल ट्रेडर्स", phone="9876543210", role="BUYER", village="मथुरा", current_balance=15000.0)
    deal = DealCreate(
        farmer_id="farmer_1",
        buyer_id="buyer_1",
        commodity="गेहूँ",
        bags=20,
        gross_weight=1000.0,
        net_weight=1000.0,
        rate=2275.0,
        farmer_total=22750.0,
        buyer_total=23205.0
    )
    tx = CashTransactionCreate(
        party_id="buyer_1",
        type="IN",
        amount=10000.0,
        category="TRADE_PAYMENT",
        notes="नकद भुगतान"
    )
    
    payload = SyncPushRequest(
        parties=[party],
        deals=[deal],
        transactions=[tx]
    )
    assert len(payload.parties) == 1
    assert len(payload.deals) == 1
    assert len(payload.transactions) == 1
    assert payload.deals[0].commodity == "गेहूँ"
