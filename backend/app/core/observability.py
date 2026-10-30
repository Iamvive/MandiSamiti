"""Logs, error tracking and ops alerts. The only module that knows about Sentry or the ops Telegram chat.

Every vendor is switched on by env (SENTRY_DSN, OPS_TELEGRAM_*); unset means off.
"""
import asyncio
import json
import logging
import re
import sys
import time
import uuid
from contextvars import ContextVar
from datetime import datetime, timezone

import httpx

from app.config import settings
from app.core.redis_client import get_redis

logger = logging.getLogger(__name__)

request_id_var: ContextVar[str | None] = ContextVar("request_id", default=None)
shop_id_var: ContextVar[str | None] = ContextVar("shop_id", default=None)

ALERT_DEDUPE_SECONDS = 600
_EXTRA_FIELDS = ("method", "path", "status", "duration_ms")


def bind_shop(shop_id: str | None) -> None:
    """Attach the authenticated shop to this request's logs and error events."""
    if shop_id:
        shop_id_var.set(str(shop_id))


# --- Structured logs ---

class JsonFormatter(logging.Formatter):
    def format(self, record: logging.LogRecord) -> str:
        line = {
            "ts": datetime.fromtimestamp(record.created, timezone.utc).isoformat(),
            "level": record.levelname,
            "logger": record.name,
            "msg": record.getMessage(),
            "request_id": request_id_var.get(),
            "shop_id": shop_id_var.get(),
        }
        for field in _EXTRA_FIELDS:
            if hasattr(record, field):
                line[field] = getattr(record, field)
        if record.exc_info:
            line["exc"] = self.formatException(record.exc_info)
        return json.dumps(line, default=str)


def setup_logging() -> None:
    """JSON lines on stdout for everything, uvicorn included; ERROR and above also go to the ops chat."""
    handler = logging.StreamHandler(sys.stdout)
    handler.setFormatter(JsonFormatter())
    root = logging.getLogger()
    root.handlers = [handler, OpsAlertHandler()]
    root.setLevel(logging.INFO)
    for name in ("uvicorn", "uvicorn.error"):
        logging.getLogger(name).handlers = []
        logging.getLogger(name).propagate = True
    logging.getLogger("uvicorn.access").disabled = True  # RequestContextMiddleware logs requests


class RequestContextMiddleware:
    """Pure ASGI so the shop_id bound by auth is still visible when the request line is logged."""

    def __init__(self, app):
        self.app = app

    async def __call__(self, scope, receive, send):
        if scope["type"] != "http":
            return await self.app(scope, receive, send)
        incoming = dict(scope["headers"]).get(b"x-request-id", b"").decode()[:64]
        rid = incoming or uuid.uuid4().hex
        request_id_var.set(rid)
        start = time.perf_counter()
        status = 500

        async def _send(message):
            nonlocal status
            if message["type"] == "http.response.start":
                status = message["status"]
                message["headers"] = [*message.get("headers", []), (b"x-request-id", rid.encode())]
            await send(message)

        try:
            await self.app(scope, receive, _send)
        finally:
            logger.info(
                "request",
                extra={
                    "method": scope["method"],
                    "path": scope["path"],
                    "status": status,
                    "duration_ms": round((time.perf_counter() - start) * 1000, 1),
                },
            )


# --- Error tracking ---

_PHONE_RE = re.compile(r"(?:\+?91[\s-]?)?[6-9]\d{4}[\s-]?\d{5}")


def _scrub(value):
    if isinstance(value, str):
        return _PHONE_RE.sub("[phone]", value)
    if isinstance(value, dict):
        return {k: _scrub(v) for k, v in value.items()}
    if isinstance(value, list):
        return [_scrub(v) for v in value]
    return value


def scrub_event(event, hint):
    """Sentry before_send: drop request bodies and cookies, mask phone numbers, tag the shop."""
    request = event.get("request") or {}
    for key in ("data", "cookies"):
        request.pop(key, None)
    event = _scrub(event)
    if shop_id_var.get():
        event.setdefault("tags", {})["shop_id"] = shop_id_var.get()
    return event


def init_sentry() -> bool:
    if not settings.SENTRY_DSN:
        return False
    import sentry_sdk

    sentry_sdk.init(
        dsn=settings.SENTRY_DSN,
        environment=settings.SENTRY_ENVIRONMENT or settings.APP_ENV,
        release=settings.VERSION,
        send_default_pii=False,
        before_send=scrub_event,
        traces_sample_rate=0,
    )
    return True


# --- Ops alerts ---

async def notify_ops(text: str, dedupe_key: str | None = None, redis=None, http=None) -> bool:
    """Send one message to the ops Telegram chat. Returns False when off or deduped.

    A repeat of the same dedupe_key within ALERT_DEDUPE_SECONDS is dropped.
    """
    token, chat_id = settings.OPS_TELEGRAM_BOT_TOKEN, settings.OPS_TELEGRAM_CHAT_ID
    if not token or not chat_id:
        return False
    if dedupe_key:
        redis = redis or get_redis()
        if not await redis.set(f"ops_alert:{dedupe_key}", "1", nx=True, ex=ALERT_DEDUPE_SECONDS):
            return False
    url = f"https://api.telegram.org/bot{token}/sendMessage"
    body = {"chat_id": chat_id, "text": text[:4000], "disable_web_page_preview": True}
    try:
        if http is not None:
            await http.post(url, json=body)
        else:
            async with httpx.AsyncClient(timeout=5) as client:
                await client.post(url, json=body)
    except Exception as e:  # never let alerting break the request; WARNING so it can't re-trigger alerts
        logger.warning("ops alert failed: %s", type(e).__name__)
    return True


class OpsAlertHandler(logging.Handler):
    """Forwards ERROR and above to notify_ops, one message per error type per 10 min."""

    def __init__(self):
        super().__init__(level=logging.ERROR)
        self._tasks: set[asyncio.Task] = set()

    def emit(self, record: logging.LogRecord) -> None:
        try:
            loop = asyncio.get_running_loop()
        except RuntimeError:
            return  # no event loop (startup scripts, CLI): stdout log is enough
        kind = record.exc_info[0].__name__ if record.exc_info else record.levelname
        dedupe_key = f"{kind}:{record.name}:{record.lineno}"
        text = _scrub(
            f"[MandiSamiti {settings.APP_ENV}] {kind} in {record.name}\n"
            f"{record.getMessage()[:500]}\nrequest_id={request_id_var.get()} shop_id={shop_id_var.get()}"
        )
        task = loop.create_task(notify_ops(text, dedupe_key=dedupe_key))
        self._tasks.add(task)
        task.add_done_callback(self._tasks.discard)
