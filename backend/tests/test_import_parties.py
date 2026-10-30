import csv
import os
import stat
import subprocess
import sys
from pathlib import Path

import pytest
import pytest_asyncio
from sqlalchemy import func, select
from sqlalchemy.ext.asyncio import AsyncSession, async_sessionmaker, create_async_engine

from app.api.v1.endpoints.sync import sync_pull
from app.database import Base
from app.importers import party_import
from app.importers.party_import import (
    ImportPlan,
    PartyCandidate,
    PartyImportError,
    ReviewIssue,
    SOURCE_BUYERS,
    SOURCE_FARMERS,
    SOURCE_GST,
    SOURCE_OCTOBER,
    apply_import,
    build_import_plan,
    merge_candidates,
    normalize_gstin,
    normalize_phone,
    parse_csv,
    write_review_csv,
)
from app.models.party import Party
from app.models.user import ShopProfile


def write_csv(path: Path, headers: list[str], rows: list[list[str]], encoding: str = "utf-8") -> Path:
    with path.open("w", encoding=encoding, newline="") as handle:
        writer = csv.writer(handle)
        writer.writerow(headers)
        writer.writerows(rows)
    return path


def candidate(
    name: str,
    *,
    role: str = "BUYER",
    source: str = SOURCE_BUYERS,
    row: int = 2,
    phone: str | None = None,
    village: str | None = None,
    gstin: str | None = None,
    licence: str | None = None,
) -> PartyCandidate:
    return PartyCandidate(
        source=source,
        row_number=row,
        role=role,
        name=name,
        normalized_name=party_import.normalized_text(name),
        phone=phone,
        village=village,
        gstin=gstin,
        mandi_license=licence,
    )


def test_phone_and_gstin_normalization_rejects_placeholders_and_guesses():
    assert normalize_phone("+91 98765-43210") == "9876543210"
    assert normalize_phone("1000000000") is None
    assert normalize_phone("123") is None
    assert normalize_gstin(" 29abcde1234f1z5 ") == "29ABCDE1234F1Z5"
    assert normalize_gstin("29ABCDE1234F1Z?") is None


def test_utf8_bom_and_hindi_headers_are_parsed(tmp_path):
    path = write_csv(
        tmp_path / "farmers.csv",
        ["किसान नाम", "मोबाइल", "गाँव"],
        [["  Test Farmer  ", "1000000000", " Test Village "]],
        encoding="utf-8-sig",
    )

    rows, reviews, skipped = parse_csv(path, SOURCE_FARMERS, tmp_path)

    assert skipped == 0
    assert rows[0].name == "Test Farmer"
    assert rows[0].village == "Test Village"
    assert rows[0].phone is None
    assert [review.code for review in reviews] == ["invalid_phone"]


def test_missing_and_ambiguous_headers_stop_without_row_values(tmp_path):
    missing = write_csv(tmp_path / "missing.csv", ["Phone"], [["9876543210"]])
    with pytest.raises(PartyImportError, match="missing_header: name"):
        parse_csv(missing, SOURCE_FARMERS, tmp_path)

    ambiguous = write_csv(tmp_path / "ambiguous.csv", ["Name", "Farmer"], [["A", "B"]])
    with pytest.raises(PartyImportError, match="ambiguous_header: name"):
        parse_csv(ambiguous, SOURCE_FARMERS, tmp_path)


def test_path_size_row_and_encoding_boundaries(tmp_path, monkeypatch):
    outside = write_csv(tmp_path.parent / "outside.csv", ["Name"], [["Outside"]])
    with pytest.raises(PartyImportError, match="path_outside_import_root"):
        parse_csv(outside, SOURCE_FARMERS, tmp_path)

    target = write_csv(tmp_path / "target.csv", ["Name"], [["Target"]])
    link = tmp_path / "link.csv"
    link.symlink_to(target)
    with pytest.raises(PartyImportError, match="symlink_rejected"):
        parse_csv(link, SOURCE_FARMERS, tmp_path)

    monkeypatch.setattr(party_import, "MAX_FILE_BYTES", 1)
    with pytest.raises(PartyImportError, match="file_too_large"):
        parse_csv(target, SOURCE_FARMERS, tmp_path)
    monkeypatch.setattr(party_import, "MAX_FILE_BYTES", 10 * 1024 * 1024)

    monkeypatch.setattr(party_import, "MAX_DATA_ROWS", 1)
    two_rows = write_csv(tmp_path / "two.csv", ["Name"], [["One"], ["Two"]])
    with pytest.raises(PartyImportError, match="row_limit_exceeded"):
        parse_csv(two_rows, SOURCE_FARMERS, tmp_path)

    bad = tmp_path / "bad.csv"
    bad.write_bytes(b"Name\n\xff\xfe")
    with pytest.raises(PartyImportError, match="bad_encoding"):
        parse_csv(bad, SOURCE_FARMERS, tmp_path)


def test_merge_keeps_villages_separate_and_one_owner_per_phone():
    plan = merge_candidates([
        candidate("Farmer One", role="FARMER", source=SOURCE_FARMERS, phone="9876543210", village="North"),
        candidate("Farmer Two", role="FARMER", source=SOURCE_FARMERS, row=3, phone="9876543210", village="South"),
        candidate("Same Name", role="FARMER", source=SOURCE_FARMERS, row=4, village="East"),
        candidate("Same Name", role="FARMER", source=SOURCE_FARMERS, row=5, village="West"),
    ])

    assert len(plan.candidates) == 4
    assert plan.candidates[0].phone == "9876543210"
    assert plan.candidates[1].phone is None
    assert [review.code for review in plan.reviews] == ["duplicate_phone"]


def test_buyer_sources_merge_by_name_then_gstin_with_priority():
    plan = merge_candidates([
        candidate("Buyer One", source=SOURCE_GST, gstin="29ABCDE1234F1Z5"),
        candidate("Buyer One", source=SOURCE_OCTOBER),
        candidate("Buyer One", source=SOURCE_BUYERS, licence="LIC-HIGH"),
    ])

    assert len(plan.candidates) == 1
    assert plan.candidates[0].gstin == "29ABCDE1234F1Z5"
    assert plan.candidates[0].mandi_license == "LIC-HIGH"


def test_build_plan_reviews_invalid_gstin_without_storing_it(tmp_path):
    buyer = write_csv(
        tmp_path / "buyers.csv",
        ["Buyer Name", "GSTIN", "Mandi Licence"],
        [["Buyer Invalid", "invalid", "LIC-002"]],
    )

    plan = build_import_plan({SOURCE_BUYERS: buyer}, tmp_path)

    assert plan.buyer_count == 1
    assert plan.candidates[0].gstin is None
    assert [review.code for review in plan.reviews] == ["invalid_gstin"]


def test_review_csv_is_private_and_formula_safe(tmp_path):
    plan = ImportPlan(reviews=[ReviewIssue(SOURCE_BUYERS, 2, "invalid_gstin", ("=1+1",))])
    destination = tmp_path / "review.csv"

    written = write_review_csv(plan, tmp_path, destination)

    assert stat.S_IMODE(written.stat().st_mode) == 0o600
    assert "'=1+1" in written.read_text(encoding="utf-8")


def test_cli_dry_run_prints_counts_without_personal_fields(tmp_path):
    farmers = write_csv(
        tmp_path / "farmers.csv",
        ["Name", "Phone", "Village"],
        [["Private Test Name", "9876543210", "Private Test Village"]],
    )

    result = subprocess.run(
        [
            sys.executable,
            "scripts/import_parties.py",
            "--farmers",
            str(farmers),
            "--import-root",
            str(tmp_path),
        ],
        cwd=Path(__file__).parents[1],
        capture_output=True,
        text=True,
        check=False,
    )

    assert result.returncode == 0
    assert "status=dry_run" in result.stdout
    assert "farmer_candidates=1" in result.stdout
    assert "Private Test Name" not in result.stdout + result.stderr
    assert "9876543210" not in result.stdout + result.stderr
    assert "Private Test Village" not in result.stdout + result.stderr


@pytest_asyncio.fixture
async def session_factory():
    engine = create_async_engine("sqlite+aiosqlite:///:memory:")
    async with engine.begin() as connection:
        await connection.run_sync(Base.metadata.create_all)
    factory = async_sessionmaker(engine, class_=AsyncSession, expire_on_commit=False)
    async with factory() as session:
        session.add(ShopProfile(id="shop-import", shop_name="Import Shop", phone_number="9000000001"))
        await session.commit()
    yield factory
    await engine.dispose()


@pytest.mark.asyncio
async def test_apply_is_atomic_idempotent_and_visible_to_fresh_pull(session_factory):
    plan = ImportPlan(candidates=[
        candidate("Buyer Import", gstin="29ABCDE1234F1Z5", licence="LIC-003"),
        candidate("Farmer Import", role="FARMER", source=SOURCE_FARMERS, phone="9876543210", village="Village"),
    ])
    async with session_factory() as session:
        first = await apply_import(session, "shop-import", plan)
    async with session_factory() as session:
        second = await apply_import(session, "shop-import", plan)
    async with session_factory() as session:
        count = await session.scalar(select(func.count()).select_from(Party))
        page = await sync_pull(
            after_seq=0,
            limit=500,
            current_user={"shop_id": "shop-import", "user_id": "test"},
            db=session,
        )

    assert first.new_parties == 2 and first.existing_parties == 0
    assert second.new_parties == 0 and second.existing_parties == 2
    assert count == 2
    pulled = next(item for item in page.parties if item.role == "BUYER")
    assert pulled.gstin == "29ABCDE1234F1Z5"
    assert pulled.mandi_license == "LIC-003"


@pytest.mark.asyncio
async def test_existing_party_is_never_overwritten(session_factory):
    async with session_factory() as session:
        async with session.begin():
            session.add(Party(
                id="manual-party",
                shop_id="shop-import",
                name="Buyer Existing",
                role="BUYER",
                gstin="29ABCDE1234F1Z5",
                mandi_license="MANUAL",
                created_at=1,
                updated_at=2,
            ))
    plan = ImportPlan(candidates=[
        candidate("Buyer Existing Changed", gstin="29ABCDE1234F1Z5", licence="IMPORT"),
    ])

    async with session_factory() as session:
        result = await apply_import(session, "shop-import", plan)
    async with session_factory() as session:
        row = (await session.execute(select(Party).where(Party.id == "manual-party"))).scalars().one()

    assert result.new_parties == 0 and result.existing_parties == 1
    assert row.name == "Buyer Existing"
    assert row.mandi_license == "MANUAL"


@pytest.mark.asyncio
async def test_valid_import_gstin_matches_existing_buyer_without_gstin_by_name(session_factory):
    async with session_factory() as session:
        async with session.begin():
            session.add(Party(
                id="manual-no-gstin",
                shop_id="shop-import",
                name="Buyer No Gstin",
                role="BUYER",
                mandi_license="MANUAL",
                created_at=1,
                updated_at=2,
            ))
    plan = ImportPlan(candidates=[
        candidate("Buyer No Gstin", gstin="29ABCDE1234F1Z5", licence="IMPORT"),
    ])

    async with session_factory() as session:
        result = await apply_import(session, "shop-import", plan)
    async with session_factory() as session:
        count = await session.scalar(select(func.count()).select_from(Party))
        row = await session.get(Party, "manual-no-gstin")

    assert result.new_parties == 0 and result.existing_parties == 1
    assert count == 1
    assert row.gstin is None
    assert row.mandi_license == "MANUAL"


@pytest.mark.asyncio
async def test_apply_rolls_back_all_rows_on_late_failure(session_factory, monkeypatch):
    plan = ImportPlan(candidates=[candidate("Buyer A"), candidate("Buyer B")])
    real_next_seq = party_import.next_seq
    calls = 0

    def fail_second(shop):
        nonlocal calls
        calls += 1
        if calls == 2:
            raise RuntimeError("injected")
        return real_next_seq(shop)

    monkeypatch.setattr(party_import, "next_seq", fail_second)
    async with session_factory() as session:
        with pytest.raises(RuntimeError, match="injected"):
            await apply_import(session, "shop-import", plan)
    async with session_factory() as session:
        count = await session.scalar(select(func.count()).select_from(Party))
        shop = await session.get(ShopProfile, "shop-import")

    assert count == 0
    assert shop.last_seq == 0


@pytest.mark.asyncio
async def test_apply_rejects_missing_shop_without_writes(session_factory):
    async with session_factory() as session:
        with pytest.raises(PartyImportError, match="shop_not_found"):
            await apply_import(session, "missing-shop", ImportPlan(candidates=[candidate("Buyer")]))
    async with session_factory() as session:
        assert await session.scalar(select(func.count()).select_from(Party)) == 0
