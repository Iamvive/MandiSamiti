import uuid
import time
from sqlalchemy import Column, String, BigInteger, Integer, ForeignKey
from app.database import Base

class LendEntry(Base):
    __tablename__ = "lend_entries"

    id = Column(String, primary_key=True, default=lambda: str(uuid.uuid4()))
    shop_id = Column(String, ForeignKey("shop_profiles.id"), nullable=False, index=True)
    party_id = Column(String, ForeignKey("parties.id"), nullable=False)
    lend_type = Column(String(20), default="MONEY")  # MONEY | GRAIN
    entry_date = Column(BigInteger, default=lambda: int(time.time() * 1000), index=True)
    amount_paisa = Column(BigInteger, default=0)
    commodity_id = Column(String, nullable=True)
    bags_count = Column(BigInteger, default=0)
    weight_grams = Column(BigInteger, default=0)
    expected_return_date = Column(BigInteger, nullable=True)
    slip_no = Column(BigInteger, nullable=True)
    notes = Column(String, nullable=True)
    status = Column(String(20), default="OPEN")  # OPEN | PARTIAL | SETTLED

    is_void = Column(Integer, default=0)
    void_reason = Column(String, nullable=True)
    revision = Column(Integer, default=1)
    is_deleted = Column(Integer, default=0)
    sync_version = Column(Integer, default=1)
    server_seq = Column(BigInteger, nullable=False, default=0, server_default="0", index=True)
    created_at = Column(BigInteger, default=lambda: int(time.time() * 1000), index=True)
    updated_at = Column(BigInteger, default=lambda: int(time.time() * 1000), onupdate=lambda: int(time.time() * 1000))


class LendSettlement(Base):
    __tablename__ = "lend_settlements"

    id = Column(String, primary_key=True, default=lambda: str(uuid.uuid4()))
    shop_id = Column(String, ForeignKey("shop_profiles.id"), nullable=False, index=True)
    lend_id = Column(String, ForeignKey("lend_entries.id"), nullable=False, index=True)
    settled_at = Column(BigInteger, default=lambda: int(time.time() * 1000), index=True)
    amount_paisa = Column(BigInteger, default=0)
    principal_paisa = Column(BigInteger, nullable=False, default=0, server_default="0")
    interest_paisa = Column(BigInteger, nullable=False, default=0, server_default="0")
    calculated_interest_paisa = Column(BigInteger, nullable=False, default=0, server_default="0")
    weight_grams = Column(BigInteger, default=0)
    bags_count = Column(BigInteger, default=0)

    is_void = Column(Integer, default=0)
    void_reason = Column(String, nullable=True)
    revision = Column(Integer, default=1)
    sync_version = Column(Integer, default=1)
    server_seq = Column(BigInteger, nullable=False, default=0, server_default="0", index=True)
    created_at = Column(BigInteger, default=lambda: int(time.time() * 1000), index=True)
    updated_at = Column(BigInteger, default=lambda: int(time.time() * 1000), onupdate=lambda: int(time.time() * 1000))
