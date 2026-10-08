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
