"""Local virtualenvs and local-only files must stay out of the server sync and the image."""
import re
from pathlib import Path

import pytest

BACKEND = Path(__file__).resolve().parents[1]


@pytest.mark.parametrize("script", ["deploy_staging.sh", "deploy_prod.sh"])
def test_deploy_sync_excludes_local_files(script):
    text = (BACKEND / "scripts" / script).read_text()
    excluded = set(re.findall(r"--exclude '([^']+)'", text))
    assert {"venv", ".venv", ".env*"} <= excluded


def test_image_build_excludes_local_files():
    rules = set((BACKEND / ".dockerignore").read_text().splitlines())
    assert {"venv", ".venv", "**/.env*"} <= rules
