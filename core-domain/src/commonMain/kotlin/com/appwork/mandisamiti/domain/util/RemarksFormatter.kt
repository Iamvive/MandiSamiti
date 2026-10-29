package com.appwork.mandisamiti.domain.util

object RemarksFormatter {
    private val TAG_REGEX = Regex("""\[(LEND|SETTLE|EXPENSE):[^\]]+\]""")

    /**
     * Cleans internal metadata tags (such as [LEND:id], [SETTLE:id:lendId], [EXPENSE:id])
     * and removes any trailing colon when notes are empty.
     */
    fun cleanForDisplay(remarks: String?): String? {
        if (remarks.isNullOrBlank()) return null
        var cleaned = remarks.replace(TAG_REGEX, "").trim()
        if (cleaned.endsWith(":")) {
            cleaned = cleaned.dropLast(1).trim()
        }
        return cleaned.ifBlank { null }
    }
}
