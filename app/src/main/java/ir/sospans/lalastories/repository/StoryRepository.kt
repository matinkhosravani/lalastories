package ir.sospans.lalastories.repository

import com.google.gson.Gson
import ir.sospans.lalastories.model.Page
import ir.sospans.lalastories.model.Story
import ir.sospans.lalastories.remote.ContentCacheIndex
import ir.sospans.lalastories.remote.ContentDownloader
import ir.sospans.lalastories.remote.ManifestClient
import ir.sospans.lalastories.remote.RemoteStoryItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class StoryRepository(
    private val storiesDir: File,
    private val remoteStoriesDir: File? = null,
    private val manifestClient: ManifestClient? = null,
    private val cacheIndex: ContentCacheIndex? = null,
    private val downloader: ContentDownloader? = null
) {

    private val gson = Gson()

    /**
     * Bundled stories, with any remote-cache download overriding the bundled copy by id,
     * followed by remote-only stories (not shipped in the app) appended in manifest order.
     * Remote items that exist in the manifest but haven't been downloaded yet appear as
     * placeholders (isRemotePending = true) so they still show up in the list while offline.
     */
    fun loadStories(): List<Story> {
        val bundled = parseDir(storiesDir)
        val remoteCached = parseDir(remoteStoriesDir).associateBy { it.id }
        val bundledIds = bundled.map { it.id }.toSet()

        val merged = bundled.map { story -> remoteCached[story.id] ?: story }

        val manifestItems = manifestClient?.cachedStoryManifest()?.items ?: emptyList()
        val remoteOnly = manifestItems
            .filter { it.id !in bundledIds }
            .map { item -> remoteCached[item.id] ?: placeholder(item) }

        return merged + remoteOnly
    }

    /**
     * Ensures the given story's full content (all pages, images, audio) is available
     * locally. Returns true if it is (already local, or freshly downloaded); false only
     * when the story has no local fallback at all and the download failed (e.g. offline).
     */
    suspend fun ensureStoryDownloaded(id: String): Boolean = withContext(Dispatchers.IO) {
        val item = manifestClient?.cachedStoryManifest()?.items?.find { it.id == id }
            ?: return@withContext true // no remote entry for this id, nothing to do

        val key = "stories/$id"
        val now = System.currentTimeMillis()
        val index = cacheIndex ?: return@withContext true

        if (!index.needsRedownload(key, item.version)) {
            if (index.isStale(key, now)) index.refreshDownloadedAt(key, now) else index.touch(key, now)
            return@withContext true
        }

        downloader?.downloadStory(item, now) ?: false
    }

    private fun parseDir(dir: File?): List<Story> {
        if (dir == null || !dir.exists()) return emptyList()
        return dir.listFiles()
            ?.filter { it.isDirectory && !it.name.startsWith("tmp-") }
            ?.mapNotNull { parseStoryDir(it) }
            ?: emptyList()
    }

    private fun parseStoryDir(dir: File): Story? {
        val jsonFile = File(dir, "story.json").takeIf { it.exists() } ?: return null
        return try {
            val raw = gson.fromJson(jsonFile.readText(), Story::class.java)
            raw.copy(
                coverPath = listOf("cover.jpg", "cover.png").map { File(dir, it) }.firstOrNull { it.exists() }?.absolutePath,
                audioPath = File(dir, "voice.mp3").takeIf { it.exists() }?.absolutePath,
                pages = raw.pages.map { page ->
                    page.copy(
                        imagePath = page.image?.let { File(dir, it).takeIf { f -> f.exists() }?.absolutePath }
                    )
                }
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Shown before the atomic download finishes (or if it fails while offline): text and
     * cover/page images load directly from the manifest's URLs (Coil/MediaPlayer accept a
     * URL the same way they accept a local path) so the story is readable immediately
     * rather than blocked behind a spinner. isRemotePending just tells the screen to keep
     * trying the background download for guaranteed future offline access.
     */
    private fun placeholder(item: RemoteStoryItem): Story = Story(
        id = item.id,
        title = item.title,
        description = item.description,
        ageMin = item.ageMin,
        ageMax = item.ageMax,
        pages = item.pages.map { page -> Page(page.pageNumber, page.text, image = page.image, imagePath = page.image) },
        coverPath = item.cover,
        audioPath = item.voice,
        isRemotePending = true
    )
}
