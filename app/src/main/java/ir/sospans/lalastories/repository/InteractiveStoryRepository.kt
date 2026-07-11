package ir.sospans.lalastories.repository

import com.google.gson.Gson
import ir.sospans.lalastories.model.ChoiceOption
import ir.sospans.lalastories.model.InteractiveStory
import ir.sospans.lalastories.model.QuizAnswer
import ir.sospans.lalastories.model.StoryNode
import java.io.File

class InteractiveStoryRepository(private val storiesDir: File) {

    private val gson = Gson()

    fun loadInteractiveStories(): List<InteractiveStory> {
        if (!storiesDir.exists()) return emptyList()
        return storiesDir.listFiles()
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
