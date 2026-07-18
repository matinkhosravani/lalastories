package ir.sospans.lalastories.ui.home

import androidx.compose.runtime.Composable
import ir.sospans.lalastories.R

@Composable
fun LullabiesEntryCard(onClick: () -> Unit) {
    SectionEntryCard(
        imageRes = R.drawable.lala,
        aspectRatio = 928f / 1152f,
        contentDescription = "لالایی‌ها",
        onClick = onClick
    )
}
