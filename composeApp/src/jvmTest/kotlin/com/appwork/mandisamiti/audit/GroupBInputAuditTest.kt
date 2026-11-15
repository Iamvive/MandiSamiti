package com.appwork.mandisamiti.audit

import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.appwork.mandisamiti.ui.components.MandiKeyboard
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GroupBInputAuditTest {

    @Test
    fun testMandiKeyboardPresets() {
        // Decimal Preset (for Money & Weight inputs in Settings, Daily Cash, Lend, Expense, Stock)
        assertEquals(KeyboardType.Decimal, MandiKeyboard.Decimal.keyboardType)
        assertEquals(ImeAction.Next, MandiKeyboard.Decimal.imeAction)

        // Whole Preset (for Bags, Denominations, Counts)
        assertEquals(KeyboardType.Number, MandiKeyboard.Whole.keyboardType)
        assertEquals(ImeAction.Next, MandiKeyboard.Whole.imeAction)

        // Name Preset (for Persons, Farmers, Traders, Crops)
        assertEquals(KeyboardType.Text, MandiKeyboard.Name.keyboardType)
        assertEquals(KeyboardCapitalization.Words, MandiKeyboard.Name.capitalization)
        assertFalse(MandiKeyboard.Name.autoCorrect)

        // Identifier Preset (for GSTIN, Licence No)
        assertEquals(KeyboardType.Ascii, MandiKeyboard.Identifier.keyboardType)
        assertEquals(KeyboardCapitalization.Characters, MandiKeyboard.Identifier.capitalization)
        assertFalse(MandiKeyboard.Identifier.autoCorrect)

        // Search Preset (for Farmer, Godown, Ledger search)
        assertEquals(KeyboardType.Text, MandiKeyboard.Search.keyboardType)
        assertEquals(ImeAction.Search, MandiKeyboard.Search.imeAction)
        assertFalse(MandiKeyboard.Search.autoCorrect)

        // Note Preset (for Reasons, Remarks, Notes)
        assertEquals(KeyboardType.Text, MandiKeyboard.Note.keyboardType)
        assertEquals(KeyboardCapitalization.Sentences, MandiKeyboard.Note.capitalization)
        assertTrue(MandiKeyboard.Note.autoCorrect)
    }

    @Test
    fun testAuditPreservesIntegerAndDecimalSeparation() {
        // Ensure Whole never defaults to Text or decimal
        assertEquals(KeyboardType.Number, MandiKeyboard.Whole.keyboardType)
        // Ensure Decimal is explicitly Decimal type
        assertEquals(KeyboardType.Decimal, MandiKeyboard.Decimal.keyboardType)
    }
}
