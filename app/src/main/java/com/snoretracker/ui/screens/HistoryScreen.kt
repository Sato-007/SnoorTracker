package com.snoretracker.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.unit.dp
import com.snoretracker.data.SnoreSession
import com.snoretracker.ui.viewmodel.SnoreViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryScreen(
    viewModel: SnoreViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.historyUiState.collectAsState()

    if (state.selectedSession != null) {
        SessionDetail(
            session = state.selectedSession!!,
            onBack = { viewModel.selectHistorySession(null) },
            onDelete = { viewModel.deleteSession(it) }
        )
    } else {
        SessionList(
            sessions = state.sessions,
            isLoading = state.isLoading,
            onSessionClick = { viewModel.selectHistorySession(it) },
            onDeleteAllClick = { viewModel.deleteAllSessions() },
            modifier = modifier
        )
    }
}

@Composable
private fun SessionList(
    sessions: List<SnoreSession>,
    isLoading: Boolean,
    onSessionClick: (SnoreSession) -> Unit,
    onDeleteAllClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeleteAllDialog by remember { mutableStateOf(false) }

    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text("Delete All History") },
            text = { Text("Are you sure you want to delete all sleep sessions? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteAllClick()
                        showDeleteAllDialog = false
                    }
                ) {
                    Text("Delete All", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Sleep History", style = MaterialTheme.typography.headlineMedium)
            if (sessions.isNotEmpty() && !isLoading) {
                IconButton(onClick = { showDeleteAllDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete All",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        
        if (isLoading) {
            CircularProgressIndicator()
        } else if (sessions.isEmpty()) {
            Text("No sessions recorded yet.", style = MaterialTheme.typography.bodyLarge)
        } else {
            LazyColumn {
                items(sessions) { session ->
                    SessionCard(session = session, onClick = { onSessionClick(session) })
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun SessionCard(session: SnoreSession, onClick: () -> Unit) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(dateFormat.format(Date(session.startTime)), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Events: ${session.totalSnoreEvents}", style = MaterialTheme.typography.bodyMedium)
                Text("Peak: ${session.peakDb.toInt()} dB", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun SessionDetail(session: SnoreSession, onBack: () -> Unit, onDelete: (SnoreSession) -> Unit) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy - HH:mm", Locale.getDefault())
    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Button(onClick = onBack) { Text("Back") }
            Button(onClick = { onDelete(session) }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                Text("Delete")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(dateFormat.format(Date(session.startTime)), style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))
        
        Text("Events Timeline:", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        
        LazyColumn {
            items(session.events) { event ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(timeFormat.format(Date(event.timestamp)), style = MaterialTheme.typography.bodyMedium.copy(fontFamily = com.snoretracker.ui.theme.JetBrainsMono))
                    Text("${event.peakDb.toInt()} dB", style = MaterialTheme.typography.bodyMedium.copy(fontFamily = com.snoretracker.ui.theme.JetBrainsMono))
                }
                Divider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 0.5.dp)
            }
        }
    }
}
