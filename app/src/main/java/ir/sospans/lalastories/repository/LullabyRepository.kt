package ir.sospans.lalastories.repository

import com.google.gson.Gson
import ir.sospans.lalastories.model.Lullaby
import ir.sospans.lalastories.remote.ContentCacheIndex
import ir.sospans.lalastories.remote.ContentDownloader
import ir.sospans.lalastories.remote.ManifestClient
import ir.sospans.lalastories.remote.RemoteLullabyItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class LullabyRepository(
    private val lullabiesDir: File,
    private val remoteLullabiesDir: File? = null,
    private val manifestClient: ManifestClient? = null,
    private val cacheIndex: ContentCacheIndex? = null,
    private val downloader: ContentDownloader? = null
) {

    private val gson = Gson()

    fun loadLullabies(): List<Lullaby> {
        val bundled = parseDir(lullabiesDir)
        val remoteCached = parseDir(remoteLullabiesDir).associateBy { it.id }
        val bundledIds = bundled.map { it.id }.toSet()

        val merged = bundled.map { lullaby -> remoteCached[lullaby.id] ?: lullaby }

        val manifestItems = manifestClient?.cachedLullabyManifest()?.items ?: emptyList()
        val remoteOnly = manifestItems
            .filter { it.id !in bundledIds }
            .map { item -> remoteCached[item.id] ?: placeholder(item) }

        return merged + remoteOnly
    }

    suspend fun ensureLullabyDownloaded(id: String): Boolean = withContext(Dispatchers.IO) {
        val item = manifestClient?.cachedLullabyManifest()?.items?.find { it.id == id }
            ?: return@withContext true

        val key = "lullabies/$id"
        val now = System.currentTimeMillis()
        val index = cacheIndex ?: return@withContext true

        if (!index.needsRedownload(key, item.version)) {
            if (index.isStale(key, now)) index.refreshDownloadedAt(key, now) else index.touch(key, now)
            return@withContext true
        }

        downloader?.downloadLullaby(item, now) ?: false
    }

    private fun parseDir(dir: File?): List<Lullaby> {
        if (dir == null || !dir.exists()) return emptyList()
        return dir.listFiles()
            ?.filter { it.isDirectory && !it.name.startsWith("tmp-") }
            ?.mapNotNull { parseLullabyDir(it) }
            ?: emptyList()
    }

    private fun parseLullabyDir(dir: File): Lullaby? {
        val jsonFile = File(dir, "lullaby.json").takeIf { it.exists() } ?: return null
        return try {
            val raw = gson.fromJson(jsonFile.readText(), Lullaby::class.java)
            raw.copy(
                imagePath = raw.image?.let { File(dir, it).takeIf { f -> f.exists() }?.absolutePath },
                audioPath = raw.audio?.let { File(dir, it).takeIf { f -> f.exists() }?.absolutePath }
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun placeholder(item: RemoteLullabyItem): Lullaby = Lullaby(
        id = item.id,
        title = item.title,
        text = item.text,
        image = item.image,
        imagePath = item.image,
        audio = item.audio,
        audioPath = item.audio,
        isRemotePending = true
    )
}
