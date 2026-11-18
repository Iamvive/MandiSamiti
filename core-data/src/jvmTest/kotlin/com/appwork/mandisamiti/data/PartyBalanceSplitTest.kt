package com.appwork.mandisamiti.data

import com.appwork.mandisamiti.data.repository.OfflineFirstBuyRepository
import com.appwork.mandisamiti.data.repository.OfflineFirstLendRepository
import com.appwork.mandisamiti.data.repository.OfflineFirstPartyRepository
import com.appwork.mandisamiti.database.createTestDatabase
import com.appwork.mandisamiti.domain.model.BuyEntry
import com.appwork.mandisamiti.domain.model.LendDirection
import com.appwork.mandisamiti.domain.model.LendEntry
import com.appwork.mandisamiti.domain.model.LendSettlement
import com.appwork.mandisamiti.domain.model.LendType
import com.appwork.mandisamiti.domain.model.Party
import com.appwork.mandisamiti.domain.model.PartyType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** Slip money and udhaar money are separate balances; a slip payment never moves udhaar. */
class PartyBalanceSplitTest {
    private val party = Party("p1", "shop", "TEST रमेश", "9876543210", "TEST गाँव", PartyType.FARMER, createdAt = 1, updatedAt = 1)

    private fun loan(id: String, amount: Long, direction: LendDirection = LendDirection.GIVEN, isVoid: Boolean = false) = LendEntry(
        id = id, shopId = "shop", partyId = "p1", lendType = LendType.MONEY, entryDate = 1,
        amountPaisa = amount, createdAt = 1, updatedAt = 1, direction = direction, isVoid = isVoid,
    )

    private fun slip(netPayable: Long, paid: Long) = BuyEntry(
        id = "b1", shopId = "shop", farmerId = "p1", commodityId = "गेहूं", entryDate = 1, bagsCount = 10,
        weightPerBagGrams = 50_000, extraWeightGrams = 0, totalWeightGrams = 500_000, ratePaisaPerQuintal = 200_000,
        commissionBps = 0, labourPerBagPaisa = 0, advancePaidPaisa = 0, grossPaisa = netPayable,
        commissionPaisa = 0, labourPaisa = 0, netPayablePaisa = netPayable, paidPaisa = paid,
        createdAt = 1, updatedAt = 1,
    )

    private suspend fun balance(db: com.appwork.mandisamiti.database.AppDatabase) =
        OfflineFirstPartyRepository(db).getPartyBalanceStream("p1").first()!!

    private suspend fun setup(): com.appwork.mandisamiti.database.AppDatabase {
        val db = createTestDatabase()
        OfflineFirstPartyRepository(db).saveParty(party)
        return db
    }

    @Test
    fun givenLoanOnlyMovesUdhaarGiven() = runTest {
        val db = setup()
        OfflineFirstLendRepository(db).saveLendEntry(loan("l1", 1_000_000))
        val b = balance(db)
        assertEquals(1_000_000L, b.udhaarGivenPaisa)
        assertEquals(0L, b.slipBalancePaisa)
    }

    @Test
    fun slipPaymentNeverChangesUdhaar() = runTest {
        val db = setup()
        OfflineFirstLendRepository(db).saveLendEntry(loan("l1", 1_000_000))
        OfflineFirstBuyRepository(db).saveBuyEntry(slip(netPayable = 1_000_000, paid = 400_000))
        val b = balance(db)
        assertEquals(-600_000L, b.slipBalancePaisa)
        assertEquals(1_000_000L, b.udhaarGivenPaisa)
    }

    @Test
    fun repaymentReducesUdhaarAndLeavesSlipAlone() = runTest {
        val db = setup()
        val lend = OfflineFirstLendRepository(db)
        lend.saveLendEntry(loan("l1", 1_000_000))
        lend.settle(
            LendSettlement(
                id = "s1", shopId = "shop", lendId = "l1", settledAt = 2, amountPaisa = 400_000,
                createdAt = 2, updatedAt = 2, principalPaisa = 400_000, interestPaisa = 0, calculatedInterestPaisa = 0,
            )
        )
        val b = balance(db)
        assertEquals(600_000L, b.udhaarGivenPaisa)
        assertEquals(0L, b.slipBalancePaisa)
    }

    @Test
    fun takenLoanIsNettedSeparately() = runTest {
        val db = setup()
        val lend = OfflineFirstLendRepository(db)
        lend.saveLendEntry(loan("l1", 600_000))
        lend.saveLendEntry(loan("l2", 200_000, LendDirection.TAKEN))
        val b = balance(db)
        assertEquals(600_000L, b.udhaarGivenPaisa)
        assertEquals(200_000L, b.udhaarTakenPaisa)
        assertEquals(400_000L, b.udhaarNetPaisa)
    }

    @Test
    fun voidedLoanIsIgnored() = runTest {
        val db = setup()
        OfflineFirstLendRepository(db).saveLendEntry(loan("l1", 1_000_000, isVoid = true))
        val b = balance(db)
        assertEquals(0L, b.udhaarGivenPaisa)
        assertEquals(0L, b.udhaarTakenPaisa)
    }
}
