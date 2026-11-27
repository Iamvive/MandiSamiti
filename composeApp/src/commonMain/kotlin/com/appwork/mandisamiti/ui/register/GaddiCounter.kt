package com.appwork.mandisamiti.ui.register

internal enum class GaddiField { BUNDLES, LOOSE }

internal data class GaddiCount(val bundles: Int = 0, val loose: Int = 0) {
    val pieces: Int get() = bundles * 100 + loose
}

internal data class GaddiDraft(
    val counts: Map<Int, GaddiCount> = emptyMap(), // a key means the denomination was counted
    val selected: Int = 500,
    val field: GaddiField = GaddiField.LOOSE,
    val replaceOnType: Boolean = true,
) {
    /** null means nothing has been counted. */
    val totalRupees: Long?
        get() = if (counts.isEmpty()) null
        else denominationTotal(counts.mapValues { it.value.pieces.toString() })

    fun select(denomination: Int): GaddiDraft {
        if (denomination !in drawerDenominations) return this
        return copy(
            selected = denomination,
            field = GaddiField.LOOSE,
            replaceOnType = true,
        )
    }

    fun focus(newField: GaddiField): GaddiDraft {
        return copy(field = newField, replaceOnType = true)
    }

    fun digit(n: Int): GaddiDraft {
        if (n !in 0..9) return this
        val currentCount = counts[selected] ?: GaddiCount(0, 0)

        val newCount = when (field) {
            GaddiField.BUNDLES -> {
                val newVal = if (replaceOnType) {
                    n
                } else {
                    val combined = currentCount.bundles.toString() + n.toString()
                    val parsed = combined.toIntOrNull() ?: return this
                    if (parsed > 999) return this
                    parsed
                }
                if (newVal > 999) return this
                currentCount.copy(bundles = newVal)
            }
            GaddiField.LOOSE -> {
                val newVal = if (replaceOnType) {
                    n
                } else {
                    val combined = currentCount.loose.toString() + n.toString()
                    val parsed = combined.toIntOrNull() ?: return this
                    if (parsed > 9999) return this
                    parsed
                }
                if (newVal > 9999) return this
                currentCount.copy(loose = newVal)
            }
        }

        return copy(
            counts = counts + (selected to newCount),
            replaceOnType = false,
        )
    }

    fun backspace(): GaddiDraft {
        if (selected !in counts) return this
        val currentCount = counts[selected] ?: GaddiCount(0, 0)

        val newCount = when (field) {
            GaddiField.BUNDLES -> {
                val str = currentCount.bundles.toString()
                val newVal = if (str.length <= 1) 0 else str.dropLast(1).toIntOrNull() ?: 0
                currentCount.copy(bundles = newVal)
            }
            GaddiField.LOOSE -> {
                val str = currentCount.loose.toString()
                val newVal = if (str.length <= 1) 0 else str.dropLast(1).toIntOrNull() ?: 0
                currentCount.copy(loose = newVal)
            }
        }

        return copy(
            counts = counts + (selected to newCount),
            replaceOnType = false,
        )
    }

    fun addOne(): GaddiDraft {
        val currentCount = counts[selected] ?: GaddiCount(0, 0)

        val newCount = when (field) {
            GaddiField.BUNDLES -> {
                val nextVal = (currentCount.bundles + 1).coerceAtMost(999)
                currentCount.copy(bundles = nextVal)
            }
            GaddiField.LOOSE -> {
                val nextVal = (currentCount.loose + 1).coerceAtMost(9999)
                currentCount.copy(loose = nextVal)
            }
        }

        return copy(
            counts = counts + (selected to newCount),
            replaceOnType = false,
        )
    }

    fun zero(): GaddiDraft {
        return copy(
            counts = counts + (selected to GaddiCount(0, 0)),
            replaceOnType = true,
        )
    }

    fun next(): GaddiDraft {
        val idx = drawerDenominations.indexOf(selected)
        if (idx == -1 || idx >= drawerDenominations.size - 1) return this
        return select(drawerDenominations[idx + 1])
    }

    fun previous(): GaddiDraft {
        val idx = drawerDenominations.indexOf(selected)
        if (idx <= 0) return this
        return select(drawerDenominations[idx - 1])
    }
}
