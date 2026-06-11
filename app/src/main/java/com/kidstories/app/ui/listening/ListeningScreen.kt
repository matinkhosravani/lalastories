package com.kidstories.app.ui.listening

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
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
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(96.dp)
                    )
                }
            }
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
