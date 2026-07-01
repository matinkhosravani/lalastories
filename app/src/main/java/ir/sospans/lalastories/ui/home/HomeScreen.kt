package ir.sospans.lalastories.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(stories: List<Story>, onStoryClick: (Story) -> Unit, onSettingsClick: () -> Unit) {
    var showAd by remember { mutableStateOf(true) }

    val gridItems: List<HomeGridItem> = buildList {
        stories.forEachIndexed { index, story ->
            if (index == 2 && showAd) add(HomeGridItem.Ad)
            add(HomeGridItem.StoryItem(story))
        }
        if (stories.size <= 2 && showAd) add(HomeGridItem.Ad)
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
                    }
                }
            ) { item ->
                when (item) {
                    is HomeGridItem.StoryItem -> StoryCard(
                        story = item.story,
                        onClick = { onStoryClick(item.story) }
                    )
                    is HomeGridItem.Ad -> NativeAdCard(onNoAd = { showAd = false })
                }
            }
        }
    }
}
