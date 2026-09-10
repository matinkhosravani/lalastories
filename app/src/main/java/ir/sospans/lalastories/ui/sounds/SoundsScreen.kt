package ir.sospans.lalastories.ui.sounds

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import ir.sospans.lalastories.R
import ir.sospans.lalastories.model.CalmSound

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoundsScreen(sounds: List<CalmSound>, onSoundClick: (CalmSound) -> Unit, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                TopAppBar(
                    title = {
                        Text(
                            stringResource(R.string.sounds_section_title),
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
        if (sounds.isEmpty()) {
            Box(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.sounds_empty))
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
            items(items = sounds, key = { it.id }) { sound ->
                SoundCard(sound = sound, onClick = { onSoundClick(sound) })
            }
        }
    }
}
