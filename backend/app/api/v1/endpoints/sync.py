import time
import uuid
from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select
from app.database import get_db
from app.models.party import Party
from app.models.deal import Deal
from app.models.transaction import CashTransaction
from app.schemas.sync import SyncPushRequest, SyncPushResponse, SyncPullResponse
from app.schemas.party import PartyResponse
from app.schemas.deal import DealResponse
from app.schemas.transaction import CashTransactionResponse
from app.core.security import get_current_user_payload

router = APIRouter()

@router.post("/push", response_model=SyncPushResponse, summary="Batch upload offline records to cloud")
async def sync_push(
    payload: SyncPushRequest,
    current_user: dict = Depends(get_current_user_payload),
    db: AsyncSession = Depends(get_db)
):
    shop_id = current_user.get("shop_id")
    if not shop_id:
        raise HTTPException(status_code=400, detail="User is not associated with a Shop")

    now = time.time()
    synced_parties = []
    synced_deals = []
    synced_txs = []

    # 1. Sync Parties
    for p_in in payload.parties:
        p_id = p_in.id or str(uuid.uuid4())
        result = await db.execute(select(Party).where(Party.id == p_id))
        existing = result.scalars().first()

        if existing:
            existing.name = p_in.name
            existing.phone = p_in.phone
            existing.role = p_in.role
            existing.village = p_in.village
            existing.current_balance = p_in.current_balance
            existing.sync_version += 1
            existing.updated_at = now
        else:
            new_party = Party(
                id=p_id,
                shop_id=shop_id,
                name=p_in.name,
                phone=p_in.phone,
                role=p_in.role,
                village=p_in.village,
                current_balance=p_in.current_balance,
                sync_version=1,
                created_at=now,
                updated_at=now
            )
            db.add(new_party)
        synced_parties.append(p_id)

    # 2. Sync Deals
    for d_in in payload.deals:
        d_id = d_in.id or str(uuid.uuid4())
        result = await db.execute(select(Deal).where(Deal.id == d_id))
        existing_deal = result.scalars().first()

        if existing_deal:
            existing_deal.commodity = d_in.commodity
            existing_deal.bags = d_in.bags
            existing_deal.gross_weight = d_in.gross_weight
            existing_deal.tare_weight = d_in.tare_weight
            existing_deal.net_weight = d_in.net_weight
            existing_deal.rate = d_in.rate
            existing_deal.commission_rate = d_in.commission_rate
            existing_deal.labour_charges = d_in.labour_charges
            existing_deal.farmer_total = d_in.farmer_total
            existing_deal.buyer_total = d_in.buyer_total
            existing_deal.status = d_in.status
            existing_deal.sync_version += 1
            existing_deal.updated_at = now
        else:
            new_deal = Deal(
                id=d_id,
                shop_id=shop_id,
                farmer_id=d_in.farmer_id,
                buyer_id=d_in.buyer_id,
                commodity=d_in.commodity,
                bags=d_in.bags,
                gross_weight=d_in.gross_weight,
                tare_weight=d_in.tare_weight,
                net_weight=d_in.net_weight,
                rate=d_in.rate,
                commission_rate=d_in.commission_rate,
                labour_charges=d_in.labour_charges,
                farmer_total=d_in.farmer_total,
                buyer_total=d_in.buyer_total,
                status=d_in.status,
                sync_version=1,
                created_at=now,
                updated_at=now
            )
            db.add(new_deal)
        synced_deals.append(d_id)

    # 3. Sync Cash Transactions
    for t_in in payload.transactions:
        t_id = t_in.id or str(uuid.uuid4())
        result = await db.execute(select(CashTransaction).where(CashTransaction.id == t_id))
        existing_tx = result.scalars().first()

        if existing_tx:
            existing_tx.amount = t_in.amount
            existing_tx.type = t_in.type
            existing_tx.category = t_in.category
            existing_tx.notes = t_in.notes
            existing_tx.sync_version += 1
            existing_tx.updated_at = now
        else:
            new_tx = CashTransaction(
                id=t_id,
                shop_id=shop_id,
                party_id=t_in.party_id,
                type=t_in.type,
                amount=t_in.amount,
                category=t_in.category,
                notes=t_in.notes,
                soundbox_broadcasted=1,
                sync_version=1,
                timestamp=t_in.timestamp or now,
                created_at=now,
                updated_at=now
            )
            db.add(new_tx)
        synced_txs.append(t_id)

    await db.commit()

    return SyncPushResponse(
        success=True,
        synced_parties=synced_parties,
        synced_deals=synced_deals,
        synced_transactions=synced_txs,
        server_sync_time=now
    )

@router.get("/pull", response_model=SyncPullResponse, summary="Download latest changes from cloud")
async def sync_pull(
    since: float = Query(0.0, description="Timestamp of last sync"),
    current_user: dict = Depends(get_current_user_payload),
    db: AsyncSession = Depends(get_db)
):
    shop_id = current_user.get("shop_id")
    now = time.time()

    parties_res = await db.execute(
        select(Party).where(Party.shop_id == shop_id, Party.updated_at > since)
    )
    deals_res = await db.execute(
        select(Deal).where(Deal.shop_id == shop_id, Deal.updated_at > since)
    )
    txs_res = await db.execute(
        select(CashTransaction).where(CashTransaction.shop_id == shop_id, CashTransaction.updated_at > since)
    )

    parties = [PartyResponse.from_orm(p) for p in parties_res.scalars().all()]
    deals = [DealResponse.from_orm(d) for d in deals_res.scalars().all()]
    txs = [CashTransactionResponse.from_orm(t) for t in txs_res.scalars().all()]

    return SyncPullResponse(
        last_sync_timestamp=since,
        parties=parties,
        deals=deals,
        transactions=txs,
        server_sync_time=now
    )
