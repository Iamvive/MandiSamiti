import pytest


@pytest.mark.asyncio
async def test_push_buy_entry_then_pull_returns_it(client, auth_headers, buy_payload):
    response = await client.post(
        "/api/v1/sync/push",
        json={"buy_entries": [buy_payload]},
        headers=auth_headers,
    )
    assert response.status_code == 200
    assert response.json()["synced_buy_entries"] == [buy_payload["id"]]

    pull = await client.get("/api/v1/sync/pull?server_seq=0", headers=auth_headers)
    assert [entry["id"] for entry in pull.json()["buy_entries"]] == [buy_payload["id"]]


@pytest.mark.asyncio
async def test_repush_same_revision_is_acked_not_duplicated(client, auth_headers, buy_payload):
    for _ in range(2):
        response = await client.post(
            "/api/v1/sync/push",
            json={"buy_entries": [buy_payload]},
            headers=auth_headers,
        )
        assert response.json()["synced_buy_entries"] == [buy_payload["id"]]

    pull = await client.get("/api/v1/sync/pull?server_seq=0", headers=auth_headers)
    assert len(pull.json()["buy_entries"]) == 1


@pytest.mark.asyncio
async def test_older_revision_does_not_overwrite(client, auth_headers, buy_payload):
    newer = {**buy_payload, "revision": 2, "paid_paisa": 500}
    await client.post(
        "/api/v1/sync/push",
        json={"buy_entries": [newer]},
        headers=auth_headers,
    )
    await client.post(
        "/api/v1/sync/push",
        json={"buy_entries": [buy_payload]},
        headers=auth_headers,
    )

    pull = await client.get("/api/v1/sync/pull?server_seq=0", headers=auth_headers)
    assert pull.json()["buy_entries"][0]["paid_paisa"] == 500


@pytest.mark.asyncio
async def test_other_shop_cannot_see_entries(
    client,
    auth_headers,
    other_shop_headers,
    buy_payload,
):
    await client.post(
        "/api/v1/sync/push",
        json={"buy_entries": [buy_payload]},
        headers=auth_headers,
    )

    pull = await client.get("/api/v1/sync/pull?server_seq=0", headers=other_shop_headers)
    assert pull.json()["buy_entries"] == []


@pytest.mark.asyncio
async def test_pull_orders_all_kinds_by_server_seq(
    client,
    auth_headers,
    buy_payload,
    expense_payload,
):
    await client.post(
        "/api/v1/sync/push",
        json={"buy_entries": [buy_payload], "expense_entries": [expense_payload]},
        headers=auth_headers,
    )

    pull = (
        await client.get("/api/v1/sync/pull?server_seq=0", headers=auth_headers)
    ).json()
    seqs = [entry["server_seq"] for entry in pull["buy_entries"] + pull["expense_entries"]]
    assert sorted(seqs) == list(set(seqs))


@pytest.mark.asyncio
async def test_batch_over_100_rejected(client, auth_headers, buy_payload):
    big = [{**buy_payload, "id": f"b{i}"} for i in range(101)]
    response = await client.post(
        "/api/v1/sync/push",
        json={"buy_entries": big},
        headers=auth_headers,
    )
    assert response.status_code == 422
