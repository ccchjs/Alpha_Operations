package com.airemore.fieldapp.ui.common

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Certification of Accomplishment — optional on all three form types.
 * Mirrors the web forms' COA radio group + conditional fields:
 *  - PM (SM Store companies only) has three types: none / generic / coa (standard, with Month of completion).
 *  - Repair and Installation only offer none / generic.
 *
 * [dateField] lets each screen plug in its own existing date-picker composable
 * (DatePickerField / DatePickerFieldR / DatePickerFieldI) so this stays purely
 * a layout/state component with no duplicated picker logic.
 */
@Composable
fun CoaSection(
    coaType: String,
    onCoaTypeChange: (String) -> Unit,
    coaDate: String,
    onCoaDateChange: (String) -> Unit,
    coaGenericText: String,
    onCoaGenericTextChange: (String) -> Unit,
    dateField: @Composable (label: String, value: String, onChange: (String) -> Unit) -> Unit,
    allowStandardCoa: Boolean = false,
    coaMonthYear: String = "",
    onCoaMonthYearChange: (String) -> Unit = {},
) {
    SectionLabel("Certification of Accomplishment (COA)")
    Text(
        "Optional. Company name / address at Accepted By (pirma ng customer) ay awtomatiko na mula sa ibang bahagi ng form na ito.",
        style = MaterialTheme.typography.bodySmall,
    )
    Spacer(Modifier.height(6.dp))

    Column {
        CoaTypeRadioRow("Wala", coaType == "none" || coaType.isBlank()) { onCoaTypeChange("none") }
        CoaTypeRadioRow("Generic COA", coaType == "generic") { onCoaTypeChange("generic") }
        if (allowStandardCoa) {
            CoaTypeRadioRow("COA (standard)", coaType == "coa") { onCoaTypeChange("coa") }
        }
    }

    if (coaType == "generic" || coaType == "coa") {
        Spacer(Modifier.height(8.dp))
        dateField("COA Date", coaDate, onCoaDateChange)

        if (allowStandardCoa && coaType == "coa") {
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = coaMonthYear,
                onValueChange = onCoaMonthYearChange,
                label = { Text("Month of completion (e.g. APRIL 2026)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Text(
                "Print uses fixed text: “Periodic Maintenance of All Air-Conditioning Equipment” for the selected month.",
                style = MaterialTheme.typography.bodySmall,
            )
        }

        if (coaType == "generic") {
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = coaGenericText,
                onValueChange = onCoaGenericTextChange,
                label = { Text("Certification text (fills: “This is to certify that the ___ has been completed.”)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
            )
        }
    }
}

@Composable
private fun CoaTypeRadioRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
    }
}
