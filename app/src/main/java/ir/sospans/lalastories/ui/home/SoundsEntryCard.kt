package ir.sospans.lalastories.ui.home

import androidx.compose.runtime.Composable
import ir.sospans.lalastories.R

@Composable
fun SoundsEntryCard(onClick: () -> Unit) {
    SectionEntryCard(
        imageRes = R.drawable.home_sounds,
        aspectRatio = 600f / 799f,
        contentDescription = "صداهای آرام‌بخش",
        onClick = onClick
    )
}
