package ir.sospans.lalastories.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import ir.sospans.lalastories.model.StoryProgress
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ProgressRepositoryTest {

    private lateinit var repo: ProgressRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        repo = ProgressRepository(context)
    }

    @Test
    fun `getProgress returns default when no progress saved`() {
        val progress = repo.getProgress("cinderella")
        assertEquals(1, progress.lastPage)
        assertEquals(0L, progress.lastPositionMs)
        assertEquals("read", progress.mode)
    }

    @Test
    fun `saveProgress and getProgress round-trip`() {
        val progress = StoryProgress("cinderella", lastPage = 3, lastPositionMs = 12000L, mode = "listen")
        repo.saveProgress(progress)
        val loaded = repo.getProgress("cinderella")
        assertEquals(3, loaded.lastPage)
        assertEquals(12000L, loaded.lastPositionMs)
        assertEquals("listen", loaded.mode)
    }

    @Test
    fun `resetProgress sets back to defaults`() {
        repo.saveProgress(StoryProgress("cinderella", lastPage = 5, lastPositionMs = 9000L, mode = "listen"))
        repo.resetProgress("cinderella")
        val progress = repo.getProgress("cinderella")
        assertEquals(1, progress.lastPage)
        assertEquals(0L, progress.lastPositionMs)
        assertEquals("read", progress.mode)
    }
}
