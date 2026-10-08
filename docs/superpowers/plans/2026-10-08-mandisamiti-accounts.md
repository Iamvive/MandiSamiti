# MandiSamiti Accounts Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A shop signs up or logs in with phone + OTP (fixed code for now) + MPIN. The server issues the shop id and tokens. The app stops using `shop_default`. Logout is blocked while anything is unsynced, and otherwise wipes the phone.

**Spec:** `docs/superpowers/specs/2026-10-08-mandisamiti-accounts-and-sync-design.md`. This plan covers §1.1, §1.2, §1.4, §2.1, §2.2 and §2.3 (except the first-login download, which is in the Sync plan).

**Architecture:**
- **Server:**
  - A swappable `OtpSender` with `StaticOtpSender` as its only implementation.
  - OTP state in Redis (fakeredis in tests).
  - Short-lived single-use signup/login passes (JWT).
  - bcrypt MPIN.
  - Rotating refresh tokens tracked in a `refresh_tokens` table.
- **App:**
  - A Ktor `AuthApi` in `core-data`.
  - A `SessionStore` (tokens + shop id), encrypted with the Android Keystore.
  - `RegisterViewModel` drives PHONE → OTP → NEW_SHOP / ENTER_MPIN.
  - `App.kt` takes the shop id from the session.

**Tech Stack:** FastAPI, SQLAlchemy async, redis.asyncio, fakeredis, passlib[bcrypt], pytest-asyncio, httpx; Kotlin 2.0.20 KMP, Ktor client 3.0.1 (OkHttp / Darwin / MockEngine), SQLDelight 2.0.2, Compose Multiplatform.

## Global Constraints
- **Branch:** `feat/sell-ready-core` in `projects/MandiSamiti`. Commit there and never push.
- **Commit messages:** end with `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- **Backend tests:**
  - Run them with `cd backend && .venv/bin/python -m pytest -q`.
  - Every task ends green.
- **App tests:**
  - Run them with `./gradlew jvmTest`.
  - Every task ends green.
  - ViewModel and repository tests use `StandardTestDispatcher(testScheduler)` as `ioDispatcher`, plus `backgroundScope` and `uiState.first { specific condition }`.
  - No delay, sleeps, retries or longer timeouts.
- **Schema:** do NOT edit `1.sqm` or any `CREATE TABLE` in `AppDatabase.sq`. The phone is already on v2. Adding new *queries* is fine.
- **Phone numbers:** 10 digits, starting with 6–9. The server normalises by stripping `+91` and spaces.
- **OTP and MPIN:** OTP is 6 digits and MPIN is 4 digits. Lockout is 5 wrong tries. OTP sends are limited to 3 per 5 minutes per phone.
- **Tokens:** access 60 minutes; refresh 90 days and rotated on every use; signup/login pass 10 minutes and single-use.
- **UI:** NGDL tokens from `ui/theme/Color.kt` only, no emoji, touch targets ≥ 48dp, no horizontal scroll at 390dp. User-facing text is Hindi.
- **Deviation from spec:** the spec's `comm_wheat` fallback removal is deferred. There is no commodity master UI yet, so removing it would break deal saving. It goes to the charges/commodity spec.

---

### Task 1: Backend test harness

**Files:**
- Create: `backend/requirements-dev.txt`
- Create: `backend/tests/conftest.py`
- Modify: `backend/.gitignore` (create it if missing): add `.venv/`

- [ ] **Step 1:** Create `backend/requirements-dev.txt`:
```
-r requirements.txt
fakeredis>=2.23.0
```
- [ ] **Step 2:** Create the venv and install:
```bash
cd backend && python3 -m venv .venv && .venv/bin/pip install -q -r requirements-dev.txt
```
- [ ] **Step 3:** Run the existing suite to get a baseline: `.venv/bin/python -m pytest -q`. Record the result in the report. If a test fails at baseline, record which one and why. Do not fix unrelated failures; report them.
- [ ] **Step 4:** Create `backend/tests/conftest.py`. It gives HTTP-level tests an app client with an in-memory DB and fake Redis. `get_redis` is created in Task 3; until then this fixture only overrides `get_db`. Write it so it already imports and overrides `get_redis` when present:
```python
import pytest_asyncio
from httpx import AsyncClient, ASGITransport
from sqlalchemy.ext.asyncio import create_async_engine, async_sessionmaker, AsyncSession
from app.main import app
from app.database import Base, get_db

@pytest_asyncio.fixture
async def db_sessionmaker():
    engine = create_async_engine("sqlite+aiosqlite:///:memory:")
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)
    yield async_sessionmaker(engine, class_=AsyncSession, expire_on_commit=False)
    await engine.dispose()

@pytest_asyncio.fixture
async def client(db_sessionmaker):
    async def _get_db():
        async with db_sessionmaker() as s:
            yield s
    app.dependency_overrides[get_db] = _get_db
    try:
        from app.core.redis_client import get_redis
        import fakeredis.aioredis
        fake = fakeredis.aioredis.FakeRedis(decode_responses=True)
        app.dependency_overrides[get_redis] = lambda: fake
    except ImportError:
        pass
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as c:
        yield c
    app.dependency_overrides.clear()
```
- [ ] **Step 5:** Run `.venv/bin/python -m pytest -q`. It should match the baseline from Step 3.
- [ ] **Step 6:** Commit: `test(backend): venv, dev requirements and app client fixture`.

---

### Task 2: bcrypt MPIN hashing

**Files:**
- Modify: `backend/app/core/security.py` (`get_password_hash`, `verify_password`)
- Test: `backend/tests/test_auth_security.py`

- [ ] **Step 1: Write the failing test.** Append it:
```python
def test_mpin_hash_is_bcrypt_and_salted_per_hash():
    a = get_password_hash("4826")
    b = get_password_hash("4826")
    assert a.startswith("$2")          # bcrypt
    assert a != b                      # random salt per hash
    assert verify_password("4826", a) and verify_password("4826", b)
```
- [ ] **Step 2:** Run `.venv/bin/python -m pytest -q tests/test_auth_security.py`. Expected: FAIL. Today's hash is a static-salt SHA-256 hex digest.
- [ ] **Step 3: Implement.** Replace the two functions:
```python
from passlib.context import CryptContext
_pwd = CryptContext(schemes=["bcrypt"], deprecated="auto")

def get_password_hash(password: str) -> str:
    return _pwd.hash(password)

def verify_password(plain_password: str, hashed_password: str) -> bool:
    try:
        return _pwd.verify(plain_password, hashed_password)
    except ValueError:
        return False
```
Remove the now-unused `hashlib` import. If passlib warns about the `bcrypt` version (`__about__`), pin `bcrypt==4.0.1` in `requirements.txt` and say so in the report.
- [ ] **Step 4:** Run the test file. Expected: PASS.
- [ ] **Step 5:** Commit: `fix(auth): bcrypt MPIN hashing with per-hash salt`.

---

### Task 3: OtpSender + Redis-backed OTP store

**Files:**
- Create: `backend/app/core/redis_client.py`
- Create: `backend/app/core/otp_sender.py`
- Rewrite: `backend/app/core/otp_service.py`
- Modify: `backend/app/config.py`, `backend/app/main.py` (`/health`)
- Test: `backend/tests/test_otp_service.py` (new). Also replace the two OTP tests in `tests/test_auth_security.py`, which use the in-memory store being deleted. Keep the MPIN tests.

**Interfaces produced:**
- `get_redis() -> redis.asyncio.Redis`, a FastAPI dependency backed by one module-level client from `settings.REDIS_URL` with `decode_responses=True`.
- `class OtpSender(Protocol): async def send(self, phone: str, code: str) -> None`.
- `StaticOtpSender(code)`. `send` does nothing.
- `get_otp_sender() -> OtpSender`. Returns `StaticOtpSender(settings.OTP_STATIC_CODE)` when `OTP_STATIC_ENABLED`, else raises `HTTPException(503, "OTP service not configured")`.
- `class OtpService(redis)` with:
  - `async def issue(phone) -> str` raises `OtpRateLimited` on the 4th call within 300 s
  - `async def verify(phone, otp) -> bool`
- Redis keys:
  - `otp:{phone}` is a hash of `code` and `fails`, TTL `OTP_EXPIRE_SECONDS`.
  - `otp_rl:{phone}` is a counter, TTL 300.
  - 5 wrong verifies delete the code.
- Settings: `OTP_STATIC_ENABLED: bool = True` and `OTP_STATIC_CODE: str = "123456"`. Delete `OTP_MOCK_MODE`.

- [ ] **Step 1: Write the failing tests** in `tests/test_otp_service.py`:
```python
import pytest, fakeredis.aioredis
from app.core.otp_service import OtpService, OtpRateLimited

@pytest.fixture
def svc():
    return OtpService(fakeredis.aioredis.FakeRedis(decode_responses=True), static_code="123456")

@pytest.mark.asyncio
async def test_issue_returns_static_code_and_verify_consumes_it(svc):
    assert await svc.issue("9876500001") == "123456"
    assert await svc.verify("9876500001", "123456") is True
    assert await svc.verify("9876500001", "123456") is False  # single use

@pytest.mark.asyncio
async def test_fourth_send_in_window_is_rate_limited(svc):
    for _ in range(3):
        await svc.issue("9876500002")
    with pytest.raises(OtpRateLimited):
        await svc.issue("9876500002")

@pytest.mark.asyncio
async def test_five_wrong_attempts_burn_the_code(svc):
    await svc.issue("9876500003")
    for _ in range(5):
        assert await svc.verify("9876500003", "000000") is False
    assert await svc.verify("9876500003", "123456") is False
```
Also add `tests/test_health.py`:
```python
import pytest
@pytest.mark.asyncio
async def test_health_reports_static_otp_mode(client):
    r = await client.get("/health")
    assert r.json()["otp_mode"] == "static"
```
- [ ] **Step 2:** Run them. Expected: import errors / FAIL.
- [ ] **Step 3: Implement.**
  - `OtpService.__init__(self, redis, static_code: str | None)`: the code is `static_code` when set, else `f"{secrets.randbelow(10**6):06d}"`.
  - `issue`:
    - `INCR otp_rl:{phone}`, and set `EXPIRE 300` when the result is 1.
    - If the counter is over 3, raise `OtpRateLimited`.
    - Otherwise `HSET otp:{phone} code=<c> fails=0` and `EXPIRE OTP_EXPIRE_SECONDS`, then return the code.
  - `verify`:
    - Read the hash. If it's missing, return False.
    - If the code matches (`secrets.compare_digest`), delete the key and return True.
    - Otherwise `HINCRBY fails 1`. When the count reaches 5 or more, delete the key. Return False.
  - Health: add `"otp_mode": "static" if settings.OTP_STATIC_ENABLED else "provider"`.
- [ ] **Step 4:** Run the full suite. Expected: green. `auth.py` still imports the old `otp_service`; update its import so the module loads. Task 4 rewrites those endpoints anyway.
- [ ] **Step 5:** Commit: `feat(auth): swappable OTP sender with static code and Redis-backed OTP state`.

---

### Task 4: Signup / login / refresh / logout endpoints

**Files:**
- Create: `backend/app/models/refresh_token.py`. Register it in `app/models/__init__.py`.
- Rewrite: `backend/app/schemas/auth.py`, `backend/app/api/v1/endpoints/auth.py`
- Modify: `backend/app/core/security.py` (token helpers), `backend/app/config.py` (`ACCESS_TOKEN_EXPIRE_MINUTES = 60`, `REFRESH_TOKEN_EXPIRE_MINUTES = 60*24*90`, `AUTH_PASS_EXPIRE_MINUTES = 10`)
- Delete: the `/mpin/setup` and `/mpin/verify` endpoints and their tests in `tests/test_auth.py`. Rewrite `test_auth.py` against the new endpoints.
- Test: `backend/tests/test_auth_flow.py` (new)

**Interfaces produced (JSON uses snake_case):**
- `POST /api/v1/auth/otp/send` `{phone}` → `200 {sent: true, cooldown_s: 30}`. Errors: `400` for a bad phone, `429` for the rate limit.
- `POST /api/v1/auth/otp/verify` `{phone, otp}`:
  - New number → `200 {status: "NEW", signup_pass}`
  - Existing number → `200 {status: "EXISTING", login_pass}`
  - Wrong code → `400 {detail: "OTP_INVALID"}`
- `POST /api/v1/auth/signup` `{signup_pass, shop_name, owner_name, mandi_name, mpin}` → `200 AuthSession`. `409` if the phone already exists. The new shop gets a `uuid4` id.
- `POST /api/v1/auth/login` `{login_pass, mpin}`:
  - Success → `200 AuthSession`.
  - Wrong MPIN → `401 {detail: {"code": "MPIN_INVALID", "attempts_left": n}}`.
  - Fifth wrong MPIN → `401 {detail: {"code": "PASS_BURNED"}}`, and the pass is unusable.
  - Count failures in Redis key `mpin_fail:{jti}`.
- `POST /api/v1/auth/refresh` `{refresh_token}` → `200 {access_token, refresh_token}`. A replayed, revoked or expired token → `401`.
- `POST /api/v1/auth/logout` `{refresh_token}` → `204`. It revokes the token and is idempotent.
- `AuthSession` = `{access_token, refresh_token, shop: {id, shop_name, owner_name, mandi_name, phone_number}}`.
- Passes are JWTs of `type` `signup_pass` or `login_pass` with `phone` and `jti`. Single use: store `pass_used:{jti}` in Redis with a TTL equal to the pass lifetime, and reject the pass if that key exists.
- `RefreshToken` table: `jti` (PK), `user_id`, `expires_at` (ms), `revoked_at` (ms, nullable).
  - Refreshing revokes the old jti and inserts a new one.
  - Presenting a revoked jti → `401`.

- [ ] **Step 1: Write the failing tests** in `tests/test_auth_flow.py`. They use the `client` fixture and the static code "123456".
```python
import pytest
P = "9876512345"

async def otp(client, phone=P):
    await client.post("/api/v1/auth/otp/send", json={"phone": phone})
    return (await client.post("/api/v1/auth/otp/verify", json={"phone": phone, "otp": "123456"})).json()

async def signup(client, phone=P, mpin="4826"):
    v = await otp(client, phone)
    return await client.post("/api/v1/auth/signup", json={
        "signup_pass": v["signup_pass"], "shop_name": "गणपति ट्रेडर्स",
        "owner_name": "रमेश", "mandi_name": "मथुरा मंडी", "mpin": mpin})

@pytest.mark.asyncio
async def test_new_number_signs_up_and_gets_server_shop_id(client):
    v = await otp(client)
    assert v["status"] == "NEW"
    r = await signup(client, phone="9876500011")
    body = r.json()
    assert r.status_code == 200 and len(body["shop"]["id"]) == 36
    assert body["access_token"] and body["refresh_token"]

@pytest.mark.asyncio
async def test_existing_number_logs_in_with_mpin(client):
    await signup(client)
    v = await otp(client)
    assert v["status"] == "EXISTING"
    r = await client.post("/api/v1/auth/login", json={"login_pass": v["login_pass"], "mpin": "4826"})
    assert r.status_code == 200 and r.json()["shop"]["shop_name"] == "गणपति ट्रेडर्स"

@pytest.mark.asyncio
async def test_wrong_mpin_counts_down_then_burns_pass(client):
    await signup(client)
    v = await otp(client)
    for left in [4, 3, 2, 1]:
        r = await client.post("/api/v1/auth/login", json={"login_pass": v["login_pass"], "mpin": "0000"})
        assert r.status_code == 401 and r.json()["detail"]["attempts_left"] == left
    r = await client.post("/api/v1/auth/login", json={"login_pass": v["login_pass"], "mpin": "0000"})
    assert r.json()["detail"]["code"] == "PASS_BURNED"
    r = await client.post("/api/v1/auth/login", json={"login_pass": v["login_pass"], "mpin": "4826"})
    assert r.status_code == 401

@pytest.mark.asyncio
async def test_pass_is_single_use(client):
    v = await otp(client, "9876500022")
    body = {"signup_pass": v["signup_pass"], "shop_name": "अ", "owner_name": "ब", "mandi_name": "स", "mpin": "1111"}
    assert (await client.post("/api/v1/auth/signup", json=body)).status_code == 200
    assert (await client.post("/api/v1/auth/signup", json=body)).status_code == 401

@pytest.mark.asyncio
async def test_refresh_rotates_and_rejects_replay(client):
    s = (await signup(client)).json()
    r1 = await client.post("/api/v1/auth/refresh", json={"refresh_token": s["refresh_token"]})
    assert r1.status_code == 200 and r1.json()["refresh_token"] != s["refresh_token"]
    replay = await client.post("/api/v1/auth/refresh", json={"refresh_token": s["refresh_token"]})
    assert replay.status_code == 401

@pytest.mark.asyncio
async def test_logout_revokes_refresh_token(client):
    s = (await signup(client)).json()
    assert (await client.post("/api/v1/auth/logout", json={"refresh_token": s["refresh_token"]})).status_code == 204
    assert (await client.post("/api/v1/auth/refresh", json={"refresh_token": s["refresh_token"]})).status_code == 401

@pytest.mark.asyncio
async def test_access_token_carries_shop_id_for_sync(client):
    s = (await signup(client)).json()
    from app.core.security import decode_token
    assert decode_token(s["access_token"])["shop_id"] == s["shop"]["id"]
```
- [ ] **Step 2:** Run `.venv/bin/python -m pytest -q tests/test_auth_flow.py`. Expected: FAIL.
- [ ] **Step 3: Implement** the endpoints, schemas, model and token helpers exactly as described under "Interfaces produced".
  - The `shop_profiles` row gets `shop_name`, `owner_name`, `mandi_name` and `phone_number`. The `users` row gets `role="OWNER"` and `mpin_hash=get_password_hash(mpin)`.
  - Validate the MPIN as exactly 4 digits and the phone as 10 digits starting 6–9. Otherwise return 422 (pydantic).
  - Keep `get_current_user_payload` unchanged. The sync endpoints depend on it.
- [ ] **Step 4:** Run the full backend suite. Expected: green, and the sync isolation tests are untouched.
- [ ] **Step 5:** Commit: `feat(auth): OTP-gated signup/login with MPIN, rotating refresh tokens and logout`.

---

### Task 5: Production config for HTTPS (files only — no deploy)

**Files:**
- Modify: `backend/docker-compose.prod.yml`
- Create: `backend/.env.prod.example`
- Create: `backend/deploy/Caddyfile.mandi-api`
- Modify: `backend/scripts/deploy_prod.sh`
- Modify: `backend/.gitignore` (add `.env.prod`)

- [ ] **Step 1: Compose file.**
  - Change the api `ports` to `"127.0.0.1:8050:8000"`, so it's only reachable through Caddy.
  - Replace every hard-coded secret (`DATABASE_URL`, `SECRET_KEY`, `POSTGRES_PASSWORD`) with `env_file: .env.prod` / `${VAR}` references.
  - Remove `OTP_MOCK_MODE`.
  - Add `OTP_STATIC_ENABLED=${OTP_STATIC_ENABLED:-true}`.
- [ ] **Step 2: `.env.prod.example`.** List every variable with placeholder values (`SECRET_KEY=change-me-64-hex`, and so on) and a comment: "copy to .env.prod on the VPS; never commit".
- [ ] **Step 3: `deploy/Caddyfile.mandi-api`.** Same shape as `projects/workout-tracker/Caddyfile`:
```
mandi-api.appworx.co.in {
    encode gzip zstd
    header {
        X-Content-Type-Options nosniff
        Referrer-Policy no-referrer
    }
    reverse_proxy 127.0.0.1:8050 {
        header_up X-Real-IP {remote_host}
    }
}
```
- [ ] **Step 4: Deploy script.**
  - Fix the rsync source in `deploy_prod.sh` to the script-relative backend dir (`"$(cd "$(dirname "$0")/.." && pwd)/"`) and exclude `.venv` and `.env*`.
  - Change the final echo URL to `https://mandi-api.appworx.co.in`.
- [ ] **Step 5:** Run `docker compose -f backend/docker-compose.prod.yml config -q`. It must exit 0. Then run the backend suite. Expected: green.
- [ ] **Step 6:** Commit: `chore(deploy): prod API behind Caddy at mandi-api.appworx.co.in, secrets out of compose`.

> Actually deploying, adding DNS and rotating the leaked DB password are **outside this plan**. They need Vivek's go-ahead via `/project-deploy`.

---

### Task 6: Ktor AuthApi in core-data

**Files:**
- Modify: `gradle/libs.versions.toml`: add `ktor = "3.0.1"`, plus the libraries `ktor-client-core`, `ktor-client-content-negotiation`, `ktor-serialization-kotlinx-json`, `ktor-client-okhttp`, `ktor-client-darwin` and `ktor-client-mock` (`io.ktor:<name>`, `version.ref = "ktor"`).
- Modify: `core-data/build.gradle.kts`:
  - `commonMain`: core, content-negotiation, serialization
  - `androidMain`: okhttp
  - `jvmMain`: okhttp
  - `iosMain`: darwin
  - `commonTest`/`jvmTest`: mock
- Create: `core-data/src/commonMain/kotlin/com/appwork/mandisamiti/data/auth/AuthApi.kt`
- Create: `core-data/src/commonMain/kotlin/com/appwork/mandisamiti/data/auth/AuthDtos.kt`
- Test: `core-data/src/jvmTest/kotlin/com/appwork/mandisamiti/data/auth/AuthApiTest.kt`

**Interfaces produced:**
```kotlin
sealed interface OtpVerifyResult {
    data class NewShop(val signupPass: String) : OtpVerifyResult
    data class ExistingShop(val loginPass: String) : OtpVerifyResult
}
@Serializable data class ShopDto(val id: String, @SerialName("shop_name") val shopName: String,
    @SerialName("owner_name") val ownerName: String? = null, @SerialName("mandi_name") val mandiName: String? = null,
    @SerialName("phone_number") val phoneNumber: String? = null)
@Serializable data class AuthSessionDto(@SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String, val shop: ShopDto)
sealed class AuthError(message: String) : Exception(message) {
    object OtpInvalid : AuthError("OTP_INVALID")
    object RateLimited : AuthError("RATE_LIMITED")
    data class MpinInvalid(val attemptsLeft: Int) : AuthError("MPIN_INVALID")
    object PassBurned : AuthError("PASS_BURNED")
    object SessionExpired : AuthError("SESSION_EXPIRED")
    data class Network(val causeMessage: String) : AuthError("NETWORK")
}
class AuthApi(private val http: HttpClient, private val baseUrl: String) {
    suspend fun sendOtp(phone: String): Result<Unit>
    suspend fun verifyOtp(phone: String, otp: String): Result<OtpVerifyResult>
    suspend fun signup(signupPass: String, shopName: String, ownerName: String, mandiName: String, mpin: String): Result<AuthSessionDto>
    suspend fun login(loginPass: String, mpin: String): Result<AuthSessionDto>
    suspend fun refresh(refreshToken: String): Result<Pair<String, String>> // access, refresh
    suspend fun logout(refreshToken: String): Result<Unit>
}
fun mandiHttpClient(engine: HttpClientEngine? = null): HttpClient // installs ContentNegotiation(json { ignoreUnknownKeys = true })
```
Error mapping:
- `400 OTP_INVALID` → `OtpInvalid`; `429` → `RateLimited`.
- `401` with `MPIN_INVALID` → `MpinInvalid(n)`; with `PASS_BURNED` → `PassBurned`.
- `401` on refresh → `SessionExpired`.
- An IO exception → `Network`.

- [ ] **Step 1: Write the failing tests** in `AuthApiTest.kt` with `MockEngine`. Cover:
  - `verifyOtp` maps `{"status":"NEW","signup_pass":"p"}` to `NewShop("p")` and `EXISTING` to `ExistingShop`.
  - `login` maps a 401 `{"detail":{"code":"MPIN_INVALID","attempts_left":3}}` to `MpinInvalid(3)` and `PASS_BURNED` to `PassBurned`.
  - `signup` posts the snake_case body. Assert the request JSON has `signup_pass`, `shop_name`, `owner_name`, `mandi_name` and `mpin`, and that the response parses into an `AuthSessionDto` with the shop id.
  - `refresh` 401 → `SessionExpired`.
  - The engine throwing `IOException` → `Network`.
  - Every call hits `"$baseUrl/api/v1/auth/<path>"`.
- [ ] **Step 2:** Run `./gradlew :core-data:jvmTest --tests "*AuthApiTest*"`. Expected: FAIL (unresolved).
- [ ] **Step 3:** Implement `AuthApi`, the DTOs and `mandiHttpClient` (engine `null` → the platform default).
- [ ] **Step 4:** Run `./gradlew jvmTest` and `./gradlew :composeApp:compileDebugKotlinAndroid`. Expected: green. If Ktor 3.0.1 fails to resolve against Kotlin 2.0.20, use the newest 3.0.x that does and say so.
- [ ] **Step 5:** Commit: `feat(app): Ktor AuthApi with typed auth errors`.

---

### Task 7: SessionStore + local wipe

**Files:**
- Create: `core-data/src/commonMain/kotlin/com/appwork/mandisamiti/data/auth/SessionStore.kt` (the interface + `InMemorySessionStore`)
- Create: `core-data/src/androidMain/kotlin/com/appwork/mandisamiti/data/auth/AndroidSessionStore.kt`
- Modify: `core-database/.../AppDatabase.sq`. Add queries only, no table changes.
- Create: `core-data/src/commonMain/kotlin/com/appwork/mandisamiti/data/auth/LocalDataWiper.kt`
- Test: `core-data/src/jvmTest/kotlin/com/appwork/mandisamiti/data/auth/LocalDataWiperTest.kt`

**Interfaces produced:**
```kotlin
data class Session(val shopId: String, val accessToken: String, val refreshToken: String)
interface SessionStore {
    fun current(): Session?
    fun save(session: Session)
    fun updateTokens(accessToken: String, refreshToken: String)
    fun clear()
}
class InMemorySessionStore : SessionStore
class LocalDataWiper(private val database: AppDatabase) { suspend fun wipeAll() }
```
`AndroidSessionStore(context)`:
- Creates or loads an AES/GCM key `mandisamiti_session` in `AndroidKeyStore`.
- Encrypts `shopId|access|refresh` (JSON) and stores Base64 `iv + ciphertext` in a private SharedPreferences file `mandisamiti_session`.
- If decryption fails (key lost), `current()` returns null and clears the file.
- No `androidx.security` dependency.

Add these to `AppDatabase.sq` (DELETE statements, not schema changes):
```sql
wipeRevisions:
DELETE FROM entryRevisionEntity;
wipeCashTransactions:
DELETE FROM cashTransactionEntity;
wipeDeals:
DELETE FROM dealEntity;
wipeCommodities:
DELETE FROM commodityEntity;
wipeParties:
DELETE FROM partyEntity;
wipeShopProfiles:
DELETE FROM shopProfileEntity;
```
`wipeAll()` runs them in that order (children first) inside one `database.transaction {}`.

- [ ] **Step 1: Write the failing test.** Seed a shop, a party, a deal (through `saveDeal`, so it writes a revision) and a cash entry. Call `wipeAll()`. Assert every table count is 0. Add a second test: `InMemorySessionStore` save → current → updateTokens → clear.
- [ ] **Step 2:** Run it. Expected: FAIL.
- [ ] **Step 3:** Implement. Then run `./gradlew :core-database:verifyCommonMainAppDatabaseMigration`. It must stay green, because only queries were added.
- [ ] **Step 4:** Run `./gradlew jvmTest` and `./gradlew :composeApp:compileDebugKotlinAndroid`. Expected: green. `AndroidSessionStore` is only compile-checked here; it's verified on the device in Task 11.
- [ ] **Step 5:** Commit: `feat(app): Keystore-encrypted session store and local data wipe`.

---

### Task 8: AuthRepository + RegisterViewModel on the real flow

**Files:**
- Create: `core-data/src/commonMain/kotlin/com/appwork/mandisamiti/data/auth/AuthRepository.kt`
- Rewrite: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/auth/RegisterViewModel.kt`
- Test: `core-data/src/jvmTest/.../auth/AuthRepositoryTest.kt` and `composeApp/src/jvmTest/.../ui/auth/RegisterViewModelTest.kt` (new)

**Interfaces produced:**
```kotlin
class AuthRepository(
    private val api: AuthApi,
    private val sessionStore: SessionStore,
    private val shopProfileRepository: ShopProfileRepository,
    private val wiper: LocalDataWiper,
    private val clock: Clock = Clock.System,
) {
    suspend fun sendOtp(phone: String): Result<Unit>
    suspend fun verifyOtp(phone: String, otp: String): Result<OtpVerifyResult>
    suspend fun signup(pass: String, shopName: String, ownerName: String, mandiName: String, mpin: String): Result<Session>
    suspend fun login(pass: String, mpin: String): Result<Session>
}
```
`signup`/`login` on success:
1. If the locally stored shop profile's id ≠ the server shop id, call `wiper.wipeAll()`. This covers both "first signup wipes the demo data" and "another shop's data never mixes in". Same shop → keep local data, which has pending entries.
2. `saveShopProfile(ShopProfile(id = shop.id, shopName, ownerName ?: "", mandiName ?: "", phoneNumber ?: phone, pinHash = "", createdAt = now, updatedAt = now, syncStatus = 1))`
3. `sessionStore.save(Session(shop.id, access, refresh))`

The steps in `RegisterViewModel` become:
`enum class AuthStep { PHONE, OTP, NEW_SHOP, ENTER_MPIN }`
- **PHONE:**
  - Validate the phone, then `sendOtp`.
  - Success → OTP, with a 30 s resend cooldown.
  - `RateLimited` → "बहुत बार OTP माँगा गया, 5 मिनट बाद कोशिश करें" ("too many OTP requests, try again in 5 minutes").
- **OTP:**
  - 6 digits, then `verifyOtp`.
  - `NewShop` → NEW_SHOP (keep the pass).
  - `ExistingShop` → ENTER_MPIN.
  - `OtpInvalid` → "OTP गलत है" ("OTP is wrong").
- **NEW_SHOP:**
  - Shop name ≥ 3 chars, owner name ≥ 2, mandi name optional. MPIN and confirm must both be 4 digits and equal.
  - Then `signup` → `isRegistrationComplete = true` plus the existing TTS greeting.
- **ENTER_MPIN:**
  - 4 digits, then `login`.
  - `MpinInvalid(n)` → "MPIN गलत है — n कोशिश बाकी" ("MPIN is wrong — n tries left").
  - `PassBurned` → back to PHONE with "बहुत गलत MPIN — दोबारा OTP लें" ("too many wrong MPINs — get a new OTP").
- **Any step:** `Network` → "नेटवर्क नहीं है — दोबारा कोशिश करें" ("no network — try again").
- The constructor takes `AuthRepository`, `SoundboxTtsManager` and `viewModelScope`.
- Delete the fake `verifyOtp` behaviour and the hard-coded `id = "shop_default"`. Keep `isClickThrottled`, but make it take an injectable clock (`clock: Clock = Clock.System`). Tests pass a fixed clock and advance it 1 s between taps.

- [ ] **Step 1: Write the failing tests.**
  - `AuthRepositoryTest` (MockEngine `AuthApi`, real in-memory database):
    - Signup wipes pre-existing `shop_default` data and saves the new profile + session.
    - Login to the same shop id keeps the existing parties.
    - Login to a different shop id wipes them.
  - `RegisterViewModelTest` (fake `AuthRepository` via MockEngine):
    - PHONE → OTP → NEW_SHOP → complete.
    - PHONE → OTP → ENTER_MPIN with a wrong MPIN shows "2 कोशिश बाकी" when `attempts_left` is 2.
    - `PassBurned` returns to PHONE.
    - `RateLimited` shows the 5-minute message.
- [ ] **Step 2:** Run them. Expected: FAIL.
- [ ] **Step 3:** Implement.
- [ ] **Step 4:** Run `./gradlew jvmTest`. Expected: green.
- [ ] **Step 5:** Commit: `feat(auth): real OTP + MPIN flow; signup/login adopt the server shop id`.

---

### Task 9: RegisterScreen for the new steps

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/auth/RegisterScreen.kt`

- [ ] **Step 1: Restructure the screen** around `AuthStep`. Reuse the existing composables, styling, keypad and inputs:
  - **PHONE:** the phone field only (the shop fields move out) and a "OTP भेजें" ("send OTP") button.
  - **OTP:** unchanged, but resend uses the VM cooldown.
  - **NEW_SHOP:** the shop name / owner name / mandi name fields (moved from step 1, keeping the `उदा.` placeholders) and MPIN + confirm, with a "दुकान बनाएँ" ("create shop") button.
  - **ENTER_MPIN:** a single 4-digit MPIN entry with a "लॉगिन करें" ("log in") button and the attempts-left error.
  - Back navigation: OTP → PHONE; NEW_SHOP / ENTER_MPIN → PHONE (a new OTP is needed anyway).
- [ ] **Step 2:** Remove imports left unused by the restructure. NGDL rules apply: tokens only, no emoji, buttons ≥ 48dp, no fixed widths that would overflow at 390dp.
- [ ] **Step 3:** Run `./gradlew jvmTest` and `./gradlew :composeApp:compileDebugKotlinAndroid`. Expected: green.
- [ ] **Step 4:** Commit: `feat(auth): phone-first login screens for new and existing shops`.

---

### Task 10: App wiring — session decides start screen; no `shop_default`; guarded logout

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/App.kt`
- Modify: `composeApp/src/androidMain/kotlin/com/appwork/mandisamiti/MainActivity.kt`, plus the iOS/JVM entry points that call `App(...)`
- Modify: `composeApp/build.gradle.kts`: `buildConfig`-style base URL. Use an `expect val apiBaseUrl: String`, with the Android actual reading `BuildConfig.API_BASE_URL`:
  - release `https://mandi-api.appworx.co.in`
  - debug from `local.properties` `mandi.apiBaseUrl`, default `http://10.0.2.2:8000`
- Create: `composeApp/src/commonMain/kotlin/com/appwork/mandisamiti/ui/settings/LogoutUseCase.kt`
- Test: `composeApp/src/jvmTest/.../ui/settings/LogoutUseCaseTest.kt` and `composeApp/src/jvmTest/.../NoDefaultShopIdTest.kt`

**Interfaces produced:**
```kotlin
sealed interface LogoutResult { data class Blocked(val pendingCount: Long) : LogoutResult; object LoggedOut : LogoutResult }
class LogoutUseCase(private val syncEngine: SyncEngine, private val authApi: AuthApi,
                    private val sessionStore: SessionStore, private val wiper: LocalDataWiper) {
    suspend operator fun invoke(): LogoutResult
}
```
`LogoutUseCase` order:
1. `pending = syncEngine.getPendingCount()`. If it's above 0, return `Blocked(pending)` and change nothing. The Sync plan will run a sync before this check.
2. `authApi.logout(refresh)`, best effort: a network failure does not block, because the local token is cleared anyway.
3. `wiper.wipeAll()`
4. `sessionStore.clear()`
5. Return `LoggedOut`.

- [ ] **Step 1: Write the failing tests.**
  - `LogoutUseCaseTest`:
    - With 1 pending revision → `Blocked(1)`, the session is still present and the data is still there.
    - With 0 pending → `LoggedOut`, the tables are empty and the session is null.
    - Logout still succeeds when the API returns a network error.
  - `NoDefaultShopIdTest`: walk `File("src/commonMain")` and `File("../core-data/src/commonMain")`. The jvmTest working directory is the `composeApp` module dir; assert that both dirs exist. Assert no `.kt` file contains `"shop_default"`.
- [ ] **Step 2:** Run them. Expected: FAIL.
- [ ] **Step 3: Implement.**
  - `App(...)` gains `sessionStore: SessionStore` and `authApi: AuthApi`. `MainActivity` builds `AndroidSessionStore`, `mandiHttpClient()` and `AuthApi(client, apiBaseUrl)`.
  - Start screen: `sessionStore.current()` non-null → Home, otherwise Register. Compute it synchronously, without `LaunchedEffect`, to avoid a Register flash.
  - Every ViewModel that took `shopId` receives `session.shopId`. Delete `val shopId = "shop_default"`.
  - Settings "Sign out" calls `LogoutUseCase`:
    - `Blocked(n)` → a Snackbar: "$n प्रविष्टियाँ अभी सर्वर पर नहीं गईं — नेटवर्क मिलने पर लॉगआउट करें" ("$n entries haven't reached the server yet — log out when you have network").
    - `LoggedOut` → the Register screen at PHONE.
  - `SyncEngine` still has no API client here (`apiClient = null`), so its pending count reflects local unsynced revisions. That is correct for now: logout is blocked until the Sync plan ships, unless the phone has no entries.
- [ ] **Step 4:** Run `./gradlew jvmTest` and `./gradlew :composeApp:compileDebugKotlinAndroid`. Expected: green.
- [ ] **Step 5:** Commit: `feat(app): session-driven start, server shop id everywhere, sync-guarded logout`.

---

### Task 11: Verify, document, record

**Files:** `docs/MANDISAMITI-MASTER-SPEC.md` (feature matrix); the vault review page and `log.md`

- [ ] **Step 1:** Run the full checks:
  - `cd backend && .venv/bin/python -m pytest -q`
  - `./gradlew jvmTest :core-database:verifyCommonMainAppDatabaseMigration :composeApp:compileDebugKotlinAndroid`
  Record the counts.
- [ ] **Step 2: Local end-to-end without a phone.**
  - Start the API locally: `cd backend && OTP_STATIC_ENABLED=true REDIS_URL=redis://localhost:6379/0 .venv/bin/uvicorn app.main:app --port 8000`. This needs a local Redis. If none is running, use `docker run -d -p 6379:6379 redis:7-alpine` and say so.
  - With `curl`: send OTP → verify → signup → login with a wrong MPIN (attempts left) → login → refresh → refresh replay (401) → logout.
  - Record the outputs.
- [ ] **Step 3: Phone check (deferred until the Redmi is connected and unlocked).** Record these as "not yet verified":
  - Signup on the Redmi wipes the demo data.
  - Restarting the app goes straight to Home.
  - Sign out is blocked with N pending.
  - With no entries, sign out wipes and returns to login.
  - The screens in light and dark theme at phone width.
- [ ] **Step 4: Spec matrix.**
  - "Authentication & Sign Out" → `🟡 Done in code (static OTP), phone test pending`.
  - Add a row "Server-issued shop id" → `🟡 Done in code`.
- [ ] **Step 5: Vault.** On `mandisamiti-product-review-2026-10-08.md`:
  - Mark the fake OTP, the plaintext MPIN, the sign-out overwrite and `shop_default` as fixed in code, with the commit SHAs.
  - Note that static OTP must be switched to a real sender before the first paying shop.
  - Append to `log.md`. Run `python3 scripts/vault_lint.py` from the hub root; it must report 0 errors.
- [ ] **Step 6:** Commit (MandiSamiti repo, spec only): `docs(spec): accounts status after plan`.
