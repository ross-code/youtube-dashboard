package com.duoplan.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.duoplan.app.data.model.CalendarChoice

/** First-run screen: choose the shared calendar both partners point at. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarSetupScreen(
    calendars: List<CalendarChoice>,
    selectedId: String?,
    onRefresh: () -> Unit,
    onSelect: (CalendarChoice) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pick your shared calendar") },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Reload calendars")
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Text(
                "Both of you should add the same shared calendar to your Google " +
                    "account, then pick it here. Requests and approvals live on it.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            )
            CalendarList(calendars, selectedId, onSelect)
        }
    }
}

/** Settings reachable from the home screen. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    email: String,
    calendars: List<CalendarChoice>,
    selectedId: String?,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onSelect: (CalendarChoice) -> Unit,
    onSignOut: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Reload calendars")
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Text(
                "Signed in as",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 20.dp, top = 12.dp),
            )
            Text(email, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 20.dp))
            HorizontalDivider(Modifier.padding(vertical = 12.dp))
            Text(
                "Shared calendar",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            Box(Modifier.weight(1f)) {
                CalendarList(calendars, selectedId, onSelect)
            }
            TextButton(
                onClick = onSignOut,
                modifier = Modifier.padding(16.dp).align(Alignment.CenterHorizontally),
            ) {
                Text("Sign out", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun CalendarList(
    calendars: List<CalendarChoice>,
    selectedId: String?,
    onSelect: (CalendarChoice) -> Unit,
) {
    if (calendars.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(
                "No calendars loaded yet. Tap refresh.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }
    LazyColumn(Modifier.fillMaxWidth()) {
        items(calendars, key = { it.id }) { cal ->
            val enabled = cal.writable
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(enabled = enabled) { onSelect(cal) }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                RadioButton(selected = cal.id == selectedId, enabled = enabled, onClick = { onSelect(cal) })
                Column(Modifier.weight(1f)) {
                    Text(
                        cal.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (cal.id == selectedId) FontWeight.SemiBold else FontWeight.Normal,
                    )
                    if (!enabled) {
                        Text(
                            "Read-only — can't host plans",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
