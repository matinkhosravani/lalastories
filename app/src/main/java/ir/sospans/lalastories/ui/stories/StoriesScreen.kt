package ir.sospans.lalastories.ui.stories

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import ir.sospans.lalastories.R
import ir.sospans.lalastories.model.Story
import ir.sospans.lalastories.ui.home.NativeAdCard
import ir.sospans.lalastories.ui.home.StoryCard

private sealed class StoriesGridItem {
    data class StoryItem(val story: Story) : StoriesGridItem()
    object Ad : StoriesGridItem()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoriesScreen(stories: List<Story>, onStoryClick: (Story) -> Unit, onBack: () -> Unit) {
    var showAd by remember { mutableStateOf(true) }
    var voiceOnly by remember { mutableStateOf(false) }

    val visibleStories = if (voiceOnly) stories.filter { it.audioPath != null } else stories

    val gridItems: List<StoriesGridItem> = buildList {
        visibleStories.forEachIndexed { index, story ->
            if (index == 2 && showAd) add(StoriesGridItem.Ad)
            add(StoriesGridItem.StoryItem(story))
        }
        if (visibleStories.size <= 2 && showAd) add(StoriesGridItem.Ad)
    }

    Scaffold(
        topBar = {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                TopAppBar(
                    title = {
                        Text(
                            stringResource(R.string.stories_section_title),
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
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    FilterChip(
                        selected = voiceOnly,
                        onClick = { voiceOnly = !voiceOnly },
                        label = { Text(stringResource(R.string.stories_voice_only_filter)) },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_headphones),
                                contentDescription = null,
                                modifier = Modifier.size(FilterChipDefaults.IconSize)
                            )
                        }
                    )
                }
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(
                    items = gridItems,
                    key = { item ->
                        when (item) {
                            is StoriesGridItem.StoryItem -> item.story.id
                            is StoriesGridItem.Ad -> "native_ad"
                        }
                    }
                ) { item ->
                    when (item) {
                        is StoriesGridItem.StoryItem -> StoryCard(
                            story = item.story,
                            onClick = { onStoryClick(item.story) }
                        )
                        is StoriesGridItem.Ad -> NativeAdCard(onNoAd = { showAd = false })
                    }
                }
            }
        }
    }
}
