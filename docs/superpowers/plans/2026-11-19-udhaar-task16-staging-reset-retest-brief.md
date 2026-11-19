# Task 16 brief for Antigravity: retest Udhaar on the rebuilt staging server (no deploy, no push, no code change unless a bug is found)

Paths are relative to `projects/MandiSamiti/`. Run this after Task 15, which is accepted and pushed (`origin/main` at `e8b7259`). Run `git pull` first.

## Why this task exists
Staging was wiped and rebuilt on 2026-11-19 because the app was never released and its data was test data. Migrations `0001` to `0008` ran from an empty Postgres 16 on the VPS, and the server is healthy at `http://64.227.142.180:8060` on `0008_udhaar_fields`. The emulator and the Redmi still hold old local data and login tokens that point at a database that no longer exists.

This is the first real run of the Udhaar fields (`direction`, `kind`, `receiver_name`, `receiver_phone`, the cash-transaction lend link) through the server. Task 12 proved the wire format on SQLite only. The point of this task is to prove a **round trip**: create on the phone, push, wipe the phone, pull, and see the same data.

Also close the Task 14 gaps from the Task 15 report: items that were read from code but not run, plus two claims with no matching device step.

## Rules
- Staging URL only (`http://64.227.142.180:8060`). Never `10.0.2.2`.
- Never write or print the signup password/MPIN, tokens, or any key. Choose a throwaway test MPIN, keep it out of every file and the report.
- Database access is **read-only**: `SELECT` over `ssh appworx-core-vps` into `mandisamiti_staging_db` is allowed. No `INSERT`, `UPDATE`, `DELETE`, no schema change, no touching `mandisamiti_prod_*`. All data is created through the app UI.
- Do not push (repo rule: never push to `main`; branch and pull request only, and only with Vivek's approval). Do not deploy. Do not change app or backend code unless a test fails because of a real bug; if so, stop and report it to Vivek before fixing.

## Do (in order)
1. `git pull` in `projects/MandiSamiti`, confirm HEAD includes `e8b7259`. Check `curl http://64.227.142.180:8060/` readiness reports `db: ok, redis: ok`.
2. Reset the emulator: uninstall the app (or clear its data), reinstall the current debug build. Sign up a fresh throwaway shop. Confirm the first sync runs from zero without an error.
3. **Round-trip data set**, all created through the UI, interest off unless stated:
   1. Loan GIVEN Rs 10,000 to farmer A.
   2. Pesgi GIVEN Rs 3,000 to farmer A, through the loan / pesgi toggle.
   3. Loan GIVEN Rs 2,000 to farmer B with the receiver switch on ("पैसा किसी और को दिया?") and a receiver name and phone.
   4. Loan TAKEN Rs 5,000 from party C.
   5. One repayment of Rs 1,000 against the loan in 3.1.
4. Trigger a sync and wait for it to finish. Then, read-only on the server, run `SELECT` on `lend_entries` and the cash/settlement tables and record: row counts, and for each entry its `direction`, `kind` and receiver fields. Pesgi must show `kind = PESGI`, the TAKEN loan must show `direction = TAKEN`, and the receiver must be stored on farmer B's loan. Do not paste phone numbers; give counts and a yes or no per field.
5. **Wipe the phone and pull.** Uninstall the app, reinstall, log in with the same test account, and let the first pull finish. Open Udhaar and confirm: the same balances, the "पेशगी" chip on the Rs 3,000 entry, farmer B's receiver shown, party C shown as TAKEN, and the repayment in the history. This is the main proof. If anything is missing or wrong after pull, stop and report which field.
6. Re-run the Task 15 walk on this data, through the UI: a buy slip for farmer A of Rs 10,000 gross with a Rs 5,000 loan cut and Rs 4,000 cash. Write down which loan the cut settled first and why. Then overpay on the person screen and see the block, then void the slip and confirm the balance returns. Sync once more after the void and confirm the server rows (read-only) show the void.
7. **Task 14 gaps, each run on the device, not read from code:**
   1. Open the khata for a party with zero slips and zero udhaar. Report what the screen shows.
   2. On the Parchi list with a voided slip, tap the party name. Report whether the khata opens.
   3. Open the khata from a Parchi party name, press back, and report where you land (Home or Parchi). Run it twice.
   4. Double-tap "खाता देखें" quickly and report whether one khata opens.
   5. Check the khata entry for a party that cannot be resolved if the app lets you reach one; if you cannot reach it, say so rather than claiming it was checked.
   6. Hindi and English labels on the new buttons: switch the language in Settings and screenshot both.
8. Screenshots, saved as `docs/testing/screenshots/udhaar/t16-<screen>-<light|dark>-390.png`: the person screen showing the pesgi chip in light and dark, and the person screen after the pull. Delete any test-only screenshots, temp files and extra worktrees (including `ms-repay-edit-wt` if it still exists) when you finish.
9. If the Redmi Note 7 (`d93c743`) is connected, repeat steps 2, 5 and 7.3 on it. If it is not connected, list it as unverified. Do not guess.
10. Logging: a `PROGRESS.md` entry (runtime, what ran, concrete evidence, unverified list, next steps), a "Udhaar round trip on rebuilt staging" feature at the end of `FEATURES.json` with `passing` only when a test run or a dated Redmi `d93c743` check proves it (repo rule in `CLAUDE.md`). Emulator-only results go in the evidence text, and the feature stays `failing` or is listed as unverified until the Redmi check is done. Add a vault `log.md` entry. Run `python3 scripts/vault_lint.py` from the repo root (0 errors). Commit the docs in the submodule as `docs(mandisamiti): task 16 staging round-trip retest`. Do not push.

## Out of scope
- Backend deploy to production, the `mandisamiti-prod` stack, grain udhaar, a taken pesgi, pesgi on the buy slip, any change to the allocator.
- Squashing the Alembic migrations.

## Report back (numbered)
1. Commit hashes and the HEAD you started from.
2. Server row counts and field checks from step 4 (yes or no per field, no phone numbers).
3. The result of step 5, the round trip, field by field.
4. Step 6 numbers and which loan the cut settled first.
5. Step 7 results for each of the six items, with "not reachable" where true.
6. Screenshot names, and what you deleted.
7. What is unverified (Redmi, production deploy, production migration).
8. Anything that conflicts with this brief. If so, stop and ask Vivek one question.
