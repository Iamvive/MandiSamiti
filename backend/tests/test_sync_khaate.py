import pytest


@pytest.mark.asyncio
async def test_sync_round_trips_is_khaate_buy_and_sell(client, auth_headers, buy_payload):
    # 1. System party khaate-shop-a
    party_payload = {
        "id": "khaate-shop-a",
        "name": "Khaate",
        "phone": None,
        "village": None,
        "role": "FARMER",
        "sync_version": 1,
        "is_deleted": 0,
    }
    party_res = await client.post(
        "/api/v1/sync/push",
        json={"parties": [party_payload]},
        headers=auth_headers,
    )
    assert party_res.status_code == 200
    assert "khaate-shop-a" in party_res.json()["synced_parties"]

    # 2. Push buy entry with is_khaate = True
    buy_khaate = {
        **buy_payload,
        "id": "buy-khaate-1",
        "farmer_id": "khaate-shop-a",
        "is_khaate": True,
    }

    # 3. Push sell entry with is_khaate = True
    sell_khaate = {
        "id": "sell-khaate-1",
        "shop_id": "shop-a",
        "buyer_id": "khaate-shop-a",
        "commodity_id": "wheat",
        "entry_date": 1_000,
        "bill_mode": "NORMAL",
        "party_kind": "MERCHANT",
        "bags_count": 8,
        "weight_per_bag_grams": 50_000,
        "total_weight_grams": 400_000,
        "rate_paisa_per_quintal": 220_000,
        "arat_bps": 0,
        "tulak_paisa": 0,
        "gross_paisa": 880_000,
        "arat_paisa": 0,
        "mandi_shulk_paisa": 0,
        "total_receivable_paisa": 880_000,
        "received_paisa": 0,
        "balance_due_paisa": 880_000,
        "created_at": 1_000,
        "updated_at": 1_000,
        "is_deleted": 0,
        "sync_status": 0,
        "revision": 1,
        "is_void": 0,
        "is_khaate": True,
    }

    push_res = await client.post(
        "/api/v1/sync/push",
        json={"buy_entries": [buy_khaate], "sell_entries": [sell_khaate]},
        headers=auth_headers,
    )
    assert push_res.status_code == 200
    assert "buy-khaate-1" in push_res.json()["synced_buy_entries"]
    assert "sell-khaate-1" in push_res.json()["synced_sell_entries"]

    # 4. Pull and verify is_khaate flag round-trips
    pull = await client.get("/api/v1/sync/pull?server_seq=0", headers=auth_headers)
    assert pull.status_code == 200
    pulled_buys = {b["id"]: b for b in pull.json()["buy_entries"]}
    pulled_sells = {s["id"]: s for s in pull.json()["sell_entries"]}

    assert "buy-khaate-1" in pulled_buys
    assert pulled_buys["buy-khaate-1"]["is_khaate"] is True

    assert "sell-khaate-1" in pulled_sells
    assert pulled_sells["sell-khaate-1"]["is_khaate"] is True


@pytest.mark.asyncio
async def test_sync_defaults_is_khaate_to_false_for_legacy_entries(client, auth_headers, buy_payload):
    legacy_buy = {**buy_payload, "id": "buy-legacy-1"}
    legacy_buy.pop("is_khaate", None)

    push_res = await client.post(
        "/api/v1/sync/push",
        json={"buy_entries": [legacy_buy]},
        headers=auth_headers,
    )
    assert push_res.status_code == 200

    pull = await client.get("/api/v1/sync/pull?server_seq=0", headers=auth_headers)
    pulled_buys = {b["id"]: b for b in pull.json()["buy_entries"]}
    assert pulled_buys["buy-legacy-1"]["is_khaate"] is False
