import pytest, fakeredis.aioredis
from app.core.otp_service import OtpService, OtpRateLimited

@pytest.fixture
def svc():
    return OtpService(fakeredis.aioredis.FakeRedis(decode_responses=True), static_code="123456")

@pytest.mark.asyncio
async def test_issue_returns_static_code_and_verify_consumes_it(svc):
    assert await svc.issue("9876500001") == "123456"
    assert await svc.verify("9876500001", "123456") is True
    assert await svc.verify("9876500001", "123456") is False  # single use

@pytest.mark.asyncio
async def test_fourth_send_in_window_is_rate_limited(svc):
    for _ in range(3):
        await svc.issue("9876500002")
    with pytest.raises(OtpRateLimited):
        await svc.issue("9876500002")

@pytest.mark.asyncio
async def test_five_wrong_attempts_burn_the_code(svc):
    await svc.issue("9876500003")
    for _ in range(5):
        assert await svc.verify("9876500003", "000000") is False
    assert await svc.verify("9876500003", "123456") is False
