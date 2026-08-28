package com.airemore.fieldapp.ui.records

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airemore.fieldapp.AiremoreApp
import com.airemore.fieldapp.data.local.SyncStatus
import com.airemore.fieldapp.sync.SyncScheduler
import com.airemore.fieldapp.ui.theme.StatusDraft
import com.airemore.fieldapp.ui.theme.StatusFailed
import com.airemore.fieldapp.ui.theme.StatusPending
import com.airemore.fieldapp.ui.theme.StatusSynced
import java.text.SimpleDateFormat
import java.util.*

data class RecordRow(
    val localId: Long,
    val companyName: String,
    val formDate: String,
    val status: SyncStatus,
    val serviceReportNo: String?,
    val lastError: String?,
    val createdAtMillis: Long,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordListScreen(
    app: AiremoreApp,
    module: String, // "pm" | "repair" | "install"
    onBack: () -> Unit,
    onOpenFailedRecord: (Long) -> Unit,
) {
    val title = when (module) {
        "pm" -> "PM Forms"
        "repair" -> "Repair / Checkup"
        else -> "Installation"
    }

    val rows: List<RecordRow> by produceState(initialValue = emptyList(), module) {
        when (module) {
            "pm" -> app.pmRepository.observeAll()
            "repair" -> app.repairRepository.observeAll()
            else -> app.installRepository.observeAll()
        }.collect { list ->
            value = when (module) {
                "pm" -> list.filterIsInstance<com.airemore.fieldapp.data.local.entity.PmFormEntity>().map {
                    RecordRow(it.localId, it.companyName, it.formDate, it.syncStatus, it.serviceReportNo, it.lastError, it.createdAtMillis)
                }
                "repair" -> list.filterIsInstance<com.airemore.fieldapp.data.local.entity.RepairFormEntity>().map {
                    RecordRow(it.localId, it.companyName, it.formDate, it.syncStatus, it.serviceReportNo, it.lastError, it.createdAtMillis)
                }
                else -> list.filterIsInstance<com.airemore.fieldapp.data.local.entity.InstallFormEntity>().map {
                    RecordRow(it.localId, it.companyName, it.formDate, it.syncStatus, it.serviceReportNo, it.lastError, it.createdAtMillis)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                actions = {
                    // Manual trigger: without this, a PENDING record only
                    // gets retried by WorkManager's own scheduling (app
                    // start, network-reconnect callback, or the 15-min
                    // periodic tick) — there was no way for staff (or us,
                    // debugging) to force an attempt right now and see the
                    // result immediately.
                    TextButton(onClick = { SyncScheduler.scheduleImmediate(app) }) {
                        Text("I-sync Ngayon")
                    }
                },
            )
        }
    ) { padding ->
        if (rows.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Wala pang record dito.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(rows, key = { it.localId }) { row ->
                RecordCard(row, onClick = { if (row.status == SyncStatus.FAILED || row.status == SyncStatus.DRAFT) onOpenFailedRecord(row.localId) })
            }
        }
    }
}

@Composable
private fun RecordCard(row: RecordRow, onClick: () -> Unit) {
    val (label, color) = when (row.status) {
        SyncStatus.DRAFT -> "DRAFT — hindi pa na-submit" to StatusDraft
        SyncStatus.PENDING -> "Naka-queue, hinihintay ang signal" to StatusPending
        SyncStatus.SYNCING -> "Ina-upload ngayon…" to StatusPending
        SyncStatus.SYNCED -> "Na-submit na ✓" to StatusSynced
        SyncStatus.FAILED -> "May problema — i-tap para ayusin" to StatusFailed
    }
    val dateStr = remember(row.createdAtMillis) {
        SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault()).format(Date(row.createdAtMillis))
    }

    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    row.companyName.ifBlank { "(Walang company)" },
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    Modifier.clip(RoundedCornerShape(6.dp)).background(color.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text(row.status.name, fontSize = 10.sp, color = color, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(4.dp))
            Text("Date: ${row.formDate.ifBlank { "—" }} · Ginawa: $dateStr", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            row.serviceReportNo?.let {
                Text("SR No: $it", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(4.dp))
            Text(label, fontSize = 12.sp, color = color)
            if (row.lastError != null && row.status != SyncStatus.SYNCED && row.status != SyncStatus.DRAFT) {
                // Surfacing this even while PENDING (not just FAILED) matters:
                // PENDING covers both "hasn't tried yet" and "tried, hit a
                // network/server error, queued for auto-retry" — without this,
                // a record stuck retrying the same failure looks identical to
                // one just waiting for signal, and nobody can tell why.
                Text(row.lastError, fontSize = 11.sp, color = StatusFailed, modifier = Modifier.padding(top = 2.dp))
            }
        }
    }
}
