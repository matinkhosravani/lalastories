package ir.sospans.lalastories.repository

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

class PoemRepositoryTest {

    private lateinit var poemsDir: File

    @Before
    fun setUp() {
        poemsDir = createTempDir("poems")
        val poemDir = File(poemsDir, "spring-rain").also { it.mkdirs() }
        poemDir.resolve("poem.json").writeText("""
            {
              "id": "spring-rain",
              "title": "باران بهاری",
              "text": "باران می‌آید نرم نرم\nروی برگ‌های سبز و گرم"
            }
        """.trimIndent())
    }

    @Test
    fun `loadPoems returns poem parsed from json`() {
        val repo = PoemRepository(poemsDir)
        val poems = repo.loadPoems()
        assertEquals(1, poems.size)
        assertEquals("spring-rain", poems[0].id)
        assertEquals("باران بهاری", poems[0].title)
        assertTrue(poems[0].text.contains("باران"))
        assertNull(poems[0].imagePath)
    }

    @Test
    fun `loadPoems sets imagePath when referenced image file exists`() {
        val poemDir = File(poemsDir, "spring-rain")
        poemDir.resolve("poem.json").writeText("""
            {
              "id": "spring-rain",
              "title": "باران بهاری",
              "text": "باران می‌آید نرم نرم",
              "image": "cover.jpg"
            }
        """.trimIndent())
        poemDir.resolve("cover.jpg").createNewFile()

        val repo = PoemRepository(poemsDir)
        val poems = repo.loadPoems()
        assertNotNull(poems[0].imagePath)
        assertTrue(poems[0].imagePath!!.endsWith("cover.jpg"))
    }

    @Test
    fun `loadPoems skips folder with no poem json`() {
        File(poemsDir, "empty-folder").mkdirs()
        val repo = PoemRepository(poemsDir)
        val poems = repo.loadPoems()
        assertEquals(1, poems.size)
    }

    @Test
    fun `loadPoems returns empty list when poems dir does not exist`() {
        val missingDir = File(poemsDir, "does-not-exist")
        val repo = PoemRepository(missingDir)
        assertTrue(repo.loadPoems().isEmpty())
    }
}
