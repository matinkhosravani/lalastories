package ir.sospans.lalastories.remote

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

data class CacheEntry(
    val key: String,
    val version: Int,
    val downloadedAt: Long,
    val lastAccessedAt: Long,
    val sizeBytes: Long
)

/**
 * Tracks metadata (version/timestamps/size) for every remote item downloaded into
 * remote-cache, keyed as "<category>/<id>". Backed by a single flat JSON file since
 * the item count here is small (dozens, not thousands).
 */
class ContentCacheIndex(private val indexFile: File) {

    private val gson = Gson()
    private val entryListType = object : TypeToken<List<CacheEntry>>() {}.type

    @Synchronized
    private fun readAll(): MutableMap<String, CacheEntry> {
        if (!indexFile.exists()) return mutableMapOf()
        return try {
            val list: List<CacheEntry>? = gson.fromJson(indexFile.readText(), entryListType)
            (list ?: emptyList()).associateBy { it.key }.toMutableMap()
        } catch (e: Exception) {
            mutableMapOf()
        }
    }

    @Synchronized
    private fun writeAll(entries: Map<String, CacheEntry>) {
        indexFile.parentFile?.mkdirs()
        indexFile.writeText(gson.toJson(entries.values.toList()))
    }

    fun entry(key: String): CacheEntry? = readAll()[key]

    fun needsRedownload(key: String, remoteVersion: Int): Boolean {
        val existing = entry(key) ?: return true
        return existing.version != remoteVersion
    }

    fun isStale(key: String, now: Long): Boolean {
        val existing = entry(key) ?: return true
        return now - existing.downloadedAt > RemoteConfig.STALE_AFTER_MS
    }

    fun touch(key: String, now: Long) {
        val entries = readAll()
        val existing = entries[key] ?: return
        entries[key] = existing.copy(lastAccessedAt = now)
        writeAll(entries)
    }

    /** Staleness check passed and version unchanged: just extend the TTL window. */
    fun refreshDownloadedAt(key: String, now: Long) {
        val entries = readAll()
        val existing = entries[key] ?: return
        entries[key] = existing.copy(downloadedAt = now, lastAccessedAt = now)
        writeAll(entries)
    }

    fun recordDownload(key: String, version: Int, sizeBytes: Long, now: Long) {
        val entries = readAll()
        entries[key] = CacheEntry(key, version, now, now, sizeBytes)
        writeAll(entries)
    }

    fun remove(key: String) {
        val entries = readAll()
        entries.remove(key)
        writeAll(entries)
    }

    /** Evicts least-recently-accessed entries (deleting their directories) until under budget. */
    fun evictLeastRecentlyUsedUntilUnder(budgetBytes: Long, keyToDir: (String) -> File) {
        val entries = readAll()
        var total = entries.values.sumOf { it.sizeBytes }
        if (total <= budgetBytes) return
        val ordered = entries.values.sortedBy { it.lastAccessedAt }
        for (e in ordered) {
            if (total <= budgetBytes) break
            keyToDir(e.key).deleteRecursively()
            entries.remove(e.key)
            total -= e.sizeBytes
        }
        writeAll(entries)
    }
}
