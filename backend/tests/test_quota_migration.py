import pytest
from app.models.user import Shop, ShopProfile

def test_shop_has_quota_attributes():
    shop = Shop(
        shop_name="Test Kisan Traders",
        phone_number="9876543210",
        monthly_bill_limit=50,
        emergency_extensions_used=0,
        emergency_extension_bills=0,
        plan_tier="FREE",
    )
    assert shop.monthly_bill_limit == 50
    assert shop.emergency_extensions_used == 0
    assert shop.emergency_extension_bills == 0
    assert shop.plan_tier == "FREE"
    assert shop.plan_valid_until is None
    assert Shop is ShopProfile
