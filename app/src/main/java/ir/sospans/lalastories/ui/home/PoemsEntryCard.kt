package ir.sospans.lalastories.ui.home

import androidx.compose.runtime.Composable
import ir.sospans.lalastories.R

@Composable
fun PoemsEntryCard(onClick: () -> Unit) {
    SectionEntryCard(
        imageRes = R.drawable.home_poems,
        aspectRatio = 360f / 490f,
        contentDescription = "اشعار",
        onClick = onClick
    )
}
