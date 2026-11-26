import re
from typing import Annotated, Optional
from pydantic import BaseModel, ConfigDict, Field, StrictInt, field_validator

INT64_MAX = (1 << 63) - 1
MAX_EVENTS_PER_BATCH = 200

_ID_RE = re.compile(r"[A-Za-z0-9_-]{1,64}")
_NAME_RE = re.compile(r"[a-z0-9_]{1,64}")
_APP_VERSION_RE = re.compile(r"[A-Za-z0-9._+-]{1,32}")


class AnalyticsEventIn(BaseModel):
    model_config = ConfigDict(extra="forbid")

    id: str
    name: str
    screen: Optional[str] = None
    session_id: str
    occurred_at_ms: Annotated[StrictInt, Field(ge=0, le=INT64_MAX)]
    duration_ms: Optional[Annotated[StrictInt, Field(ge=0, le=INT64_MAX)]] = None
    app_version: str

    @field_validator("id", "session_id")
    @classmethod
    def validate_identifier(cls, value: str) -> str:
        if not _ID_RE.fullmatch(value):
            raise ValueError("must be 1..64 chars of [A-Za-z0-9_-]")
        return value

    @field_validator("name")
    @classmethod
    def validate_name(cls, value: str) -> str:
        if not _NAME_RE.fullmatch(value):
            raise ValueError("must match ^[a-z0-9_]{1,64}$")
        return value

    @field_validator("screen")
    @classmethod
    def validate_screen(cls, value: Optional[str]) -> Optional[str]:
        if value is not None and not _NAME_RE.fullmatch(value):
            raise ValueError("must be null or match ^[a-z0-9_]{1,64}$")
        return value

    @field_validator("app_version")
    @classmethod
    def validate_app_version(cls, value: str) -> str:
        if not _APP_VERSION_RE.fullmatch(value):
            raise ValueError("must be 1..32 chars of [A-Za-z0-9._+-]")
        return value


class EventsBatchRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")

    events: list[AnalyticsEventIn] = Field(..., max_length=MAX_EVENTS_PER_BATCH)


class EventsBatchResponse(BaseModel):
    accepted: int
