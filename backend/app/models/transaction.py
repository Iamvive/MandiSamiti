import uuid
import time
from sqlalchemy import Column, String, Integer, BigInteger, ForeignKey
from sqlalchemy.orm import relationship
from app.database import Base

class CashTransaction(Base):
    __tablename__ = "cash_transactions"

    id = Column(String, primary_key=True, default=lambda: str(uuid.uuid4()))
    shop_id = Column(String, ForeignKey("shop_profiles.id"), nullable=False, index=True)
    party_id = Column(String, ForeignKey("parties.id"), nullable=True)
    deal_id = Column(String, ForeignKey("deals.id"), nullable=True)
    transaction_type = Column(String(20), nullable=False)  # UDHAR_GIVEN, JAMA_RECEIVED, INTEREST_ADDED, DISCOUNT_GIVEN
    amount_paisa = Column(BigInteger, nullable=False)
    payment_mode = Column(String(20), default="CASH")
    transaction_date = Column(BigInteger, default=lambda: int(time.time() * 1000), index=True)
    voice_note_uri = Column(String, nullable=True)
    remarks = Column(String, nullable=True)
    category = Column(String(50), default="TRADE_PAYMENT")  # TRADE_PAYMENT, EXPENSE, CASH_DEPOSIT, DRAWING
    soundbox_broadcasted = Column(Integer, default=0)
    
    is_void = Column(Integer, default=0)
    void_reason = Column(String, nullable=True)
    revision = Column(Integer, default=1)
    is_deleted = Column(Integer, default=0)
    sync_version = Column(Integer, default=1)
    server_seq = Column(BigInteger, nullable=False, default=0, server_default="0", index=True)
    created_at = Column(BigInteger, default=lambda: int(time.time() * 1000))
    updated_at = Column(BigInteger, default=lambda: int(time.time() * 1000), onupdate=lambda: int(time.time() * 1000))

    shop = relationship("ShopProfile", back_populates="transactions")

