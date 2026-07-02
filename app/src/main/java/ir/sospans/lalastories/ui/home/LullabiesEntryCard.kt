package ir.sospans.lalastories.ui.home

import androidx.compose.runtime.Composable
import ir.sospans.lalastories.R

@Composable
fun LullabiesEntryCard(onClick: () -> Unit) {
    SectionEntryCard(
        imageRes = R.drawable.home_lullaby,
        aspectRatio = 740f / 719f,
        contentDescription = "لالایی‌ها",
        onClick = onClick
    )
}
