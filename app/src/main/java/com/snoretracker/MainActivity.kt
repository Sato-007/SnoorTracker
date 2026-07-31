package com.snoretracker

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.snoretracker.ui.screens.HistoryScreen
import com.snoretracker.ui.screens.TrackerScreen
import com.snoretracker.ui.theme.SnoreTrackerTheme
import com.snoretracker.ui.viewmodel.SnoreViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: SnoreViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val mic = permissions[Manifest.permission.RECORD_AUDIO] ?: false
        var notif = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notif = permissions[Manifest.permission.POST_NOTIFICATIONS] ?: false
        }
        viewModel.setPermissions(mic, notif)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        requestPermissionLauncher.launch(permissions.toTypedArray())

        setContent {
            SnoreTrackerTheme {
                val navController = rememberNavController()
                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            val navBackStackEntry by navController.currentBackStackEntryAsState()
                            val currentRoute = navBackStackEntry?.destination?.route

                            NavigationBarItem(
                                icon = { Text("🎙️") },
                                label = { Text("Tracker") },
                                selected = currentRoute == "tracker",
                                onClick = {
                                    navController.navigate("tracker") {
                                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                            NavigationBarItem(
                                icon = { Text("📋") },
                                label = { Text("History") },
                                selected = currentRoute == "history",
                                onClick = {
                                    navController.navigate("history") {
                                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "tracker",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("tracker") { TrackerScreen(viewModel) }
                        composable("history") { HistoryScreen(viewModel) }
                    }
                }
            }
        }
    }
}
