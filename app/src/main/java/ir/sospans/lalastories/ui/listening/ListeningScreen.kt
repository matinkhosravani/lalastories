package ir.sospans.lalastories.ui.listening

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import com.adivery.sdk.AdiveryBannerAdView
import com.adivery.sdk.BannerSize
import ir.sospans.lalastories.model.Story
import ir.sospans.lalastories.model.StoryProgress
import ir.sospans.lalastories.player.AudioPlayer
import ir.sospans.lalastories.player.TtsPlayer
import ir.sospans.lalastories.repository.ProgressRepository

private data class TextSegment(val startMs: Long, val text: String)

private fun parseVoiceTextMap(lines: List<String>): List<TextSegment> {
    val pattern = Regex("""^(\d+)s:\s*(.+)""")
    val secondToText = sortedMapOf<Int, String>()
    for (line in lines) {
        val match = pattern.find(line.trim()) ?: continue
        val text = match.groupValues[2].substringBefore("/").trim()
        if (text.isNotEmpty()) secondToText[match.groupValues[1].toInt()] = text
    }
    val segments = mutableListOf<TextSegment>()
    var lastText = ""
    for ((sec, text) in secondToText) {
        if (text != lastText) { segments.add(TextSegment(sec * 1000L, text)); lastText = text }
    }
    return segments
}

private fun formatTime(ms: Long): String {
    val totalSecs = ms / 1000
    return "%d:%02d".format(totalSecs / 60, totalSecs % 60)
}

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

    var durationMs by remember { mutableLongStateOf(0L) }
    var currentPositionMs by remember { mutableLongStateOf(savedProgress.lastPositionMs) }
    var isDragging by remember { mutableStateOf(false) }
    var dragPositionMs by remember { mutableLongStateOf(0L) }

    val textSegments = remember(story.id) {
        try {
            context.assets.open("stories/${story.id}/voice_text_map.txt")
                .bufferedReader().readLines().let { parseVoiceTextMap(it) }
        } catch (e: Exception) { emptyList() }
    }
    val textListState = rememberLazyListState()
    val activeIdx by remember {
        derivedStateOf {
            if (textSegments.isEmpty()) 0
            else textSegments.indexOfLast { it.startMs <= currentPositionMs }.coerceAtLeast(0)
        }
    }
    LaunchedEffect(activeIdx) {
        if (textSegments.isNotEmpty()) {
            textListState.animateScrollToItem(maxOf(0, activeIdx - 1))
        }
    }

    LaunchedEffect(Unit) {
        if (useAudio) {
            audioPlayer.load(story.audioPath!!, savedProgress.lastPositionMs)
            durationMs = audioPlayer.getDurationMs()
            currentPositionMs = savedProgress.lastPositionMs
            audioPlayer.setOnCompletionListener {
                isPlaying = false
                currentPositionMs = durationMs
            }
        }
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            if (!isDragging) currentPositionMs = audioPlayer.getCurrentPositionMs()
            delay(500)
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
                        .height(200.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
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
            Spacer(modifier = Modifier.height(12.dp))
            if (textSegments.isNotEmpty()) {
                LazyColumn(
                    state = textListState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(textSegments) { index, segment ->
                        val isActive = index == activeIdx
                        Text(
                            text = segment.text,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                            ),
                            textAlign = TextAlign.Right,
                            modifier = Modifier
                                .fillMaxWidth()
                                .alpha(if (isActive) 1f else 0.35f)
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            if (useAudio && durationMs > 0) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Slider(
                        value = if (isDragging) dragPositionMs.toFloat() / durationMs
                                else currentPositionMs.toFloat() / durationMs,
                        onValueChange = { fraction ->
                            isDragging = true
                            dragPositionMs = (fraction * durationMs).toLong()
                        },
                        onValueChangeFinished = {
                            audioPlayer.seekTo(dragPositionMs)
                            currentPositionMs = dragPositionMs
                            isDragging = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        formatTime(if (isDragging) dragPositionMs else currentPositionMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        formatTime(durationMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

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
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                AndroidView(
                    factory = { ctx ->
                        AdiveryBannerAdView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                            )
                            setPlacementId("aa78c7e1-292a-40fa-973a-6abb2fa7e6db")
                            setBannerSize(BannerSize.BANNER)
                            loadAd()
                        }
                    }
                )
            }
        }
    }
}
