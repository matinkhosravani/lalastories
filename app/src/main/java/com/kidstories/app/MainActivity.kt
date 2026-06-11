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
        if (prefs.getBoolean("stories_copied", false)) return

        assets.list("stories")?.forEach { storyFolder ->
            val destDir = File(storiesDir, storyFolder).also { it.mkdirs() }
            assets.list("stories/$storyFolder")?.forEach { fileName ->
                val destFile = File(destDir, fileName)
                if (!destFile.exists()) {
                    assets.open("stories/$storyFolder/$fileName").use { input ->
                        destFile.outputStream().use { output -> input.copyTo(output) }
                    }
                }
            }
        }
        prefs.edit().putBoolean("stories_copied", true).apply()
    }
}
