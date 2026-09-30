from fastapi import APIRouter, Depends, HTTPException
from pydantic import BaseModel, Field
from app.core.security import get_current_user_payload

router = APIRouter()

class SoundboxTriggerRequest(BaseModel):
    amount: float = Field(..., gt=0, example=2500.0)
    payer_name: str = Field(..., example="अग्रवाल ट्रेडर्स")
    type: str = Field(default="PAYMENT_RECEIVED", example="PAYMENT_RECEIVED")
    language: str = Field(default="hi-IN", example="hi-IN")

class SoundboxTriggerResponse(BaseModel):
    success: bool
    voice_script: str
    amount: float
    shop_id: str

@router.post("/broadcast", response_model=SoundboxTriggerResponse, summary="Trigger Soundbox voice announcement")
async def broadcast_soundbox(
    req: SoundboxTriggerRequest,
    current_user: dict = Depends(get_current_user_payload)
):
    shop_id = current_user.get("shop_id")
    if not shop_id:
        raise HTTPException(status_code=400, detail="Shop ID required")

    # Generate Hindi audio script
    int_amount = int(req.amount)
    voice_script = f"{req.payer_name} से {int_amount} रुपये प्राप्त हुए"

    return SoundboxTriggerResponse(
        success=True,
        voice_script=voice_script,
        amount=req.amount,
        shop_id=shop_id
    )
