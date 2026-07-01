package ir.sospans.lalastories.player

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AudioPlayerTest {

    private lateinit var player: AudioPlayer

    @Before
    fun setUp() {
        player = AudioPlayer()
    }

    @Test
    fun `isPlaying is false initially`() {
        assertFalse(player.isPlaying)
    }

    @Test
    fun `pause sets isPlaying to false`() {
        player.pause()
        assertFalse(player.isPlaying)
    }

    @Test
    fun `getCurrentPositionMs returns 0 when nothing loaded`() {
        assertEquals(0L, player.getCurrentPositionMs())
    }
}
