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
        return LotValidation(
            bagsError = bagsVal.errorOrNull,
            weightError = weightVal.errorOrNull,
            rateError = rateVal.errorOrNull,
            extraError = extraVal.errorOrNull,
            isValid = bagsVal.isValid && weightVal.isValid && rateVal.isValid && extraVal.isValid
        )
    }
}

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
    val netPayablePaisa: Long
)

class BuyEntryFormState {
    var farmerName by mutableStateOf("")
    var phone by mutableStateOf("")
    var village by mutableStateOf("")

    val lots = mutableStateListOf<BuyLotItemState>()

    var commissionPercent by mutableStateOf("2")
    var labourPerBag by mutableStateOf("5")
    var advancePaid by mutableStateOf("") // Optional, default blank

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
        val totalWeight = lotResults.sumOf { it.second.totalWeightGrams }
        val totalGross = lotResults.sumOf { it.second.grossPaisa }

        val commBps = MandiMathEngine.parsePercentToBasisPoints(commissionPercent.ifBlank { "0" })
        val commissionPaisa = MandiMathEngine.percentageOf(totalGross, commBps)

        val labourPaisaPerBag = MandiMathEngine.parseRupeesToPaisa(labourPerBag.ifBlank { "0" })
        val labourPaisa = totalBags * labourPaisaPerBag

        val advancePaisa = MandiMathEngine.parseRupeesToPaisa(advancePaid.ifBlank { "0" })
        val netPayable = totalGross - commissionPaisa - labourPaisa - advancePaisa

        return AggregatedBuyPreview(
            lotsCalculations = lotResults,
            totalBags = totalBags,
            totalWeightGrams = totalWeight,
            totalGrossPaisa = totalGross,
            commissionPaisa = commissionPaisa,
            labourPaisa = labourPaisa,
            advancePaidPaisa = advancePaisa,
            netPayablePaisa = netPayable
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
    fun validatePhone(): ValidationResult = MandiValidators.validatePhone(phone, isRequired = false)
    fun validateLots(): List<LotValidation> = lots.map { it.validate() }

    fun canSave(): Boolean {
        if (!validateFarmerName().isValid) return false
        if (!validatePhone().isValid) return false
        val lotsVal = validateLots()
        if (lotsVal.isEmpty() || lotsVal.any { !it.isValid }) return false
        val agg = aggregatePreview()
        if (agg.totalGrossPaisa <= 0) return false
        if (agg.netPayablePaisa < 0) return false
        return true
    }

    fun validationReason(): String? {
        val nameVal = validateFarmerName()
        if (!nameVal.isValid) return nameVal.errorOrNull
        val phoneVal = validatePhone()
        if (!phoneVal.isValid) return phoneVal.errorOrNull
        val lotsVal = validateLots()
        val firstInvalidIdx = lotsVal.indexOfFirst { !it.isValid }
        if (firstInvalidIdx != -1) {
            val err = lotsVal[firstInvalidIdx]
            return "फसल #${firstInvalidIdx + 1}: ${err.bagsError ?: err.weightError ?: err.rateError ?: "विवरण सही करें"}"
        }
        val agg = aggregatePreview()
        if (agg.totalGrossPaisa <= 0) return "कुल राशि शून्य से अधिक होनी चाहिए"
        if (agg.netPayablePaisa < 0) return "चेतावनी: पेशगी राशि कुल हिसाब से अधिक है"
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
        val combinedRemarks = buildString {
            if (village.isNotBlank()) append("गाँव: ${village.trim()} | ")
            append("DATA: $lotsJson")
        }

        return BuyEntry(
            id = existingId ?: existingEntry?.id ?: IdGenerator.newId(),
            shopId = shopId,
            farmerId = farmerName.trim().ifBlank { "अज्ञात किसान" },
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
            paidPaisa = advancePaisa,
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
        farmerName = entry.farmerId
        phone = entry.vehicleNumber ?: ""
        commissionPercent = if (entry.commissionBps > 0) MandiMathEngine.paisaToInputString(entry.commissionBps) else "2"
        labourPerBag = if (entry.labourPerBagPaisa > 0) MandiMathEngine.paisaToInputString(entry.labourPerBagPaisa) else "5"
        advancePaid = if (entry.advancePaidPaisa > 0) MandiMathEngine.paisaToInputString(entry.advancePaidPaisa) else ""

        // Try extracting village and lots from remarks
        entry.remarks?.let { rem ->
            if (rem.contains("गाँव:")) {
                village = rem.substringAfter("गाँव:").substringBefore("|").trim()
            }
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
                                    initialWeightPerBagKg = if (lot.weightPerBagGrams > 0) (lot.weightPerBagGrams / 1000.0).toString() else "50",
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
                initialWeightPerBagKg = if (entry.weightPerBagGrams > 0) (entry.weightPerBagGrams / 1000.0).toString() else "50",
                initialExtraWeightKg = if (entry.extraWeightGrams > 0) (entry.extraWeightGrams / 1000.0).toString() else "",
                initialRatePerQuintal = if (entry.ratePaisaPerQuintal > 0) MandiMathEngine.paisaToInputString(entry.ratePaisaPerQuintal) else ""
            )
        )
    }
}
