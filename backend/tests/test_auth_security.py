import pytest
from app.core.otp_service import OTPService, _rate_limit_store, _memory_otp_store
from app.core.security import get_password_hash, verify_password

def test_otp_rate_limiting():
    phone = "9899001122"
    _rate_limit_store.pop(phone, None)
    _memory_otp_store.pop(phone, None)

    # 3 requests allowed
    assert len(OTPService.generate_otp(phone)) == 6
    assert len(OTPService.generate_otp(phone)) == 6
    assert len(OTPService.generate_otp(phone)) == 6

    # 4th request must be rate-limited
    with pytest.raises(ValueError, match="Too many OTP requests"):
        OTPService.generate_otp(phone)

def test_otp_failed_attempts_lockout():
    phone = "9899001133"
    _rate_limit_store.pop(phone, None)
    _memory_otp_store.pop(phone, None)

    real_otp = OTPService.generate_otp(phone)

    # 5 wrong attempts
    for _ in range(5):
        assert OTPService.verify_otp(phone, "000000") is False

    # 6th attempt even with correct OTP should be locked out (record evicted)
    assert OTPService.verify_otp(phone, real_otp) is False

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
