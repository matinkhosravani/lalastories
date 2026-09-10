package ir.sospans.lalastories.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.layout.ContentScale
import ir.sospans.lalastories.R

@Composable
fun LullabiesEntryCard(onClick: () -> Unit) {
    SectionEntryCard(
        imageRes = R.drawable.home_lullaby,
        aspectRatio = 2f / 3f,
        contentDescription = "لالایی‌ها",
        onClick = onClick,
        contentScale = ContentScale.Fit
    )
}
