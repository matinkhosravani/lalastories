package ir.sospans.lalastories.repository

import com.google.gson.Gson
import ir.sospans.lalastories.model.Poem
import java.io.File

class PoemRepository(private val poemsDir: File) {

    private val gson = Gson()

    fun loadPoems(): List<Poem> {
        if (!poemsDir.exists()) return emptyList()
        return poemsDir.listFiles()
            ?.filter { it.isDirectory }
            ?.mapNotNull { parsePoemDir(it) }
            ?: emptyList()
    }

    private fun parsePoemDir(dir: File): Poem? {
        val jsonFile = File(dir, "poem.json").takeIf { it.exists() } ?: return null
        return try {
            val raw = gson.fromJson(jsonFile.readText(), Poem::class.java)
            raw.copy(
                imagePath = raw.image?.let { File(dir, it).takeIf { f -> f.exists() }?.absolutePath }
            )
        } catch (e: Exception) {
            null
        }
    }
}
