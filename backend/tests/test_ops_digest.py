import os
import pytest
from sqlalchemy import select
from app.models.analytics_event import AnalyticsEvent
from app.models.user import ShopProfile
from app.models.party import Party
from app.models.deal import Deal
from app.models.transaction import CashTransaction
from app.models.sync_conflict import SyncConflict
from scripts import ops_digest

DAY = 86_400_000
NOW = 1_760_000_000_000  # fixed "now" in ms


async def _seed(session):
    session.add_all([
        ShopProfile(id="s1", shop_name="Sharma Traders"),
        ShopProfile(id="s2", shop_name="Gupta Aadhat"),
        ShopProfile(id="s3", shop_name="New Shop"),  # never entered anything: not "silent"
        Party(id="p1", shop_id="s1", name="Ram", role="FARMER"),
        Party(id="p2", shop_id="s2", name="Shyam", role="FARMER"),
        Deal(id="d1", shop_id="s1", farmer_id="p1", commodity="wheat", created_at=NOW - 1000),
        Deal(id="d2", shop_id="s1", farmer_id="p1", commodity="wheat", created_at=NOW - 2000),
        CashTransaction(id="c1", shop_id="s1", transaction_type="JAMA_RECEIVED", amount_paisa=100, created_at=NOW - 3000),
        Deal(id="d3", shop_id="s2", farmer_id="p2", commodity="chana", created_at=NOW - 3 * DAY),
        SyncConflict(shop_id="s1", entry_id="d1", revision=2, snapshot_json="{}", received_at=NOW - 5000),
        SyncConflict(shop_id="s2", entry_id="d3", revision=2, snapshot_json="{}", received_at=NOW - 2 * DAY),
    ])
    await session.commit()


@pytest.mark.asyncio
async def test_collect_counts(db_sessionmaker):
    async with db_sessionmaker() as s:
        await _seed(s)
        stats = await ops_digest.collect(s, now_ms=NOW)
    assert stats["entries_by_shop"] == {"Sharma Traders": 3}
    assert stats["silent_shops"] == ["Gupta Aadhat"]
    assert stats["conflicts_24h"] == 1


def _dump(path, size, age_h, now):
    path.write_bytes(b"x" * size)
    os.utime(path, (now - age_h * 3600, now - age_h * 3600))


def test_backup_ok(tmp_path):
    now = 1_760_000_000
    _dump(tmp_path / "a.sql.gz", 1000, 25, now)
    _dump(tmp_path / "b.sql.gz", 1000, 1, now)
    assert ops_digest.check_backup(str(tmp_path), now=now) is None


def test_backup_stale(tmp_path):
    now = 1_760_000_000
    _dump(tmp_path / "a.sql.gz", 1000, 30, now)
    assert "older than 26h" in ops_digest.check_backup(str(tmp_path), now=now)


def test_backup_shrunk(tmp_path):
    now = 1_760_000_000
    _dump(tmp_path / "a.sql.gz", 1000, 25, now)
    _dump(tmp_path / "b.sql.gz", 800, 1, now)
    assert "shrank" in ops_digest.check_backup(str(tmp_path), now=now)


def test_backup_missing(tmp_path):
    assert "no backup" in ops_digest.check_backup(str(tmp_path), now=1_760_000_000)


def test_backup_not_configured():
    assert "not configured" in ops_digest.check_backup("", now=1_760_000_000)


def test_format_digest():
    text = ops_digest.format_digest(
        {"entries_by_shop": {"Sharma Traders": 3}, "silent_shops": ["Gupta Aadhat"], "conflicts_24h": 1},
        backup_problem=None,
    )
    assert "Active shops today: 1" in text
    assert "Sharma Traders: 3" in text
    assert "Silent 2+ days: Gupta Aadhat" in text
    assert "Sync conflicts (24h): 1" in text
    assert "App events today: none" in text
    assert "Backup: ok" in text


def _ev(shop_id: str, ev_id: str, name: str, screen: str | None = None, session_id: str = "sess-1", received_at_ms: int = NOW - 1000) -> AnalyticsEvent:
    return AnalyticsEvent(
        shop_id=shop_id,
        id=ev_id,
        name=name,
        screen=screen,
        session_id=session_id,
        occurred_at_ms=received_at_ms - 500,
        duration_ms=None,
        app_version="1.0",
        received_at_ms=received_at_ms,
    )


@pytest.mark.asyncio
async def test_collect_telemetry_counts(db_sessionmaker):
    async with db_sessionmaker() as s:
        await _seed(s)
        s.add_all([
            # Sharma Traders: 2 sessions, 2 *_saved (buy_saved, sell_saved); unsaved should NOT match *_saved
            _ev("s1", "a1", "app_open", session_id="sess-1"),
            _ev("s1", "a2", "buy_saved", session_id="sess-1"),
            _ev("s1", "a3", "sell_saved", session_id="sess-2"),
            _ev("s1", "a4", "unsaved", session_id="sess-2"),
            _ev("s1", "a5", "sync_ok", session_id="sess-2"),
            # Gupta Aadhat: 1 session, 1 *_saved, 1 sync_failed
            _ev("s2", "b1", "expense_saved", session_id="sess-g1"),
            _ev("s2", "b2", "sync_failed", session_id="sess-g1"),
            # 6 distinct screens viewed today; top 5 should exclude the least viewed one (6th)
            _ev("s1", "sv1", "screen_view", screen="home"),
            _ev("s1", "sv2", "screen_view", screen="home"),
            _ev("s1", "sv3", "screen_view", screen="home"),
            _ev("s1", "sv4", "screen_view", screen="buy_list"),
            _ev("s1", "sv5", "screen_view", screen="buy_list"),
            _ev("s1", "sv6", "screen_view", screen="sell_list"),
            _ev("s1", "sv7", "screen_view", screen="sell_list"),
            _ev("s2", "sv8", "screen_view", screen="parchi", session_id="sess-g1"),
            _ev("s2", "sv9", "screen_view", screen="parchi", session_id="sess-g1"),
            _ev("s2", "sv10", "screen_view", screen="udhaar", session_id="sess-g1"),
            _ev("s2", "sv11", "screen_view", screen="udhaar", session_id="sess-g1"),
            _ev("s2", "sv12", "screen_view", screen="stock", session_id="sess-g1"),
            # Yesterday's event (excluded from today's window)
            _ev("s1", "old1", "sync_ok", session_id="sess-old", received_at_ms=NOW - 2 * DAY),
            _ev("s1", "old2", "buy_saved", session_id="sess-old", received_at_ms=NOW - 2 * DAY),
        ])
        await s.commit()
        stats = await ops_digest.collect(s, now_ms=NOW)

    assert stats["telemetry_by_shop"] == {
        "Sharma Traders": {"sessions": 2, "saved": 2},
        "Gupta Aadhat": {"sessions": 1, "saved": 1},
    }
    assert stats["sync_ok"] == 1
    assert stats["sync_failed"] == 1
    assert len(stats["top_screens"]) == 5
    assert stats["top_screens"][0] == ("home", 3)
    assert ("stock", 1) not in stats["top_screens"]

    text = ops_digest.format_digest(stats, backup_problem=None)
    assert "App events today: sync_ok=1, sync_failed=1" in text
    assert "Sharma Traders: 2 sessions, 2 saved" in text
    assert "Gupta Aadhat: 1 sessions, 1 saved" in text
    assert "Top screens: home=3" in text


@pytest.mark.asyncio
async def test_purge_old_events_keeps_recent_rows(db_sessionmaker, monkeypatch):
    now = 1_760_000_000_000
    async with db_sessionmaker() as s:
        s.add(ShopProfile(id="s1", shop_name="Sharma Traders"))
        s.add_all([
            _ev("s1", "expired", "app_open", received_at_ms=now - 90 * DAY - 1),
            _ev("s1", "boundary", "app_open", received_at_ms=now - 90 * DAY),
            _ev("s1", "recent", "app_open", received_at_ms=now - 1000),
        ])
        await s.commit()

    monkeypatch.setattr(ops_digest, "AsyncSessionLocal", db_sessionmaker)
    monkeypatch.setattr(ops_digest.time, "time", lambda: now / 1000)

    async def _noop_notify(_text):
        return False

    monkeypatch.setattr(ops_digest, "notify_ops", _noop_notify)
    await ops_digest.main()

    async with db_sessionmaker() as s:
        remaining = (await s.execute(select(AnalyticsEvent.id).order_by(AnalyticsEvent.id))).scalars().all()
    assert remaining == ["boundary", "recent"]
