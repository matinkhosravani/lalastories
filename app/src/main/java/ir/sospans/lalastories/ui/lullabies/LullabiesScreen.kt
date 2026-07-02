package ir.sospans.lalastories.ui.lullabies

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import ir.sospans.lalastories.R
import ir.sospans.lalastories.model.Lullaby
import ir.sospans.lalastories.ui.home.NativeAdCard

private sealed class LullabiesGridItem {
    data class LullabyItem(val lullaby: Lullaby) : LullabiesGridItem()
    object Ad : LullabiesGridItem()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LullabiesScreen(lullabies: List<Lullaby>, onLullabyClick: (Lullaby) -> Unit, onBack: () -> Unit) {
    var showAd by remember { mutableStateOf(true) }

    val gridItems: List<LullabiesGridItem> = buildList {
        lullabies.forEachIndexed { index, lullaby ->
            if (index == 2 && showAd) add(LullabiesGridItem.Ad)
            add(LullabiesGridItem.LullabyItem(lullaby))
        }
        if (lullabies.size <= 2 && showAd) add(LullabiesGridItem.Ad)
    }

    Scaffold(
        topBar = {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                TopAppBar(
                    title = {
                        Text(
                            stringResource(R.string.lullabies_section_title),
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
        if (lullabies.isEmpty()) {
            Box(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.lullabies_empty))
            }
            return@Scaffold
        }

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
                        is LullabiesGridItem.LullabyItem -> item.lullaby.id
                        is LullabiesGridItem.Ad -> "native_ad"
                    }
                }
            ) { item ->
                when (item) {
                    is LullabiesGridItem.LullabyItem -> LullabyCard(
                        lullaby = item.lullaby,
                        onClick = { onLullabyClick(item.lullaby) }
                    )
                    is LullabiesGridItem.Ad -> NativeAdCard(onNoAd = { showAd = false })
                }
            }
        }
    }
}
