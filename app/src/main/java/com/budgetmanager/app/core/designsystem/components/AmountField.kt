package com.budgetmanager.app.core.designsystem.components

import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions

/** A rupee amount field that only accepts digits and a single decimal point - the same shape
 *  Money.parseRupeeInput expects. Formatting and grouping happen only on display, never here. */
@Composable
fun AmountField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    supportingText: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = { new ->
            if (new.isEmpty() || new.matches(Regex("""\d*\.?\d{0,2}"""))) onValueChange(new)
        },
        modifier = modifier,
        label = { Text("Amount") },
        leadingIcon = { Text("₹") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        isError = isError,
        supportingText = supportingText?.let { { Text(it) } },
        singleLine = true
    )
}
