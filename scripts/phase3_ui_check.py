#!/usr/bin/env python3
"""Check changed Phase 3 content for UI policy violations."""

from __future__ import annotations

import argparse
from collections import defaultdict
import re
import subprocess
import sys
from dataclasses import dataclass


THEME_PATH = re.compile(r"(?:^|/)ui/theme/")
TARGET_UI_FILE = re.compile(r"(?:Screen|Sheet|Dialog)\.kt$")
NUMBER = r"\d[\d_]*(?:\.\d[\d_]*)?[fF]?"
COLOUR_LITERAL = re.compile(
    r"(?:\bColor\s*\(\s*0x[0-9a-fA-F_]+\b|"
    rf"\bColor\s*\(\s*{NUMBER}\s*,\s*{NUMBER}\s*,\s*{NUMBER}(?:\s*,\s*{NUMBER})?\s*\)|"
    r"\bColor\.(?:Black|Blue|Cyan|DarkGray|Gray|Green|LightGray|Magenta|Red|Transparent|White|Yellow)\b|"
    r"\bColor\.(?:argb|parseColor|rgb)\s*\(|#[0-9a-fA-F]{3,8}\b)"
)
EMOJI = re.compile(
    r"(?:[#*0-9]\ufe0f?\u20e3|[\U0001F000-\U0001FAFF]|"
    r"[\u00A9\u00AE\u203C\u2049\u2122\u2139\u231A-\u231B\u2328\u23CF\u23E9-\u23F3\u23F8-\u23FA\u24C2"
    r"\u25AA-\u25AB\u25B6\u25C0\u25FB-\u25FE\u2600-\u2604\u260E\u2611\u2614-\u2615\u2618\u261D"
    r"\u2620\u2622-\u2623\u2626\u262A\u262E-\u262F\u2638-\u263A\u2640\u2642\u2648-\u2653\u265F-\u2660"
    r"\u2663\u2665-\u2666\u2668\u267B\u267E-\u267F\u2692-\u2697\u2699\u269B-\u269C\u26A0-\u26A1"
    r"\u26A7\u26AA-\u26AB\u26B0-\u26B1\u26BD-\u26BE\u26C4-\u26C5\u26C8\u26CE-\u26CF\u26D1\u26D3-\u26D4"
    r"\u26E9-\u26EA\u26F0-\u26F5\u26F7-\u26FA\u26FD\u2702\u2705\u2708-\u270D\u270F\u2712\u2714\u2716"
    r"\u271D\u2721\u2728\u2733-\u2734\u2744\u2747\u274C\u274E\u2753-\u2755\u2757\u2763-\u2764"
    r"\u2795-\u2797\u27A1\u27B0\u27BF\u2934-\u2935\u2B05-\u2B07\u2B1B-\u2B1C\u2B50\u2B55]|"
    r"[\u2194-\u21FF\u2300-\u27FF\u2934-\u2BFF]\ufe0f)"
)
VALUE_NAME = re.compile(r"(?:paisa|grams?)", re.IGNORECASE)
ARITHMETIC = re.compile(
    r"(?:\+=|-=|\*=|/=|%=|(?<!\+)\+(?![+=])|(?<![=!<>-])-(?![=>-])|(?<![*/])[*/%](?![=]))"
)
INSTALL_DEBUG = re.compile("install" + "Debug")


@dataclass(frozen=True)
class ChangedLine:
    path: str
    number: int
    text: str


def git_output(args: list[str]) -> str:
    result = subprocess.run(
        ["git", *args],
        capture_output=True,
        text=True,
    )
    if result.returncode:
        print("phase3-ui-check: unable to compare the requested Git diff", file=sys.stderr)
        raise SystemExit(2)
    return result.stdout


def changed_paths(base_ref: str) -> list[str]:
    fields = git_output(
        ["diff", "--name-status", "-z", "--find-renames", f"{base_ref}...HEAD"]
    ).split("\0")
    paths: list[str] = []
    index = 0
    while index < len(fields) - 1:
        status = fields[index]
        index += 1
        if not status:
            continue
        if status[0] in {"R", "C"}:
            index += 1  # Old path; only the changed destination is scanned.
        if index >= len(fields):
            break
        path = fields[index]
        index += 1
        if status[0] != "D":
            paths.append(path)
    return paths


def changed_lines_for_path(base_ref: str, path: str) -> list[ChangedLine]:
    diff = git_output(
        ["diff", "--no-color", "--unified=0", f"{base_ref}...HEAD", "--", path]
    )

    lines: list[ChangedLine] = []
    new_line = 0
    in_hunk = False
    for diff_line in diff.splitlines():
        hunk = re.match(r"@@ -\d+(?:,\d+)? \+(\d+)(?:,\d+)? @@", diff_line)
        if hunk:
            new_line = int(hunk.group(1))
            in_hunk = True
            continue
        if not in_hunk or not diff_line or diff_line.startswith("\\"):
            continue
        if diff_line.startswith("+"):
            lines.append(ChangedLine(path, new_line, diff_line[1:]))
            new_line += 1
        elif not diff_line.startswith("-"):
            new_line += 1
    return lines


def changed_lines(base_ref: str) -> list[ChangedLine]:
    return [
        line
        for path in changed_paths(base_ref)
        for line in changed_lines_for_path(base_ref, path)
    ]


def strip_kotlin_comments_and_strings(lines: list[str]) -> list[str]:
    cleaned: list[str] = []
    in_block_comment = False
    in_triple_string = False
    for source in lines:
        output: list[str] = []
        index = 0
        while index < len(source):
            if in_block_comment:
                end = source.find("*/", index)
                if end < 0:
                    index = len(source)
                    continue
                in_block_comment = False
                index = end + 2
                continue
            if in_triple_string:
                end = source.find('\"\"\"', index)
                if end < 0:
                    index = len(source)
                    continue
                in_triple_string = False
                index = end + 3
                continue
            if source.startswith("//", index):
                break
            if source.startswith("/*", index):
                in_block_comment = True
                index += 2
                continue
            if source.startswith('\"\"\"', index):
                in_triple_string = True
                index += 3
                continue
            if source[index] in {'\"', "'"}:
                quote = source[index]
                index += 1
                while index < len(source):
                    if source[index] == "\\":
                        index += 2
                    elif source[index] == quote:
                        index += 1
                        break
                    else:
                        index += 1
                continue
            output.append(source[index])
            index += 1
        cleaned.append("".join(output))
    return cleaned


def current_file_lines(path: str) -> list[str]:
    return git_output(["show", f"HEAD:{path}"]).splitlines()


def continues_expression(source: str, next_source: str) -> bool:
    source = source.rstrip()
    next_source = next_source.lstrip()
    if not source or not next_source:
        return False
    if re.search(r"(?:[=+*/%,.]|&&|\|\|)$", source):
        return True
    if re.match(r"(?:[+*/%]|-(?!>))", next_source):
        return True
    return source.count("(") > source.count(")") or source.count("[") > source.count("]")


def arithmetic_violations(lines: list[ChangedLine]) -> list[ChangedLine]:
    by_path: dict[str, list[ChangedLine]] = defaultdict(list)
    for line in lines:
        if TARGET_UI_FILE.search(line.path):
            by_path[line.path].append(line)

    violations: list[ChangedLine] = []
    for path_lines in by_path.values():
        source_lines = strip_kotlin_comments_and_strings(
            current_file_lines(path_lines[0].path)
        )
        code_lines = [
            source_lines[line.number - 1] if line.number <= len(source_lines) else ""
            for line in path_lines
        ]
        run_start = 0
        for index in range(1, len(path_lines) + 1):
            if (
                index < len(path_lines)
                and path_lines[index].number == path_lines[index - 1].number + 1
                and continues_expression(code_lines[index - 1], code_lines[index])
            ):
                continue
            run = path_lines[run_start:index]
            code = "\n".join(code_lines[run_start:index])
            if VALUE_NAME.search(code) and ARITHMETIC.search(code):
                violation_index = next(
                    (offset for offset, source in enumerate(code_lines[run_start:index]) if ARITHMETIC.search(source)),
                    0,
                )
                violations.append(run[violation_index])
            run_start = index
    return violations


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
    return None


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base-ref", default="phase3/integration")
    args = parser.parse_args()

    lines = changed_lines(args.base_ref)
    violations = [(line, rule) for line in lines if (rule := rule_for(line)) is not None]
    violations.extend((line, "paisa-gram-arithmetic") for line in arithmetic_violations(lines))
    for line, rule in violations:
        print(f"{line.path}:{line.number}: {rule}")
    if violations:
        print(f"phase3-ui-check: {len(violations)} violation(s)", file=sys.stderr)
        return 1
    print("phase3-ui-check: PASS")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
