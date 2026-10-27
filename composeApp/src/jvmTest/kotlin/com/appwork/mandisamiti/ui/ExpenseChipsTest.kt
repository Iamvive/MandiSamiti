package com.appwork.mandisamiti.ui

import com.appwork.mandisamiti.domain.model.ExpenseCategory
import com.appwork.mandisamiti.ui.expense.labelHi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExpenseChipsTest {
    @Test
    fun everyCategoryHasAHindiLabel() {
        assertEquals("हम्माली", ExpenseCategory.HAMMALI.labelHi())
        assertEquals("किराया", ExpenseCategory.KIRAYA.labelHi())
        assertEquals("बिजली", ExpenseCategory.BIJLI.labelHi())
        assertEquals("चाय-पानी", ExpenseCategory.CHAI_PANI.labelHi())
        assertEquals("मजदूरी", ExpenseCategory.MAJDOORI.labelHi())
        ExpenseCategory.entries.forEach { assertTrue(it.labelHi().isNotBlank()) }
    }
}
