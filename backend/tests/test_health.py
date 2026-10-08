import pytest
@pytest.mark.asyncio
async def test_health_reports_static_otp_mode(client):
    r = await client.get("/health")
    assert r.json()["otp_mode"] == "static"
