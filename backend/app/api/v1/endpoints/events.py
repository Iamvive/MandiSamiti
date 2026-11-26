import time
from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.dialects.postgresql import insert as pg_insert
from sqlalchemy.dialects.sqlite import insert as sqlite_insert
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.redis_client import get_redis
from app.core.security import get_current_user_payload
from app.database import get_db
from app.models.analytics_event import AnalyticsEvent
from app.models.user import ShopProfile
from app.schemas.events import EventsBatchRequest, EventsBatchResponse

router = APIRouter()

EVENTS_RATE_LIMIT = 60
EVENTS_RATE_WINDOW_S = 3600


@router.post("", response_model=EventsBatchResponse, summary="Batch ingest app telemetry events")
async def ingest_events(
    payload: EventsBatchRequest,
    current_user: dict = Depends(get_current_user_payload),
    db: AsyncSession = Depends(get_db),
    redis=Depends(get_redis),
):
    shop_id = current_user.get("shop_id")
    if not shop_id:
        raise HTTPException(status_code=400, detail="User is not associated with a Shop")

    key = f"events_rl:{shop_id}"
    try:
        count = await redis.incr(key)
        await redis.expire(key, EVENTS_RATE_WINDOW_S, nx=True)
    except Exception:
        count = None

    if count is not None and count > EVENTS_RATE_LIMIT:
        raise HTTPException(status_code=429, detail="Rate limit exceeded")

    shop = await db.get(ShopProfile, shop_id)
    if not shop:
        raise HTTPException(status_code=409, detail="Shop not found")

    if not payload.events:
        return EventsBatchResponse(accepted=0)

    now_ms = int(time.time() * 1000)
    seen_ids: set[str] = set()
    rows: list[dict] = []
    for ev in payload.events:
        if ev.id in seen_ids:
            continue
        seen_ids.add(ev.id)
        rows.append(
            {
                "shop_id": shop_id,
                "id": ev.id,
                "name": ev.name,
                "screen": ev.screen,
                "session_id": ev.session_id,
                "occurred_at_ms": ev.occurred_at_ms,
                "duration_ms": ev.duration_ms,
                "app_version": ev.app_version,
                "received_at_ms": now_ms,
            }
        )

    bind = db.get_bind()
    dialect_name = bind.dialect.name if bind is not None else "postgresql"
    insert_fn = pg_insert if dialect_name == "postgresql" else sqlite_insert
    stmt = insert_fn(AnalyticsEvent).values(rows).on_conflict_do_nothing(index_elements=["shop_id", "id"])
    await db.execute(stmt)
    await db.commit()
    return EventsBatchResponse(accepted=len(payload.events))
