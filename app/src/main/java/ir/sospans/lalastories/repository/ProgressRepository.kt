package ir.sospans.lalastories.repository

import android.content.Context
import ir.sospans.lalastories.model.StoryProgress

class ProgressRepository(context: Context) {

    private val prefs = context.getSharedPreferences("story_progress", Context.MODE_PRIVATE)

    fun getProgress(storyId: String): StoryProgress = StoryProgress(
        storyId = storyId,
        lastPage = prefs.getInt("${storyId}_page", 1),
        lastPositionMs = prefs.getLong("${storyId}_position_ms", 0L),
        mode = prefs.getString("${storyId}_mode", "read") ?: "read"
    )

    fun saveProgress(progress: StoryProgress) {
        prefs.edit()
            .putInt("${progress.storyId}_page", progress.lastPage)
            .putLong("${progress.storyId}_position_ms", progress.lastPositionMs)
            .putString("${progress.storyId}_mode", progress.mode)
            .apply()
    }

    fun resetProgress(storyId: String) {
        saveProgress(StoryProgress(storyId))
    }
}
