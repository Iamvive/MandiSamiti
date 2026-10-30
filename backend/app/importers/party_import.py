from __future__ import annotations

import csv
import os
import re
import tempfile
import time
import unicodedata
import uuid
from dataclasses import dataclass, field, replace
from pathlib import Path
from typing import Iterable, Mapping, Sequence

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.sync_seq import lock_shop, next_seq
from app.models.party import Party


MAX_FILE_BYTES = 10 * 1024 * 1024
MAX_DATA_ROWS = 50_000
PLACEHOLDER_PHONE = "1000000000"
GSTIN_RE = re.compile(r"^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z]$", re.ASCII)
IMPORT_NAMESPACE = uuid.UUID("ead1ced3-71f0-4f05-a4aa-dc2075c871ba")

SOURCE_BUYERS = "buyers"
SOURCE_GST = "gst_purpose"
SOURCE_OCTOBER = "october_buyers"
SOURCE_FARMERS = "farmers"
SOURCE_PRIORITY = (SOURCE_BUYERS, SOURCE_GST, SOURCE_OCTOBER, SOURCE_FARMERS)

ALIASES: dict[str, dict[str, tuple[str, ...]]] = {
    SOURCE_FARMERS: {
        "name": ("name", "farmer", "farmer name", "party", "किसान", "किसान नाम"),
        "phone": ("phone", "mobile", "mobile number", "फोन", "मोबाइल"),
        "village": ("village", "village name", "गाँव", "गांव"),
    },
    SOURCE_BUYERS: {
        "name": ("name", "buyer", "buyer name", "party", "खरीदार", "व्यापारी"),
        "phone": ("phone", "mobile", "mobile number", "फोन", "मोबाइल"),
        "gstin": ("gstin", "gst no", "gst number", "gstin no"),
        "mandi_license": ("mandi licence", "mandi license", "licence", "license", "मंडी लाइसेंस"),
    },
    SOURCE_GST: {
        "name": ("name", "buyer", "buyer name", "party", "खरीदार", "व्यापारी"),
        "gstin": ("gstin", "gst no", "gst number", "gstin no"),
    },
    SOURCE_OCTOBER: {
        "name": ("name", "buyer", "buyer name", "party", "खरीदार", "व्यापारी"),
        "gstin": ("gstin", "gst no", "gst number", "gstin no"),
    },
}

REQUIRED_FIELDS = {
    SOURCE_FARMERS: ("name",),
    SOURCE_BUYERS: ("name",),
    SOURCE_GST: ("name", "gstin"),
    SOURCE_OCTOBER: ("name",),
}


class PartyImportError(ValueError):
    """An expected import error whose message contains no row data."""

    def __init__(self, code: str, detail: str | None = None):
        self.code = code
        self.detail = detail
        super().__init__(code if detail is None else f"{code}: {detail}")


@dataclass(frozen=True)
class PartyCandidate:
    source: str
    row_number: int
    role: str
    name: str
    normalized_name: str
    phone: str | None = None
    village: str | None = None
    gstin: str | None = None
    mandi_license: str | None = None


@dataclass(frozen=True)
class ReviewIssue:
    source: str
    row_number: int
    code: str
    values: tuple[str, ...] = ()


@dataclass
class ImportPlan:
    candidates: list[PartyCandidate] = field(default_factory=list)
    reviews: list[ReviewIssue] = field(default_factory=list)
    skipped_rows: int = 0

    @property
    def farmer_count(self) -> int:
        return sum(candidate.role == "FARMER" for candidate in self.candidates)

    @property
    def buyer_count(self) -> int:
        return sum(candidate.role == "BUYER" for candidate in self.candidates)


@dataclass(frozen=True)
class ApplyResult:
    new_parties: int
    existing_parties: int


def normalized_text(value: str | None) -> str:
    text = unicodedata.normalize("NFKC", value or "")
    return " ".join(text.split()).casefold()


def display_text(value: str | None) -> str | None:
    text = " ".join((value or "").strip().split())
    return text or None


def normalize_phone(value: str | None) -> str | None:
    raw = re.sub(r"[\s-]", "", value or "").removeprefix("+")
    if len(raw) == 12 and raw.startswith("91"):
        raw = raw[2:]
    if raw == PLACEHOLDER_PHONE:
        return None
    return raw if len(raw) == 10 and raw.isascii() and raw.isdigit() else None


def normalize_gstin(value: str | None) -> str | None:
    raw = (value or "").strip().upper()
    return raw if GSTIN_RE.fullmatch(raw) else None


def validate_import_path(path: Path, import_root: Path) -> Path:
    if path.is_symlink():
        raise PartyImportError("symlink_rejected", path.name)
    try:
        root = import_root.expanduser().resolve(strict=True)
        resolved = path.expanduser().resolve(strict=True)
    except (FileNotFoundError, OSError) as exc:
        raise PartyImportError("file_unavailable", path.name) from exc
    if not root.is_dir():
        raise PartyImportError("import_root_invalid")
    if not resolved.is_relative_to(root):
        raise PartyImportError("path_outside_import_root", path.name)
    if not resolved.is_file():
        raise PartyImportError("not_a_regular_file", path.name)
    if resolved.stat().st_size > MAX_FILE_BYTES:
        raise PartyImportError("file_too_large", path.name)
    return resolved


def _header_map(source: str, fieldnames: Sequence[str] | None) -> dict[str, str]:
    if not fieldnames:
        raise PartyImportError("missing_header", source)
    cleaned = {normalized_text(header): header for header in fieldnames if header is not None}
    result: dict[str, str] = {}
    for canonical, aliases in ALIASES[source].items():
        matches = [cleaned[normalized_text(alias)] for alias in aliases if normalized_text(alias) in cleaned]
        if len(matches) > 1:
            raise PartyImportError("ambiguous_header", canonical)
        if matches:
            result[canonical] = matches[0]
        elif canonical in REQUIRED_FIELDS[source]:
            raise PartyImportError("missing_header", canonical)
    return result


def parse_csv(path: Path, source: str, import_root: Path) -> tuple[list[PartyCandidate], list[ReviewIssue], int]:
    if source not in ALIASES:
        raise PartyImportError("unknown_source", source)
    resolved = validate_import_path(path, import_root)
    candidates: list[PartyCandidate] = []
    reviews: list[ReviewIssue] = []
    skipped = 0
    try:
        with resolved.open("r", encoding="utf-8-sig", newline="") as handle:
            reader = csv.DictReader(handle)
            headers = _header_map(source, reader.fieldnames)
            for index, row in enumerate(reader, start=2):
                if index - 1 > MAX_DATA_ROWS:
                    raise PartyImportError("row_limit_exceeded", resolved.name)
                name = display_text(row.get(headers["name"]))
                if not name:
                    skipped += 1
                    reviews.append(ReviewIssue(source, index, "missing_name"))
                    continue
                raw_phone = row.get(headers["phone"]) if "phone" in headers else None
                phone = normalize_phone(raw_phone)
                if display_text(raw_phone) and phone is None:
                    reviews.append(ReviewIssue(source, index, "invalid_phone", (raw_phone or "",)))
                raw_gstin = row.get(headers["gstin"]) if "gstin" in headers else None
                gstin = normalize_gstin(raw_gstin)
                if display_text(raw_gstin) and gstin is None:
                    reviews.append(ReviewIssue(source, index, "invalid_gstin", (raw_gstin or "",)))
                village = display_text(row.get(headers["village"])) if "village" in headers else None
                licence = display_text(row.get(headers["mandi_license"])) if "mandi_license" in headers else None
                if licence and len(licence) > 100:
                    reviews.append(ReviewIssue(source, index, "mandi_license_too_long", (licence,)))
                    licence = None
                candidates.append(PartyCandidate(
                    source=source,
                    row_number=index,
                    role="FARMER" if source == SOURCE_FARMERS else "BUYER",
                    name=name,
                    normalized_name=normalized_text(name),
                    phone=phone,
                    village=village,
                    gstin=gstin,
                    mandi_license=licence,
                ))
    except UnicodeDecodeError as exc:
        raise PartyImportError("bad_encoding", resolved.name) from exc
    except csv.Error as exc:
        raise PartyImportError("malformed_csv", resolved.name) from exc
    return candidates, reviews, skipped


def _merge_value(
    current: str | None,
    incoming: str | None,
    source: str,
    row_number: int,
    field_name: str,
    reviews: list[ReviewIssue],
) -> str | None:
    if not current:
        return incoming
    if incoming and normalized_text(current) != normalized_text(incoming):
        reviews.append(ReviewIssue(source, row_number, f"conflicting_{field_name}", (current, incoming)))
    return current


def merge_candidates(raw: Iterable[PartyCandidate], initial_reviews: Iterable[ReviewIssue] = ()) -> ImportPlan:
    reviews = list(initial_reviews)
    merged: list[PartyCandidate] = []
    buyer_by_gstin: dict[str, int] = {}
    buyer_by_name: dict[str, int] = {}
    farmer_by_key: dict[tuple[str, ...], int] = {}
    phone_owner: dict[str, tuple[str, int]] = {}

    grouped = {source: [] for source in SOURCE_PRIORITY}
    for candidate in raw:
        grouped[candidate.source].append(candidate)

    for source in SOURCE_PRIORITY:
        for candidate in grouped[source]:
            if candidate.role == "BUYER":
                existing_index = buyer_by_gstin.get(candidate.gstin or "") if candidate.gstin else None
                if existing_index is None:
                    existing_index = buyer_by_name.get(candidate.normalized_name)
                if existing_index is None:
                    existing_index = len(merged)
                    merged.append(candidate)
                else:
                    current = merged[existing_index]
                    if current.normalized_name != candidate.normalized_name:
                        reviews.append(ReviewIssue(source, candidate.row_number, "conflicting_name", (current.name, candidate.name)))
                    merged[existing_index] = replace(
                        current,
                        phone=_merge_value(current.phone, candidate.phone, source, candidate.row_number, "phone", reviews),
                        village=_merge_value(current.village, candidate.village, source, candidate.row_number, "village", reviews),
                        gstin=_merge_value(current.gstin, candidate.gstin, source, candidate.row_number, "gstin", reviews),
                        mandi_license=_merge_value(current.mandi_license, candidate.mandi_license, source, candidate.row_number, "mandi_license", reviews),
                    )
                chosen = merged[existing_index]
                buyer_by_name[chosen.normalized_name] = existing_index
                if chosen.gstin:
                    buyer_by_gstin[chosen.gstin] = existing_index
                continue

            farmer = candidate
            if farmer.phone:
                owner = phone_owner.get(farmer.phone)
                if owner and owner[0] != farmer.normalized_name:
                    reviews.append(ReviewIssue(source, farmer.row_number, "duplicate_phone", (farmer.phone,)))
                    farmer = replace(farmer, phone=None)
                elif owner is None:
                    phone_owner[farmer.phone] = (farmer.normalized_name, farmer.row_number)
            key = ("phone", farmer.phone) if farmer.phone else (
                "name_village",
                farmer.normalized_name,
                normalized_text(farmer.village),
            )
            existing_index = farmer_by_key.get(key)
            if existing_index is None:
                farmer_by_key[key] = len(merged)
                merged.append(farmer)
            else:
                current = merged[existing_index]
                merged[existing_index] = replace(
                    current,
                    village=_merge_value(current.village, farmer.village, source, farmer.row_number, "village", reviews),
                )

    return ImportPlan(candidates=merged, reviews=reviews)


def build_import_plan(paths: Mapping[str, Path], import_root: Path) -> ImportPlan:
    raw: list[PartyCandidate] = []
    reviews: list[ReviewIssue] = []
    skipped = 0
    for source in SOURCE_PRIORITY:
        path = paths.get(source)
        if path is None:
            continue
        candidates, source_reviews, source_skipped = parse_csv(path, source, import_root)
        raw.extend(candidates)
        reviews.extend(source_reviews)
        skipped += source_skipped
    if not paths:
        raise PartyImportError("no_input_files")
    plan = merge_candidates(raw, reviews)
    plan.skipped_rows = skipped
    return plan


def natural_key(candidate: PartyCandidate) -> tuple[str, ...]:
    if candidate.role == "FARMER":
        return ("FARMER", "phone", candidate.phone) if candidate.phone else (
            "FARMER", "name_village", candidate.normalized_name, normalized_text(candidate.village)
        )
    return ("BUYER", "gstin", candidate.gstin) if candidate.gstin else (
        "BUYER", "name", candidate.normalized_name
    )


def existing_natural_key(party: Party) -> tuple[str, ...]:
    candidate = PartyCandidate(
        source="existing",
        row_number=0,
        role=party.role,
        name=party.name,
        normalized_name=normalized_text(party.name),
        phone=normalize_phone(party.phone),
        village=party.village,
        gstin=normalize_gstin(party.gstin),
    )
    return natural_key(candidate)


def deterministic_party_id(shop_id: str, candidate: PartyCandidate) -> str:
    material = "|".join((shop_id, *[part or "" for part in natural_key(candidate)]))
    return str(uuid.uuid5(IMPORT_NAMESPACE, material))


async def apply_import(session: AsyncSession, shop_id: str, plan: ImportPlan) -> ApplyResult:
    async with session.begin():
        shop = await lock_shop(session, shop_id)
        if shop is None:
            raise PartyImportError("shop_not_found")
        rows = (await session.execute(select(Party).where(Party.shop_id == shop_id))).scalars().all()
        ids = {row.id for row in rows}
        buyer_gstins = {row.gstin for row in rows if row.role == "BUYER" and normalize_gstin(row.gstin)}
        buyer_names = {normalized_text(row.name) for row in rows if row.role == "BUYER"}
        buyer_names_without_gstin = {
            normalized_text(row.name) for row in rows if row.role == "BUYER" and not normalize_gstin(row.gstin)
        }
        farmer_phones = {normalize_phone(row.phone) for row in rows if row.role == "FARMER" and normalize_phone(row.phone)}
        farmer_names = {
            (normalized_text(row.name), normalized_text(row.village)) for row in rows if row.role == "FARMER"
        }
        farmer_names_without_phone = {
            (normalized_text(row.name), normalized_text(row.village))
            for row in rows if row.role == "FARMER" and not normalize_phone(row.phone)
        }
        new_count = 0
        existing_count = 0
        now_ms = int(time.time() * 1000)
        for candidate in plan.candidates:
            party_id = deterministic_party_id(shop_id, candidate)
            name_village = (candidate.normalized_name, normalized_text(candidate.village))
            if candidate.role == "BUYER":
                identity_exists = (
                    candidate.gstin in buyer_gstins
                    or (candidate.gstin is not None and candidate.normalized_name in buyer_names_without_gstin)
                    or (candidate.gstin is None and candidate.normalized_name in buyer_names)
                )
            else:
                identity_exists = (
                    candidate.phone in farmer_phones
                    or (candidate.phone is not None and name_village in farmer_names_without_phone)
                    or (candidate.phone is None and name_village in farmer_names)
                )
            if party_id in ids or identity_exists:
                existing_count += 1
                continue
            session.add(Party(
                id=party_id,
                shop_id=shop_id,
                name=candidate.name,
                phone=candidate.phone,
                role=candidate.role,
                village=candidate.village,
                gstin=candidate.gstin,
                mandi_license=candidate.mandi_license,
                is_deleted=0,
                sync_version=1,
                server_seq=next_seq(shop),
                created_at=now_ms,
                updated_at=now_ms,
            ))
            ids.add(party_id)
            if candidate.role == "BUYER":
                buyer_names.add(candidate.normalized_name)
                if candidate.gstin:
                    buyer_gstins.add(candidate.gstin)
                else:
                    buyer_names_without_gstin.add(candidate.normalized_name)
            else:
                farmer_names.add(name_village)
                if candidate.phone:
                    farmer_phones.add(candidate.phone)
                else:
                    farmer_names_without_phone.add(name_village)
            new_count += 1
    return ApplyResult(new_parties=new_count, existing_parties=existing_count)


def spreadsheet_safe(value: str) -> str:
    return "'" + value if value.startswith(("=", "+", "-", "@")) else value


def write_review_csv(plan: ImportPlan, import_root: Path, destination: Path | None = None) -> Path:
    root = import_root.expanduser().resolve(strict=True)
    destination = destination or root / f"party-import-review-{int(time.time())}.csv"
    if destination.parent.resolve() != root:
        raise PartyImportError("review_path_outside_import_root")
    if destination.exists():
        raise PartyImportError("review_file_exists")
    fd, temporary_name = tempfile.mkstemp(prefix=".party-review-", suffix=".csv", dir=root)
    try:
        os.fchmod(fd, 0o600)
        with os.fdopen(fd, "w", encoding="utf-8", newline="") as handle:
            writer = csv.writer(handle)
            writer.writerow(("source", "row_number", "issue_code", "values"))
            for issue in plan.reviews:
                writer.writerow((
                    issue.source,
                    issue.row_number,
                    issue.code,
                    spreadsheet_safe(" | ".join(issue.values)),
                ))
        os.replace(temporary_name, destination)
        os.chmod(destination, 0o600)
    except BaseException:
        try:
            os.unlink(temporary_name)
        except FileNotFoundError:
            pass
        raise
    return destination
