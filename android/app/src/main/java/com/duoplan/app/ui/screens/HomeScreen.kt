package com.duoplan.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.duoplan.app.data.model.Plan
import com.duoplan.app.data.model.PlanStatus
import com.duoplan.app.ui.UiState
import com.duoplan.app.ui.components.PlanCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: UiState,
    onNew: () -> Unit,
    onOpen: (Plan) -> Unit,
    onRefresh: () -> Unit,
    onSettings: () -> Unit,
) {
    val me = state.me
    val yourTurn = state.plans.filter { it.awaitingMyResponse(me) }
    val yourMove = state.plans.filter { it.needsMyRevision(me) }
    val waiting = state.plans.filter {
        (it.status == PlanStatus.PENDING && it.isMine(me)) ||
            (it.status == PlanStatus.REVISION_REQUESTED && !it.isMine(me))
    }
    val decided = state.plans.filter {
        it.status == PlanStatus.APPROVED || it.status == PlanStatus.DENIED
    }
    val isEmpty = state.plans.isEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("DuoPlan")
                        state.settings.calendarName?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNew,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Suggest") },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())

            if (isEmpty && !state.loading) {
                EmptyState()
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    section("Your turn", yourTurn, me, onOpen)
                    section("Needs a new idea from you", yourMove, me, onOpen)
                    section("Waiting on your partner", waiting, me, onOpen)
                    section("Recently decided", decided, me, onOpen)
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.section(
    title: String,
    plans: List<Plan>,
    me: String,
    onOpen: (Plan) -> Unit,
) {
    if (plans.isEmpty()) return
    item(key = "h_$title") {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 12.dp, bottom = 2.dp),
        )
    }
    items(plans, key = { it.id }) { plan ->
        PlanCard(plan = plan, me = me, onClick = { onOpen(plan) })
    }
}

@Composable
private fun EmptyState() {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🍜🎬", style = MaterialTheme.typography.headlineMedium)
            Text(
                "No plans yet",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 12.dp),
            )
            Text(
                "Tap Suggest to propose what to eat or do tonight. " +
                    "Your partner will get to approve, decline, or ask for another idea.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}
