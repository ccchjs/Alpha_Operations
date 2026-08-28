package com.airemore.fieldapp.ui.repair

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airemore.fieldapp.AiremoreApp
import com.airemore.fieldapp.data.local.RepairUnit
import com.airemore.fieldapp.data.local.entity.RepairFormEntity
import com.airemore.fieldapp.data.repository.LookupData
import com.airemore.fieldapp.ui.common.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

private const val TOTAL_STEPS = 6

@Composable
fun RepairFormScreen(app: AiremoreApp, localId: Long, onDone: () -> Unit) {
    val scope = rememberCoroutineScope()
    var form by remember { mutableStateOf<RepairFormEntity?>(null) }
    var lookups by remember { mutableStateOf(LookupData.EMPTY) }
    var step by remember { mutableIntStateOf(0) }
    var submitting by remember { mutableStateOf(false) }

    LaunchedEffect(localId) {
        lookups = app.lookupRepository.getCached()
        form = if (localId <= 0) {
            val fullName = app.session.currentFullName()
            val newId = app.repairRepository.createDraft(fullName)
            app.repairRepository.getById(newId)
        } else {
            app.repairRepository.getById(localId)
        }
    }

    val currentForm = form ?: run {
        Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) { CircularProgressIndicator() }
        return
    }

    fun update(transform: (RepairFormEntity) -> RepairFormEntity) {
        // Always read the latest state at the moment of the edit, not a
        // snapshot captured at the start of this recomposition. Two edits
        // landing in the same frame (e.g. typing Address then Date quickly)
        // would otherwise both transform from the same stale `currentForm`,
        // and whichever write happened last would silently wipe out the
        // other field's change.
        val latest = form ?: return
        val updated = transform(latest)
        form = updated
        scope.launch { app.repairRepository.saveDraft(updated) }
    }

    val stepTitle = listOf("Company Info", "Personnel", "AC Units", "Findings", "Customer & Pirma", "Review")[step]

    WizardScaffold(
        title = "Repair / Checkup",
        stepIndex = step,
        stepCount = TOTAL_STEPS,
        stepTitle = stepTitle,
        onBack = { step-- },
        onClose = onDone,
        onNext = if (step < TOTAL_STEPS - 1) { { step++ } } else null,
        onSubmit = if (step == TOTAL_STEPS - 1) {
            { submitting = true; scope.launch { app.repairRepository.submit(currentForm.localId); submitting = false; onDone() } }
        } else null,
        nextEnabled = !submitting && stepIsValid(step, currentForm),
    ) {
        when (step) {
            0 -> InfoStep(currentForm, lookups, onUpdate = ::update)
            1 -> PersonnelStep(currentForm, onUpdate = ::update)
            2 -> UnitsStep(currentForm, lookups, onUpdate = ::update)
            3 -> FindingsStep(currentForm, lookups, onUpdate = ::update)
            4 -> CustomerSignatureStep(currentForm, onUpdate = ::update)
            5 -> ReviewStep(currentForm)
        }
    }
}

private fun stepIsValid(step: Int, form: RepairFormEntity): Boolean = when (step) {
    0 -> form.companyName.isNotBlank() && form.formDate.isNotBlank()
    2 -> form.units.isNotEmpty()
    4 -> form.customerName.isNotBlank() && form.customerSignaturePath != null
    else -> true
}

@Composable
private fun InfoStep(form: RepairFormEntity, lookups: LookupData, onUpdate: ((RepairFormEntity) -> RepairFormEntity) -> Unit) {
    var showAddCompany by remember { mutableStateOf(false) }

    SectionLabel("Company")
    CompanyPickerRepair(lookups.companies, form.companyName,
        onSelect = { c -> onUpdate { it.copy(companyId = c.id, companyName = c.name, address = c.address ?: it.address) } },
        onAddNew = { showAddCompany = true })
    OutlinedTextField(form.address, { v -> onUpdate { it.copy(address = v) } }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
    DatePickerFieldR("Petsa ng Serbisyo", form.formDate) { v -> onUpdate { it.copy(formDate = v) } }
    OutlinedTextField(form.workOrderNo, { v -> onUpdate { it.copy(workOrderNo = v) } }, label = { Text("Work Order No. (optional)") }, modifier = Modifier.fillMaxWidth())

    if (showAddCompany) {
        AlertDialog(
            onDismissRequest = { showAddCompany = false },
            title = { Text("Bagong Company") },
            text = {
                var name by remember { mutableStateOf("") }
                var address by remember { mutableStateOf("") }
                Column {
                    OutlinedTextField(name, { name = it }, label = { Text("Pangalan") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(address, { address = it }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(4.dp))
                    Button(onClick = {
                        if (name.isNotBlank()) { onUpdate { it.copy(companyId = null, companyName = name, address = address) }; showAddCompany = false }
                    }, modifier = Modifier.fillMaxWidth()) { Text("Idagdag") }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showAddCompany = false }) { Text("Cancel") } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompanyPickerRepair(
    companies: List<com.airemore.fieldapp.data.local.Company>,
    selectedName: String,
    onSelect: (com.airemore.fieldapp.data.local.Company) -> Unit,
    onAddNew: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf(selectedName) }
    val filtered = remember(query, companies) { if (query.isBlank()) companies else companies.filter { it.name.contains(query, true) } }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = query, onValueChange = { query = it; expanded = true },
            label = { Text("Piliin o i-type ang company") },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            filtered.take(20).forEach { c -> DropdownMenuItem(text = { Text(c.name) }, onClick = { query = c.name; onSelect(c); expanded = false }) }
            DropdownMenuItem(text = { Text("+ Magdagdag ng Bagong Company") }, onClick = { expanded = false; onAddNew() })
        }
    }
}

@Composable
private fun DatePickerFieldR(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) }, placeholder = { Text("YYYY-MM-DD") },
        trailingIcon = { TextButton(onClick = { onChange(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())) }) { Text("Today") } },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun PersonnelStep(form: RepairFormEntity, onUpdate: ((RepairFormEntity) -> RepairFormEntity) -> Unit) {
    SectionLabel("Personnel")
    Text("Ikaw ang laging Personnel #1. Idagdag ang mga kasama mo (hanggang 9 pa).", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    form.personnel.forEachIndexed { index, name ->
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            OutlinedTextField(
                value = name,
                onValueChange = { v -> onUpdate { f -> f.copy(personnel = f.personnel.toMutableList().also { it[index] = v }) } },
                label = { Text("Personnel #${index + 2}") }, modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { onUpdate { f -> f.copy(personnel = f.personnel.toMutableList().also { it.removeAt(index) }) } }) {
                Icon(Icons.Filled.Delete, contentDescription = "Remove")
            }
        }
    }
    if (form.personnel.size < 9) {
        OutlinedButton(onClick = { onUpdate { f -> f.copy(personnel = f.personnel + "") } }) {
            Icon(Icons.Filled.Add, contentDescription = null); Spacer(Modifier.width(4.dp)); Text("Magdagdag ng Personnel")
        }
    }
}

@Composable
private fun UnitsStep(form: RepairFormEntity, lookups: LookupData, onUpdate: ((RepairFormEntity) -> RepairFormEntity) -> Unit) {
    SectionLabel("AC Units (${form.units.size})")
    form.units.forEachIndexed { index, unit ->
        RepairUnitCard(
            index = index, unit = unit, lookups = lookups,
            onChange = { updated -> onUpdate { f -> f.copy(units = f.units.toMutableList().also { it[index] = updated }) } },
            onRemove = { onUpdate { f -> f.copy(units = f.units.toMutableList().also { it.removeAt(index) }) } },
        )
    }
    OutlinedButton(onClick = { onUpdate { f -> f.copy(units = f.units + RepairUnit()) } }) {
        Icon(Icons.Filled.Add, contentDescription = null); Spacer(Modifier.width(4.dp)); Text("Magdagdag ng Unit")
    }
}

@Composable
private fun RepairUnitCard(index: Int, unit: RepairUnit, lookups: LookupData, onChange: (RepairUnit) -> Unit, onRemove: () -> Unit) {
    var expanded by remember { mutableStateOf(true) }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text("Unit ${index + 1}${if (unit.brand.isNotBlank()) " — ${unit.brand}" else ""}", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                IconButton(onClick = { expanded = !expanded }) { Icon(if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null) }
                IconButton(onClick = onRemove) { Icon(Icons.Filled.Delete, contentDescription = "Remove unit") }
            }
            if (expanded) {
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(unit.location, { onChange(unit.copy(location = it)) }, label = { Text("Location (e.g. 2nd Floor Office)") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                DropdownWithOthers("Brand", lookups.acBrands, unit.brand, { onChange(unit.copy(brand = it)) })
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(unit.serialNumber, { onChange(unit.copy(serialNumber = it)) }, label = { Text("Serial Number (optional)") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(unit.model, { onChange(unit.copy(model = it)) }, label = { Text("Model") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                DropdownWithOthers("Type", lookups.acTypes, unit.acType, { onChange(unit.copy(acType = it)) })
                Spacer(Modifier.height(8.dp))
                DropdownWithOthers("Capacity", lookups.acCapacities, unit.capacity, { onChange(unit.copy(capacity = it)) })

                Spacer(Modifier.height(12.dp))
                Text("Readings", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                RReadingRow("Voltage", unit.voltageBefore, unit.voltageAfter, { b -> onChange(unit.copy(voltageBefore = b)) }, { a -> onChange(unit.copy(voltageAfter = a)) })
                RReadingRow("Current", unit.currentBefore, unit.currentAfter, { b -> onChange(unit.copy(currentBefore = b)) }, { a -> onChange(unit.copy(currentAfter = a)) })
                RReadingRow("Suction Pressure", unit.suctionPressureBefore, unit.suctionPressureAfter, { b -> onChange(unit.copy(suctionPressureBefore = b)) }, { a -> onChange(unit.copy(suctionPressureAfter = a)) })
                RReadingRow("Discharge Pressure", unit.dischargePressureBefore, unit.dischargePressureAfter, { b -> onChange(unit.copy(dischargePressureBefore = b)) }, { a -> onChange(unit.copy(dischargePressureAfter = a)) })
                RReadingRow("Temp Supply", unit.tempSupplyBefore, unit.tempSupplyAfter, { b -> onChange(unit.copy(tempSupplyBefore = b)) }, { a -> onChange(unit.copy(tempSupplyAfter = a)) })
                RReadingRow("Temp Return", unit.tempReturnBefore, unit.tempReturnAfter, { b -> onChange(unit.copy(tempReturnBefore = b)) }, { a -> onChange(unit.copy(tempReturnAfter = a)) })

                Spacer(Modifier.height(12.dp))
                PhotoPickerRow("Larawan Before (max 2)", unit.photosBefore, 2) { onChange(unit.copy(photosBefore = it.toMutableList())) }
                Spacer(Modifier.height(8.dp))
                PhotoPickerRow("Larawan After (max 2)", unit.photosAfter, 2) { onChange(unit.copy(photosAfter = it.toMutableList())) }
            }
        }
    }
}

@Composable
private fun RReadingRow(label: String, before: String, after: String, onBefore: (String) -> Unit, onAfter: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, fontSize = 12.sp, modifier = Modifier.width(120.dp).align(androidx.compose.ui.Alignment.CenterVertically))
        OutlinedTextField(before, onBefore, label = { Text("Before") }, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f), singleLine = true)
        Spacer(Modifier.width(6.dp))
        OutlinedTextField(after, onAfter, label = { Text("After") }, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f), singleLine = true)
    }
}

@Composable
private fun FindingsStep(form: RepairFormEntity, lookups: LookupData, onUpdate: ((RepairFormEntity) -> RepairFormEntity) -> Unit) {
    SectionLabel("Findings")
    OutlinedTextField(form.findings, { v -> onUpdate { it.copy(findings = v) } }, label = { Text("Findings (free text, optional if may napili sa baba)") }, modifier = Modifier.fillMaxWidth(), minLines = 3)

    MultiSelectChips("A.F.I. (As Found Inspection)", lookups.findingOptions["afi"].orEmpty(), form.afi, form.afiOther,
        { v -> onUpdate { it.copy(afi = v) } }, { v -> onUpdate { it.copy(afiOther = v) } })
    MultiSelectChips("Recommendation", lookups.findingOptions["recommendation"].orEmpty(), form.recommendation, form.recommendationOther,
        { v -> onUpdate { it.copy(recommendation = v) } }, { v -> onUpdate { it.copy(recommendationOther = v) } })
    MultiSelectChips("Action Taken", lookups.findingOptions["action_taken"].orEmpty(), form.actionTaken, form.actionTakenOther,
        { v -> onUpdate { it.copy(actionTaken = v) } }, { v -> onUpdate { it.copy(actionTakenOther = v) } })
    MultiSelectChips("A.L.I. (As Left Inspection)", lookups.findingOptions["ali"].orEmpty(), form.ali, form.aliOther,
        { v -> onUpdate { it.copy(ali = v) } }, { v -> onUpdate { it.copy(aliOther = v) } })
}

@Composable
private fun CustomerSignatureStep(form: RepairFormEntity, onUpdate: ((RepairFormEntity) -> RepairFormEntity) -> Unit) {
    CoaSection(
        coaType = form.coaType,
        onCoaTypeChange = { v -> onUpdate { it.copy(coaType = v) } },
        coaDate = form.coaDate ?: "",
        onCoaDateChange = { v -> onUpdate { it.copy(coaDate = v.ifBlank { null }) } },
        coaGenericText = form.coaGenericText ?: "",
        onCoaGenericTextChange = { v -> onUpdate { it.copy(coaGenericText = v) } },
        dateField = { label, value, onChange -> DatePickerFieldR(label, value, onChange) },
    )
    Spacer(Modifier.height(16.dp))

    SectionLabel("Customer")
    OutlinedTextField(form.customerName, { v -> onUpdate { it.copy(customerName = v) } }, label = { Text("Pangalan ng Customer") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(form.customerPosition, { v -> onUpdate { it.copy(customerPosition = v) } }, label = { Text("Position (optional)") }, modifier = Modifier.fillMaxWidth())
    DatePickerFieldR("Petsa ng Pirma", form.customerSignatureDate ?: "") { v -> onUpdate { it.copy(customerSignatureDate = v.ifBlank { null }) } }

    SectionLabel("Pirma ng Customer")
    SignaturePad(
        existingPath = form.customerSignaturePath,
        fileNamePrefix = "repair_${form.localId}",
        onSaved = { path -> onUpdate { it.copy(customerSignaturePath = path) } },
        onCleared = { onUpdate { it.copy(customerSignaturePath = null) } },
    )
}

@Composable
private fun ReviewStep(form: RepairFormEntity) {
    SectionLabel("Review bago i-submit")
    Column {
        listOf(
            "Company" to form.companyName,
            "Address" to form.address,
            "Date" to form.formDate,
            "Personnel" to (listOf("(ikaw)") + form.personnel).joinToString(", "),
            "AC Units" to "${form.units.size} unit(s)",
            "Customer" to form.customerName,
            "Pirma" to if (form.customerSignaturePath != null) "✓ Naka-save" else "✗ Wala pa",
        ).forEach { (label, value) ->
            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(100.dp))
                Text(value.ifBlank { "—" }, fontSize = 13.sp, modifier = Modifier.weight(1f))
            }
        }
    }
    Spacer(Modifier.height(8.dp))
    Text(
        "Pag-tap ng I-submit, ise-save ito sa device at awtomatikong mag-a-upload pag may internet signal.",
        fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
