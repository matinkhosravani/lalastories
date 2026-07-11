package ir.sospans.lalastories.repository

import ir.sospans.lalastories.model.StoryNode
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

class InteractiveStoryRepositoryTest {

    private lateinit var storiesDir: File

    @Before
    fun setUp() {
        storiesDir = createTempDir("interactive-stories")
    }

    private fun writeStory(id: String, json: String) {
        File(storiesDir, id).mkdirs()
        File(storiesDir, "$id/story.json").writeText(json)
    }

    @Test
    fun `loads a simple linear story ending at an EndNode`() {
        writeStory("linear", """
            {
              "id": "linear", "title": "خطی", "description": "...", "ageMin": 4, "ageMax": 8,
              "startNode": "n1",
              "nodes": {
                "n1": {"type": "content", "text": "شروع", "next": "n2"},
                "n2": {"type": "end", "text": "پایان"}
              }
            }
        """.trimIndent())

        val stories = InteractiveStoryRepository(storiesDir).loadInteractiveStories()
        assertEquals(1, stories.size)
        val story = stories[0]
        assertEquals("n1", story.startNodeId)
        assertTrue(story.nodes["n1"] is StoryNode.ContentNode)
        assertTrue(story.nodes["n2"] is StoryNode.EndNode)
    }

    @Test
    fun `loads a branching story with choice options`() {
        writeStory("branch", """
            {
              "id": "branch", "title": "انشعابی", "description": "...", "ageMin": 4, "ageMax": 8,
              "startNode": "n1",
              "nodes": {
                "n1": {"type": "content", "text": "شروع", "next": "n2"},
                "n2": {"type": "choice", "prompt": "کدام راه؟", "options": [
                  {"label": "راه ۱", "next": "n3"},
                  {"label": "راه ۲", "next": "n4"}
                ]},
                "n3": {"type": "end", "text": "پایان ۱"},
                "n4": {"type": "end", "text": "پایان ۲"}
              }
            }
        """.trimIndent())

        val story = InteractiveStoryRepository(storiesDir).loadInteractiveStories().first()
        val choice = story.nodes.getValue("n2") as StoryNode.ChoiceNode
        assertEquals(2, choice.options.size)
        assertEquals("n3", choice.options[0].next)
        assertEquals("n4", choice.options[1].next)
    }

    @Test
    fun `loads a quiz node with correct and incorrect answers`() {
        writeStory("quiz", """
            {
              "id": "quiz", "title": "آزمون", "description": "...", "ageMin": 4, "ageMax": 8,
              "startNode": "n1",
              "nodes": {
                "n1": {"type": "quiz", "question": "دو به‌علاوه‌ی دو؟", "answers": [
                  {"text": "چهار", "isCorrect": true},
                  {"text": "پنج", "isCorrect": false}
                ], "next": "n2"},
                "n2": {"type": "end", "text": "پایان"}
              }
            }
        """.trimIndent())

        val story = InteractiveStoryRepository(storiesDir).loadInteractiveStories().first()
        val quiz = story.nodes.getValue("n1") as StoryNode.QuizNode
        assertEquals(2, quiz.answers.size)
        assertTrue(quiz.answers.first { it.text == "چهار" }.isCorrect)
        assertFalse(quiz.answers.first { it.text == "پنج" }.isCorrect)
    }

    @Test
    fun `resolves imagePath and coverPath when referenced files exist`() {
        writeStory("with-image", """
            {
              "id": "with-image", "title": "با عکس", "description": "...", "ageMin": 4, "ageMax": 8,
              "cover": "cover.jpg",
              "startNode": "n1",
              "nodes": {
                "n1": {"type": "content", "text": "شروع", "image": "n1.jpg", "next": "n2"},
                "n2": {"type": "end", "text": "پایان"}
              }
            }
        """.trimIndent())
        File(storiesDir, "with-image/cover.jpg").createNewFile()
        File(storiesDir, "with-image/n1.jpg").createNewFile()

        val story = InteractiveStoryRepository(storiesDir).loadInteractiveStories().first()
        assertNotNull(story.coverPath)
        val content = story.nodes.getValue("n1") as StoryNode.ContentNode
        assertNotNull(content.imagePath)
    }

    @Test
    fun `skips story missing story json`() {
        File(storiesDir, "empty-folder").mkdirs()
        assertTrue(InteractiveStoryRepository(storiesDir).loadInteractiveStories().isEmpty())
    }

    @Test
    fun `skips story with dangling next reference`() {
        writeStory("dangling", """
            {
              "id": "dangling", "title": "خراب", "description": "...", "ageMin": 4, "ageMax": 8,
              "startNode": "n1",
              "nodes": {
                "n1": {"type": "content", "text": "شروع", "next": "does-not-exist"}
              }
            }
        """.trimIndent())
        assertTrue(InteractiveStoryRepository(storiesDir).loadInteractiveStories().isEmpty())
    }

    @Test
    fun `skips story with a cycle that never reaches an EndNode`() {
        writeStory("cycle", """
            {
              "id": "cycle", "title": "حلقه", "description": "...", "ageMin": 4, "ageMax": 8,
              "startNode": "n1",
              "nodes": {
                "n1": {"type": "content", "text": "شروع", "next": "n2"},
                "n2": {"type": "content", "text": "ادامه", "next": "n1"}
              }
            }
        """.trimIndent())
        assertTrue(InteractiveStoryRepository(storiesDir).loadInteractiveStories().isEmpty())
    }

    @Test
    fun `returns empty list when stories dir does not exist`() {
        val missingDir = File(storiesDir, "does-not-exist")
        assertTrue(InteractiveStoryRepository(missingDir).loadInteractiveStories().isEmpty())
    }
}
