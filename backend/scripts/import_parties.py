#!/usr/bin/env python3
from __future__ import annotations

import argparse
import asyncio
import sys
from pathlib import Path

from app.database import AsyncSessionLocal
from app.importers.party_import import (
    SOURCE_BUYERS,
    SOURCE_FARMERS,
    SOURCE_GST,
    SOURCE_OCTOBER,
    PartyImportError,
    apply_import,
    build_import_plan,
    write_review_csv,
)


def parser() -> argparse.ArgumentParser:
    command = argparse.ArgumentParser(description="Import party masters from private CSV exports")
    command.add_argument("--farmers", type=Path)
    command.add_argument("--buyers", type=Path)
    command.add_argument("--gst-purpose", type=Path)
    command.add_argument("--october-buyers", type=Path)
    command.add_argument("--shop-id")
    command.add_argument("--apply", action="store_true")
    command.add_argument("--import-root", type=Path, default=Path.home() / "mandisamiti-import", help=argparse.SUPPRESS)
    return command


async def run(args: argparse.Namespace) -> int:
    if args.apply and not args.shop_id:
        raise PartyImportError("shop_id_required")
    paths = {
        key: value for key, value in {
            SOURCE_FARMERS: args.farmers,
            SOURCE_BUYERS: args.buyers,
            SOURCE_GST: args.gst_purpose,
            SOURCE_OCTOBER: args.october_buyers,
        }.items() if value is not None
    }
    plan = build_import_plan(paths, args.import_root)
    review_path = write_review_csv(plan, args.import_root)
    result_new = len(plan.candidates)
    result_existing = 0
    if args.apply:
        async with AsyncSessionLocal() as session:
            result = await apply_import(session, args.shop_id, plan)
        result_new = result.new_parties
        result_existing = result.existing_parties
    print(f"farmer_candidates={plan.farmer_count}")
    print(f"buyer_candidates={plan.buyer_count}")
    print(f"new_parties={result_new}")
    print(f"existing_parties={result_existing}")
    print(f"skipped_rows={plan.skipped_rows}")
    print(f"review_rows={len(plan.reviews)}")
    print(f"status={'applied' if args.apply else 'dry_run'}")
    print(f"review_file={review_path}")
    return 0


def main() -> int:
    args = parser().parse_args()
    try:
        return asyncio.run(run(args))
    except PartyImportError as exc:
        print(f"import_error={exc.code}", file=sys.stderr)
        return 2
    except Exception:
        print("import_error=unexpected_failure", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
