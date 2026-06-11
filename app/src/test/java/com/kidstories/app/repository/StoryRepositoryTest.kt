package com.kidstories.app.repository

import com.kidstories.app.model.Page
import com.kidstories.app.model.Story
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

class StoryRepositoryTest {

    private lateinit var storiesDir: File

    @Before
    fun setUp() {
        storiesDir = createTempDir("stories")
        val storyDir = File(storiesDir, "cinderella").also { it.mkdirs() }
        storyDir.resolve("story.json").writeText("""
            {
              "id": "cinderella",
              "title": "سیندرلا",
              "description": "داستان سیندرلا",
              "ageMin": 4,
              "ageMax": 10,
              "pages": [
                {"pageNumber": 1, "text": "روزی روزگاری..."},
                {"pageNumber": 2, "text": "سیندرلا..."}
              ]
            }
        """.trimIndent())
    }

    @Test
    fun `loadStories returns story parsed from json`() {
        val repo = StoryRepository(storiesDir)
        val stories = repo.loadStories()
        assertEquals(1, stories.size)
        assertEquals("cinderella", stories[0].id)
        assertEquals("سیندرلا", stories[0].title)
        assertEquals(2, stories[0].pages.size)
    }

    @Test
    fun `loadStories sets coverPath when cover png exists`() {
        val storyDir = File(storiesDir, "cinderella")
        storyDir.resolve("cover.png").createNewFile()
        val repo = StoryRepository(storiesDir)
        val stories = repo.loadStories()
        assertNotNull(stories[0].coverPath)
    }

    @Test
    fun `loadStories sets audioPath when audio mp3 exists`() {
        val storyDir = File(storiesDir, "cinderella")
        storyDir.resolve("audio.mp3").createNewFile()
        val repo = StoryRepository(storiesDir)
        val stories = repo.loadStories()
        assertNotNull(stories[0].audioPath)
    }

    @Test
    fun `loadStories skips folder with no story json`() {
        File(storiesDir, "empty-folder").mkdirs()
        val repo = StoryRepository(storiesDir)
        val stories = repo.loadStories()
        assertEquals(1, stories.size)
    }
}
