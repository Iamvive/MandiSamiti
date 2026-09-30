import random
import time
from typing import Dict, Tuple
from app.config import settings

# In-memory OTP store (fallback when Redis is not running locally)
_memory_otp_store: Dict[str, Tuple[str, float]] = {}

class OTPService:
    @staticmethod
    def generate_otp(phone_number: str) -> str:
        # Generate 6 digit OTP
        if settings.OTP_MOCK_MODE and (phone_number.endswith("9999") or phone_number == "9876543210"):
            otp = "123456"
        else:
            otp = f"{random.randint(100000, 999999)}"

        expires_at = time.time() + settings.OTP_EXPIRE_SECONDS
        _memory_otp_store[phone_number] = (otp, expires_at)
        return otp

    @staticmethod
    def verify_otp(phone_number: str, otp: str) -> bool:
        record = _memory_otp_store.get(phone_number)
        if not record:
            return False
        
        saved_otp, expires_at = record
        if time.time() > expires_at:
            _memory_otp_store.pop(phone_number, None)
            return False

        if saved_otp == otp.strip():
            _memory_otp_store.pop(phone_number, None)
            return True

        return False

otp_service = OTPService()
