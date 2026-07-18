package ir.sospans.lalastories.ui.interactivestory

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import ir.sospans.lalastories.R
import ir.sospans.lalastories.model.InteractiveStory
import ir.sospans.lalastories.model.InteractiveStoryProgress
import ir.sospans.lalastories.model.StoryNode
import ir.sospans.lalastories.repository.InteractiveStoryRepository
import ir.sospans.lalastories.repository.ProgressRepository
import kotlinx.coroutines.delay

private val CorrectColor = Color(0xFF4CAF50)
private val WrongColor = Color(0xFFE53935)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InteractiveStoryPlayerScreen(
    story: InteractiveStory,
    interactiveStoryRepository: InteractiveStoryRepository,
    progressRepository: ProgressRepository,
    onBack: () -> Unit
) {
    var resolvedStory by remember(story.id) { mutableStateOf(story) }

    LaunchedEffect(story.id) {
        val available = interactiveStoryRepository.ensureInteractiveStoryDownloaded(story.id)
        if (available) {
            interactiveStoryRepository.loadInteractiveStories().firstOrNull { it.id == story.id }?.let { resolvedStory = it }
        }
    }

    val savedProgress = remember { progressRepository.getInteractiveProgress(resolvedStory.id) }
    var currentNodeId by remember {
        mutableStateOf(savedProgress.currentNodeId?.takeIf { it in resolvedStory.nodes } ?: resolvedStory.startNodeId)
    }

    DisposableEffect(currentNodeId) {
        onDispose {
            progressRepository.saveInteractiveProgress(InteractiveStoryProgress(resolvedStory.id, currentNodeId))
        }
    }

    val currentNode = resolvedStory.nodes[currentNodeId] ?: resolvedStory.nodes.getValue(resolvedStory.startNodeId)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(resolvedStory.title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "برگشت")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val node = currentNode) {
                is StoryNode.ContentNode -> ContentNodeView(node, onNext = { currentNodeId = node.next })
                is StoryNode.ChoiceNode -> ChoiceNodeView(node, onChoice = { next -> currentNodeId = next })
                is StoryNode.QuizNode -> QuizNodeView(node, onCorrect = { currentNodeId = node.next })
                is StoryNode.EndNode -> EndNodeView(
                    node = node,
                    onRestart = {
                        progressRepository.clearInteractiveProgress(resolvedStory.id)
                        currentNodeId = resolvedStory.startNodeId
                    },
                    onHome = onBack
                )
            }
        }
    }
}

@Composable
private fun ContentNodeView(node: StoryNode.ContentNode, onNext: () -> Unit) {
    Column(modifier = Modifier.padding(24.dp).fillMaxSize()) {
        if (node.imagePath != null) {
            AsyncImage(
                model = node.imagePath,
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        Box(modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
            Text(
                text = node.text,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Text("بعدی ←")
        }
    }
}

@Composable
private fun ChoiceNodeView(node: StoryNode.ChoiceNode, onChoice: (String) -> Unit) {
    Column(
        modifier = Modifier.padding(24.dp).fillMaxSize().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (node.imagePath != null) {
            AsyncImage(
                model = node.imagePath,
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        Text(node.prompt, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(24.dp))
        node.options.forEach { option ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clickable { onChoice(option.next) },
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (option.imagePath != null) {
                        AsyncImage(
                            model = option.imagePath,
                            contentDescription = option.label,
                            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Text(option.label, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

@Composable
private fun QuizNodeView(node: StoryNode.QuizNode, onCorrect: () -> Unit) {
    var lastTappedIndex by remember(node.id) { mutableStateOf<Int?>(null) }
    var advancing by remember(node.id) { mutableStateOf(false) }

    LaunchedEffect(node.id, lastTappedIndex) {
        val index = lastTappedIndex ?: return@LaunchedEffect
        if (node.answers[index].isCorrect) {
            advancing = true
            delay(700)
            onCorrect()
        }
    }

    Column(
        modifier = Modifier.padding(24.dp).fillMaxSize().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (node.imagePath != null) {
            AsyncImage(
                model = node.imagePath,
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        Text(node.question, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(24.dp))
        node.answers.forEachIndexed { index, answer ->
            val isTapped = lastTappedIndex == index
            val backgroundColor = when {
                !isTapped -> MaterialTheme.colorScheme.surfaceVariant
                answer.isCorrect -> CorrectColor
                else -> WrongColor
            }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clickable(enabled = !advancing) { lastTappedIndex = index },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = backgroundColor)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (answer.imagePath != null) {
                        AsyncImage(
                            model = answer.imagePath,
                            contentDescription = answer.text,
                            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                    if (answer.text != null) {
                        Text(answer.text, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun EndNodeView(node: StoryNode.EndNode, onRestart: () -> Unit, onHome: () -> Unit) {
    Column(
        modifier = Modifier.padding(24.dp).fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (node.imagePath != null) {
            AsyncImage(
                model = node.imagePath,
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        Box(modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
            Text(node.text, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onRestart,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Text(stringResource(R.string.btn_restart))
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = onHome,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Text(stringResource(R.string.btn_back_home))
        }
    }
}
