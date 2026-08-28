package com.airemore.fieldapp.ui.install

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airemore.fieldapp.AiremoreApp
import com.airemore.fieldapp.data.local.InstallUnit
import com.airemore.fieldapp.data.local.entity.InstallFormEntity
import com.airemore.fieldapp.data.repository.LookupData
import com.airemore.fieldapp.ui.common.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

private const val TOTAL_STEPS = 5

@Composable
fun InstallFormScreen(app: AiremoreApp, localId: Long, onDone: () -> Unit) {
    val scope = rememberCoroutineScope()
    var form by remember { mutableStateOf<InstallFormEntity?>(null) }
    var lookups by remember { mutableStateOf(LookupData.EMPTY) }
    var step by remember { mutableIntStateOf(0) }
    var submitting by remember { mutableStateOf(false) }

    LaunchedEffect(localId) {
        lookups = app.lookupRepository.getCached()
        form = if (localId <= 0) {
            val fullName = app.session.currentFullName()
            val newId = app.installRepository.createDraft(fullName)
            app.installRepository.getById(newId)
        } else {
            app.installRepository.getById(localId)
        }
    }

    val currentForm = form ?: run {
        Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) { CircularProgressIndicator() }
        return
    }

    fun update(transform: (InstallFormEntity) -> InstallFormEntity) {
        // Always read the latest state at the moment of the edit, not a
        // snapshot captured at the start of this recomposition. Two edits
        // landing in the same frame (e.g. typing Address then Date quickly)
        // would otherwise both transform from the same stale `currentForm`,
        // and whichever write happened last would silently wipe out the
        // other field's change.
        val latest = form ?: return
        val updated = transform(latest)
        form = updated
        scope.launch { app.installRepository.saveDraft(updated) }
    }

    val stepTitle = listOf("Company Info", "Personnel", "Units", "Larawan & Details", "Customer & Pirma")[step]

    WizardScaffold(
        title = "Installation",
        stepIndex = step,
        stepCount = TOTAL_STEPS,
        stepTitle = stepTitle,
        onBack = { step-- },
        onClose = onDone,
        onNext = if (step < TOTAL_STEPS - 1) { { step++ } } else null,
        onSubmit = if (step == TOTAL_STEPS - 1) {
            { submitting = true; scope.launch { app.installRepository.submit(currentForm.localId); submitting = false; onDone() } }
        } else null,
        nextEnabled = !submitting && stepIsValid(step, currentForm),
    ) {
        when (step) {
            0 -> InfoStep(currentForm, lookups, onUpdate = ::update)
            1 -> PersonnelStep(currentForm, onUpdate = ::update)
            2 -> UnitsStep(currentForm, lookups, onUpdate = ::update)
            3 -> DetailsPhotosStep(currentForm, onUpdate = ::update)
            4 -> CustomerSignatureStep(currentForm, onUpdate = ::update)
        }
    }
}

private fun stepIsValid(step: Int, form: InstallFormEntity): Boolean = when (step) {
    0 -> form.companyName.isNotBlank() && form.formDate.isNotBlank()
    4 -> form.customerName.isNotBlank() && form.customerSignaturePath != null
    else -> true
}

@Composable
private fun InfoStep(form: InstallFormEntity, lookups: LookupData, onUpdate: ((InstallFormEntity) -> InstallFormEntity) -> Unit) {
    var showAddCompany by remember { mutableStateOf(false) }
    SectionLabel("Company")
    CompanyPickerInstall(lookups.companies, form.companyName,
        onSelect = { c -> onUpdate { it.copy(companyId = c.id, companyName = c.name, address = c.address ?: it.address) } },
        onAddNew = { showAddCompany = true })
    OutlinedTextField(form.address, { v -> onUpdate { it.copy(address = v) } }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
    OutlinedTextField(
        value = form.formDate, onValueChange = { v -> onUpdate { it.copy(formDate = v) } },
        label = { Text("Petsa") }, placeholder = { Text("YYYY-MM-DD") },
        trailingIcon = { TextButton(onClick = { onUpdate { it.copy(formDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())) } }) { Text("Today") } },
        modifier = Modifier.fillMaxWidth(),
    )
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
                    Button(onClick = { if (name.isNotBlank()) { onUpdate { it.copy(companyId = null, companyName = name, address = address) }; showAddCompany = false } }, modifier = Modifier.fillMaxWidth()) { Text("Idagdag") }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showAddCompany = false }) { Text("Cancel") } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompanyPickerInstall(
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
            label = { Text("Piliin o i-type ang company") }, modifier = Modifier.menuAnchor().fillMaxWidth(),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            filtered.take(20).forEach { c -> DropdownMenuItem(text = { Text(c.name) }, onClick = { query = c.name; onSelect(c); expanded = false }) }
            DropdownMenuItem(text = { Text("+ Magdagdag ng Bagong Company") }, onClick = { expanded = false; onAddNew() })
        }
    }
}

@Composable
private fun PersonnelStep(form: InstallFormEntity, onUpdate: ((InstallFormEntity) -> InstallFormEntity) -> Unit) {
    SectionLabel("Personnel")
    Text("Ikaw ang laging Personnel #1.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
private fun UnitsStep(form: InstallFormEntity, lookups: LookupData, onUpdate: ((InstallFormEntity) -> InstallFormEntity) -> Unit) {
    SectionLabel("Mga Units na Ii-install (${form.units.size})")
    form.units.forEachIndexed { index, unit ->
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text("Unit ${index + 1}", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    IconButton(onClick = { onUpdate { f -> f.copy(units = f.units.toMutableList().also { it.removeAt(index) }) } }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Remove unit")
                    }
                }
                OutlinedTextField(
                    value = unit.quantity,
                    onValueChange = { v -> onUpdate { f -> f.copy(units = f.units.toMutableList().also { it[index] = unit.copy(quantity = v) }) } },
                    label = { Text("Quantity") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                DropdownWithOthers("Type", lookups.acTypes, unit.acType,
                    { v -> onUpdate { f -> f.copy(units = f.units.toMutableList().also { it[index] = unit.copy(acType = v) }) } })
                Spacer(Modifier.height(8.dp))
                DropdownWithOthers("Brand", lookups.acBrands, unit.brand,
                    { v -> onUpdate { f -> f.copy(units = f.units.toMutableList().also { it[index] = unit.copy(brand = v) }) } })
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = unit.model,
                    onValueChange = { v -> onUpdate { f -> f.copy(units = f.units.toMutableList().also { it[index] = unit.copy(model = v) }) } },
                    label = { Text("Model") }, modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                DropdownWithOthers("Capacity", lookups.acCapacities, unit.capacity,
                    { v -> onUpdate { f -> f.copy(units = f.units.toMutableList().also { it[index] = unit.copy(capacity = v) }) } })
            }
        }
    }
    OutlinedButton(onClick = { onUpdate { f -> f.copy(units = f.units + InstallUnit()) } }) {
        Icon(Icons.Filled.Add, contentDescription = null); Spacer(Modifier.width(4.dp)); Text("Magdagdag ng Unit")
    }
}

@Composable
private fun DetailsPhotosStep(form: InstallFormEntity, onUpdate: ((InstallFormEntity) -> InstallFormEntity) -> Unit) {
    SectionLabel("Detalye ng Trabaho")
    OutlinedTextField(form.pmActivity, { v -> onUpdate { it.copy(pmActivity = v) } }, label = { Text("Detalye / gawain sa site") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
    Spacer(Modifier.height(8.dp))
    PhotoPickerRow("Larawan ng Site — Bago (optional, max 4)", form.photosBefore, 4) { onUpdate { f -> f.copy(photosBefore = it) } }
    Spacer(Modifier.height(8.dp))
    PhotoPickerRow("Larawan ng Site — Pagkatapos (optional, max 4)", form.photosAfter, 4) { onUpdate { f -> f.copy(photosAfter = it) } }
}

@Composable
private fun CustomerSignatureStep(form: InstallFormEntity, onUpdate: ((InstallFormEntity) -> InstallFormEntity) -> Unit) {
    SectionLabel("Customer")
    OutlinedTextField(form.customerName, { v -> onUpdate { it.copy(customerName = v) } }, label = { Text("Pangalan ng Customer") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(form.customerPosition, { v -> onUpdate { it.copy(customerPosition = v) } }, label = { Text("Position (optional)") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(
        value = form.customerSignatureDate ?: "", onValueChange = { v -> onUpdate { it.copy(customerSignatureDate = v.ifBlank { null }) } },
        label = { Text("Petsa ng Pirma") }, placeholder = { Text("YYYY-MM-DD") },
        trailingIcon = { TextButton(onClick = { onUpdate { it.copy(customerSignatureDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())) } }) { Text("Today") } },
        modifier = Modifier.fillMaxWidth(),
    )
    SectionLabel("Pirma ng Customer")
    SignaturePad(
        existingPath = form.customerSignaturePath,
        fileNamePrefix = "install_${form.localId}",
        onSaved = { path -> onUpdate { it.copy(customerSignaturePath = path) } },
        onCleared = { onUpdate { it.copy(customerSignaturePath = null) } },
    )
}
