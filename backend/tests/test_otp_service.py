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

@pytest.mark.asyncio
async def test_verify_without_issue_is_false_and_leaves_no_key(svc):
    assert await svc.verify("9876500004", "123456") is False
    assert await svc.redis.exists("otp:9876500004") == 0

@pytest.mark.asyncio
async def test_after_five_wrong_no_code_verifies_even_concurrently(svc):
    import asyncio
    await svc.issue("9876500005")
    res = await asyncio.gather(*[svc.verify("9876500005", "000000") for _ in range(10)])
    assert not any(res)
    assert await svc.verify("9876500005", "123456") is False

@pytest.mark.asyncio
async def test_concurrent_correct_verifies_succeed_once(svc):
    import asyncio
    await svc.issue("9876500006")
    res = await asyncio.gather(svc.verify("9876500006", "123456"), svc.verify("9876500006", "123456"))
    assert sorted(res) == [False, True]

@pytest.mark.asyncio
async def test_rate_limit_key_has_ttl(svc):
    await svc.issue("9876500007")
    assert await svc.redis.ttl("otp_rl:9876500007") > 0

@pytest.mark.asyncio
async def test_otp_key_keeps_ttl_after_wrong_attempt(svc):
    await svc.issue("9876500008")
    await svc.verify("9876500008", "000000")
    assert await svc.redis.ttl("otp:9876500008") > 0
