package ir.sospans.lalastories.model

data class Poem(
    val id: String,
    val title: String,
    val text: String,
    val image: String? = null,
    val imagePath: String? = null,
    val isRemotePending: Boolean = false
)
