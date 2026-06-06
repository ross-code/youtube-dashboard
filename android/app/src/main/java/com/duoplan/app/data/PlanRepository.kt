package com.duoplan.app.data

import com.duoplan.app.data.model.CalendarChoice
import com.duoplan.app.data.model.Plan
import com.duoplan.app.data.model.PlanCategory
import com.duoplan.app.data.model.PlanStatus
import com.duoplan.app.data.remote.CalendarApi
import com.duoplan.app.data.remote.EventDateTime
import com.duoplan.app.data.remote.EventDto
import com.duoplan.app.data.remote.ExtendedProperties
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Reads and writes [Plan]s, each backed one-to-one by an event on the shared
 * Google Calendar. The calendar *is* the database: both partners point the app
 * at the same shared calendar, so a write by one shows up for the other.
 */
class PlanRepository(private val api: CalendarApi) {

    private val zone: ZoneId get() = ZoneId.systemDefault()

    /** Email of the signed-in Google account. */
    suspend fun fetchEmail(): String = api.userInfo().email

    /** Calendars the user can choose as the shared sync target. */
    suspend fun calendars(): List<CalendarChoice> =
        api.calendarList().items.map {
            CalendarChoice(
                id = it.id,
                name = it.summary ?: it.id,
                primary = it.primary,
                writable = it.accessRole == "owner" || it.accessRole == "writer",
            )
        }.sortedByDescending { it.primary }

    /** Recent + open plans, newest activity first. */
    suspend fun loadPlans(calendarId: String): List<Plan> {
        val since = Instant.now().minusSeconds(14L * 24 * 3600)
        return api.listEvents(
            calendarId = calendarId,
            timeMin = ISO.format(OffsetDateTime.ofInstant(since, zone)),
        ).items
            .mapNotNull { it.toPlanOrNull() }
            .sortedByDescending { it.updated }
    }

    suspend fun create(
        calendarId: String,
        title: String,
        category: PlanCategory,
        proposedBy: String,
        start: Instant,
        end: Instant,
    ): Plan {
        val props = props(
            category = category,
            status = PlanStatus.PENDING,
            proposedBy = proposedBy,
            respondedBy = null,
            note = null,
        )
        val dto = EventDto(
            summary = title,
            status = eventStatus(PlanStatus.PENDING),
            start = dateTime(start),
            end = dateTime(end),
            extendedProperties = ExtendedProperties(props),
        )
        return api.insertEvent(calendarId, dto).toPlanOrNull()!!
    }

    suspend fun approve(calendarId: String, plan: Plan, me: String, note: String?): Plan =
        respond(calendarId, plan, PlanStatus.APPROVED, me, note)

    suspend fun deny(calendarId: String, plan: Plan, me: String, note: String?): Plan =
        respond(calendarId, plan, PlanStatus.DENIED, me, note)

    suspend fun requestAnother(calendarId: String, plan: Plan, me: String, note: String?): Plan =
        respond(calendarId, plan, PlanStatus.REVISION_REQUESTED, me, note)

    /** Proposer replaces a turned-down idea with a fresh one; back to PENDING. */
    suspend fun revise(
        calendarId: String,
        plan: Plan,
        newTitle: String,
        newCategory: PlanCategory,
        start: Instant,
        end: Instant,
    ): Plan {
        val props = props(
            category = newCategory,
            status = PlanStatus.PENDING,
            proposedBy = plan.proposedBy,
            respondedBy = null,
            note = null,
        )
        val patch = EventDto(
            summary = newTitle,
            status = eventStatus(PlanStatus.PENDING),
            start = dateTime(start),
            end = dateTime(end),
            extendedProperties = ExtendedProperties(props),
        )
        return api.patchEvent(calendarId, plan.id, patch).toPlanOrNull()!!
    }

    /** Permanently removes the plan's event. */
    suspend fun delete(calendarId: String, plan: Plan) {
        api.deleteEvent(calendarId, plan.id)
    }

    private suspend fun respond(
        calendarId: String,
        plan: Plan,
        status: PlanStatus,
        me: String,
        note: String?,
    ): Plan {
        val props = props(
            category = plan.category,
            status = status,
            proposedBy = plan.proposedBy,
            respondedBy = me,
            note = note?.takeIf { it.isNotBlank() },
        )
        val patch = EventDto(
            status = eventStatus(status),
            extendedProperties = ExtendedProperties(props),
        )
        return api.patchEvent(calendarId, plan.id, patch).toPlanOrNull()!!
    }

    // --- mapping helpers ----------------------------------------------------

    private fun props(
        category: PlanCategory,
        status: PlanStatus,
        proposedBy: String,
        respondedBy: String?,
        note: String?,
    ): Map<String, String> = buildMap {
        put(KEY_TAG, "1")
        put(KEY_CATEGORY, category.name)
        put(KEY_STATUS, status.name)
        put(KEY_PROPOSED_BY, proposedBy)
        respondedBy?.let { put(KEY_RESPONDED_BY, it) }
        note?.let { put(KEY_NOTE, it) }
    }

    private fun dateTime(instant: Instant) = EventDateTime(
        dateTime = ISO.format(OffsetDateTime.ofInstant(instant, zone)),
        timeZone = zone.id,
    )

    private fun eventStatus(status: PlanStatus): String = when (status) {
        PlanStatus.APPROVED -> "confirmed"
        PlanStatus.DENIED -> "cancelled"
        PlanStatus.PENDING, PlanStatus.REVISION_REQUESTED -> "tentative"
    }

    private fun EventDto.toPlanOrNull(): Plan? {
        val props = extendedProperties?.privateProps ?: return null
        if (props[KEY_TAG] != "1") return null
        val id = id ?: return null
        return Plan(
            id = id,
            title = summary.orEmpty(),
            category = runCatching { PlanCategory.valueOf(props[KEY_CATEGORY] ?: "") }
                .getOrDefault(PlanCategory.ACTIVITY),
            status = runCatching { PlanStatus.valueOf(props[KEY_STATUS] ?: "") }
                .getOrDefault(PlanStatus.PENDING),
            proposedBy = props[KEY_PROPOSED_BY].orEmpty(),
            respondedBy = props[KEY_RESPONDED_BY],
            note = props[KEY_NOTE],
            start = parseInstant(start),
            end = parseInstant(end),
            updated = updated?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: Instant.now(),
        )
    }

    private fun parseInstant(dt: EventDateTime?): Instant {
        if (dt == null) return Instant.now()
        dt.dateTime?.let { return OffsetDateTime.parse(it).toInstant() }
        dt.date?.let { return LocalDate.parse(it).atStartOfDay(zone).toInstant() }
        return Instant.now()
    }

    private companion object {
        val ISO: DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
        const val KEY_TAG = "duoplan"
        const val KEY_CATEGORY = "category"
        const val KEY_STATUS = "status"
        const val KEY_PROPOSED_BY = "proposedBy"
        const val KEY_RESPONDED_BY = "respondedBy"
        const val KEY_NOTE = "note"
    }
}
