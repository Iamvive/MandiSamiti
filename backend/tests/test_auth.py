import pytest
from app.core.otp_service import otp_service
from app.core.security import create_access_token, decode_token

def test_otp_generation_and_verification():
    phone = "9876543210"
    otp = otp_service.generate_otp(phone)
    assert len(otp) == 6
    assert otp_service.verify_otp(phone, otp) is True
    # Second verify fails because OTP is consumed
    assert otp_service.verify_otp(phone, otp) is False

def test_jwt_token_generation_and_decoding():
    token = create_access_token(subject="user_123", shop_id="shop_456", role="OWNER")
    assert isinstance(token, str)
    
    payload = decode_token(token)
    assert payload["sub"] == "user_123"
    assert payload["shop_id"] == "shop_456"
    assert payload["role"] == "OWNER"
    assert payload["type"] == "access"
