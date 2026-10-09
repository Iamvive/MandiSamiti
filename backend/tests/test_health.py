import pytest
@pytest.mark.asyncio
async def test_health_does_not_expose_otp_mode(client):
    r = await client.get("/health")
    assert r.status_code == 200 and "otp_mode" not in r.json()
