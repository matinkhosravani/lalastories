package com.kidstories.app.repository

import com.google.gson.Gson
import com.kidstories.app.model.Story
import java.io.File

class StoryRepository(private val storiesDir: File) {

    private val gson = Gson()

    fun loadStories(): List<Story> {
        if (!storiesDir.exists()) return emptyList()
        return storiesDir.listFiles()
            ?.filter { it.isDirectory }
            ?.mapNotNull { parseStoryDir(it) }
            ?: emptyList()
    }

    private fun parseStoryDir(dir: File): Story? {
        val jsonFile = File(dir, "story.json").takeIf { it.exists() } ?: return null
        return try {
            val raw = gson.fromJson(jsonFile.readText(), Story::class.java)
            raw.copy(
                coverPath = File(dir, "cover.png").takeIf { it.exists() }?.absolutePath,
                audioPath = File(dir, "voice.mp3").takeIf { it.exists() }?.absolutePath,
                pages = raw.pages.map { page ->
                    page.copy(
                        imagePath = page.image?.let { File(dir, it).takeIf { f -> f.exists() }?.absolutePath }
                    )
                }
            )
        } catch (e: Exception) {
            null
        }
    }
}
