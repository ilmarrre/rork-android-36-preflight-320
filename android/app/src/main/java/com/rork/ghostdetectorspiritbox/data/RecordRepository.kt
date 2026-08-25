package com.rork.ghostdetectorspiritbox.data

import android.content.Context
import android.util.Log
import com.rork.ghostdetectorspiritbox.config.Limits
import com.rork.ghostdetectorspiritbox.domain.SessionRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Local archive of finished sessions, persisted as JSON in app storage.
 * The free tier keeps the most recent [FREE_ARCHIVE_CAPACITY] records.
 */
class RecordRepository private constructor(context: Context) {

    private val file = File(context.applicationContext.filesDir, "records.json")
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _records = MutableStateFlow<List<SessionRecord>>(emptyList())
    val records: StateFlow<List<SessionRecord>> = _records.asStateFlow()

    private val _lastSaveEvicted = MutableStateFlow(false)
    val lastSaveEvicted: StateFlow<Boolean> = _lastSaveEvicted.asStateFlow()

    /** False until the archive has been read from disk once. Gates the bootstrap screen. */
    private val _isLoaded = MutableStateFlow(false)
    val isLoaded: StateFlow<Boolean> = _isLoaded.asStateFlow()

    init {
        scope.launch {
            _records.value = load()
            _isLoaded.value = true
        }
    }

    private suspend fun load(): List<SessionRecord> = withContext(Dispatchers.IO) {
        runCatching {
            if (!file.exists()) return@runCatching emptyList()
            json.decodeFromString<List<SessionRecord>>(file.readText())
        }.onFailure { error ->
            Log.w(TAG, "Archive could not be read, starting empty: ${error.message}")
        }.getOrDefault(emptyList())
    }

    private fun persist(records: List<SessionRecord>) {
        scope.launch {
            runCatching { file.writeText(json.encodeToString(records)) }
                .onFailure { error -> Log.w(TAG, "Archive could not be written: ${error.message}") }
        }
    }

    /** Next session number, continuing from the highest number ever recorded. */
    fun nextSessionNumber(): Int = (_records.value.maxOfOrNull { it.number } ?: 0) + 1

    fun save(record: SessionRecord) {
        val merged = (listOf(record) + _records.value).sortedByDescending { it.startedAtEpochMillis }
        val trimmed = merged.take(FREE_ARCHIVE_CAPACITY)
        _lastSaveEvicted.value = merged.size > trimmed.size
        _records.value = trimmed
        persist(trimmed)
    }

    fun delete(id: String) {
        val next = _records.value.filterNot { it.id == id }
        _records.value = next
        persist(next)
    }

    fun record(id: String): SessionRecord? = _records.value.firstOrNull { it.id == id }

    companion object {
        private const val TAG = "RecordRepository"

        /** Free tier archive capacity, owned by [Limits]. */
        const val FREE_ARCHIVE_CAPACITY: Int = Limits.FREE_ARCHIVE_CAPACITY

        @Volatile
        private var instance: RecordRepository? = null

        fun get(context: Context): RecordRepository =
            instance ?: synchronized(this) {
                instance ?: RecordRepository(context).also { instance = it }
            }
    }
}
