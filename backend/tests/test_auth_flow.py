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

# --- additional coverage ---

@pytest.mark.asyncio
async def test_bad_phone_is_400_and_wrong_otp_is_400(client):
    r = await client.post("/api/v1/auth/otp/send", json={"phone": "12345"})
    assert r.status_code == 400
    await client.post("/api/v1/auth/otp/send", json={"phone": P})
    r = await client.post("/api/v1/auth/otp/verify", json={"phone": P, "otp": "000000"})
    assert r.status_code == 400 and r.json()["detail"] == "OTP_INVALID"

@pytest.mark.asyncio
async def test_send_returns_cooldown_and_rate_limits(client):
    r = await client.post("/api/v1/auth/otp/send", json={"phone": P})
    assert r.status_code == 200 and r.json() == {"sent": True, "cooldown_s": 30}
    for _ in range(2):
        await client.post("/api/v1/auth/otp/send", json={"phone": P})
    assert (await client.post("/api/v1/auth/otp/send", json={"phone": P})).status_code == 429

@pytest.mark.asyncio
async def test_signup_rejects_bad_mpin_and_existing_phone(client):
    v = await otp(client, "9876500033")
    body = {"signup_pass": v["signup_pass"], "shop_name": "अ", "owner_name": "ब", "mandi_name": "स", "mpin": "12a4"}
    assert (await client.post("/api/v1/auth/signup", json=body)).status_code == 422
    await signup(client)
    # a signup_pass for a phone that now exists -> 409
    from app.core.security import create_pass_token
    stale, _ = create_pass_token("signup_pass", P)
    body = {"signup_pass": stale, "shop_name": "अ", "owner_name": "ब", "mandi_name": "स", "mpin": "1111"}
    assert (await client.post("/api/v1/auth/signup", json=body)).status_code == 409

@pytest.mark.asyncio
async def test_pass_types_are_not_interchangeable(client):
    v = await otp(client, "9876500044")
    r = await client.post("/api/v1/auth/login", json={"login_pass": v["signup_pass"], "mpin": "1111"})
    assert r.status_code == 401

@pytest.mark.asyncio
async def test_access_token_cannot_be_refreshed_and_logout_is_idempotent(client):
    s = (await signup(client)).json()
    assert (await client.post("/api/v1/auth/refresh", json={"refresh_token": s["access_token"]})).status_code == 401
    for _ in range(2):
        assert (await client.post("/api/v1/auth/logout", json={"refresh_token": s["refresh_token"]})).status_code == 204

@pytest.mark.asyncio
async def test_account_locks_after_too_many_wrong_mpins_across_passes(client):
    from app.core.security import create_pass_token
    await signup(client)
    for _ in range(2):  # 2 passes x 5 wrong = 10 per-phone failures
        lp, _ = create_pass_token("login_pass", P)
        for _ in range(5):
            r = await client.post("/api/v1/auth/login", json={"login_pass": lp, "mpin": "0000"})
            assert r.status_code == 401
    lp, _ = create_pass_token("login_pass", P)  # third fresh pass, correct MPIN
    r = await client.post("/api/v1/auth/login", json={"login_pass": lp, "mpin": "4826"})
    assert r.status_code == 401 and r.json()["detail"] == {"code": "ACCOUNT_LOCKED"}

@pytest.mark.asyncio
async def test_successful_login_resets_per_phone_counter(client):
    from app.core.security import create_pass_token
    await signup(client)
    for _ in range(2):
        lp, _ = create_pass_token("login_pass", P)
        for _ in range(4):
            await client.post("/api/v1/auth/login", json={"login_pass": lp, "mpin": "0000"})
        r = await client.post("/api/v1/auth/login", json={"login_pass": lp, "mpin": "4826"})
        assert r.status_code == 200  # 8 fails + 2 successes would lock if not reset

    lp, _ = create_pass_token("login_pass", P)
    for _ in range(4):
        await client.post("/api/v1/auth/login", json={"login_pass": lp, "mpin": "0000"})
    assert (await client.post("/api/v1/auth/login", json={"login_pass": lp, "mpin": "4826"})).status_code == 200

@pytest.mark.asyncio
async def test_pass_and_refresh_tokens_are_not_bearer_tokens(client):
    s = (await signup(client)).json()
    v = await otp(client)
    for tok in (v["login_pass"], s["refresh_token"]):
        r = await client.get("/api/v1/sync/pull", headers={"Authorization": f"Bearer {tok}"})
        assert r.status_code == 401

async def _seed_legacy_user(db_sessionmaker, phone, mpin_hash, role="OWNER", is_active=True):
    import hashlib
    from app.models.user import User, ShopProfile
    async with db_sessionmaker() as s:
        shop = ShopProfile(id="legacy-shop-1", shop_name="पुरानी दुकान", owner_name="पुराना", mandi_name="पुरानी मंडी", phone_number=phone)
        s.add(shop)
        await s.flush()
        s.add(User(phone_number=phone, name="पुराना", role=role, shop_id=shop.id, is_active=is_active,
                   mpin_hash=mpin_hash if mpin_hash != "sha" else hashlib.sha256(b"4826").hexdigest()))
        await s.commit()

@pytest.mark.parametrize("legacy_hash", ["sha", None])
@pytest.mark.asyncio
async def test_legacy_hash_user_resignups_keeping_shop_id_then_logs_in(client, db_sessionmaker, legacy_hash):
    await _seed_legacy_user(db_sessionmaker, P, legacy_hash)
    v = await otp(client)
    assert v["status"] == "NEW" and v["signup_pass"]
    r = await client.post("/api/v1/auth/signup", json={
        "signup_pass": v["signup_pass"], "shop_name": "नई दुकान", "owner_name": "रमेश",
        "mandi_name": "मथुरा मंडी", "mpin": "1357"})
    assert r.status_code == 200, r.text
    body = r.json()
    assert body["shop"]["id"] == "legacy-shop-1"
    assert body["shop"]["shop_name"] == "नई दुकान" and body["shop"]["mandi_name"] == "मथुरा मंडी"
    v2 = await otp(client)
    assert v2["status"] == "EXISTING"
    r = await client.post("/api/v1/auth/login", json={"login_pass": v2["login_pass"], "mpin": "1357"})
    assert r.status_code == 200 and r.json()["shop"]["id"] == "legacy-shop-1"

@pytest.mark.asyncio
async def test_bcrypt_user_signup_still_409(client):
    await signup(client)
    from app.core.security import create_pass_token
    sp, _ = create_pass_token("signup_pass", P)
    r = await client.post("/api/v1/auth/signup", json={
        "signup_pass": sp, "shop_name": "अ", "owner_name": "ब", "mandi_name": "स", "mpin": "1111"})
    assert r.status_code == 409


async def _legacy_shop_name(db_sessionmaker):
    from app.models.user import ShopProfile
    async with db_sessionmaker() as s:
        return (await s.get(ShopProfile, "legacy-shop-1")).shop_name

async def _signup_with_stale_pass(client, phone=P):
    from app.core.security import create_pass_token
    sp, _ = create_pass_token("signup_pass", phone)
    return await client.post("/api/v1/auth/signup", json={
        "signup_pass": sp, "shop_name": "कब्ज़ा", "owner_name": "चोर", "mandi_name": "कहीं", "mpin": "1111"})

@pytest.mark.parametrize("role,is_active", [("OWNER", False), ("MUNIM", True), ("VIEWER", True)])
@pytest.mark.asyncio
async def test_legacy_disabled_or_non_owner_is_refused_and_shop_unchanged(client, db_sessionmaker, role, is_active):
    await _seed_legacy_user(db_sessionmaker, P, "sha", role=role, is_active=is_active)
    await client.post("/api/v1/auth/otp/send", json={"phone": P})
    v = await client.post("/api/v1/auth/otp/verify", json={"phone": P, "otp": "123456"})
    assert v.status_code == 401 and v.json()["detail"] == {"code": "ACCOUNT_DISABLED"}
    r = await _signup_with_stale_pass(client)  # a signup pass obtained some other way
    assert r.status_code == 401 and r.json()["detail"] == {"code": "ACCOUNT_DISABLED"}
    assert await _legacy_shop_name(db_sessionmaker) == "पुरानी दुकान"
