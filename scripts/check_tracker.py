#!/usr/bin/env python3
"""Guards FEATURES.json and PROGRESS.md (session contract, AGENTS.md section 4).
Usage: check_tracker.py [--staged]   (--staged is used by .githooks/pre-commit)
Fails when: an entry is deleted or reworded, `passing` has no evidence, an id repeats,
or (staged) FEATURES.json changes without a PROGRESS.md entry in the same commit."""
import json, subprocess, sys

def git(*a):
    r = subprocess.run(["git", *a], capture_output=True, text=True)
    return r.stdout if r.returncode == 0 else None

errs = []
cur = json.load(open("FEATURES.json"))["features"]
ids = [x.get("id") for x in cur]
for x in cur:
    for k in ("id", "description", "status", "evidence"):
        if k not in x:
            errs.append(f"{x.get('id', '?')}: missing field {k}")
    if x.get("status") not in ("passing", "failing"):
        errs.append(f"{x.get('id')}: status must be passing or failing")
    if x.get("status") == "passing" and not str(x.get("evidence", "")).strip():
        errs.append(f"{x.get('id')}: passing without evidence")
errs += [f"duplicate id {i}" for i in set(ids) if ids.count(i) > 1]

old = git("show", "HEAD:FEATURES.json")
if old:
    for o in json.loads(old)["features"]:
        n = next((x for x in cur if x.get("id") == o["id"]), None)
        if n is None:
            errs.append(f"{o['id']}: entry deleted")
        elif n.get("description") != o["description"]:
            errs.append(f"{o['id']}: description reworded")

if "--staged" in sys.argv:
    staged = (git("diff", "--cached", "--name-only") or "").split()
    if "FEATURES.json" in staged and "PROGRESS.md" not in staged:
        errs.append("FEATURES.json changed but PROGRESS.md has no entry in this commit")

if errs:
    print("tracker check FAILED:\n- " + "\n- ".join(errs), file=sys.stderr)
    sys.exit(1)
print(f"tracker ok ({len(cur)} features)")
