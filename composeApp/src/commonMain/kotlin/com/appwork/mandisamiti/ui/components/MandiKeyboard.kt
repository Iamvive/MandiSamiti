package com.appwork.mandisamiti.ui.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType

object MandiKeyboard {
    val Name = KeyboardOptions(
        capitalization = KeyboardCapitalization.Words,
        autoCorrect = false,
        keyboardType = KeyboardType.Text,
        imeAction = ImeAction.Next
    )
    val Note = KeyboardOptions(
        capitalization = KeyboardCapitalization.Sentences,
        autoCorrect = true,
        keyboardType = KeyboardType.Text,
        imeAction = ImeAction.Default
    )
    val Search = KeyboardOptions(
        capitalization = KeyboardCapitalization.None,
        autoCorrect = false,
        keyboardType = KeyboardType.Text,
        imeAction = ImeAction.Search
    )
    val Phone = KeyboardOptions(
        keyboardType = KeyboardType.Phone,
        imeAction = ImeAction.Next
    )
    val Pin = KeyboardOptions(
        keyboardType = KeyboardType.NumberPassword,
        imeAction = ImeAction.Done
    )
    val Identifier = KeyboardOptions(
        capitalization = KeyboardCapitalization.Characters,
        autoCorrect = false,
        keyboardType = KeyboardType.Ascii,
        imeAction = ImeAction.Next
    )
    val Whole = KeyboardOptions(
        keyboardType = KeyboardType.Number,
        imeAction = ImeAction.Next
    )
    val Decimal = KeyboardOptions(
        keyboardType = KeyboardType.Decimal,
        imeAction = ImeAction.Next
    )
}
