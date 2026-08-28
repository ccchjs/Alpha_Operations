package com.airemore.fieldapp.ui.common

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Dropdown built from a cached option list, with an "Others…" entry that
 * reveals a free-text field. This mirrors the web app's brand/type/
 * capacity selects. Works fully offline since [options] comes from the
 * locally cached LookupData.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownWithOthers(
    label: String,
    options: List<String>,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val isOther = value.isNotBlank() && value !in options
    var otherText by remember(value) { mutableStateOf(if (isOther) value else "") }

    Column(modifier) {
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = if (isOther) "Others" else value,
                onValueChange = {},
                readOnly = true,
                label = { Text(label) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(text = { Text(option) }, onClick = {
                        onValueChange(option)
                        expanded = false
                    })
                }
                DropdownMenuItem(text = { Text("Others…") }, onClick = {
                    onValueChange(otherText) // may be blank; user types next
                    expanded = false
                })
            }
        }
        if (isOther) {
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = otherText,
                onValueChange = { otherText = it; onValueChange(it) },
                label = { Text("$label (specify)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Multi-select chip row + a free-text "Others" field, used for A.F.I. / Recommendation / Action Taken / A.L.I. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiSelectChips(
    label: String,
    options: List<String>,
    selected: List<String>,
    otherText: String,
    onSelectedChange: (List<String>) -> Unit,
    onOtherChange: (String) -> Unit,
) {
    Column {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(6.dp))
        if (options.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(options) { option ->
                    FilterChip(
                        selected = option in selected,
                        onClick = {
                            onSelectedChange(if (option in selected) selected - option else selected + option)
                        },
                        label = { Text(option, maxLines = 1) },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        OutlinedTextField(
            value = otherText,
            onValueChange = onOtherChange,
            label = { Text("Iba pa (optional)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 1,
            maxLines = 3,
        )
    }
}
