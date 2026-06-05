package com.duoplan.app.auth

import android.content.Context
import android.content.Intent
import android.content.IntentSender
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.tasks.await

/** Result of an authorization attempt. */
sealed interface AuthOutcome {
    /** Consent already granted; [token] is a usable OAuth access token. */
    data class Authorized(val token: String) : AuthOutcome

    /** First run (or revoked): launch [intentSender] to collect user consent. */
    data class NeedsConsent(val intentSender: IntentSender) : AuthOutcome
}

/**
 * Wraps Play Services' [com.google.android.gms.auth.api.identity.AuthorizationClient].
 *
 * Once the user grants consent, Play Services caches it and silently returns
 * fresh (auto-refreshed) access tokens — so [blockingToken] can be called from
 * the OkHttp interceptor on every request without showing UI.
 */
class GoogleAuthManager(context: Context) {

    private val client = Identity.getAuthorizationClient(context.applicationContext)

    private val request: AuthorizationRequest =
        AuthorizationRequest.builder()
            .setRequestedScopes(
                listOf(
                    Scope(SCOPE_CALENDAR),
                    Scope(SCOPE_EMAIL),
                    Scope(SCOPE_PROFILE),
                ),
            )
            .build()

    /** Interactive entry point used by the auth screen. */
    suspend fun authorize(): AuthOutcome {
        val result = client.authorize(request).await()
        val pending = result.pendingIntent
        return if (result.hasResolution() && pending != null) {
            AuthOutcome.NeedsConsent(pending.intentSender)
        } else {
            AuthOutcome.Authorized(result.accessToken.orEmpty())
        }
    }

    /** Extract the access token after the consent activity returns. */
    fun tokenFromIntent(data: Intent?): String? =
        data?.let { client.getAuthorizationResultFromIntent(it).accessToken }

    /**
     * Synchronous token fetch for the network interceptor. Returns null when
     * consent has not been granted yet (the request then goes out unauthenticated
     * and the API responds 401, prompting the UI to ask for sign-in).
     */
    fun blockingToken(): String? = try {
        val result = Tasks.await(client.authorize(request))
        if (result.hasResolution()) null else result.accessToken
    } catch (e: Exception) {
        null
    }

    private companion object {
        const val SCOPE_CALENDAR = "https://www.googleapis.com/auth/calendar"
        const val SCOPE_EMAIL = "https://www.googleapis.com/auth/userinfo.email"
        const val SCOPE_PROFILE = "https://www.googleapis.com/auth/userinfo.profile"
    }
}
