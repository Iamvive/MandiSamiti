package com.appwork.mandisamiti.ui.settings

import com.appwork.mandisamiti.data.auth.AuthApi
import com.appwork.mandisamiti.data.auth.LocalDataWiper
import com.appwork.mandisamiti.data.auth.SessionStore
import com.appwork.mandisamiti.data.sync.SyncEngine

sealed interface LogoutResult {
    data class Blocked(val pendingCount: Long) : LogoutResult
    object LoggedOut : LogoutResult
}

/** Hindi message for [LogoutResult.Blocked]. */
fun logoutBlockedMessage(pendingCount: Long): String =
    "$pendingCount प्रविष्टियाँ अभी सर्वर पर नहीं गईं — नेटवर्क मिलने पर लॉगआउट करें"

/**
 * Signs out without losing data: refuses while any local entry is unsynced, otherwise
 * revokes the refresh token (best effort), clears the session, then wipes local rows.
 */
class LogoutUseCase(
    private val syncEngine: SyncEngine,
    private val authApi: AuthApi,
    private val sessionStore: SessionStore,
    private val wiper: LocalDataWiper,
) {
    suspend operator fun invoke(): LogoutResult {
        val pending = syncEngine.getPendingCount()
        if (pending > 0) return LogoutResult.Blocked(pending)

        // Best effort: a network failure must not block, the local token is cleared below anyway.
        sessionStore.current()?.refreshToken?.let { authApi.logout(it) }
        // Session first: no session means no new writes, so a crash before the wipe leaves
        // "no session, stale data" (cleaned on next login) rather than "session, empty DB".
        sessionStore.clear()
        wiper.wipeAll()
        return LogoutResult.LoggedOut
    }
}
