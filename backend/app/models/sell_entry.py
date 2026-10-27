import uuid
import time
from sqlalchemy import Column, String, BigInteger, Integer, ForeignKey
from app.database import Base

class SellEntry(Base):
    __tablename__ = "sell_entries"

    id = Column(String, primary_key=True, default=lambda: str(uuid.uuid4()))
    shop_id = Column(String, ForeignKey("shop_profiles.id"), nullable=False, index=True)
    buyer_id = Column(String, ForeignKey("parties.id"), nullable=False)
    commodity_id = Column(String, nullable=False)
    entry_date = Column(BigInteger, default=lambda: int(time.time() * 1000), index=True)
    bill_mode = Column(String(20), default="NORMAL")  # NORMAL | BILL_OF_SUPPLY
    party_kind = Column(String(20), default="MERCHANT")  # MERCHANT | CUSTOMER
    gstin = Column(String, nullable=True)

    bags_count = Column(BigInteger, default=0)
    weight_per_bag_grams = Column(BigInteger, default=0)
    total_weight_grams = Column(BigInteger, nullable=False, default=0)
    rate_paisa_per_quintal = Column(BigInteger, nullable=False, default=0)
    arat_bps = Column(BigInteger, default=0)
    tulak_paisa = Column(BigInteger, default=0)
    gross_paisa = Column(BigInteger, nullable=False, default=0)
    arat_paisa = Column(BigInteger, nullable=False, default=0)
    mandi_shulk_paisa = Column(BigInteger, nullable=False, default=0)
    total_receivable_paisa = Column(BigInteger, nullable=False, default=0)
    received_paisa = Column(BigInteger, default=0)
    balance_due_paisa = Column(BigInteger, nullable=False, default=0)

    vehicle_number = Column(String, nullable=True)
    invoice_number = Column(String, nullable=True)
    remarks = Column(String, nullable=True)

    is_void = Column(Integer, default=0)
    void_reason = Column(String, nullable=True)
    revision = Column(Integer, default=1)
    is_deleted = Column(Integer, default=0)
    sync_version = Column(Integer, default=1)
    server_seq = Column(BigInteger, nullable=False, default=0, server_default="0", index=True)
    created_at = Column(BigInteger, default=lambda: int(time.time() * 1000), index=True)
    updated_at = Column(BigInteger, default=lambda: int(time.time() * 1000), onupdate=lambda: int(time.time() * 1000))
