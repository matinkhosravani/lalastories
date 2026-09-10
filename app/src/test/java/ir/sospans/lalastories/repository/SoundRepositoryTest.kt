package ir.sospans.lalastories.repository

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

class SoundRepositoryTest {

    private lateinit var soundsDir: File

    @Before
    fun setUp() {
        soundsDir = createTempDir("sounds")
        val fanDir = File(soundsDir, "fan").also { it.mkdirs() }
        fanDir.resolve("sound.json").writeText(
            """
            {
              "id": "fan",
              "title": "صدای پنکه",
              "audio": "audio.mp3"
            }
            """.trimIndent()
        )
    }

    @Test
    fun `loadSounds returns sound parsed from json`() {
        val repo = SoundRepository(soundsDir)
        val sounds = repo.loadSounds()
        assertEquals(1, sounds.size)
        assertEquals("fan", sounds[0].id)
        assertEquals("صدای پنکه", sounds[0].title)
        assertNull(sounds[0].audioPath)
    }

    @Test
    fun `loadSounds sets audioPath when referenced file exists`() {
        File(soundsDir, "fan").resolve("audio.mp3").createNewFile()

        val repo = SoundRepository(soundsDir)
        val sounds = repo.loadSounds()
        assertNotNull(sounds[0].audioPath)
        assertTrue(sounds[0].audioPath!!.endsWith("audio.mp3"))
    }

    @Test
    fun `loadSounds returns empty list when directory is missing`() {
        val repo = SoundRepository(File(soundsDir, "does-not-exist"))
        assertTrue(repo.loadSounds().isEmpty())
    }

    @Test
    fun `loadSounds is ordered alphabetically by id for a stable picker`() {
        File(soundsDir, "vacuum").also { it.mkdirs() }.resolve("sound.json").writeText(
            """{ "id": "vacuum", "title": "صدای جاروبرقی" }"""
        )
        File(soundsDir, "hairdryer").also { it.mkdirs() }.resolve("sound.json").writeText(
            """{ "id": "hairdryer", "title": "صدای سشوار" }"""
        )

        val repo = SoundRepository(soundsDir)
        assertEquals(listOf("fan", "hairdryer", "vacuum"), repo.loadSounds().map { it.id })
    }
}
