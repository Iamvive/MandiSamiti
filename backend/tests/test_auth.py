from app.core.security import create_access_token, create_refresh_token, create_pass_token, decode_token


def test_jwt_token_generation_and_decoding():
    token = create_access_token(subject="user_123", shop_id="shop_456", role="OWNER")
    payload = decode_token(token)
    assert payload["sub"] == "user_123"
    assert payload["shop_id"] == "shop_456"
    assert payload["role"] == "OWNER"
    assert payload["type"] == "access"


def test_refresh_and_pass_tokens_carry_unique_jti():
    t1, j1, exp_ms = create_refresh_token("u1")
    t2, j2, _ = create_refresh_token("u1")
    assert j1 != j2 and decode_token(t1)["jti"] == j1 and decode_token(t1)["type"] == "refresh"
    assert exp_ms == decode_token(t1)["exp"] * 1000
    p, pj = create_pass_token("login_pass", "9876512345")
    d = decode_token(p)
    assert d["type"] == "login_pass" and d["phone"] == "9876512345" and d["jti"] == pj
