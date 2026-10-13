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

from sqlalchemy import func, select, union_all

from app.core.observability import notify_ops
from app.database import AsyncSessionLocal
from app.models.deal import Deal
from app.models.sync_conflict import SyncConflict
from app.models.transaction import CashTransaction
from app.models.user import ShopProfile

IST = timezone(timedelta(hours=5, minutes=30))
DAY_MS = 86_400_000
SILENT_AFTER_MS = 2 * DAY_MS
BACKUP_MAX_AGE_S = 26 * 3600
BACKUP_MIN_RATIO = 0.9


def _ist_midnight_ms(now_ms: int) -> int:
    day = datetime.fromtimestamp(now_ms / 1000, IST).replace(hour=0, minute=0, second=0, microsecond=0)
    return int(day.timestamp() * 1000)


async def collect(session, now_ms: int) -> dict:
    entries = union_all(
        select(Deal.shop_id, Deal.created_at),
        select(CashTransaction.shop_id, CashTransaction.created_at),
    ).subquery()

    today = await session.execute(
        select(ShopProfile.shop_name, func.count())
        .join(entries, entries.c.shop_id == ShopProfile.id)
        .where(entries.c.created_at >= _ist_midnight_ms(now_ms))
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
    return {
        "entries_by_shop": dict(today.all()),
        "silent_shops": sorted(r[0] for r in last.all()),
        "conflicts_24h": conflicts,
    }


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
    lines = [f"MandiSamiti daily digest", f"Active shops today: {len(stats['entries_by_shop'])}"]
    lines += [f"  {name}: {n}" for name, n in sorted(stats["entries_by_shop"].items(), key=lambda kv: -kv[1])]
    lines.append(f"Silent 2+ days: {', '.join(stats['silent_shops']) or 'none'}")
    lines.append(f"Sync conflicts (24h): {stats['conflicts_24h']}")
    lines.append(f"Backup: {backup_problem or 'ok'}")
    return "\n".join(lines)


async def main() -> None:
    async with AsyncSessionLocal() as session:
        stats = await collect(session, now_ms=int(time.time() * 1000))
    text = format_digest(stats, check_backup(os.environ.get("BACKUP_DIR", ""), now=time.time()))
    print(text)
    await notify_ops(text)


if __name__ == "__main__":
    asyncio.run(main())
