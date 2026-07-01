package ir.sospans.lalastories.ui.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import ir.sospans.lalastories.R

@Composable
fun PoemsEntryCard(onClick: () -> Unit) {
    SectionEntryCard(
        title = stringResource(R.string.poems_section_title),
        subtitle = stringResource(R.string.poems_section_subtitle),
        icon = Icons.Default.AutoStories,
        iconBackgroundColor = Color(0xFF7C4DFF),
        onClick = onClick
    )
}
