package ir.sospans.lalastories.model

data class InteractiveStoryProgress(
    val storyId: String,
    val currentNodeId: String? = null
)
