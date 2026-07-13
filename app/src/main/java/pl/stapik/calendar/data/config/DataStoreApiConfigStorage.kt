package pl.stapik.calendar.data.config

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import java.security.GeneralSecurityException
import javax.crypto.AEADBadTagException
import kotlin.text.get

private val Context.apiConfigDataStore by preferencesDataStore(name = "api_config")

class DataStoreApiConfigStorage(
    private val context: Context,
    private val cryptoManager: CryptoManager = CryptoManager()
) : ApiConfigStorage {

    override suspend fun load(): ApiConfig? {
        val prefs = context.apiConfigDataStore.data.first()
        val urlCipher = prefs[KEY_URL_CIPHER] ?: return null
        val urlIv = prefs[KEY_URL_IV] ?: return null
        val keyCipher = prefs[KEY_KEY_CIPHER] ?: return null
        val keyIv = prefs[KEY_KEY_IV] ?: return null

        return try {
            ApiConfig(
                baseUrl = cryptoManager.decrypt(EncryptedPayload(urlCipher, urlIv)),
                apiKey = cryptoManager.decrypt(EncryptedPayload(keyCipher, keyIv))
            )
        } catch (e: GeneralSecurityException) {
            Log.w(TAG, "Failed to decrypt API config", e)
            null
        } catch (e: AEADBadTagException) {
            Log.w(TAG, "Failed to decrypt API config", e)
            null
        }
    }

    override suspend fun save(config: ApiConfig) {
        val encryptedUrl = cryptoManager.encrypt(config.baseUrl)
        val encryptedKey = cryptoManager.encrypt(config.apiKey)

        context.apiConfigDataStore.edit { prefs ->
            prefs[KEY_URL_CIPHER] = encryptedUrl.cipherText
            prefs[KEY_URL_IV] = encryptedUrl.iv
            prefs[KEY_KEY_CIPHER] = encryptedKey.cipherText
            prefs[KEY_KEY_IV] = encryptedKey.iv
        }
    }

    private companion object {
        const val TAG = "DataStoreApiConfigStorage"
        val KEY_URL_CIPHER = stringPreferencesKey("base_url_cipher")
        val KEY_URL_IV = stringPreferencesKey("base_url_iv")
        val KEY_KEY_CIPHER = stringPreferencesKey("api_key_cipher")
        val KEY_KEY_IV = stringPreferencesKey("api_key_iv")
    }
}