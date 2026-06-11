# Kid Stories Android App Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a Farsi RTL Android app that lets Persian kids read or listen to stories loaded from local JSON+MP3 files, with progress persistence and a colorful UI.

**Architecture:** Native Android with Kotlin + Jetpack Compose. Stories are discovered at runtime from the app's external files directory. The bundled Cinderella story is copied from assets on first launch. TTS uses Android's `TextToSpeech` API; pre-recorded audio uses `MediaPlayer`.

**Tech Stack:** Kotlin, Jetpack Compose, Android TTS API, MediaPlayer, SharedPreferences, Gson (JSON parsing), Coil (image loading)

---

## File Map

```
app/
  src/main/
    assets/stories/cinderella/
      story.json
      cover.png
    java/com/kidstories/app/
      MainActivity.kt              — app entry point, copies bundled stories on first launch
      navigation/
        AppNavigation.kt           — NavHost + route definitions
      model/
        Story.kt                   — data classes: Story, Page
        StoryProgress.kt           — data class for saved progress
      repository/
        StoryRepository.kt         — discovers + loads stories from filesystem
        ProgressRepository.kt      — reads/writes SharedPreferences progress
      player/
        TtsPlayer.kt               — wraps TextToSpeech
        AudioPlayer.kt             — wraps MediaPlayer for MP3
      ui/
        home/
          HomeScreen.kt            — story grid
          StoryCard.kt             — single story card composable
        detail/
          StoryDetailScreen.kt     — cover + read/listen buttons
        reading/
          ReadingScreen.kt         — page-by-page text reader
        listening/
          ListeningScreen.kt       — audio playback screen
        settings/
          SettingsScreen.kt        — TTS speed + font size
        theme/
          Theme.kt                 — colors, typography, shapes (RTL)
  src/test/java/com/kidstories/app/
    repository/
      StoryRepositoryTest.kt
      ProgressRepositoryTest.kt
    player/
      TtsPlayerTest.kt
      AudioPlayerTest.kt
```

---

### Task 1: Project Setup

**Files:**
- Modify: `app/build.gradle`
- Create: `app/src/main/res/values/strings.xml`
- Create: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Open `app/build.gradle` and add dependencies**

```groovy
dependencies {
    implementation "androidx.core:core-ktx:1.12.0"
    implementation "androidx.compose.ui:ui:1.6.0"
    implementation "androidx.compose.material3:material3:1.2.0"
    implementation "androidx.navigation:navigation-compose:2.7.6"
    implementation "io.coil-kt:coil-compose:2.5.0"
    implementation "com.google.code.gson:gson:2.10.1"
    testImplementation "junit:junit:4.13.2"
    testImplementation "org.robolectric:robolectric:4.11.1"
    testImplementation "androidx.test:core:1.5.0"
}
```

- [ ] **Step 2: Enable Compose in `app/build.gradle`**

```groovy
android {
    compileSdk 34
    defaultConfig { minSdk 24; targetSdk 34 }
    buildFeatures { compose true }
    composeOptions { kotlinCompilerExtensionVersion "1.5.8" }
}
```

- [ ] **Step 3: Set RTL support in `AndroidManifest.xml`**

```xml
<application
    android:supportsRtl="true"
    android:theme="@style/Theme.KidStories"
    ...>
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
</application>
```

- [ ] **Step 4: Add Farsi app name in `strings.xml`**

```xml
<resources>
    <string name="app_name">قصه‌های کودک</string>
    <string name="btn_read">بخوان</string>
    <string name="btn_listen">گوش بده</string>
    <string name="btn_restart">شروع از اول</string>
    <string name="settings_title">تنظیمات</string>
    <string name="font_size_label">اندازه قلم</string>
    <string name="speed_label">سرعت صدا</string>
</resources>
```

- [ ] **Step 5: Commit**

```bash
git add app/build.gradle app/src/main/AndroidManifest.xml app/src/main/res/values/strings.xml
git commit -m "chore: project setup with Compose, Coil, Gson, RTL"
```

---

### Task 2: Data Models

**Files:**
- Create: `app/src/main/java/com/kidstories/app/model/Story.kt`
- Create: `app/src/main/java/com/kidstories/app/model/StoryProgress.kt`

- [ ] **Step 1: Create `Story.kt`**

```kotlin
package com.kidstories.app.model

data class Page(
    val pageNumber: Int,
    val text: String
)

data class Story(
    val id: String,
    val title: String,
    val description: String,
    val ageMin: Int,
    val ageMax: Int,
    val pages: List<Page>,
    val coverPath: String? = null,   // absolute path to cover.png
    val audioPath: String? = null    // absolute path to audio.mp3
)
```

- [ ] **Step 2: Create `StoryProgress.kt`**

```kotlin
package com.kidstories.app.model

data class StoryProgress(
    val storyId: String,
    val lastPage: Int = 1,
    val lastPositionMs: Long = 0L,
    val mode: String = "read"   // "read" or "listen"
)
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/kidstories/app/model/
git commit -m "feat: add Story and StoryProgress data models"
```

---

### Task 3: StoryRepository

**Files:**
- Create: `app/src/main/java/com/kidstories/app/repository/StoryRepository.kt`
- Create: `app/src/test/java/com/kidstories/app/repository/StoryRepositoryTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
// StoryRepositoryTest.kt
package com.kidstories.app.repository

import com.kidstories.app.model.Story
import com.kidstories.app.model.Page
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

class StoryRepositoryTest {

    private lateinit var storiesDir: File

    @Before
    fun setUp() {
        storiesDir = createTempDir("stories")
        val storyDir = File(storiesDir, "cinderella").also { it.mkdirs() }
        storyDir.resolve("story.json").writeText("""
            {
              "id": "cinderella",
              "title": "سیندرلا",
              "description": "داستان سیندرلا",
              "ageMin": 4,
              "ageMax": 10,
              "pages": [
                {"pageNumber": 1, "text": "روزی روزگاری..."},
                {"pageNumber": 2, "text": "سیندرلا..."}
              ]
            }
        """.trimIndent())
    }

    @Test
    fun `loadStories returns story parsed from json`() {
        val repo = StoryRepository(storiesDir)
        val stories = repo.loadStories()
        assertEquals(1, stories.size)
        assertEquals("cinderella", stories[0].id)
        assertEquals("سیندرلا", stories[0].title)
        assertEquals(2, stories[0].pages.size)
    }

    @Test
    fun `loadStories sets coverPath when cover png exists`() {
        val storyDir = File(storiesDir, "cinderella")
        storyDir.resolve("cover.png").createNewFile()
        val repo = StoryRepository(storiesDir)
        val stories = repo.loadStories()
        assertNotNull(stories[0].coverPath)
    }

    @Test
    fun `loadStories sets audioPath when audio mp3 exists`() {
        val storyDir = File(storiesDir, "cinderella")
        storyDir.resolve("audio.mp3").createNewFile()
        val repo = StoryRepository(storiesDir)
        val stories = repo.loadStories()
        assertNotNull(stories[0].audioPath)
    }

    @Test
    fun `loadStories skips folder with no story json`() {
        File(storiesDir, "empty-folder").mkdirs()
        val repo = StoryRepository(storiesDir)
        val stories = repo.loadStories()
        assertEquals(1, stories.size)
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
./gradlew test --tests "com.kidstories.app.repository.StoryRepositoryTest"
```
Expected: FAIL — `StoryRepository` not defined.

- [ ] **Step 3: Implement `StoryRepository.kt`**

```kotlin
package com.kidstories.app.repository

import com.google.gson.Gson
import com.kidstories.app.model.Story
import java.io.File

class StoryRepository(private val storiesDir: File) {

    private val gson = Gson()

    fun loadStories(): List<Story> {
        if (!storiesDir.exists()) return emptyList()
        return storiesDir.listFiles()
            ?.filter { it.isDirectory }
            ?.mapNotNull { parseStoryDir(it) }
            ?: emptyList()
    }

    private fun parseStoryDir(dir: File): Story? {
        val jsonFile = File(dir, "story.json").takeIf { it.exists() } ?: return null
        return try {
            val raw = gson.fromJson(jsonFile.readText(), Story::class.java)
            raw.copy(
                coverPath = File(dir, "cover.png").takeIf { it.exists() }?.absolutePath,
                audioPath = File(dir, "audio.mp3").takeIf { it.exists() }?.absolutePath
            )
        } catch (e: Exception) {
            null
        }
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
./gradlew test --tests "com.kidstories.app.repository.StoryRepositoryTest"
```
Expected: PASS (4 tests)

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/kidstories/app/repository/StoryRepository.kt \
        app/src/test/java/com/kidstories/app/repository/StoryRepositoryTest.kt
git commit -m "feat: add StoryRepository with filesystem discovery"
```

---

### Task 4: ProgressRepository

**Files:**
- Create: `app/src/main/java/com/kidstories/app/repository/ProgressRepository.kt`
- Create: `app/src/test/java/com/kidstories/app/repository/ProgressRepositoryTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
// ProgressRepositoryTest.kt
package com.kidstories.app.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.kidstories.app.model.StoryProgress
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
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
./gradlew test --tests "com.kidstories.app.repository.ProgressRepositoryTest"
```
Expected: FAIL — `ProgressRepository` not defined.

- [ ] **Step 3: Implement `ProgressRepository.kt`**

```kotlin
package com.kidstories.app.repository

import android.content.Context
import com.kidstories.app.model.StoryProgress

class ProgressRepository(context: Context) {

    private val prefs = context.getSharedPreferences("story_progress", Context.MODE_PRIVATE)

    fun getProgress(storyId: String): StoryProgress = StoryProgress(
        storyId = storyId,
        lastPage = prefs.getInt("${storyId}_page", 1),
        lastPositionMs = prefs.getLong("${storyId}_position_ms", 0L),
        mode = prefs.getString("${storyId}_mode", "read") ?: "read"
    )

    fun saveProgress(progress: StoryProgress) {
        prefs.edit()
            .putInt("${progress.storyId}_page", progress.lastPage)
            .putLong("${progress.storyId}_position_ms", progress.lastPositionMs)
            .putString("${progress.storyId}_mode", progress.mode)
            .apply()
    }

    fun resetProgress(storyId: String) {
        saveProgress(StoryProgress(storyId))
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
./gradlew test --tests "com.kidstories.app.repository.ProgressRepositoryTest"
```
Expected: PASS (3 tests)

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/kidstories/app/repository/ProgressRepository.kt \
        app/src/test/java/com/kidstories/app/repository/ProgressRepositoryTest.kt
git commit -m "feat: add ProgressRepository with SharedPreferences"
```

---

### Task 5: TtsPlayer

**Files:**
- Create: `app/src/main/java/com/kidstories/app/player/TtsPlayer.kt`
- Create: `app/src/test/java/com/kidstories/app/player/TtsPlayerTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
// TtsPlayerTest.kt
package com.kidstories.app.player

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowTextToSpeech

@RunWith(RobolectricTestRunner::class)
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
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
./gradlew test --tests "com.kidstories.app.player.TtsPlayerTest"
```
Expected: FAIL — `TtsPlayer` not defined.

- [ ] **Step 3: Implement `TtsPlayer.kt`**

```kotlin
package com.kidstories.app.player

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class TtsPlayer(context: Context) {

    var isSpeaking: Boolean = false
        private set
    var speed: Float = 1.0f
        private set

    private val tts = TextToSpeech(context) { status ->
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale("fa")
        }
    }

    fun speak(text: String) {
        tts.setSpeechRate(speed)
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        isSpeaking = true
    }

    fun stop() {
        tts.stop()
        isSpeaking = false
    }

    fun setSpeed(newSpeed: Float) {
        speed = newSpeed
    }

    fun shutdown() {
        tts.shutdown()
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
./gradlew test --tests "com.kidstories.app.player.TtsPlayerTest"
```
Expected: PASS (3 tests)

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/kidstories/app/player/TtsPlayer.kt \
        app/src/test/java/com/kidstories/app/player/TtsPlayerTest.kt
git commit -m "feat: add TtsPlayer wrapping Android TextToSpeech"
```

---

### Task 6: AudioPlayer

**Files:**
- Create: `app/src/main/java/com/kidstories/app/player/AudioPlayer.kt`
- Create: `app/src/test/java/com/kidstories/app/player/AudioPlayerTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
// AudioPlayerTest.kt
package com.kidstories.app.player

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
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
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
./gradlew test --tests "com.kidstories.app.player.AudioPlayerTest"
```
Expected: FAIL — `AudioPlayer` not defined.

- [ ] **Step 3: Implement `AudioPlayer.kt`**

```kotlin
package com.kidstories.app.player

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

    fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
        isPlaying = false
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
./gradlew test --tests "com.kidstories.app.player.AudioPlayerTest"
```
Expected: PASS (3 tests)

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/kidstories/app/player/AudioPlayer.kt \
        app/src/test/java/com/kidstories/app/player/AudioPlayerTest.kt
git commit -m "feat: add AudioPlayer wrapping MediaPlayer"
```

---

### Task 7: App Theme (RTL, Colors, Typography)

**Files:**
- Create: `app/src/main/java/com/kidstories/app/ui/theme/Theme.kt`

- [ ] **Step 1: Create `Theme.kt`**

```kotlin
package com.kidstories.app.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp

private val KidColorScheme = lightColorScheme(
    primary = Color(0xFFFF6B6B),
    secondary = Color(0xFF4ECDC4),
    tertiary = Color(0xFFFFE66D),
    background = Color(0xFFFFF9F0),
    surface = Color(0xFFFFFFFF),
    onPrimary = Color.White,
    onBackground = Color(0xFF2D2D2D)
)

private val KidTypography = Typography(
    bodyLarge = androidx.compose.ui.text.TextStyle(fontSize = 20.sp),
    bodyMedium = androidx.compose.ui.text.TextStyle(fontSize = 16.sp),
    titleLarge = androidx.compose.ui.text.TextStyle(fontSize = 28.sp),
    titleMedium = androidx.compose.ui.text.TextStyle(fontSize = 22.sp)
)

@Composable
fun KidStoriesTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = KidColorScheme,
        typography = KidTypography,
        content = content
    )
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/kidstories/app/ui/theme/Theme.kt
git commit -m "feat: add colorful kid-friendly app theme"
```

---

### Task 8: Navigation

**Files:**
- Create: `app/src/main/java/com/kidstories/app/navigation/AppNavigation.kt`

- [ ] **Step 1: Create `AppNavigation.kt`**

```kotlin
package com.kidstories.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kidstories.app.repository.ProgressRepository
import com.kidstories.app.repository.StoryRepository
import com.kidstories.app.ui.detail.StoryDetailScreen
import com.kidstories.app.ui.home.HomeScreen
import com.kidstories.app.ui.listening.ListeningScreen
import com.kidstories.app.ui.reading.ReadingScreen
import com.kidstories.app.ui.settings.SettingsScreen

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Detail : Screen("detail/{storyId}") {
        fun createRoute(storyId: String) = "detail/$storyId"
    }
    object Reading : Screen("reading/{storyId}") {
        fun createRoute(storyId: String) = "reading/$storyId"
    }
    object Listening : Screen("listening/{storyId}") {
        fun createRoute(storyId: String) = "listening/$storyId"
    }
    object Settings : Screen("settings")
}

@Composable
fun AppNavigation(storyRepository: StoryRepository, progressRepository: ProgressRepository) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            HomeScreen(
                stories = storyRepository.loadStories(),
                onStoryClick = { navController.navigate(Screen.Detail.createRoute(it.id)) },
                onSettingsClick = { navController.navigate(Screen.Settings.route) }
            )
        }
        composable(
            Screen.Detail.route,
            arguments = listOf(navArgument("storyId") { type = NavType.StringType })
        ) { backStack ->
            val storyId = backStack.arguments?.getString("storyId")!!
            val story = storyRepository.loadStories().first { it.id == storyId }
            StoryDetailScreen(
                story = story,
                onReadClick = { navController.navigate(Screen.Reading.createRoute(storyId)) },
                onListenClick = { navController.navigate(Screen.Listening.createRoute(storyId)) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            Screen.Reading.route,
            arguments = listOf(navArgument("storyId") { type = NavType.StringType })
        ) { backStack ->
            val storyId = backStack.arguments?.getString("storyId")!!
            val story = storyRepository.loadStories().first { it.id == storyId }
            ReadingScreen(
                story = story,
                progressRepository = progressRepository,
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            Screen.Listening.route,
            arguments = listOf(navArgument("storyId") { type = NavType.StringType })
        ) { backStack ->
            val storyId = backStack.arguments?.getString("storyId")!!
            val story = storyRepository.loadStories().first { it.id == storyId }
            ListeningScreen(
                story = story,
                progressRepository = progressRepository,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/kidstories/app/navigation/AppNavigation.kt
git commit -m "feat: add navigation graph with 5 screens"
```

---

### Task 9: HomeScreen & StoryCard

**Files:**
- Create: `app/src/main/java/com/kidstories/app/ui/home/HomeScreen.kt`
- Create: `app/src/main/java/com/kidstories/app/ui/home/StoryCard.kt`

- [ ] **Step 1: Create `StoryCard.kt`**

```kotlin
package com.kidstories.app.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.kidstories.app.model.Story

@Composable
fun StoryCard(story: Story, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.75f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AsyncImage(
                model = story.coverPath ?: "file:///android_asset/stories/default_cover.png",
                contentDescription = story.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = story.title,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Text(
                text = "${story.ageMin}–${story.ageMax} سال",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
    }
}
```

- [ ] **Step 2: Create `HomeScreen.kt`**

```kotlin
package com.kidstories.app.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kidstories.app.R
import com.kidstories.app.model.Story

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(stories: List<Story>, onStoryClick: (Story) -> Unit, onSettingsClick: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "تنظیمات")
                    }
                }
            )
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(padding)
        ) {
            items(stories) { story ->
                StoryCard(story = story, onClick = { onStoryClick(story) })
            }
        }
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/kidstories/app/ui/home/
git commit -m "feat: add HomeScreen with story grid and StoryCard"
```

---

### Task 10: StoryDetailScreen

**Files:**
- Create: `app/src/main/java/com/kidstories/app/ui/detail/StoryDetailScreen.kt`

- [ ] **Step 1: Create `StoryDetailScreen.kt`**

```kotlin
package com.kidstories.app.ui.detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.kidstories.app.R
import com.kidstories.app.model.Story

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryDetailScreen(story: Story, onReadClick: () -> Unit, onListenClick: () -> Unit, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(story.title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "برگشت")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(24.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AsyncImage(
                model = story.coverPath ?: "file:///android_asset/stories/default_cover.png",
                contentDescription = story.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(story.title, style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text(story.description, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = onReadClick,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text(stringResource(R.string.btn_read), style = MaterialTheme.typography.titleMedium)
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onListenClick,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text(stringResource(R.string.btn_listen), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/kidstories/app/ui/detail/StoryDetailScreen.kt
git commit -m "feat: add StoryDetailScreen with read/listen buttons"
```

---

### Task 11: ReadingScreen

**Files:**
- Create: `app/src/main/java/com/kidstories/app/ui/reading/ReadingScreen.kt`

- [ ] **Step 1: Create `ReadingScreen.kt`**

```kotlin
package com.kidstories.app.ui.reading

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kidstories.app.R
import com.kidstories.app.model.Story
import com.kidstories.app.model.StoryProgress
import com.kidstories.app.repository.ProgressRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingScreen(story: Story, progressRepository: ProgressRepository, onBack: () -> Unit) {
    val savedProgress = remember { progressRepository.getProgress(story.id) }
    var currentPage by remember { mutableIntStateOf(savedProgress.lastPage) }
    val totalPages = story.pages.size

    DisposableEffect(currentPage) {
        onDispose {
            progressRepository.saveProgress(
                StoryProgress(story.id, lastPage = currentPage, mode = "read")
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(story.title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "برگشت")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(24.dp)
                .fillMaxSize()
        ) {
            LinearProgressIndicator(
                progress = { currentPage.toFloat() / totalPages },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = story.pages[currentPage - 1].text,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Right,
                modifier = Modifier.weight(1f).fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        progressRepository.resetProgress(story.id)
                        currentPage = 1
                    }
                ) {
                    Text(stringResource(R.string.btn_restart))
                }
                Row {
                    if (currentPage > 1) {
                        OutlinedButton(onClick = { currentPage-- }) { Text("قبلی") }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    if (currentPage < totalPages) {
                        Button(onClick = { currentPage++ }) { Text("بعدی") }
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/kidstories/app/ui/reading/ReadingScreen.kt
git commit -m "feat: add ReadingScreen with page navigation and progress save"
```

---

### Task 12: ListeningScreen

**Files:**
- Create: `app/src/main/java/com/kidstories/app/ui/listening/ListeningScreen.kt`

- [ ] **Step 1: Create `ListeningScreen.kt`**

```kotlin
package com.kidstories.app.ui.listening

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kidstories.app.model.Story
import com.kidstories.app.model.StoryProgress
import com.kidstories.app.player.AudioPlayer
import com.kidstories.app.player.TtsPlayer
import com.kidstories.app.repository.ProgressRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListeningScreen(story: Story, progressRepository: ProgressRepository, onBack: () -> Unit) {
    val context = LocalContext.current
    val savedProgress = remember { progressRepository.getProgress(story.id) }
    val fullText = remember { story.pages.joinToString("\n\n") { it.text } }

    val ttsPlayer = remember { TtsPlayer(context) }
    val audioPlayer = remember { AudioPlayer() }
    var isPlaying by remember { mutableStateOf(false) }
    val useAudio = story.audioPath != null

    LaunchedEffect(Unit) {
        if (useAudio) {
            audioPlayer.load(story.audioPath!!, savedProgress.lastPositionMs)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            val positionMs = if (useAudio) audioPlayer.getCurrentPositionMs() else 0L
            progressRepository.saveProgress(
                StoryProgress(story.id, lastPositionMs = positionMs, mode = "listen")
            )
            audioPlayer.release()
            ttsPlayer.shutdown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(story.title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "برگشت")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(24.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = fullText,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(
                onClick = {
                    if (isPlaying) {
                        if (useAudio) audioPlayer.pause() else ttsPlayer.stop()
                        isPlaying = false
                    } else {
                        if (useAudio) audioPlayer.play() else ttsPlayer.speak(fullText)
                        isPlaying = true
                    }
                },
                modifier = Modifier.size(72.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "توقف" else "پخش",
                    modifier = Modifier.size(48.dp)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/kidstories/app/ui/listening/ListeningScreen.kt
git commit -m "feat: add ListeningScreen with TTS and MP3 playback"
```

---

### Task 13: SettingsScreen

**Files:**
- Create: `app/src/main/java/com/kidstories/app/ui/settings/SettingsScreen.kt`

- [ ] **Step 1: Create `SettingsScreen.kt`**

```kotlin
package com.kidstories.app.ui.settings

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kidstories.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }
    var ttsSpeed by remember { mutableFloatStateOf(prefs.getFloat("tts_speed", 1.0f)) }
    var fontSize by remember { mutableFloatStateOf(prefs.getFloat("font_size", 20f)) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "برگشت")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(24.dp)
                .fillMaxSize()
        ) {
            Text(stringResource(R.string.speed_label), style = MaterialTheme.typography.titleMedium)
            Slider(
                value = ttsSpeed,
                onValueChange = {
                    ttsSpeed = it
                    prefs.edit().putFloat("tts_speed", it).apply()
                },
                valueRange = 0.5f..2.0f,
                steps = 5
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(stringResource(R.string.font_size_label), style = MaterialTheme.typography.titleMedium)
            Slider(
                value = fontSize,
                onValueChange = {
                    fontSize = it
                    prefs.edit().putFloat("font_size", it).apply()
                },
                valueRange = 14f..32f,
                steps = 5
            )
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/kidstories/app/ui/settings/SettingsScreen.kt
git commit -m "feat: add SettingsScreen with TTS speed and font size sliders"
```

---

### Task 14: MainActivity & First-Launch Story Copy

**Files:**
- Create: `app/src/main/java/com/kidstories/app/MainActivity.kt`
- Create: `app/src/main/assets/stories/cinderella/story.json`

- [ ] **Step 1: Create the bundled Cinderella `story.json` in assets**

```json
{
  "id": "cinderella",
  "title": "سیندرلا",
  "description": "داستان دختری مهربان که با کمک یک پری به جشن شاهزاده می‌رود.",
  "ageMin": 4,
  "ageMax": 10,
  "pages": [
    {"pageNumber": 1, "text": "روزی روزگاری دختری مهربان به نام سیندرلا با نامادری بدجنس و دو خواهرناتنی زندگی می‌کرد."},
    {"pageNumber": 2, "text": "سیندرلا هر روز کارهای خانه را انجام می‌داد، اما هرگز لبخند از لبانش نمی‌رفت."},
    {"pageNumber": 3, "text": "یک روز خبر رسید که شاهزاده جشنی بزرگ ترتیب داده است و همه دختران شهر را دعوت کرده."},
    {"pageNumber": 4, "text": "نامادری اجازه نداد سیندرلا به جشن برود. اما پری مهربان ظاهر شد و لباسی زیبا و کالسکه‌ای از کدو برایش درست کرد."},
    {"pageNumber": 5, "text": "سیندرلا به جشن رفت. شاهزاده شیفته او شد. اما ساعت دوازده شد و سیندرلا فرار کرد. کفش شیشه‌ای‌اش جا ماند."},
    {"pageNumber": 6, "text": "شاهزاده همه شهر را گشت. کفش شیشه‌ای فقط به پای سیندرلا جور بود. آنها با هم ازدواج کردند و خوشبخت زندگی کردند."}
  ]
}
```

- [ ] **Step 2: Create `MainActivity.kt`**

```kotlin
package com.kidstories.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.kidstories.app.navigation.AppNavigation
import com.kidstories.app.repository.ProgressRepository
import com.kidstories.app.repository.StoryRepository
import com.kidstories.app.ui.theme.KidStoriesTheme
import java.io.File

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val storiesDir = File(getExternalFilesDir(null), "stories")
        copyBundledStoriesIfNeeded(storiesDir)

        val storyRepository = StoryRepository(storiesDir)
        val progressRepository = ProgressRepository(this)

        setContent {
            KidStoriesTheme {
                AppNavigation(
                    storyRepository = storyRepository,
                    progressRepository = progressRepository
                )
            }
        }
    }

    private fun copyBundledStoriesIfNeeded(storiesDir: File) {
        val prefs = getSharedPreferences("app_state", MODE_PRIVATE)
        if (prefs.getBoolean("stories_copied", false)) return

        assets.list("stories")?.forEach { storyFolder ->
            val destDir = File(storiesDir, storyFolder).also { it.mkdirs() }
            assets.list("stories/$storyFolder")?.forEach { fileName ->
                val destFile = File(destDir, fileName)
                if (!destFile.exists()) {
                    assets.open("stories/$storyFolder/$fileName").use { input ->
                        destFile.outputStream().use { output -> input.copyTo(output) }
                    }
                }
            }
        }
        prefs.edit().putBoolean("stories_copied", true).apply()
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/kidstories/app/MainActivity.kt \
        app/src/main/assets/stories/cinderella/story.json
git commit -m "feat: add MainActivity with first-launch bundled story copy"
```

---

### Task 15: Full Build & Smoke Test

- [ ] **Step 1: Build the project**

```bash
./gradlew assembleDebug
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 2: Run all unit tests**

```bash
./gradlew test
```
Expected: all tests PASS

- [ ] **Step 3: Install on emulator or device and verify**

```bash
./gradlew installDebug
```

Verify:
1. App opens and shows سیندرلا card on the home screen
2. Tapping the card shows the detail screen with بخوان and گوش بده buttons
3. Tapping بخوان shows page 1 of the Cinderella story in Farsi
4. Page navigation works (بعدی / قبلی)
5. Progress is saved — exit and re-enter; should resume on the same page
6. شروع از اول resets to page 1
7. گوش بده opens the listening screen; pressing play triggers TTS narration in Farsi
8. Settings screen shows speed and font size sliders

- [ ] **Step 4: Final commit**

```bash
git add .
git commit -m "chore: verified full build and smoke test"
```
