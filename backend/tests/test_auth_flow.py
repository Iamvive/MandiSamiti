import pytest

P = "9876512345"
URL = "/api/v1/auth"


async def signup(client, phone=P, mpin="4826", shop="गणपति ट्रेडर्स"):
    return await client.post(f"{URL}/signup", json={
        "phone": phone, "shop_name": shop,
        "owner_name": "रमेश", "mandi_name": "मथुरा मंडी", "mpin": mpin})


async def login(client, mpin, phone=P):
    return await client.post(f"{URL}/login", json={"phone": phone, "mpin": mpin})


@pytest.mark.asyncio
async def test_new_number_signs_up_and_gets_server_shop_id(client):
    r = await client.post(f"{URL}/check-phone", json={"phone": "9876500011"})
    assert r.status_code == 200 and r.json() == {"is_new_user": True}
    r = await signup(client, phone="9876500011")
    body = r.json()
    assert r.status_code == 200 and len(body["shop"]["id"]) == 36
    assert body["access_token"] and body["refresh_token"]


@pytest.mark.asyncio
async def test_existing_number_logs_in_with_mpin(client):
    await signup(client)
    r = await client.post(f"{URL}/check-phone", json={"phone": P})
    assert r.json() == {"is_new_user": False}
    r = await login(client, "4826")
    assert r.status_code == 200 and r.json()["shop"]["shop_name"] == "गणपति ट्रेडर्स"


@pytest.mark.asyncio
async def test_wrong_mpin_counts_down(client):
    await signup(client)
    for left in [9, 8, 7]:
        r = await login(client, "0000")
        assert r.status_code == 401
        assert r.json()["detail"] == {"code": "MPIN_INVALID", "attempts_left": left}
    assert (await login(client, "4826")).status_code == 200


@pytest.mark.asyncio
async def test_unknown_number_cannot_log_in(client):
    r = await login(client, "4826", phone="9876500099")
    assert r.status_code == 401 and r.json()["detail"]["code"] == "MPIN_INVALID"


@pytest.mark.asyncio
async def test_refresh_rotates_and_rejects_replay(client):
    s = (await signup(client)).json()
    r1 = await client.post(f"{URL}/refresh", json={"refresh_token": s["refresh_token"]})
    assert r1.status_code == 200 and r1.json()["refresh_token"] != s["refresh_token"]
    replay = await client.post(f"{URL}/refresh", json={"refresh_token": s["refresh_token"]})
    assert replay.status_code == 401


@pytest.mark.asyncio
async def test_logout_revokes_refresh_token(client):
    s = (await signup(client)).json()
    assert (await client.post(f"{URL}/logout", json={"refresh_token": s["refresh_token"]})).status_code == 204
    assert (await client.post(f"{URL}/refresh", json={"refresh_token": s["refresh_token"]})).status_code == 401


@pytest.mark.asyncio
async def test_access_token_carries_shop_id_for_sync(client):
    s = (await signup(client)).json()
    from app.core.security import decode_token
    assert decode_token(s["access_token"])["shop_id"] == s["shop"]["id"]


@pytest.mark.asyncio
async def test_bad_phone_is_400(client):
    for path, body in [("check-phone", {"phone": "12345"}),
                       ("login", {"phone": "12345", "mpin": "1111"}),
                       ("signup", {"phone": "12345", "shop_name": "अ", "owner_name": "ब",
                                   "mandi_name": "स", "mpin": "1111"})]:
        r = await client.post(f"{URL}/{path}", json=body)
        assert r.status_code == 400 and r.json()["detail"] == "INVALID_PHONE"


@pytest.mark.asyncio
async def test_signup_rejects_bad_mpin_and_existing_phone(client):
    r = await signup(client, phone="9876500033", mpin="12a4")
    assert r.status_code == 422
    assert (await signup(client)).status_code == 200
    r = await signup(client, shop="कब्ज़ा")
    assert r.status_code == 409 and r.json()["detail"] == "PHONE_EXISTS"
    assert (await login(client, "4826")).json()["shop"]["shop_name"] == "गणपति ट्रेडर्स"


@pytest.mark.asyncio
async def test_access_token_cannot_be_refreshed_and_logout_is_idempotent(client):
    s = (await signup(client)).json()
    assert (await client.post(f"{URL}/refresh", json={"refresh_token": s["access_token"]})).status_code == 401
    for _ in range(2):
        assert (await client.post(f"{URL}/logout", json={"refresh_token": s["refresh_token"]})).status_code == 204


@pytest.mark.asyncio
async def test_account_locks_after_too_many_wrong_mpins(client):
    await signup(client)
    for _ in range(10):
        assert (await login(client, "0000")).status_code == 401
    r = await login(client, "4826")  # correct MPIN, but the phone is locked
    assert r.status_code == 401 and r.json()["detail"] == {"code": "ACCOUNT_LOCKED"}


@pytest.mark.asyncio
async def test_successful_login_resets_per_phone_counter(client):
    await signup(client)
    for _ in range(3):  # 27 failures + 3 successes would lock if the counter were not reset
        for _ in range(9):
            await login(client, "0000")
        assert (await login(client, "4826")).status_code == 200


@pytest.mark.asyncio
async def test_refresh_token_is_not_a_bearer_token(client):
    s = (await signup(client)).json()
    r = await client.get("/api/v1/sync/pull", headers={"Authorization": f"Bearer {s['refresh_token']}"})
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
    r = await client.post(f"{URL}/check-phone", json={"phone": P})
    assert r.json() == {"is_new_user": True}
    r = await signup(client, mpin="1357", shop="नई दुकान")
    assert r.status_code == 200, r.text
    body = r.json()
    assert body["shop"]["id"] == "legacy-shop-1"
    assert body["shop"]["shop_name"] == "नई दुकान" and body["shop"]["mandi_name"] == "मथुरा मंडी"
    r = await login(client, "1357")
    assert r.status_code == 200 and r.json()["shop"]["id"] == "legacy-shop-1"


@pytest.mark.asyncio
async def test_bcrypt_user_signup_still_409(client):
    await signup(client)
    assert (await signup(client, mpin="1111")).status_code == 409


async def _legacy_shop_name(db_sessionmaker):
    from app.models.user import ShopProfile
    async with db_sessionmaker() as s:
        return (await s.get(ShopProfile, "legacy-shop-1")).shop_name


@pytest.mark.parametrize("role,is_active", [("OWNER", False), ("MUNIM", True), ("VIEWER", True)])
@pytest.mark.asyncio
async def test_legacy_disabled_or_non_owner_is_refused_and_shop_unchanged(client, db_sessionmaker, role, is_active):
    await _seed_legacy_user(db_sessionmaker, P, "sha", role=role, is_active=is_active)
    r = await signup(client, shop="कब्ज़ा")
    assert r.status_code == 401 and r.json()["detail"] == {"code": "ACCOUNT_DISABLED"}
    assert await _legacy_shop_name(db_sessionmaker) == "पुरानी दुकान"
