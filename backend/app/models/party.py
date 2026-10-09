import uuid
import time
from sqlalchemy import Column, String, Integer, Float, BigInteger, ForeignKey
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
    monthly_interest_rate = Column(Float, nullable=True, default=1.5)
    photo_uri = Column(String, nullable=True)
    is_deleted = Column(Integer, default=0)
    sync_version = Column(Integer, default=1)
    server_seq = Column(BigInteger, nullable=False, default=0, server_default="0", index=True)
    created_at = Column(BigInteger, default=lambda: int(time.time() * 1000))
    updated_at = Column(BigInteger, default=lambda: int(time.time() * 1000), onupdate=lambda: int(time.time() * 1000))

    shop = relationship("ShopProfile", back_populates="parties")

