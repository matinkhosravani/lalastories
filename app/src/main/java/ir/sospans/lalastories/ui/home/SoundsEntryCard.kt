package ir.sospans.lalastories.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.layout.ContentScale
import ir.sospans.lalastories.R

@Composable
fun SoundsEntryCard(onClick: () -> Unit) {
    SectionEntryCard(
        imageRes = R.drawable.home_sounds,
        aspectRatio = 2f / 3f,
        contentDescription = "صداهای آرام‌بخش",
        onClick = onClick,
        contentScale = ContentScale.Fit
    )
}
