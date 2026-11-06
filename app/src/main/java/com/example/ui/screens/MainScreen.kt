package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import com.example.R
import com.example.ui.screens.camera.PhotoScreen
import com.example.ui.screens.camera.VideoScreen
import com.example.ui.screens.gallery.GalleryScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.viewmodel.AppViewModel
import com.example.ui.viewmodel.MainTab

@Composable
fun MainScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val selectedTab by viewModel.selectedTab.collectAsState()

    BackHandler(enabled = selectedTab != MainTab.PHOTO) {
        viewModel.selectTab(MainTab.PHOTO)
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("main_navigation_bar")
            ) {
                NavigationBarItem(
                    selected = selectedTab == MainTab.PHOTO,
                    onClick = { viewModel.selectTab(MainTab.PHOTO) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null
                        )
                    },
                    label = { Text(stringResource(R.string.tab_photo)) },
                    modifier = Modifier
                        .semantics {
                            this.contentDescription = "Photo tab. Capture photos with camera."
                            this.role = Role.Tab
                        }
                        .testTag("tab_photo")
                )

                NavigationBarItem(
                    selected = selectedTab == MainTab.VIDEO,
                    onClick = { viewModel.selectTab(MainTab.VIDEO) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = null
                        )
                    },
                    label = { Text(stringResource(R.string.tab_video)) },
                    modifier = Modifier
                        .semantics {
                            this.contentDescription = "Video tab. Record videos with camera."
                            this.role = Role.Tab
                        }
                        .testTag("tab_video")
                )

                NavigationBarItem(
                    selected = selectedTab == MainTab.GALLERY,
                    onClick = { viewModel.selectTab(MainTab.GALLERY) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = null
                        )
                    },
                    label = { Text(stringResource(R.string.tab_gallery)) },
                    modifier = Modifier
                        .semantics {
                            this.contentDescription = "Gallery tab. View captured media."
                            this.role = Role.Tab
                        }
                        .testTag("tab_gallery")
                )

                NavigationBarItem(
                    selected = selectedTab == MainTab.SETTINGS,
                    onClick = { viewModel.selectTab(MainTab.SETTINGS) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null
                        )
                    },
                    label = { Text(stringResource(R.string.tab_settings)) },
                    modifier = Modifier
                        .semantics {
                            this.contentDescription = "Settings tab. App preferences and accessibility."
                            this.role = Role.Tab
                        }
                        .testTag("tab_settings")
                )
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        when (selectedTab) {
            MainTab.PHOTO -> PhotoScreen(
                viewModel = viewModel,
                modifier = Modifier.padding(innerPadding)
            )
            MainTab.VIDEO -> VideoScreen(
                viewModel = viewModel,
                modifier = Modifier.padding(innerPadding)
            )
            MainTab.GALLERY -> GalleryScreen(
                viewModel = viewModel,
                modifier = Modifier.padding(innerPadding)
            )
            MainTab.SETTINGS -> SettingsScreen(
                viewModel = viewModel,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}
