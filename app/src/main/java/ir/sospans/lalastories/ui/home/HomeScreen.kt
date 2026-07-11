package ir.sospans.lalastories.ui.home

import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.adivery.sdk.AdiveryBannerAdView
import com.adivery.sdk.BannerSize
import ir.sospans.lalastories.R

private const val BANNER_PLACEMENT_ID = "aa78c7e1-292a-40fa-973a-6abb2fa7e6db"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onStoriesClick: () -> Unit,
    onPoemsClick: () -> Unit,
    onLullabiesClick: () -> Unit,
    onInteractiveStoriesClick: () -> Unit
) {
    Scaffold(
        bottomBar = {
            AndroidView(
                factory = { ctx ->
                    AdiveryBannerAdView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                        setPlacementId(BANNER_PLACEMENT_ID)
                        setBannerSize(BannerSize.SMART_BANNER)
                        loadAd()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Image(
                painter = painterResource(id = R.drawable.home_header),
                contentDescription = stringResource(R.string.app_name),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(768f / 122f),
                contentScale = ContentScale.FillWidth
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    StoriesEntryCard(onClick = onStoriesClick)
                }
                Box(modifier = Modifier.weight(1f)) {
                    PoemsEntryCard(onClick = onPoemsClick)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                LullabiesEntryCard(onClick = onLullabiesClick)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                InteractiveStoriesEntryCard(onClick = onInteractiveStoriesClick)
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
