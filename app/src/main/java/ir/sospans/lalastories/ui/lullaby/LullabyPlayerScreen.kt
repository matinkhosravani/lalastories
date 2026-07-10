package ir.sospans.lalastories.ui.lullaby

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.adivery.sdk.AdiveryBannerAdView
import com.adivery.sdk.BannerSize
import ir.sospans.lalastories.R
import ir.sospans.lalastories.model.Lullaby
import ir.sospans.lalastories.player.AudioPlayer
import ir.sospans.lalastories.repository.LullabyRepository
import kotlinx.coroutines.delay

private const val BANNER_PLACEMENT_ID = "aa78c7e1-292a-40fa-973a-6abb2fa7e6db"
private val SLEEP_TIMER_OPTIONS_MINUTES = listOf(0, 5, 15, 30, 60)

private fun formatTime(ms: Long): String {
    val totalSecs = ms / 1000
    return "%d:%02d".format(totalSecs / 60, totalSecs % 60)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LullabyPlayerScreen(
    lullabies: List<Lullaby>,
    lullabyRepository: LullabyRepository,
    startLullabyId: String,
    onBack: () -> Unit
) {
    var lullabies by remember { mutableStateOf(lullabies) }
    val startIndex = remember { lullabies.indexOfFirst { it.id == startLullabyId }.coerceAtLeast(0) }
    val audioPlayer = remember { AudioPlayer() }

    var currentIndex by remember { mutableIntStateOf(startIndex) }
    var isPlaying by remember { mutableStateOf(false) }
    var repeatEnabled by remember { mutableStateOf(false) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var isDragging by remember { mutableStateOf(false) }
    var dragPositionMs by remember { mutableLongStateOf(0L) }
    var sleepTimerMinutes by remember { mutableIntStateOf(0) }
    var sleepTimerEpoch by remember { mutableIntStateOf(0) }
    var showTimerMenu by remember { mutableStateOf(false) }

    val lullaby = lullabies.getOrNull(currentIndex)

    fun loadLullaby(index: Int, autoplay: Boolean) {
        currentIndex = index
        val path = lullabies.getOrNull(index)?.audioPath
        if (path == null) {
            durationMs = 0L
            currentPositionMs = 0L
            isPlaying = false
            return
        }
        audioPlayer.load(path)
        durationMs = audioPlayer.getDurationMs()
        currentPositionMs = 0L
        audioPlayer.setOnCompletionListener {
            if (repeatEnabled) {
                loadLullaby(index, autoplay = true)
            } else if (lullabies.size > 1) {
                loadLullaby((index + 1) % lullabies.size, autoplay = true)
            } else {
                currentPositionMs = durationMs
                isPlaying = false
            }
        }
        if (autoplay) {
            audioPlayer.play()
            isPlaying = true
        } else {
            isPlaying = false
        }
    }

    LaunchedEffect(Unit) {
        loadLullaby(startIndex, autoplay = false)
    }

    // Lullaby already renders/plays from the manifest's URL (see placeholder()); this just
    // downloads it in the background so the next offline visit doesn't need the network.
    LaunchedEffect(currentIndex) {
        val current = lullabies.getOrNull(currentIndex) ?: return@LaunchedEffect
        if (!current.isRemotePending) return@LaunchedEffect
        if (lullabyRepository.ensureLullabyDownloaded(current.id)) {
            lullabyRepository.loadLullabies().firstOrNull { it.id == current.id }?.let { fresh ->
                lullabies = lullabies.map { if (it.id == fresh.id) fresh else it }
                if (currentIndex < lullabies.size && lullabies[currentIndex].id == fresh.id) {
                    loadLullaby(currentIndex, autoplay = isPlaying)
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { audioPlayer.release() }
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            if (!isDragging) currentPositionMs = audioPlayer.getCurrentPositionMs()
            delay(500)
        }
    }

    LaunchedEffect(sleepTimerEpoch) {
        if (sleepTimerMinutes > 0) {
            delay(sleepTimerMinutes * 60_000L)
            audioPlayer.pause()
            isPlaying = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(lullaby?.title ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "برگشت")
                    }
                }
            )
        }
    ) { padding ->
        if (lullaby == null) {
            Box(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.lullabies_empty))
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(20.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (lullaby.imagePath != null) {
                AsyncImage(
                    model = lullaby.imagePath,
                    contentDescription = lullaby.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.NightsStay,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(72.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = lullaby.text,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Right,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (durationMs > 0) {
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
                        modifier = Modifier.fillMaxWidth()
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
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                IconButton(onClick = { repeatEnabled = !repeatEnabled }) {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = "تکرار",
                        tint = if (repeatEnabled) MaterialTheme.colorScheme.primary
                               else LocalContentColor.current
                    )
                }

                IconButton(
                    onClick = {
                        if (lullaby.audioPath == null) return@IconButton
                        if (isPlaying) {
                            audioPlayer.pause()
                            isPlaying = false
                        } else {
                            audioPlayer.play()
                            isPlaying = true
                        }
                    },
                    enabled = lullaby.audioPath != null,
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "توقف" else "پخش",
                        modifier = Modifier.size(40.dp)
                    )
                }

                Box {
                    IconButton(onClick = { showTimerMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = "تایمر خواب",
                            tint = if (sleepTimerMinutes > 0) MaterialTheme.colorScheme.primary
                                   else LocalContentColor.current
                        )
                    }
                    DropdownMenu(expanded = showTimerMenu, onDismissRequest = { showTimerMenu = false }) {
                        SLEEP_TIMER_OPTIONS_MINUTES.forEach { minutes ->
                            DropdownMenuItem(
                                text = { Text(if (minutes == 0) "خاموش" else "$minutes دقیقه") },
                                onClick = {
                                    sleepTimerMinutes = minutes
                                    sleepTimerEpoch++
                                    showTimerMenu = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                AndroidView(
                    factory = { ctx ->
                        AdiveryBannerAdView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                            )
                            setPlacementId(BANNER_PLACEMENT_ID)
                            setBannerSize(BannerSize.BANNER)
                            loadAd()
                        }
                    }
                )
            }
        }
    }
}
