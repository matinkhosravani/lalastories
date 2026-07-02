package ir.sospans.lalastories.repository

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

class LullabyRepositoryTest {

    private lateinit var lullabiesDir: File

    @Before
    fun setUp() {
        lullabiesDir = createTempDir("lullabies")
        val lullabyDir = File(lullabiesDir, "sleepy-star").also { it.mkdirs() }
        lullabyDir.resolve("lullaby.json").writeText("""
            {
              "id": "sleepy-star",
              "title": "لالایی ستاره خواب‌آلود",
              "text": "بخواب بخواب نازنینم"
            }
        """.trimIndent())
    }

    @Test
    fun `loadLullabies returns lullaby parsed from json`() {
        val repo = LullabyRepository(lullabiesDir)
        val lullabies = repo.loadLullabies()
        assertEquals(1, lullabies.size)
        assertEquals("sleepy-star", lullabies[0].id)
        assertEquals("لالایی ستاره خواب‌آلود", lullabies[0].title)
        assertTrue(lullabies[0].text.contains("بخواب"))
        assertNull(lullabies[0].imagePath)
        assertNull(lullabies[0].audioPath)
    }

    @Test
    fun `loadLullabies sets imagePath and audioPath when referenced files exist`() {
        val lullabyDir = File(lullabiesDir, "sleepy-star")
        lullabyDir.resolve("lullaby.json").writeText("""
            {
              "id": "sleepy-star",
              "title": "لالایی ستاره خواب‌آلود",
              "text": "بخواب بخواب نازنینم",
              "image": "image.jpg",
              "audio": "audio.mp3"
            }
        """.trimIndent())
        lullabyDir.resolve("image.jpg").createNewFile()
        lullabyDir.resolve("audio.mp3").createNewFile()

        val repo = LullabyRepository(lullabiesDir)
        val lullabies = repo.loadLullabies()
        assertNotNull(lullabies[0].imagePath)
        assertTrue(lullabies[0].imagePath!!.endsWith("image.jpg"))
        assertNotNull(lullabies[0].audioPath)
        assertTrue(lullabies[0].audioPath!!.endsWith("audio.mp3"))
    }

    @Test
    fun `loadLullabies skips folder with no lullaby json`() {
        File(lullabiesDir, "empty-folder").mkdirs()
        val repo = LullabyRepository(lullabiesDir)
        val lullabies = repo.loadLullabies()
        assertEquals(1, lullabies.size)
    }

    @Test
    fun `loadLullabies returns empty list when lullabies dir does not exist`() {
        val missingDir = File(lullabiesDir, "does-not-exist")
        val repo = LullabyRepository(missingDir)
        assertTrue(repo.loadLullabies().isEmpty())
    }
}
