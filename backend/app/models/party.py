import uuid
import time
from sqlalchemy import Column, String, Integer, Float, ForeignKey
from sqlalchemy.orm import relationship
from app.database import Base

class Party(Base):
    __tablename__ = "parties"

    id = Column(String, primary_key=True, default=lambda: str(uuid.uuid4()))
    shop_id = Column(String, ForeignKey("shop_profiles.id"), nullable=False, index=True)
    name = Column(String, nullable=False, index=True)
    phone = Column(String(15), nullable=True)
    role = Column(String(20), nullable=False)  # FARMER (किसान), BUYER (व्यापारी)
    village = Column(String, nullable=True)
    current_balance = Column(Float, default=0.0)  # Positive = Receivable, Negative = Payable
    sync_version = Column(Integer, default=1)
    updated_at = Column(Float, default=lambda: time.time())
    created_at = Column(Float, default=lambda: time.time())

    shop = relationship("ShopProfile", back_populates="parties")
