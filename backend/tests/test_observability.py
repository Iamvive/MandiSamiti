import json
import logging
import pytest
import fakeredis.aioredis
from sqlalchemy.ext.asyncio import create_async_engine, async_sessionmaker, AsyncSession
from app.main import app
from app.database import get_db
from app.core.redis_client import get_redis
from app.core import observability as obs


# --- /health/ready ---

@pytest.mark.asyncio
async def test_ready_ok(client):
    r = await client.get("/health/ready")
    assert r.status_code == 200
    assert r.json() == {"status": "ready", "checks": {"db": "ok", "redis": "ok"}}


@pytest.mark.asyncio
async def test_ready_503_when_redis_down(client):
    class _DownRedis:
        async def ping(self):
            raise ConnectionError("redis down")
    app.dependency_overrides[get_redis] = lambda: _DownRedis()
    r = await client.get("/health/ready")
    assert r.status_code == 503
    assert r.json()["checks"] == {"db": "ok", "redis": "fail"}


@pytest.mark.asyncio
async def test_ready_503_when_db_down(client):
    bad = create_async_engine("sqlite+aiosqlite:////nonexistent-dir/x.db")
    maker = async_sessionmaker(bad, class_=AsyncSession)

    async def _bad_db():
        async with maker() as s:
            yield s
    app.dependency_overrides[get_db] = _bad_db
    r = await client.get("/health/ready")
    assert r.status_code == 503
    assert r.json()["checks"] == {"db": "fail", "redis": "ok"}
    await bad.dispose()


# --- notify_ops ---

class _FakeHttp:
    def __init__(self):
        self.posts = []

    async def post(self, url, json):
        self.posts.append((url, json))


@pytest.mark.asyncio
async def test_notify_ops_noop_when_unset(monkeypatch):
    monkeypatch.setattr(obs.settings, "OPS_TELEGRAM_BOT_TOKEN", "")
    http = _FakeHttp()
    sent = await obs.notify_ops("boom", redis=fakeredis.aioredis.FakeRedis(), http=http)
    assert sent is False and http.posts == []


@pytest.mark.asyncio
async def test_notify_ops_posts_once_then_dedupes(monkeypatch):
    monkeypatch.setattr(obs.settings, "OPS_TELEGRAM_BOT_TOKEN", "tok")
    monkeypatch.setattr(obs.settings, "OPS_TELEGRAM_CHAT_ID", "42")
    redis, http = fakeredis.aioredis.FakeRedis(), _FakeHttp()
    assert await obs.notify_ops("boom", dedupe_key="E1", redis=redis, http=http) is True
    assert await obs.notify_ops("boom again", dedupe_key="E1", redis=redis, http=http) is False
    assert await obs.notify_ops("other", dedupe_key="E2", redis=redis, http=http) is True
    assert len(http.posts) == 2
    url, body = http.posts[0]
    assert url == "https://api.telegram.org/bottok/sendMessage"
    assert body["chat_id"] == "42" and "boom" in body["text"]
    assert 0 < await redis.ttl("ops_alert:E1") <= 600


# --- Sentry ---

def test_sentry_not_initialised_without_dsn(monkeypatch):
    monkeypatch.setattr(obs.settings, "SENTRY_DSN", "")
    assert obs.init_sentry() is False


def test_scrub_strips_phone_numbers():
    event = {
        "message": "login failed for 9876543210",
        "extra": {"phone": "+919876543210", "note": "call 98765 43210"},
        "request": {"data": {"phone": "9876543210", "mpin": "1234"}},
    }
    out = json.dumps(obs.scrub_event(event, None))
    assert "9876543210" not in out and "98765 43210" not in out
    assert "1234" not in out  # request bodies are dropped entirely


# --- JSON logs ---

def test_json_log_line_carries_request_id():
    token = obs.request_id_var.set("req-abc")
    try:
        rec = logging.LogRecord("x", logging.INFO, __file__, 1, "hello %s", ("world",), None)
        rec.status = 200
        line = json.loads(obs.JsonFormatter().format(rec))
    finally:
        obs.request_id_var.reset(token)
    assert line["msg"] == "hello world"
    assert line["level"] == "INFO"
    assert line["request_id"] == "req-abc"
    assert line["status"] == 200


@pytest.mark.asyncio
async def test_response_has_request_id_header(client):
    r = await client.get("/health")
    assert r.headers.get("x-request-id")
