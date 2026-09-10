package ir.sospans.lalastories.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.layout.ContentScale
import ir.sospans.lalastories.R

@Composable
fun LullabiesEntryCard(onClick: () -> Unit) {
    SectionEntryCard(
        imageRes = R.drawable.home_lullaby,
        aspectRatio = 700f / 1003f,
        contentDescription = "لالایی‌ها",
        onClick = onClick,
        contentScale = ContentScale.Fit
    )
}
