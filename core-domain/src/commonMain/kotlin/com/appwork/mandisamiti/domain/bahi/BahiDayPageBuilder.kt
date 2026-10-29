package com.appwork.mandisamiti.domain.bahi

enum class BahiSide { LEFT, RIGHT }

enum class BahiPaymentMode {
    CASH, RTGS, NEFT, CHEQUE, UPI, ONLINE;

    val isCash: Boolean get() = this == CASH
}

enum class BahiEntryKind {
    OPENING_BALANCE,
    FARMER_MONEY_LEFT,
    BUYER_PAYMENT,
    LOAN_PRINCIPAL_REPAID,
    LOAN_INTEREST,
    ADVANCE_CARRIED_IN,
    EXPENSE,
    ADVANCE_PAID,
    FARMER_PAYMENT,
    BUYER_BANK_RECEIPT,
    LABOUR,
    GRAIN_LEDGER,
    FARMER_RECEIPT_NET,
    CLOSING_CASH,
    ONLINE_COUNTERPART,
    DAY_CLOSE_DIFF,
}

data class BahiDayEntry(
    val id: String,
    val kind: BahiEntryKind,
    val label: String,
    val amountPaisa: Long,
    val paymentMode: BahiPaymentMode = BahiPaymentMode.CASH,
    val occurredAt: Long = 0L,
    val isVoid: Boolean = false,
    val isSameDayAdvance: Boolean = false,
    val isUnusualAmount: Boolean = false,
    val savedAfterClose: Boolean = false,
)

data class BahiDayLine(
    val id: String,
    val kind: BahiEntryKind,
    val label: String,
    val amountPaisa: Long,
    val paymentMode: BahiPaymentMode,
    val isVoid: Boolean = false,
    val isStruckThrough: Boolean = isVoid,
)

data class BahiLikelyCause(val entryId: String, val label: String)

data class BahiDayPage(
    val dateEpochDay: Long,
    val left: List<BahiDayLine>,
    val right: List<BahiDayLine>,
    val leftTotalPaisa: Long,
    val rightTotalPaisa: Long,
    val differencePaisa: Long,
    val likelyCauses: List<BahiLikelyCause>,
    val changedAfterClosing: Boolean,
) {
    val isBalanced: Boolean get() = differencePaisa == 0L
}

/** Pure accounting projection for one two-sided paper-Bahi day. */
object BahiDayPageBuilder {
    fun build(
        dateEpochDay: Long,
        openingPaisa: Long,
        closingPaisa: Long,
        entries: List<BahiDayEntry>,
        closeMismatch: Boolean = false,
    ): BahiDayPage {
        require(openingPaisa >= 0L && closingPaisa >= 0L)
        require(entries.all { it.amountPaisa >= 0L })

        val left = mutableListOf(
            BahiDayLine("opening:$dateEpochDay", BahiEntryKind.OPENING_BALANCE, "Opening balance", openingPaisa, BahiPaymentMode.CASH)
        )
        val right = mutableListOf<BahiDayLine>()

        entries.sortedBy { it.occurredAt }.forEach { entry ->
            val line = entry.toLine()
            when (entry.kind) {
                BahiEntryKind.OPENING_BALANCE, BahiEntryKind.CLOSING_CASH, BahiEntryKind.DAY_CLOSE_DIFF -> Unit
                BahiEntryKind.FARMER_MONEY_LEFT,
                BahiEntryKind.BUYER_PAYMENT,
                BahiEntryKind.LOAN_PRINCIPAL_REPAID,
                BahiEntryKind.LOAN_INTEREST,
                BahiEntryKind.ADVANCE_CARRIED_IN -> left += line
                BahiEntryKind.EXPENSE,
                BahiEntryKind.ADVANCE_PAID,
                BahiEntryKind.FARMER_PAYMENT,
                BahiEntryKind.BUYER_BANK_RECEIPT,
                BahiEntryKind.LABOUR,
                BahiEntryKind.GRAIN_LEDGER,
                BahiEntryKind.FARMER_RECEIPT_NET,
                BahiEntryKind.ONLINE_COUNTERPART -> right += line
            }

            // Online money is written on both sides; the counterpart keeps drawer cash unchanged.
            if (!entry.paymentMode.isCash && entry.kind != BahiEntryKind.BUYER_BANK_RECEIPT && entry.kind != BahiEntryKind.ONLINE_COUNTERPART) {
                val counterpart = line.copy(
                    id = "${entry.id}:online",
                    kind = BahiEntryKind.ONLINE_COUNTERPART,
                    label = "${entry.label} · ${entry.paymentMode.name}",
                )
                if (entry.kind.isLeftKind()) right += counterpart else left += counterpart
            }
        }

        right += BahiDayLine("closing:$dateEpochDay", BahiEntryKind.CLOSING_CASH, "Closing cash", closingPaisa, BahiPaymentMode.CASH)
        val rawDifference = activeTotal(left) - activeTotal(right)
        if (closeMismatch && rawDifference != 0L) {
            // A shortage is the normal close mismatch: preserve it as an explicit Right-side expense.
            if (rawDifference > 0L) {
                right += BahiDayLine(
                    "day-close-diff:$dateEpochDay",
                    BahiEntryKind.DAY_CLOSE_DIFF,
                    "Day-close difference",
                    rawDifference,
                    BahiPaymentMode.CASH,
                )
            }
        }

        val leftTotal = activeTotal(left)
        val rightTotal = activeTotal(right)
        val causes = entries.asSequence()
            .filterNot { it.isVoid }
            .mapNotNull {
                when {
                    it.savedAfterClose -> BahiLikelyCause(it.id, "Entry saved after the day was closed")
                    it.isUnusualAmount -> BahiLikelyCause(it.id, "Amount is far from this party's usual")
                    !it.paymentMode.isCash && it.kind == BahiEntryKind.BUYER_BANK_RECEIPT -> BahiLikelyCause(it.id, "Online entry may be missing its other side")
                    else -> null
                }
            }.toList()

        return BahiDayPage(
            dateEpochDay = dateEpochDay,
            left = left,
            right = right,
            leftTotalPaisa = leftTotal,
            rightTotalPaisa = rightTotal,
            differencePaisa = leftTotal - rightTotal,
            likelyCauses = causes,
            changedAfterClosing = entries.any { it.savedAfterClose },
        )
    }

    private fun BahiDayEntry.toLine() = BahiDayLine(
        id = id,
        kind = kind,
        label = label,
        amountPaisa = amountPaisa,
        paymentMode = paymentMode,
        isVoid = isVoid,
        isStruckThrough = isVoid || isSameDayAdvance,
    )

    private fun BahiEntryKind.isLeftKind() = this in setOf(
        BahiEntryKind.FARMER_MONEY_LEFT,
        BahiEntryKind.BUYER_PAYMENT,
        BahiEntryKind.LOAN_PRINCIPAL_REPAID,
        BahiEntryKind.LOAN_INTEREST,
        BahiEntryKind.ADVANCE_CARRIED_IN,
    )

    private fun activeTotal(lines: List<BahiDayLine>) = lines.filterNot { it.isVoid }.sumOf { it.amountPaisa }
}
