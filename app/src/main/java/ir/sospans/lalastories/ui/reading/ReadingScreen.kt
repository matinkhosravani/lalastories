package ir.sospans.lalastories.ui.reading

import android.view.ViewGroup
import androidx.compose.animation.*
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.runtime.CompositionLocalProvider
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.adivery.sdk.Adivery
import com.adivery.sdk.AdiveryBannerAdView
import com.adivery.sdk.AdiveryListener
import com.adivery.sdk.BannerSize
import ir.sospans.lalastories.R
import ir.sospans.lalastories.model.Story
import ir.sospans.lalastories.model.StoryProgress
import ir.sospans.lalastories.repository.ProgressRepository
import ir.sospans.lalastories.repository.StoryRepository

private const val BANNER_PLACEMENT_ID = "aa78c7e1-292a-40fa-973a-6abb2fa7e6db"
private const val INTERSTITIAL_PLACEMENT_ID = "e3d7931e-195b-4ee7-b621-e3b1dbd0a569"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun ReadingScreen(
    story: Story,
    storyRepository: StoryRepository,
    progressRepository: ProgressRepository,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    // Text/cover/page images render immediately from the manifest's URLs (Coil/MediaPlayer
    // load a URL the same way they load a local path) - no need to wait for the atomic
    // download. It just runs quietly in the background so the next open is fully offline-safe.
    var resolvedStory by remember(story.id) { mutableStateOf(story) }

    LaunchedEffect(story.id) {
        val available = storyRepository.ensureStoryDownloaded(story.id)
        if (available) {
            storyRepository.loadStories().firstOrNull { it.id == story.id }?.let { resolvedStory = it }
        }
    }

    if (resolvedStory.pages.isEmpty()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(resolvedStory.title) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "برگشت")
                        }
                    }
                )
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        return
    }

    val savedProgress = remember { progressRepository.getProgress(resolvedStory.id) }
    var currentPage by remember { mutableIntStateOf(savedProgress.lastPage) }
    var goingForward by remember { mutableStateOf(true) }
    val totalPages = resolvedStory.pages.size

    LaunchedEffect(Unit) {
        Adivery.prepareInterstitialAd(context, INTERSTITIAL_PLACEMENT_ID)
    }

    fun goToNextPage() {
        if (currentPage == 1 && Adivery.isLoaded(INTERSTITIAL_PLACEMENT_ID)) {
            Adivery.addPlacementListener(INTERSTITIAL_PLACEMENT_ID, object : AdiveryListener() {
                override fun onInterstitialAdClosed(placementId: String) {
                    Adivery.removePlacementListener(INTERSTITIAL_PLACEMENT_ID)
                    goingForward = true
                    currentPage++
                }
            })
            Adivery.showAd(INTERSTITIAL_PLACEMENT_ID)
        } else {
            goingForward = true
            currentPage++
        }
    }

    DisposableEffect(currentPage) {
        onDispose {
            progressRepository.saveProgress(
                StoryProgress(resolvedStory.id, lastPage = currentPage, mode = "read")
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(resolvedStory.title) },
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
                .fillMaxSize()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    LinearProgressIndicator(
                        progress = { currentPage.toFloat() / totalPages },
                        modifier = Modifier.weight(1f)
                    )
                }
                Text(
                    text = "$currentPage / $totalPages",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            var dragTotal by remember { mutableFloatStateOf(0f) }

            AnimatedContent(
                targetState = currentPage,
                transitionSpec = {
                    if (goingForward) {
                        (slideInHorizontally(tween(350)) { it } + fadeIn(tween(350))) togetherWith
                                (slideOutHorizontally(tween(350)) { -it } + fadeOut(tween(350)))
                    } else {
                        (slideInHorizontally(tween(350)) { -it } + fadeIn(tween(350))) togetherWith
                                (slideOutHorizontally(tween(350)) { it } + fadeOut(tween(350)))
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .pointerInput(currentPage) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                if (dragTotal < -80 && currentPage < totalPages) {
                                    goToNextPage()
                                } else if (dragTotal > 80 && currentPage > 1) {
                                    goingForward = false
                                    currentPage--
                                }
                                dragTotal = 0f
                            },
                            onDragCancel = { dragTotal = 0f },
                            onHorizontalDrag = { _, dragAmount -> dragTotal += dragAmount }
                        )
                    },
                label = "page"
            ) { pageIndex ->
                val page = resolvedStory.pages[pageIndex - 1]
                Column(modifier = Modifier.fillMaxSize()) {
                    if (page.imagePath != null) {
                        SubcomposeAsyncImage(
                            model = page.imagePath,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.FillWidth
                        ) {
                            when (painter.state) {
                                is AsyncImagePainter.State.Loading -> {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(180.dp)
                                            .background(MaterialTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator()
                                    }
                                }
                                is AsyncImagePainter.State.Error -> Unit
                                else -> SubcomposeAsyncImageContent()
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        val scrollState = rememberScrollState()
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState)
                        ) {
                            Text(
                                text = page.text,
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Right,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        val needsScrollHint by remember {
                            derivedStateOf { scrollState.maxValue > 0 && scrollState.value < scrollState.maxValue }
                        }
                        if (needsScrollHint) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .height(36.dp)
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color.Transparent, MaterialTheme.colorScheme.background)
                                        )
                                    )
                            )
                            val infiniteTransition = rememberInfiniteTransition(label = "scrollHint")
                            val bounce by infiniteTransition.animateFloat(
                                initialValue = 0f,
                                targetValue = 8f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(550),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "scrollHintBounce"
                            )
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .offset(y = bounce.dp)
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Right side in RTL (first child = start = right)
                if (currentPage < totalPages) {
                    Button(onClick = { goToNextPage() }) { Text("→ بعدی") }
                } else {
                    TextButton(
                        onClick = {
                            goingForward = false
                            progressRepository.resetProgress(resolvedStory.id)
                            currentPage = 1
                        }
                    ) {
                        Text(stringResource(R.string.btn_restart))
                    }
                }
                // Left side in RTL (second child = end = left)
                if (currentPage > 1) {
                    OutlinedButton(onClick = {
                        goingForward = false
                        currentPage--
                    }) { Text("قبلی ←") }
                } else {
                    TextButton(
                        onClick = {
                            progressRepository.resetProgress(resolvedStory.id)
                            currentPage = 1
                        }
                    ) {
                        Text(stringResource(R.string.btn_restart))
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
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
