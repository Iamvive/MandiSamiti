import asyncio
import logging
from fastapi import Depends, FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from contextlib import asynccontextmanager
from sqlalchemy import text
from app.config import settings
from app.database import engine, Base, get_db
from app.api.v1.api import api_router
from app.core.redis_client import get_redis
from app.core.observability import RequestContextMiddleware, init_sentry, setup_logging

logger = logging.getLogger(__name__)
init_sentry()

@asynccontextmanager
async def lifespan(app: FastAPI):
    setup_logging()
    # Database initialization with retry loop for Docker / production startup
    for attempt in range(1, 15):
        try:
            async with engine.begin() as conn:
                await conn.run_sync(Base.metadata.create_all)
            logger.info("Database connected and schema initialized successfully.")
            break
        except Exception as e:
            logger.warning("Waiting for database connection (attempt %d/15)... error: %s", attempt, e)
            await asyncio.sleep(2)
    else:
        logger.error("Database unreachable after 14 attempts; starting anyway, /health/ready will fail.")
    yield

app = FastAPI(
    title=settings.PROJECT_NAME,
    version=settings.VERSION,
    description="Scalable, offline-first Cloud Backend API for MandiSamiti (Agri-Trade Ledger & Soundbox)",
    lifespan=lifespan
)

# CORS Middleware
if settings.BACKEND_CORS_ORIGINS:
    app.add_middleware(
        CORSMiddleware,
        allow_origins=settings.BACKEND_CORS_ORIGINS,
        allow_credentials=True,
        allow_methods=["*"],
        allow_headers=["*"],
    )
app.add_middleware(RequestContextMiddleware)

app.include_router(api_router, prefix=settings.API_V1_STR)

@app.get("/health", tags=["Health"])
async def health_check():
    return {
        "status": "healthy",
        "service": settings.PROJECT_NAME,
        "version": settings.VERSION,
    }

async def _check(awaitable) -> str:
    try:
        await asyncio.wait_for(awaitable, timeout=2)
        return "ok"
    except Exception:
        return "fail"

@app.get("/health/ready", tags=["Health"])
async def readiness_check(db=Depends(get_db), redis=Depends(get_redis)):
    """Readiness for the uptime monitor: Postgres and Redis both answer within 2s."""
    checks = {"db": await _check(db.execute(text("SELECT 1"))), "redis": await _check(redis.ping())}
    ready = all(v == "ok" for v in checks.values())
    return JSONResponse(
        status_code=200 if ready else 503,
        content={"status": "ready" if ready else "not_ready", "checks": checks},
    )
