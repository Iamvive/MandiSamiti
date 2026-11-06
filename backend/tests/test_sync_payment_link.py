import pytest
import pytest_asyncio
from app.models.party import Party


@pytest_asyncio.fixture
async def seed_party(db_sessionmaker):
    async with db_sessionmaker() as session:
        session.add(Party(id="party-pl", shop_id="shop-a", name="TEST Ramesh", phone="9876500001", role="FARMER"))
        await session.commit()


def _tx(**extra):
    base = {
        "id": "tx-pl-1",
        "party_id": "party-pl",
        "transaction_type": "UDHAR_GIVEN",
        "amount_paisa": 25000,
        "revision": 1,
    }
    base.update(extra)
    return base


@pytest.mark.asyncio
async def test_payment_link_to_slip_round_trips(client, auth_headers, seed_party):
    push = await client.post(
        "/api/v1/sync/push",
        json={"transactions": [_tx(source_entry_type="BUY", source_entry_id="buy-1")]},
        headers=auth_headers,
    )
    assert push.status_code == 200
    assert push.json()["synced_transactions"] == ["tx-pl-1"]

    pull = await client.get("/api/v1/sync/pull?server_seq=0", headers=auth_headers)
    row = pull.json()["transactions"][0]
    assert row["source_entry_type"] == "BUY"
    assert row["source_entry_id"] == "buy-1"


@pytest.mark.asyncio
async def test_old_client_payload_without_link_is_accepted(client, auth_headers, seed_party):
    push = await client.post("/api/v1/sync/push", json={"transactions": [_tx()]}, headers=auth_headers)
    assert push.status_code == 200
    assert push.json()["synced_transactions"] == ["tx-pl-1"]

    pull = await client.get("/api/v1/sync/pull?server_seq=0", headers=auth_headers)
    row = pull.json()["transactions"][0]
    assert row["source_entry_type"] is None
    assert row["source_entry_id"] is None


@pytest.mark.asyncio
async def test_newer_revision_updates_the_link(client, auth_headers, seed_party):
    await client.post("/api/v1/sync/push", json={"transactions": [_tx()]}, headers=auth_headers)
    await client.post(
        "/api/v1/sync/push",
        json={"transactions": [_tx(revision=2, source_entry_type="BUY", source_entry_id="buy-1")]},
        headers=auth_headers,
    )
    pull = await client.get("/api/v1/sync/pull?server_seq=0", headers=auth_headers)
    row = pull.json()["transactions"][0]
    assert row["source_entry_id"] == "buy-1"
