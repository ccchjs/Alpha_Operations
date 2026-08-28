package com.airemore.fieldapp.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airemore.fieldapp.AiremoreApp
import com.airemore.fieldapp.ui.theme.StatusFailed
import com.airemore.fieldapp.ui.theme.StatusPending
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    app: AiremoreApp,
    onOpenRecords: (String) -> Unit,
    onNewPm: () -> Unit,
    onNewRepair: () -> Unit,
    onNewInstall: () -> Unit,
    onLoggedOut: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val fullName by app.session.fullNameFlow.collectAsState(initial = "")

    val pendingTotal by remember {
        combine(
            app.pmRepository.observePendingCount(),
            app.repairRepository.observePendingCount(),
            app.installRepository.observePendingCount(),
        ) { a, b, c -> a + b + c }
    }.collectAsState(initial = 0)

    val failedTotal by remember {
        combine(
            app.pmRepository.observeFailedCount(),
            app.repairRepository.observeFailedCount(),
            app.installRepository.observeFailedCount(),
        ) { a, b, c -> a + b + c }
    }.collectAsState(initial = 0)

    var showLogoutConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Airemore") },
                actions = {
                    IconButton(onClick = { showLogoutConfirm = true }) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout")
                    }
                },
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Text("Hi, ${fullName?.ifBlank { "Staff" } ?: "Staff"} 👋", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }

            if (pendingTotal > 0 || failedTotal > 0) {
                item { SyncStatusBanner(pendingTotal, failedTotal) }
            }

            item {
                Text("Bagong Form", fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 4.dp))
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    ModuleActionCard("PM", "Preventive\nMaintenance", Icons.Filled.Build, Modifier.weight(1f), onNewPm)
                    ModuleActionCard("Repair", "Repair /\nCheckup", Icons.Filled.Handyman, Modifier.weight(1f), onNewRepair)
                    ModuleActionCard("Install", "Installation", Icons.Filled.Construction, Modifier.weight(1f), onNewInstall)
                }
            }

            item {
                Text("Mga Records Ko", fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 12.dp))
            }
            item { RecordsRow("PM Forms", Icons.Filled.Build) { onOpenRecords("pm") } }
            item { RecordsRow("Repair / Checkup", Icons.Filled.Handyman) { onOpenRecords("repair") } }
            item { RecordsRow("Installation", Icons.Filled.Construction) { onOpenRecords("install") } }
        }
    }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text("Mag-logout?") },
            text = { Text("Ang mga naka-DRAFT o PENDING na form ay mananatili sa device — hindi mawawala.") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutConfirm = false
                    scope.launch { app.authRepository.logout(); onLoggedOut() }
                }) { Text("Logout") }
            },
            dismissButton = { TextButton(onClick = { showLogoutConfirm = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun SyncStatusBanner(pending: Int, failed: Int) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (failed > 0) StatusFailed.copy(alpha = 0.12f) else StatusPending.copy(alpha = 0.12f))
            .padding(12.dp),
    ) {
        if (pending > 0) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.CloudUpload, contentDescription = null, tint = StatusPending, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("$pending record(s) naka-queue, mag-a-upload pag may signal.", fontSize = 13.sp)
            }
        }
        if (failed > 0) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = if (pending > 0) 6.dp else 0.dp)) {
                Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = StatusFailed, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("$failed record(s) hindi na-submit — buksan sa 'Mga Records Ko' para ayusin.", fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun ModuleActionCard(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.height(110.dp).clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(
            Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(6.dp))
            Text(subtitle, fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center, lineHeight = 14.sp)
        }
    }
}

@Composable
private fun RecordsRow(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Text(label, fontSize = 14.sp, modifier = Modifier.weight(1f))
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Color.Gray)
        }
    }
}
