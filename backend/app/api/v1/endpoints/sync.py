import time
from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select
from app.database import get_db
from app.models.party import Party
from app.models.deal import Deal
from app.models.transaction import CashTransaction
from app.models.revision import EntryRevision
from app.models.sync_conflict import SyncConflict
from app.core.sync_seq import lock_shop, next_seq
from app.schemas.sync import SyncPushRequest, SyncPushResponse, SyncPullResponse
from app.schemas.party import PartyResponse
from app.schemas.deal import DealResponse
from app.schemas.transaction import CashTransactionResponse
from app.schemas.revision import EntryRevisionResponse
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

    now_ms = int(time.time() * 1000)
    synced_parties = []
    synced_deals = []
    synced_txs = []
    synced_revisions = []
    conflicts = []

    shop = await lock_shop(db, shop_id)
    if shop is None:
        raise HTTPException(status_code=400, detail="User is not associated with a Shop")

    # 1. Sync Parties (Strictly isolated by shop_id)
    for p_in in payload.parties:
        p_id = p_in.id
        if p_id is None:
            continue
        incoming_at = p_in.updated_at or now_ms
        result = await db.execute(select(Party).where(Party.id == p_id))
        existing = result.scalars().first()

        if existing:
            if existing.shop_id == shop_id:
                if incoming_at > (existing.updated_at or 0):
                    existing.name = p_in.name
                    existing.phone = p_in.phone
                    existing.role = p_in.role
                    existing.village = p_in.village
                    existing.monthly_interest_rate = p_in.monthly_interest_rate
                    existing.photo_uri = p_in.photo_uri
                    existing.is_deleted = p_in.is_deleted
                    existing.sync_version += 1
                    existing.updated_at = incoming_at
                    existing.server_seq = next_seq(shop)
                synced_parties.append(p_id)
            else:
                # Collision with another shop's ID: do not overwrite and skip
                continue
        else:
            new_party = Party(
                id=p_id,
                shop_id=shop_id,
                name=p_in.name,
                phone=p_in.phone,
                role=p_in.role,
                village=p_in.village,
                monthly_interest_rate=p_in.monthly_interest_rate,
                photo_uri=p_in.photo_uri,
                is_deleted=p_in.is_deleted,
                sync_version=1,
                created_at=incoming_at,
                updated_at=incoming_at,
                server_seq=next_seq(shop)
            )
            db.add(new_party)
            synced_parties.append(p_id)

    # 2. Sync Deals (Strictly isolated by shop_id)
    for d_in in payload.deals:
        d_id = d_in.id
        if d_id is None:
            continue
        result = await db.execute(select(Deal).where(Deal.id == d_id))
        existing_deal = result.scalars().first()

        if existing_deal:
            if existing_deal.shop_id == shop_id:
                if d_in.revision > (existing_deal.revision or 0):
                    existing_deal.farmer_id = d_in.farmer_id
                    existing_deal.buyer_id = d_in.buyer_id
                    existing_deal.commodity = d_in.commodity
                    existing_deal.deal_status = d_in.deal_status
                    existing_deal.deal_date = d_in.deal_date or existing_deal.deal_date
                    existing_deal.bags_count = d_in.bags_count
                    existing_deal.gross_weight_grams = d_in.gross_weight_grams
                    existing_deal.cut_weight_grams = d_in.cut_weight_grams
                    existing_deal.net_weight_grams = d_in.net_weight_grams
                    existing_deal.rate_paisa_per_unit = d_in.rate_paisa_per_unit
                    existing_deal.gross_amount_paisa = d_in.gross_amount_paisa
                    existing_deal.farmer_commission_bps = d_in.farmer_commission_bps
                    existing_deal.farmer_commission_paisa = d_in.farmer_commission_paisa
                    existing_deal.buyer_commission_paisa = d_in.buyer_commission_paisa
                    existing_deal.labour_charge_paisa = d_in.labour_charge_paisa
                    existing_deal.weighing_charge_paisa = d_in.weighing_charge_paisa
                    existing_deal.other_deductions_paisa = d_in.other_deductions_paisa
                    existing_deal.net_farmer_payable_paisa = d_in.net_farmer_payable_paisa
                    existing_deal.net_buyer_receivable_paisa = d_in.net_buyer_receivable_paisa
                    existing_deal.receipt_photo_uri = d_in.receipt_photo_uri
                    existing_deal.voice_note_uri = d_in.voice_note_uri
                    existing_deal.remarks = d_in.remarks
                    existing_deal.is_void = d_in.is_void
                    existing_deal.void_reason = d_in.void_reason
                    existing_deal.revision = d_in.revision
                    existing_deal.is_deleted = d_in.is_deleted
                    existing_deal.sync_version += 1
                    existing_deal.updated_at = now_ms
                    existing_deal.server_seq = next_seq(shop)
                synced_deals.append(d_id)
            else:
                # Cross-tenant collision: skip overwrite
                continue
        else:
            new_deal = Deal(
                id=d_id,
                shop_id=shop_id,
                farmer_id=d_in.farmer_id,
                buyer_id=d_in.buyer_id,
                commodity=d_in.commodity,
                deal_status=d_in.deal_status,
                deal_date=d_in.deal_date or now_ms,
                bags_count=d_in.bags_count,
                gross_weight_grams=d_in.gross_weight_grams,
                cut_weight_grams=d_in.cut_weight_grams,
                net_weight_grams=d_in.net_weight_grams,
                rate_paisa_per_unit=d_in.rate_paisa_per_unit,
                gross_amount_paisa=d_in.gross_amount_paisa,
                farmer_commission_bps=d_in.farmer_commission_bps,
                farmer_commission_paisa=d_in.farmer_commission_paisa,
                buyer_commission_paisa=d_in.buyer_commission_paisa,
                labour_charge_paisa=d_in.labour_charge_paisa,
                weighing_charge_paisa=d_in.weighing_charge_paisa,
                other_deductions_paisa=d_in.other_deductions_paisa,
                net_farmer_payable_paisa=d_in.net_farmer_payable_paisa,
                net_buyer_receivable_paisa=d_in.net_buyer_receivable_paisa,
                receipt_photo_uri=d_in.receipt_photo_uri,
                voice_note_uri=d_in.voice_note_uri,
                remarks=d_in.remarks,
                is_void=d_in.is_void,
                void_reason=d_in.void_reason,
                revision=d_in.revision,
                is_deleted=d_in.is_deleted,
                sync_version=1,
                created_at=now_ms,
                updated_at=now_ms,
                server_seq=next_seq(shop)
            )
            db.add(new_deal)
            synced_deals.append(d_id)

    # 3. Sync Cash Transactions (Strictly isolated by shop_id)
    for t_in in payload.transactions:
        t_id = t_in.id
        if t_id is None:
            continue
        result = await db.execute(select(CashTransaction).where(CashTransaction.id == t_id))
        existing_tx = result.scalars().first()

        if existing_tx:
            if existing_tx.shop_id == shop_id:
                if t_in.revision > (existing_tx.revision or 0):
                    existing_tx.party_id = t_in.party_id
                    existing_tx.deal_id = t_in.deal_id
                    existing_tx.transaction_type = t_in.transaction_type
                    existing_tx.amount_paisa = t_in.amount_paisa
                    existing_tx.payment_mode = t_in.payment_mode
                    existing_tx.category = t_in.category
                    existing_tx.transaction_date = t_in.transaction_date or existing_tx.transaction_date
                    existing_tx.voice_note_uri = t_in.voice_note_uri
                    existing_tx.remarks = t_in.remarks
                    existing_tx.is_void = t_in.is_void
                    existing_tx.void_reason = t_in.void_reason
                    existing_tx.revision = t_in.revision
                    existing_tx.is_deleted = t_in.is_deleted
                    existing_tx.sync_version += 1
                    existing_tx.updated_at = now_ms
                    existing_tx.server_seq = next_seq(shop)
                synced_txs.append(t_id)
            else:
                continue
        else:
            new_tx = CashTransaction(
                id=t_id,
                shop_id=shop_id,
                party_id=t_in.party_id,
                deal_id=t_in.deal_id,
                transaction_type=t_in.transaction_type,
                amount_paisa=t_in.amount_paisa,
                payment_mode=t_in.payment_mode,
                category=t_in.category,
                transaction_date=t_in.transaction_date or now_ms,
                voice_note_uri=t_in.voice_note_uri,
                remarks=t_in.remarks,
                soundbox_broadcasted=1,
                is_void=t_in.is_void,
                void_reason=t_in.void_reason,
                revision=t_in.revision,
                is_deleted=t_in.is_deleted,
                sync_version=1,
                created_at=now_ms,
                updated_at=now_ms,
                server_seq=next_seq(shop)
            )
            db.add(new_tx)
            synced_txs.append(t_id)

    # 4. Sync Revisions (Append-only audit trail)
    for r_in in payload.revisions:
        r_id = r_in.id
        if r_id is None:
            continue
        result = await db.execute(
            select(EntryRevision).where(
                EntryRevision.entry_id == r_in.entry_id,
                EntryRevision.revision == r_in.revision
            )
        )
        existing_rev = result.scalars().first()
        if not existing_rev:
            new_rev = EntryRevision(
                id=r_id,
                shop_id=shop_id,
                entry_id=r_in.entry_id,
                entry_kind=r_in.entry_kind,
                revision=r_in.revision,
                change_kind=r_in.change_kind,
                snapshot_json=r_in.snapshot_json,
                void_reason=r_in.void_reason,
                changed_at=r_in.changed_at or now_ms,
                server_seq=next_seq(shop)
            )
            db.add(new_rev)
        elif existing_rev.shop_id != shop_id:
            continue
        elif existing_rev.snapshot_json != r_in.snapshot_json:
            db.add(SyncConflict(
                shop_id=shop_id,
                entry_id=r_in.entry_id,
                revision=r_in.revision,
                snapshot_json=r_in.snapshot_json
            ))
            conflicts.append(r_id)
        synced_revisions.append(r_id)

    await db.commit()

    return SyncPushResponse(
        success=True,
        synced_parties=synced_parties,
        synced_deals=synced_deals,
        synced_transactions=synced_txs,
        synced_revisions=synced_revisions,
        conflicts=conflicts,
        server_sync_time=now_ms,
        server_seq=shop.last_seq or 0
    )

@router.get("/pull", response_model=SyncPullResponse, summary="Download changes after a server_seq cursor")
async def sync_pull(
    after_seq: int = Query(0, ge=0, description="Last server_seq this phone has applied"),
    limit: int = Query(500, ge=1, le=1000),
    current_user: dict = Depends(get_current_user_payload),
    db: AsyncSession = Depends(get_db),
):
    shop_id = current_user.get("shop_id")
    # Each table's first `limit` rows after the cursor contain every row of the global first `limit`.
    kinds = ((Party, PartyResponse), (Deal, DealResponse),
             (CashTransaction, CashTransactionResponse), (EntryRevision, EntryRevisionResponse))
    merged = []
    for model, schema in kinds:
        res = await db.execute(
            select(model).where(model.shop_id == shop_id, model.server_seq > after_seq)
            .order_by(model.server_seq.asc()).limit(limit + 1)
        )
        merged += [(row.server_seq, schema, row) for row in res.scalars().all()]
    merged.sort(key=lambda t: t[0])
    page = merged[:limit]

    out = {PartyResponse: [], DealResponse: [], CashTransactionResponse: [], EntryRevisionResponse: []}
    for _, schema, row in page:
        out[schema].append(schema.model_validate(row))
    return SyncPullResponse(
        after_seq=after_seq,
        next_seq=page[-1][0] if page else after_seq,
        has_more=len(merged) > limit,
        parties=out[PartyResponse], deals=out[DealResponse],
        transactions=out[CashTransactionResponse], revisions=out[EntryRevisionResponse],
        server_sync_time=int(time.time() * 1000),
    )
