package com.appwork.mandisamiti.ui.forms

import com.appwork.mandisamiti.domain.model.BillMode
import com.appwork.mandisamiti.domain.model.BuyEntry
import com.appwork.mandisamiti.domain.model.LendEntry
import com.appwork.mandisamiti.domain.model.LendType
import com.appwork.mandisamiti.domain.model.Party
import com.appwork.mandisamiti.domain.model.PartyType
import com.appwork.mandisamiti.domain.model.SellEntry
import com.appwork.mandisamiti.domain.model.SellPartyKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PartyAutocompleteAndDuplicateGuardF15Test {

    private val testParties = listOf(
        Party(
            id = "p-1",
            shopId = "shop-1",
            name = "RadheyShyam Verma",
            phone = "9826012345",
            village = "Khedi",
            partyType = PartyType.FARMER,
            createdAt = 1000L,
            updatedAt = 1000L
        ),
        Party(
            id = "p-2",
            shopId = "shop-1",
            name = "Kailash Patel",
            phone = "9425098765",
            village = "Pipariya",
            partyType = PartyType.FARMER,
            createdAt = 1000L,
            updatedAt = 1000L
        ),
        Party(
            id = "p-3",
            shopId = "shop-1",
            name = "Maa Sharda Traders",
            phone = "9826055555",
            village = "Mandi Yard",
            partyType = PartyType.BUYER,
            createdAt = 1000L,
            updatedAt = 1000L
        )
    )

    @Test
    fun partyAutocomplete_matchesByNameVillageAndPhone() {
        fun filterParties(query: String, type: PartyType? = null): List<Party> {
            val q = query.trim()
            if (q.isEmpty()) return emptyList()
            return testParties.filter { p ->
                (type == null || p.partyType == type) &&
                (p.name.contains(q, ignoreCase = true) ||
                 (p.village?.contains(q, ignoreCase = true) == true) ||
                 (p.phone?.contains(q) == true))
            }
        }

        // Search by name
        val nameMatch = filterParties("radhey")
        assertEquals(1, nameMatch.size)
        assertEquals("RadheyShyam Verma", nameMatch.first().name)

        // Search by village
        val villageMatch = filterParties("khedi")
        assertEquals(1, villageMatch.size)
        assertEquals("RadheyShyam Verma", villageMatch.first().name)

        // Search by phone
        val phoneMatch = filterParties("98765")
        assertEquals(1, phoneMatch.size)
        assertEquals("Kailash Patel", phoneMatch.first().name)

        // Search with PartyType filter
        val farmerMatches = filterParties("98260", PartyType.FARMER)
        assertEquals(1, farmerMatches.size)
        assertEquals("RadheyShyam Verma", farmerMatches.first().name)

        val buyerMatches = filterParties("98260", PartyType.BUYER)
        assertEquals(1, buyerMatches.size)
        assertEquals("Maa Sharda Traders", buyerMatches.first().name)
    }

    @Test
    fun buyDuplicateGuard_flagsIdenticalEntryWithin2Minutes() {
        val now = 1_000_000L
        val existingBuy = BuyEntry(
            id = "buy-1",
            shopId = "shop-1",
            farmerId = "RadheyShyam",
            commodityId = "गेहूं लोकवान",
            entryDate = now - 45_000L, // 45 seconds ago
            bagsCount = 30L,
            weightPerBagGrams = 50_000L,
            extraWeightGrams = 0L,
            totalWeightGrams = 1_500_000L,
            ratePaisaPerQuintal = 245_000L,
            commissionBps = 150L,
            labourPerBagPaisa = 500L,
            advancePaidPaisa = 0L,
            grossPaisa = 36_750_000L,
            commissionPaisa = 551_250L,
            labourPaisa = 15_000L,
            netPayablePaisa = 36_183_750L,
            createdAt = now - 45_000L,
            updatedAt = now - 45_000L
        )

        fun checkBuyDuplicate(candidate: BuyEntry, recents: List<BuyEntry>, checkTime: Long): Boolean {
            return recents.any { prev ->
                !prev.isVoid &&
                prev.farmerId.trim().equals(candidate.farmerId.trim(), ignoreCase = true) &&
                prev.netPayablePaisa == candidate.netPayablePaisa &&
                prev.bagsCount == candidate.bagsCount &&
                prev.commodityId.trim().equals(candidate.commodityId.trim(), ignoreCase = true) &&
                kotlin.math.abs(checkTime - prev.createdAt) <= 2 * 60 * 1000L
            }
        }

        // Same entry 45s later -> DUPLICATE
        val identicalCandidate = existingBuy.copy(id = "buy-2", createdAt = now)
        assertTrue(checkBuyDuplicate(identicalCandidate, listOf(existingBuy), now))

        // Different farmer -> NOT duplicate
        val diffFarmer = identicalCandidate.copy(farmerId = "Kailash")
        assertFalse(checkBuyDuplicate(diffFarmer, listOf(existingBuy), now))

        // Different amount -> NOT duplicate
        val diffAmount = identicalCandidate.copy(netPayablePaisa = 35_000_000L)
        assertFalse(checkBuyDuplicate(diffAmount, listOf(existingBuy), now))

        // Different bags -> NOT duplicate
        val diffBags = identicalCandidate.copy(bagsCount = 25L)
        assertFalse(checkBuyDuplicate(diffBags, listOf(existingBuy), now))

        // Saved 3 minutes later -> NOT duplicate (> 2 min window)
        assertFalse(checkBuyDuplicate(identicalCandidate, listOf(existingBuy), now + 3 * 60 * 1000L))
    }

    @Test
    fun sellDuplicateGuard_flagsIdenticalEntryWithin2Minutes() {
        val now = 2_000_000L
        val existingSell = SellEntry(
            id = "sell-1",
            shopId = "shop-1",
            buyerId = "Maa Sharda Traders",
            commodityId = "चना",
            entryDate = now - 30_000L, // 30s ago
            billMode = BillMode.NORMAL,
            partyKind = SellPartyKind.MERCHANT,
            bagsCount = 20L,
            weightPerBagGrams = 50_000L,
            totalWeightGrams = 1_000_000L,
            ratePaisaPerQuintal = 520_000L,
            aratBps = 100L,
            tulakPaisa = 500L,
            grossPaisa = 52_000_000L,
            aratPaisa = 520_000L,
            mandiShulkPaisa = 0L,
            totalReceivablePaisa = 52_620_000L,
            receivedPaisa = 0L,
            balanceDuePaisa = 52_620_000L,
            createdAt = now - 30_000L,
            updatedAt = now - 30_000L
        )

        fun checkSellDuplicate(candidate: SellEntry, recents: List<SellEntry>, checkTime: Long): Boolean {
            return recents.any { prev ->
                !prev.isVoid &&
                prev.buyerId.trim().equals(candidate.buyerId.trim(), ignoreCase = true) &&
                prev.totalReceivablePaisa == candidate.totalReceivablePaisa &&
                prev.bagsCount == candidate.bagsCount &&
                prev.commodityId.trim().equals(candidate.commodityId.trim(), ignoreCase = true) &&
                kotlin.math.abs(checkTime - prev.createdAt) <= 2 * 60 * 1000L
            }
        }

        // Identical -> DUPLICATE
        val candidate = existingSell.copy(id = "sell-2", createdAt = now)
        assertTrue(checkSellDuplicate(candidate, listOf(existingSell), now))

        // Different commodity -> NOT duplicate
        val diffCrop = candidate.copy(commodityId = "सोयाबीन")
        assertFalse(checkSellDuplicate(diffCrop, listOf(existingSell), now))

        // Beyond 2 minutes -> NOT duplicate
        assertFalse(checkSellDuplicate(candidate, listOf(existingSell), now + 150_000L))
    }

    @Test
    fun lendDuplicateGuard_flagsIdenticalEntryWithin2Minutes() {
        val now = 3_000_000L
        val existingLend = LendEntry(
            id = "lend-1",
            shopId = "shop-1",
            partyId = "RadheyShyam",
            lendType = LendType.MONEY,
            entryDate = now - 20_000L,
            amountPaisa = 500_000L,
            createdAt = now - 20_000L,
            updatedAt = now - 20_000L
        )

        fun checkLendDuplicate(candidate: LendEntry, recents: List<LendEntry>, checkTime: Long): Boolean {
            return recents.any { prev ->
                !prev.isVoid &&
                prev.partyId.trim().equals(candidate.partyId.trim(), ignoreCase = true) &&
                prev.lendType == candidate.lendType &&
                (if (candidate.lendType == LendType.MONEY) prev.amountPaisa == candidate.amountPaisa else (prev.commodityId == candidate.commodityId && prev.weightGrams == candidate.weightGrams)) &&
                kotlin.math.abs(checkTime - prev.createdAt) <= 2 * 60 * 1000L
            }
        }

        val candidate = existingLend.copy(id = "lend-2", createdAt = now)
        assertTrue(checkLendDuplicate(candidate, listOf(existingLend), now))

        val diffAmount = candidate.copy(amountPaisa = 600_000L)
        assertFalse(checkLendDuplicate(diffAmount, listOf(existingLend), now))

        assertFalse(checkLendDuplicate(candidate, listOf(existingLend), now + 180_000L))
    }
}
