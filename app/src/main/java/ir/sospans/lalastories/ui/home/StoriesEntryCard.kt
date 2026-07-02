package ir.sospans.lalastories.ui.home

import androidx.compose.runtime.Composable
import ir.sospans.lalastories.R

@Composable
fun StoriesEntryCard(onClick: () -> Unit) {
    SectionEntryCard(
        imageRes = R.drawable.home_stories,
        aspectRatio = 370f / 490f,
        contentDescription = "داستان‌ها",
        onClick = onClick
    )
}
