package com.appwork.mandisamiti.data.auth

data class Session(val shopId: String, val accessToken: String, val refreshToken: String)

interface SessionStore {
    fun current(): Session?
    fun save(session: Session)
    fun updateTokens(accessToken: String, refreshToken: String)
    fun clear()
}

class InMemorySessionStore : SessionStore {
    private var session: Session? = null

    override fun current(): Session? = session
    override fun save(session: Session) { this.session = session }
    override fun updateTokens(accessToken: String, refreshToken: String) {
        session = session?.copy(accessToken = accessToken, refreshToken = refreshToken)
    }
    override fun clear() { session = null }
}
