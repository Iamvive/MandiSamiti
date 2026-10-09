import logging
import secrets
import pytest
from app.config import Settings, DEFAULT_SECRET_KEY


@pytest.mark.parametrize("key", [DEFAULT_SECRET_KEY, "change-me-64-hex-character-secret-key-for-jwt", "short-but-random-1234"])
def test_prod_refuses_default_placeholder_or_short_secret(key):
    with pytest.raises(ValueError, match="SECRET_KEY"):
        Settings(APP_ENV="prod", SECRET_KEY=key, _env_file=None)


def test_prod_accepts_strong_secret():
    s = Settings(APP_ENV="prod", SECRET_KEY=secrets.token_hex(32), OTP_STATIC_ENABLED=False, _env_file=None)
    assert s.APP_ENV == "prod"


def test_dev_allows_default_secret():
    assert Settings(_env_file=None).SECRET_KEY == DEFAULT_SECRET_KEY


def test_prod_static_otp_logs_loud_warning(caplog):
    with caplog.at_level(logging.WARNING):
        Settings(APP_ENV="prod", SECRET_KEY=secrets.token_hex(32), OTP_STATIC_ENABLED=True, _env_file=None)
    assert any("OTP_STATIC_ENABLED" in r.message and r.levelno == logging.WARNING for r in caplog.records)


@pytest.mark.parametrize("env", [" PROD ", "production", "Prod", "prod "])
def test_prod_env_is_normalised(env):
    with pytest.raises(ValueError, match="SECRET_KEY"):
        Settings(APP_ENV=env, SECRET_KEY=DEFAULT_SECRET_KEY, _env_file=None)
