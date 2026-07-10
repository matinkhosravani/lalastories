package ir.sospans.lalastories.remote

object RemoteConfig {
    const val CDN_BASE_URL = "https://cdn-lalastories.baftaloo.ir"

    const val STORIES_MANIFEST_URL = "$CDN_BASE_URL/stories-manifest.json"
    const val POEMS_MANIFEST_URL = "$CDN_BASE_URL/poems-manifest.json"
    const val LULLABIES_MANIFEST_URL = "$CDN_BASE_URL/lullabies-manifest.json"

    const val STORIES_MANIFEST_CACHE_FILE = "stories-manifest.json"
    const val POEMS_MANIFEST_CACHE_FILE = "poems-manifest.json"
    const val LULLABIES_MANIFEST_CACHE_FILE = "lullabies-manifest.json"

    const val MAX_CACHE_BYTES = 200L * 1024 * 1024
    const val STALE_AFTER_MS = 30L * 24 * 60 * 60 * 1000
    const val CONNECT_TIMEOUT_SECONDS = 10L
    const val READ_TIMEOUT_SECONDS = 20L
}
