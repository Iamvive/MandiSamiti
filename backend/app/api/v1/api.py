from fastapi import APIRouter
from app.api.v1.endpoints import auth, sync, soundbox

api_router = APIRouter()

api_router.include_router(auth.router, prefix="/auth", tags=["Authentication & MPIN"])
api_router.include_router(sync.router, prefix="/sync", tags=["Offline-to-Cloud Sync"])
api_router.include_router(soundbox.router, prefix="/soundbox", tags=["Soundbox Voice Broadcast"])
