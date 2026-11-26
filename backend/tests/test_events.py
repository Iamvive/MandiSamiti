import fakeredis.aioredis
import pytest
from sqlalchemy import select

from app.core.redis_client import get_redis
from app.core.security import create_access_token
from app.main import app
from app.models.analytics_event import AnalyticsEvent
from app.models.user import ShopProfile


def _event(
    event_id: str = "e1",
    name: str = "screen_view",
    screen: str | None = "buy_list",
    session_id: str = "s1",
    occurred_at_ms: int = 1_764_140_000_000,
    duration_ms: int | None = None,
    app_version: str = "1.0",
) -> dict:
    return {
        "id": event_id,
        "name": name,
        "screen": screen,
        "session_id": session_id,
        "occurred_at_ms": occurred_at_ms,
        "duration_ms": duration_ms,
        "app_version": app_version,
    }


def _bearer(shop_id: str | None, user_id: str = "u1") -> dict[str, str]:
    token = create_access_token(subject=user_id, shop_id=shop_id, role="OWNER")
    return {"Authorization": f"Bearer {token}"}


@pytest.mark.asyncio
async def test_events_requires_auth(client):
    res = await client.post("/api/v1/events", json={"events": [_event()]})
    assert res.status_code in (401, 403)


@pytest.mark.asyncio
async def test_events_rejects_token_without_shop_id(client):
    res = await client.post(
        "/api/v1/events",
        headers=_bearer(shop_id=None),
        json={"events": [_event()]},
    )
    assert res.status_code == 400


@pytest.mark.asyncio
async def test_shop_taken_from_token_and_server_received_at_set(client, db_sessionmaker):
    async with db_sessionmaker() as s:
        s.add(ShopProfile(id="shop-a", shop_name="Shop A"))
        await s.commit()

    res = await client.post(
        "/api/v1/events",
        headers=_bearer("shop-a"),
        json={"events": [_event(event_id="ev-1", duration_ms=42)]},
    )
    assert res.status_code == 200
    assert res.json() == {"accepted": 1}

    async with db_sessionmaker() as s:
        rows = (await s.execute(select(AnalyticsEvent))).scalars().all()
    assert len(rows) == 1
    assert rows[0].shop_id == "shop-a"
    assert rows[0].id == "ev-1"
    assert rows[0].duration_ms == 42
    assert rows[0].received_at_ms > 0


@pytest.mark.asyncio
async def test_repeat_ids_stored_once_in_batch_and_across_requests(client, db_sessionmaker):
    async with db_sessionmaker() as s:
        s.add(ShopProfile(id="shop-a", shop_name="Shop A"))
        await s.commit()

    headers = _bearer("shop-a")
    res1 = await client.post(
        "/api/v1/events",
        headers=headers,
        json={"events": [_event(event_id="dup-1"), _event(event_id="dup-1", name="app_open", screen=None)]},
    )
    assert res1.status_code == 200
    assert res1.json() == {"accepted": 2}

    res2 = await client.post(
        "/api/v1/events",
        headers=headers,
        json={"events": [_event(event_id="dup-1"), _event(event_id="new-2")]},
    )
    assert res2.status_code == 200
    assert res2.json() == {"accepted": 2}

    async with db_sessionmaker() as s:
        rows = (await s.execute(select(AnalyticsEvent).order_by(AnalyticsEvent.id))).scalars().all()
    assert [(r.shop_id, r.id, r.name) for r in rows] == [
        ("shop-a", "dup-1", "screen_view"),
        ("shop-a", "new-2", "screen_view"),
    ]


@pytest.mark.asyncio
async def test_same_id_from_two_shops_stored_twice(client, db_sessionmaker):
    async with db_sessionmaker() as s:
        s.add_all([
            ShopProfile(id="shop-a", shop_name="Shop A"),
            ShopProfile(id="shop-b", shop_name="Shop B"),
        ])
        await s.commit()

    r_a = await client.post("/api/v1/events", headers=_bearer("shop-a"), json={"events": [_event(event_id="shared-1")]})
    r_b = await client.post("/api/v1/events", headers=_bearer("shop-b"), json={"events": [_event(event_id="shared-1")]})
    assert r_a.status_code == 200
    assert r_b.status_code == 200

    async with db_sessionmaker() as s:
        rows = (await s.execute(select(AnalyticsEvent).order_by(AnalyticsEvent.shop_id))).scalars().all()
    assert [(r.shop_id, r.id) for r in rows] == [("shop-a", "shared-1"), ("shop-b", "shared-1")]


@pytest.mark.asyncio
async def test_unknown_fields_rejected_at_both_levels(client):
    headers = _bearer("shop-a")
    top_extra = await client.post(
        "/api/v1/events",
        headers=headers,
        json={"events": [_event()], "unexpected": True},
    )
    assert top_extra.status_code == 422

    item_extra = await client.post(
        "/api/v1/events",
        headers=headers,
        json={"events": [{**_event(), "shop_id": "other-shop"}]},
    )
    assert item_extra.status_code == 422


@pytest.mark.asyncio
@pytest.mark.parametrize(
    "overrides",
    [
        {"name": "ScreenView"},
        {"name": "bad-name"},
        {"name": ""},
        {"name": "screen_view\n"},
        {"name": "a" * 65},
        {"screen": "BadScreen"},
        {"screen": ""},
        {"id": "bad/id"},
        {"id": ""},
        {"id": "x" * 65},
        {"session_id": "bad session"},
        {"app_version": ""},
        {"app_version": "1" * 33},
        {"app_version": "1.0/beta"},
        {"occurred_at_ms": -1},
        {"occurred_at_ms": 2**63},
        {"occurred_at_ms": True},
        {"duration_ms": -1},
        {"duration_ms": 2**63},
    ],
)
async def test_invalid_event_fields_rejected(client, overrides):
    res = await client.post(
        "/api/v1/events",
        headers=_bearer("shop-a"),
        json={"events": [{**_event(), **overrides}]},
    )
    assert res.status_code == 422


@pytest.mark.asyncio
async def test_batch_size_limit(client, db_sessionmaker):
    async with db_sessionmaker() as s:
        s.add(ShopProfile(id="shop-a", shop_name="Shop A"))
        await s.commit()

    headers = _bearer("shop-a")
    too_many = [_event(event_id=f"e-{i}") for i in range(201)]
    res_422 = await client.post("/api/v1/events", headers=headers, json={"events": too_many})
    assert res_422.status_code == 422

    exact_200 = [_event(event_id=f"e-{i}") for i in range(200)]
    res_200 = await client.post("/api/v1/events", headers=headers, json={"events": exact_200})
    assert res_200.status_code == 200
    assert res_200.json() == {"accepted": 200}

    empty_res = await client.post("/api/v1/events", headers=headers, json={"events": []})
    assert empty_res.status_code == 200
    assert empty_res.json() == {"accepted": 0}


@pytest.mark.asyncio
async def test_under_limit_accepted(client, db_sessionmaker):
    async with db_sessionmaker() as s:
        s.add(ShopProfile(id="shop-a", shop_name="Shop A"))
        await s.commit()

    headers = _bearer("shop-a")
    for i in range(60):
        res = await client.post(
            "/api/v1/events",
            headers=headers,
            json={"events": [_event(event_id=f"ev-{i}")]},
        )
        assert res.status_code == 200


@pytest.mark.asyncio
async def test_over_limit_returns_429(client, db_sessionmaker):
    async with db_sessionmaker() as s:
        s.add(ShopProfile(id="shop-a", shop_name="Shop A"))
        await s.commit()

    headers = _bearer("shop-a")
    for i in range(60):
        res = await client.post(
            "/api/v1/events",
            headers=headers,
            json={"events": [_event(event_id=f"ev-{i}")]},
        )
        assert res.status_code == 200

    res_61 = await client.post(
        "/api/v1/events",
        headers=headers,
        json={"events": [_event(event_id="ev-61")]},
    )
    assert res_61.status_code == 429

    async with db_sessionmaker() as s:
        stored = (await s.execute(select(AnalyticsEvent).where(AnalyticsEvent.id == "ev-61"))).scalars().first()
    assert stored is None


@pytest.mark.asyncio
async def test_limits_are_per_shop(client, db_sessionmaker):
    async with db_sessionmaker() as s:
        s.add_all([
            ShopProfile(id="shop-a", shop_name="Shop A"),
            ShopProfile(id="shop-b", shop_name="Shop B"),
        ])
        await s.commit()

    headers_a = _bearer("shop-a")
    for i in range(60):
        res = await client.post(
            "/api/v1/events",
            headers=headers_a,
            json={"events": [_event(event_id=f"ev-a-{i}")]},
        )
        assert res.status_code == 200

    res_61 = await client.post(
        "/api/v1/events",
        headers=headers_a,
        json={"events": [_event(event_id="ev-a-61")]},
    )
    assert res_61.status_code == 429

    headers_b = _bearer("shop-b")
    res_b = await client.post(
        "/api/v1/events",
        headers=headers_b,
        json={"events": [_event(event_id="ev-b-1")]},
    )
    assert res_b.status_code == 200


@pytest.mark.asyncio
async def test_redis_failure_accepts_upload(client, db_sessionmaker):
    async with db_sessionmaker() as s:
        s.add(ShopProfile(id="shop-a", shop_name="Shop A"))
        await s.commit()

    class _DownRedis:
        async def incr(self, *args, **kwargs):
            raise ConnectionError("redis down")

        async def expire(self, *args, **kwargs):
            raise ConnectionError("redis down")

    app.dependency_overrides[get_redis] = lambda: _DownRedis()

    res = await client.post(
        "/api/v1/events",
        headers=_bearer("shop-a"),
        json={"events": [_event(event_id="ev-down-1")]},
    )
    assert res.status_code == 200
    assert res.json() == {"accepted": 1}

    async with db_sessionmaker() as s:
        stored = (await s.execute(select(AnalyticsEvent).where(AnalyticsEvent.id == "ev-down-1"))).scalars().first()
    assert stored is not None
    assert stored.shop_id == "shop-a"


@pytest.mark.asyncio
async def test_missing_shop_returns_409(client, db_sessionmaker):
    res = await client.post(
        "/api/v1/events",
        headers=_bearer("shop-missing"),
        json={"events": [_event(event_id="ev-missing-1")]},
    )
    assert res.status_code == 409
    assert res.json() == {"detail": "Shop not found"}

    async with db_sessionmaker() as s:
        rows = (await s.execute(select(AnalyticsEvent))).scalars().all()
    assert len(rows) == 0


@pytest.mark.asyncio
async def test_rate_limit_key_expires_within_window(client, db_sessionmaker):
    async with db_sessionmaker() as s:
        s.add(ShopProfile(id="shop-a", shop_name="Shop A"))
        await s.commit()

    fake = fakeredis.aioredis.FakeRedis(decode_responses=True)
    app.dependency_overrides[get_redis] = lambda: fake

    res = await client.post(
        "/api/v1/events",
        headers=_bearer("shop-a"),
        json={"events": [_event(event_id="ev-ttl-1")]},
    )
    assert res.status_code == 200

    ttl = await fake.ttl("events_rl:shop-a")
    assert 0 < ttl <= 3600
