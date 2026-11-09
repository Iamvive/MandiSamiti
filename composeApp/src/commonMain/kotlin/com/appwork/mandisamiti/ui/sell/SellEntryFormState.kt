package com.appwork.mandisamiti.ui.sell

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.appwork.mandisamiti.domain.id.IdGenerator
import com.appwork.mandisamiti.domain.math.BuySellMath
import com.appwork.mandisamiti.domain.math.MandiMathEngine
import com.appwork.mandisamiti.domain.model.BillMode
import com.appwork.mandisamiti.domain.model.SellCropLot
import com.appwork.mandisamiti.domain.model.SellEntry
import com.appwork.mandisamiti.domain.model.SellPartyKind
import com.appwork.mandisamiti.domain.model.TradeSettings
import com.appwork.mandisamiti.domain.validation.MandiValidators
import com.appwork.mandisamiti.domain.validation.ValidationResult
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

const val MAX_BAGS = 9_999L
const val MAX_BAG_WEIGHT_KG = 999L
const val MAX_RATE_RUPEES = 100_000L

data class SellLotValidation(
    val bagsError: String?,
    val weightError: String?,
    val rateError: String?,
    val extraError: String?,
    val isValid: Boolean,
)

class SellLotItemState(
    val id: String = IdGenerator.newId(),
    initialCropName: String = "गेहूं",
    initialBags: String = "",
    initialWeightPerBagKg: String = "50",
    initialExtraWeightKg: String = "",
    initialRatePerQuintal: String = "",
) {
    var cropName by mutableStateOf(initialCropName)
    var bags by mutableStateOf(initialBags)
    var weightPerBagKg by mutableStateOf(initialWeightPerBagKg)
    var extraWeightKg by mutableStateOf(initialExtraWeightKg)
    var ratePerQuintal by mutableStateOf(initialRatePerQuintal)

    val bagsValidation: ValidationResult
        get() = MandiValidators.validatePositiveNumber(bags, "बोरी संख्या")

    val weightValidation: ValidationResult
        get() = MandiValidators.validatePositiveNumber(weightPerBagKg, "वजन प्रति बोरी")

    val rateValidation: ValidationResult
        get() = MandiValidators.validatePositiveNumber(ratePerQuintal, "भाव")

    fun validate(): SellLotValidation {
        val bagsVal = MandiValidators.validatePositiveNumber(bags, "बोरी संख्या")
        val weightVal = MandiValidators.validatePositiveNumber(weightPerBagKg, "वजन/बोरी")
        val rateVal = MandiValidators.validatePositiveNumber(ratePerQuintal, "भाव")
        val extraVal = MandiValidators.validateNonNegativeNumber(extraWeightKg, "अतिरिक्त वजन")
        val bagGrams = MandiMathEngine.parseKgToGrams(weightPerBagKg.ifBlank { "0" })
        val extraGrams = MandiMathEngine.parseKgToGrams(extraWeightKg.ifBlank { "0" })
        val ratePaisa = MandiMathEngine.parseRupeesToPaisa(ratePerQuintal.ifBlank { "0" })

        val bagsError = bagsVal.errorOrNull
            ?: if ((bags.toLongOrNull() ?: 0L) > MAX_BAGS) "बोरी संख्या अधिकतम $MAX_BAGS" else null
        val weightError = weightVal.errorOrNull
            ?: if (bagGrams % 1000L != 0L || bagGrams > MAX_BAG_WEIGHT_KG * 1000L) "वजन/बोरी पूरे किलो में, अधिकतम $MAX_BAG_WEIGHT_KG" else null
        val rateError = rateVal.errorOrNull
            ?: if (ratePaisa % 100L != 0L || ratePaisa > MAX_RATE_RUPEES * 100L) "भाव पूरे रुपये में, अधिकतम $MAX_RATE_RUPEES" else null
        val extraError = extraVal.errorOrNull
            ?: if (bagGrams > 0L && extraGrams >= bagGrams) "अतिरिक्त वजन बोरी के वजन से कम होना चाहिए" else null

        return SellLotValidation(
            bagsError = bagsError,
            weightError = weightError,
            rateError = rateError,
            extraError = extraError,
            isValid = bagsError == null && weightError == null && rateError == null && extraError == null
        )
    }

    fun toCropLot(): SellCropLot {
        val bagsCount = bags.toLongOrNull() ?: 0L
        val weightGrams = MandiMathEngine.parseKgToGrams(weightPerBagKg.ifBlank { "0" })
        val extraGrams = MandiMathEngine.parseKgToGrams(extraWeightKg.ifBlank { "0" })
        val ratePaisa = MandiMathEngine.parseRupeesToPaisa(ratePerQuintal.ifBlank { "0" })
        return SellCropLot.create(
            id = id,
            cropName = cropName.ifBlank { "गेहूं" },
            bags = bagsCount,
            weightPerBagGrams = weightGrams,
            extraWeightGrams = extraGrams,
            ratePaisaPerQuintal = ratePaisa
        )
    }
}

data class SellAggregateResult(
    val totalBags: Long,
    val totalWeightGrams: Long,
    val grossPaisa: Long,
    val commissionPaisa: Long,
    val labourPaisa: Long,
    val mandiShulkPaisa: Long,
    val krishakCessPaisa: Long,
    val tulakPaisa: Long,
    val totalReceivablePaisa: Long,
    val advanceReceivedPaisa: Long,
    val balanceDuePaisa: Long,
    val lots: List<SellCropLot>,
)

class SellEntryFormState(
    tradeSettings: TradeSettings? = null
) {
    var buyerName by mutableStateOf("")
    var selectedBuyerId: String? by mutableStateOf(null)
    var phone by mutableStateOf("")

    private var _village by mutableStateOf("")
    var village: String
        get() = _village
        set(value) {
            _village = MandiValidators.sanitizeVillage(value)
        }

    var partyKind by mutableStateOf(SellPartyKind.CUSTOMER)
    var billMode by mutableStateOf(BillMode.NORMAL)

    // Bill of Supply specific fields
    var gstin by mutableStateOf("")
    var stateCode by mutableStateOf("23")
    var invoiceDate by mutableStateOf("")

    // Deductions & charges
    private var settings: TradeSettings? = tradeSettings

    private fun defaultCommissionFor(kind: SellPartyKind): String = when (kind) {
        SellPartyKind.MERCHANT -> settings?.let { MandiMathEngine.paisaToInputString(it.buyerCommissionBps) } ?: "1.5"
        SellPartyKind.CUSTOMER -> settings?.let { MandiMathEngine.paisaToInputString(it.customerCommissionBps) } ?: "1.5"
    }

    var commissionPercent by mutableStateOf(defaultCommissionFor(SellPartyKind.CUSTOMER))
    var labourPerBag by mutableStateOf(
        tradeSettings?.let { MandiMathEngine.paisaToRupeesString(it.defaultLabourPaisa) } ?: "10"
    )
    // Tare is gone for new slips (Vivek, 2026-10-31). Only populateFrom() fills it for saved slips.
    var tareQuintals by mutableStateOf("")
    var tulakPerQuintal by mutableStateOf("1")

    // Advance received now (अग्रिम / प्राप्त राशि)
    var advanceReceived by mutableStateOf("")

    // Read-only balances (existing debt / deposit)
    var loanDebtPaisa by mutableStateOf(0L)
        private set
    var depositPaisa by mutableStateOf(0L)
        private set

    fun setLoan(debtPaisa: Long) {
        loanDebtPaisa = debtPaisa
    }

    fun setDeposit(paisa: Long) {
        depositPaisa = paisa
    }

    fun updatePhone(input: String) {
        phone = input.filter { it.isDigit() }.take(10)
    }

    fun applySettings(settings: TradeSettings) {
        this.settings = settings
        commissionPercent = defaultCommissionFor(partyKind)
        labourPerBag = MandiMathEngine.paisaToRupeesString(settings.defaultLabourPaisa)
    }

    val lots: SnapshotStateList<SellLotItemState> = mutableStateListOf(
        SellLotItemState(initialCropName = "गेहूं", initialWeightPerBagKg = "50")
    )

    fun setParty(newParty: SellPartyKind) {
        // Move to the new party's default rate, unless the owner typed a custom one.
        if (newParty != partyKind && commissionPercent == defaultCommissionFor(partyKind)) {
            commissionPercent = defaultCommissionFor(newParty)
        }
        partyKind = newParty
        // Guardrail: Bill of Supply is strictly for Merchants
        if (newParty == SellPartyKind.CUSTOMER && billMode == BillMode.BILL_OF_SUPPLY) {
            billMode = BillMode.NORMAL
        }
    }

    fun setMode(newMode: BillMode) {
        if (partyKind == SellPartyKind.CUSTOMER && newMode == BillMode.BILL_OF_SUPPLY) {
            return
        }
        billMode = newMode
    }

    fun addLot(defaultCrop: String = "गेहूं") {
        lots.add(SellLotItemState(initialCropName = defaultCrop, initialWeightPerBagKg = "50"))
    }

    fun removeLot(index: Int) {
        if (lots.size > 1 && index in lots.indices) {
            lots.removeAt(index)
        }
    }

    fun hasSanityWarning(): Boolean {
        return lots.any { lot ->
            val b = lot.bags.toLongOrNull() ?: 0L
            val w = lot.weightPerBagKg.toDoubleOrNull() ?: 0.0
            val ratePaisa = MandiMathEngine.parseRupeesToPaisa(lot.ratePerQuintal.ifBlank { "0" })
            MandiValidators.isBagsOutlier(b) ||
                (w > 0.0 && MandiValidators.isWeightPerBagOutlier(w)) ||
                (ratePaisa > 0L && MandiValidators.isRateOutlier(ratePaisa))
        }
    }

    fun sanityWarningMessage(): String? {
        val warnings = mutableListOf<String>()
        lots.forEachIndexed { idx, lot ->
            val lotNum = if (lots.size > 1) "फसल #${idx + 1}: " else ""
            val b = lot.bags.toLongOrNull() ?: 0L
            if (MandiValidators.isBagsOutlier(b)) {
                warnings.add("${lotNum}बोरी संख्या (${MandiMathEngine.formatIndianGrouping(b)}) 2,000 से अधिक है।")
            }
            val w = lot.weightPerBagKg.toDoubleOrNull() ?: 0.0
            if (w > 0.0 && MandiValidators.isWeightPerBagOutlier(w)) {
                warnings.add("${lotNum}वजन प्रति बोरी ($w किग्रा) सामान्य सीमा (20-120 किग्रा) से बाहर है।")
            }
            val ratePaisa = MandiMathEngine.parseRupeesToPaisa(lot.ratePerQuintal.ifBlank { "0" })
            if (ratePaisa > 0L && MandiValidators.isRateOutlier(ratePaisa)) {
                warnings.add("${lotNum}भाव (₹${MandiMathEngine.paisaToRupeesString(ratePaisa)}/क्विंटल) सामान्य सीमा (₹500 - ₹20,000) से बाहर है।")
            }
        }
        return if (warnings.isEmpty()) null else warnings.joinToString("\n")
    }

    fun aggregatePreview(): SellAggregateResult {
        val calculatedLots = lots.map { it.toCropLot() }
        val totalBags = calculatedLots.sumOf { it.bags }
        val totalGrossWeight = calculatedLots.sumOf { it.totalWeightGrams }
        val tareGrams = MandiMathEngine.parseQuintalsStringToGrams(tareQuintals.ifBlank { "0" })
        val totalWeight = (totalGrossWeight - tareGrams).coerceAtLeast(0L)

        val gross = if (totalGrossWeight <= 0L || tareGrams == 0L) {
            calculatedLots.sumOf { it.grossPaisa }
        } else {
            calculatedLots.sumOf { lot ->
                val lotWeight = lot.totalWeightGrams
                val lotNetWeight = (lotWeight - (tareGrams * lotWeight / totalGrossWeight)).coerceAtLeast(0L)
                MandiMathEngine.grossAmountPaisa(lotNetWeight, lot.ratePaisaPerQuintal)
            }
        }

        val isBos = (billMode == BillMode.BILL_OF_SUPPLY && partyKind == SellPartyKind.MERCHANT)
        val commBps = MandiMathEngine.parsePercentToBasisPoints(commissionPercent.ifBlank { "0" })

        val commissionPaisa: Long
        val labourPaisa: Long = 0L
        val mandiShulkPaisa: Long
        val krishakCessPaisa: Long = 0L
        val tulakPaisa: Long
        val totalReceivable: Long

        if (isBos) {
            // Bill of Supply: Arat at entered rate, Mandi Shulk 1.5%, Tulak Rs 1/qtl
            commissionPaisa = MandiMathEngine.percentageOf(gross, commBps)
            mandiShulkPaisa = MandiMathEngine.percentageOf(gross, BuySellMath.MANDI_SHULK_BPS) // 150 bps = 1.5%
            val tulakRatePaisa = MandiMathEngine.parseRupeesToPaisa(tulakPerQuintal.ifBlank { "1" })
            tulakPaisa = MandiMathEngine.grossAmountPaisa(totalWeight, tulakRatePaisa)
            totalReceivable = gross + commissionPaisa + mandiShulkPaisa + tulakPaisa
        } else {
            // Cash Memo: Arat at entered rate, final cut down to whole rupee, Tulak absorbs difference
            commissionPaisa = MandiMathEngine.percentageOf(gross, commBps)
            mandiShulkPaisa = 0L
            val nominalTulak = MandiMathEngine.grossAmountPaisa(totalWeight, 100L) // Rs 1 per quintal nominal
            val unrounded = gross + commissionPaisa + nominalTulak
            totalReceivable = (unrounded / 100L) * 100L // cut down to whole rupee per sheet rule 8
            tulakPaisa = totalReceivable - gross - commissionPaisa
        }

        val advancePaisa = MandiMathEngine.parseRupeesToPaisa(advanceReceived.ifBlank { "0" })
        val balanceDue = (totalReceivable - advancePaisa).coerceAtLeast(0L)

        return SellAggregateResult(
            totalBags = totalBags,
            totalWeightGrams = totalWeight,
            grossPaisa = gross,
            commissionPaisa = commissionPaisa,
            labourPaisa = labourPaisa,
            mandiShulkPaisa = mandiShulkPaisa,
            krishakCessPaisa = krishakCessPaisa,
            tulakPaisa = tulakPaisa,
            totalReceivablePaisa = totalReceivable,
            advanceReceivedPaisa = advancePaisa,
            balanceDuePaisa = balanceDue,
            lots = calculatedLots,
        )
    }

    val buyerNameValidation: ValidationResult
        get() = MandiValidators.validateBuyerName(buyerName, partyKind == SellPartyKind.MERCHANT)

    val phoneValidation: ValidationResult
        get() = MandiValidators.validatePhone(phone, isRequired = true)

    val villageValidation: ValidationResult
        get() = if (MandiValidators.sanitizeVillage(village).isBlank()) {
            ValidationResult.Invalid("गाँव / शहर का नाम दर्ज करें")
        } else {
            ValidationResult.Valid
        }

    val gstinValidation: ValidationResult
        get() = MandiValidators.validateGstin(
            gstin = gstin,
            isRequired = (billMode == BillMode.BILL_OF_SUPPLY && partyKind == SellPartyKind.MERCHANT)
        )

    val stateCodeValidation: ValidationResult
        get() = MandiValidators.validateStateCode(
            stateCode = stateCode,
            isRequired = (billMode == BillMode.BILL_OF_SUPPLY && partyKind == SellPartyKind.MERCHANT)
        )

    fun validateLots(): List<SellLotValidation> = lots.map { it.validate() }

    fun validationReason(): String? {
        val nameErr = buyerNameValidation.errorOrNull
        if (nameErr != null) return nameErr

        val phoneErr = phoneValidation.errorOrNull
        if (phoneErr != null) return phoneErr

        val villageErr = villageValidation.errorOrNull
        if (villageErr != null) return villageErr

        if (billMode == BillMode.BILL_OF_SUPPLY && partyKind == SellPartyKind.MERCHANT) {
            val gstinErr = gstinValidation.errorOrNull
            if (gstinErr != null) return gstinErr

            val stateErr = stateCodeValidation.errorOrNull
            if (stateErr != null) return stateErr
        }

        val lotsVal = validateLots()
        val firstInvalidIdx = lotsVal.indexOfFirst { !it.isValid }
        if (firstInvalidIdx != -1) {
            val err = lotsVal[firstInvalidIdx]
            return "फसल #${firstInvalidIdx + 1}: ${err.bagsError ?: err.weightError ?: err.extraError ?: err.rateError ?: "विवरण सही करें"}"
        }

        val preview = aggregatePreview()
        if (preview.grossPaisa <= 0) return "कम से कम 1 फसल का वजन और भाव दर्ज करें"

        return null
    }

    fun canSave(): Boolean = validationReason() == null

    fun toSellEntry(existingId: String? = null, shopId: String = "shop-default"): SellEntry {
        val preview = aggregatePreview()
        val now = Clock.System.now().toEpochMilliseconds()
        val lotsJson = Json.encodeToString(preview.lots)

        val firstLot = preview.lots.firstOrNull()
        val singleBags = preview.totalBags
        val singleWeightPerBag = if (preview.totalBags > 0) preview.totalWeightGrams / preview.totalBags else 50_000L
        val effectiveRate = if (preview.totalWeightGrams > 0) {
            (preview.grossPaisa * 100_000L) / preview.totalWeightGrams
        } else {
            firstLot?.ratePaisaPerQuintal ?: 0L
        }

        val cleanVillage = MandiValidators.sanitizeVillage(village)
        val combinedRemarks = buildString {
            if (cleanVillage.isNotBlank()) append("गाँव: $cleanVillage | ")
            if (tareQuintals.isNotBlank() && tareQuintals != "0") append("काट: ${tareQuintals}Q | ")
            append("DATA: $lotsJson")
        }

        return SellEntry(
            id = existingId ?: "sell-${IdGenerator.newId()}",
            shopId = shopId,
            buyerId = selectedBuyerId ?: buyerName.trim(),
            commodityId = if (preview.lots.size > 1) "विविध जिंस" else (firstLot?.cropName ?: "गेहूं"),
            entryDate = now,
            billMode = billMode,
            partyKind = partyKind,
            gstin = if (billMode == BillMode.BILL_OF_SUPPLY) gstin.trim().uppercase() else null,
            bagsCount = singleBags,
            weightPerBagGrams = singleWeightPerBag,
            totalWeightGrams = preview.totalWeightGrams,
            ratePaisaPerQuintal = effectiveRate,
            aratBps = MandiMathEngine.parsePercentToBasisPoints(commissionPercent.ifBlank { "0" }),
            tulakPaisa = preview.tulakPaisa,
            grossPaisa = preview.grossPaisa,
            aratPaisa = preview.commissionPaisa,
            mandiShulkPaisa = preview.mandiShulkPaisa + preview.krishakCessPaisa,
            totalReceivablePaisa = preview.totalReceivablePaisa,
            receivedPaisa = preview.advanceReceivedPaisa,
            balanceDuePaisa = preview.balanceDuePaisa,
            vehicleNumber = phone.trim().ifBlank { null },
            invoiceNumber = if (billMode == BillMode.BILL_OF_SUPPLY) {
                "BOS-${(now % 100000).toString().padStart(5, '0')}"
            } else {
                null
            },
            remarks = combinedRemarks,
            createdAt = now,
            updatedAt = now,
            revision = 1L
        )
    }

    fun populateFrom(entry: SellEntry) {
        selectedBuyerId = entry.buyerId
        buyerName = entry.buyerId
        phone = entry.vehicleNumber ?: ""
        partyKind = entry.partyKind
        billMode = entry.billMode
        gstin = entry.gstin ?: ""
        commissionPercent = if (entry.aratBps > 0) MandiMathEngine.paisaToInputString(entry.aratBps) else "0"

        val rem = entry.remarks
        if (rem != null) {
            extractVillage(rem)?.let { village = it }
            if (rem.contains("काट: ")) {
                val tareQ = rem.substringAfter("काट: ").substringBefore("Q").trim()
                if (tareQ.isNotBlank()) tareQuintals = tareQ
            }
        }
        if (entry.receivedPaisa > 0L) {
            advanceReceived = MandiMathEngine.paisaToRupeesString(entry.receivedPaisa)
        }

        lots.clear()
        val savedLots = try {
            val jsonStr = if (rem != null && rem.contains("DATA: ")) {
                rem.substringAfter("DATA: ").trim()
            } else {
                rem
            }
            jsonStr?.let { Json.decodeFromString<List<SellCropLot>>(it) }
        } catch (_: Exception) {
            null
        }

        if (!savedLots.isNullOrEmpty()) {
            savedLots.forEach { lot ->
                lots.add(
                    SellLotItemState(
                        id = lot.id,
                        initialCropName = lot.cropName,
                        initialBags = if (lot.bags > 0) lot.bags.toString() else "",
                        initialWeightPerBagKg = if (lot.weightPerBagGrams > 0) (lot.weightPerBagGrams / 1000.0).toString() else "50",
                        initialExtraWeightKg = if (lot.extraWeightGrams > 0) (lot.extraWeightGrams / 1000.0).toString() else "",
                        initialRatePerQuintal = if (lot.ratePaisaPerQuintal > 0) MandiMathEngine.paisaToRupeesString(lot.ratePaisaPerQuintal) else "",
                    )
                )
            }
        } else {
            lots.add(
                SellLotItemState(
                    initialCropName = entry.commodityId,
                    initialBags = if (entry.bagsCount > 0) entry.bagsCount.toString() else "",
                    initialWeightPerBagKg = if (entry.weightPerBagGrams > 0) (entry.weightPerBagGrams / 1000.0).toString() else "50",
                    initialRatePerQuintal = if (entry.ratePaisaPerQuintal > 0) MandiMathEngine.paisaToRupeesString(entry.ratePaisaPerQuintal) else "",
                )
            )
        }
    }

    fun hasUnsavedChanges(originalEntry: SellEntry? = null): Boolean {
        if (originalEntry == null) {
            return buyerName.isNotBlank() ||
                phone.isNotBlank() ||
                village.isNotBlank() ||
                advanceReceived.isNotBlank() ||
                gstin.isNotBlank() ||
                invoiceDate.isNotBlank() ||
                lots.any { it.bags.isNotBlank() || it.ratePerQuintal.isNotBlank() || (it.weightPerBagKg.isNotBlank() && it.weightPerBagKg != "50") || it.extraWeightKg.isNotBlank() }
        }
        return buyerName != originalEntry.buyerId ||
            phone != (originalEntry.vehicleNumber ?: "") ||
            aggregatePreview().grossPaisa != originalEntry.grossPaisa
    }
}

fun extractVillage(remarks: String?): String? {
    if (remarks == null || !remarks.contains("गाँव: ")) return null
    return remarks.substringAfter("गाँव: ").substringBefore(" |").trim().ifBlank { null }
}
