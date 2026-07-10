package ir.sospans.lalastories.repository

import com.google.gson.Gson
import ir.sospans.lalastories.model.Poem
import ir.sospans.lalastories.remote.ContentCacheIndex
import ir.sospans.lalastories.remote.ContentDownloader
import ir.sospans.lalastories.remote.ManifestClient
import ir.sospans.lalastories.remote.RemotePoemItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class PoemRepository(
    private val poemsDir: File,
    private val remotePoemsDir: File? = null,
    private val manifestClient: ManifestClient? = null,
    private val cacheIndex: ContentCacheIndex? = null,
    private val downloader: ContentDownloader? = null
) {

    private val gson = Gson()

    fun loadPoems(): List<Poem> {
        val bundled = parseDir(poemsDir)
        val remoteCached = parseDir(remotePoemsDir).associateBy { it.id }
        val bundledIds = bundled.map { it.id }.toSet()

        val merged = bundled.map { poem -> remoteCached[poem.id] ?: poem }

        val manifestItems = manifestClient?.cachedPoemManifest()?.items ?: emptyList()
        val remoteOnly = manifestItems
            .filter { it.id !in bundledIds }
            .map { item -> remoteCached[item.id] ?: placeholder(item) }

        return merged + remoteOnly
    }

    suspend fun ensurePoemDownloaded(id: String): Boolean = withContext(Dispatchers.IO) {
        val item = manifestClient?.cachedPoemManifest()?.items?.find { it.id == id }
            ?: return@withContext true

        val key = "poems/$id"
        val now = System.currentTimeMillis()
        val index = cacheIndex ?: return@withContext true

        if (!index.needsRedownload(key, item.version)) {
            if (index.isStale(key, now)) index.refreshDownloadedAt(key, now) else index.touch(key, now)
            return@withContext true
        }

        downloader?.downloadPoem(item, now) ?: false
    }

    private fun parseDir(dir: File?): List<Poem> {
        if (dir == null || !dir.exists()) return emptyList()
        return dir.listFiles()
            ?.filter { it.isDirectory && !it.name.startsWith("tmp-") }
            ?.mapNotNull { parsePoemDir(it) }
            ?: emptyList()
    }

    private fun parsePoemDir(dir: File): Poem? {
        val jsonFile = File(dir, "poem.json").takeIf { it.exists() } ?: return null
        return try {
            val raw = gson.fromJson(jsonFile.readText(), Poem::class.java)
            raw.copy(
                imagePath = raw.image?.let { File(dir, it).takeIf { f -> f.exists() }?.absolutePath }
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun placeholder(item: RemotePoemItem): Poem = Poem(
        id = item.id,
        title = item.title,
        text = item.text,
        image = item.image,
        imagePath = item.image,
        isRemotePending = true
    )
}
