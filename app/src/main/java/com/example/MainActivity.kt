package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.ui.screens.MainScreen
import com.example.ui.screens.WelcomeScreen
import com.example.ui.screens.gallery.MediaEditorScreen
import com.example.ui.screens.gallery.MediaViewerScreen
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.AppViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val prefs by viewModel.preferences.collectAsState()
            val currentScreen by viewModel.currentScreen.collectAsState()

            val themeMode = when (prefs.themeMode) {
                "HIGH_CONTRAST_DARK" -> AppThemeMode.HIGH_CONTRAST_DARK
                "HIGH_CONTRAST_LIGHT" -> AppThemeMode.HIGH_CONTRAST_LIGHT
                "YELLOW_ON_BLACK" -> AppThemeMode.YELLOW_ON_BLACK
                else -> AppThemeMode.SYSTEM
            }

            // Permissions launcher
            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) { permissionsMap ->
                val cameraGranted = permissionsMap[Manifest.permission.CAMERA] == true
                val audioGranted = permissionsMap[Manifest.permission.RECORD_AUDIO] == true

                if (cameraGranted) {
                    viewModel.completeWelcome()
                } else {
                    viewModel.voiceGuidanceManager.announce(
                        "Camera permission is required to capture photos and videos. You can grant it in App Settings.",
                        com.example.domain.accessibility.VoiceGuidanceManager.Priority.CRITICAL
                    )
                }
            }

            MyApplicationTheme(themeMode = themeMode) {
                when (currentScreen) {
                    AppScreen.WELCOME -> {
                        WelcomeScreen(
                            viewModel = viewModel,
                            onRequestPermissions = {
                                val permissions = mutableListOf(
                                    Manifest.permission.CAMERA,
                                    Manifest.permission.RECORD_AUDIO
                                )
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
                                    permissions.add(Manifest.permission.READ_MEDIA_VIDEO)
                                } else {
                                    permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                                }
                                permissionLauncher.launch(permissions.toTypedArray())
                            }
                        )
                    }
                    AppScreen.MAIN -> {
                        MainScreen(
                            viewModel = viewModel,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    AppScreen.MEDIA_VIEWER -> {
                        MediaViewerScreen(
                            viewModel = viewModel,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    AppScreen.MEDIA_EDITOR -> {
                        MediaEditorScreen(
                            viewModel = viewModel,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}
