package ir.sospans.lalastories.repository

import com.google.gson.Gson
import ir.sospans.lalastories.model.Lullaby
import java.io.File

class LullabyRepository(private val lullabiesDir: File) {

    private val gson = Gson()

    fun loadLullabies(): List<Lullaby> {
        if (!lullabiesDir.exists()) return emptyList()
        return lullabiesDir.listFiles()
            ?.filter { it.isDirectory }
            ?.mapNotNull { parseLullabyDir(it) }
            ?: emptyList()
    }

    private fun parseLullabyDir(dir: File): Lullaby? {
        val jsonFile = File(dir, "lullaby.json").takeIf { it.exists() } ?: return null
        return try {
            val raw = gson.fromJson(jsonFile.readText(), Lullaby::class.java)
            raw.copy(
                imagePath = raw.image?.let { File(dir, it).takeIf { f -> f.exists() }?.absolutePath },
                audioPath = raw.audio?.let { File(dir, it).takeIf { f -> f.exists() }?.absolutePath }
            )
        } catch (e: Exception) {
            null
        }
    }
}
