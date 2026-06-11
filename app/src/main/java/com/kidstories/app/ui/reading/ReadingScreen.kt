package com.kidstories.app.ui.reading

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kidstories.app.R
import com.kidstories.app.model.Story
import com.kidstories.app.model.StoryProgress
import com.kidstories.app.repository.ProgressRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingScreen(story: Story, progressRepository: ProgressRepository, onBack: () -> Unit) {
    val savedProgress = remember { progressRepository.getProgress(story.id) }
    var currentPage by remember { mutableIntStateOf(savedProgress.lastPage) }
    val totalPages = story.pages.size

    DisposableEffect(currentPage) {
        onDispose {
            progressRepository.saveProgress(
                StoryProgress(story.id, lastPage = currentPage, mode = "read")
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(story.title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "برگشت")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(24.dp)
                .fillMaxSize()
        ) {
            LinearProgressIndicator(
                progress = { currentPage.toFloat() / totalPages },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = story.pages[currentPage - 1].text,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Right,
                modifier = Modifier.weight(1f).fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        progressRepository.resetProgress(story.id)
                        currentPage = 1
                    }
                ) {
                    Text(stringResource(R.string.btn_restart))
                }
                Row {
                    if (currentPage > 1) {
                        OutlinedButton(onClick = { currentPage-- }) { Text("قبلی") }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    if (currentPage < totalPages) {
                        Button(onClick = { currentPage++ }) { Text("بعدی") }
                    }
                }
            }
        }
    }
}
