import pytest
import pytest_asyncio
from fastapi import Request
from httpx import AsyncClient, ASGITransport
from sqlalchemy.ext.asyncio import create_async_engine, async_sessionmaker, AsyncSession
from app.main import app
from app.database import Base, get_db
from app.core.redis_client import get_redis
import fakeredis.aioredis
from app.core.security import get_current_user_payload
from app.models.user import ShopProfile


async def _test_sync_user(request: Request):
    shop_id = request.headers.get("X-Test-Shop")
    return {"shop_id": shop_id, "user_id": f"user-{shop_id}"}

@pytest_asyncio.fixture
async def db_sessionmaker():
    engine = create_async_engine("sqlite+aiosqlite:///:memory:")
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)
    yield async_sessionmaker(engine, class_=AsyncSession, expire_on_commit=False)
    await engine.dispose()

@pytest_asyncio.fixture
async def client(db_sessionmaker):
    async def _get_db():
        async with db_sessionmaker() as s:
            yield s
    app.dependency_overrides[get_db] = _get_db
    fake = fakeredis.aioredis.FakeRedis(decode_responses=True)
    app.dependency_overrides[get_redis] = lambda: fake
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as c:
        yield c
    app.dependency_overrides.clear()

@pytest_asyncio.fixture
async def auth_headers(db_sessionmaker):
    async with db_sessionmaker() as session:
        session.add(ShopProfile(id="shop-a", shop_name="Shop A", phone_number="9000000001"))
        await session.commit()
    app.dependency_overrides[get_current_user_payload] = _test_sync_user
    yield {"X-Test-Shop": "shop-a"}
    app.dependency_overrides.pop(get_current_user_payload, None)

@pytest_asyncio.fixture
async def other_shop_headers(db_sessionmaker):
    async with db_sessionmaker() as session:
        session.add(ShopProfile(id="shop-b", shop_name="Shop B", phone_number="9000000002"))
        await session.commit()
    app.dependency_overrides[get_current_user_payload] = _test_sync_user
    yield {"X-Test-Shop": "shop-b"}
    app.dependency_overrides.pop(get_current_user_payload, None)

@pytest.fixture
def buy_payload():
    return {
        "id": "buy-1",
        "shop_id": "shop-a",
        "farmer_id": "farmer-1",
        "commodity_id": "wheat",
        "entry_date": 1_000,
        "bags_count": 10,
        "weight_per_bag_grams": 50_000,
        "extra_weight_grams": 5_000,
        "total_weight_grams": 505_000,
        "rate_paisa_per_quintal": 250_000,
        "commission_bps": 200,
        "labour_per_bag_paisa": 500,
        "advance_paid_paisa": 100_000,
        "gross_paisa": 1_262_500,
        "commission_paisa": 25_250,
        "labour_paisa": 5_000,
        "net_payable_paisa": 1_132_250,
        "paid_paisa": 0,
        "vehicle_number": None,
        "slip_number": None,
        "remarks": None,
        "created_at": 1_000,
        "updated_at": 1_000,
        "is_deleted": 0,
        "sync_status": 0,
        "revision": 1,
        "is_void": 0,
        "void_reason": None,
    }

@pytest.fixture
def expense_payload():
    return {
        "id": "expense-1",
        "shop_id": "shop-a",
        "entry_date": 1_000,
        "category": "HAMMALI",
        "amount_paisa": 50_000,
        "paid_to": "Worker",
        "payment_mode": "CASH",
        "remarks": None,
        "created_at": 1_000,
        "updated_at": 1_000,
        "is_deleted": 0,
        "sync_status": 0,
        "revision": 1,
        "is_void": 0,
        "void_reason": None,
    }
