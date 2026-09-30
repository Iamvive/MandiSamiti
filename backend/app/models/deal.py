import uuid
import time
from sqlalchemy import Column, String, Integer, Float, ForeignKey
from sqlalchemy.orm import relationship
from app.database import Base

class Deal(Base):
    __tablename__ = "deals"

    id = Column(String, primary_key=True, default=lambda: str(uuid.uuid4()))
    shop_id = Column(String, ForeignKey("shop_profiles.id"), nullable=False, index=True)
    farmer_id = Column(String, ForeignKey("parties.id"), nullable=False)
    buyer_id = Column(String, ForeignKey("parties.id"), nullable=False)
    commodity = Column(String, nullable=False)  # उदा. गेहूँ, चना, सरसों
    bags = Column(Integer, default=0)
    gross_weight = Column(Float, nullable=False)
    tare_weight = Column(Float, default=0.0)
    net_weight = Column(Float, nullable=False)
    rate = Column(Float, nullable=False)
    commission_rate = Column(Float, default=0.0)
    labour_charges = Column(Float, default=0.0)
    farmer_total = Column(Float, nullable=False)
    buyer_total = Column(Float, nullable=False)
    status = Column(String(20), default="COMPLETED")
    sync_version = Column(Integer, default=1)
    created_at = Column(Float, default=lambda: time.time(), index=True)
    updated_at = Column(Float, default=lambda: time.time())

    shop = relationship("ShopProfile", back_populates="deals")
