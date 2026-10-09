import uuid
import time
from sqlalchemy import Column, String, Integer, BigInteger, ForeignKey
from sqlalchemy.orm import relationship
from app.database import Base

class Deal(Base):
    __tablename__ = "deals"

    id = Column(String, primary_key=True, default=lambda: str(uuid.uuid4()))
    shop_id = Column(String, ForeignKey("shop_profiles.id"), nullable=False, index=True)
    farmer_id = Column(String, ForeignKey("parties.id"), nullable=False)
    buyer_id = Column(String, ForeignKey("parties.id"), nullable=True)
    commodity = Column(String, nullable=False)  # उदा. गेहूँ, चना, सरसों (or commodity_id)
    deal_status = Column(String(20), default="SETTLED")  # PENDING_SETTLEMENT, SETTLED, CANCELLED
    deal_date = Column(BigInteger, default=lambda: int(time.time() * 1000), index=True)
    
    # Weight details (Grams)
    bags_count = Column(Integer, default=0)
    gross_weight_grams = Column(BigInteger, nullable=False, default=0)
    cut_weight_grams = Column(BigInteger, default=0)
    net_weight_grams = Column(BigInteger, nullable=False, default=0)

    # Pricing & Deductions (Paisa)
    rate_paisa_per_unit = Column(BigInteger, default=0)
    gross_amount_paisa = Column(BigInteger, default=0)
    farmer_commission_bps = Column(Integer, default=0)
    farmer_commission_paisa = Column(BigInteger, default=0)
    buyer_commission_paisa = Column(BigInteger, default=0)
    labour_charge_paisa = Column(BigInteger, default=0)
    weighing_charge_paisa = Column(BigInteger, default=0)
    other_deductions_paisa = Column(BigInteger, default=0)

    # Final Settlements (Paisa)
    net_farmer_payable_paisa = Column(BigInteger, nullable=False, default=0)
    net_buyer_receivable_paisa = Column(BigInteger, nullable=False, default=0)

    receipt_photo_uri = Column(String, nullable=True)
    voice_note_uri = Column(String, nullable=True)
    remarks = Column(String, nullable=True)

    is_void = Column(Integer, default=0)
    void_reason = Column(String, nullable=True)
    revision = Column(Integer, default=1)
    is_deleted = Column(Integer, default=0)
    sync_version = Column(Integer, default=1)
    server_seq = Column(BigInteger, nullable=False, default=0, server_default="0", index=True)
    created_at = Column(BigInteger, default=lambda: int(time.time() * 1000), index=True)
    updated_at = Column(BigInteger, default=lambda: int(time.time() * 1000), onupdate=lambda: int(time.time() * 1000))

    shop = relationship("ShopProfile", back_populates="deals")

