package com.duoplan.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.duoplan.app.AppContainer
import com.duoplan.app.data.Settings
import com.duoplan.app.data.model.CalendarChoice
import com.duoplan.app.data.model.Plan
import com.duoplan.app.data.model.PlanCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant

data class UiState(
    val settings: Settings = Settings(),
    val plans: List<Plan> = emptyList(),
    val calendars: List<CalendarChoice> = emptyList(),
    val loading: Boolean = false,
    val busy: Boolean = false,
    val error: String? = null,
    val message: String? = null,
) {
    val me: String get() = settings.email.orEmpty()
}

class MainViewModel(private val container: AppContainer) : ViewModel() {

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    private var loadedCalendar: String? = null

    init {
        viewModelScope.launch {
            container.settings.state.collect { settings ->
                _ui.update { it.copy(settings = settings) }
                if (settings.isSignedIn && settings.hasCalendar &&
                    settings.calendarId != loadedCalendar
                ) {
                    loadedCalendar = settings.calendarId
                    refresh()
                }
            }
        }
    }

    /** Called after the Google authorization activity returns successfully. */
    fun completeSignIn() = viewModelScope.launch {
        _ui.update { it.copy(busy = true, error = null) }
        runCatching { container.plans.fetchEmail() }
            .onSuccess { email ->
                container.settings.setUser(email)
                loadCalendars()
            }
            .onFailure { e -> _ui.update { it.copy(error = e.userMessage()) } }
        _ui.update { it.copy(busy = false) }
    }

    fun loadCalendars() = viewModelScope.launch {
        runCatching { container.plans.calendars() }
            .onSuccess { list -> _ui.update { it.copy(calendars = list) } }
            .onFailure { e -> _ui.update { it.copy(error = e.userMessage()) } }
    }

    fun selectCalendar(choice: CalendarChoice) = viewModelScope.launch {
        container.settings.setCalendar(choice.id, choice.name)
    }

    fun refresh() = viewModelScope.launch {
        val calendarId = _ui.value.settings.calendarId ?: return@launch
        _ui.update { it.copy(loading = true, error = null) }
        runCatching { container.plans.loadPlans(calendarId) }
            .onSuccess { list -> _ui.update { it.copy(plans = list) } }
            .onFailure { e -> _ui.update { it.copy(error = e.userMessage()) } }
        _ui.update { it.copy(loading = false) }
    }

    fun createPlan(title: String, category: PlanCategory, start: Instant, end: Instant) = mutate {
        val calendarId = requireCalendar()
        container.plans.create(calendarId, title.trim(), category, _ui.value.me, start, end)
        message("Sent to your partner")
    }

    fun approve(plan: Plan, note: String?) = mutate {
        container.plans.approve(requireCalendar(), plan, _ui.value.me, note)
        message("Approved 🎉")
    }

    fun deny(plan: Plan, note: String?) = mutate {
        container.plans.deny(requireCalendar(), plan, _ui.value.me, note)
        message("Declined")
    }

    fun requestAnother(plan: Plan, note: String?) = mutate {
        container.plans.requestAnother(requireCalendar(), plan, _ui.value.me, note)
        message("Asked for another idea")
    }

    fun revise(plan: Plan, title: String, category: PlanCategory, start: Instant, end: Instant) = mutate {
        container.plans.revise(requireCalendar(), plan, title.trim(), category, start, end)
        message("New idea sent")
    }

    fun delete(plan: Plan) = mutate {
        container.plans.delete(requireCalendar(), plan)
        message("Removed")
    }

    fun signOut() = viewModelScope.launch {
        loadedCalendar = null
        container.settings.clear()
        _ui.update { UiState() }
    }

    fun consumeMessage() = _ui.update { it.copy(message = null) }
    fun consumeError() = _ui.update { it.copy(error = null) }

    private fun requireCalendar(): String =
        _ui.value.settings.calendarId ?: error("No shared calendar selected")

    private fun message(text: String) = _ui.update { it.copy(message = text) }

    /** Run a mutating call, then reload so both partners' views converge. */
    private fun mutate(block: suspend () -> Unit) = viewModelScope.launch {
        _ui.update { it.copy(busy = true, error = null) }
        runCatching { block() }
            .onFailure { e -> _ui.update { it.copy(error = e.userMessage()) } }
        _ui.update { it.copy(busy = false) }
        refresh()
    }

    companion object {
        fun factory(container: AppContainer) = viewModelFactory {
            initializer { MainViewModel(container) }
        }
    }
}

private fun Throwable.userMessage(): String =
    message?.takeIf { it.isNotBlank() } ?: "Something went wrong. Check your connection and try again."
