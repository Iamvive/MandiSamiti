"""Daily ops digest sent to the ops Telegram chat via notify_ops(). Phase B of the observability spec.

Run on the VPS by cron at 21:00 IST (server clock in UTC):
    30 15 * * * cd /opt/mandisamiti-prod && docker compose -f docker-compose.prod.yml exec -T api python -m scripts.ops_digest

BACKUP_DIR (env) is the folder holding the off-site pg_dump files; unset means the check reports "not configured".
Sync lag is left out until deals/cash_transactions get a server-side received timestamp.
"""
import asyncio
import os
import time
from datetime import datetime, timedelta, timezone

from sqlalchemy import case, delete, func, select, union_all

from app.core.observability import notify_ops
from app.database import AsyncSessionLocal
from app.models.analytics_event import AnalyticsEvent
from app.models.deal import Deal
from app.models.sync_conflict import SyncConflict
from app.models.transaction import CashTransaction
from app.models.user import ShopProfile

IST = timezone(timedelta(hours=5, minutes=30))
DAY_MS = 86_400_000
SILENT_AFTER_MS = 2 * DAY_MS
RETENTION_MS = 90 * DAY_MS
BACKUP_MAX_AGE_S = 26 * 3600
BACKUP_MIN_RATIO = 0.9


def _ist_midnight_ms(now_ms: int) -> int:
    day = datetime.fromtimestamp(now_ms / 1000, IST).replace(hour=0, minute=0, second=0, microsecond=0)
    return int(day.timestamp() * 1000)


async def collect(session, now_ms: int) -> dict:
    day_start = _ist_midnight_ms(now_ms)
    entries = union_all(
        select(Deal.shop_id, Deal.created_at),
        select(CashTransaction.shop_id, CashTransaction.created_at),
    ).subquery()

    today = await session.execute(
        select(ShopProfile.shop_name, func.count())
        .join(entries, entries.c.shop_id == ShopProfile.id)
        .where(entries.c.created_at >= day_start)
        .group_by(ShopProfile.shop_name)
    )
    last = await session.execute(
        select(ShopProfile.shop_name)
        .join(entries, entries.c.shop_id == ShopProfile.id)
        .group_by(ShopProfile.id, ShopProfile.shop_name)
        .having(func.max(entries.c.created_at) < now_ms - SILENT_AFTER_MS)
    )
    conflicts = await session.scalar(
        select(func.count()).select_from(SyncConflict).where(SyncConflict.received_at >= now_ms - DAY_MS)
    )

    shop_label = func.coalesce(ShopProfile.shop_name, AnalyticsEvent.shop_id)
    shop_events = await session.execute(
        select(
            shop_label,
            func.count(func.distinct(AnalyticsEvent.session_id)),
            func.coalesce(func.sum(case((AnalyticsEvent.name.like("%/_saved", escape="/"), 1), else_=0)), 0),
            func.count(),
        )
        .outerjoin(ShopProfile, ShopProfile.id == AnalyticsEvent.shop_id)
        .where(AnalyticsEvent.received_at_ms >= day_start)
        .group_by(AnalyticsEvent.shop_id, ShopProfile.shop_name)
    )
    telemetry_by_shop: dict[str, dict[str, int]] = {}
    events_total = 0
    for name, sessions, saved, total in shop_events.all():
        entry = telemetry_by_shop.setdefault(name, {"sessions": 0, "saved": 0})
        entry["sessions"] += int(sessions or 0)
        entry["saved"] += int(saved or 0)
        events_total += int(total or 0)

    screen_rows = await session.execute(
        select(AnalyticsEvent.screen, func.count())
        .where(
            AnalyticsEvent.received_at_ms >= day_start,
            AnalyticsEvent.name == "screen_view",
            AnalyticsEvent.screen.isnot(None),
        )
        .group_by(AnalyticsEvent.screen)
        .order_by(func.count().desc(), AnalyticsEvent.screen.asc())
        .limit(5)
    )
    top_screens = [(screen, int(cnt)) for screen, cnt in screen_rows.all()]

    sync_counts = await session.execute(
        select(
            func.coalesce(func.sum(case((AnalyticsEvent.name == "sync_ok", 1), else_=0)), 0),
            func.coalesce(func.sum(case((AnalyticsEvent.name == "sync_failed", 1), else_=0)), 0),
        ).where(AnalyticsEvent.received_at_ms >= day_start)
    )
    sync_ok, sync_failed = sync_counts.one()

    return {
        "entries_by_shop": dict(today.all()),
        "silent_shops": sorted(r[0] for r in last.all()),
        "conflicts_24h": conflicts,
        "events_total": events_total,
        "telemetry_by_shop": telemetry_by_shop,
        "top_screens": top_screens,
        "sync_ok": int(sync_ok or 0),
        "sync_failed": int(sync_failed or 0),
    }


async def purge_old_events(session, now_ms: int) -> int:
    cutoff = now_ms - RETENTION_MS
    result = await session.execute(
        delete(AnalyticsEvent).where(AnalyticsEvent.received_at_ms < cutoff)
    )
    await session.commit()
    return result.rowcount or 0


def check_backup(backup_dir: str, now: float) -> str | None:
    """None when the newest dump is fresh and not smaller than the one before; otherwise the problem."""
    if not backup_dir:
        return "not configured (set BACKUP_DIR)"
    try:
        dumps = sorted((e for e in os.scandir(backup_dir) if e.is_file()), key=lambda e: e.stat().st_mtime)
    except OSError as e:
        return f"cannot read {backup_dir}: {type(e).__name__}"
    if not dumps:
        return f"no backup files in {backup_dir}"
    newest = dumps[-1].stat()
    age_h = (now - newest.st_mtime) / 3600
    if age_h * 3600 > BACKUP_MAX_AGE_S:
        return f"newest dump is older than 26h ({age_h:.0f}h)"
    if len(dumps) > 1 and newest.st_size < dumps[-2].stat().st_size * BACKUP_MIN_RATIO:
        return f"newest dump shrank by more than 10% ({dumps[-2].stat().st_size} -> {newest.st_size} bytes)"
    return None


def format_digest(stats: dict, backup_problem: str | None) -> str:
    lines = ["MandiSamiti daily digest", f"Active shops today: {len(stats['entries_by_shop'])}"]
    lines += [f"  {name}: {n}" for name, n in sorted(stats["entries_by_shop"].items(), key=lambda kv: -kv[1])]
    lines.append(f"Silent 2+ days: {', '.join(stats['silent_shops']) or 'none'}")
    lines.append(f"Sync conflicts (24h): {stats['conflicts_24h']}")
    telemetry_by_shop = stats.get("telemetry_by_shop") or {}
    top_screens = stats.get("top_screens") or []
    sync_ok = stats.get("sync_ok", 0)
    sync_failed = stats.get("sync_failed", 0)
    events_total = stats.get("events_total", 0)
    if not (events_total or telemetry_by_shop or top_screens or sync_ok or sync_failed):
        lines.append("App events today: none")
    else:
        lines.append(f"App events today: sync_ok={sync_ok}, sync_failed={sync_failed}")
        for name, counts in sorted(
            telemetry_by_shop.items(),
            key=lambda kv: (-kv[1].get("sessions", 0), -kv[1].get("saved", 0), kv[0]),
        ):
            lines.append(f"  {name}: {counts.get('sessions', 0)} sessions, {counts.get('saved', 0)} saved")
        screens_str = ", ".join(f"{screen}={cnt}" for screen, cnt in top_screens) or "none"
        lines.append(f"Top screens: {screens_str}")
    lines.append(f"Backup: {backup_problem or 'ok'}")
    return "\n".join(lines)


async def main() -> None:
    now_ms = int(time.time() * 1000)
    async with AsyncSessionLocal() as session:
        stats = await collect(session, now_ms=now_ms)
        await purge_old_events(session, now_ms=now_ms)
    text = format_digest(stats, check_backup(os.environ.get("BACKUP_DIR", ""), now=time.time()))
    print(text)
    await notify_ops(text)


if __name__ == "__main__":
    asyncio.run(main())
