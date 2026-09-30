import uuid
import time
from sqlalchemy import Column, String, Integer, Float, Boolean, ForeignKey
from sqlalchemy.orm import relationship
from app.database import Base

class ShopProfile(Base):
    __tablename__ = "shop_profiles"

    id = Column(String, primary_key=True, default=lambda: str(uuid.uuid4()))
    shop_name = Column(String, nullable=False)
    mandi_name = Column(String, nullable=True)
    mandi_license_number = Column(String, nullable=True)
    soundbox_enabled = Column(Boolean, default=True)
    soundbox_voice_lang = Column(String, default="hi-IN")
    created_at = Column(Float, default=lambda: time.time())
    updated_at = Column(Float, default=lambda: time.time())

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
    created_at = Column(Float, default=lambda: time.time())
    updated_at = Column(Float, default=lambda: time.time())

    shop = relationship("ShopProfile", back_populates="users")
