import pytest
from app.models.user import ShopProfile
from app.models.deal import Deal
from app.models.buy_entry import BuyEntry
from app.models.sell_entry import SellEntry
import time
from datetime import datetime, timezone

@pytest.mark.asyncio
async def test_quota_endpoint_returns_correct_free_limit(client, auth_headers):
    response = await client.get("/api/v1/billing/quota", headers=auth_headers)
    assert response.status_code == 200
    data = response.json()
    assert data["plan_tier"] == "FREE"
    assert data["bills_used_this_month"] == 0
    assert data["monthly_limit"] == 50
    assert data["emergency_extensions_used"] == 0
    assert data["emergency_extensions_max"] == 2
    assert data["emergency_bills_added"] == 0
    assert data["effective_limit"] == 50
    assert data["can_create_bill"] is True
    assert data["is_emergency_extension_available"] is True

@pytest.mark.asyncio
async def test_quota_endpoint_counts_current_month_deals(client, auth_headers, db_sessionmaker):
    now = datetime.now(timezone.utc)
    current_month_ts = int(datetime(now.year, now.month, 15, 12, 0, 0, tzinfo=timezone.utc).timestamp() * 1000)
    
    # Prev month timestamp
    if now.month == 1:
        prev_month_ts = int(datetime(now.year - 1, 12, 15, 12, 0, 0, tzinfo=timezone.utc).timestamp() * 1000)
    else:
        prev_month_ts = int(datetime(now.year, now.month - 1, 15, 12, 0, 0, tzinfo=timezone.utc).timestamp() * 1000)

    async with db_sessionmaker() as session:
        # Add deals/entries for shop-a
        session.add(BuyEntry(id="b1", shop_id="shop-a", farmer_id="p1", commodity_id="c1", created_at=current_month_ts, entry_date=current_month_ts))
        session.add(SellEntry(id="s1", shop_id="shop-a", buyer_id="p2", commodity_id="c1", created_at=current_month_ts, entry_date=current_month_ts))
        session.add(Deal(id="d1", shop_id="shop-a", farmer_id="p1", commodity="c1", created_at=current_month_ts, deal_date=current_month_ts))
        
        # Add past month deals (should not count)
        session.add(BuyEntry(id="b_past", shop_id="shop-a", farmer_id="p1", commodity_id="c1", created_at=prev_month_ts, entry_date=prev_month_ts))
        # Add deal for other shop (should not count)
        session.add(BuyEntry(id="b_other", shop_id="shop-b", farmer_id="p1", commodity_id="c1", created_at=current_month_ts, entry_date=current_month_ts))
        # Add void/deleted entries (should not count)
        session.add(BuyEntry(id="b_void", shop_id="shop-a", farmer_id="p1", commodity_id="c1", created_at=current_month_ts, is_void=1))
        session.add(BuyEntry(id="b_del", shop_id="shop-a", farmer_id="p1", commodity_id="c1", created_at=current_month_ts, is_deleted=1))

        await session.commit()

    response = await client.get("/api/v1/billing/quota", headers=auth_headers)
    assert response.status_code == 200
    data = response.json()
    assert data["bills_used_this_month"] == 3

@pytest.mark.asyncio
async def test_claim_emergency_extension_lifecycle(client, auth_headers):
    # 1. Claim first extension
    res1 = await client.post("/api/v1/billing/claim-emergency-extension", headers=auth_headers)
    assert res1.status_code == 200
    data1 = res1.json()
    assert data1["emergency_extensions_used"] == 1
    assert data1["emergency_bills_added"] == 10
    assert data1["effective_limit"] == 60
    assert data1["is_emergency_extension_available"] is True

    # Check GET /quota reflects updated extension
    res_get1 = await client.get("/api/v1/billing/quota", headers=auth_headers)
    assert res_get1.status_code == 200
    assert res_get1.json()["effective_limit"] == 60

    # 2. Claim second extension
    res2 = await client.post("/api/v1/billing/claim-emergency-extension", headers=auth_headers)
    assert res2.status_code == 200
    data2 = res2.json()
    assert data2["emergency_extensions_used"] == 2
    assert data2["emergency_bills_added"] == 20
    assert data2["effective_limit"] == 70
    assert data2["is_emergency_extension_available"] is False

    # 3. Third claim rejected with 400
    res3 = await client.post("/api/v1/billing/claim-emergency-extension", headers=auth_headers)
    assert res3.status_code == 400
    assert "Maximum emergency extensions reached" in res3.json()["detail"]
