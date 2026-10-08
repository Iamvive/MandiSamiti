import random
import time
from typing import Dict, Tuple
from app.config import settings

# In-memory OTP store: phone -> (otp, expires_at, failed_attempts)
_memory_otp_store: Dict[str, Tuple[str, float, int]] = {}
# Rate limit store: phone -> list of request timestamps in last 5 mins
_rate_limit_store: Dict[str, list] = {}

class OTPService:
    @staticmethod
    def is_rate_limited(phone_number: str) -> bool:
        now = time.time()
        window_start = now - 300  # 5 minutes window
        timestamps = _rate_limit_store.get(phone_number, [])
        # Keep only timestamps within window
        valid_timestamps = [t for t in timestamps if t > window_start]
        _rate_limit_store[phone_number] = valid_timestamps
        return len(valid_timestamps) >= 3

    @staticmethod
    def record_request(phone_number: str):
        now = time.time()
        timestamps = _rate_limit_store.get(phone_number, [])
        timestamps.append(now)
        _rate_limit_store[phone_number] = timestamps

    @staticmethod
    def generate_otp(phone_number: str) -> str:
        if OTPService.is_rate_limited(phone_number):
            raise ValueError("Too many OTP requests. Please try again after 5 minutes.")

        OTPService.record_request(phone_number)

        # Generate 6 digit OTP
        if settings.OTP_MOCK_MODE and (phone_number.endswith("9999") or phone_number == "9876543210"):
            otp = "123456"
        else:
            otp = f"{random.randint(100000, 999999)}"

        expires_at = time.time() + settings.OTP_EXPIRE_SECONDS
        _memory_otp_store[phone_number] = (otp, expires_at, 0)
        return otp

    @staticmethod
    def verify_otp(phone_number: str, otp: str) -> bool:
        record = _memory_otp_store.get(phone_number)
        if not record:
            return False
        
        saved_otp, expires_at, failed_attempts = record
        if time.time() > expires_at:
            _memory_otp_store.pop(phone_number, None)
            return False

        if failed_attempts >= 5:
            # Lockout after 5 failed attempts
            _memory_otp_store.pop(phone_number, None)
            return False

        if saved_otp == otp.strip():
            _memory_otp_store.pop(phone_number, None)
            return True

        # Increment failed attempts
        _memory_otp_store[phone_number] = (saved_otp, expires_at, failed_attempts + 1)
        return False

otp_service = OTPService()

