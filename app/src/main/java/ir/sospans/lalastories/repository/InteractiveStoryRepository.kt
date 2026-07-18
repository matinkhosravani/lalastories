package ir.sospans.lalastories.repository

import com.google.gson.Gson
import ir.sospans.lalastories.model.ChoiceOption
import ir.sospans.lalastories.model.InteractiveStory
import ir.sospans.lalastories.model.QuizAnswer
import ir.sospans.lalastories.model.StoryNode
import ir.sospans.lalastories.remote.ContentCacheIndex
import ir.sospans.lalastories.remote.ContentDownloader
import ir.sospans.lalastories.remote.ManifestClient
import ir.sospans.lalastories.remote.RemoteInteractiveNode
import ir.sospans.lalastories.remote.RemoteInteractiveStoryItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class InteractiveStoryRepository(
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
     * placeholders built directly from the manifest so they still show up in the list while
     * offline.
     */
    fun loadInteractiveStories(): List<InteractiveStory> {
        val bundled = parseDir(storiesDir)
        val remoteCached = parseDir(remoteStoriesDir).associateBy { it.id }
        val bundledIds = bundled.map { it.id }.toSet()

        val merged = bundled.map { story -> remoteCached[story.id] ?: story }

        val manifestItems = manifestClient?.cachedInteractiveStoriesManifest()?.items ?: emptyList()
        val remoteOnly = manifestItems
            .filter { it.id !in bundledIds }
            .mapNotNull { item -> remoteCached[item.id] ?: placeholder(item) }

        return merged + remoteOnly
    }

    /**
     * Ensures the given interactive story's full content (all node images, cover) is
     * available locally. Returns true if it is (already local, or freshly downloaded);
     * false only when the story has no local fallback at all and the download failed
     * (e.g. offline).
     */
    suspend fun ensureInteractiveStoryDownloaded(id: String): Boolean = withContext(Dispatchers.IO) {
        val item = manifestClient?.cachedInteractiveStoriesManifest()?.items?.find { it.id == id }
            ?: return@withContext true // no remote entry for this id, nothing to do

        val key = "interactive-stories/$id"
        val now = System.currentTimeMillis()
        val index = cacheIndex ?: return@withContext true

        if (!index.needsRedownload(key, item.version)) {
            if (index.isStale(key, now)) index.refreshDownloadedAt(key, now) else index.touch(key, now)
            return@withContext true
        }

        downloader?.downloadInteractiveStory(item, now) ?: false
    }

    private fun parseDir(dir: File?): List<InteractiveStory> {
        if (dir == null || !dir.exists()) return emptyList()
        return dir.listFiles()
            ?.filter { it.isDirectory && !it.name.startsWith("tmp-") }
            ?.mapNotNull { parseStoryDir(it) }
            ?: emptyList()
    }

    private fun parseStoryDir(dir: File): InteractiveStory? {
        val jsonFile = File(dir, "story.json").takeIf { it.exists() } ?: return null
        return try {
            val raw = gson.fromJson(jsonFile.readText(), RawInteractiveStory::class.java)
            val nodes = raw.nodes.mapValues { (nodeId, rawNode) -> toStoryNode(nodeId, rawNode, dir) }
            if (!hasValidGraph(raw.startNode, nodes)) return null
            InteractiveStory(
                id = raw.id,
                title = raw.title,
                description = raw.description,
                ageMin = raw.ageMin,
                ageMax = raw.ageMax,
                coverPath = raw.cover?.let { resolveImage(dir, it) },
                startNodeId = raw.startNode,
                nodes = nodes
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun toStoryNode(nodeId: String, raw: RawNode, dir: File): StoryNode = when (raw.type) {
        "content" -> StoryNode.ContentNode(
            id = nodeId,
            text = raw.text.orEmpty(),
            imagePath = raw.image?.let { resolveImage(dir, it) },
            next = raw.next.orEmpty()
        )
        "choice" -> StoryNode.ChoiceNode(
            id = nodeId,
            prompt = raw.prompt.orEmpty(),
            imagePath = raw.image?.let { resolveImage(dir, it) },
            options = raw.options.orEmpty().map { option ->
                ChoiceOption(
                    label = option.label,
                    imagePath = option.image?.let { resolveImage(dir, it) },
                    next = option.next
                )
            }
        )
        "quiz" -> StoryNode.QuizNode(
            id = nodeId,
            question = raw.question.orEmpty(),
            imagePath = raw.image?.let { resolveImage(dir, it) },
            answers = raw.answers.orEmpty().map { answer ->
                QuizAnswer(
                    text = answer.text,
                    imagePath = answer.image?.let { resolveImage(dir, it) },
                    isCorrect = answer.isCorrect
                )
            },
            next = raw.next.orEmpty()
        )
        "end" -> StoryNode.EndNode(
            id = nodeId,
            text = raw.text.orEmpty(),
            imagePath = raw.image?.let { resolveImage(dir, it) }
        )
        else -> throw IllegalArgumentException("Unknown node type: ${raw.type}")
    }

    private fun resolveImage(dir: File, relativePath: String): String? =
        File(dir, relativePath).takeIf { it.exists() }?.absolutePath

    /**
     * Shown before the atomic download finishes (or if it fails while offline): node/option/
     * answer images and cover load directly from the manifest's URLs (Coil accepts a URL the
     * same way it accepts a local path) so the story is playable immediately rather than
     * blocked behind a spinner. This intentionally does not touch the filesystem - it must
     * stay usable purely from cached manifest data.
     */
    private fun placeholder(item: RemoteInteractiveStoryItem): InteractiveStory? {
        val nodes = item.nodes.mapValues { (nodeId, raw) -> toRemoteStoryNode(nodeId, raw) }
        if (!hasValidGraph(item.startNode, nodes)) return null
        return InteractiveStory(
            id = item.id,
            title = item.title,
            description = item.description,
            ageMin = item.ageMin,
            ageMax = item.ageMax,
            coverPath = item.cover,
            startNodeId = item.startNode,
            nodes = nodes
        )
    }

    private fun toRemoteStoryNode(nodeId: String, raw: RemoteInteractiveNode): StoryNode = when (raw.type) {
        "content" -> StoryNode.ContentNode(
            id = nodeId,
            text = raw.text.orEmpty(),
            imagePath = raw.image,
            next = raw.next.orEmpty()
        )
        "choice" -> StoryNode.ChoiceNode(
            id = nodeId,
            prompt = raw.prompt.orEmpty(),
            imagePath = raw.image,
            options = raw.options.orEmpty().map { option ->
                ChoiceOption(label = option.label, imagePath = option.image, next = option.next)
            }
        )
        "quiz" -> StoryNode.QuizNode(
            id = nodeId,
            question = raw.question.orEmpty(),
            imagePath = raw.image,
            answers = raw.answers.orEmpty().map { answer ->
                QuizAnswer(text = answer.text, imagePath = answer.image, isCorrect = answer.isCorrect)
            },
            next = raw.next.orEmpty()
        )
        "end" -> StoryNode.EndNode(id = nodeId, text = raw.text.orEmpty(), imagePath = raw.image)
        else -> throw IllegalArgumentException("Unknown node type: ${raw.type}")
    }

    /**
     * A story only loads if every next/option/answer reference resolves to a real node,
     * and at least one EndNode is reachable from the start - otherwise a kid could get
     * stuck on a dead or looping path with no way to finish.
     */
    private fun hasValidGraph(startNodeId: String, nodes: Map<String, StoryNode>): Boolean {
        if (startNodeId !in nodes) return false

        fun nextIdsOf(node: StoryNode): List<String> = when (node) {
            is StoryNode.ContentNode -> listOf(node.next)
            is StoryNode.ChoiceNode -> node.options.map { it.next }
            is StoryNode.QuizNode -> listOf(node.next)
            is StoryNode.EndNode -> emptyList()
        }

        if (nodes.values.any { node -> nextIdsOf(node).any { it !in nodes } }) return false

        val visited = mutableSetOf<String>()
        val queue = ArrayDeque(listOf(startNodeId))
        var reachedEnd = false
        while (queue.isNotEmpty()) {
            val currentId = queue.removeFirst()
            if (!visited.add(currentId)) continue
            val node = nodes.getValue(currentId)
            if (node is StoryNode.EndNode) reachedEnd = true
            queue.addAll(nextIdsOf(node).filterNot { it in visited })
        }
        return reachedEnd
    }

    private data class RawInteractiveStory(
        val id: String,
        val title: String,
        val description: String,
        val ageMin: Int,
        val ageMax: Int,
        val cover: String? = null,
        val startNode: String,
        val nodes: Map<String, RawNode> = emptyMap()
    )

    private data class RawNode(
        val type: String,
        val text: String? = null,
        val image: String? = null,
        val next: String? = null,
        val prompt: String? = null,
        val options: List<RawChoiceOption>? = null,
        val question: String? = null,
        val answers: List<RawQuizAnswer>? = null
    )

    private data class RawChoiceOption(val label: String, val image: String? = null, val next: String)
    private data class RawQuizAnswer(val text: String? = null, val image: String? = null, val isCorrect: Boolean = false)
}
