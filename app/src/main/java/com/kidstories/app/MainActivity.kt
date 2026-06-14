package com.kidstories.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.kidstories.app.navigation.AppNavigation
import com.kidstories.app.repository.ProgressRepository
import com.kidstories.app.repository.StoryRepository
import com.kidstories.app.ui.theme.KidStoriesTheme
import java.io.File

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val storiesDir = File(getExternalFilesDir(null), "stories")
        copyBundledStoriesIfNeeded(storiesDir)

        val storyRepository = StoryRepository(storiesDir)
        val progressRepository = ProgressRepository(this)

        setContent {
            KidStoriesTheme {
                AppNavigation(
                    storyRepository = storyRepository,
                    progressRepository = progressRepository
                )
            }
        }
    }

    private fun copyBundledStoriesIfNeeded(storiesDir: File) {
        val prefs = getSharedPreferences("app_state", MODE_PRIVATE)
        if (prefs.getBoolean("stories_copied_v3", false)) return

        copyAssetDir("stories", storiesDir)
        prefs.edit().putBoolean("stories_copied_v3", true).apply()
    }

    private fun copyAssetDir(assetPath: String, destDir: File) {
        destDir.mkdirs()
        assets.list(assetPath)?.forEach { name ->
            val childAsset = "$assetPath/$name"
            val childDest = File(destDir, name)
            val children = assets.list(childAsset)
            if (children != null && children.isNotEmpty()) {
                copyAssetDir(childAsset, childDest)
            } else if (!childDest.exists() || name.endsWith(".json") || name == "cover.png") {
                childDest.delete()
                assets.open(childAsset).use { it.copyTo(childDest.outputStream()) }
            }
        }
    }
}
