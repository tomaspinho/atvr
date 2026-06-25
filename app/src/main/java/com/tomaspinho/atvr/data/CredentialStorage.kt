package com.tomaspinho.atvr.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

private val Context.credentialStore by preferencesDataStore(name = "atv_credentials")

/**
 * Single source of truth for pairing credentials. The Python-side
 * `_stored_credentials` dict has been removed; on connect Kotlin reads
 * credentials from here and passes them into `connect_to_device_sync`.
 *
 * Stored entries are encoded as "{identifier}::{protocol}::{credentials}"
 * inside a single StringSet for simplicity. Per-device and bulk clear both
 * operate on this store only -- no Python call is required to clear.
 */
class CredentialStorage(private val context: Context) {

    private data class Entry(val identifier: String, val protocol: String, val credentials: String) {
        fun encode(): String = "$identifier::$protocol::$credentials"
        companion object {
            fun decode(raw: String): Entry? {
                val parts = raw.split("::", limit = 3)
                return if (parts.size == 3) Entry(parts[0], parts[1], parts[2]) else null
            }
        }
    }

    val allCredentials: Flow<Map<String, Map<String, String>>> =
        context.credentialStore.data.map { prefs ->
            prefs[KEY]?.mapNotNull { Entry.decode(it) }
                ?.groupBy { it.identifier }
                ?.mapValues { (_, entries) -> entries.associate { it.protocol to it.credentials } }
                ?: emptyMap()
        }

    fun credentialsFor(identifier: String): Flow<Map<String, String>> =
        allCredentials.map { it[identifier] ?: emptyMap() }

    suspend fun saveCredentials(identifier: String, protocol: String, credentials: String) {
        context.credentialStore.edit { prefs ->
            val current = prefs[KEY]?.toMutableSet() ?: mutableSetOf()
            current.removeAll(current.mapNotNull { Entry.decode(it) }
                .filter { it.identifier == identifier && it.protocol == protocol }
                .map { it.encode() })
            current.add(Entry(identifier, protocol, credentials).encode())
            prefs[KEY] = current
        }
    }

    suspend fun clearCredentials(identifier: String) {
        context.credentialStore.edit { prefs ->
            val current = prefs[KEY] ?: return@edit
            prefs[KEY] = current.filterNot { raw ->
                Entry.decode(raw)?.identifier == identifier
            }.toSet()
        }
    }

    suspend fun clearAllCredentials() {
        context.credentialStore.edit { prefs -> prefs.remove(KEY) }
    }

    /**
     * Synchronous snapshot of stored credentials for [identifier], serialized
     * to a JSON string `{"protocol":"creds"}` suitable to pass into
     * `connect_to_device_sync`. Returns null when no credentials are stored.
     */
    suspend fun snapshotCredentials(identifier: String): String? {
        val map = credentialsFor(identifier).first()
        if (map.isEmpty()) return null
        return Json.encodeToString(
            MapSerializer(String.serializer(), String.serializer()),
            map
        )
    }

    companion object {
        private val KEY = stringSetPreferencesKey("stored_credentials")
    }
}