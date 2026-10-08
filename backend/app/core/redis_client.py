import redis.asyncio as aioredis
from app.config import settings

_client = aioredis.from_url(settings.REDIS_URL, decode_responses=True)


def get_redis() -> aioredis.Redis:
    return _client
