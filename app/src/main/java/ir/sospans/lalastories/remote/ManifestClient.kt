package ir.sospans.lalastories.remote

import android.util.Log
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Fetches the per-category static manifest files from the CDN and keeps the last
 * successfully-fetched copy on disk so category metadata (title, cover URL, version)
 * is still available offline even for items that were never opened/downloaded.
 */
class ManifestClient(private val cacheDir: File) {

    private val gson = Gson()

    private val client = OkHttpClient.Builder()
        .connectTimeout(RemoteConfig.CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(RemoteConfig.READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    fun refreshStoryManifest(): RemoteStoryManifest? =
        refresh(RemoteConfig.STORIES_MANIFEST_URL, RemoteConfig.STORIES_MANIFEST_CACHE_FILE, RemoteStoryManifest::class.java)

    fun refreshPoemManifest(): RemotePoemManifest? =
        refresh(RemoteConfig.POEMS_MANIFEST_URL, RemoteConfig.POEMS_MANIFEST_CACHE_FILE, RemotePoemManifest::class.java)

    fun refreshLullabyManifest(): RemoteLullabyManifest? =
        refresh(RemoteConfig.LULLABIES_MANIFEST_URL, RemoteConfig.LULLABIES_MANIFEST_CACHE_FILE, RemoteLullabyManifest::class.java)

    fun refreshInteractiveStoriesManifest(): RemoteInteractiveStoryManifest? =
        refresh(RemoteConfig.INTERACTIVE_STORIES_MANIFEST_URL, RemoteConfig.INTERACTIVE_STORIES_MANIFEST_CACHE_FILE, RemoteInteractiveStoryManifest::class.java)

    fun cachedStoryManifest(): RemoteStoryManifest? =
        readCached(RemoteConfig.STORIES_MANIFEST_CACHE_FILE, RemoteStoryManifest::class.java)

    fun cachedPoemManifest(): RemotePoemManifest? =
        readCached(RemoteConfig.POEMS_MANIFEST_CACHE_FILE, RemotePoemManifest::class.java)

    fun cachedLullabyManifest(): RemoteLullabyManifest? =
        readCached(RemoteConfig.LULLABIES_MANIFEST_CACHE_FILE, RemoteLullabyManifest::class.java)

    fun cachedInteractiveStoriesManifest(): RemoteInteractiveStoryManifest? =
        readCached(RemoteConfig.INTERACTIVE_STORIES_MANIFEST_CACHE_FILE, RemoteInteractiveStoryManifest::class.java)

    private fun <T> refresh(url: String, cacheFileName: String, type: Class<T>): T? {
        return try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w("ManifestClient", "refresh($url) HTTP ${response.code}")
                    return null
                }
                val body = response.body?.string() ?: return null
                val parsed = gson.fromJson(body, type) ?: return null
                cacheDir.mkdirs()
                File(cacheDir, cacheFileName).writeText(body)
                parsed
            }
        } catch (e: Exception) {
            Log.w("ManifestClient", "refresh($url) failed", e)
            null
        }
    }

    private fun <T> readCached(cacheFileName: String, type: Class<T>): T? {
        val file = File(cacheDir, cacheFileName)
        if (!file.exists()) return null
        return try {
            gson.fromJson(file.readText(), type)
        } catch (e: Exception) {
            null
        }
    }
}
