package ir.sospans.lalastories.remote

import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Downloads one story/poem/lullaby atomically: everything is fetched into a temp
 * directory first, and only renamed into remote-cache/<category>/<id>/ once every
 * asset succeeded. A partial network failure never leaves a half-written item behind,
 * and whatever was cached from a previous successful download stays untouched.
 *
 * The on-disk shape mirrors the bundled assets folders exactly (story.json/poem.json/
 * lullaby.json + images + cover/audio), so the existing StoryRepository/PoemRepository/
 * LullabyRepository parsing code reads remote-cache directories with zero changes.
 */
class ContentDownloader(
    private val remoteRootDir: File,
    private val cacheIndex: ContentCacheIndex
) {
    private val gson = Gson()

    private val client = OkHttpClient.Builder()
        .connectTimeout(RemoteConfig.CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(RemoteConfig.READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    fun downloadStory(item: RemoteStoryItem, now: Long): Boolean {
        return downloadAtomically("stories", item.id, item.version, now) { tempDir ->
            var size = 0L
            val coverExt = item.cover?.let { extensionOf(it, default = "jpg") }
            if (item.cover != null && coverExt != null) {
                size += downloadFile(item.cover, File(tempDir, "cover.$coverExt"))
            }
            item.voice?.let { size += downloadFile(it, File(tempDir, "voice.mp3")) }

            val pages = item.pages.map { page ->
                val relativeImage = page.image?.let { url ->
                    val fileName = "page-${page.pageNumber}.${extensionOf(url, default = "jpg")}"
                    size += downloadFile(url, File(tempDir, "images/$fileName"))
                    "images/$fileName"
                }
                linkedMapOf(
                    "pageNumber" to page.pageNumber,
                    "text" to page.text,
                    "image" to relativeImage
                )
            }
            val json = gson.toJson(
                linkedMapOf(
                    "id" to item.id,
                    "title" to item.title,
                    "description" to item.description,
                    "ageMin" to item.ageMin,
                    "ageMax" to item.ageMax,
                    "pages" to pages
                )
            )
            val jsonFile = File(tempDir, "story.json")
            jsonFile.writeText(json)
            size + jsonFile.length()
        }
    }

    fun downloadPoem(item: RemotePoemItem, now: Long): Boolean {
        return downloadAtomically("poems", item.id, item.version, now) { tempDir ->
            var size = 0L
            val relativeImage = item.image?.let { url ->
                val fileName = "image.${extensionOf(url, default = "jpg")}"
                size += downloadFile(url, File(tempDir, fileName))
                fileName
            }
            val json = gson.toJson(
                linkedMapOf(
                    "id" to item.id,
                    "title" to item.title,
                    "text" to item.text,
                    "image" to relativeImage
                )
            )
            val jsonFile = File(tempDir, "poem.json")
            jsonFile.writeText(json)
            size + jsonFile.length()
        }
    }

    fun downloadLullaby(item: RemoteLullabyItem, now: Long): Boolean {
        return downloadAtomically("lullabies", item.id, item.version, now) { tempDir ->
            var size = 0L
            val relativeImage = item.image?.let { url ->
                val fileName = "image.${extensionOf(url, default = "jpg")}"
                size += downloadFile(url, File(tempDir, fileName))
                fileName
            }
            val relativeAudio = item.audio?.let { url ->
                size += downloadFile(url, File(tempDir, "audio.mp3"))
                "audio.mp3"
            }
            val json = gson.toJson(
                linkedMapOf(
                    "id" to item.id,
                    "title" to item.title,
                    "text" to item.text,
                    "image" to relativeImage,
                    "audio" to relativeAudio
                )
            )
            val jsonFile = File(tempDir, "lullaby.json")
            jsonFile.writeText(json)
            size + jsonFile.length()
        }
    }

    fun downloadInteractiveStory(item: RemoteInteractiveStoryItem, now: Long): Boolean {
        return downloadAtomically("interactive-stories", item.id, item.version, now) { tempDir ->
            var size = 0L
            val relativeCover = item.cover?.let { url ->
                val fileName = "cover.${extensionOf(url, default = "jpg")}"
                size += downloadFile(url, File(tempDir, fileName))
                fileName
            }

            val nodes = item.nodes.mapValues { (nodeId, node) ->
                val relativeImage = node.image?.let { url ->
                    val fileName = "images/$nodeId.${extensionOf(url, default = "jpg")}"
                    size += downloadFile(url, File(tempDir, fileName))
                    fileName
                }
                val relativeOptions = node.options?.mapIndexed { index, option ->
                    val relativeOptionImage = option.image?.let { url ->
                        val fileName = "images/$nodeId-opt-$index.${extensionOf(url, default = "jpg")}"
                        size += downloadFile(url, File(tempDir, fileName))
                        fileName
                    }
                    linkedMapOf(
                        "label" to option.label,
                        "image" to relativeOptionImage,
                        "next" to option.next
                    )
                }
                val relativeAnswers = node.answers?.mapIndexed { index, answer ->
                    val relativeAnswerImage = answer.image?.let { url ->
                        val fileName = "images/$nodeId-ans-$index.${extensionOf(url, default = "jpg")}"
                        size += downloadFile(url, File(tempDir, fileName))
                        fileName
                    }
                    linkedMapOf(
                        "text" to answer.text,
                        "image" to relativeAnswerImage,
                        "isCorrect" to answer.isCorrect
                    )
                }
                linkedMapOf(
                    "type" to node.type,
                    "text" to node.text,
                    "image" to relativeImage,
                    "next" to node.next,
                    "prompt" to node.prompt,
                    "options" to relativeOptions,
                    "question" to node.question,
                    "answers" to relativeAnswers
                )
            }

            val json = gson.toJson(
                linkedMapOf(
                    "id" to item.id,
                    "title" to item.title,
                    "description" to item.description,
                    "ageMin" to item.ageMin,
                    "ageMax" to item.ageMax,
                    "cover" to relativeCover,
                    "startNode" to item.startNode,
                    "nodes" to nodes
                )
            )
            val jsonFile = File(tempDir, "story.json")
            jsonFile.writeText(json)
            size + jsonFile.length()
        }
    }

    private fun downloadAtomically(
        category: String,
        id: String,
        version: Int,
        now: Long,
        writeContent: (tempDir: File) -> Long
    ): Boolean {
        val tempDir = File(remoteRootDir, "tmp-${UUID.randomUUID()}")
        return try {
            tempDir.mkdirs()
            val size = writeContent(tempDir)

            val finalDir = File(File(remoteRootDir, category), id)
            finalDir.deleteRecursively()
            finalDir.parentFile?.mkdirs()
            if (!tempDir.renameTo(finalDir)) throw IllegalStateException("Could not move $tempDir to $finalDir")

            val key = "$category/$id"
            cacheIndex.recordDownload(key, version, size, now)
            cacheIndex.evictLeastRecentlyUsedUntilUnder(RemoteConfig.MAX_CACHE_BYTES) { evictKey ->
                val (evictCategory, evictId) = evictKey.split("/", limit = 2)
                File(File(remoteRootDir, evictCategory), evictId)
            }
            true
        } catch (e: Exception) {
            tempDir.deleteRecursively()
            false
        }
    }

    private fun downloadFile(url: String, dest: File): Long {
        dest.parentFile?.mkdirs()
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IllegalStateException("HTTP ${response.code} for $url")
            val body = response.body ?: throw IllegalStateException("Empty body for $url")
            dest.outputStream().use { out -> body.byteStream().copyTo(out) }
        }
        return dest.length()
    }

    private fun extensionOf(url: String, default: String): String {
        val lastSegment = url.substringAfterLast('/').substringBefore('?')
        val ext = lastSegment.substringAfterLast('.', missingDelimiterValue = "")
        return if (ext.isNotEmpty() && ext.length <= 5) ext else default
    }
}
