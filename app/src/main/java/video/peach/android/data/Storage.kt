package video.peach.android.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.*
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private val Context.preferences by preferencesDataStore("peach_preferences")

data class ServerProfile(val id: String, val name: String, val url: String, val caPem: String = "") {
    fun json() = JSONObject().put("id", id).put("name", name).put("url", url).put("ca", caPem)
    companion object {
        fun from(json: JSONObject) = ServerProfile(json.getString("id"), json.getString("name"), json.getString("url"), json.optString("ca"))
    }
}

class Preferences(private val context: Context) {
    suspend fun servers(): List<ServerProfile> = runCatching {
        val json = JSONArray(context.preferences.data.first()[stringPreferencesKey("servers")] ?: "[]")
        (0 until json.length()).map { ServerProfile.from(json.getJSONObject(it)) }
    }.getOrDefault(emptyList())
    suspend fun saveServers(servers: List<ServerProfile>) {
        context.preferences.edit { it[stringPreferencesKey("servers")] = JSONArray(servers.map { row -> row.json() }).toString() }
    }
    suspend fun get(key: String, default: String = "") = context.preferences.data.first()[stringPreferencesKey(key)] ?: default
    suspend fun put(key: String, value: String) { context.preferences.edit { it[stringPreferencesKey(key)] = value } }
}

/** 只在应用私有目录保存密文，Keystore 密钥不可导出。 */
interface SessionStorage {
    fun write(name: String, value: String)
    fun read(name: String): String
}
class SecretStore(context: Context) : SessionStorage {
    private val storage = context.getSharedPreferences("encrypted_sessions", Context.MODE_PRIVATE)
    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey("peach-sessions-v1", null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(KeyGenParameterSpec.Builder("peach-sessions-v1", KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
            generateKey()
        }
    }
    @Synchronized override fun write(name: String, value: String) {
        if (value.isEmpty()) { storage.edit().remove(name).commit(); return }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
        val encoded = Base64.encodeToString(cipher.iv + cipher.doFinal(value.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)
        check(storage.edit().putString(name, encoded).commit()) { "无法保存登录会话" }
    }
    @Synchronized override fun read(name: String): String = runCatching {
        val data = Base64.decode(storage.getString(name, null) ?: return "", Base64.NO_WRAP)
        Cipher.getInstance("AES/GCM/NoPadding").run {
            init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, data.copyOfRange(0, 12)))
            String(doFinal(data.copyOfRange(12, data.size)), Charsets.UTF_8)
        }
    }.getOrDefault("")
}

@Entity(primaryKeys = ["server", "key"])
data class CacheRecord(val server: String, val key: String, val payload: String, val updatedAt: Long = System.currentTimeMillis())
@Entity(primaryKeys = ["server", "kind", "asset", "edition"])
data class PlaybackPosition(val server: String, val kind: String, val asset: String, val edition: String, val milliseconds: Long)
@Dao interface PeachDao {
    @Query("SELECT * FROM CacheRecord WHERE server=:server AND `key`=:key") suspend fun cache(server: String, key: String): CacheRecord?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun cache(record: CacheRecord)
    @Query("SELECT * FROM PlaybackPosition WHERE server=:server AND kind=:kind AND asset=:asset AND edition=:edition")
    suspend fun position(server: String, kind: String, asset: String, edition: String): PlaybackPosition?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun position(record: PlaybackPosition)
    @Query("DELETE FROM CacheRecord WHERE server=:server") suspend fun clear(server: String)
}
@Database(entities = [CacheRecord::class, PlaybackPosition::class], version = 1, exportSchema = false)
abstract class PeachDatabase : RoomDatabase() { abstract fun dao(): PeachDao }
