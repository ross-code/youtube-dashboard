package com.duoplan.app.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Google Calendar API v3 (plus the OAuth userinfo endpoint, which lives on the
 * same host). Every call is authenticated by [AuthInterceptor]; the base URL is
 * https://www.googleapis.com/.
 */
interface CalendarApi {

    @GET("oauth2/v3/userinfo")
    suspend fun userInfo(): UserInfo

    @GET("calendar/v3/users/me/calendarList")
    suspend fun calendarList(): CalendarListResponse

    @GET("calendar/v3/calendars/{calendarId}/events")
    suspend fun listEvents(
        @Path("calendarId") calendarId: String,
        // Returns only events DuoPlan created (tagged via extended properties).
        @Query("privateExtendedProperty") tag: String = "duoplan=1",
        @Query("singleEvents") singleEvents: Boolean = true,
        @Query("orderBy") orderBy: String = "updated",
        @Query("timeMin") timeMin: String? = null,
        @Query("maxResults") maxResults: Int = 250,
    ): EventsResponse

    @POST("calendar/v3/calendars/{calendarId}/events")
    suspend fun insertEvent(
        @Path("calendarId") calendarId: String,
        @Body event: EventDto,
    ): EventDto

    @PATCH("calendar/v3/calendars/{calendarId}/events/{eventId}")
    suspend fun patchEvent(
        @Path("calendarId") calendarId: String,
        @Path("eventId") eventId: String,
        @Body event: EventDto,
    ): EventDto

    @DELETE("calendar/v3/calendars/{calendarId}/events/{eventId}")
    suspend fun deleteEvent(
        @Path("calendarId") calendarId: String,
        @Path("eventId") eventId: String,
    ): Response<Unit>
}
