import pytest
import pytest_asyncio
from app.models.party import Party
from app.models.lend_entry import LendEntry


@pytest_asyncio.fixture
async def seed_party_and_lend(db_sessionmaker):
    async with db_sessionmaker() as session:
        session.add(Party(id="party-c4", shop_id="shop-a", name="TEST Ramesh", phone="9876500001", role="FARMER"))
        session.add(LendEntry(id="lend-c4", shop_id="shop-a", party_id="party-c4", amount_paisa=100000))
        await session.commit()


@pytest.mark.asyncio
async def test_settlement_split_round_trip(client, auth_headers, seed_party_and_lend):
    settlement = {
        "id": "settle-c4-1",
        "lend_id": "lend-c4",
        "settled_at": 1000,
        "amount_paisa": 120000,
        "principal_paisa": 100000,
        "interest_paisa": 20000,
        "calculated_interest_paisa": 20000,
        "weight_grams": 0,
        "bags_count": 0,
        "is_void": 0,
        "void_reason": None,
        "revision": 1,
    }
    push_res = await client.post(
        "/api/v1/sync/push",
        json={"lend_settlements": [settlement]},
        headers=auth_headers,
    )
    assert push_res.status_code == 200
    assert push_res.json()["synced_lend_settlements"] == ["settle-c4-1"]

    pull = await client.get("/api/v1/sync/pull?server_seq=0", headers=auth_headers)
    assert pull.status_code == 200
    pulled_settlements = pull.json()["lend_settlements"]
    assert len(pulled_settlements) == 1
    s = pulled_settlements[0]
    assert s["id"] == "settle-c4-1"
    assert s["amount_paisa"] == 120000
    assert s["principal_paisa"] == 100000
    assert s["interest_paisa"] == 20000
    assert s["calculated_interest_paisa"] == 20000


@pytest.mark.asyncio
async def test_old_settlement_payload_without_split_accepted(client, auth_headers, seed_party_and_lend):
    old_settlement = {
        "id": "settle-c4-old",
        "lend_id": "lend-c4",
        "settled_at": 2000,
        "amount_paisa": 50000,
        "weight_grams": 0,
        "bags_count": 0,
        "is_void": 0,
        "void_reason": None,
        "revision": 1,
    }
    push_res = await client.post(
        "/api/v1/sync/push",
        json={"lend_settlements": [old_settlement]},
        headers=auth_headers,
    )
    assert push_res.status_code == 200
    assert push_res.json()["synced_lend_settlements"] == ["settle-c4-old"]

    pull = await client.get("/api/v1/sync/pull?server_seq=0", headers=auth_headers)
    assert pull.status_code == 200
    pulled = [s for s in pull.json()["lend_settlements"] if s["id"] == "settle-c4-old"][0]
    assert pulled["amount_paisa"] == 50000
    assert pulled["principal_paisa"] == 0
    assert pulled["interest_paisa"] == 0
    assert pulled["calculated_interest_paisa"] == 0


@pytest.mark.asyncio
async def test_interest_received_cash_transaction_round_trip(client, auth_headers, seed_party_and_lend):
    cash_tx = {
        "id": "tx-interest-1",
        "transaction_type": "INTEREST_RECEIVED",
        "amount_paisa": 20000,
        "transaction_date": 1000,
        "payment_mode": "CASH",
        "party_id": "party-c4",
        "reference_id": "settle-c4-1",
        "notes": "ब्याज हिसाब",
        "is_void": 0,
        "revision": 1,
    }
    push_res = await client.post(
        "/api/v1/sync/push",
        json={"transactions": [cash_tx]},
        headers=auth_headers,
    )
    assert push_res.status_code == 200
    assert push_res.json()["synced_transactions"] == ["tx-interest-1"]

    pull = await client.get("/api/v1/sync/pull?server_seq=0", headers=auth_headers)
    assert pull.status_code == 200
    txs = [t for t in pull.json()["transactions"] if t["id"] == "tx-interest-1"]
    assert len(txs) == 1
    assert txs[0]["transaction_type"] == "INTEREST_RECEIVED"
    assert txs[0]["amount_paisa"] == 20000


@pytest.mark.asyncio
async def test_slip_no_round_trip_and_null_accepted(client, auth_headers, buy_payload, seed_party_and_lend):
    # Buy entry with slip_no = 42
    buy_with_slip = {**buy_payload, "id": "buy-slip-42", "slip_no": 42}
    # Buy entry with slip_no = None
    buy_null_slip = {**buy_payload, "id": "buy-slip-null", "slip_no": None}

    # Sell entry with slip_no = 101
    sell_entry = {
        "id": "sell-slip-101",
        "buyer_id": "party-c4",
        "commodity_id": "wheat",
        "entry_date": 1000,
        "slip_no": 101,
        "gross_paisa": 500000,
        "total_receivable_paisa": 500000,
        "balance_due_paisa": 500000,
        "revision": 1,
    }

    # Lend entry with slip_no = 7
    lend_entry = {
        "id": "lend-slip-7",
        "party_id": "party-c4",
        "lend_type": "MONEY",
        "entry_date": 1000,
        "amount_paisa": 100000,
        "slip_no": 7,
        "revision": 1,
    }

    push_res = await client.post(
        "/api/v1/sync/push",
        json={
            "buy_entries": [buy_with_slip, buy_null_slip],
            "sell_entries": [sell_entry],
            "lend_entries": [lend_entry],
        },
        headers=auth_headers,
    )
    assert push_res.status_code == 200
    assert "buy-slip-42" in push_res.json()["synced_buy_entries"]
    assert "buy-slip-null" in push_res.json()["synced_buy_entries"]
    assert "sell-slip-101" in push_res.json()["synced_sell_entries"]
    assert "lend-slip-7" in push_res.json()["synced_lend_entries"]

    pull = await client.get("/api/v1/sync/pull?server_seq=0", headers=auth_headers)
    assert pull.status_code == 200
    data = pull.json()

    pulled_buys = {b["id"]: b for b in data["buy_entries"]}
    assert pulled_buys["buy-slip-42"]["slip_no"] == 42
    assert pulled_buys["buy-slip-null"]["slip_no"] is None

    pulled_sells = {s["id"]: s for s in data["sell_entries"]}
    assert pulled_sells["sell-slip-101"]["slip_no"] == 101

    pulled_lends = {l["id"]: l for l in data["lend_entries"]}
    assert pulled_lends["lend-slip-7"]["slip_no"] == 7
