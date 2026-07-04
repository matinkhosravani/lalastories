package ir.sospans.lalastories.ui.home

import android.graphics.drawable.Drawable
import android.view.ViewGroup
import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.adivery.sdk.Adivery
import com.adivery.sdk.NativeAd

private const val NATIVE_PLACEMENT_ID = "807495c9-d941-4a8f-b820-91e3a1923472"

private data class NativeAdData(
    val headline: String,
    val ctaText: String,
    val imageUrl: String?,
    val imageDrawable: Drawable?,
    val ad: NativeAd
)

// Tries AdiveryNativeAd API first (getImage/getImageUrl), falls back to w0 Drawable fields.
private fun NativeAd.extract(): NativeAdData? {
    return try {
        val cls = javaClass
        val headline = cls.getMethod("getHeadline").invoke(this) as? String ?: return null
        val cta = cls.getMethod("getCallToAction").invoke(this) as? String ?: ""

        val imageUrl = runCatching {
            cls.getMethod("getImageUrl").invoke(this) as? String
        }.getOrNull()

        val imageDrawable = runCatching {
            cls.getMethod("getImage").invoke(this) as? Drawable
        }.getOrNull() ?: runCatching {
            cls.getMethod("getIcon").invoke(this) as? Drawable
        }.getOrNull()

        NativeAdData(headline, cta, imageUrl, imageDrawable, this)
    } catch (_: Exception) { null }
}

private fun NativeAd.click() {
    // AdiveryNativeAd exposes recordClick(); w0 uses obfuscated a(View,List)
    runCatching { javaClass.getMethod("recordClick").invoke(this) }
}

@Composable
fun NativeAdCard(onNoAd: () -> Unit) {
    val context = LocalContext.current
    var adData by remember { mutableStateOf<NativeAdData?>(null) }

    DisposableEffect(Unit) {
        Adivery.requestNativeAd(context, NATIVE_PLACEMENT_ID,
            object : NativeAdCallbackHelper() {
                override fun onLoaded(nativeAd: NativeAd) {
                    val data = nativeAd.extract()
                    if (data != null) {
                        adData = data
                        runCatching { nativeAd.javaClass.getMethod("recordImpression").invoke(nativeAd) }
                    } else {
                        onNoAd()
                    }
                }
                override fun onFailed() { onNoAd() }
            }
        )
        onDispose {}
    }

    val data = adData ?: return

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.75f),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                // Image: prefer URL (loaded by Coil), fall back to Drawable, then placeholder
                when {
                    data.imageUrl != null -> AsyncImage(
                        model = data.imageUrl,
                        contentDescription = data.headline,
                        modifier = Modifier.fillMaxSize()
                    )
                    data.imageDrawable != null -> AndroidView(
                        factory = { ctx ->
                            ImageView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                scaleType = ImageView.ScaleType.CENTER_CROP
                                setImageDrawable(data.imageDrawable)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                    else -> Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    )
                }

                // "تبلیغ" badge
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .background(Color(0xAA7C4DFF), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("تبلیغ", color = Color.White, fontSize = 10.sp)
                }
            }

            // Headline + CTA below the image
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xDD1A1A2E))
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = data.headline,
                    color = Color.White,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = { data.ad.click() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp),
                    contentPadding = PaddingValues(0.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(data.ctaText.ifBlank { "بیشتر بدانید" }, fontSize = 11.sp)
                }
            }
        }
    }
}
