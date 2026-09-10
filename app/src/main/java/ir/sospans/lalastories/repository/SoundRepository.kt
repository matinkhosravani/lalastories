package ir.sospans.lalastories.repository

import com.google.gson.Gson
import ir.sospans.lalastories.model.CalmSound
import java.io.File

/**
 * Calm white-noise sounds (fan, hair dryer, vacuum) shipped as a fixed bundled set.
 * No remote manifest / downloader machinery - the whole catalogue lives in assets.
 */
class SoundRepository(private val soundsDir: File) {

    private val gson = Gson()

    fun loadSounds(): List<CalmSound> {
        if (!soundsDir.exists()) return emptyList()
        return soundsDir.listFiles()
            ?.filter { it.isDirectory }
            ?.mapNotNull { parseSoundDir(it) }
            ?.sortedBy { it.id }
            ?: emptyList()
    }

    private fun parseSoundDir(dir: File): CalmSound? {
        val jsonFile = File(dir, "sound.json").takeIf { it.exists() } ?: return null
        return try {
            val raw = gson.fromJson(jsonFile.readText(), CalmSound::class.java)
            raw.copy(
                audioPath = raw.audio?.let { File(dir, it).takeIf { f -> f.exists() }?.absolutePath }
            )
        } catch (e: Exception) {
            null
        }
    }
}
