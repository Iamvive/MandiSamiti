package com.appwork.mandisamiti.data.auth

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.security.keystore.KeyPermanentlyInvalidatedException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.KeyStore
import java.security.UnrecoverableKeyException
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

@Serializable
internal data class SessionPayload(val shopId: String, val accessToken: String, val refreshToken: String)

/** Session persisted as AES/GCM ciphertext; the key lives in the AndroidKeyStore. */
class AndroidSessionStore(context: Context) : SessionStore {
    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    override fun current(): Session? = synchronized(LOCK) {
        val stored = prefs.getString(KEY_BLOB, null) ?: return null
        try {
            val raw = Base64.decode(stored, Base64.NO_WRAP)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE, secretKey(),
                GCMParameterSpec(TAG_BITS, raw, 0, IV_BYTES)
            )
            val plain = cipher.doFinal(raw, IV_BYTES, raw.size - IV_BYTES)
            val p = Json.decodeFromString<SessionPayload>(String(plain, Charsets.UTF_8))
            Session(p.shopId, p.accessToken, p.refreshToken)
        } catch (e: Exception) {
            if (e.isPermanent()) clear()
            null
        }
    }

    // BadPaddingException covers AEADBadTagException (and its non-AEAD variants on some OEM keystores).
    private fun Exception.isPermanent() = this is BadPaddingException ||
        this is KeyPermanentlyInvalidatedException ||
        this is IllegalArgumentException ||
        this is SerializationException ||
        this is UnrecoverableKeyException

    override fun save(session: Session) = synchronized(LOCK) {
        val json = Json.encodeToString(
            SessionPayload(session.shopId, session.accessToken, session.refreshToken)
        )
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val encrypted = cipher.doFinal(json.toByteArray(Charsets.UTF_8))
        prefs.edit()
            .putString(KEY_BLOB, Base64.encodeToString(cipher.iv + encrypted, Base64.NO_WRAP))
            .commit()
        Unit
    }

    override fun updateTokens(accessToken: String, refreshToken: String) = synchronized(LOCK) {
        current()?.let { save(it.copy(accessToken = accessToken, refreshToken = refreshToken)) }
        Unit
    }

    override fun clear() = synchronized(LOCK) {
        prefs.edit().clear().commit()
        Unit
    }

    private fun secretKey(): SecretKey = synchronized(LOCK) {
        val ks = KeyStore.getInstance(PROVIDER).apply { load(null) }
        (ks.getKey(ALIAS, null) as? SecretKey)?.let { return@synchronized it }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVIDER)
        gen.init(
            KeyGenParameterSpec.Builder(
                ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        gen.generateKey()
    }

    private companion object {
        val LOCK = Any()
        const val PROVIDER = "AndroidKeyStore"
        const val ALIAS = "mandisamiti_session"
        const val PREFS = "mandisamiti_session"
        const val KEY_BLOB = "blob"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_BYTES = 12
        const val TAG_BITS = 128
    }
}
