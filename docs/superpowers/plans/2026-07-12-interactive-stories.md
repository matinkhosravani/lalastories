# Interactive Stories Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a new "Interactive Stories" content type — a node-graph story that can branch on kid-made choices and/or ask quiz questions with instant feedback — alongside the existing Stories/Poems/Lullabies, plus two hand-authored example stories (one branching, one quiz) proving it out.

**Architecture:** A new `InteractiveStory` domain model represents a story as a `Map<String, StoryNode>` (`ContentNode`, `ChoiceNode`, `QuizNode`, `EndNode`) plus a `startNodeId`. `InteractiveStoryRepository` folder-scans a bundled `assets/interactive-stories/` directory, parses each `story.json` via a private Gson DTO layer, converts it to the sealed `StoryNode` model, and validates the graph (no dangling references, at least one reachable `EndNode`) before exposing it — invalid stories are silently excluded rather than crashing. A single new `InteractiveStoryPlayerScreen` renders whichever node type is current, swapping views as the kid taps through. This is fully isolated from `Story`/`Poem`/`Lullaby` and their screens — no existing file's *runtime behavior* changes, only `HomeScreen.kt`, `AppNavigation.kt`, and `MainActivity.kt` gain new wiring alongside their existing code.

**Tech Stack:** Kotlin, Jetpack Compose (Material3), Gson, JUnit4 (existing project stack — no new dependencies).

**Design spec:** `docs/superpowers/specs/2026-07-12-interactive-stories-design.md`

## Global Constraints

- No new Gradle dependencies — `com.google.code.gson:gson:2.10.1`, `io.coil-kt:coil-compose:2.5.0`, and `androidx.compose.material:material-icons-extended` are already present and sufficient (`Icons.Default.Extension` is confirmed present in the extended icon set, version 1.6.1, used for this feature's puzzle-piece iconography).
- Follow existing code style: no comments unless explaining a non-obvious constraint; data classes with default `null`/`false` for optional fields; `Gson().fromJson` + manual conversion for resolved file paths (see `StoryRepository.kt`).
- This repo has no Compose UI test setup (`app/src/androidTest` has no real tests) — UI-only tasks are verified by `./gradlew :app:compileDebugKotlin` / `:app:assembleDebug` plus manual on-device walkthrough. Only the repository/model layer gets JUnit tests, matching `StoryRepositoryTest.kt` / `PoemRepositoryTest.kt`.
- All content is Persian (Farsi), RTL. List/grid screens use the existing `CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr)` + `TextAlign.Right` top-bar pattern (see `StoriesScreen.kt`); single-item detail/player screens use a plain `TopAppBar` title with no override (see `StoryDetailScreen.kt`) — match whichever pattern the screen you're copying from actually uses, don't invent a third variant.
- No ads (Adivery) and no audio narration/Listen mode in this feature — both are explicitly out of scope per the design spec.
- **Deviation from the design spec, flagged for the user:** the spec's authoring section calls for AI-generated illustrations per node. This plan ships the two example stories **without any images** instead — `coverPath`/`imagePath` are all `null`, and every screen already renders a graceful icon/text-only fallback when an image is absent (this mirrors how the Poems feature originally shipped six poems with zero images, relying on the same fallback pattern — see `docs/superpowers/plans/2026-07-02-poems-feature.md` Task 3). This keeps the plan deterministic and offline-executable instead of depending on live paid image-generation API calls. Illustrations can be added later by running `scripts/generate_story.py`'s image-generation step and dropping files into each story's folder — no code changes needed. One consequence: neither example story actually demonstrates the image-based ("tap-the-picture") quiz answer format from the spec, even though `QuizAnswer.imagePath` fully supports it in code — both example quizzes are text-answer-only for now.
- This repo's commit hook rejects commits that (a) have a subject line over 50 characters, or (b) include any AI-attribution trailer (e.g. `Co-Authored-By: Claude ...`). Every commit step below is written to satisfy both; keep any deviation the same way.

---

### Task 1: Interactive story model and repository

**Files:**
- Create: `app/src/main/java/ir/sospans/lalastories/model/InteractiveStory.kt`
- Create: `app/src/main/java/ir/sospans/lalastories/repository/InteractiveStoryRepository.kt`
- Test: `app/src/test/java/ir/sospans/lalastories/repository/InteractiveStoryRepositoryTest.kt`

**Interfaces:**
- Produces: `data class InteractiveStory(val id: String, val title: String, val description: String, val ageMin: Int, val ageMax: Int, val coverPath: String? = null, val startNodeId: String, val nodes: Map<String, StoryNode> = emptyMap())`
- Produces: `sealed class StoryNode { abstract val id: String }` with `StoryNode.ContentNode(id, text, imagePath, next)`, `StoryNode.ChoiceNode(id, prompt, imagePath, options: List<ChoiceOption>)`, `StoryNode.QuizNode(id, question, imagePath, answers: List<QuizAnswer>, next)`, `StoryNode.EndNode(id, text, imagePath)`
- Produces: `data class ChoiceOption(val label: String, val imagePath: String? = null, val next: String)`
- Produces: `data class QuizAnswer(val text: String? = null, val imagePath: String? = null, val isCorrect: Boolean = false)`
- Produces: `class InteractiveStoryRepository(storiesDir: File) { fun loadInteractiveStories(): List<InteractiveStory> }` — consumed by every later task.

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/ir/sospans/lalastories/repository/InteractiveStoryRepositoryTest.kt`:

```kotlin
package ir.sospans.lalastories.repository

import ir.sospans.lalastories.model.StoryNode
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

class InteractiveStoryRepositoryTest {

    private lateinit var storiesDir: File

    @Before
    fun setUp() {
        storiesDir = createTempDir("interactive-stories")
    }

    private fun writeStory(id: String, json: String) {
        File(storiesDir, id).mkdirs()
        File(storiesDir, "$id/story.json").writeText(json)
    }

    @Test
    fun `loads a simple linear story ending at an EndNode`() {
        writeStory("linear", """
            {
              "id": "linear", "title": "خطی", "description": "...", "ageMin": 4, "ageMax": 8,
              "startNode": "n1",
              "nodes": {
                "n1": {"type": "content", "text": "شروع", "next": "n2"},
                "n2": {"type": "end", "text": "پایان"}
              }
            }
        """.trimIndent())

        val stories = InteractiveStoryRepository(storiesDir).loadInteractiveStories()
        assertEquals(1, stories.size)
        val story = stories[0]
        assertEquals("n1", story.startNodeId)
        assertTrue(story.nodes["n1"] is StoryNode.ContentNode)
        assertTrue(story.nodes["n2"] is StoryNode.EndNode)
    }

    @Test
    fun `loads a branching story with choice options`() {
        writeStory("branch", """
            {
              "id": "branch", "title": "انشعابی", "description": "...", "ageMin": 4, "ageMax": 8,
              "startNode": "n1",
              "nodes": {
                "n1": {"type": "content", "text": "شروع", "next": "n2"},
                "n2": {"type": "choice", "prompt": "کدام راه؟", "options": [
                  {"label": "راه ۱", "next": "n3"},
                  {"label": "راه ۲", "next": "n4"}
                ]},
                "n3": {"type": "end", "text": "پایان ۱"},
                "n4": {"type": "end", "text": "پایان ۲"}
              }
            }
        """.trimIndent())

        val story = InteractiveStoryRepository(storiesDir).loadInteractiveStories().first()
        val choice = story.nodes.getValue("n2") as StoryNode.ChoiceNode
        assertEquals(2, choice.options.size)
        assertEquals("n3", choice.options[0].next)
        assertEquals("n4", choice.options[1].next)
    }

    @Test
    fun `loads a quiz node with correct and incorrect answers`() {
        writeStory("quiz", """
            {
              "id": "quiz", "title": "آزمون", "description": "...", "ageMin": 4, "ageMax": 8,
              "startNode": "n1",
              "nodes": {
                "n1": {"type": "quiz", "question": "دو به‌علاوه‌ی دو؟", "answers": [
                  {"text": "چهار", "isCorrect": true},
                  {"text": "پنج", "isCorrect": false}
                ], "next": "n2"},
                "n2": {"type": "end", "text": "پایان"}
              }
            }
        """.trimIndent())

        val story = InteractiveStoryRepository(storiesDir).loadInteractiveStories().first()
        val quiz = story.nodes.getValue("n1") as StoryNode.QuizNode
        assertEquals(2, quiz.answers.size)
        assertTrue(quiz.answers.first { it.text == "چهار" }.isCorrect)
        assertFalse(quiz.answers.first { it.text == "پنج" }.isCorrect)
    }

    @Test
    fun `resolves imagePath and coverPath when referenced files exist`() {
        writeStory("with-image", """
            {
              "id": "with-image", "title": "با عکس", "description": "...", "ageMin": 4, "ageMax": 8,
              "cover": "cover.jpg",
              "startNode": "n1",
              "nodes": {
                "n1": {"type": "content", "text": "شروع", "image": "n1.jpg", "next": "n2"},
                "n2": {"type": "end", "text": "پایان"}
              }
            }
        """.trimIndent())
        File(storiesDir, "with-image/cover.jpg").createNewFile()
        File(storiesDir, "with-image/n1.jpg").createNewFile()

        val story = InteractiveStoryRepository(storiesDir).loadInteractiveStories().first()
        assertNotNull(story.coverPath)
        val content = story.nodes.getValue("n1") as StoryNode.ContentNode
        assertNotNull(content.imagePath)
    }

    @Test
    fun `skips story missing story json`() {
        File(storiesDir, "empty-folder").mkdirs()
        assertTrue(InteractiveStoryRepository(storiesDir).loadInteractiveStories().isEmpty())
    }

    @Test
    fun `skips story with dangling next reference`() {
        writeStory("dangling", """
            {
              "id": "dangling", "title": "خراب", "description": "...", "ageMin": 4, "ageMax": 8,
              "startNode": "n1",
              "nodes": {
                "n1": {"type": "content", "text": "شروع", "next": "does-not-exist"}
              }
            }
        """.trimIndent())
        assertTrue(InteractiveStoryRepository(storiesDir).loadInteractiveStories().isEmpty())
    }

    @Test
    fun `skips story with a cycle that never reaches an EndNode`() {
        writeStory("cycle", """
            {
              "id": "cycle", "title": "حلقه", "description": "...", "ageMin": 4, "ageMax": 8,
              "startNode": "n1",
              "nodes": {
                "n1": {"type": "content", "text": "شروع", "next": "n2"},
                "n2": {"type": "content", "text": "ادامه", "next": "n1"}
              }
            }
        """.trimIndent())
        assertTrue(InteractiveStoryRepository(storiesDir).loadInteractiveStories().isEmpty())
    }

    @Test
    fun `returns empty list when stories dir does not exist`() {
        val missingDir = File(storiesDir, "does-not-exist")
        assertTrue(InteractiveStoryRepository(missingDir).loadInteractiveStories().isEmpty())
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "ir.sospans.lalastories.repository.InteractiveStoryRepositoryTest"`
Expected: FAIL — compilation error, `InteractiveStoryRepository`/`StoryNode` unresolved references.

- [ ] **Step 3: Create the domain model**

Create `app/src/main/java/ir/sospans/lalastories/model/InteractiveStory.kt`:

```kotlin
package ir.sospans.lalastories.model

data class InteractiveStory(
    val id: String,
    val title: String,
    val description: String,
    val ageMin: Int,
    val ageMax: Int,
    val coverPath: String? = null,
    val startNodeId: String,
    val nodes: Map<String, StoryNode> = emptyMap()
)

sealed class StoryNode {
    abstract val id: String

    data class ContentNode(
        override val id: String,
        val text: String,
        val imagePath: String? = null,
        val next: String
    ) : StoryNode()

    data class ChoiceNode(
        override val id: String,
        val prompt: String,
        val imagePath: String? = null,
        val options: List<ChoiceOption>
    ) : StoryNode()

    data class QuizNode(
        override val id: String,
        val question: String,
        val imagePath: String? = null,
        val answers: List<QuizAnswer>,
        val next: String
    ) : StoryNode()

    data class EndNode(
        override val id: String,
        val text: String,
        val imagePath: String? = null
    ) : StoryNode()
}

data class ChoiceOption(
    val label: String,
    val imagePath: String? = null,
    val next: String
)

data class QuizAnswer(
    val text: String? = null,
    val imagePath: String? = null,
    val isCorrect: Boolean = false
)
```

- [ ] **Step 4: Create the repository**

Create `app/src/main/java/ir/sospans/lalastories/repository/InteractiveStoryRepository.kt`:

```kotlin
package ir.sospans.lalastories.repository

import com.google.gson.Gson
import ir.sospans.lalastories.model.ChoiceOption
import ir.sospans.lalastories.model.InteractiveStory
import ir.sospans.lalastories.model.QuizAnswer
import ir.sospans.lalastories.model.StoryNode
import java.io.File

class InteractiveStoryRepository(private val storiesDir: File) {

    private val gson = Gson()

    fun loadInteractiveStories(): List<InteractiveStory> {
        if (!storiesDir.exists()) return emptyList()
        return storiesDir.listFiles()
            ?.filter { it.isDirectory && !it.name.startsWith("tmp-") }
            ?.mapNotNull { parseStoryDir(it) }
            ?: emptyList()
    }

    private fun parseStoryDir(dir: File): InteractiveStory? {
        val jsonFile = File(dir, "story.json").takeIf { it.exists() } ?: return null
        return try {
            val raw = gson.fromJson(jsonFile.readText(), RawInteractiveStory::class.java)
            val nodes = raw.nodes.mapValues { (nodeId, rawNode) -> toStoryNode(nodeId, rawNode, dir) }
            if (!hasValidGraph(raw.startNode, nodes)) return null
            InteractiveStory(
                id = raw.id,
                title = raw.title,
                description = raw.description,
                ageMin = raw.ageMin,
                ageMax = raw.ageMax,
                coverPath = raw.cover?.let { resolveImage(dir, it) },
                startNodeId = raw.startNode,
                nodes = nodes
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun toStoryNode(nodeId: String, raw: RawNode, dir: File): StoryNode = when (raw.type) {
        "content" -> StoryNode.ContentNode(
            id = nodeId,
            text = raw.text.orEmpty(),
            imagePath = raw.image?.let { resolveImage(dir, it) },
            next = raw.next.orEmpty()
        )
        "choice" -> StoryNode.ChoiceNode(
            id = nodeId,
            prompt = raw.prompt.orEmpty(),
            imagePath = raw.image?.let { resolveImage(dir, it) },
            options = raw.options.orEmpty().map { option ->
                ChoiceOption(
                    label = option.label,
                    imagePath = option.image?.let { resolveImage(dir, it) },
                    next = option.next
                )
            }
        )
        "quiz" -> StoryNode.QuizNode(
            id = nodeId,
            question = raw.question.orEmpty(),
            imagePath = raw.image?.let { resolveImage(dir, it) },
            answers = raw.answers.orEmpty().map { answer ->
                QuizAnswer(
                    text = answer.text,
                    imagePath = answer.image?.let { resolveImage(dir, it) },
                    isCorrect = answer.isCorrect
                )
            },
            next = raw.next.orEmpty()
        )
        "end" -> StoryNode.EndNode(
            id = nodeId,
            text = raw.text.orEmpty(),
            imagePath = raw.image?.let { resolveImage(dir, it) }
        )
        else -> throw IllegalArgumentException("Unknown node type: ${raw.type}")
    }

    private fun resolveImage(dir: File, relativePath: String): String? =
        File(dir, relativePath).takeIf { it.exists() }?.absolutePath

    /**
     * A story only loads if every next/option/answer reference resolves to a real node,
     * and at least one EndNode is reachable from the start - otherwise a kid could get
     * stuck on a dead or looping path with no way to finish.
     */
    private fun hasValidGraph(startNodeId: String, nodes: Map<String, StoryNode>): Boolean {
        if (startNodeId !in nodes) return false

        fun nextIdsOf(node: StoryNode): List<String> = when (node) {
            is StoryNode.ContentNode -> listOf(node.next)
            is StoryNode.ChoiceNode -> node.options.map { it.next }
            is StoryNode.QuizNode -> listOf(node.next)
            is StoryNode.EndNode -> emptyList()
        }

        if (nodes.values.any { node -> nextIdsOf(node).any { it !in nodes } }) return false

        val visited = mutableSetOf<String>()
        val queue = ArrayDeque(listOf(startNodeId))
        var reachedEnd = false
        while (queue.isNotEmpty()) {
            val currentId = queue.removeFirst()
            if (!visited.add(currentId)) continue
            val node = nodes.getValue(currentId)
            if (node is StoryNode.EndNode) reachedEnd = true
            queue.addAll(nextIdsOf(node).filterNot { it in visited })
        }
        return reachedEnd
    }

    private data class RawInteractiveStory(
        val id: String,
        val title: String,
        val description: String,
        val ageMin: Int,
        val ageMax: Int,
        val cover: String? = null,
        val startNode: String,
        val nodes: Map<String, RawNode> = emptyMap()
    )

    private data class RawNode(
        val type: String,
        val text: String? = null,
        val image: String? = null,
        val next: String? = null,
        val prompt: String? = null,
        val options: List<RawChoiceOption>? = null,
        val question: String? = null,
        val answers: List<RawQuizAnswer>? = null
    )

    private data class RawChoiceOption(val label: String, val image: String? = null, val next: String)
    private data class RawQuizAnswer(val text: String? = null, val image: String? = null, val isCorrect: Boolean = false)
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "ir.sospans.lalastories.repository.InteractiveStoryRepositoryTest"`
Expected: PASS (8 tests).

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/ir/sospans/lalastories/model/InteractiveStory.kt \
        app/src/main/java/ir/sospans/lalastories/repository/InteractiveStoryRepository.kt \
        app/src/test/java/ir/sospans/lalastories/repository/InteractiveStoryRepositoryTest.kt
git commit -m "feat: interactive story model and repository"
```

---

### Task 2: Interactive story progress tracking

**Files:**
- Create: `app/src/main/java/ir/sospans/lalastories/model/InteractiveStoryProgress.kt`
- Modify: `app/src/main/java/ir/sospans/lalastories/repository/ProgressRepository.kt`
- Modify: `app/src/test/java/ir/sospans/lalastories/repository/ProgressRepositoryTest.kt`

**Interfaces:**
- Consumes: nothing new from Task 1.
- Produces: `data class InteractiveStoryProgress(val storyId: String, val currentNodeId: String? = null)`
- Produces: `ProgressRepository.getInteractiveProgress(storyId: String): InteractiveStoryProgress`, `.saveInteractiveProgress(progress: InteractiveStoryProgress)`, `.clearInteractiveProgress(storyId: String)` — consumed by Task 7 (player screen).

- [ ] **Step 1: Write the failing test**

Modify `app/src/test/java/ir/sospans/lalastories/repository/ProgressRepositoryTest.kt` — replace the entire file:

```kotlin
package ir.sospans.lalastories.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import ir.sospans.lalastories.model.InteractiveStoryProgress
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

    @Test
    fun `getInteractiveProgress returns null currentNodeId when nothing saved`() {
        val progress = repo.getInteractiveProgress("khargoosh-va-rahe-jangal")
        assertNull(progress.currentNodeId)
    }

    @Test
    fun `saveInteractiveProgress and getInteractiveProgress round-trip`() {
        repo.saveInteractiveProgress(InteractiveStoryProgress("khargoosh-va-rahe-jangal", currentNodeId = "n6"))
        val loaded = repo.getInteractiveProgress("khargoosh-va-rahe-jangal")
        assertEquals("n6", loaded.currentNodeId)
    }

    @Test
    fun `clearInteractiveProgress resets currentNodeId to null`() {
        repo.saveInteractiveProgress(InteractiveStoryProgress("khargoosh-va-rahe-jangal", currentNodeId = "n6"))
        repo.clearInteractiveProgress("khargoosh-va-rahe-jangal")
        assertNull(repo.getInteractiveProgress("khargoosh-va-rahe-jangal").currentNodeId)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "ir.sospans.lalastories.repository.ProgressRepositoryTest"`
Expected: FAIL — compilation error, `getInteractiveProgress`/`saveInteractiveProgress`/`clearInteractiveProgress`/`InteractiveStoryProgress` unresolved references.

- [ ] **Step 3: Create the progress model**

Create `app/src/main/java/ir/sospans/lalastories/model/InteractiveStoryProgress.kt`:

```kotlin
package ir.sospans.lalastories.model

data class InteractiveStoryProgress(
    val storyId: String,
    val currentNodeId: String? = null
)
```

- [ ] **Step 4: Extend ProgressRepository**

Modify `app/src/main/java/ir/sospans/lalastories/repository/ProgressRepository.kt` — replace the entire file:

```kotlin
package ir.sospans.lalastories.repository

import android.content.Context
import ir.sospans.lalastories.model.InteractiveStoryProgress
import ir.sospans.lalastories.model.StoryProgress

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

    fun getInteractiveProgress(storyId: String): InteractiveStoryProgress = InteractiveStoryProgress(
        storyId = storyId,
        currentNodeId = prefs.getString("${storyId}_interactive_node", null)
    )

    fun saveInteractiveProgress(progress: InteractiveStoryProgress) {
        prefs.edit()
            .putString("${progress.storyId}_interactive_node", progress.currentNodeId)
            .apply()
    }

    fun clearInteractiveProgress(storyId: String) {
        prefs.edit().remove("${storyId}_interactive_node").apply()
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "ir.sospans.lalastories.repository.ProgressRepositoryTest"`
Expected: PASS (6 tests).

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/ir/sospans/lalastories/model/InteractiveStoryProgress.kt \
        app/src/main/java/ir/sospans/lalastories/repository/ProgressRepository.kt \
        app/src/test/java/ir/sospans/lalastories/repository/ProgressRepositoryTest.kt
git commit -m "feat: interactive story progress tracking"
```

---

### Task 3: Interactive stories string resources

**Files:**
- Modify: `app/src/main/res/values/strings.xml`

**Interfaces:**
- Produces: `R.string.interactive_stories_section_title`, `R.string.btn_play`, `R.string.btn_back_home` — consumed by Tasks 5, 6, 7, 8. (`R.string.btn_restart` already exists and is reused by Task 7 for the "read again" action.)

- [ ] **Step 1: Add the new strings**

Modify `app/src/main/res/values/strings.xml` — replace the entire file:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">داستان، شعر، قصه و لالایی کودکانه</string>
    <string name="btn_read">بخوان</string>
    <string name="btn_listen">گوش بده</string>
    <string name="btn_restart">شروع از اول</string>
    <string name="poems_section_title">اشعار</string>
    <string name="poems_section_subtitle">یک شعر تصادفی بخوان</string>
    <string name="poems_empty">هنوز شعری اضافه نشده است</string>
    <string name="stories_section_title">داستان‌ها</string>
    <string name="stories_section_subtitle">داستان‌های کودکانه رو بخون</string>
    <string name="lullabies_section_title">لالایی‌ها</string>
    <string name="lullabies_section_subtitle">لالایی بخون تا بچه بخوابه</string>
    <string name="lullabies_empty">هنوز لالایی‌ای اضافه نشده است</string>
    <string name="stories_voice_only_filter">فقط داستان‌های صوتی</string>
    <string name="interactive_stories_section_title">داستان‌های تعاملی</string>
    <string name="btn_play">بازی</string>
    <string name="btn_back_home">بازگشت به خانه</string>
</resources>
```

- [ ] **Step 2: Verify resources compile**

Run: `./gradlew :app:processDebugResources`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/res/values/strings.xml
git commit -m "feat: interactive stories string resources"
```

---

### Task 4: Seed the two example stories' content

**Files:**
- Create: `app/src/main/assets/interactive-stories/khargoosh-va-rahe-jangal/story.json`
- Create: `app/src/main/assets/interactive-stories/rouzi-ba-parvanehha/story.json`

**Interfaces:**
- Produces: two story folders under `app/src/main/assets/interactive-stories/`, consumed at runtime by `InteractiveStoryRepository` once `MainActivity` copies them to external storage (Task 9). No image files are included — see the "Deviation from the design spec" note in Global Constraints; every screen already renders correctly without them.

- [ ] **Step 1: Create the branching story**

Create `app/src/main/assets/interactive-stories/khargoosh-va-rahe-jangal/story.json`:

```json
{
  "id": "khargoosh-va-rahe-jangal",
  "title": "خرگوش و راه جنگل",
  "description": "نوشین خرگوشه باید قبل از غروب آفتاب به خانه‌ی مادربزرگش برسد. کدام راه را انتخاب می‌کنی؟",
  "ageMin": 4,
  "ageMax": 8,
  "startNode": "n1",
  "nodes": {
    "n1": {
      "type": "content",
      "text": "یک روز صبح آفتابی، مادر نوشین خرگوشه گفت: «نوشین جان، این سبد نان تازه رو برای مادربزرگ ببر. باید قبل از غروب آفتاب به خونه‌اش برسی.» نوشین سبد را برداشت و با خوشحالی از خانه بیرون زد.",
      "next": "n2"
    },
    "n2": {
      "type": "content",
      "text": "نوشین کمی راه رفت تا به کنار جنگل رسید. آنجا، جاده به دو راه جدا می‌شد؛ یکی کنار رودخانه می‌رفت و دیگری از میان چمنزار پر از گل می‌گذشت. نوشین ایستاد و به هر دو راه نگاه کرد.",
      "next": "n3"
    },
    "n3": {
      "type": "choice",
      "prompt": "نوشین کدام راه را انتخاب کند؟",
      "options": [
        { "label": "راه کنار رودخانه", "next": "n4" },
        { "label": "راه میان چمنزار", "next": "n5" }
      ]
    },
    "n4": {
      "type": "content",
      "text": "نوشین راه کنار رودخانه را انتخاب کرد. صدای آب و آواز پرنده‌ها را می‌شنید. کمی جلوتر، جغد پیری روی شاخه‌ای نشسته بود. جغد گفت: «سلام نوشین کوچولو! جلوتر یک پل چوبی هست که کمی لق می‌زند. مراقب باش.»",
      "next": "n6"
    },
    "n5": {
      "type": "content",
      "text": "نوشین راه میان چمنزار را انتخاب کرد. چمنزار پر از گل‌های رنگی بود. سنجاب کوچولویی که دنبال بلوط می‌گشت، نوشین را دید و گفت: «سلام! می‌خوای با هم بریم؟ من راه رو بلدم.» نوشین خوشحال شد و با هم به راه افتادند.",
      "next": "n9"
    },
    "n6": {
      "type": "choice",
      "prompt": "نوشین به پل چوبی رسید. حالا چه‌کار کند؟",
      "options": [
        { "label": "با احتیاط از روی پل رد شو", "next": "n7" },
        { "label": "از جغد بخواه کمکت کند", "next": "n8" }
      ]
    },
    "n7": {
      "type": "end",
      "text": "نوشین آرام و با احتیاط، قدم به قدم از روی پل رد شد. قلبش تند می‌زد، اما توانست! آن‌طرف پل، به سلامتی به راهش ادامه داد و درست قبل از غروب به خانه‌ی مادربزرگ رسید. مادربزرگ گفت: «آفرین نوشین! چقدر شجاع شدی.»"
    },
    "n8": {
      "type": "end",
      "text": "نوشین از جغد کمک خواست. جغد با بال‌های بزرگش نوشین را روی پشتش نشاند و به آرامی از روی رودخانه پرواز کرد. نوشین از آن بالا، جنگل زیبا را دید. آن‌ها با هم به خانه‌ی مادربزرگ رسیدند و از آن روز، جغد و نوشین دوستان خوبی شدند."
    },
    "n9": {
      "type": "content",
      "text": "سنجاب و نوشین از میان چمنزار گذشتند. سنجاب چند توت وحشی پیدا کرد و با نوشین قسمت کرد. آن‌ها با خنده و بازی راه را طی کردند تا چمنزار تمام شد.",
      "next": "n10"
    },
    "n10": {
      "type": "end",
      "text": "نوشین و سنجاب، درست قبل از غروب آفتاب، به خانه‌ی مادربزرگ رسیدند. مادربزرگ از دیدن دوست جدید نوشین خیلی خوشحال شد و برایشان کیک عسلی آورد. آن روز، نوشین یک دوست تازه پیدا کرده بود."
    }
  }
}
```

This graph has 2 choice points (`n3`, `n6`) and 3 endings (`n7`, `n8`, `n10`), matching the design spec.

- [ ] **Step 2: Create the quiz story**

Create `app/src/main/assets/interactive-stories/rouzi-ba-parvanehha/story.json`:

```json
{
  "id": "rouzi-ba-parvanehha",
  "title": "روزی با پروانه‌ها",
  "description": "داستان کرمکی که پروانه شد. همراهش باش و به سوال‌هایش جواب بده!",
  "ageMin": 4,
  "ageMax": 8,
  "startNode": "n1",
  "nodes": {
    "n1": {
      "type": "content",
      "text": "روی برگ سبز یک درخت، کرم کوچولویی به نام «کرمک» زندگی می‌کرد. کرمک هر روز برگ‌های سبز می‌خورد.",
      "next": "n2"
    },
    "n2": {
      "type": "content",
      "text": "کرمک هر روز کمی بیشتر برگ می‌خورد. هفته به هفته، بدنش بزرگ‌تر و بزرگ‌تر می‌شد و رنگ سبز روشنی پیدا کرده بود.",
      "next": "n3"
    },
    "n3": {
      "type": "content",
      "text": "یک روز، کرمک احساس کرد وقتش رسیده که یک خانه‌ی نرم و گرد دور خودش بسازد. او این خانه را «پیله» صدا کرد و داخل آن، آرام خوابید.",
      "next": "n4"
    },
    "n4": {
      "type": "quiz",
      "question": "کرمک برای خوابیدن، چه چیزی دور خودش ساخت؟",
      "answers": [
        { "text": "پیله", "isCorrect": true },
        { "text": "لانه‌ی پرنده", "isCorrect": false }
      ],
      "next": "n5"
    },
    "n5": {
      "type": "content",
      "text": "روزها گذشت. یک صبح آفتابی، پیله آرام‌آرام باز شد. کرمک دیگر کرمک نبود؛ او حالا بال‌های رنگارنگ و زیبا داشت. او یک پروانه شده بود!",
      "next": "n6"
    },
    "n6": {
      "type": "content",
      "text": "پروانه بال‌هایش را تکان داد و به آسمان پرواز کرد. او به باغی پر از گل رسید و روی یک گل قرمز نشست تا شهد شیرینش را بنوشد.",
      "next": "n7"
    },
    "n7": {
      "type": "content",
      "text": "زنبور کوچولویی هم آنجا بود. زنبور گفت: «سلام! من هم عاشق این گل‌ها هستم.» پروانه و زنبور با هم از گلی به گل دیگر پرواز کردند و روز قشنگی را با هم گذراندند.",
      "next": "n8"
    },
    "n8": {
      "type": "quiz",
      "question": "کرمک قبل از پروانه شدن، چه بود؟",
      "answers": [
        { "text": "یک کرم روی برگ", "isCorrect": true },
        { "text": "یک پرنده‌ی کوچک", "isCorrect": false }
      ],
      "next": "n9"
    },
    "n9": {
      "type": "quiz",
      "question": "پروانه از گل‌ها چه چیزی می‌نوشد؟",
      "answers": [
        { "text": "شهد گل (نکتار)", "isCorrect": true },
        { "text": "آب رودخانه", "isCorrect": false }
      ],
      "next": "n10"
    },
    "n10": {
      "type": "end",
      "text": "غروب که شد، پروانه‌ی رنگارنگ به آرامی روی شاخه‌ای نشست. او به یاد روزهایی افتاد که یک کرم کوچک روی برگ بود. حالا می‌دانست که هر تغییری، حتی وقتی کمی طول بکشد، می‌تواند به چیزی زیبا برسد."
    }
  }
}
```

This graph has one mid-story quiz (`n4`) and two end-of-story quizzes (`n8`, `n9`) before the final ending (`n10`), matching the design spec.

- [ ] **Step 3: Verify both files parse as valid JSON**

Run: `for f in app/src/main/assets/interactive-stories/*/story.json; do python3 -c "import json,sys; json.load(open(sys.argv[1]))" "$f" || echo "INVALID: $f"; done`
Expected: no "INVALID" lines printed.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/assets/interactive-stories/
git commit -m "feat: seed two interactive stories"
```

---

### Task 5: Interactive story card and list screen

**Files:**
- Create: `app/src/main/java/ir/sospans/lalastories/ui/home/InteractiveStoryCard.kt`
- Create: `app/src/main/java/ir/sospans/lalastories/ui/interactivestories/InteractiveStoriesScreen.kt`

**Interfaces:**
- Consumes: `InteractiveStory` (Task 1), `R.string.interactive_stories_section_title` (Task 3)
- Produces: `@Composable fun InteractiveStoryCard(story: InteractiveStory, onClick: () -> Unit)`
- Produces: `@Composable fun InteractiveStoriesScreen(stories: List<InteractiveStory>, onStoryClick: (InteractiveStory) -> Unit, onBack: () -> Unit)` — consumed by Task 9 (`AppNavigation`).

- [ ] **Step 1: Create the card**

Create `app/src/main/java/ir/sospans/lalastories/ui/home/InteractiveStoryCard.kt`:

```kotlin
package ir.sospans.lalastories.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import ir.sospans.lalastories.model.InteractiveStory

@Composable
fun InteractiveStoryCard(story: InteractiveStory, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.75f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            ) {
                if (story.coverPath != null) {
                    AsyncImage(
                        model = story.coverPath,
                        contentDescription = story.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Extension,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(64.dp)
                        )
                    }
                }
            }
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

- [ ] **Step 2: Create the list screen**

Create `app/src/main/java/ir/sospans/lalastories/ui/interactivestories/InteractiveStoriesScreen.kt`:

```kotlin
package ir.sospans.lalastories.ui.interactivestories

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import ir.sospans.lalastories.R
import ir.sospans.lalastories.model.InteractiveStory
import ir.sospans.lalastories.ui.home.InteractiveStoryCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InteractiveStoriesScreen(
    stories: List<InteractiveStory>,
    onStoryClick: (InteractiveStory) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                TopAppBar(
                    title = {
                        Text(
                            stringResource(R.string.interactive_stories_section_title),
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "برگشت")
                        }
                    }
                )
            }
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(padding).fillMaxSize()
        ) {
            items(items = stories, key = { it.id }) { story ->
                InteractiveStoryCard(story = story, onClick = { onStoryClick(story) })
            }
        }
    }
}
```

- [ ] **Step 3: Verify it compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL (these two files aren't referenced anywhere yet, so nothing else can break).

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/ir/sospans/lalastories/ui/home/InteractiveStoryCard.kt \
        app/src/main/java/ir/sospans/lalastories/ui/interactivestories/InteractiveStoriesScreen.kt
git commit -m "feat: interactive stories list screen"
```

---

### Task 6: Interactive story detail screen

**Files:**
- Create: `app/src/main/java/ir/sospans/lalastories/ui/detail/InteractiveStoryDetailScreen.kt`

**Interfaces:**
- Consumes: `InteractiveStory` (Task 1), `R.string.btn_play` (Task 3)
- Produces: `@Composable fun InteractiveStoryDetailScreen(story: InteractiveStory, onPlayClick: () -> Unit, onBack: () -> Unit)` — consumed by Task 9 (`AppNavigation`).

- [ ] **Step 1: Create the screen**

Create `app/src/main/java/ir/sospans/lalastories/ui/detail/InteractiveStoryDetailScreen.kt`:

```kotlin
package ir.sospans.lalastories.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import ir.sospans.lalastories.R
import ir.sospans.lalastories.model.InteractiveStory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InteractiveStoryDetailScreen(story: InteractiveStory, onPlayClick: () -> Unit, onBack: () -> Unit) {
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
            if (story.coverPath != null) {
                AsyncImage(
                    model = story.coverPath,
                    contentDescription = story.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Extension,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(96.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(story.title, style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text(story.description, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = onPlayClick,
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("🧩", style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Default))
                    Text(stringResource(R.string.btn_play), style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}
```

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL (not referenced anywhere yet).

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/ir/sospans/lalastories/ui/detail/InteractiveStoryDetailScreen.kt
git commit -m "feat: interactive story detail screen"
```

---

### Task 7: Interactive story player screen

**Files:**
- Create: `app/src/main/java/ir/sospans/lalastories/ui/interactivestory/InteractiveStoryPlayerScreen.kt`

**Interfaces:**
- Consumes: `InteractiveStory`, `StoryNode` and its subtypes (Task 1); `ProgressRepository.getInteractiveProgress/saveInteractiveProgress/clearInteractiveProgress`, `InteractiveStoryProgress` (Task 2); `R.string.btn_restart`, `R.string.btn_back_home` (Task 3)
- Produces: `@Composable fun InteractiveStoryPlayerScreen(story: InteractiveStory, progressRepository: ProgressRepository, onBack: () -> Unit)` — consumed by Task 9 (`AppNavigation`).

- [ ] **Step 1: Create the screen**

Create `app/src/main/java/ir/sospans/lalastories/ui/interactivestory/InteractiveStoryPlayerScreen.kt`:

```kotlin
package ir.sospans.lalastories.ui.interactivestory

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import ir.sospans.lalastories.R
import ir.sospans.lalastories.model.InteractiveStory
import ir.sospans.lalastories.model.InteractiveStoryProgress
import ir.sospans.lalastories.model.StoryNode
import ir.sospans.lalastories.repository.ProgressRepository
import kotlinx.coroutines.delay

private val CorrectColor = Color(0xFF4CAF50)
private val WrongColor = Color(0xFFE53935)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InteractiveStoryPlayerScreen(
    story: InteractiveStory,
    progressRepository: ProgressRepository,
    onBack: () -> Unit
) {
    val savedProgress = remember { progressRepository.getInteractiveProgress(story.id) }
    var currentNodeId by remember {
        mutableStateOf(savedProgress.currentNodeId?.takeIf { it in story.nodes } ?: story.startNodeId)
    }

    DisposableEffect(currentNodeId) {
        onDispose {
            progressRepository.saveInteractiveProgress(InteractiveStoryProgress(story.id, currentNodeId))
        }
    }

    val currentNode = story.nodes[currentNodeId] ?: story.nodes.getValue(story.startNodeId)

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
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val node = currentNode) {
                is StoryNode.ContentNode -> ContentNodeView(node, onNext = { currentNodeId = node.next })
                is StoryNode.ChoiceNode -> ChoiceNodeView(node, onChoice = { next -> currentNodeId = next })
                is StoryNode.QuizNode -> QuizNodeView(node, onCorrect = { currentNodeId = node.next })
                is StoryNode.EndNode -> EndNodeView(
                    node = node,
                    onRestart = {
                        progressRepository.clearInteractiveProgress(story.id)
                        currentNodeId = story.startNodeId
                    },
                    onHome = onBack
                )
            }
        }
    }
}

@Composable
private fun ContentNodeView(node: StoryNode.ContentNode, onNext: () -> Unit) {
    Column(modifier = Modifier.padding(24.dp).fillMaxSize()) {
        if (node.imagePath != null) {
            AsyncImage(
                model = node.imagePath,
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        Box(modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
            Text(
                text = node.text,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Text("بعدی ←")
        }
    }
}

@Composable
private fun ChoiceNodeView(node: StoryNode.ChoiceNode, onChoice: (String) -> Unit) {
    Column(
        modifier = Modifier.padding(24.dp).fillMaxSize().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (node.imagePath != null) {
            AsyncImage(
                model = node.imagePath,
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        Text(node.prompt, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(24.dp))
        node.options.forEach { option ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clickable { onChoice(option.next) },
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (option.imagePath != null) {
                        AsyncImage(
                            model = option.imagePath,
                            contentDescription = option.label,
                            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Text(option.label, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

@Composable
private fun QuizNodeView(node: StoryNode.QuizNode, onCorrect: () -> Unit) {
    var lastTappedIndex by remember(node.id) { mutableStateOf<Int?>(null) }
    var advancing by remember(node.id) { mutableStateOf(false) }

    LaunchedEffect(node.id, lastTappedIndex) {
        val index = lastTappedIndex ?: return@LaunchedEffect
        if (node.answers[index].isCorrect) {
            advancing = true
            delay(700)
            onCorrect()
        }
    }

    Column(
        modifier = Modifier.padding(24.dp).fillMaxSize().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (node.imagePath != null) {
            AsyncImage(
                model = node.imagePath,
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        Text(node.question, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(24.dp))
        node.answers.forEachIndexed { index, answer ->
            val isTapped = lastTappedIndex == index
            val backgroundColor = when {
                !isTapped -> MaterialTheme.colorScheme.surfaceVariant
                answer.isCorrect -> CorrectColor
                else -> WrongColor
            }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clickable(enabled = !advancing) { lastTappedIndex = index },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = backgroundColor)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (answer.imagePath != null) {
                        AsyncImage(
                            model = answer.imagePath,
                            contentDescription = answer.text,
                            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                    if (answer.text != null) {
                        Text(answer.text, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun EndNodeView(node: StoryNode.EndNode, onRestart: () -> Unit, onHome: () -> Unit) {
    Column(
        modifier = Modifier.padding(24.dp).fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (node.imagePath != null) {
            AsyncImage(
                model = node.imagePath,
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        Box(modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
            Text(node.text, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onRestart,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Text(stringResource(R.string.btn_restart))
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = onHome,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Text(stringResource(R.string.btn_back_home))
        }
    }
}
```

Wrong quiz answers stay tappable (`advancing` only becomes `true`, disabling further taps, once the *correct* answer is tapped) — tapping any answer recolors just that card (green if correct, red if wrong) via `lastTappedIndex`, matching the spec's "instant feedback, then auto-continue, no retry lock" behavior.

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL (not referenced anywhere yet).

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/ir/sospans/lalastories/ui/interactivestory/InteractiveStoryPlayerScreen.kt
git commit -m "feat: interactive story player screen"
```

---

### Task 8: Interactive stories home entry card

**Files:**
- Create: `app/src/main/java/ir/sospans/lalastories/ui/home/InteractiveStoriesEntryCard.kt`
- Modify: `app/src/main/java/ir/sospans/lalastories/ui/home/HomeScreen.kt`

**Interfaces:**
- Consumes: `R.string.interactive_stories_section_title` (Task 3)
- Produces: `@Composable fun InteractiveStoriesEntryCard(onClick: () -> Unit)`
- Produces: `HomeScreen(onStoriesClick: () -> Unit, onPoemsClick: () -> Unit, onLullabiesClick: () -> Unit, onInteractiveStoriesClick: () -> Unit)` — the new `onInteractiveStoriesClick` parameter is consumed by Task 9 (`AppNavigation`).

No custom artwork is used for this card (unlike `StoriesEntryCard`/`PoemsEntryCard`/`LullabiesEntryCard`, which use bundled `drawable-nodpi` images) — it's a self-drawn gradient + icon card instead, so this task needs no new image assets.

- [ ] **Step 1: Create the entry card**

Create `app/src/main/java/ir/sospans/lalastories/ui/home/InteractiveStoriesEntryCard.kt`:

```kotlin
package ir.sospans.lalastories.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ir.sospans.lalastories.R

@Composable
fun InteractiveStoriesEntryCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.horizontalGradient(listOf(Color(0xFFFF6B35), Color(0xFF7C4DFF))))
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Extension,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(40.dp)
            )
            Text(
                text = stringResource(R.string.interactive_stories_section_title),
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )
        }
    }
}
```

- [ ] **Step 2: Wire it into HomeScreen as a new full-width row**

Modify `app/src/main/java/ir/sospans/lalastories/ui/home/HomeScreen.kt` — replace the entire file:

```kotlin
package ir.sospans.lalastories.ui.home

import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.adivery.sdk.AdiveryBannerAdView
import com.adivery.sdk.BannerSize
import ir.sospans.lalastories.R

private const val BANNER_PLACEMENT_ID = "aa78c7e1-292a-40fa-973a-6abb2fa7e6db"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onStoriesClick: () -> Unit,
    onPoemsClick: () -> Unit,
    onLullabiesClick: () -> Unit,
    onInteractiveStoriesClick: () -> Unit
) {
    Scaffold(
        bottomBar = {
            AndroidView(
                factory = { ctx ->
                    AdiveryBannerAdView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                        setPlacementId(BANNER_PLACEMENT_ID)
                        setBannerSize(BannerSize.SMART_BANNER)
                        loadAd()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Image(
                painter = painterResource(id = R.drawable.home_header),
                contentDescription = stringResource(R.string.app_name),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(768f / 122f),
                contentScale = ContentScale.FillWidth
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    StoriesEntryCard(onClick = onStoriesClick)
                }
                Box(modifier = Modifier.weight(1f)) {
                    PoemsEntryCard(onClick = onPoemsClick)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                LullabiesEntryCard(onClick = onLullabiesClick)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                InteractiveStoriesEntryCard(onClick = onInteractiveStoriesClick)
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
```

- [ ] **Step 3: Verify it compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD FAILED — `AppNavigation.kt` call site `HomeScreen(...)` is now missing the required `onInteractiveStoriesClick` argument. This is expected; Task 9 fixes the call site. Confirm the *only* error is that missing argument, then proceed.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/ir/sospans/lalastories/ui/home/InteractiveStoriesEntryCard.kt \
        app/src/main/java/ir/sospans/lalastories/ui/home/HomeScreen.kt
git commit -m "feat: interactive stories home entry card"
```

---

### Task 9: Wire navigation and asset copying end-to-end

**Files:**
- Modify: `app/src/main/java/ir/sospans/lalastories/navigation/AppNavigation.kt`
- Modify: `app/src/main/java/ir/sospans/lalastories/MainActivity.kt`

**Interfaces:**
- Consumes: everything produced by Tasks 1, 5, 6, 7, 8.
- Produces: fully wired feature — `AppNavigation` gains an `interactiveStoryRepository: InteractiveStoryRepository` parameter and three new routes; `MainActivity` copies bundled interactive stories to external storage on first run and constructs the repository.

- [ ] **Step 1: Update AppNavigation**

Modify `app/src/main/java/ir/sospans/lalastories/navigation/AppNavigation.kt` — replace the entire file:

```kotlin
package ir.sospans.lalastories.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.adivery.sdk.Adivery
import com.adivery.sdk.AdiveryListener
import ir.sospans.lalastories.repository.InteractiveStoryRepository
import ir.sospans.lalastories.repository.LullabyRepository
import ir.sospans.lalastories.repository.PoemRepository
import ir.sospans.lalastories.repository.ProgressRepository
import ir.sospans.lalastories.repository.StoryRepository
import ir.sospans.lalastories.ui.detail.InteractiveStoryDetailScreen
import ir.sospans.lalastories.ui.detail.StoryDetailScreen
import ir.sospans.lalastories.ui.home.HomeScreen
import ir.sospans.lalastories.ui.interactivestories.InteractiveStoriesScreen
import ir.sospans.lalastories.ui.interactivestory.InteractiveStoryPlayerScreen
import ir.sospans.lalastories.ui.listening.ListeningScreen
import ir.sospans.lalastories.ui.lullabies.LullabiesScreen
import ir.sospans.lalastories.ui.lullaby.LullabyPlayerScreen
import ir.sospans.lalastories.ui.poems.PoemsScreen
import ir.sospans.lalastories.ui.reading.ReadingScreen
import ir.sospans.lalastories.ui.stories.StoriesScreen

private const val INTERSTITIAL_PLACEMENT_ID = "e3d7931e-195b-4ee7-b621-e3b1dbd0a569"

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Stories : Screen("stories")
    object Detail : Screen("detail/{storyId}") {
        fun createRoute(storyId: String) = "detail/$storyId"
    }
    object Reading : Screen("reading/{storyId}") {
        fun createRoute(storyId: String) = "reading/$storyId"
    }
    object Listening : Screen("listening/{storyId}") {
        fun createRoute(storyId: String) = "listening/$storyId"
    }
    object Poems : Screen("poems")
    object Lullabies : Screen("lullabies")
    object LullabyPlayer : Screen("lullabyPlayer/{lullabyId}") {
        fun createRoute(lullabyId: String) = "lullabyPlayer/$lullabyId"
    }
    object InteractiveStories : Screen("interactiveStories")
    object InteractiveStoryDetail : Screen("interactiveStoryDetail/{storyId}") {
        fun createRoute(storyId: String) = "interactiveStoryDetail/$storyId"
    }
    object InteractiveStoryPlayer : Screen("interactiveStoryPlayer/{storyId}") {
        fun createRoute(storyId: String) = "interactiveStoryPlayer/$storyId"
    }
}

@Composable
fun AppNavigation(
    storyRepository: StoryRepository,
    poemRepository: PoemRepository,
    lullabyRepository: LullabyRepository,
    interactiveStoryRepository: InteractiveStoryRepository,
    progressRepository: ProgressRepository
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            HomeScreen(
                onStoriesClick = { navController.navigate(Screen.Stories.route) },
                onPoemsClick = { navController.navigate(Screen.Poems.route) },
                onLullabiesClick = { navController.navigate(Screen.Lullabies.route) },
                onInteractiveStoriesClick = { navController.navigate(Screen.InteractiveStories.route) }
            )
        }
        composable(Screen.Stories.route) {
            StoriesScreen(
                stories = storyRepository.loadStories(),
                onStoryClick = { story ->
                    navController.navigate(Screen.Detail.createRoute(story.id))
                },
                onBack = { navController.popBackStack() }
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
                storyRepository = storyRepository,
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
                storyRepository = storyRepository,
                progressRepository = progressRepository,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Poems.route) {
            PoemsScreen(
                poemRepository = poemRepository,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Lullabies.route) {
            LaunchedEffect(Unit) {
                Adivery.prepareInterstitialAd(context, INTERSTITIAL_PLACEMENT_ID)
            }
            LullabiesScreen(
                lullabies = lullabyRepository.loadLullabies(),
                onLullabyClick = { lullaby ->
                    if (Adivery.isLoaded(INTERSTITIAL_PLACEMENT_ID)) {
                        Adivery.addPlacementListener(INTERSTITIAL_PLACEMENT_ID, object : AdiveryListener() {
                            override fun onInterstitialAdClosed(placementId: String) {
                                Adivery.removePlacementListener(INTERSTITIAL_PLACEMENT_ID)
                                navController.navigate(Screen.LullabyPlayer.createRoute(lullaby.id))
                            }
                        })
                        Adivery.showAd(INTERSTITIAL_PLACEMENT_ID)
                    } else {
                        navController.navigate(Screen.LullabyPlayer.createRoute(lullaby.id))
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            Screen.LullabyPlayer.route,
            arguments = listOf(navArgument("lullabyId") { type = NavType.StringType })
        ) { backStack ->
            val lullabyId = backStack.arguments?.getString("lullabyId")!!
            LullabyPlayerScreen(
                lullabies = lullabyRepository.loadLullabies(),
                lullabyRepository = lullabyRepository,
                startLullabyId = lullabyId,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.InteractiveStories.route) {
            InteractiveStoriesScreen(
                stories = interactiveStoryRepository.loadInteractiveStories(),
                onStoryClick = { story ->
                    navController.navigate(Screen.InteractiveStoryDetail.createRoute(story.id))
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            Screen.InteractiveStoryDetail.route,
            arguments = listOf(navArgument("storyId") { type = NavType.StringType })
        ) { backStack ->
            val storyId = backStack.arguments?.getString("storyId")!!
            val story = interactiveStoryRepository.loadInteractiveStories().first { it.id == storyId }
            InteractiveStoryDetailScreen(
                story = story,
                onPlayClick = { navController.navigate(Screen.InteractiveStoryPlayer.createRoute(storyId)) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            Screen.InteractiveStoryPlayer.route,
            arguments = listOf(navArgument("storyId") { type = NavType.StringType })
        ) { backStack ->
            val storyId = backStack.arguments?.getString("storyId")!!
            val story = interactiveStoryRepository.loadInteractiveStories().first { it.id == storyId }
            InteractiveStoryPlayerScreen(
                story = story,
                progressRepository = progressRepository,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
```

- [ ] **Step 2: Update MainActivity**

Modify `app/src/main/java/ir/sospans/lalastories/MainActivity.kt` — replace the entire file:

```kotlin
package ir.sospans.lalastories

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import com.adivery.sdk.Adivery
import ir.sospans.lalastories.navigation.AppNavigation
import ir.sospans.lalastories.remote.ContentCacheIndex
import ir.sospans.lalastories.remote.ContentDownloader
import ir.sospans.lalastories.remote.ManifestClient
import ir.sospans.lalastories.repository.InteractiveStoryRepository
import ir.sospans.lalastories.repository.LullabyRepository
import ir.sospans.lalastories.repository.PoemRepository
import ir.sospans.lalastories.repository.ProgressRepository
import ir.sospans.lalastories.repository.StoryRepository
import ir.sospans.lalastories.ui.theme.KidStoriesTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Reopening the app from the launcher/recents while a task already exists can spawn a
        // second MainActivity instance on top of the existing one, so back navigation on the
        // home screen just reveals the older instance instead of exiting the app.
        if (!isTaskRoot && intent?.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_LAUNCHER)) {
            finish()
            return
        }

        Adivery.configure(application, "2c1809b1-b6e4-4305-8757-847a73410a18")

        val storiesDir = File(getExternalFilesDir(null), "stories")
        copyBundledStoriesIfNeeded(storiesDir)

        val poemsDir = File(getExternalFilesDir(null), "poems")
        copyBundledPoemsIfNeeded(poemsDir)

        val lullabiesDir = File(getExternalFilesDir(null), "lullabies")
        copyBundledLullabiesIfNeeded(lullabiesDir)

        val interactiveStoriesDir = File(getExternalFilesDir(null), "interactive-stories")
        copyBundledInteractiveStoriesIfNeeded(interactiveStoriesDir)

        val remoteRootDir = File(getExternalFilesDir(null), "remote-cache")
        val manifestClient = ManifestClient(remoteRootDir)
        val cacheIndex = ContentCacheIndex(File(remoteRootDir, "cache-index.json"))
        val contentDownloader = ContentDownloader(remoteRootDir, cacheIndex)

        val storyRepository = StoryRepository(
            storiesDir, File(remoteRootDir, "stories"), manifestClient, cacheIndex, contentDownloader
        )
        val poemRepository = PoemRepository(
            poemsDir, File(remoteRootDir, "poems"), manifestClient, cacheIndex, contentDownloader
        )
        val lullabyRepository = LullabyRepository(
            lullabiesDir, File(remoteRootDir, "lullabies"), manifestClient, cacheIndex, contentDownloader
        )
        val interactiveStoryRepository = InteractiveStoryRepository(interactiveStoriesDir)
        val progressRepository = ProgressRepository(this)

        // Refresh the 3 static manifests in the background on every launch. Failure (offline,
        // CDN unreachable) is silent - screens just keep using whatever was cached last time,
        // or bundled-only content if a manifest has never been fetched successfully.
        lifecycleScope.launch(Dispatchers.IO) {
            manifestClient.refreshStoryManifest()
        }
        lifecycleScope.launch(Dispatchers.IO) {
            manifestClient.refreshPoemManifest()
        }
        lifecycleScope.launch(Dispatchers.IO) {
            manifestClient.refreshLullabyManifest()
        }

        setContent {
            KidStoriesTheme {
                AppNavigation(
                    storyRepository = storyRepository,
                    poemRepository = poemRepository,
                    lullabyRepository = lullabyRepository,
                    interactiveStoryRepository = interactiveStoryRepository,
                    progressRepository = progressRepository
                )
            }
        }
    }

    private fun copyBundledStoriesIfNeeded(storiesDir: File) {
        val prefs = getSharedPreferences("app_state", MODE_PRIVATE)
        if (prefs.getBoolean("stories_copied_v15", false)) return

        copyAssetDir("stories", storiesDir)
        prefs.edit().putBoolean("stories_copied_v15", true).apply()
    }

    private fun copyBundledPoemsIfNeeded(poemsDir: File) {
        val prefs = getSharedPreferences("app_state", MODE_PRIVATE)
        if (prefs.getBoolean("poems_copied_v2", false)) return

        poemsDir.deleteRecursively()
        copyAssetDir("poems", poemsDir)
        prefs.edit().putBoolean("poems_copied_v2", true).apply()
    }

    private fun copyBundledLullabiesIfNeeded(lullabiesDir: File) {
        val prefs = getSharedPreferences("app_state", MODE_PRIVATE)
        if (prefs.getBoolean("lullabies_copied_v2", false)) return

        lullabiesDir.deleteRecursively()
        copyAssetDir("lullabies", lullabiesDir)
        prefs.edit().putBoolean("lullabies_copied_v2", true).apply()
    }

    private fun copyBundledInteractiveStoriesIfNeeded(dir: File) {
        val prefs = getSharedPreferences("app_state", MODE_PRIVATE)
        if (prefs.getBoolean("interactive_stories_copied_v1", false)) return

        dir.deleteRecursively()
        copyAssetDir("interactive-stories", dir)
        prefs.edit().putBoolean("interactive_stories_copied_v1", true).apply()
    }

    private fun copyAssetDir(assetPath: String, destDir: File) {
        destDir.mkdirs()
        assets.list(assetPath)?.forEach { name ->
            val childAsset = "$assetPath/$name"
            val childDest = File(destDir, name)
            val children = assets.list(childAsset)
            if (children != null && children.isNotEmpty()) {
                copyAssetDir(childAsset, childDest)
            } else if (!childDest.exists() || name.endsWith(".json") || name.startsWith("cover.")) {
                childDest.delete()
                assets.open(childAsset).use { it.copyTo(childDest.outputStream()) }
            }
        }
    }
}
```

- [ ] **Step 3: Verify it compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL — every call site now matches.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/ir/sospans/lalastories/navigation/AppNavigation.kt \
        app/src/main/java/ir/sospans/lalastories/MainActivity.kt
git commit -m "feat: wire interactive stories navigation"
```

---

### Task 10: Build, run tests, and manually verify on device

**Files:** none (verification only).

- [ ] **Step 1: Run the full unit test suite**

Run: `./gradlew :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL, all tests pass (including the 8 new `InteractiveStoryRepositoryTest` cases and 3 new `ProgressRepositoryTest` cases).

- [ ] **Step 2: Assemble and install the debug build**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

Run: `./gradlew :app:installDebug`
Expected: installs on a connected device/emulator. If none is attached, skip this step and sideload the APK from `app/build/outputs/apk/debug/` manually.

- [ ] **Step 3: Manually verify the full feature on-device**

Walk through every path — this is the only place branching/quiz logic gets exercised end-to-end, since there's no Compose UI test setup in this repo:

- Open the app, confirm a new "داستان‌های تعاملی" card appears on the home screen below the lullabies card, and tapping it opens a 2-card grid ("خرگوش و راه جنگل", "روزی با پروانه‌ها").
- Open **خرگوش و راه جنگل**, tap through to the detail screen, tap "بازی". Play through to **all three endings** across separate replays (use the "شروع از اول" button on the end screen to restart): river path → careful crossing; river path → owl helps; meadow path → squirrel friend.
- Back out mid-story (system/app back) after a choice, reopen the story, and confirm it resumes from where you left off rather than restarting from `n1`.
- Open **روزی با پروانه‌ها**, play through to the mid-story quiz (`n4`): tap the wrong answer first (confirm it flashes red and does *not* advance, and remains tappable), then tap the correct answer (confirm it flashes green and auto-advances after a short delay). Repeat the same right/wrong check for both end-of-story quizzes (`n8`, `n9`), then confirm you reach the final ending screen.
- Confirm both stories render correctly with no images (fallback icon on cards/detail screens, text-only nodes in the player) — no broken image placeholders or crashes.

- [ ] **Step 4: Fix forward, don't patch backward**

If manual verification finds a bug, fix it in the relevant task's file directly (don't add a patch commit on top describing the bug) and re-run the affected step(s) above before considering Task 10 done.
