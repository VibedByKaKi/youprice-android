package net.kaki87.youprice

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private const val KEY_ALIAS = "youprice_password"
private const val BASE = "https://api.youprice.fr/Vitrine"

class ApiException(val status: Int, message: String) : Exception(message)

/** Token and trusted-device key, kept in app-private storage. The password is stored only when the user ticks "remember", encrypted with an Android Keystore key. */
class Session(context: Context) {
    private val prefs = context.getSharedPreferences("session", Context.MODE_PRIVATE)

    var email: String
        get() = prefs.getString("email", "") ?: ""
        set(v) = prefs.edit().putString("email", v).apply()

    var publicKey: String
        get() = prefs.getString("publicKey", "") ?: ""
        set(v) = prefs.edit().putString("publicKey", v).apply()

    /** Null when no password is saved or the Keystore key can no longer decrypt it. */
    var savedPassword: String?
        get() = prefs.getString("password", null)?.let { runCatching { decrypt(it) }.getOrNull() }
        set(v) {
            if (v == null) prefs.edit().remove("password").apply()
            else prefs.edit().putString("password", encrypt(v)).apply()
        }

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val spec = KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .build()
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            .apply { init(spec) }.generateKey()
    }

    private fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
        val data = cipher.iv + cipher.doFinal(plain.toByteArray())
        return Base64.encodeToString(data, Base64.NO_WRAP)
    }

    private fun decrypt(encoded: String): String {
        val data = Base64.decode(encoded, Base64.NO_WRAP)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            .apply { init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, data, 0, 12)) }
        return String(cipher.doFinal(data, 12, data.size - 12))
    }

    val token: String?
        get() = if (System.currentTimeMillis() < prefs.getLong("expiresAt", 0)) prefs.getString("token", null) else null

    fun saveToken(token: String, expiresInSeconds: Long) {
        prefs.edit().putString("token", token)
            .putLong("expiresAt", System.currentTimeMillis() + (expiresInSeconds - 60) * 1000).apply()
    }

    fun clearToken() = prefs.edit().remove("token").remove("expiresAt").apply()
}

class Api(private val session: Session) {

    /** Returns true when signed in, false when the API asks for the confirmation code. */
    fun login(email: String, password: String): Boolean {
        val body = JSONObject().put("username", email).put("password", password)
            .put("publicKey", session.publicKey)
        val res = json(call("POST", "/Auth/authenticate", body.toString(), "application/json", auth = false))
        session.email = email
        val token = res?.optString("access_token", "").orEmpty()
        if (token.isEmpty()) return false
        session.saveToken(token, res!!.optLong("expires_in", 3600))
        return true
    }

    fun loginWithCode(email: String, password: String, code: String) {
        val body = JSONObject().put("username", email).put("password", password).put("code", code)
        val res = json(call("POST", "/Auth/authenticateWithCode", body.toString(), "application/json", auth = false))
            ?: throw ApiException(0, "Réponse vide")
        session.publicKey = res.optString("publicKey", "")
        session.saveToken(res.getString("access_token"), res.optLong("expires_in", 3600))
    }

    fun get(path: String): Any? = parse(call("GET", path))

    /** The dashboard sends the phone number as a plain-text body on these read endpoints. */
    fun postPhone(path: String, phone: String): Any? =
        parse(call("POST", "$path?phoneNumber=${URLEncoder.encode(phone, "UTF-8")}", phone, "text/plain"))

    private fun call(method: String, path: String, body: String? = null, type: String? = null, auth: Boolean = true): String? {
        val conn = URL(BASE + path).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = method
            conn.connectTimeout = 15_000
            conn.readTimeout = 30_000
            conn.setRequestProperty("Accept", "application/json")
            if (auth) conn.setRequestProperty("Authorization", "Bearer " + (session.token ?: throw ApiException(401, "Session expirée")))
            if (body != null) {
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", type)
                conn.outputStream.use { it.write(body.toByteArray()) }
            }
            val status = conn.responseCode
            val text = (if (status < 400) conn.inputStream else conn.errorStream)?.bufferedReader()?.use { it.readText() }
            if (status >= 400) throw ApiException(status, errorMessage(text) ?: "Erreur $status")
            return text?.takeIf { it.isNotBlank() }
        } finally {
            conn.disconnect()
        }
    }

    private fun errorMessage(text: String?): String? = runCatching {
        val o = JSONObject(text!!)
        o.optString("errorDescription").ifEmpty { o.optString("message") }.ifEmpty { null }
    }.getOrNull()

    private fun parse(text: String?): Any? = text?.let { JSONTokener(it).nextValue() }
    private fun json(text: String?): JSONObject? = parse(text) as? JSONObject
}

fun JSONObject.str(key: String): String = if (isNull(key)) "" else optString(key)
fun JSONObject.obj(key: String): JSONObject? = optJSONObject(key)
fun JSONObject.arr(key: String): JSONArray = optJSONArray(key) ?: JSONArray()
inline fun JSONArray.forEachObject(block: (JSONObject) -> Unit) {
    for (i in 0 until length()) optJSONObject(i)?.let(block)
}
