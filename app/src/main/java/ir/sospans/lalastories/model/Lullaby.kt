package ir.sospans.lalastories.model

data class Lullaby(
    val id: String,
    val title: String,
    val text: String,
    val image: String? = null,
    val imagePath: String? = null,
    val audio: String? = null,
    val audioPath: String? = null,
    val isRemotePending: Boolean = false
)
