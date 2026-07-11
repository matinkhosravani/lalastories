package ir.sospans.lalastories.model

data class InteractiveStory(
    val id: String,
    val title: String,
    val description: String,
    val ageMin: Int,
    val ageMax: Int,
    val coverPath: String? = null,
    val startNodeId: String,
    val nodes: Map<String, StoryNode> = emptyMap()
)

sealed class StoryNode {
    abstract val id: String

    data class ContentNode(
        override val id: String,
        val text: String,
        val imagePath: String? = null,
        val next: String
    ) : StoryNode()

    data class ChoiceNode(
        override val id: String,
        val prompt: String,
        val imagePath: String? = null,
        val options: List<ChoiceOption>
    ) : StoryNode()

    data class QuizNode(
        override val id: String,
        val question: String,
        val imagePath: String? = null,
        val answers: List<QuizAnswer>,
        val next: String
    ) : StoryNode()

    data class EndNode(
        override val id: String,
        val text: String,
        val imagePath: String? = null
    ) : StoryNode()
}

data class ChoiceOption(
    val label: String,
    val imagePath: String? = null,
    val next: String
)

data class QuizAnswer(
    val text: String? = null,
    val imagePath: String? = null,
    val isCorrect: Boolean = false
)
