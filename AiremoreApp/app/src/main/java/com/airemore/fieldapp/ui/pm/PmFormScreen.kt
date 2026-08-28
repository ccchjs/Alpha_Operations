package com.airemore.fieldapp.ui.pm

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.airemore.fieldapp.data.local.ChecklistEntry
import com.airemore.fieldapp.data.local.PmUnit
import com.airemore.fieldapp.data.local.entity.PmFormEntity
import com.airemore.fieldapp.data.repository.LookupData
import com.airemore.fieldapp.ui.common.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

private val TOTAL_STEPS = 6

@Composable
fun PmFormScreen(app: AiremoreApp, localId: Long, onDone: () -> Unit) {
    val scope = rememberCoroutineScope()
    var form by remember { mutableStateOf<PmFormEntity?>(null) }
    var lookups by remember { mutableStateOf(LookupData.EMPTY) }
    var step by remember { mutableIntStateOf(0) }
    var submitting by remember { mutableStateOf(false) }

    LaunchedEffect(localId) {
        lookups = app.lookupRepository.getCached()
        form = if (localId <= 0) {
            val fullName = app.session.currentFullName()
            val newId = app.pmRepository.createDraft(fullName)
            app.pmRepository.getById(newId)
        } else {
            app.pmRepository.getById(localId)
        }
    }

    val currentForm = form ?: run {
        Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) { CircularProgressIndicator() }
        return
    }

    fun update(transform: (PmFormEntity) -> PmFormEntity) {
        // Always read the latest state at the moment of the edit, not a
        // snapshot captured at the start of this recomposition. Two edits
        // landing in the same frame (e.g. typing Address then Date quickly)
        // would otherwise both transform from the same stale `currentForm`,
        // and whichever write happened last would silently wipe out the
        // other field's change.
        val latest = form ?: return
        val updated = transform(latest)
        form = updated
        scope.launch { app.pmRepository.saveDraft(updated) }
    }

    val stepTitle = if (currentForm.isSmStore)
        listOf("Company Info", "Personnel", "Checklist & Particulars", "PM Statement", "Customer & Pirma", "Review")[step]
    else
        listOf("Company Info", "Personnel", "AC Units", "Findings", "Customer & Pirma", "Review")[step]

    WizardScaffold(
        title = "PM Form",
        stepIndex = step,
        stepCount = TOTAL_STEPS,
        stepTitle = stepTitle,
        onBack = { step-- },
        onClose = onDone,
        onNext = if (step < TOTAL_STEPS - 1) { { step++ } } else null,
        onSubmit = if (step == TOTAL_STEPS - 1) {
            {
                submitting = true
                scope.launch {
                    app.pmRepository.submit(currentForm.localId)
                    submitting = false
                    onDone()
                }
            }
        } else null,
        nextEnabled = !submitting && stepIsValid(step, currentForm),
    ) {
        when (step) {
            0 -> InfoStep(currentForm, lookups, app, onUpdate = ::update)
            1 -> PersonnelStep(currentForm, onUpdate = ::update)
            2 -> if (currentForm.isSmStore) SmStoreChecklistParticularsStep(currentForm, lookups, onUpdate = ::update)
                 else UnitsStep(currentForm, lookups, onUpdate = ::update)
            3 -> if (currentForm.isSmStore) PmStatementStep(currentForm, onUpdate = ::update)
                 else FindingsStep(currentForm, lookups, onUpdate = ::update)
            4 -> CustomerSignatureStep(currentForm, onUpdate = ::update)
            5 -> ReviewStep(currentForm)
        }
    }
}

private fun stepIsValid(step: Int, form: PmFormEntity): Boolean = when (step) {
    0 -> form.companyName.isNotBlank() && form.formDate.isNotBlank()
    2 -> form.isSmStore || form.units.isNotEmpty()
    4 -> form.customerName.isNotBlank() && form.customerSignaturePath != null
    else -> true
}

@Composable
private fun InfoStep(form: PmFormEntity, lookups: LookupData, app: AiremoreApp, onUpdate: ((PmFormEntity) -> PmFormEntity) -> Unit) {
    var showAddCompany by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    SectionLabel("Company")
    CompanyPicker(
        companies = lookups.companies,
        selectedName = form.companyName,
        onSelect = { c ->
            onUpdate { it.copy(companyId = c.id, companyName = c.name, address = c.address ?: it.address, isSmStore = c.isSmStore) }
            if (c.isSmStore) {
                // SM Store companies have their Particulars pre-assigned by
                // the admin (company_particulars table) — fetch them so the
                // technician only has to fill in Temperature/Status. Falls
                // back to whatever was already saved if offline.
                scope.launch {
                    app.lookupRepository.fetchCompanyParticulars(c.id)?.let { particulars ->
                        onUpdate { it.copy(particulars = particulars) }
                    }
                }
            }
        },
        onAddNew = { showAddCompany = true },
    )
    OutlinedTextField(
        value = form.address,
        onValueChange = { v -> onUpdate { it.copy(address = v) } },
        label = { Text("Address") },
        modifier = Modifier.fillMaxWidth(),
        minLines = 2,
    )
    DatePickerField("Petsa ng Serbisyo", form.formDate) { v -> onUpdate { it.copy(formDate = v) } }
    OutlinedTextField(
        value = form.workOrderNo,
        onValueChange = { v -> onUpdate { it.copy(workOrderNo = v) } },
        label = { Text("Work Order No. (optional)") },
        modifier = Modifier.fillMaxWidth(),
    )

    if (showAddCompany) {
        AddCompanyDialog(
            onDismiss = { showAddCompany = false },
            onConfirm = { name, address ->
                showAddCompany = false
                // Optimistically use the typed name/address offline; the
                // server resolves/creates it by name on sync (see
                // api/pm_save.php's company auto-create fallback).
                onUpdate { it.copy(companyId = null, companyName = name, address = address) }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompanyPicker(
    companies: List<com.airemore.fieldapp.data.local.Company>,
    selectedName: String,
    onSelect: (com.airemore.fieldapp.data.local.Company) -> Unit,
    onAddNew: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf(selectedName) }
    val filtered = remember(query, companies) {
        if (query.isBlank()) companies else companies.filter { it.name.contains(query, ignoreCase = true) }
    }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it; expanded = true },
            label = { Text("Piliin o i-type ang company") },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            filtered.take(20).forEach { c ->
                DropdownMenuItem(text = { Text(c.name) }, onClick = {
                    query = c.name; onSelect(c); expanded = false
                })
            }
            DropdownMenuItem(text = { Text("+ Magdagdag ng Bagong Company") }, onClick = { expanded = false; onAddNew() })
        }
    }
}

@Composable
private fun AddCompanyDialog(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Bagong Company") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Pangalan") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { TextButton(onClick = { if (name.isNotBlank()) onConfirm(name, address) }) { Text("Idagdag") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun DatePickerField(label: String, value: String, onChange: (String) -> Unit) {
    // Simple text-based date entry (yyyy-MM-dd) with a "Today" shortcut —
    // avoids pulling in the full Material DatePickerDialog just for this;
    // swap for androidx.compose.material3.DatePicker if you want a native
    // calendar UI later.
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        placeholder = { Text("YYYY-MM-DD") },
        trailingIcon = {
            TextButton(onClick = { onChange(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())) }) { Text("Today") }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun PersonnelStep(form: PmFormEntity, onUpdate: ((PmFormEntity) -> PmFormEntity) -> Unit) {
    SectionLabel("Personnel")
    Text("Ikaw ang laging Personnel #1. Idagdag ang mga kasama mo (hanggang 9 pa).", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

    form.personnel.forEachIndexed { index, name ->
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            OutlinedTextField(
                value = name,
                onValueChange = { v -> onUpdate { f -> f.copy(personnel = f.personnel.toMutableList().also { it[index] = v }) } },
                label = { Text("Personnel #${index + 2}") },
                modifier = Modifier.weight(1f),
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
private fun UnitsStep(form: PmFormEntity, lookups: LookupData, onUpdate: ((PmFormEntity) -> PmFormEntity) -> Unit) {
    SectionLabel("AC Units (${form.units.size})")
    form.units.forEachIndexed { index, unit ->
        UnitCard(
            index = index,
            unit = unit,
            lookups = lookups,
            checklistTemplate = lookups.pmChecklistItems,
            onChange = { updated ->
                onUpdate { f -> f.copy(units = f.units.toMutableList().also { it[index] = updated }) }
            },
            onRemove = {
                onUpdate { f -> f.copy(units = f.units.toMutableList().also { it.removeAt(index) }) }
            },
        )
    }
    OutlinedButton(onClick = {
        val fresh = PmUnit(checklist = lookups.pmChecklistItems.map { ChecklistEntry(it) }.toMutableList())
        onUpdate { f -> f.copy(units = f.units + fresh) }
    }) {
        Icon(Icons.Filled.Add, contentDescription = null); Spacer(Modifier.width(4.dp)); Text("Magdagdag ng Unit")
    }
}

@Composable
private fun SmStoreChecklistParticularsStep(form: PmFormEntity, lookups: LookupData, onUpdate: ((PmFormEntity) -> PmFormEntity) -> Unit) {
    SectionLabel("PM Checklist")
    if (lookups.smStoreChecklistGroups.isEmpty()) {
        Text(
            "Wala pang na-download na checklist. Kumonekta sa internet at bumalik sa Dashboard para mag-sync.",
            fontSize = 12.sp, color = MaterialTheme.colorScheme.error,
        )
    }
    lookups.smStoreChecklistGroups.forEach { group ->
        Text("${group.letter}. ${group.title}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
        group.items.forEach { item ->
            val checked = item.key in form.smChecklist
            Row(
                Modifier.fillMaxWidth().padding(vertical = 2.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = checked,
                    onCheckedChange = { isChecked ->
                        onUpdate { f -> f.copy(smChecklist = if (isChecked) f.smChecklist + item.key else f.smChecklist - item.key) }
                    },
                )
                Text(item.text, fontSize = 13.sp, modifier = Modifier.weight(1f))
            }
        }
    }

    Spacer(Modifier.height(16.dp))
    SectionLabel("Particulars (${form.particulars.size})")
    Text(
        "Ito ang mga particulars na naka-assign sa company na ito. Punan lang ang Temperature at Status.",
        fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (form.particulars.isEmpty()) {
        Text(
            "Wala pang particulars na naka-assign sa company na ito, o kailangan pa ng internet para ma-download. Puwede ring bumalik sa Company Info at piliin ulit ang company.",
            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp),
        )
    }
    form.particulars.forEachIndexed { index, p ->
        Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Column(Modifier.padding(12.dp)) {
                Text(p.item, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(Modifier.height(6.dp))
                Row {
                    OutlinedTextField(
                        value = p.tempBefore,
                        onValueChange = { v -> onUpdate { f -> f.copy(particulars = f.particulars.toMutableList().also { it[index] = p.copy(tempBefore = v) }) } },
                        label = { Text("Temp. (Before)") }, modifier = Modifier.weight(1f), singleLine = true,
                    )
                    Spacer(Modifier.width(6.dp))
                    OutlinedTextField(
                        value = p.tempAfter,
                        onValueChange = { v -> onUpdate { f -> f.copy(particulars = f.particulars.toMutableList().also { it[index] = p.copy(tempAfter = v) }) } },
                        label = { Text("Temp. (After)") }, modifier = Modifier.weight(1f), singleLine = true,
                    )
                }
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = p.status,
                    onValueChange = { v -> onUpdate { f -> f.copy(particulars = f.particulars.toMutableList().also { it[index] = p.copy(status = v) }) } },
                    label = { Text("Status") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                )
            }
        }
    }
}

/** SM Store variant's PM Statement — replaces Findings/AFI/Recommendation/Action Taken/ALI. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PmStatementStep(form: PmFormEntity, onUpdate: ((PmFormEntity) -> PmFormEntity) -> Unit) {
    val months = listOf("JANUARY","FEBRUARY","MARCH","APRIL","MAY","JUNE","JULY","AUGUST","SEPTEMBER","OCTOBER","NOVEMBER","DECEMBER")
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val years = (currentYear - 1..currentYear + 2).map { it.toString() }
    var monthExpanded by remember { mutableStateOf(false) }
    var yearExpanded by remember { mutableStateOf(false) }
    var selectedMonth by remember { mutableStateOf(months[Calendar.getInstance().get(Calendar.MONTH)]) }
    var selectedYear by remember { mutableStateOf(currentYear.toString()) }

    SectionLabel("PM Statement")
    Row(Modifier.fillMaxWidth()) {
        ExposedDropdownMenuBox(expanded = monthExpanded, onExpandedChange = { monthExpanded = it }, modifier = Modifier.weight(1f)) {
            OutlinedTextField(
                value = selectedMonth, onValueChange = {}, readOnly = true, label = { Text("Month") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(monthExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
            )
            ExposedDropdownMenu(expanded = monthExpanded, onDismissRequest = { monthExpanded = false }) {
                months.forEach { m ->
                    DropdownMenuItem(text = { Text(m) }, onClick = {
                        selectedMonth = m; monthExpanded = false
                        onUpdate { it.copy(pmStatement = "PERFORM PERIODIC MAINTENANCE OF ALL AIR CONDITIONING UNIT FOR THE MONTH OF $m $selectedYear") }
                    })
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        ExposedDropdownMenuBox(expanded = yearExpanded, onExpandedChange = { yearExpanded = it }, modifier = Modifier.weight(1f)) {
            OutlinedTextField(
                value = selectedYear, onValueChange = {}, readOnly = true, label = { Text("Year") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(yearExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
            )
            ExposedDropdownMenu(expanded = yearExpanded, onDismissRequest = { yearExpanded = false }) {
                years.forEach { y ->
                    DropdownMenuItem(text = { Text(y) }, onClick = {
                        selectedYear = y; yearExpanded = false
                        onUpdate { it.copy(pmStatement = "PERFORM PERIODIC MAINTENANCE OF ALL AIR CONDITIONING UNIT FOR THE MONTH OF $selectedMonth $y") }
                    })
                }
            }
        }
    }
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value = form.pmStatement.ifBlank { "PERFORM PERIODIC MAINTENANCE OF ALL AIR CONDITIONING UNIT FOR THE MONTH OF $selectedMonth $selectedYear" },
        onValueChange = { v -> onUpdate { it.copy(pmStatement = v) } },
        label = { Text("Statement") }, modifier = Modifier.fillMaxWidth(), minLines = 2,
    )
}

@Composable
private fun UnitCard(
    index: Int,
    unit: PmUnit,
    lookups: LookupData,
    checklistTemplate: List<String>,
    onChange: (PmUnit) -> Unit,
    onRemove: () -> Unit,
) {
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
                DropdownWithOthers("Brand", lookups.acBrands, unit.brand, { onChange(unit.copy(brand = it)) })
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(unit.model, { onChange(unit.copy(model = it)) }, label = { Text("Model") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                DropdownWithOthers("Type", lookups.acTypes, unit.acType, { onChange(unit.copy(acType = it)) })
                Spacer(Modifier.height(8.dp))
                DropdownWithOthers("Capacity", lookups.acCapacities, unit.capacity, { onChange(unit.copy(capacity = it)) })

                Spacer(Modifier.height(12.dp))
                Text("Readings", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                ReadingRow("Voltage", unit.voltageBefore, unit.voltageAfter, { b -> onChange(unit.copy(voltageBefore = b)) }, { a -> onChange(unit.copy(voltageAfter = a)) })
                ReadingRow("Current", unit.currentBefore, unit.currentAfter, { b -> onChange(unit.copy(currentBefore = b)) }, { a -> onChange(unit.copy(currentAfter = a)) })
                ReadingRow("Suction Pressure", unit.suctionPressureBefore, unit.suctionPressureAfter, { b -> onChange(unit.copy(suctionPressureBefore = b)) }, { a -> onChange(unit.copy(suctionPressureAfter = a)) })
                ReadingRow("Discharge Pressure", unit.dischargePressureBefore, unit.dischargePressureAfter, { b -> onChange(unit.copy(dischargePressureBefore = b)) }, { a -> onChange(unit.copy(dischargePressureAfter = a)) })
                ReadingRow("Temp Supply", unit.tempSupplyBefore, unit.tempSupplyAfter, { b -> onChange(unit.copy(tempSupplyBefore = b)) }, { a -> onChange(unit.copy(tempSupplyAfter = a)) })
                ReadingRow("Temp Return", unit.tempReturnBefore, unit.tempReturnAfter, { b -> onChange(unit.copy(tempReturnBefore = b)) }, { a -> onChange(unit.copy(tempReturnAfter = a)) })

                Spacer(Modifier.height(12.dp))
                Text("Checklist", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                val checklist = if (unit.checklist.isEmpty() && checklistTemplate.isNotEmpty())
                    checklistTemplate.map { ChecklistEntry(it) }.toMutableList() else unit.checklist
                checklist.forEachIndexed { ci, item ->
                    ChecklistRow(item) { updatedStatus ->
                        val newList = checklist.toMutableList()
                        newList[ci] = item.copy(status = updatedStatus)
                        onChange(unit.copy(checklist = newList))
                    }
                }

                Spacer(Modifier.height(12.dp))
                PhotoPickerRow("Larawan Before (max 2)", unit.photosBefore, 2) { onChange(unit.copy(photosBefore = it.toMutableList())) }
                Spacer(Modifier.height(8.dp))
                PhotoPickerRow("Larawan After (max 2)", unit.photosAfter, 2) { onChange(unit.copy(photosAfter = it.toMutableList())) }
            }
        }
    }
}

@Composable
private fun ReadingRow(label: String, before: String, after: String, onBefore: (String) -> Unit, onAfter: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, fontSize = 12.sp, modifier = Modifier.width(120.dp).align(androidx.compose.ui.Alignment.CenterVertically))
        OutlinedTextField(
            value = before, onValueChange = onBefore, label = { Text("Before") },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f), singleLine = true,
        )
        Spacer(Modifier.width(6.dp))
        OutlinedTextField(
            value = after, onValueChange = onAfter, label = { Text("After") },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f), singleLine = true,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChecklistRow(item: ChecklistEntry, onStatusChange: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Text(item.itemName, fontSize = 12.sp, modifier = Modifier.weight(1f))
        listOf("check" to "✓", "x" to "✗").forEach { (statusValue, symbol) ->
            FilterChip(
                selected = item.status == statusValue,
                onClick = { onStatusChange(if (item.status == statusValue) "" else statusValue) },
                label = { Text(symbol) },
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    }
}

@Composable
private fun FindingsStep(form: PmFormEntity, lookups: LookupData, onUpdate: ((PmFormEntity) -> PmFormEntity) -> Unit) {
    SectionLabel("Findings")
    OutlinedTextField(form.findings, { v -> onUpdate { it.copy(findings = v) } }, label = { Text("Findings (free text)") }, modifier = Modifier.fillMaxWidth(), minLines = 3)

    val afiOptions = lookups.findingOptions["afi"].orEmpty()
    MultiSelectChips("A.F.I. (As Found Inspection)", afiOptions, form.afi, form.afiOther,
        onSelectedChange = { v -> onUpdate { it.copy(afi = v) } },
        onOtherChange = { v -> onUpdate { it.copy(afiOther = v) } })

    val recOptions = lookups.findingOptions["recommendation"].orEmpty()
    MultiSelectChips("Recommendation", recOptions, form.recommendation, form.recommendationOther,
        onSelectedChange = { v -> onUpdate { it.copy(recommendation = v) } },
        onOtherChange = { v -> onUpdate { it.copy(recommendationOther = v) } })

    val actOptions = lookups.findingOptions["action_taken"].orEmpty()
    MultiSelectChips("Action Taken", actOptions, form.actionTaken, form.actionTakenOther,
        onSelectedChange = { v -> onUpdate { it.copy(actionTaken = v) } },
        onOtherChange = { v -> onUpdate { it.copy(actionTakenOther = v) } })

    val aliOptions = lookups.findingOptions["ali"].orEmpty()
    MultiSelectChips("A.L.I. (As Left Inspection)", aliOptions, form.ali, form.aliOther,
        onSelectedChange = { v -> onUpdate { it.copy(ali = v) } },
        onOtherChange = { v -> onUpdate { it.copy(aliOther = v) } })

    DatePickerField("Susunod na PM Date (optional)", form.nextPmDate ?: "") { v -> onUpdate { it.copy(nextPmDate = v.ifBlank { null }) } }
}

@Composable
private fun CustomerSignatureStep(form: PmFormEntity, onUpdate: ((PmFormEntity) -> PmFormEntity) -> Unit) {
    CoaSection(
        coaType = form.coaType,
        onCoaTypeChange = { v -> onUpdate { it.copy(coaType = v) } },
        coaDate = form.coaDate ?: "",
        onCoaDateChange = { v -> onUpdate { it.copy(coaDate = v.ifBlank { null }) } },
        coaGenericText = form.coaGenericText ?: "",
        onCoaGenericTextChange = { v -> onUpdate { it.copy(coaGenericText = v) } },
        dateField = { label, value, onChange -> DatePickerField(label, value, onChange) },
        allowStandardCoa = true,
        coaMonthYear = form.coaMonthYear ?: "",
        onCoaMonthYearChange = { v -> onUpdate { it.copy(coaMonthYear = v) } },
    )
    Spacer(Modifier.height(16.dp))

    SectionLabel("Customer")
    OutlinedTextField(form.customerName, { v -> onUpdate { it.copy(customerName = v) } }, label = { Text("Pangalan ng Customer") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(form.customerPosition, { v -> onUpdate { it.copy(customerPosition = v) } }, label = { Text("Position (optional)") }, modifier = Modifier.fillMaxWidth())
    DatePickerField("Petsa ng Pirma", form.customerSignatureDate ?: "") { v -> onUpdate { it.copy(customerSignatureDate = v.ifBlank { null }) } }

    SectionLabel("Pirma ng Customer")
    SignaturePad(
        existingPath = form.customerSignaturePath,
        fileNamePrefix = "pm_${form.localId}",
        onSaved = { path -> onUpdate { it.copy(customerSignaturePath = path) } },
        onCleared = { onUpdate { it.copy(customerSignaturePath = null) } },
    )
}

@Composable
private fun ReviewStep(form: PmFormEntity) {
    SectionLabel("Review bago i-submit")
    ReviewRow("Company", form.companyName)
    ReviewRow("Address", form.address)
    ReviewRow("Date", form.formDate)
    ReviewRow("Personnel", (listOf("(ikaw)") + form.personnel).joinToString(", "))
    if (form.isSmStore) {
        ReviewRow("Checklist", "${form.smChecklist.size} item(s) checked")
        ReviewRow("Particulars", "${form.particulars.size} item(s)")
    } else {
        ReviewRow("AC Units", "${form.units.size} unit(s)")
    }
    ReviewRow("COA", if (form.coaType == "none" || form.coaType.isBlank()) "Wala" else form.coaType)
    ReviewRow("Customer", form.customerName)
    ReviewRow("Pirma", if (form.customerSignaturePath != null) "✓ Naka-save" else "✗ Wala pa")
    Spacer(Modifier.height(8.dp))
    Text(
        "Pag-tap ng I-submit, ise-save ito sa device at awtomatikong mag-a-upload pag may internet signal. Makikita mo ang status sa 'Mga Records Ko'.",
        fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ReviewRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(100.dp))
        Text(value.ifBlank { "—" }, fontSize = 13.sp, modifier = Modifier.weight(1f))
    }
}
