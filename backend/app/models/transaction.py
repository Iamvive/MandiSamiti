import uuid
import time
from sqlalchemy import Column, String, Integer, Float, ForeignKey
from sqlalchemy.orm import relationship
from app.database import Base

class CashTransaction(Base):
    __tablename__ = "cash_transactions"

    id = Column(String, primary_key=True, default=lambda: str(uuid.uuid4()))
    shop_id = Column(String, ForeignKey("shop_profiles.id"), nullable=False, index=True)
    party_id = Column(String, ForeignKey("parties.id"), nullable=True)
    type = Column(String(10), nullable=False)  # IN (आवक), OUT (जावक)
    amount = Column(Float, nullable=False)
    category = Column(String(50), default="TRADE_PAYMENT")  # TRADE_PAYMENT, EXPENSE, CASH_DEPOSIT, DRAWING
    notes = Column(String, nullable=True)
    soundbox_broadcasted = Column(Integer, default=0)
    sync_version = Column(Integer, default=1)
    timestamp = Column(Float, default=lambda: time.time(), index=True)
    created_at = Column(Float, default=lambda: time.time())
    updated_at = Column(Float, default=lambda: time.time())

    shop = relationship("ShopProfile", back_populates="transactions")
