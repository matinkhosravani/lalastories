# Poems Feature Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a "poems" (اشعار) section to the home screen, entered via a single colorful banner card, that opens a shuffled, colorful poem viewer with wraparound Next/Previous navigation.

**Architecture:** A new `Poem` model + `PoemRepository` mirror the existing `Story`/`StoryRepository` pair but trimmed to only what poems need (no pages, no age range, no audio). Assets live in `assets/poems/<id>/poem.json`, copied to external storage on first run the same way stories are. A new `PoemsScreen` composable shuffles the poem list once and lets the user step through it. The home screen's existing story grid is untouched; a new full-width entry card is appended to it.

**Tech Stack:** Kotlin, Jetpack Compose (Material3), Gson, JUnit4 (existing project stack — no new dependencies).

## Global Constraints

- RTL layout is the app default (`KidStoriesTheme` forces `LayoutDirection.Rtl`); any back arrow / top bar must use the existing forced-`LayoutDirection.Ltr` + `TextAlign.Right` pattern already applied in `StoryDetailScreen.kt`, `ReadingScreen.kt`, `ListeningScreen.kt`, `SettingsScreen.kt` — do not deviate from it.
- No new Gradle dependencies — `com.google.code.gson:gson:2.10.1` and `androidx.compose.material:material-icons-extended` are already present and sufficient.
- Follow existing code style: no comments unless explaining a non-obvious constraint, data classes with default `null`/`false` for optional fields, `Gson().fromJson` + manual `.copy()` for resolved file paths (see `StoryRepository.kt`).
- This repo has no Compose UI test setup (`app/src/androidTest` is empty) — verification for UI tasks is manual build+install+visual check, matching existing project convention. Only the repository layer gets JUnit tests, matching `StoryRepositoryTest.kt`.

---

### Task 1: Poem model and PoemRepository

**Files:**
- Create: `app/src/main/java/ir/sospans/lalastories/model/Poem.kt`
- Create: `app/src/main/java/ir/sospans/lalastories/repository/PoemRepository.kt`
- Test: `app/src/test/java/ir/sospans/lalastories/repository/PoemRepositoryTest.kt`

**Interfaces:**
- Produces: `data class Poem(val id: String, val title: String, val text: String, val image: String? = null, val imagePath: String? = null)`
- Produces: `class PoemRepository(poemsDir: File) { fun loadPoems(): List<Poem> }`

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/ir/sospans/lalastories/repository/PoemRepositoryTest.kt`:

```kotlin
package ir.sospans.lalastories.repository

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

class PoemRepositoryTest {

    private lateinit var poemsDir: File

    @Before
    fun setUp() {
        poemsDir = createTempDir("poems")
        val poemDir = File(poemsDir, "spring-rain").also { it.mkdirs() }
        poemDir.resolve("poem.json").writeText("""
            {
              "id": "spring-rain",
              "title": "باران بهاری",
              "text": "باران می‌آید نرم نرم\nروی برگ‌های سبز و گرم"
            }
        """.trimIndent())
    }

    @Test
    fun `loadPoems returns poem parsed from json`() {
        val repo = PoemRepository(poemsDir)
        val poems = repo.loadPoems()
        assertEquals(1, poems.size)
        assertEquals("spring-rain", poems[0].id)
        assertEquals("باران بهاری", poems[0].title)
        assertTrue(poems[0].text.contains("باران"))
        assertNull(poems[0].imagePath)
    }

    @Test
    fun `loadPoems sets imagePath when referenced image file exists`() {
        val poemDir = File(poemsDir, "spring-rain")
        poemDir.resolve("poem.json").writeText("""
            {
              "id": "spring-rain",
              "title": "باران بهاری",
              "text": "باران می‌آید نرم نرم",
              "image": "cover.jpg"
            }
        """.trimIndent())
        poemDir.resolve("cover.jpg").createNewFile()

        val repo = PoemRepository(poemsDir)
        val poems = repo.loadPoems()
        assertNotNull(poems[0].imagePath)
        assertTrue(poems[0].imagePath!!.endsWith("cover.jpg"))
    }

    @Test
    fun `loadPoems skips folder with no poem json`() {
        File(poemsDir, "empty-folder").mkdirs()
        val repo = PoemRepository(poemsDir)
        val poems = repo.loadPoems()
        assertEquals(1, poems.size)
    }

    @Test
    fun `loadPoems returns empty list when poems dir does not exist`() {
        val missingDir = File(poemsDir, "does-not-exist")
        val repo = PoemRepository(missingDir)
        assertTrue(repo.loadPoems().isEmpty())
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "ir.sospans.lalastories.repository.PoemRepositoryTest"`
Expected: FAIL — compilation error, `PoemRepository`/`Poem` unresolved references.

- [ ] **Step 3: Create the Poem model**

Create `app/src/main/java/ir/sospans/lalastories/model/Poem.kt`:

```kotlin
package ir.sospans.lalastories.model

data class Poem(
    val id: String,
    val title: String,
    val text: String,
    val image: String? = null,
    val imagePath: String? = null
)
```

- [ ] **Step 4: Create PoemRepository**

Create `app/src/main/java/ir/sospans/lalastories/repository/PoemRepository.kt`:

```kotlin
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
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "ir.sospans.lalastories.repository.PoemRepositoryTest"`
Expected: PASS (4 tests)

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/ir/sospans/lalastories/model/Poem.kt \
        app/src/main/java/ir/sospans/lalastories/repository/PoemRepository.kt \
        app/src/test/java/ir/sospans/lalastories/repository/PoemRepositoryTest.kt
git commit -m "feat(poems): add Poem model and PoemRepository"
```

---

### Task 2: Poems string resources

**Files:**
- Modify: `app/src/main/res/values/strings.xml`

**Interfaces:**
- Produces: string resources `R.string.poems_section_title`, `R.string.poems_section_subtitle`, `R.string.poems_empty` — consumed by Task 3 and Task 4.

- [ ] **Step 1: Add the new strings**

Modify `app/src/main/res/values/strings.xml` — add three lines before the closing `</resources>`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">داستان سیندرلا و لالایی کودکانه</string>
    <string name="btn_read">بخوان</string>
    <string name="btn_listen">گوش بده</string>
    <string name="btn_restart">شروع از اول</string>
    <string name="settings_title">تنظیمات</string>
    <string name="font_size_label">اندازه قلم</string>
    <string name="speed_label">سرعت صدا</string>
    <string name="poems_section_title">اشعار</string>
    <string name="poems_section_subtitle">یک شعر تصادفی بخوان</string>
    <string name="poems_empty">هنوز شعری اضافه نشده است</string>
</resources>
```

- [ ] **Step 2: Verify resources compile**

Run: `./gradlew :app:processDebugResources`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/res/values/strings.xml
git commit -m "feat(poems): add poems section string resources"
```

---

### Task 3: Seed poem content assets

**Files:**
- Create: `app/src/main/assets/poems/baran-bahari/poem.json`
- Create: `app/src/main/assets/poems/gorbe-koochak/poem.json`
- Create: `app/src/main/assets/poems/khorshid-mehraban/poem.json`
- Create: `app/src/main/assets/poems/parvaneh-rangi/poem.json`
- Create: `app/src/main/assets/poems/mahtab-shab/poem.json`
- Create: `app/src/main/assets/poems/bagh-e-sabz/poem.json`

**Interfaces:**
- Produces: six poem folders under `app/src/main/assets/poems/`, consumed at runtime by `PoemRepository` once `MainActivity` copies them to external storage (Task 6).

No image files are included (placeholder content only) — `PoemRepository` already handles a missing `image` field (`imagePath` stays `null`), and the viewer (Task 5) shows a fallback icon in that case.

- [ ] **Step 1: Create `app/src/main/assets/poems/baran-bahari/poem.json`**

```json
{
  "id": "baran-bahari",
  "title": "باران بهاری",
  "text": "باران می‌آید نرم نرم\nروی برگ‌های سبز و گرم\nگل‌ها سرشان را بلند کردند\nبوی خاک را نفس کشیدند"
}
```

- [ ] **Step 2: Create `app/src/main/assets/poems/gorbe-koochak/poem.json`**

```json
{
  "id": "gorbe-koochak",
  "title": "گربه کوچولو",
  "text": "گربه‌ی کوچولوی من\nمی‌دود در باغ و چمن\nدنبال یک توپ رنگی\nشاد و بازیگوش، هر روز و هر شب"
}
```

- [ ] **Step 3: Create `app/src/main/assets/poems/khorshid-mehraban/poem.json`**

```json
{
  "id": "khorshid-mehraban",
  "title": "خورشید مهربان",
  "text": "خورشید هر صبح می‌آید\nاز پشت کوه سرک می‌کشد\nبا نور گرم و مهربانش\nدنیا را روشن می‌کند"
}
```

- [ ] **Step 4: Create `app/src/main/assets/poems/parvaneh-rangi/poem.json`**

```json
{
  "id": "parvaneh-rangi",
  "title": "پروانه رنگی",
  "text": "پروانه‌ی رنگارنگ من\nپرواز می‌کند بی هیچ سخن\nاز این گل به آن گل می‌رود\nشادی به باغچه می‌آورد"
}
```

- [ ] **Step 5: Create `app/src/main/assets/poems/mahtab-shab/poem.json`**

```json
{
  "id": "mahtab-shab",
  "title": "مهتاب شب",
  "text": "شب شد و ماه بیرون آمد\nستاره‌ها را همراه آورد\nنور نقره‌ای‌اش تابید\nخواب شیرین برای بچه‌ها آورد"
}
```

- [ ] **Step 6: Create `app/src/main/assets/poems/bagh-e-sabz/poem.json`**

```json
{
  "id": "bagh-e-sabz",
  "title": "باغ سبز",
  "text": "باغ سبز و پر از گل\nدرخت‌هایش بلند و خوش‌دل\nپرنده‌ها آواز می‌خوانند\nبچه‌ها آنجا بازی می‌کنند"
}
```

- [ ] **Step 7: Verify all six files parse as valid JSON**

Run: `for f in app/src/main/assets/poems/*/poem.json; do python3 -c "import json,sys; json.load(open(sys.argv[1]))" "$f" || echo "INVALID: $f"; done`
Expected: no "INVALID" lines printed.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/assets/poems/
git commit -m "feat(poems): seed six placeholder poems"
```

---

### Task 4: PoemsEntryCard and HomeScreen wiring

**Files:**
- Create: `app/src/main/java/ir/sospans/lalastories/ui/home/PoemsEntryCard.kt`
- Modify: `app/src/main/java/ir/sospans/lalastories/ui/home/HomeScreen.kt`

**Interfaces:**
- Consumes: `R.string.poems_section_title`, `R.string.poems_section_subtitle` (Task 2)
- Produces: `@Composable fun PoemsEntryCard(onClick: () -> Unit)`
- Produces: `HomeScreen(stories: List<Story>, onStoryClick: (Story) -> Unit, onSettingsClick: () -> Unit, onPoemsClick: () -> Unit)` — the new `onPoemsClick` parameter is consumed by Task 6 (`AppNavigation`).

- [ ] **Step 1: Create the entry card**

Create `app/src/main/java/ir/sospans/lalastories/ui/home/PoemsEntryCard.kt`:

```kotlin
package ir.sospans.lalastories.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
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
fun PoemsEntryCard(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFFFF6B35), Color(0xFF7C4DFF), Color(0xFF00BFA5))
                )
            )
            .clickable(onClick = onClick)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.AutoStories,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(40.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = stringResource(R.string.poems_section_title),
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )
            Text(
                text = stringResource(R.string.poems_section_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )
        }
    }
}
```

- [ ] **Step 2: Wire it into HomeScreen as a full-width grid item**

Modify `app/src/main/java/ir/sospans/lalastories/ui/home/HomeScreen.kt` — replace the entire file:

```kotlin
package ir.sospans.lalastories.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ir.sospans.lalastories.R
import ir.sospans.lalastories.model.Story

private sealed class HomeGridItem {
    data class StoryItem(val story: Story) : HomeGridItem()
    object Ad : HomeGridItem()
    object PoemsEntry : HomeGridItem()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    stories: List<Story>,
    onStoryClick: (Story) -> Unit,
    onSettingsClick: () -> Unit,
    onPoemsClick: () -> Unit
) {
    var showAd by remember { mutableStateOf(true) }

    val gridItems: List<HomeGridItem> = buildList {
        stories.forEachIndexed { index, story ->
            if (index == 2 && showAd) add(HomeGridItem.Ad)
            add(HomeGridItem.StoryItem(story))
        }
        if (stories.size <= 2 && showAd) add(HomeGridItem.Ad)
        add(HomeGridItem.PoemsEntry)
    }

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
            items(
                items = gridItems,
                key = { item ->
                    when (item) {
                        is HomeGridItem.StoryItem -> item.story.id
                        is HomeGridItem.Ad -> "native_ad"
                        is HomeGridItem.PoemsEntry -> "poems_entry"
                    }
                },
                span = { item ->
                    if (item is HomeGridItem.PoemsEntry) GridItemSpan(maxLineSpan) else GridItemSpan(1)
                }
            ) { item ->
                when (item) {
                    is HomeGridItem.StoryItem -> StoryCard(
                        story = item.story,
                        onClick = { onStoryClick(item.story) }
                    )
                    is HomeGridItem.Ad -> NativeAdCard(onNoAd = { showAd = false })
                    is HomeGridItem.PoemsEntry -> PoemsEntryCard(onClick = onPoemsClick)
                }
            }
        }
    }
}
```

- [ ] **Step 3: Verify it compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD FAILED — `AppNavigation.kt:46` call site `HomeScreen(...)` is now missing the required `onPoemsClick` argument. This is expected; Task 6 fixes the call site. Confirm the *only* error is the missing argument at that call site (no other errors), then proceed.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/ir/sospans/lalastories/ui/home/PoemsEntryCard.kt \
        app/src/main/java/ir/sospans/lalastories/ui/home/HomeScreen.kt
git commit -m "feat(poems): add poems entry card to home screen grid"
```

---

### Task 5: PoemsScreen viewer

**Files:**
- Create: `app/src/main/java/ir/sospans/lalastories/ui/poems/PoemsScreen.kt`

**Interfaces:**
- Consumes: `Poem` and `PoemRepository` (Task 1), `R.string.poems_section_title` / `R.string.poems_empty` (Task 2)
- Produces: `@Composable fun PoemsScreen(poemRepository: PoemRepository, onBack: () -> Unit)` — consumed by Task 6 (`AppNavigation`).

- [ ] **Step 1: Create the screen**

Create `app/src/main/java/ir/sospans/lalastories/ui/poems/PoemsScreen.kt`:

```kotlin
package ir.sospans.lalastories.ui.poems

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import ir.sospans.lalastories.R
import ir.sospans.lalastories.model.Poem
import ir.sospans.lalastories.repository.PoemRepository

private val PoemGradients = listOf(
    listOf(Color(0xFFFF6B35), Color(0xFFFFB199)),
    listOf(Color(0xFF7C4DFF), Color(0xFFB39DFF)),
    listOf(Color(0xFF00BFA5), Color(0xFF64FFDA))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PoemsScreen(poemRepository: PoemRepository, onBack: () -> Unit) {
    val poems = remember { poemRepository.loadPoems().shuffled() }
    var currentIndex by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                TopAppBar(
                    title = {
                        Text(
                            stringResource(R.string.poems_section_title),
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
        if (poems.isEmpty()) {
            Box(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.poems_empty))
            }
            return@Scaffold
        }

        val gradientColors = PoemGradients[currentIndex % PoemGradients.size]

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Brush.verticalGradient(gradientColors))
                .padding(20.dp)
        ) {
            PoemCardContent(
                poem = poems[currentIndex],
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(onClick = { currentIndex = (currentIndex + 1) % poems.size }) {
                    Text("بعدی")
                }
                OutlinedButton(
                    onClick = { currentIndex = (currentIndex - 1 + poems.size) % poems.size },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Text("قبلی")
                }
            }
        }
    }
}

@Composable
private fun PoemCardContent(poem: Poem, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (poem.imagePath != null) {
                AsyncImage(
                    model = poem.imagePath,
                    contentDescription = poem.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(64.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = poem.title,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = poem.text,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
```

- [ ] **Step 2: Verify it compiles in isolation**

Run: `./gradlew :app:compileDebugKotlin`
Expected: Same single missing-argument error as Task 4 Step 3 (this new file itself compiles cleanly; nothing else references it yet).

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/ir/sospans/lalastories/ui/poems/PoemsScreen.kt
git commit -m "feat(poems): add colorful poems viewer screen"
```

---

### Task 6: Wire navigation and asset copying end-to-end

**Files:**
- Modify: `app/src/main/java/ir/sospans/lalastories/navigation/AppNavigation.kt`
- Modify: `app/src/main/java/ir/sospans/lalastories/MainActivity.kt`

**Interfaces:**
- Consumes: `PoemRepository` (Task 1), `HomeScreen(..., onPoemsClick)` (Task 4), `PoemsScreen(poemRepository, onBack)` (Task 5)

- [ ] **Step 1: Add the Poems route and wire HomeScreen/PoemsScreen into AppNavigation**

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
import ir.sospans.lalastories.repository.PoemRepository
import ir.sospans.lalastories.repository.ProgressRepository
import ir.sospans.lalastories.repository.StoryRepository
import ir.sospans.lalastories.ui.detail.StoryDetailScreen
import ir.sospans.lalastories.ui.home.HomeScreen
import ir.sospans.lalastories.ui.listening.ListeningScreen
import ir.sospans.lalastories.ui.poems.PoemsScreen
import ir.sospans.lalastories.ui.reading.ReadingScreen
import ir.sospans.lalastories.ui.settings.SettingsScreen

private const val INTERSTITIAL_PLACEMENT_ID = "e3d7931e-195b-4ee7-b621-e3b1dbd0a569"

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
    object Poems : Screen("poems")
}

@Composable
fun AppNavigation(
    storyRepository: StoryRepository,
    poemRepository: PoemRepository,
    progressRepository: ProgressRepository
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            LaunchedEffect(Unit) {
                Adivery.prepareInterstitialAd(context, INTERSTITIAL_PLACEMENT_ID)
            }
            HomeScreen(
                stories = storyRepository.loadStories(),
                onStoryClick = { story ->
                    if (Adivery.isLoaded(INTERSTITIAL_PLACEMENT_ID)) {
                        Adivery.addPlacementListener(INTERSTITIAL_PLACEMENT_ID, object : AdiveryListener() {
                            override fun onInterstitialAdClosed(placementId: String) {
                                Adivery.removePlacementListener(INTERSTITIAL_PLACEMENT_ID)
                                navController.navigate(Screen.Detail.createRoute(story.id))
                            }
                        })
                        Adivery.showAd(INTERSTITIAL_PLACEMENT_ID)
                    } else {
                        navController.navigate(Screen.Detail.createRoute(story.id))
                    }
                },
                onSettingsClick = { navController.navigate(Screen.Settings.route) },
                onPoemsClick = { navController.navigate(Screen.Poems.route) }
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
        composable(Screen.Poems.route) {
            PoemsScreen(
                poemRepository = poemRepository,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
```

- [ ] **Step 2: Copy bundled poems on first run and pass PoemRepository through**

Modify `app/src/main/java/ir/sospans/lalastories/MainActivity.kt` — replace the entire file:

```kotlin
package ir.sospans.lalastories

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.adivery.sdk.Adivery
import ir.sospans.lalastories.navigation.AppNavigation
import ir.sospans.lalastories.repository.PoemRepository
import ir.sospans.lalastories.repository.ProgressRepository
import ir.sospans.lalastories.repository.StoryRepository
import ir.sospans.lalastories.ui.theme.KidStoriesTheme
import java.io.File

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Adivery.configure(application, "2c1809b1-b6e4-4305-8757-847a73410a18")

        val storiesDir = File(getExternalFilesDir(null), "stories")
        copyBundledStoriesIfNeeded(storiesDir)

        val poemsDir = File(getExternalFilesDir(null), "poems")
        copyBundledPoemsIfNeeded(poemsDir)

        val storyRepository = StoryRepository(storiesDir)
        val poemRepository = PoemRepository(poemsDir)
        val progressRepository = ProgressRepository(this)

        setContent {
            KidStoriesTheme {
                AppNavigation(
                    storyRepository = storyRepository,
                    poemRepository = poemRepository,
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
        if (prefs.getBoolean("poems_copied_v1", false)) return

        copyAssetDir("poems", poemsDir)
        prefs.edit().putBoolean("poems_copied_v1", true).apply()
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

- [ ] **Step 3: Run the full unit test suite**

Run: `./gradlew :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL, all existing tests plus `PoemRepositoryTest` pass.

- [ ] **Step 4: Compile the whole app**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL — no more missing-argument errors.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/ir/sospans/lalastories/navigation/AppNavigation.kt \
        app/src/main/java/ir/sospans/lalastories/MainActivity.kt
git commit -m "feat(poems): wire poems screen into navigation and asset copying"
```

---

### Task 7: Build, install, and manually verify on device

**Files:** none (verification only)

- [ ] **Step 1: Build and install the debug APK**

Run:
```bash
export ANDROID_HOME=/home/matin/Android/Sdk
export PATH="$ANDROID_HOME/platform-tools:$PATH"
./gradlew :app:installDebug
```
Expected: `BUILD SUCCESSFUL`, `Installed on 1 device.`

- [ ] **Step 2: Force a fresh asset copy so the new poems/ folder is picked up**

The `poems_copied_v1` flag only copies once per install; since this may be an upgrade over a previous debug build, clear app data to be sure the copy runs on this verification pass:
```bash
adb shell pm clear ir.sospans.lalastories
adb shell monkey -p ir.sospans.lalastories -c android.intent.category.LAUNCHER 1
```

- [ ] **Step 3: Manually verify on the device**

Check, on the physical device:
1. Home screen shows the existing stories grid unchanged, followed by a full-width colorful "اشعار" banner.
2. Tapping the banner opens the poems viewer directly on some poem (title + text visible, right-aligned Persian text).
3. The gradient background is visibly colorful and differs between poems as you navigate.
4. Tapping "بعدی" (Next) advances to a different poem each time, and tapping it repeatedly eventually wraps back to the first poem shown (loop, no dead end).
5. Tapping "قبلی" (Previous) from the first poem shown wraps to the last poem in the shuffled order (loop the other way).
6. The back arrow is top-left (not top-right), pointing left, consistent with the other screens fixed earlier.
7. Back arrow returns to the home screen.

- [ ] **Step 4: Report results**

If all checks in Step 3 pass, the feature is complete. If any check fails, return to Task 5 (viewer logic) or Task 6 (wiring) as appropriate — do not proceed to further work until this task's checklist passes.
