import uuid
import time
from sqlalchemy import Column, String, BigInteger, Integer, ForeignKey
from app.database import Base

class BuyEntry(Base):
    __tablename__ = "buy_entries"

    id = Column(String, primary_key=True, default=lambda: str(uuid.uuid4()))
    shop_id = Column(String, ForeignKey("shop_profiles.id"), nullable=False, index=True)
    farmer_id = Column(String, ForeignKey("parties.id"), nullable=False)
    commodity_id = Column(String, nullable=False)
    entry_date = Column(BigInteger, default=lambda: int(time.time() * 1000), index=True)

    bags_count = Column(BigInteger, default=0)
    weight_per_bag_grams = Column(BigInteger, default=0)
    extra_weight_grams = Column(BigInteger, default=0)
    total_weight_grams = Column(BigInteger, nullable=False, default=0)
    rate_paisa_per_quintal = Column(BigInteger, nullable=False, default=0)
    commission_bps = Column(BigInteger, default=0)
    labour_per_bag_paisa = Column(BigInteger, default=0)
    advance_paid_paisa = Column(BigInteger, default=0)
    gross_paisa = Column(BigInteger, nullable=False, default=0)
    commission_paisa = Column(BigInteger, nullable=False, default=0)
    labour_paisa = Column(BigInteger, nullable=False, default=0)
    net_payable_paisa = Column(BigInteger, nullable=False, default=0)
    paid_paisa = Column(BigInteger, default=0)

    vehicle_number = Column(String, nullable=True)
    slip_number = Column(String, nullable=True)
    remarks = Column(String, nullable=True)

    is_void = Column(Integer, default=0)
    void_reason = Column(String, nullable=True)
    revision = Column(Integer, default=1)
    is_deleted = Column(Integer, default=0)
    sync_version = Column(Integer, default=1)
    server_seq = Column(BigInteger, nullable=False, default=0, server_default="0", index=True)
    created_at = Column(BigInteger, default=lambda: int(time.time() * 1000), index=True)
    updated_at = Column(BigInteger, default=lambda: int(time.time() * 1000), onupdate=lambda: int(time.time() * 1000))
