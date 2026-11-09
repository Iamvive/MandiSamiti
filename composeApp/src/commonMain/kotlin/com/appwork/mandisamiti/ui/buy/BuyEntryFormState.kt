package com.appwork.mandisamiti.ui.buy

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.appwork.mandisamiti.domain.id.IdGenerator
import com.appwork.mandisamiti.domain.math.BuyMathResult
import com.appwork.mandisamiti.domain.math.MandiMathEngine
import com.appwork.mandisamiti.domain.model.BuyCropLot
import com.appwork.mandisamiti.domain.model.BuyEntry
import com.appwork.mandisamiti.domain.model.LendSettlement
import com.appwork.mandisamiti.ui.ledger.repayPreview
import com.appwork.mandisamiti.domain.model.TradeSettings
import com.appwork.mandisamiti.domain.validation.MandiValidators
import com.appwork.mandisamiti.domain.validation.ValidationResult
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class BuyLotItemState(
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

    fun calculateLot(): BuyCropLot {
        val b = bags.toLongOrNull() ?: 0L
        val wGrams = MandiMathEngine.parseKgToGrams(weightPerBagKg.ifBlank { "0" })
        val extraGrams = MandiMathEngine.parseKgToGrams(extraWeightKg.ifBlank { "0" })
        val ratePaisa = MandiMathEngine.parseRupeesToPaisa(ratePerQuintal.ifBlank { "0" })
        return BuyCropLot.create(
            id = id,
            cropName = cropName.ifBlank { "गेहूं" },
            bags = b,
            weightPerBagGrams = wGrams,
            extraWeightGrams = extraGrams,
            ratePaisaPerQuintal = ratePaisa
        )
    }

    fun validate(): LotValidation {
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
        return LotValidation(
            bagsError = bagsError,
            weightError = weightError,
            rateError = rateError,
            extraError = extraError,
            isValid = bagsError == null && weightError == null && rateError == null && extraError == null
        )
    }
}

const val MAX_BAGS = 9_999L
const val MAX_BAG_WEIGHT_KG = 999L
const val MAX_RATE_RUPEES = 100_000L

data class LotValidation(
    val bagsError: String?,
    val weightError: String?,
    val rateError: String?,
    val extraError: String?,
    val isValid: Boolean
)

data class AggregatedBuyPreview(
    val lotsCalculations: List<Pair<BuyLotItemState, BuyCropLot>>,
    val totalBags: Long,
    val totalWeightGrams: Long,
    val totalGrossPaisa: Long,
    val commissionPaisa: Long,
    val labourPaisa: Long,
    val advancePaidPaisa: Long,
    val netPayablePaisa: Long,
    val loanCutPaisa: Long = 0L
) {
    /** Still to hand over after the advance and the loan cut. */
    val payoutPaisa: Long get() = netPayablePaisa - advancePaidPaisa - loanCutPaisa
}

class BuyEntryFormState(
    tradeSettings: TradeSettings? = null
) {
    var farmerName by mutableStateOf("")
    var selectedFarmerId: String? by mutableStateOf(null)
    var phone by mutableStateOf("")
    // Raw text while typing so spaces survive; sanitizeVillage runs on validate/save.
    var village by mutableStateOf("")

    val lots = mutableStateListOf<BuyLotItemState>()

    var commissionPercent by mutableStateOf(
        tradeSettings?.let { MandiMathEngine.paisaToInputString(it.farmerCommissionBps) } ?: "0.7"
    )
    var labourPerBag by mutableStateOf(
        tradeSettings?.let { MandiMathEngine.paisaToRupeesString(it.defaultLabourPaisa) } ?: "10"
    )
    var advancePaid by mutableStateOf("") // Optional, default blank
    var showRateOutlierConfirmation by mutableStateOf(false)

    // Loan section: the caller sets the person's open money loan; the cut is typed in whole rupees.
    var loanLendId: String? by mutableStateOf(null)
        private set
    var loanPrincipalPaisa by mutableStateOf(0L)
        private set
    var loanInterestDuePaisa by mutableStateOf(0L)
        private set
    var loanCut by mutableStateOf("")

    var depositPaisa by mutableStateOf(0L)
        private set

    fun setDeposit(paisa: Long) {
        depositPaisa = paisa
    }

    fun setLoan(lendId: String?, principalPaisa: Long, interestDuePaisa: Long) {
        loanLendId = lendId
        loanPrincipalPaisa = principalPaisa
        loanInterestDuePaisa = interestDuePaisa
        loanCut = ""
    }

    fun loanCutPaisa(): Long = (loanCut.toLongOrNull() ?: 0L) * 100L

    /** Interest first, then principal, as in RepayLoanDialog. */
    fun loanCutSplit() = repayPreview(loanCutPaisa(), loanInterestDuePaisa, loanPrincipalPaisa)

    fun loanCutError(): String? {
        val cut = loanCutPaisa()
        if (cut == 0L) return null
        if (loanLendId == null) return "इस पार्टी का कोई उधार नहीं है"
        if (cut > loanPrincipalPaisa + loanInterestDuePaisa) return "कटौती बकाया उधार से अधिक है"
        return null
    }

    /** The settlement saved with the slip, or null when no cut was entered. */
    fun buildLoanCut(shopId: String, now: Long): LendSettlement? {
        val cut = loanCutPaisa()
        val lendId = loanLendId
        if (cut <= 0L || lendId == null) return null
        val split = loanCutSplit()
        return LendSettlement(
            id = IdGenerator.newId(), shopId = shopId, lendId = lendId, settledAt = now,
            amountPaisa = split.interestPaisa + split.principalPaisa,
            createdAt = now, updatedAt = now,
            principalPaisa = split.principalPaisa, interestPaisa = split.interestPaisa,
            calculatedInterestPaisa = loanInterestDuePaisa,
        )
    }

    fun applySettings(settings: TradeSettings) {
        commissionPercent = MandiMathEngine.paisaToInputString(settings.farmerCommissionBps)
        labourPerBag = MandiMathEngine.paisaToRupeesString(settings.defaultLabourPaisa)
    }

    fun hasRateOutlier(): Boolean {
        return lots.any { lot ->
            val ratePaisa = MandiMathEngine.parseRupeesToPaisa(lot.ratePerQuintal.ifBlank { "0" })
            ratePaisa > 0L && MandiValidators.isRateOutlier(ratePaisa)
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

    init {
        if (lots.isEmpty()) {
            lots.add(BuyLotItemState())
        }
    }

    // Single-lot legacy accessors for backwards compatibility
    var bags: String
        get() = ensureLot().bags
        set(value) { ensureLot().bags = value.filter { it.isDigit() } }

    var weightPerBagKg: String
        get() = ensureLot().weightPerBagKg
        set(value) { ensureLot().weightPerBagKg = value }

    var extraWeightKg: String
        get() = ensureLot().extraWeightKg
        set(value) { ensureLot().extraWeightKg = value }

    var ratePerQuintal: String
        get() = ensureLot().ratePerQuintal
        set(value) { ensureLot().ratePerQuintal = value }

    private fun ensureLot(): BuyLotItemState {
        if (lots.isEmpty()) {
            lots.add(BuyLotItemState())
        }
        return lots[0]
    }

    fun addLot(cropName: String = "गेहूं") {
        lots.add(BuyLotItemState(initialCropName = cropName, initialWeightPerBagKg = "50"))
    }

    fun removeLot(index: Int) {
        if (lots.size > 1 && index in lots.indices) {
            lots.removeAt(index)
        }
    }

    fun updatePhone(input: String) {
        // Indian phone numbers: max 10 digits
        phone = input.filter { it.isDigit() }.take(10)
    }

    fun aggregatePreview(): AggregatedBuyPreview {
        val lotResults = lots.map { it to it.calculateLot() }
        val totalBags = lotResults.sumOf { it.second.bags }
        val totalGrossWeight = lotResults.sumOf { it.second.totalWeightGrams }
        val totalWeight = totalGrossWeight
        val totalGross = lotResults.sumOf { it.second.grossPaisa }

        val commBps = MandiMathEngine.parsePercentToBasisPoints(commissionPercent.ifBlank { "0" })
        val labourPaisaPerBag = MandiMathEngine.parseRupeesToPaisa(labourPerBag.ifBlank { "0" })
        val advancePaisa = MandiMathEngine.parseRupeesToPaisa(advancePaid.ifBlank { "0" })

        // Sheet Rule 2: Extra loose weight > 5kg adds 1 billed bag for labour
        val totalExtraGrams = lotResults.sumOf { it.second.extraWeightGrams }
        val billedBags = totalBags + if (totalExtraGrams > 5000L) 1L else 0L
        val labourPaisa = billedBags * labourPaisaPerBag

        // Sheet Rule 3 & 4: Final rounded to nearest Rs 10, commission absorbs rounding
        val commRaw = MandiMathEngine.percentageOf(totalGross, commBps)
        val unroundedFinal = totalGross - labourPaisa - commRaw
        var roundedFinal = ((unroundedFinal + 500L) / 1000L) * 1000L
        // Rounding up must never turn commission negative (blank or 0% rate): round down instead.
        if (totalGross - labourPaisa - roundedFinal < 0L) roundedFinal = (unroundedFinal / 1000L) * 1000L
        val commissionPaisa = totalGross - labourPaisa - roundedFinal
        // Net is the commodity value only (Vivek's sheet, 2026-11-01). Advance is money already paid, tracked in paid.
        val netPayable = roundedFinal

        return AggregatedBuyPreview(
            lotsCalculations = lotResults,
            totalBags = totalBags,
            totalWeightGrams = totalWeight,
            totalGrossPaisa = totalGross,
            commissionPaisa = commissionPaisa,
            labourPaisa = labourPaisa,
            advancePaidPaisa = advancePaisa,
            netPayablePaisa = netPayable,
            loanCutPaisa = loanCutPaisa()
        )
    }

    fun preview(): BuyMathResult {
        val agg = aggregatePreview()
        return BuyMathResult(
            totalWeightGrams = agg.totalWeightGrams,
            grossPaisa = agg.totalGrossPaisa,
            commissionPaisa = agg.commissionPaisa,
            labourPaisa = agg.labourPaisa,
            netPayablePaisa = agg.netPayablePaisa
        )
    }

    fun validateFarmerName(): ValidationResult = MandiValidators.validateFarmerName(farmerName)
    fun validatePhone(): ValidationResult = MandiValidators.validatePhone(phone, isRequired = true)
    fun validateVillage(): ValidationResult =
        if (MandiValidators.sanitizeVillage(village).isBlank()) ValidationResult.Invalid("गाँव का नाम दर्ज करें") else ValidationResult.Valid
    fun validateLots(): List<LotValidation> = lots.map { it.validate() }

    fun canSave(): Boolean {
        if (!validateFarmerName().isValid) return false
        if (!validatePhone().isValid) return false
        if (!validateVillage().isValid) return false
        val lotsVal = validateLots()
        if (lotsVal.isEmpty() || lotsVal.any { !it.isValid }) return false
        val agg = aggregatePreview()
        if (agg.totalGrossPaisa <= 0) return false
        if (agg.payoutPaisa < 0) return false
        if (loanCutError() != null) return false
        return true
    }

    fun validationReason(): String? {
        val nameVal = validateFarmerName()
        if (!nameVal.isValid) return nameVal.errorOrNull
        val phoneVal = validatePhone()
        if (!phoneVal.isValid) return phoneVal.errorOrNull
        val villageVal = validateVillage()
        if (!villageVal.isValid) return villageVal.errorOrNull
        val lotsVal = validateLots()
        val firstInvalidIdx = lotsVal.indexOfFirst { !it.isValid }
        if (firstInvalidIdx != -1) {
            val err = lotsVal[firstInvalidIdx]
            return "फसल #${firstInvalidIdx + 1}: ${err.bagsError ?: err.weightError ?: err.extraError ?: err.rateError ?: "विवरण सही करें"}"
        }
        val agg = aggregatePreview()
        if (agg.totalGrossPaisa <= 0) return "कुल राशि शून्य से अधिक होनी चाहिए"
        loanCutError()?.let { return it }
        if (agg.payoutPaisa < 0) return "चेतावनी: पेशगी और उधार कटौती कुल हिसाब से अधिक है"
        return null
    }

    fun toBuyEntry(
        shopId: String,
        existingId: String? = null,
        existingEntry: BuyEntry? = null,
        clock: Clock = Clock.System
    ): BuyEntry {
        val agg = aggregatePreview()
        val now = clock.now().toEpochMilliseconds()
        val commBps = MandiMathEngine.parsePercentToBasisPoints(commissionPercent.ifBlank { "0" })
        val labourPaisaPerBag = MandiMathEngine.parseRupeesToPaisa(labourPerBag.ifBlank { "0" })
        val advancePaisa = MandiMathEngine.parseRupeesToPaisa(advancePaid.ifBlank { "0" })
        val primaryLot = lots.firstOrNull()?.calculateLot()

        // Build lots domain list
        val domainLots = lots.map { it.calculateLot() }
        val lotsJson = Json.encodeToString(domainLots)
        val cleanVillage = MandiValidators.sanitizeVillage(village)
        val cutPaisa = if (agg.loanCutPaisa > 0) agg.loanCutPaisa else extractLoanCut(existingEntry?.remarks)
        val combinedRemarks = buildString {
            if (cleanVillage.isNotBlank()) append("गाँव: $cleanVillage | ")
            if (cutPaisa > 0) append("उधार कटौती: $cutPaisa | ")
            append("DATA: $lotsJson")
        }

        return BuyEntry(
            id = existingId ?: existingEntry?.id ?: IdGenerator.newId(),
            shopId = shopId,
            farmerId = selectedFarmerId ?: farmerName.trim().ifBlank { "अज्ञात किसान" },
            commodityId = lots.firstOrNull()?.cropName ?: "गेहूं",
            entryDate = existingEntry?.entryDate ?: now,
            bagsCount = agg.totalBags,
            weightPerBagGrams = primaryLot?.weightPerBagGrams ?: 50_000L,
            extraWeightGrams = primaryLot?.extraWeightGrams ?: 0L,
            totalWeightGrams = agg.totalWeightGrams,
            ratePaisaPerQuintal = primaryLot?.ratePaisaPerQuintal ?: 0L,
            commissionBps = commBps,
            labourPerBagPaisa = labourPaisaPerBag,
            advancePaidPaisa = advancePaisa,
            grossPaisa = agg.totalGrossPaisa,
            commissionPaisa = agg.commissionPaisa,
            labourPaisa = agg.labourPaisa,
            netPayablePaisa = agg.netPayablePaisa,
            paidPaisa = advancePaisa + agg.loanCutPaisa,
            vehicleNumber = phone.ifBlank { null },
            slipNumber = existingEntry?.slipNumber,
            remarks = combinedRemarks,
            createdAt = existingEntry?.createdAt ?: now,
            updatedAt = now,
            revision = (existingEntry?.revision ?: 0L) + 1L,
            isVoid = false,
            voidReason = null,
            syncStatus = 0L
        )
    }

    fun populateFrom(entry: BuyEntry) {
        selectedFarmerId = entry.farmerId
        farmerName = entry.farmerId
        phone = entry.vehicleNumber ?: ""
        commissionPercent = if (entry.commissionBps > 0) MandiMathEngine.paisaToInputString(entry.commissionBps) else "0"
        labourPerBag = if (entry.labourPerBagPaisa > 0) MandiMathEngine.paisaToRupeesString(entry.labourPerBagPaisa) else "0"
        advancePaid = if (entry.advancePaidPaisa > 0) MandiMathEngine.paisaToInputString(entry.advancePaidPaisa) else ""

        // Try extracting village and lots from remarks
        entry.remarks?.let { rem ->
            extractVillage(rem)?.let { village = it }
            if (rem.contains("DATA: ")) {
                val jsonPart = rem.substringAfter("DATA: ").trim()
                try {
                    val parsedLots = Json.decodeFromString<List<BuyCropLot>>(jsonPart)
                    if (parsedLots.isNotEmpty()) {
                        lots.clear()
                        parsedLots.forEach { lot ->
                            lots.add(
                                BuyLotItemState(
                                    id = lot.id,
                                    initialCropName = lot.cropName,
                                    initialBags = if (lot.bags > 0) lot.bags.toString() else "",
                                    initialWeightPerBagKg = if (lot.weightPerBagGrams > 0) MandiMathEngine.paisaToInputString(lot.weightPerBagGrams / 10) else "50",
                                    initialExtraWeightKg = if (lot.extraWeightGrams > 0) (lot.extraWeightGrams / 1000.0).toString() else "",
                                    initialRatePerQuintal = if (lot.ratePaisaPerQuintal > 0) MandiMathEngine.paisaToInputString(lot.ratePaisaPerQuintal) else ""
                                )
                            )
                        }
                        return
                    }
                } catch (_: Exception) {}
            }
        }

        // Fallback to single lot
        lots.clear()
        lots.add(
            BuyLotItemState(
                initialCropName = entry.commodityId,
                initialBags = if (entry.bagsCount > 0) entry.bagsCount.toString() else "",
                initialWeightPerBagKg = if (entry.weightPerBagGrams > 0) MandiMathEngine.paisaToInputString(entry.weightPerBagGrams / 10) else "50",
                initialExtraWeightKg = if (entry.extraWeightGrams > 0) (entry.extraWeightGrams / 1000.0).toString() else "",
                initialRatePerQuintal = if (entry.ratePaisaPerQuintal > 0) MandiMathEngine.paisaToInputString(entry.ratePaisaPerQuintal) else ""
            )
        )
    }

    fun hasUnsavedChanges(originalEntry: BuyEntry? = null): Boolean {
        if (originalEntry == null) {
            return farmerName.isNotBlank() ||
                phone.isNotBlank() ||
                village.isNotBlank() ||
                advancePaid.isNotBlank() ||
                lots.any { it.bags.isNotBlank() || it.ratePerQuintal.isNotBlank() || (it.weightPerBagKg.isNotBlank() && it.weightPerBagKg != "50") || it.extraWeightKg.isNotBlank() }
        }
        val origVillage = extractVillage(originalEntry.remarks) ?: ""
        val origPhone = originalEntry.vehicleNumber ?: ""
        val origAdvance = if (originalEntry.advancePaidPaisa > 0) MandiMathEngine.paisaToInputString(originalEntry.advancePaidPaisa) else ""
        return farmerName != originalEntry.farmerId ||
            phone != origPhone ||
            village != origVillage ||
            advancePaid != origAdvance ||
            preview().grossPaisa != originalEntry.grossPaisa
    }
}

fun extractVillage(remarks: String?): String? {
    if (remarks.isNullOrBlank()) return null
    if (remarks.contains("गाँव:")) {
        val v = remarks.substringAfter("गाँव:").substringBefore("|").trim()
        if (v.isNotBlank()) return v
    }
    return null
}

fun extractLoanCut(remarks: String?): Long {
    if (remarks.isNullOrBlank()) return 0L
    if (remarks.contains("उधार कटौती:")) {
        val cutStr = remarks.substringAfter("उधार कटौती:").substringBefore("|").trim()
        val paisa = cutStr.toLongOrNull()
        if (paisa != null && paisa > 0L) return paisa
    }
    val match = Regex("""(?:उधार कटौती|Loan cut|LOAN_CUT):\s*([0-9]+)""").find(remarks)
    return match?.groupValues?.get(1)?.toLongOrNull() ?: 0L
}


