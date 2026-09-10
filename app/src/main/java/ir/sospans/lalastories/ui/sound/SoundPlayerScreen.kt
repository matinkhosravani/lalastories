package ir.sospans.lalastories.ui.sound

import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.adivery.sdk.AdiveryBannerAdView
import com.adivery.sdk.BannerSize
import ir.sospans.lalastories.model.CalmSound
import ir.sospans.lalastories.player.AudioPlayer
import ir.sospans.lalastories.ui.sounds.soundImageRes

private const val BANNER_PLACEMENT_ID = "aa78c7e1-292a-40fa-973a-6abb2fa7e6db"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoundPlayerScreen(sound: CalmSound, onBack: () -> Unit) {
    val audioPlayer = remember { AudioPlayer() }
    var isPlaying by remember { mutableStateOf(false) }
    var ready by remember { mutableStateOf(false) }

    LaunchedEffect(sound.id) {
        val path = sound.audioPath ?: return@LaunchedEffect
        audioPlayer.load(path)
        audioPlayer.setLooping(true)
        ready = true
        audioPlayer.play()
        isPlaying = true
    }

    DisposableEffect(Unit) {
        onDispose { audioPlayer.release() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(sound.title) },
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                val imageRes = soundImageRes(sound.id)
                if (imageRes != null) {
                    Image(
                        painter = painterResource(id = imageRes),
                        contentDescription = sound.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(96.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(text = sound.title, style = MaterialTheme.typography.headlineSmall)

            Spacer(modifier = Modifier.height(40.dp))

            FilledIconButton(
                onClick = {
                    if (!ready) return@FilledIconButton
                    if (isPlaying) {
                        audioPlayer.pause()
                        isPlaying = false
                    } else {
                        audioPlayer.play()
                        isPlaying = true
                    }
                },
                enabled = ready,
                modifier = Modifier.size(88.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "توقف" else "پخش",
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

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
