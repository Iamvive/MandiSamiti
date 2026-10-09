import uuid
import time
from sqlalchemy import Column, String, Integer, Float, Boolean, BigInteger, ForeignKey
from sqlalchemy.orm import relationship
from app.database import Base

class ShopProfile(Base):
    __tablename__ = "shop_profiles"

    id = Column(String, primary_key=True, default=lambda: str(uuid.uuid4()))
    shop_name = Column(String, nullable=False)
    owner_name = Column(String, nullable=True)
    mandi_name = Column(String, nullable=True)
    shop_number = Column(String, nullable=True)
    phone_number = Column(String(15), nullable=True)
    pin_hash = Column(String, nullable=True)
    default_monthly_interest_rate = Column(Float, default=1.5)
    is_sound_enabled = Column(Integer, default=1)
    soundbox_voice_lang = Column(String, default="hi-IN")
    created_at = Column(BigInteger, default=lambda: int(time.time() * 1000))
    updated_at = Column(BigInteger, default=lambda: int(time.time() * 1000), onupdate=lambda: int(time.time() * 1000))
    # Per-shop change counter. Every row a push accepts takes the next value as its server_seq.
    last_seq = Column(BigInteger, nullable=False, default=0, server_default="0")

    users = relationship("User", back_populates="shop")
    parties = relationship("Party", back_populates="shop")
    deals = relationship("Deal", back_populates="shop")
    transactions = relationship("CashTransaction", back_populates="shop")

class User(Base):
    __tablename__ = "users"

    id = Column(String, primary_key=True, default=lambda: str(uuid.uuid4()))
    phone_number = Column(String(15), unique=True, index=True, nullable=False)
    name = Column(String, nullable=False)
    role = Column(String(20), default="OWNER")  # OWNER, MUNIM, VIEWER
    shop_id = Column(String, ForeignKey("shop_profiles.id"), nullable=True)
    mpin_hash = Column(String, nullable=True)
    is_active = Column(Boolean, default=True)
    created_at = Column(BigInteger, default=lambda: int(time.time() * 1000))
    updated_at = Column(BigInteger, default=lambda: int(time.time() * 1000), onupdate=lambda: int(time.time() * 1000))

    shop = relationship("ShopProfile", back_populates="users")

