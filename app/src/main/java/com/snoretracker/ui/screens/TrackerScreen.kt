package com.snoretracker.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.foundation.clickable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.snoretracker.service.SnoreTrackerService
import com.snoretracker.ui.components.WaveformVisualizer
import com.snoretracker.ui.viewmodel.SnoreViewModel
import com.snoretracker.ui.viewmodel.TrackerUiState
import com.snoretracker.ui.viewmodel.TrackingStatus
import com.snoretracker.util.PermissionHelper

@Composable
fun TrackerScreen(
    viewModel: SnoreViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.trackerUiState.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (state.trackingStatus) {
            TrackingStatus.IDLE -> IdleState(state, viewModel, context)
            TrackingStatus.ACTIVE -> ActiveState(state, viewModel, context)
            TrackingStatus.SUMMARY -> SummaryState(state, viewModel)
        }
    }
}

@Composable
private fun IdleState(state: TrackerUiState, viewModel: SnoreViewModel, context: Context) {
    Text("Snore Tracker", style = MaterialTheme.typography.headlineMedium)
    Spacer(modifier = Modifier.height(32.dp))
    
    Text("Sensitivity Threshold: ${state.sensitivityThreshold.toInt()} dB", style = MaterialTheme.typography.titleMedium)
    Slider(
        value = state.sensitivityThreshold,
        onValueChange = { viewModel.setSensitivity(it) },
        valueRange = 30f..90f,
        modifier = Modifier.fillMaxWidth()
    )
    
    Spacer(modifier = Modifier.height(24.dp))
    
    Text("Duration Range: ${state.minSnoreDurationMs}ms - ${state.maxSnoreDurationMs}ms", style = MaterialTheme.typography.titleMedium)
    RangeSlider(
        value = state.minSnoreDurationMs.toFloat()..state.maxSnoreDurationMs.toFloat(),
        onValueChange = { range -> viewModel.setDurationRange(range.start.toLong(), range.endInclusive.toLong()) },
        valueRange = 300f..3000f,
        modifier = Modifier.fillMaxWidth()
    )
    
    Spacer(modifier = Modifier.height(24.dp))
    
    Text("Silence Interval: ${state.silenceCooldownMs} ms", style = MaterialTheme.typography.titleMedium)
    Slider(
        value = state.silenceCooldownMs.toFloat(),
        onValueChange = { viewModel.setSilenceCooldown(it.toLong()) },
        valueRange = 100f..3000f,
        steps = 28,
        modifier = Modifier.fillMaxWidth()
    )
    
    Spacer(modifier = Modifier.height(24.dp))

    Text("Microphone Source", style = MaterialTheme.typography.titleMedium)
    var expanded by remember { mutableStateOf(false) }
    @OptIn(ExperimentalMaterial3Api::class)
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = if (state.audioSource == android.media.MediaRecorder.AudioSource.MIC) "Standard Mic (MIC)" else "Far-Field (CAMCORDER)",
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Standard Mic (MIC)") },
                onClick = {
                    viewModel.setAudioSource(android.media.MediaRecorder.AudioSource.MIC)
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("Far-Field (CAMCORDER)") },
                onClick = {
                    viewModel.setAudioSource(android.media.MediaRecorder.AudioSource.CAMCORDER)
                    expanded = false
                }
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { viewModel.setEnableZcrFilter(!state.enableZcrFilter) }
    ) {
        Checkbox(
            checked = state.enableZcrFilter,
            onCheckedChange = { viewModel.setEnableZcrFilter(it) }
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("Filter High-Pitch / Cough Noise (ZCR)", style = MaterialTheme.typography.bodyMedium)
    }
    
    Spacer(modifier = Modifier.height(32.dp))
    Button(
        onClick = {
            if (PermissionHelper.hasMicrophonePermission(context)) {
                val intent = Intent(context, SnoreTrackerService::class.java)
                ContextCompat.startForegroundService(context, intent)
            }
        },
        modifier = Modifier.height(56.dp).padding(horizontal = 24.dp)
    ) {
        Text("START TRACKING", style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun ActiveState(state: TrackerUiState, viewModel: SnoreViewModel, context: Context) {
    Text("Tracking Active", style = MaterialTheme.typography.headlineMedium)
    Text(
        text = "${state.currentDb.toInt()} dB",
        style = MaterialTheme.typography.displayLarge.copy(fontFamily = com.snoretracker.ui.theme.JetBrainsMono)
    )
    
    Spacer(modifier = Modifier.height(32.dp))
    
    WaveformVisualizer(
        amplitudes = state.amplitudeHistory,
        threshold = state.sensitivityThreshold
    )
    
    Spacer(modifier = Modifier.height(32.dp))
    
    val seconds = (state.elapsedTimeMs / 1000) % 60
    val minutes = (state.elapsedTimeMs / (1000 * 60)) % 60
    val hours = (state.elapsedTimeMs / (1000 * 60 * 60))
    Text(
        text = String.format("%02d:%02d:%02d", hours, minutes, seconds),
        style = MaterialTheme.typography.titleLarge.copy(fontFamily = com.snoretracker.ui.theme.JetBrainsMono)
    )
    
    Spacer(modifier = Modifier.height(48.dp))
    
    Button(
        onClick = {
            val intent = Intent(context, SnoreTrackerService::class.java)
            context.stopService(intent)
            viewModel.markSessionStopped()
        },
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
        modifier = Modifier.height(56.dp).padding(horizontal = 24.dp)
    ) {
        Text("STOP TRACKING", style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun SummaryState(state: TrackerUiState, viewModel: SnoreViewModel) {
    Text("Sleep Session Summary", style = MaterialTheme.typography.headlineLarge)
    Spacer(modifier = Modifier.height(32.dp))
    
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Total Snore Events: ${state.snoreEventCount}", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Peak Volume: ${state.peakDb.toInt()} dB", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            val durationSecs = state.totalSnoreDurationMs / 1000
            Text("Total Snoring Time: $durationSecs seconds", style = MaterialTheme.typography.titleMedium)
        }
    }
    
    Spacer(modifier = Modifier.height(48.dp))
    
    Button(
        onClick = { viewModel.resetTracker() },
        modifier = Modifier.height(56.dp).padding(horizontal = 24.dp)
    ) {
        Text("START NEW SESSION", style = MaterialTheme.typography.labelLarge)
    }
}
