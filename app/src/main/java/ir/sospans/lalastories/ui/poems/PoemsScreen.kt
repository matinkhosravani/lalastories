package ir.sospans.lalastories.ui.poems

import android.view.ViewGroup
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.adivery.sdk.Adivery
import com.adivery.sdk.AdiveryBannerAdView
import com.adivery.sdk.BannerSize
import ir.sospans.lalastories.R
import ir.sospans.lalastories.model.Poem
import ir.sospans.lalastories.repository.PoemRepository

private const val BANNER_PLACEMENT_ID = "aa78c7e1-292a-40fa-973a-6abb2fa7e6db"
private const val INTERSTITIAL_PLACEMENT_ID = "e3d7931e-195b-4ee7-b621-e3b1dbd0a569"
private const val NAV_COUNT_FOR_INTERSTITIAL = 4

private val PoemGradients = listOf(
    listOf(Color(0xFFFF6B35), Color(0xFFFFB199)),
    listOf(Color(0xFF7C4DFF), Color(0xFFB39DFF)),
    listOf(Color(0xFF00BFA5), Color(0xFF64FFDA))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PoemsScreen(poemRepository: PoemRepository, onBack: () -> Unit) {
    val context = LocalContext.current
    var poems by remember { mutableStateOf(poemRepository.loadPoems().shuffled()) }
    var currentIndex by remember { mutableIntStateOf(0) }
    var navigationCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        Adivery.prepareInterstitialAd(context, INTERSTITIAL_PLACEMENT_ID)
    }

    // Poem already renders from the manifest's URL (see placeholder()); this just downloads
    // it in the background so the next offline visit doesn't need the network at all.
    LaunchedEffect(poems.getOrNull(currentIndex)?.id) {
        val current = poems.getOrNull(currentIndex) ?: return@LaunchedEffect
        if (!current.isRemotePending) return@LaunchedEffect
        if (poemRepository.ensurePoemDownloaded(current.id)) {
            poemRepository.loadPoems().firstOrNull { it.id == current.id }?.let { fresh ->
                poems = poems.map { if (it.id == fresh.id) fresh else it }
            }
        }
    }

    fun onNavigate(newIndex: Int) {
        currentIndex = newIndex
        navigationCount++
        if (navigationCount >= NAV_COUNT_FOR_INTERSTITIAL) {
            navigationCount = 0
            if (Adivery.isLoaded(INTERSTITIAL_PLACEMENT_ID)) {
                Adivery.showAd(INTERSTITIAL_PLACEMENT_ID)
                Adivery.prepareInterstitialAd(context, INTERSTITIAL_PLACEMENT_ID)
            }
        }
    }

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
                Button(onClick = { onNavigate((currentIndex + 1) % poems.size) }) {
                    Text("بعدی")
                }
                OutlinedButton(
                    onClick = { onNavigate((currentIndex - 1 + poems.size) % poems.size) },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Text("قبلی")
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
                        .height(240.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
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
