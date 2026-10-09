import pytest
from sqlalchemy.ext.asyncio import create_async_engine, async_sessionmaker, AsyncSession
from app.database import Base
from app.models.user import ShopProfile
from app.core.sync_seq import lock_shop, next_seq

@pytest.mark.asyncio
async def test_next_seq_counts_per_shop_and_persists():
    engine = create_async_engine("sqlite+aiosqlite:///:memory:")
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)
    sm = async_sessionmaker(engine, class_=AsyncSession, expire_on_commit=False)
    async with sm() as db:
        db.add_all([
            ShopProfile(id="a", shop_name="A", phone_number="9000000001"),
            ShopProfile(id="b", shop_name="B", phone_number="9000000002"),
        ])
        await db.commit()

        a = await lock_shop(db, "a")
        assert [next_seq(a), next_seq(a)] == [1, 2]
        b = await lock_shop(db, "b")
        assert next_seq(b) == 1
        await db.commit()

    async with sm() as db:
        a = await lock_shop(db, "a")
        assert next_seq(a) == 3
        assert await lock_shop(db, "missing") is None
    await engine.dispose()
