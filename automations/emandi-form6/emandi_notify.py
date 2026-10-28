#!/usr/bin/env python3
"""Telegram notifier for the emandi-form6 skill.

Usage: emandi_notify.py <ready|saved|error> "<message>" [--print]
Settings come from .env (EMANDI_TEST_MODE, EMANDI_CHANNEL_ID); the new eMandi bot token stays in Keychain (service vivek-k.emandi-bot-token).
Test mode (default) sends to the personal chat tagged [TEST]. --print shows the message and sends nothing.
"""
import os
import re
import subprocess
import sys
from pathlib import Path

HUB = Path(__file__).resolve().parents[4]  # Vivek-K hub: holds .env and scripts/notify_telegram.py
STAGES = {"ready": "Form 6 ready: captcha + Save needed", "saved": "Form 6 saved", "error": "Form 6 problem"}


def load_env():
    f = HUB / ".env"
    if not f.exists():
        return
    for line in f.read_text().splitlines():
        k, sep, v = line.partition("=")
        if sep and k.strip().startswith("EMANDI_"):
            os.environ.setdefault(k.strip(), v.strip().strip("\"'"))


def keychain(service: str) -> str:
    r = subprocess.run(["security", "find-generic-password", "-a", "vivek-k", "-s", service, "-w"],
                       capture_output=True, text=True)
    return r.stdout.strip() if r.returncode == 0 else ""


def redact(text: str) -> str:
    """Licence numbers never leave the machine."""
    return re.sub(r"L/\d{4}/\d+/\d+", "<LICENCE_NO>", text)


def main():
    args = [a for a in sys.argv[1:] if a != "--print"]
    if len(args) < 2 or args[0] not in STAGES:
        sys.exit(__doc__)
    load_env()
    test = os.environ.get("EMANDI_TEST_MODE", "1") != "0"
    title = ("[TEST] " if test else "") + STAGES[args[0]]
    message = redact(args[1])
    if "--print" in sys.argv:
        print(title, message, sep="\n")
        return
    if not test:
        chan = os.environ.get("EMANDI_CHANNEL_ID")
        if not chan:
            sys.exit("EMANDI_CHANNEL_ID not set in .env")
        os.environ["TELEGRAM_CHAT_ID"] = chan
    sys.path.insert(0, str(HUB / "scripts"))
    token = keychain("vivek-k.emandi-bot-token")
    if not token:
        sys.exit("eMandi bot token missing in Keychain (service vivek-k.emandi-bot-token)")
    os.environ["AUTOMINI_BOT_TOKEN"] = token  # read by notify_telegram at import
    from notify_telegram import send_telegram_alert
    send_telegram_alert(title, message)


if __name__ == "__main__":
    main()
