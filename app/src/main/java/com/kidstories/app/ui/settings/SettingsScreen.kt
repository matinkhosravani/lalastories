package com.kidstories.app.ui.settings

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kidstories.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }
    var ttsSpeed by remember { mutableFloatStateOf(prefs.getFloat("tts_speed", 1.0f)) }
    var fontSize by remember { mutableFloatStateOf(prefs.getFloat("font_size", 20f)) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
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
            Text(stringResource(R.string.speed_label), style = MaterialTheme.typography.titleMedium)
            Slider(
                value = ttsSpeed,
                onValueChange = {
                    ttsSpeed = it
                    prefs.edit().putFloat("tts_speed", it).apply()
                },
                valueRange = 0.5f..2.0f,
                steps = 5
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(stringResource(R.string.font_size_label), style = MaterialTheme.typography.titleMedium)
            Slider(
                value = fontSize,
                onValueChange = {
                    fontSize = it
                    prefs.edit().putFloat("font_size", it).apply()
                },
                valueRange = 14f..32f,
                steps = 5
            )
        }
    }
}
