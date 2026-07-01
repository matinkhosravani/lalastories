package ir.sospans.lalastories.model

data class StoryProgress(
    val storyId: String,
    val lastPage: Int = 1,
    val lastPositionMs: Long = 0L,
    val mode: String = "read"
)
