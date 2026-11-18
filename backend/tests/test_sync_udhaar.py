import pytest
import pytest_asyncio
from app.models.party import Party


@pytest_asyncio.fixture
async def seed_party(db_sessionmaker):
    async with db_sessionmaker() as session:
        session.add(Party(id="party-ud", shop_id="shop-a", name="TEST Sohan", phone="9876500002", role="FARMER"))
        await session.commit()


def _lend(**extra):
    base = {"id": "lend-ud-1", "party_id": "party-ud", "amount_paisa": 100000, "revision": 1}
    base.update(extra)
    return base


def _tx(**extra):
    base = {
        "id": "tx-ud-1", "party_id": "party-ud", "transaction_type": "JAMA_RECEIVED",
        "amount_paisa": 40000, "revision": 1,
    }
    base.update(extra)
    return base


async def _pull(client, auth_headers):
    return (await client.get("/api/v1/sync/pull?server_seq=0", headers=auth_headers)).json()


@pytest.mark.asyncio
async def test_lend_direction_kind_receiver_round_trip(client, auth_headers, seed_party):
    push = await client.post(
        "/api/v1/sync/push",
        json={"lend_entries": [_lend(direction="TAKEN", kind="PESGI", receiver_name="Ramu", receiver_phone="9876543210")]},
        headers=auth_headers,
    )
    assert push.status_code == 200
    assert push.json()["synced_lend_entries"] == ["lend-ud-1"]

    row = (await _pull(client, auth_headers))["lend_entries"][0]
    assert (row["direction"], row["kind"]) == ("TAKEN", "PESGI")
    assert (row["receiver_name"], row["receiver_phone"]) == ("Ramu", "9876543210")


@pytest.mark.asyncio
async def test_old_client_lend_without_new_fields_gets_defaults(client, auth_headers, seed_party):
    push = await client.post("/api/v1/sync/push", json={"lend_entries": [_lend()]}, headers=auth_headers)
    assert push.status_code == 200

    row = (await _pull(client, auth_headers))["lend_entries"][0]
    assert (row["direction"], row["kind"]) == ("GIVEN", "LOAN")
    assert row["receiver_name"] is None and row["receiver_phone"] is None


@pytest.mark.asyncio
async def test_newer_revision_updates_udhaar_fields(client, auth_headers, seed_party):
    await client.post("/api/v1/sync/push", json={"lend_entries": [_lend()]}, headers=auth_headers)
    await client.post(
        "/api/v1/sync/push",
        json={"lend_entries": [_lend(revision=2, direction="TAKEN", kind="CASH", receiver_name="Ramu")]},
        headers=auth_headers,
    )
    row = (await _pull(client, auth_headers))["lend_entries"][0]
    assert (row["direction"], row["kind"], row["receiver_name"]) == ("TAKEN", "CASH", "Ramu")


@pytest.mark.asyncio
async def test_cash_row_lend_id_round_trip(client, auth_headers, seed_party):
    push = await client.post("/api/v1/sync/push", json={"transactions": [_tx(lend_id="lend-ud-1")]}, headers=auth_headers)
    assert push.status_code == 200
    assert (await _pull(client, auth_headers))["transactions"][0]["lend_id"] == "lend-ud-1"


@pytest.mark.asyncio
async def test_old_client_cash_row_has_null_lend_id(client, auth_headers, seed_party):
    await client.post("/api/v1/sync/push", json={"transactions": [_tx()]}, headers=auth_headers)
    assert (await _pull(client, auth_headers))["transactions"][0]["lend_id"] is None


@pytest.mark.asyncio
async def test_settlement_principal_and_interest_round_trip(client, auth_headers, seed_party):
    await client.post("/api/v1/sync/push", json={"lend_entries": [_lend()]}, headers=auth_headers)
    push = await client.post(
        "/api/v1/sync/push",
        json={"lend_settlements": [{
            "id": "set-ud-1", "lend_id": "lend-ud-1", "amount_paisa": 45000, "principal_paisa": 40000,
            "interest_paisa": 5000, "calculated_interest_paisa": 4800, "revision": 1,
        }]},
        headers=auth_headers,
    )
    assert push.status_code == 200
    row = (await _pull(client, auth_headers))["lend_settlements"][0]
    assert (row["principal_paisa"], row["interest_paisa"], row["calculated_interest_paisa"]) == (40000, 5000, 4800)


@pytest.mark.asyncio
async def test_voided_settlement_and_cash_row_sync_as_void(client, auth_headers, seed_party):
    await client.post("/api/v1/sync/push", json={"lend_entries": [_lend()]}, headers=auth_headers)
    settle = {"id": "set-ud-2", "lend_id": "lend-ud-1", "amount_paisa": 40000, "principal_paisa": 40000, "revision": 1}
    await client.post(
        "/api/v1/sync/push",
        json={"lend_settlements": [settle], "transactions": [_tx(id="tx-ud-2", lend_id="lend-ud-1")]},
        headers=auth_headers,
    )
    push = await client.post(
        "/api/v1/sync/push",
        json={
            "lend_settlements": [{**settle, "revision": 2, "is_void": 1, "void_reason": "WRONG_ENTRY"}],
            "transactions": [_tx(id="tx-ud-2", lend_id="lend-ud-1", revision=2, is_void=1, void_reason="WRONG_ENTRY")],
        },
        headers=auth_headers,
    )
    assert push.status_code == 200
    pulled = await _pull(client, auth_headers)
    assert pulled["lend_settlements"][0]["is_void"] == 1
    assert pulled["transactions"][0]["is_void"] == 1
    assert pulled["transactions"][0]["lend_id"] == "lend-ud-1"
