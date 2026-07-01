package ir.sospans.lalastories

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.adivery.sdk.Adivery
import ir.sospans.lalastories.navigation.AppNavigation
import ir.sospans.lalastories.repository.PoemRepository
import ir.sospans.lalastories.repository.ProgressRepository
import ir.sospans.lalastories.repository.StoryRepository
import ir.sospans.lalastories.ui.theme.KidStoriesTheme
import java.io.File

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Reopening the app from the launcher/recents while a task already exists can spawn a
        // second MainActivity instance on top of the existing one, so back navigation on the
        // home screen just reveals the older instance instead of exiting the app.
        if (!isTaskRoot && intent?.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_LAUNCHER)) {
            finish()
            return
        }

        Adivery.configure(application, "2c1809b1-b6e4-4305-8757-847a73410a18")

        val storiesDir = File(getExternalFilesDir(null), "stories")
        copyBundledStoriesIfNeeded(storiesDir)

        val poemsDir = File(getExternalFilesDir(null), "poems")
        copyBundledPoemsIfNeeded(poemsDir)

        val storyRepository = StoryRepository(storiesDir)
        val poemRepository = PoemRepository(poemsDir)
        val progressRepository = ProgressRepository(this)

        setContent {
            KidStoriesTheme {
                AppNavigation(
                    storyRepository = storyRepository,
                    poemRepository = poemRepository,
                    progressRepository = progressRepository
                )
            }
        }
    }

    private fun copyBundledStoriesIfNeeded(storiesDir: File) {
        val prefs = getSharedPreferences("app_state", MODE_PRIVATE)
        if (prefs.getBoolean("stories_copied_v15", false)) return

        copyAssetDir("stories", storiesDir)
        prefs.edit().putBoolean("stories_copied_v15", true).apply()
    }

    private fun copyBundledPoemsIfNeeded(poemsDir: File) {
        val prefs = getSharedPreferences("app_state", MODE_PRIVATE)
        if (prefs.getBoolean("poems_copied_v1", false)) return

        copyAssetDir("poems", poemsDir)
        prefs.edit().putBoolean("poems_copied_v1", true).apply()
    }

    private fun copyAssetDir(assetPath: String, destDir: File) {
        destDir.mkdirs()
        assets.list(assetPath)?.forEach { name ->
            val childAsset = "$assetPath/$name"
            val childDest = File(destDir, name)
            val children = assets.list(childAsset)
            if (children != null && children.isNotEmpty()) {
                copyAssetDir(childAsset, childDest)
            } else if (!childDest.exists() || name.endsWith(".json") || name.startsWith("cover.")) {
                childDest.delete()
                assets.open(childAsset).use { it.copyTo(childDest.outputStream()) }
            }
        }
    }
}
