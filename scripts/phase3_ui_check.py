#!/usr/bin/env python3
"""Check changed Phase 3 content for UI policy violations."""

from __future__ import annotations

import argparse
import re
import subprocess
import sys
from dataclasses import dataclass


THEME_PATH = re.compile(r"(?:^|/)ui/theme/")
TARGET_UI_FILE = re.compile(r"(?:Screen|Sheet|Dialog)\.kt$")
COLOUR_LITERAL = re.compile(
    r"(?:\bColor\s*\(\s*0x[0-9a-fA-F]{6,8}\b|"
    r"\bColor\.(?:Black|Blue|Cyan|DarkGray|Gray|Green|LightGray|Magenta|Red|Transparent|White|Yellow)\b|"
    r"\bColor\.(?:argb|parseColor|rgb)\s*\(|#[0-9a-fA-F]{3,8}\b)"
)
EMOJI = re.compile(r"[\U0001F000-\U0001FAFF\u2600-\u27BF]")
VALUE_NAME = re.compile(r"(?:paisa|grams?)", re.IGNORECASE)
ARITHMETIC = re.compile(r"(?:\+|\*|/|%|(?<![=!<>])-|\+=|-=|\*=|/=|%=)")
INSTALL_DEBUG = re.compile("install" + "Debug")


@dataclass(frozen=True)
class ChangedLine:
    path: str
    number: int
    text: str


def changed_lines(base_ref: str) -> list[ChangedLine]:
    result = subprocess.run(
        ["git", "diff", "--no-color", "--unified=0", f"{base_ref}...HEAD"],
        capture_output=True,
        text=True,
    )
    if result.returncode:
        print(f"phase3-ui-check: unable to compare against {base_ref}", file=sys.stderr)
        raise SystemExit(2)

    lines: list[ChangedLine] = []
    path: str | None = None
    new_line = 0
    for diff_line in result.stdout.splitlines():
        if diff_line.startswith("+++ b/"):
            path = diff_line[6:]
            continue
        hunk = re.match(r"@@ -\d+(?:,\d+)? \+(\d+)(?:,\d+)? @@", diff_line)
        if hunk:
            new_line = int(hunk.group(1))
            continue
        if path is None or not diff_line or diff_line.startswith("\\"):
            continue
        if diff_line.startswith("+"):
            lines.append(ChangedLine(path, new_line, diff_line[1:]))
            new_line += 1
        elif not diff_line.startswith("-"):
            new_line += 1
    return lines


def rule_for(line: ChangedLine) -> str | None:
    if INSTALL_DEBUG.search(line.text):
        return "install-debug"
    if EMOJI.search(line.text):
        return "emoji"
    if (
        line.path.endswith((".kt", ".xml"))
        and not THEME_PATH.search(line.path)
        and COLOUR_LITERAL.search(line.text)
    ):
        return "raw-colour"
    if (
        TARGET_UI_FILE.search(line.path)
        and VALUE_NAME.search(line.text)
        and ARITHMETIC.search(line.text)
    ):
        return "paisa-gram-arithmetic"
    return None


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base-ref", default="phase3/integration")
    args = parser.parse_args()

    violations = [
        (line, rule)
        for line in changed_lines(args.base_ref)
        if (rule := rule_for(line)) is not None
    ]
    for line, rule in violations:
        print(f"{line.path}:{line.number}: {rule}")
    if violations:
        print(f"phase3-ui-check: {len(violations)} violation(s)", file=sys.stderr)
        return 1
    print("phase3-ui-check: PASS")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
