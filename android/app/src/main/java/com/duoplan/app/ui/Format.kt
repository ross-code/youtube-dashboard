package com.duoplan.app.ui

import com.duoplan.app.data.model.PlanCategory
import com.duoplan.app.data.model.PlanStatus
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val timeFmt = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
private val dateFmt = DateTimeFormatter.ofPattern("EEE, MMM d")

/** "Tonight · 7:00 PM", "Tomorrow · 6:30 PM", or "Sat, Jun 7 · 8:00 PM". */
fun Instant.asWhenLabel(zone: ZoneId = ZoneId.systemDefault()): String {
    val dt = atZone(zone)
    val today = LocalDate.now(zone)
    val day = when (dt.toLocalDate()) {
        today -> "Tonight"
        today.plusDays(1) -> "Tomorrow"
        else -> dateFmt.format(dt)
    }
    return "$day · ${timeFmt.format(dt)}"
}

fun PlanCategory.emoji(): String = when (this) {
    PlanCategory.MEAL -> "🍽️"
    PlanCategory.ACTIVITY -> "🎉"
}

fun PlanCategory.label(): String = when (this) {
    PlanCategory.MEAL -> "Meal"
    PlanCategory.ACTIVITY -> "Activity"
}

fun PlanStatus.label(): String = when (this) {
    PlanStatus.PENDING -> "Waiting"
    PlanStatus.APPROVED -> "Approved"
    PlanStatus.DENIED -> "Declined"
    PlanStatus.REVISION_REQUESTED -> "New idea, please"
}
