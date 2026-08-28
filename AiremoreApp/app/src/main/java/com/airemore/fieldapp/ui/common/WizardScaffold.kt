package com.airemore.fieldapp.ui.common

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Renders one step of a multi-step form: a top bar with a step counter and
 * progress bar (so staff always sees how much is left — addresses the
 * "form is too long, staff loses track" problem from the original web
 * app), scrollable content, and Back/Next (or Submit) buttons pinned to
 * the bottom so they're reachable with a thumb on a big screen without
 * hunting for them at the very bottom of a long scroll.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WizardScaffold(
    title: String,
    stepIndex: Int,          // 0-based
    stepCount: Int,
    stepTitle: String,
    onBack: () -> Unit,
    onClose: () -> Unit,
    onNext: (() -> Unit)?,   // null on the last step
    onSubmit: (() -> Unit)?, // non-null only on the last step
    nextEnabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(title) },
                    navigationIcon = {
                        IconButton(onClick = if (stepIndex == 0) onClose else onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        }
                    },
                )
                LinearProgressIndicator(
                    progress = { (stepIndex + 1f) / stepCount },
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(
                        "Step ${stepIndex + 1} of $stepCount — $stepTitle",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        },
        bottomBar = {
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (stepIndex > 0) {
                    OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text("Balik") }
                }
                if (onNext != null) {
                    Button(onClick = onNext, enabled = nextEnabled, modifier = Modifier.weight(1f)) { Text("Susunod") }
                }
                if (onSubmit != null) {
                    Button(onClick = onSubmit, enabled = nextEnabled, modifier = Modifier.weight(1f)) { Text("I-submit") }
                }
            }
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            content()
        }
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(text, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
}
