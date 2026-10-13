import os
import pytest
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
    assert "Backup: ok" in text
