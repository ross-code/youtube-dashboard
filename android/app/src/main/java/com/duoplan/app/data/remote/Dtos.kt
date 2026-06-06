package com.duoplan.app.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Subset of a Google Calendar v3 event that DuoPlan reads and writes. */
@Serializable
data class EventDto(
    val id: String? = null,
    val status: String? = null,
    val summary: String? = null,
    val description: String? = null,
    val start: EventDateTime? = null,
    val end: EventDateTime? = null,
    val updated: String? = null,
    val extendedProperties: ExtendedProperties? = null,
)

@Serializable
data class EventDateTime(
    val dateTime: String? = null,
    val timeZone: String? = null,
    val date: String? = null,
)

@Serializable
data class ExtendedProperties(
    // "private" is a Kotlin keyword, so map the JSON key explicitly.
    @SerialName("private") val privateProps: Map<String, String>? = null,
)

@Serializable
data class EventsResponse(
    val items: List<EventDto> = emptyList(),
    val nextPageToken: String? = null,
)

@Serializable
data class CalendarListResponse(
    val items: List<CalendarListEntry> = emptyList(),
)

@Serializable
data class CalendarListEntry(
    val id: String,
    val summary: String? = null,
    val primary: Boolean = false,
    val accessRole: String? = null,
)

@Serializable
data class UserInfo(
    val email: String,
    val name: String? = null,
    val picture: String? = null,
)
