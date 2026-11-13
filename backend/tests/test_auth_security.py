import pytest
from app.core.security import get_password_hash, verify_password

def test_mpin_bcrypt_hash_verification():
    raw_pin = "4826"
    hashed = get_password_hash(raw_pin)
    assert hashed != raw_pin
    assert verify_password(raw_pin, hashed) is True
    assert verify_password("0000", hashed) is False


def test_mpin_hash_is_bcrypt_and_salted_per_hash():
    a = get_password_hash("4826")
    b = get_password_hash("4826")
    assert a.startswith("$2")          # bcrypt
    assert a != b                      # random salt per hash
    assert verify_password("4826", a) and verify_password("4826", b)


def test_secret_key_guard_in_prod_and_staging():
    from app.config import Settings
    with pytest.raises(ValueError, match="SECRET_KEY is the code default"):
        Settings(APP_ENV="prod", SECRET_KEY="mandisamiti-super-secure-production-jwt-key-32chars-min")
    with pytest.raises(ValueError, match="SECRET_KEY is the code default"):
        Settings(APP_ENV="staging", SECRET_KEY="change-me-short")


@pytest.mark.asyncio
async def test_unknown_phone_does_not_pollute_redis_or_lockout(client):
    from app.core.redis_client import get_redis
    redis = get_redis()
    fake_phone = "9999911223"
    await redis.delete(f"mpin_fail:{fake_phone}")

    for _ in range(5):
        r = await client.post("/api/v1/auth/login", json={"phone": fake_phone, "mpin": "1234"})
        assert r.status_code == 401
        assert r.json()["detail"]["code"] == "MPIN_INVALID"

    # Counter in Redis should not have been set/incremented for non-existent user
    val = await redis.get(f"mpin_fail:{fake_phone}")
    assert val is None


@pytest.mark.asyncio
async def test_viewer_role_blocked_from_sync_push(client, db_sessionmaker):
    from app.core.security import create_access_token
    from app.models.user import ShopProfile, User
    async with db_sessionmaker() as s:
        shop = ShopProfile(id="shop-viewer-test", shop_name="Viewer Test Shop", phone_number="9876543210")
        s.add(shop)
        await s.flush()
        user = User(id="user-viewer", phone_number="9876543210", name="Viewer User", role="VIEWER", shop_id=shop.id)
        s.add(user)
        await s.commit()

    token = create_access_token(subject="user-viewer", shop_id="shop-viewer-test", role="VIEWER")
    r = await client.post(
        "/api/v1/sync/push",
        headers={"Authorization": f"Bearer {token}"},
        json={"parties": [], "deals": [], "transactions": [], "revisions": []}
    )
    assert r.status_code == 403
    assert "READ_ONLY_ACCESS" in r.json()["detail"]
