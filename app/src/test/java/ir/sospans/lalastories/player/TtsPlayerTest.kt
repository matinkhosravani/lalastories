package ir.sospans.lalastories.player

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TtsPlayerTest {

    private lateinit var context: Context
    private lateinit var player: TtsPlayer

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        player = TtsPlayer(context)
    }

    @Test
    fun `speak sets speaking state to true`() {
        player.speak("سلام")
        assertTrue(player.isSpeaking)
    }

    @Test
    fun `stop sets speaking state to false`() {
        player.speak("سلام")
        player.stop()
        assertFalse(player.isSpeaking)
    }

    @Test
    fun `setSpeed stores speed value`() {
        player.setSpeed(1.5f)
        assertEquals(1.5f, player.speed)
    }
}
