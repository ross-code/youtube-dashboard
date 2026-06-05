package com.duoplan.app.data.model

import java.time.Instant

/** Whether a proposal is about food or an activity. */
enum class PlanCategory { MEAL, ACTIVITY }

/**
 * Lifecycle of a proposal. This is the source of truth for app logic and is
 * stored in the calendar event's private extended properties (the event's own
 * confirmed/tentative/cancelled status is also set, so the plan looks sensible
 * in Google Calendar itself).
 */
enum class PlanStatus { PENDING, APPROVED, DENIED, REVISION_REQUESTED }

/**
 * A single request a partner makes ("Tacos tonight?", "Movie night?"), backed
 * one-to-one by an event on the shared Google Calendar.
 */
data class Plan(
    val id: String,
    val title: String,
    val category: PlanCategory,
    val status: PlanStatus,
    val proposedBy: String,
    val respondedBy: String?,
    val note: String?,
    val start: Instant,
    val end: Instant,
    val updated: Instant,
) {
    /** True when [me] is the partner who made this proposal. */
    fun isMine(me: String) = proposedBy.equals(me, ignoreCase = true)

    /** True when [me] is the one expected to respond right now. */
    fun awaitingMyResponse(me: String) =
        status == PlanStatus.PENDING && !isMine(me)

    /** True when [me] proposed it and needs to send a fresh idea. */
    fun needsMyRevision(me: String) =
        status == PlanStatus.REVISION_REQUESTED && isMine(me)
}

/** A calendar the signed-in user can see, offered as a sync target in Settings. */
data class CalendarChoice(
    val id: String,
    val name: String,
    val primary: Boolean,
    val writable: Boolean,
)
