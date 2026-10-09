# MandiSamiti Backend API 🌾

Scalable, asynchronous cloud backend for MandiSamiti (Indian Mandi Agri-Trade Ledger & Soundbox System).

## Tech Stack
- **Framework:** FastAPI (Python 3.11+)
- **Database:** PostgreSQL 16 (Async SQLAlchemy 2.0 + asyncpg)
- **Cache & Rate-Limiting:** Redis 7
- **Authentication:** Mobile Phone OTP + Signed JWTs + Fast 4-Digit MPIN
- **Deployment:** Docker & Docker Compose (Ready for VPS deployment)

## API Endpoints Overview

### Authentication (`/api/v1/auth`)
- `POST /api/v1/auth/otp/send` — Request 6-digit OTP for 10-digit mobile number
- `POST /api/v1/auth/otp/verify` — Verify OTP, register/login user, and issue access/refresh tokens
- `POST /api/v1/auth/mpin/setup` — Configure local 4-digit MPIN for fast daily login
- `POST /api/v1/auth/mpin/verify` — Fast MPIN authentication
- `POST /api/v1/auth/refresh` — Refresh expired JWT access token

### Offline-to-Cloud Sync (`/api/v1/sync`)
- `POST /api/v1/sync/push` — Ingests batches of offline Parties, Deals, and Cash Transactions from mobile app
- `GET /api/v1/sync/pull?after_seq=N&limit=500` — Returns the shop's rows with `server_seq > after_seq` (all kinds merged in seq order, capped at the shop's committed `last_seq`); the phone stores the response's `next_seq` as its cursor and keeps pulling while `has_more` is true

### Soundbox Voice Broadcast (`/api/v1/soundbox`)
- `POST /api/v1/soundbox/broadcast` — Triggers instant Hindi audio voice script for payment received

## Quickstart (Local Development)
```bash
# 1. Create virtual environment
python3 -m venv venv
source venv/bin/activate

# 2. Install dependencies
pip install -r requirements.txt

# 3. Run FastAPI Dev Server
uvicorn app.main:app --reload --port 8000
```
Interactive Swagger Documentation will be live at: `http://localhost:8000/docs`

## Deploy on VPS (Single Command)
```bash
docker compose up -d --build
```
