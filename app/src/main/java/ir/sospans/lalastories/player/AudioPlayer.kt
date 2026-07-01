package ir.sospans.lalastories.player

import android.media.MediaPlayer

class AudioPlayer {

    var isPlaying: Boolean = false
        private set

    private var mediaPlayer: MediaPlayer? = null

    fun load(filePath: String, startPositionMs: Long = 0L) {
        release()
        mediaPlayer = MediaPlayer().apply {
            setDataSource(filePath)
            prepare()
            seekTo(startPositionMs.toInt())
        }
    }

    fun play() {
        mediaPlayer?.start()
        isPlaying = true
    }

    fun pause() {
        mediaPlayer?.pause()
        isPlaying = false
    }

    fun getCurrentPositionMs(): Long = mediaPlayer?.currentPosition?.toLong() ?: 0L

    fun getDurationMs(): Long = mediaPlayer?.duration?.toLong() ?: 0L

    fun seekTo(positionMs: Long) {
        mediaPlayer?.seekTo(positionMs.toInt())
    }

    fun setOnCompletionListener(listener: () -> Unit) {
        mediaPlayer?.setOnCompletionListener { listener() }
    }

    fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
        isPlaying = false
    }
}
