package com.duoplan.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.duoplan.app.data.model.Plan
import com.duoplan.app.data.model.PlanStatus
import com.duoplan.app.ui.asWhenLabel
import com.duoplan.app.ui.components.StatusChip
import com.duoplan.app.ui.emoji

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    plan: Plan,
    me: String,
    busy: Boolean,
    onBack: () -> Unit,
    onApprove: (note: String?) -> Unit,
    onDeny: (note: String?) -> Unit,
    onRequestAnother: (note: String?) -> Unit,
    onRevise: () -> Unit,
    onDelete: () -> Unit,
) {
    // Which optional-note dialog is open, if any.
    var noteFor by remember { mutableStateOf<NoteAction?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Plan") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Card(shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Text("${plan.category.emoji()}  ", style = MaterialTheme.typography.headlineMedium)
                        Text(
                            plan.title.ifBlank { "(untitled)" },
                            style = MaterialTheme.typography.titleLarge,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Text(plan.start.asWhenLabel(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        StatusChip(plan.status)
                        Text(
                            "  ${if (plan.isMine(me)) "Suggested by you" else "Suggested by your partner"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    plan.note?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            "“$it”",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }

            Actions(
                plan = plan,
                me = me,
                busy = busy,
                onApprove = { onApprove(null) },
                onDenyClick = { noteFor = NoteAction.DENY },
                onAnotherClick = { noteFor = NoteAction.ANOTHER },
                onRevise = onRevise,
                onDelete = onDelete,
            )
        }
    }

    noteFor?.let { action ->
        NoteDialog(
            title = if (action == NoteAction.DENY) "Decline this?" else "Ask for another idea?",
            confirmLabel = if (action == NoteAction.DENY) "Decline" else "Ask for another",
            onDismiss = { noteFor = null },
            onConfirm = { note ->
                noteFor = null
                when (action) {
                    NoteAction.DENY -> onDeny(note)
                    NoteAction.ANOTHER -> onRequestAnother(note)
                }
            },
        )
    }
}

private enum class NoteAction { DENY, ANOTHER }

@Composable
private fun Actions(
    plan: Plan,
    me: String,
    busy: Boolean,
    onApprove: () -> Unit,
    onDenyClick: () -> Unit,
    onAnotherClick: () -> Unit,
    onRevise: () -> Unit,
    onDelete: () -> Unit,
) {
    when {
        plan.awaitingMyResponse(me) -> {
            Button(onClick = onApprove, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                Text("Approve")
            }
            OutlinedButton(onClick = onAnotherClick, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                Text("Ask for another idea")
            }
            TextButton(onClick = onDenyClick, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                Text("Decline", color = MaterialTheme.colorScheme.error)
            }
        }

        plan.needsMyRevision(me) -> {
            Text(
                "Your partner asked for a different idea.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onRevise, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                Text("Send a new idea")
            }
            TextButton(onClick = onDelete, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                Text("Withdraw", color = MaterialTheme.colorScheme.error)
            }
        }

        plan.status == PlanStatus.PENDING && plan.isMine(me) -> {
            Text(
                "Waiting for your partner to respond.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onDelete, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                Text("Withdraw", color = MaterialTheme.colorScheme.error)
            }
        }

        plan.status == PlanStatus.REVISION_REQUESTED && !plan.isMine(me) -> {
            Text(
                "You asked for another idea. Waiting for a new suggestion.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        else -> {
            // Approved or declined: it's history now.
            Text(
                if (plan.status == PlanStatus.APPROVED) "It's a date! This is on your shared calendar."
                else "This one was declined.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onDelete, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                Text("Remove from calendar", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun NoteDialog(
    title: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String?) -> Unit,
) {
    var note by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Add a note (optional)") },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(note.ifBlank { null }) },
                colors = ButtonDefaults.buttonColors(),
            ) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
