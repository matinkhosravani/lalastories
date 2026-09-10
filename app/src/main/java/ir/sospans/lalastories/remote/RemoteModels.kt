package ir.sospans.lalastories.remote

data class RemoteStoryPage(
    val pageNumber: Int,
    val text: String,
    val image: String? = null
)

data class RemoteStoryItem(
    val id: String,
    val version: Int,
    val title: String,
    val description: String = "",
    val ageMin: Int = 0,
    val ageMax: Int = 0,
    val cover: String? = null,
    val voice: String? = null,
    val pages: List<RemoteStoryPage> = emptyList()
)

data class RemoteStoryManifest(
    val manifestVersion: Int = 0,
    val generatedAt: String? = null,
    val items: List<RemoteStoryItem> = emptyList()
)

data class RemotePoemItem(
    val id: String,
    val version: Int,
    val title: String,
    val image: String? = null,
    val text: String = ""
)

data class RemotePoemManifest(
    val manifestVersion: Int = 0,
    val generatedAt: String? = null,
    val items: List<RemotePoemItem> = emptyList()
)

data class RemoteLullabyItem(
    val id: String,
    val version: Int,
    val title: String,
    val image: String? = null,
    val audio: String? = null,
    val text: String = ""
)

data class RemoteLullabyManifest(
    val manifestVersion: Int = 0,
    val generatedAt: String? = null,
    val items: List<RemoteLullabyItem> = emptyList()
)
