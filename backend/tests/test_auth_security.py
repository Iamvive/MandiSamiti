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
