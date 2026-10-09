# MandiSamiti Sync Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Entries made offline reach the server and come back down on another phone or after a reinstall, without losing or duplicating anything, and without blocking sync forever.

**Spec:** `docs/superpowers/specs/2026-10-08-mandisamiti-accounts-and-sync-design.md` §1.3, §2.4, §3 (sync rows), §4 (sync tests). This is plan 2 of 2 in §6.

**Starting point:** commit `8493139` (Antigravity, unreviewed) added WorkManager scheduling, a Ktor sync client, `syncMetadataEntity` (`2.sqm`) and a timestamp-paged pull. Keep the scaffolding; this plan fixes what is wrong in it:
1. Pulling back our own revisions throws (`insertRevision` is a plain `INSERT`, `UNIQUE(entry_id, revision)`), so every sync after the first push fails.
2. Pull uses `INSERT OR REPLACE ... sync_status = 1`, which silently overwrites unsynced local edits.
3. Pulled revisions are inserted with the default `sync_status = 0`, so they are pushed back and counted as pending.
4. The pull cursor is a timestamp with a separate `LIMIT` per table; rows are skipped when one table fills a page or timestamps tie.
5. The server acks a revision only when it is new, so a revision whose ack was lost (timeout after commit) stays pending forever and blocks logout.
6. `App.kt` builds `LogoutUseCase(SyncEngine(database), ...)` with no API client, so "push before logout" never runs.
7. No 401 → refresh → retry, no `NEEDS_LOGIN`, no first download after login, `HomeViewModel.createParty` does not enqueue a sync, the one-time job has no backoff.

**Architecture:**
- **Server:** a per-shop change counter `shop_profiles.last_seq`. A push locks the shop row (`SELECT ... FOR UPDATE`), so pushes for one shop are serialised and their `server_seq` values commit in order; a puller can never see seq 12 before seq 11 commits. (A global Postgres sequence would not give this.) Every accepted party, deal, cash row and revision gets the next value in its `server_seq` column. Pull is one stream ordered by `server_seq` across the four tables.
- **Push semantics:** the app still sends rows **and** revisions (rows carry current state; that is "applying the revision"). Rows are applied only if the incoming `revision` is newer (parties: newer `updated_at`). Anything the server already has is acked. A revision with the same `(entry_id, revision)` but different `snapshot_json` is stored in `sync_conflicts` and acked (spec: "kept and flagged"; showing it is a later spec).
- **App:** pull applies a server row only when the local row is absent or already synced (delete-if-synced + `INSERT OR IGNORE`; SQLDelight's default 3.18 dialect has no UPSERT). Push goes in batches of 100 and acks each batch before the next. The sync client refreshes the token once on 401 under a process-wide mutex; a dead refresh token sets a `needs_login` flag that Home shows as a banner.

**Tech Stack:** FastAPI, SQLAlchemy async (Postgres prod, SQLite in tests), pytest-asyncio; Kotlin 2.0.20 KMP, Ktor client 3.0.1 (MockEngine in tests), SQLDelight 2.0.2, WorkManager, Compose Multiplatform.

## Global Constraints
- **Branch:** `feat/sell-ready-core` in `projects/MandiSamiti`. Commit there and never push.
- **Commit messages:** end with `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- **Backend tests:** `cd backend && .venv/bin/python -m pytest -q`. Every task ends green.
- **App tests:** `./gradlew jvmTest`. Every task ends green. ViewModel/repository tests use `StandardTestDispatcher(testScheduler)` as `ioDispatcher`. No delays, sleeps, retries or longer timeouts.
- **Schema (app):** do NOT edit `1.sqm`, `2.sqm` or any existing `CREATE TABLE` in `AppDatabase.sq`. Adding *queries* is fine. `./gradlew :core-database:verifyCommonMainAppDatabaseMigration` must pass.
- **Schema (server):** tables come from `Base.metadata.create_all` (no Alembic in use). The server has never been deployed, so new columns are fine; the local dev DB must be recreated once (Task 1 step 6).
- **Batch size:** at most 100 items per list per push request. Pull page size 500, max 1000.
- **UI:** NGDL tokens from `ui/theme/Color.kt` only, no emoji, touch targets ≥ 48dp, no horizontal scroll at 390dp, readable in light and dark. User-facing text is Hindi.
- **Deviations from spec (deliberate):** a per-shop counter instead of a global Postgres sequence (gap-free ordering, see Architecture); push still carries rows + revisions rather than revisions only; request bodies are not gzip-compressed (100-item batches are small; FastAPI would need a decompression middleware — add later if data costs show up).
- **Out of scope:** showing conflicts to the owner; iOS background sync; the "no sync for 24h" banner; staff / multi-device.

---

### Task 1: Server change counter and `server_seq` columns

**Files:**
- Modify: `backend/app/models/user.py` (ShopProfile), `backend/app/models/party.py`, `backend/app/models/deal.py`, `backend/app/models/transaction.py`, `backend/app/models/revision.py`
- Create: `backend/app/models/sync_conflict.py`
- Modify: `backend/app/models/__init__.py` (export `SyncConflict`)
- Create: `backend/app/core/sync_seq.py`
- Test: `backend/tests/test_sync_seq.py`

**Interfaces:**
- Produces: `ShopProfile.last_seq: int`; `server_seq` column (BigInteger, default 0, indexed) on `Party`, `Deal`, `CashTransaction`, `EntryRevision`; model `SyncConflict(id, shop_id, entry_id, revision, snapshot_json, received_at)`; `async def lock_shop(db, shop_id) -> ShopProfile | None`; `def next_seq(shop: ShopProfile) -> int`.

- [ ] **Step 1: Write the failing test** — `backend/tests/test_sync_seq.py`:

```python
import pytest
from sqlalchemy.ext.asyncio import create_async_engine, async_sessionmaker, AsyncSession
from app.database import Base
from app.models.user import ShopProfile
from app.core.sync_seq import lock_shop, next_seq

@pytest.mark.asyncio
async def test_next_seq_counts_per_shop_and_persists():
    engine = create_async_engine("sqlite+aiosqlite:///:memory:")
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)
    sm = async_sessionmaker(engine, class_=AsyncSession, expire_on_commit=False)
    async with sm() as db:
        db.add_all([
            ShopProfile(id="a", shop_name="A", phone_number="9000000001"),
            ShopProfile(id="b", shop_name="B", phone_number="9000000002"),
        ])
        await db.commit()

        a = await lock_shop(db, "a")
        assert [next_seq(a), next_seq(a)] == [1, 2]
        b = await lock_shop(db, "b")
        assert next_seq(b) == 1
        await db.commit()

    async with sm() as db:
        a = await lock_shop(db, "a")
        assert next_seq(a) == 3
        assert await lock_shop(db, "missing") is None
    await engine.dispose()
```

- [ ] **Step 2: Run it, expect FAIL** (`ModuleNotFoundError: app.core.sync_seq`): `cd backend && .venv/bin/python -m pytest -q tests/test_sync_seq.py`

- [ ] **Step 3: Implement.** Add to `ShopProfile` (after `updated_at`):

```python
    # Per-shop change counter. Every row a push accepts takes the next value as its server_seq.
    last_seq = Column(BigInteger, nullable=False, default=0, server_default="0")
```

Add to each of `Party`, `Deal`, `CashTransaction`, `EntryRevision` (import `BigInteger` where missing):

```python
    server_seq = Column(BigInteger, nullable=False, default=0, server_default="0", index=True)
```

`backend/app/models/sync_conflict.py`:

```python
import uuid
import time
from sqlalchemy import Column, String, Integer, BigInteger, ForeignKey
from app.database import Base

class SyncConflict(Base):
    """A pushed revision whose (entry_id, revision) we already hold with different content. Kept, not applied."""
    __tablename__ = "sync_conflicts"

    id = Column(String, primary_key=True, default=lambda: str(uuid.uuid4()))
    shop_id = Column(String, ForeignKey("shop_profiles.id"), nullable=False, index=True)
    entry_id = Column(String, nullable=False)
    revision = Column(Integer, nullable=False)
    snapshot_json = Column(String, nullable=False)
    received_at = Column(BigInteger, default=lambda: int(time.time() * 1000), nullable=False)
```

Export it from `app/models/__init__.py` the same way the other models are exported (so `create_all` sees it).

`backend/app/core/sync_seq.py`:

```python
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession
from app.models.user import ShopProfile

async def lock_shop(db: AsyncSession, shop_id: str) -> ShopProfile | None:
    """Row-locks the shop until commit (Postgres), so one shop's pushes take server_seq values in commit order."""
    res = await db.execute(select(ShopProfile).where(ShopProfile.id == shop_id).with_for_update())
    return res.scalars().first()

def next_seq(shop: ShopProfile) -> int:
    shop.last_seq = (shop.last_seq or 0) + 1
    return shop.last_seq
```

- [ ] **Step 4: Run the whole suite, expect PASS:** `cd backend && .venv/bin/python -m pytest -q`

- [ ] **Step 5: Commit**

```bash
git add backend/app/models backend/app/core/sync_seq.py backend/tests/test_sync_seq.py
git commit -m "feat(sync): per-shop server_seq counter and conflict table"
```

- [ ] **Step 6: Recreate the local dev DB** (new columns; `create_all` does not alter tables). Check `backend/app/config.py` for `DATABASE_URL`. If it is the docker Postgres: `cd backend && docker compose down -v && docker compose up -d`. If it is `mandi_backend.db`: delete that file. Note in the task report that the Redmi must sign up again afterwards.

---

### Task 2: Server push — ack what the server holds, seq every change, keep conflicts

**Files:**
- Modify: `backend/app/api/v1/endpoints/sync.py` (`sync_push`)
- Modify: `backend/app/schemas/sync.py`, `backend/app/schemas/party.py`
- Test: `backend/tests/test_sync_push.py`

**Interfaces:**
- Consumes: `lock_shop`, `next_seq`, `SyncConflict` (Task 1).
- Produces: `SyncPushResponse{success, synced_parties, synced_deals, synced_transactions, synced_revisions, conflicts: List[str], server_sync_time, server_seq}`. `synced_*` now means "the server holds this version or a newer one" — the app may mark it synced. `conflicts` lists revision ids stored in `sync_conflicts` (they are also in `synced_revisions`). `PartyBase.updated_at: Optional[int]`. Each request list has `max_length=100` (422 above).

- [ ] **Step 1: Write the failing tests** — `backend/tests/test_sync_push.py`:

```python
import pytest
import pytest_asyncio
from pydantic import ValidationError
from sqlalchemy import select
from sqlalchemy.ext.asyncio import create_async_engine, async_sessionmaker, AsyncSession
from app.database import Base
from app.models.user import ShopProfile
from app.models.deal import Deal
from app.models.party import Party
from app.models.revision import EntryRevision
from app.models.sync_conflict import SyncConflict
from app.api.v1.endpoints.sync import sync_push
from app.schemas.sync import SyncPushRequest
from app.schemas.party import PartyCreate
from app.schemas.deal import DealCreate
from app.schemas.revision import EntryRevisionCreate

USER = {"shop_id": "shop-a", "user_id": "u-a"}

@pytest_asyncio.fixture
async def db():
    engine = create_async_engine("sqlite+aiosqlite:///:memory:")
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)
    sm = async_sessionmaker(engine, class_=AsyncSession, expire_on_commit=False)
    async with sm() as s:
        s.add(ShopProfile(id="shop-a", shop_name="A", phone_number="9000000001"))
        await s.commit()
        yield s
    await engine.dispose()

def deal(revision=1, payable=1000):
    return DealCreate(id="d1", farmer_id="p1", commodity="गेहूँ", gross_weight_grams=1, net_weight_grams=1,
                      net_farmer_payable_paisa=payable, net_buyer_receivable_paisa=payable, revision=revision)

def rev(revision=1, snapshot="{}", rid="r1"):
    return EntryRevisionCreate(id=rid, entry_id="d1", entry_kind="DEAL", revision=revision,
                               change_kind="CREATE", snapshot_json=snapshot)

@pytest.mark.asyncio
async def test_duplicate_push_is_acked_again_without_new_seq(db):
    req = SyncPushRequest(parties=[PartyCreate(id="p1", name="रामवीर", role="FARMER", updated_at=10)],
                          deals=[deal()], revisions=[rev()])
    first = await sync_push(req, USER, db)
    second = await sync_push(req, USER, db)   # response of the first one was "lost"
    assert first.synced_revisions == second.synced_revisions == ["r1"]
    assert second.synced_deals == ["d1"] and second.synced_parties == ["p1"]
    assert first.server_seq == 3 and second.server_seq == 3   # nothing new, no new seq

@pytest.mark.asyncio
async def test_older_deal_revision_does_not_overwrite_newer(db):
    await sync_push(SyncPushRequest(deals=[deal(revision=2, payable=2000)]), USER, db)
    res = await sync_push(SyncPushRequest(deals=[deal(revision=1, payable=1000)]), USER, db)
    assert res.synced_deals == ["d1"]
    row = (await db.execute(select(Deal).where(Deal.id == "d1"))).scalars().one()
    assert row.net_farmer_payable_paisa == 2000 and row.revision == 2

@pytest.mark.asyncio
async def test_newer_deal_revision_applies_and_takes_next_seq(db):
    await sync_push(SyncPushRequest(deals=[deal(revision=1)]), USER, db)
    res = await sync_push(SyncPushRequest(deals=[deal(revision=2, payable=5)]), USER, db)
    row = (await db.execute(select(Deal).where(Deal.id == "d1"))).scalars().one()
    assert row.net_farmer_payable_paisa == 5 and row.server_seq == 2 == res.server_seq

@pytest.mark.asyncio
async def test_conflicting_revision_is_kept_flagged_and_acked(db):
    await sync_push(SyncPushRequest(revisions=[rev(snapshot='{"a":1}')]), USER, db)
    res = await sync_push(SyncPushRequest(revisions=[rev(snapshot='{"a":2}', rid="r1-other")]), USER, db)
    assert res.synced_revisions == ["r1-other"] and res.conflicts == ["r1-other"]
    kept = (await db.execute(select(EntryRevision))).scalars().all()
    assert [r.snapshot_json for r in kept] == ['{"a":1}']
    conflict = (await db.execute(select(SyncConflict))).scalars().one()
    assert conflict.snapshot_json == '{"a":2}' and conflict.shop_id == "shop-a"

@pytest.mark.asyncio
async def test_party_last_write_wins_by_updated_at(db):
    await sync_push(SyncPushRequest(parties=[PartyCreate(id="p1", name="नया", role="FARMER", updated_at=20)]), USER, db)
    res = await sync_push(SyncPushRequest(parties=[PartyCreate(id="p1", name="पुराना", role="FARMER", updated_at=10)]), USER, db)
    assert res.synced_parties == ["p1"]
    row = (await db.execute(select(Party).where(Party.id == "p1"))).scalars().one()
    assert row.name == "नया"

def test_more_than_100_revisions_is_rejected():
    with pytest.raises(ValidationError):
        SyncPushRequest(revisions=[rev(revision=i, rid=f"r{i}") for i in range(101)])
```

- [ ] **Step 2: Run, expect FAIL:** `cd backend && .venv/bin/python -m pytest -q tests/test_sync_push.py`

- [ ] **Step 3: Schemas.** In `backend/app/schemas/party.py`, add to `PartyBase`: `updated_at: Optional[int] = None` (`PartyResponse` keeps `updated_at: int`). In `backend/app/schemas/sync.py`:

```python
from pydantic import BaseModel, Field

class SyncPushRequest(BaseModel):
    parties: List[PartyCreate] = Field(default_factory=list, max_length=100)
    deals: List[DealCreate] = Field(default_factory=list, max_length=100)
    transactions: List[CashTransactionCreate] = Field(default_factory=list, max_length=100)
    revisions: List[EntryRevisionCreate] = Field(default_factory=list, max_length=100)

class SyncPushResponse(BaseModel):
    success: bool = True
    # The server holds this version or a newer one: the app may mark it synced.
    synced_parties: List[str] = []
    synced_deals: List[str] = []
    synced_transactions: List[str] = []
    synced_revisions: List[str] = []
    # Revision ids whose (entry_id, revision) already existed with different content; kept in sync_conflicts.
    conflicts: List[str] = []
    server_sync_time: int
    server_seq: int = 0
```

- [ ] **Step 4: Rewrite `sync_push`.** Keep the existing field-copy blocks; change only the decision logic. Shape:

```python
    shop = await lock_shop(db, shop_id)
    if shop is None:
        raise HTTPException(status_code=400, detail="User is not associated with a Shop")

    # Parties: newer updated_at wins; equal/older is already held.
    for p_in in payload.parties:
        existing = (await db.execute(select(Party).where(Party.id == p_in.id))).scalars().first()
        if existing and existing.shop_id != shop_id:
            continue  # another shop's id: never touch, never ack
        incoming_at = p_in.updated_at or now_ms
        if existing is None:
            db.add(Party(id=p_in.id, shop_id=shop_id, ..., created_at=incoming_at, updated_at=incoming_at,
                         server_seq=next_seq(shop)))
        elif incoming_at > (existing.updated_at or 0):
            # copy fields as today, then:
            existing.updated_at = incoming_at
            existing.sync_version += 1
            existing.server_seq = next_seq(shop)
        synced_parties.append(p_in.id)

    # Deals and cash rows: apply only a newer revision; same or older is already held.
    for d_in in payload.deals:
        existing = ...
        if existing and existing.shop_id != shop_id:
            continue
        if existing is None:
            db.add(Deal(..., server_seq=next_seq(shop)))
        elif d_in.revision > (existing.revision or 0):
            # copy fields as today
            existing.server_seq = next_seq(shop)
        synced_deals.append(d_in.id)
    # (same pattern for payload.transactions / CashTransaction)

    # Revisions: append-only, keyed by (entry_id, revision) within this shop.
    for r_in in payload.revisions:
        held = (await db.execute(select(EntryRevision).where(
            EntryRevision.entry_id == r_in.entry_id, EntryRevision.revision == r_in.revision
        ))).scalars().first()
        if held is None:
            db.add(EntryRevision(id=r_in.id, shop_id=shop_id, ..., server_seq=next_seq(shop)))
        elif held.shop_id != shop_id:
            continue
        elif held.snapshot_json != r_in.snapshot_json:
            db.add(SyncConflict(shop_id=shop_id, entry_id=r_in.entry_id, revision=r_in.revision,
                                snapshot_json=r_in.snapshot_json))
            conflicts.append(r_in.id)
        synced_revisions.append(r_in.id)

    await db.commit()
    return SyncPushResponse(..., conflicts=conflicts, server_sync_time=now_ms, server_seq=shop.last_seq)
```

Rows pushed with `id=None` are no longer accepted silently: the app always sends ids; if `p_in.id` / `d_in.id` / `t_in.id` / `r_in.id` is `None`, skip the item (do not generate an id the app can never ack).

- [ ] **Step 5: Run the whole suite, expect PASS.** `tests/test_sync_isolation.py` must still pass; if its assertions read `synced_*` for cross-shop collisions, they should still see the colliding id omitted.

- [ ] **Step 6: Commit**

```bash
git add backend/app/api/v1/endpoints/sync.py backend/app/schemas backend/tests/test_sync_push.py
git commit -m "fix(sync): push acks what the server holds, seqs every change, keeps conflicts"
```

---

### Task 3: Server pull — one stream by `server_seq`

**Files:**
- Modify: `backend/app/api/v1/endpoints/sync.py` (`sync_pull`), `backend/app/schemas/sync.py` (`SyncPullResponse`), `backend/app/schemas/deal.py`, `backend/app/schemas/transaction.py`, `backend/app/schemas/party.py`, `backend/app/schemas/revision.py`
- Modify: `backend/tests/test_sync_isolation.py` (call signature only)
- Test: `backend/tests/test_sync_pull.py`

**Interfaces:**
- Consumes: `server_seq` columns (Task 1), push (Task 2).
- Produces: `GET /api/v1/sync/pull?after_seq=N&limit=500` → `SyncPullResponse{after_seq, next_seq, has_more, parties, deals, transactions, revisions, server_sync_time}`; each item response carries `server_seq`. Python signature `sync_pull(after_seq: int, limit: int, current_user, db)`. The `since` parameter and `last_sync_timestamp` are removed.

- [ ] **Step 1: Write the failing tests** — `backend/tests/test_sync_pull.py` (reuse the `db` fixture and helpers by copying them from `test_sync_push.py`):

```python
@pytest.mark.asyncio
async def test_paging_by_seq_has_no_gaps_across_kinds(db):
    # 3 parties, then 3 deals, then 3 revisions: seq 1..9 across three tables.
    await sync_push(SyncPushRequest(parties=[PartyCreate(id=f"p{i}", name=f"n{i}", role="FARMER") for i in range(3)]), USER, db)
    await sync_push(SyncPushRequest(deals=[
        DealCreate(id=f"d{i}", farmer_id="p0", commodity="गेहूँ", gross_weight_grams=1, net_weight_grams=1,
                   net_farmer_payable_paisa=1, net_buyer_receivable_paisa=1) for i in range(3)]), USER, db)
    await sync_push(SyncPushRequest(revisions=[
        EntryRevisionCreate(id=f"r{i}", entry_id=f"d{i}", entry_kind="DEAL", revision=1, change_kind="CREATE",
                            snapshot_json="{}") for i in range(3)]), USER, db)

    seen, cursor, pages = [], 0, 0
    while True:
        page = await sync_pull(after_seq=cursor, limit=4, current_user=USER, db=db)
        items = page.parties + page.deals + page.transactions + page.revisions
        seen += sorted(i.server_seq for i in items)
        cursor, pages = page.next_seq, pages + 1
        if not page.has_more:
            break
    assert seen == list(range(1, 10)) and pages == 3

@pytest.mark.asyncio
async def test_empty_pull_keeps_cursor(db):
    page = await sync_pull(after_seq=7, limit=500, current_user=USER, db=db)
    assert page.next_seq == 7 and page.has_more is False
```

- [ ] **Step 2: Run, expect FAIL** (`sync_pull` has no `after_seq` keyword that orders by seq / items lack `server_seq`).

- [ ] **Step 3: Implement.** Add `server_seq: int = 0` to `PartyResponse`, `DealResponse`, `CashTransactionResponse`, `EntryRevisionResponse`. `SyncPullResponse`:

```python
class SyncPullResponse(BaseModel):
    after_seq: int = 0
    next_seq: int = 0
    has_more: bool = False
    parties: List[PartyResponse] = []
    deals: List[DealResponse] = []
    transactions: List[CashTransactionResponse] = []
    revisions: List[EntryRevisionResponse] = []
    server_sync_time: int
```

`sync_pull` (replace the whole function, including the `getattr(..., "default")` lines):

```python
@router.get("/pull", response_model=SyncPullResponse, summary="Download changes after a server_seq cursor")
async def sync_pull(
    after_seq: int = Query(0, ge=0, description="Last server_seq this phone has applied"),
    limit: int = Query(500, ge=1, le=1000),
    current_user: dict = Depends(get_current_user_payload),
    db: AsyncSession = Depends(get_db),
):
    shop_id = current_user.get("shop_id")
    # Each table's first `limit` rows after the cursor contain every row of the global first `limit`.
    kinds = ((Party, PartyResponse), (Deal, DealResponse),
             (CashTransaction, CashTransactionResponse), (EntryRevision, EntryRevisionResponse))
    merged = []
    for model, schema in kinds:
        res = await db.execute(
            select(model).where(model.shop_id == shop_id, model.server_seq > after_seq)
            .order_by(model.server_seq.asc()).limit(limit + 1)
        )
        merged += [(row.server_seq, schema, row) for row in res.scalars().all()]
    merged.sort(key=lambda t: t[0])
    page = merged[:limit]

    out = {PartyResponse: [], DealResponse: [], CashTransactionResponse: [], EntryRevisionResponse: []}
    for _, schema, row in page:
        out[schema].append(schema.model_validate(row))
    return SyncPullResponse(
        after_seq=after_seq,
        next_seq=page[-1][0] if page else after_seq,
        has_more=len(merged) > limit,
        parties=out[PartyResponse], deals=out[DealResponse],
        transactions=out[CashTransactionResponse], revisions=out[EntryRevisionResponse],
        server_sync_time=int(time.time() * 1000),
    )
```

In `tests/test_sync_isolation.py`, change every `sync_pull(since=..., ...)` call to `sync_pull(after_seq=0, limit=500, current_user=..., db=...)`. Do not weaken its assertions.

- [ ] **Step 4: Run the whole suite, expect PASS.**

- [ ] **Step 5: Commit**

```bash
git add backend/app backend/tests
git commit -m "fix(sync): pull pages one server_seq stream; drop timestamp cursor"
```

---

### Task 4: App DB queries — pull never overwrites pending rows

**Files:**
- Modify: `core-database/src/commonMain/sqldelight/com/appwork/mandisamiti/database/AppDatabase.sq` (queries only)
- Test: `core-database/src/jvmTest/kotlin/com/appwork/mandisamiti/database/ServerApplyQueriesTest.kt`

**Interfaces:**
- Produces queries: `deleteSyncedParty(id)`, `insertPartyIfAbsent(<same 12 columns as insertParty>)`, `deleteSyncedDeal(id)`, `insertDealIfAbsent(<same columns as insertDeal>)`, `deleteSyncedCashTransaction(id)`, `insertCashTransactionIfAbsent(<same columns as insertCashTransaction>)`, `insertRevisionFromServer(id, shop_id, entry_id, entry_kind, revision, change_kind, snapshot_json, void_reason, changed_at)` (stores `sync_status = 1`), `markPartySyncedAt(id, updated_at)`.

- [ ] **Step 1: Write the failing test** (look at an existing test in `core-database/src/jvmTest` for how it creates the DB, e.g. `createTestDatabase()` / `DatabaseTest.kt`, and use the same helper):

```kotlin
class ServerApplyQueriesTest {
    private fun party(q: AppDatabaseQueries, id: String, name: String, sync: Long) =
        q.insertParty(id, "shop-1", name, null, null, "FARMER", null, null, 1L, 1L, 0L, sync)

    @Test
    fun serverRowReplacesSyncedRowButNotPendingRow() {
        val q = createTestDatabase().appDatabaseQueries
        party(q, "synced", "old", 1L)
        party(q, "pending", "local edit", 0L)

        listOf("synced", "pending", "new").forEach { id ->
            q.deleteSyncedParty(id)
            q.insertPartyIfAbsent(id, "shop-1", "server", null, null, "FARMER", null, null, 2L, 2L, 0L, 1L)
        }

        assertEquals("server", q.getPartyById("synced").executeAsOne().name)
        assertEquals("local edit", q.getPartyById("pending").executeAsOne().name)
        assertEquals(0L, q.getPartyById("pending").executeAsOne().sync_status)
        assertEquals("server", q.getPartyById("new").executeAsOne().name)
    }

    @Test
    fun revisionFromServerIsIgnoredWhenHeldAndStoredAsSynced() {
        val q = createTestDatabase().appDatabaseQueries
        q.insertRevision("r1", "shop-1", "d1", "DEAL", 1L, "CREATE", "{}", null, 1L)
        q.insertRevisionFromServer("r1", "shop-1", "d1", "DEAL", 1L, "CREATE", "{}", null, 1L)       // own revision back
        q.insertRevisionFromServer("r2", "shop-1", "d1", "DEAL", 2L, "EDIT", "{}", null, 2L)
        assertEquals(1L, q.getPendingSyncRevisions().executeAsList().size.toLong())            // only r1, ours
        assertEquals(2, q.getRevisionsForEntry("d1").executeAsList().size)
    }

    @Test
    fun markPartySyncedAtSkipsRowEditedAfterPush() {
        val q = createTestDatabase().appDatabaseQueries
        party(q, "p", "v1", 0L)                                                     // pushed at updated_at = 1
        q.insertParty("p", "shop-1", "v2", null, null, "FARMER", null, null, 1L, 5L, 0L, 0L)  // edited meanwhile
        q.markPartySyncedAt("p", 1L)
        assertEquals(0L, q.getPartyById("p").executeAsOne().sync_status)
    }
}
```

- [ ] **Step 2: Run, expect FAIL** (unresolved query functions): `./gradlew :core-database:jvmTest`

- [ ] **Step 3: Add the queries** under `-- Sync Outbox Queries` in `AppDatabase.sq`:

```sql
-- Applying a server row: replace only a row that has nothing unsynced; a pending local edit wins until pushed.
-- (Default SQLDelight dialect is SQLite 3.18: no UPSERT, so delete-if-synced then insert-if-absent.)
deleteSyncedParty:
DELETE FROM partyEntity WHERE id = ? AND sync_status = 1;

insertPartyIfAbsent:
INSERT OR IGNORE INTO partyEntity(id, shop_id, name, phone, village, party_type, monthly_interest_rate, photo_uri, created_at, updated_at, is_deleted, sync_status)
VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);

deleteSyncedDeal:
DELETE FROM dealEntity WHERE id = ? AND sync_status = 1;

insertDealIfAbsent:
INSERT OR IGNORE INTO dealEntity(
    id, shop_id, farmer_id, buyer_id, commodity_id, deal_status, deal_date,
    bags_count, gross_weight_grams, cut_weight_grams, net_weight_grams,
    rate_paisa_per_unit, gross_amount_paisa,
    farmer_commission_paisa, buyer_commission_paisa, labour_charge_paisa, weighing_charge_paisa, other_deductions_paisa,
    net_farmer_payable_paisa, net_buyer_receivable_paisa,
    receipt_photo_uri, voice_note_uri, remarks,
    created_at, updated_at, is_deleted, sync_status,
    farmer_commission_bps, revision, is_void, void_reason
) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);

deleteSyncedCashTransaction:
DELETE FROM cashTransactionEntity WHERE id = ? AND sync_status = 1;

insertCashTransactionIfAbsent:
INSERT OR IGNORE INTO cashTransactionEntity(id, shop_id, party_id, deal_id, transaction_type, amount_paisa, payment_mode, transaction_date, voice_note_uri, remarks, created_at, updated_at, is_deleted, sync_status, revision, is_void, void_reason)
VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);

-- A revision pulled from the server: already synced; ignored if we hold (id) or (entry_id, revision).
insertRevisionFromServer:
INSERT OR IGNORE INTO entryRevisionEntity(id, shop_id, entry_id, entry_kind, revision, change_kind, snapshot_json, void_reason, changed_at, sync_status)
VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 1);

-- Ack a pushed party only if it was not edited after the push read it.
markPartySyncedAt:
UPDATE partyEntity SET sync_status = 1 WHERE id = ? AND updated_at = ?;
```

- [ ] **Step 4: Run, expect PASS:** `./gradlew :core-database:jvmTest :core-database:verifyCommonMainAppDatabaseMigration`

- [ ] **Step 5: Commit**

```bash
git add core-database
git commit -m "fix(db): server rows never overwrite pending local rows; pulled revisions stored as synced"
```

---

### Task 5: SyncEngine — batched push with safe acks, pull that can't break

**Files:**
- Modify: `core-data/src/commonMain/kotlin/com/appwork/mandisamiti/data/sync/SyncEngine.kt`
- Modify: `core-data/src/commonMain/kotlin/com/appwork/mandisamiti/data/sync/model/SyncDto.kt`
- Test: `core-data/src/jvmTest/kotlin/com/appwork/mandisamiti/data/SyncEngineTest.kt` (extend `FakeSyncApiClient`, add tests)

**Interfaces:**
- Consumes: Task 4 queries; server contract from Tasks 2–3.
- Produces:
  - DTOs: `PartySyncDto` gains `created_at: Long = 0L, updated_at: Long = 0L`; `DealSyncDto` and `CashTransactionSyncDto` gain `created_at: Long = 0L, updated_at: Long = 0L`; `SyncPushResponseDto` gains `conflicts: List<String> = emptyList()`; `SyncPullResponseDto` drops `last_sync_timestamp`.
  - `SyncEngine.PUSH_BATCH = 100`, `KEY_NEEDS_LOGIN = "needs_login"`.
  - `suspend fun needsLogin(): Boolean`, `suspend fun setNeedsLogin(value: Boolean)`.
  - `pushPendingChanges(): Result<Int>` and `pullRemoteChanges(shopId, onProgress): Result<Int>` keep their signatures. `syncFull` clears `needs_login` on success.

- [ ] **Step 1: Extend the fake to script several calls.** In `SyncEngineTest.kt`, give `FakeSyncApiClient` queues:

```kotlin
class FakeSyncApiClient : MandiSyncApiClient {
    val pushed = mutableListOf<SyncPushRequestDto>()
    val pushResponses = ArrayDeque<Result<SyncPushResponseDto>>()
    val pullResponses = ArrayDeque<Result<SyncPullResponseDto>>()
    val pullCursors = mutableListOf<Long>()
    // Default when a queue is empty: ack everything that was sent / empty page.
    override suspend fun pushSync(request: SyncPushRequestDto): Result<SyncPushResponseDto> {
        pushed += request
        return pushResponses.removeFirstOrNull() ?: Result.success(SyncPushResponseDto(
            synced_parties = request.parties.map { it.id }, synced_deals = request.deals.map { it.id },
            synced_transactions = request.transactions.map { it.id }, synced_revisions = request.revisions.map { it.id }))
    }
    override suspend fun pullSync(afterSeq: Long, limit: Int): Result<SyncPullResponseDto> {
        pullCursors += afterSeq
        return pullResponses.removeFirstOrNull() ?: Result.success(SyncPullResponseDto(after_seq = afterSeq, next_seq = afterSeq))
    }
}
```

Update the existing tests in the file to the new fake (replace `pushResponseToReturn = x` with `pushResponses += x`, `lastPushedRequest` with `pushed.last()`, `lastPullAfterSeq` with `pullCursors.last()`).

- [ ] **Step 2: Write the failing tests** (build entries with the repositories as the existing test does; `saveDeal` writes the deal row and its CREATE revision):

```kotlin
@Test
fun pullingBackOurOwnRevisionDoesNotFailAndLeavesNothingPending() = runTest {
    val db = createTestDatabase(); val fake = FakeSyncApiClient(); val engine = SyncEngine(db, fake)
    OfflineFirstPartyRepository(db).saveParty(farmer())          // helper: Party(id="farmer-1", shopId="shop-1", ...)
    OfflineFirstDealRepository(db).saveDeal(deal())             // helper: Deal(id="deal-1", ...)
    assertTrue(engine.pushPendingChanges().isSuccess)
    val ownRev = db.appDatabaseQueries.getRevisionsForEntry("deal-1").executeAsOne()
    fake.pullResponses += Result.success(SyncPullResponseDto(next_seq = 3, revisions = listOf(
        EntryRevisionSyncDto(ownRev.id, "deal-1", "DEAL", 1, "CREATE", ownRev.snapshot_json, null, ownRev.changed_at))))
    assertTrue(engine.pullRemoteChanges("shop-1").isSuccess)
    assertEquals(0L, engine.getPendingCount())
    assertEquals(3L, engine.getLastServerSeq())
}

@Test
fun pullDoesNotOverwriteAPendingLocalEdit() = runTest {
    val db = createTestDatabase(); val fake = FakeSyncApiClient(); val engine = SyncEngine(db, fake)
    OfflineFirstPartyRepository(db).saveParty(farmer().copy(name = "local edit"))   // sync_status = 0
    fake.pullResponses += Result.success(SyncPullResponseDto(next_seq = 1, parties = listOf(
        PartySyncDto(id = "farmer-1", name = "server", role = "FARMER", updated_at = 1L))))
    engine.pullRemoteChanges("shop-1").getOrThrow()
    assertEquals("local edit", db.appDatabaseQueries.getPartyById("farmer-1").executeAsOne().name)
    assertEquals(1L, engine.getPendingCount())
}

@Test
fun pushGoesInBatchesOf100AndKeepsEarlierAcksWhenALaterBatchFails() = runTest {
    val db = createTestDatabase(); val fake = FakeSyncApiClient(); val engine = SyncEngine(db, fake)
    val repo = OfflineFirstPartyRepository(db)
    repeat(150) { repo.saveParty(farmer().copy(id = "p$it")) }
    fake.pushResponses += Result.success(SyncPushResponseDto(synced_parties = (0 until 100).map { "p$it" }))
    fake.pushResponses += Result.failure(RuntimeException("connection dropped"))
    assertTrue(engine.pushPendingChanges().isFailure)
    assertEquals(listOf(100, 50), fake.pushed.map { it.parties.size })
    assertEquals(50L, engine.getPendingCount())
}

@Test
fun pullResumesFromTheLastAppliedPage() = runTest {
    val db = createTestDatabase(); val fake = FakeSyncApiClient(); val engine = SyncEngine(db, fake)
    fake.pullResponses += Result.success(SyncPullResponseDto(next_seq = 500, has_more = true,
        parties = listOf(PartySyncDto(id = "a", name = "a", role = "FARMER"))))
    fake.pullResponses += Result.failure(RuntimeException("timeout"))
    assertTrue(engine.pullRemoteChanges("shop-1").isFailure)
    assertEquals(500L, engine.getLastServerSeq())
    engine.pullRemoteChanges("shop-1")
    assertEquals(listOf(0L, 500L, 500L), fake.pullCursors)
}

@Test
fun conflictedRevisionIsAckedSoItDoesNotBlockLogout() = runTest {
    val db = createTestDatabase(); val fake = FakeSyncApiClient(); val engine = SyncEngine(db, fake)
    OfflineFirstPartyRepository(db).saveParty(farmer())
    OfflineFirstDealRepository(db).saveDeal(deal())
    val revId = db.appDatabaseQueries.getRevisionsForEntry("deal-1").executeAsOne().id
    fake.pushResponses += Result.success(SyncPushResponseDto(synced_parties = listOf("farmer-1"),
        synced_deals = listOf("deal-1"), synced_revisions = listOf(revId), conflicts = listOf(revId)))
    engine.pushPendingChanges().getOrThrow()
    assertEquals(0L, engine.getPendingCount())
}
```

- [ ] **Step 3: Run, expect FAIL:** `./gradlew :core-data:jvmTest --tests "*SyncEngineTest*"`

- [ ] **Step 4: Implement.**
  - DTO fields as listed in Interfaces. When building `PartySyncDto` for push, set `updated_at = it.updated_at`, `created_at = it.created_at`; same for deals and cash rows.
  - `pushPendingChanges`: read the four pending lists once, then loop `i` over batches: `parties.chunked(PUSH_BATCH)`, etc., sending batch `i` of each list in one request (empty lists where a kind has fewer batches). After each successful response, ack inside `database.transaction { }`:
    - parties: `markPartySyncedAt(id, updatedAt)` for each sent party whose id is in `synced_parties` (look up the `updated_at` from what you sent).
    - deals / cash: `markDealSynced(id, revision)` / `markTransactionSynced(id, revision)` as today.
    - revisions: `markRevisionSynced(id)` for each id in `synced_revisions` (conflicts are included there).
    - On a failed response, return `Result.failure` immediately; earlier batches stay acked. Return the total acked count.
  - `pullRemoteChanges`: keep the page loop and the per-page `database.transaction`. Inside it, per item:
    - party: `deleteSyncedParty(p.id)` then `insertPartyIfAbsent(..., created_at = p.created_at.takeIf { it > 0 } ?: res.server_sync_time, updated_at = p.updated_at.takeIf { it > 0 } ?: res.server_sync_time, sync_status = 1L)`
    - deal / cash: `deleteSyncedDeal` + `insertDealIfAbsent`, `deleteSyncedCashTransaction` + `insertCashTransactionIfAbsent`, same timestamp rule.
    - revision: `insertRevisionFromServer(...)`.
    - Save the cursor with `setSyncMetadataLong(KEY_LAST_SERVER_SEQ, res.next_seq)` when `res.next_seq > currentSeq` — also when the page has 0 items (move the `if (pageItemsCount > 0)` so it guards only the inserts).
  - `needsLogin()` = `getSyncMetadataLong(KEY_NEEDS_LOGIN) == 1L`; `setNeedsLogin(v)` writes 1/0. `syncFull`: after a successful push+pull call `setNeedsLogin(false)`.
  - Delete `markAllBatchSynced` if nothing calls it (grep first).

- [ ] **Step 5: Run, expect PASS:** `./gradlew jvmTest`

- [ ] **Step 6: Commit**

```bash
git add core-data
git commit -m "fix(sync): batched push with safe acks; pull keeps pending edits and tolerates own revisions"
```

---

### Task 6: Sync client — 401 → refresh once → retry; dead refresh = needs login

**Files:**
- Modify: `core-data/src/commonMain/kotlin/com/appwork/mandisamiti/data/sync/remote/KtorMandiSyncApiClient.kt`
- Test: `core-data/src/jvmTest/kotlin/com/appwork/mandisamiti/data/sync/remote/KtorMandiSyncApiClientTest.kt`

**Interfaces:**
- Consumes: `SessionStore` (`current()`, `updateTokens`), `AuthApi.refresh(refreshToken): Result<Pair<String, String>>`, `AuthError.SessionExpired`.
- Produces: `class SyncAuthExpired : Exception("refresh token rejected")` in the same package. Constructor `KtorMandiSyncApiClient(http: HttpClient, baseUrl: String, sessionStore: SessionStore, refresh: suspend (refreshToken: String) -> Result<Pair<String, String>>)`. A process-wide `Mutex` serialises refreshes (the worker and logout can hit 401 together; refresh tokens rotate, so a second parallel refresh with the old token would fail).

- [ ] **Step 1: Write the failing tests** (follow the MockEngine setup already in this test file):

```kotlin
@Test
fun on401RefreshesOnceAndRetriesWithTheNewToken() = runTest {
    val seen = mutableListOf<String?>()
    val engine = MockEngine { req ->
        seen += req.headers[HttpHeaders.Authorization]
        if (req.headers[HttpHeaders.Authorization] == "Bearer old") respond("", HttpStatusCode.Unauthorized)
        else respond("""{"next_seq":0,"server_sync_time":1}""", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
    }
    val store = InMemorySessionStore().apply { save(Session("shop-1", "old", "r-old")) }
    var refreshCalls = 0
    val client = KtorMandiSyncApiClient(mandiHttpClient(engine), "http://x", store) { refreshCalls++; Result.success("new" to "r-new") }

    assertTrue(client.pullSync(0, 500).isSuccess)
    assertEquals(listOf("Bearer old", "Bearer new"), seen)
    assertEquals(1, refreshCalls)
    assertEquals("r-new", store.current()!!.refreshToken)
}

@Test
fun rejectedRefreshFailsWithSyncAuthExpiredAndKeepsTheSession() = runTest {
    val engine = MockEngine { respond("", HttpStatusCode.Unauthorized) }
    val store = InMemorySessionStore().apply { save(Session("shop-1", "old", "r-old")) }
    val client = KtorMandiSyncApiClient(mandiHttpClient(engine), "http://x", store) { Result.failure(AuthError.SessionExpired) }
    assertIs<SyncAuthExpired>(client.pullSync(0, 500).exceptionOrNull())
    assertEquals("shop-1", store.current()!!.shopId)
}

@Test
fun refreshNetworkErrorIsAPlainFailureNotNeedsLogin() = runTest {
    val engine = MockEngine { respond("", HttpStatusCode.Unauthorized) }
    val store = InMemorySessionStore().apply { save(Session("shop-1", "old", "r-old")) }
    val client = KtorMandiSyncApiClient(mandiHttpClient(engine), "http://x", store) { Result.failure(RuntimeException("offline")) }
    val err = client.pullSync(0, 500).exceptionOrNull()
    assertTrue(err != null && err !is SyncAuthExpired)
}
```

(If `AuthError.SessionExpired` is not an object, construct it the way `AuthApiTest` does.)

- [ ] **Step 2: Run, expect FAIL:** `./gradlew :core-data:jvmTest --tests "*KtorMandiSyncApiClientTest*"`

- [ ] **Step 3: Implement.** Replace `tokenProvider` with `sessionStore` + `refresh`. Core helper:

```kotlin
class SyncAuthExpired : Exception("refresh token rejected")

private val refreshLock = Mutex()   // top-level: one refresh at a time per process

private suspend fun send(request: suspend (token: String) -> HttpResponse): HttpResponse {
    val session = sessionStore.current() ?: throw SyncAuthExpired()
    val first = request(session.accessToken)
    if (first.status != HttpStatusCode.Unauthorized) return first
    val token = refreshLock.withLock {
        val now = sessionStore.current() ?: throw SyncAuthExpired()
        if (now.accessToken != session.accessToken) {
            now.accessToken                     // someone else already refreshed
        } else {
            val (access, refreshToken) = refresh(now.refreshToken).getOrElse { e ->
                throw if (e is AuthError.SessionExpired) SyncAuthExpired() else e
            }
            sessionStore.updateTokens(access, refreshToken)
            access
        }
    }
    val retried = request(token)
    if (retried.status == HttpStatusCode.Unauthorized) throw SyncAuthExpired()
    return retried
}
```

`pushSync` / `pullSync` call `send { token -> http.post(url) { header(Authorization, "Bearer $token"); ... } }`, then keep the existing OK / error-body handling. Fix the existing tests in this file for the new constructor.

- [ ] **Step 4: Run, expect PASS:** `./gradlew jvmTest`

- [ ] **Step 5: Commit**

```bash
git add core-data
git commit -m "feat(sync): refresh access token on 401 once, serialised; dead refresh = SyncAuthExpired"
```

---

### Task 7: Wiring — real client everywhere, backoff, enqueue after every save

**Files:**
- Modify: `composeApp/src/androidMain/kotlin/com/appwork/mandisamiti/sync/MandiSyncWorker.kt`
- Modify: `composeApp/src/androidMain/kotlin/com/appwork/mandisamiti/sync/AndroidSyncScheduler.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/App.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/home/HomeViewModel.kt`
- Test: `composeApp/src/jvmTest/kotlin/com/appwork/mandisamiti/ui/home/HomeViewModelTest.kt`

**Interfaces:**
- Consumes: Task 5 `SyncEngine`, Task 6 client.
- Produces: `HomeViewModel(..., onLocalWrite: () -> Unit = {})`; App builds one `syncEngine` with the real client and passes it to `LogoutUseCase`.

- [ ] **Step 1: Write the failing test** in `HomeViewModelTest.kt` (reuse its existing fixture for building the VM):

```kotlin
@Test
fun createPartyAsksForASync() = runTest {
    var writes = 0
    // shopRepo / partyRepo: build them the way the existing tests in this file do.
    val vm = HomeViewModel(shopId = "shop-1", shopProfileRepository = shopRepo,
        partyRepository = partyRepo, viewModelScope = backgroundScope, onLocalWrite = { writes++ })
    vm.createParty("रामवीर", null, null, PartyType.FARMER)
    assertEquals(1, writes)
}
```

- [ ] **Step 2: Run, expect FAIL:** `./gradlew :composeApp:jvmTest --tests "*HomeViewModelTest*"`

- [ ] **Step 3: Implement.**
  - `HomeViewModel`: add `private val onLocalWrite: () -> Unit = {}` as the last constructor param; call it after `partyRepository.saveParty(newParty)` in `createParty`.
  - `App.kt`:
    - Build once: `val syncEngine = remember { SyncEngine(database, KtorMandiSyncApiClient(mandiHttpClient(), apiBaseUrl, sessionStore, authApi::refresh)) }`.
    - `LogoutUseCase(syncEngine, authApi, sessionStore, wiper)` (this is the bug where logout's push never ran).
    - Pass `onLocalWrite = { syncScheduler.scheduleOneTimeSync() }` to `HomeViewModel`.
    - After a successful sign-out, call `syncScheduler.cancelAll()`.
  - `MandiSyncWorker`: build the client the same way (`AuthApi(mandiHttpClient(), apiBaseUrl)` for `refresh`). Result mapping:

```kotlin
when (val r = syncEngine.syncFull(currentSession.shopId)) {
    is SyncResult.Success -> Result.success()
    is SyncResult.Failure ->
        if (r.error is SyncAuthExpired) { syncEngine.setNeedsLogin(true); Result.failure() }
        else Result.retry()   // WorkManager's exponential backoff; no attempt cap
}
```

  - `AndroidSyncScheduler.scheduleOneTimeSync`: add `.setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)` and change `ExistingWorkPolicy.REPLACE` to `ExistingWorkPolicy.APPEND_OR_REPLACE`, so a save during a running sync queues one more run instead of cancelling the running one mid-batch.
  - `MainActivity.kt`: only call `schedulePeriodicSync()` when `sessionStore.current() != null`.

- [ ] **Step 4: Run, expect PASS:** `./gradlew jvmTest :composeApp:compileDebugKotlinAndroid`

- [ ] **Step 5: Commit**

```bash
git add composeApp
git commit -m "fix(sync): logout uses the real sync client; backoff; every save enqueues a sync"
```

---

### Task 8: First download after login + "log in again" banner

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/sync/FirstSyncViewModel.kt`
- Create: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/sync/FirstSyncScreen.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/App.kt` (new `Screen.FirstSync`, banner wiring)
- Modify: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/home/HomeScreen.kt` (banner params)
- Test: `composeApp/src/jvmTest/kotlin/com/appwork/mandisamiti/ui/sync/FirstSyncViewModelTest.kt`

**Interfaces:**
- Consumes: `SyncEngine.pullRemoteChanges(shopId, onProgress)`, `needsLogin()`, `setNeedsLogin()`.
- Produces: `sealed interface FirstSyncState { data class Downloading(val count: Int); data object Done; data object Failed }`; `FirstSyncViewModel(shopId, pull: suspend (String, (Int) -> Unit) -> Result<Int>, viewModelScope)` with `state: StateFlow<FirstSyncState>` and `retry()`. `HomeScreen(..., needsLogin: Boolean = false, onReLogin: () -> Unit = {})`.

- [ ] **Step 1: Write the failing tests:**

```kotlin
class FirstSyncViewModelTest {
    @Test
    fun reportsProgressThenDone() = runTest {
        val vm = FirstSyncViewModel("shop-1", { _, progress -> progress(120); progress(240); Result.success(240) }, backgroundScope)
        assertEquals(FirstSyncState.Done, vm.state.first { it == FirstSyncState.Done })
    }

    @Test
    fun failureShowsFailedAndRetryRunsAgain() = runTest {
        var calls = 0
        val vm = FirstSyncViewModel("shop-1", { _, _ -> if (++calls == 1) Result.failure(RuntimeException()) else Result.success(0) }, backgroundScope)
        vm.state.first { it == FirstSyncState.Failed }
        vm.retry()
        vm.state.first { it == FirstSyncState.Done }
        assertEquals(2, calls)
    }
}
```

- [ ] **Step 2: Run, expect FAIL.** `./gradlew :composeApp:jvmTest --tests "*FirstSyncViewModelTest*"`

- [ ] **Step 3: Implement the ViewModel:**

```kotlin
sealed interface FirstSyncState {
    data class Downloading(val count: Int) : FirstSyncState
    data object Done : FirstSyncState
    data object Failed : FirstSyncState
}

class FirstSyncViewModel(
    private val shopId: String,
    private val pull: suspend (String, (Int) -> Unit) -> Result<Int>,
    private val viewModelScope: CoroutineScope,
) {
    private val _state = MutableStateFlow<FirstSyncState>(FirstSyncState.Downloading(0))
    val state: StateFlow<FirstSyncState> = _state.asStateFlow()

    init { start() }

    fun retry() = start()

    private fun start() {
        _state.value = FirstSyncState.Downloading(0)
        viewModelScope.launch {
            val r = pull(shopId) { n -> _state.value = FirstSyncState.Downloading(n) }
            _state.value = if (r.isSuccess) FirstSyncState.Done else FirstSyncState.Failed
        }
    }
}
```

- [ ] **Step 4: Screen + wiring.**
  - `FirstSyncScreen(state, onRetry, onSkip, onDone)`: centred column on `MandiBackground`. `Downloading(n)`: `CircularProgressIndicator` + "खाता डाउनलोड हो रहा है…" and, when `n > 0`, "$n एंट्री मिलीं" in `MandiTextSecondary`. `Failed`: "इंटरनेट नहीं मिला" + primary button "फिर कोशिश करें" (`MandiPrimaryAction` / `MandiPrimaryActionText`) + text button "बाद में" (goes Home; background sync continues). `Done`: `LaunchedEffect(Unit) { onDone() }`. Buttons ≥ 48dp. No emoji.
  - `App.kt`: in `onRegistrationSuccess`, go to `Screen.FirstSync` instead of `Screen.Home` (both signup and login; a new shop finishes instantly). `onDone` / `onSkip` → `Screen.Home`. Pull with `{ id, p -> syncEngine.pullRemoteChanges(id, p) }`.
  - Banner: in the `Screen.Home` branch, `val needsLogin by produceState(false, shopId) { value = syncEngine.needsLogin() }`; pass to `HomeScreen`. In `HomeScreen`, when `needsLogin`, show a full-width card at the top (`MandiAmberLight` fill, `MandiAmberBorder` border, `MandiTextPrimary` text, `--ngdl-card-shadow` equivalent elevation used by other cards) reading "दोबारा लॉगिन करें" with subtitle "आपकी एंट्री फ़ोन में सुरक्षित हैं". Tapping it calls `onReLogin`.
  - `onReLogin` in App: `sessionStore.clear()` (keeps all data; `AuthRepository.adopt` keeps data for the same shop), `session = null`. On the next successful registration/login `syncEngine.setNeedsLogin(false)` (call it in `onRegistrationSuccess`).

- [ ] **Step 5: Run, expect PASS:** `./gradlew jvmTest :composeApp:compileDebugKotlinAndroid`

- [ ] **Step 6: Commit**

```bash
git add composeApp
git commit -m "feat(sync): first download after login; log-in-again banner keeps data"
```

---

### Task 9: Device verification (Redmi Note 7) and records

No new code unless a bug is found (then: failing test first, fix, commit).

- [ ] **Step 1:** Fresh local server (Task 1 step 6), `adb reverse tcp:8000 tcp:8000`, install debug build.
- [ ] **Step 2 — sync works:** sign up; add a party (Khata add — also checks `ad11422`), a deal and a cash entry. Within a minute the pending badge clears. Confirm rows in the server DB with `server_seq` 1..N.
- [ ] **Step 3 — void reasons:** void a cash entry; only cash reasons are offered (`ad11422`).
- [ ] **Step 4 — rush hour:** airplane mode, make 10 entries, sign-out is blocked with the right count; airplane off; all 10 reach the server; khata totals on phone match a SQL sum on the server.
- [ ] **Step 5 — restore:** with nothing pending, sign out (phone wipes); log in again → First-sync screen → khata and totals identical to before.
- [ ] **Step 6 — needs login:** delete the shop's rows in `refresh_tokens` on the server, wait for a 401 (or set the access token TTL to 1 minute locally), trigger a save → banner "दोबारा लॉगिन करें" appears, entries still on the phone; tap, log in, banner gone, data intact.
- [ ] **Step 7 — UI check:** first-sync screen and banner in light and dark theme, no clipping at the phone's width.
- [ ] **Step 8 — records:** add a "Sync — done" section to `vaults/Vivek-K/wiki/concepts/mandisamiti-product-review-2026-10-08.md` (what was built, what was phone-verified, what was not), a `log.md` entry, refresh `hot.md`; mark Sync rows in the spec's feature matrix; run `python3 scripts/vault_lint.py` (0 errors). Do not merge, push or deploy.
