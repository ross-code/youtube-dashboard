package com.duoplan.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.duoplan.app.auth.AuthOutcome
import com.duoplan.app.ui.MainViewModel
import com.duoplan.app.ui.UiState
import com.duoplan.app.ui.screens.AuthScreen
import com.duoplan.app.ui.screens.CalendarSetupScreen
import com.duoplan.app.ui.screens.ComposeRequestScreen
import com.duoplan.app.ui.screens.DetailScreen
import com.duoplan.app.ui.screens.HomeScreen
import com.duoplan.app.ui.screens.SettingsScreen
import com.duoplan.app.ui.theme.DuoPlanTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val container: AppContainer by lazy { (application as DuoPlanApp).container }
    private val viewModel: MainViewModel by viewModels { MainViewModel.factory(container) }

    private val consentLauncher =
        registerForActivityResult(StartIntentSenderForResult()) {
            // Regardless of the exact result, try to finish sign-in: the token is
            // now cached if the user granted consent.
            viewModel.completeSignIn()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DuoPlanTheme {
                DuoPlanRoot(viewModel = viewModel, onConnect = ::connectGoogle)
            }
        }
    }

    /** Kicks off interactive Google authorization. */
    private fun connectGoogle() {
        lifecycleScope.launch {
            runCatching { container.auth.authorize() }
                .onSuccess { outcome ->
                    when (outcome) {
                        is AuthOutcome.Authorized -> viewModel.completeSignIn()
                        is AuthOutcome.NeedsConsent ->
                            consentLauncher.launch(
                                IntentSenderRequest.Builder(outcome.intentSender).build(),
                            )
                    }
                }
        }
    }
}

private object Routes {
    const val HOME = "home"
    const val NEW = "new"
    const val SETTINGS = "settings"
    const val DETAIL = "detail/{id}"
    const val REVISE = "revise/{id}"
    fun detail(id: String) = "detail/$id"
    fun revise(id: String) = "revise/$id"
}

@Composable
private fun DuoPlanRoot(viewModel: MainViewModel, onConnect: () -> Unit) {
    val state by viewModel.ui.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }
    LaunchedEffect(state.error) {
        state.error?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeError()
        }
    }
    // Make sure calendars are available when a signed-in user still needs to pick one.
    LaunchedEffect(state.settings.isSignedIn, state.settings.hasCalendar) {
        if (state.settings.isSignedIn && !state.settings.hasCalendar && state.calendars.isEmpty()) {
            viewModel.loadCalendars()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                !state.settings.isSignedIn -> AuthScreen(busy = state.busy, onConnect = onConnect)
                !state.settings.hasCalendar -> CalendarSetupScreen(
                    calendars = state.calendars,
                    selectedId = state.settings.calendarId,
                    onRefresh = viewModel::loadCalendars,
                    onSelect = viewModel::selectCalendar,
                )
                else -> MainNav(state, viewModel)
            }
        }
    }
}

@Composable
private fun MainNav(state: UiState, viewModel: MainViewModel) {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                state = state,
                onNew = { nav.navigate(Routes.NEW) },
                onOpen = { nav.navigate(Routes.detail(it.id)) },
                onRefresh = viewModel::refresh,
                onSettings = { nav.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.NEW) {
            ComposeRequestScreen(
                screenTitle = "Suggest something",
                submitLabel = "Send to partner",
                busy = state.busy,
                onBack = { nav.popBackStack() },
                onSubmit = { title, category, start, end ->
                    viewModel.createPlan(title, category, start, end)
                    nav.popBackStack()
                },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                email = state.me,
                calendars = state.calendars,
                selectedId = state.settings.calendarId,
                onBack = { nav.popBackStack() },
                onRefresh = viewModel::loadCalendars,
                onSelect = viewModel::selectCalendar,
                onSignOut = viewModel::signOut,
            )
        }
        composable(
            Routes.DETAIL,
            arguments = listOf(navArgument("id") { type = NavType.StringType }),
        ) { entry ->
            val id = entry.arguments?.getString("id")
            val plan = state.plans.firstOrNull { it.id == id }
            if (plan == null) {
                LaunchedEffect(Unit) { nav.popBackStack() }
            } else {
                DetailScreen(
                    plan = plan,
                    me = state.me,
                    busy = state.busy,
                    onBack = { nav.popBackStack() },
                    onApprove = { note -> viewModel.approve(plan, note); nav.popBackStack() },
                    onDeny = { note -> viewModel.deny(plan, note); nav.popBackStack() },
                    onRequestAnother = { note -> viewModel.requestAnother(plan, note); nav.popBackStack() },
                    onRevise = { nav.navigate(Routes.revise(plan.id)) },
                    onDelete = { viewModel.delete(plan); nav.popBackStack() },
                )
            }
        }
        composable(
            Routes.REVISE,
            arguments = listOf(navArgument("id") { type = NavType.StringType }),
        ) { entry ->
            val id = entry.arguments?.getString("id")
            val plan = state.plans.firstOrNull { it.id == id }
            if (plan == null) {
                LaunchedEffect(Unit) { nav.popBackStack() }
            } else {
                ComposeRequestScreen(
                    screenTitle = "New idea",
                    submitLabel = "Send new idea",
                    initialTitle = plan.title,
                    initialCategory = plan.category,
                    busy = state.busy,
                    onBack = { nav.popBackStack() },
                    onSubmit = { title, category, start, end ->
                        viewModel.revise(plan, title, category, start, end)
                        nav.popBackStack(Routes.HOME, inclusive = false)
                    },
                )
            }
        }
    }
}
