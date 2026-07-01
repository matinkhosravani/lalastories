package ir.sospans.lalastories.ui.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import ir.sospans.lalastories.R

@Composable
fun StoriesEntryCard(onClick: () -> Unit) {
    SectionEntryCard(
        title = stringResource(R.string.stories_section_title),
        subtitle = stringResource(R.string.stories_section_subtitle),
        icon = Icons.Default.MenuBook,
        iconBackgroundColor = Color(0xFFFF6B35),
        onClick = onClick
    )
}
