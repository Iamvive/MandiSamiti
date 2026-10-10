import redis.asyncio as aioredis
from app.config import settings

_fake_server = None
_client = None


def get_redis() -> aioredis.Redis:
    global _client, _fake_server
    if _client is not None:
        return _client

    if settings.APP_ENV.strip().lower() in ("prod", "production"):
        _client = aioredis.from_url(settings.REDIS_URL, decode_responses=True)
        return _client

    try:
        import fakeredis
        import fakeredis.aioredis
        if _fake_server is None:
            _fake_server = fakeredis.FakeServer()
        _client = fakeredis.aioredis.FakeRedis(server=_fake_server, decode_responses=True)
        return _client
    except Exception:
        _client = aioredis.from_url(settings.REDIS_URL, decode_responses=True)
        return _client
