package com.tapboard.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.tapboard.app.ui.screens.ConnectScreen
import com.tapboard.app.ui.screens.HelpScreen
import com.tapboard.app.ui.screens.KeyboardScreen
import com.tapboard.app.ui.screens.OnboardingScreen
import com.tapboard.app.ui.screens.SettingsScreen
import com.tapboard.app.ui.screens.TouchpadScreen

private enum class Dest(val route: String, val label: String) {
    Connect("connect", "Connect"),
    Touchpad("touchpad", "Pad"),
    Keyboard("keyboard", "Keys"),
    Settings("settings", "Settings")
}

@Composable
fun TapBoardNavHost(viewModel: TapBoardViewModel) {
    val onboardingDone by viewModel.onboardingDone.collectAsState()
    if (!onboardingDone) {
        OnboardingScreen(onDone = { viewModel.completeOnboarding() })
        return
    }

    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val current = backStack?.destination?.route
    val showBottom = Dest.entries.any { it.route == current } || current == "help"

    Scaffold(
        bottomBar = {
            if (showBottom && current != "help") {
                NavigationBar {
                    Dest.entries.forEach { dest ->
                        val selected = current == dest.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(dest.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = when (dest) {
                                        Dest.Connect -> Icons.Outlined.Link
                                        Dest.Touchpad -> Icons.Outlined.TouchApp
                                        Dest.Keyboard -> Icons.Outlined.Keyboard
                                        Dest.Settings -> Icons.Outlined.Settings
                                    },
                                    contentDescription = dest.label
                                )
                            },
                            label = { Text(dest.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Dest.Connect.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Dest.Connect.route) {
                ConnectScreen(
                    viewModel = viewModel,
                    onOpenHelp = { navController.navigate("help") }
                )
            }
            composable(Dest.Touchpad.route) { TouchpadScreen(viewModel) }
            composable(Dest.Keyboard.route) { KeyboardScreen(viewModel) }
            composable(Dest.Settings.route) {
                SettingsScreen(
                    viewModel = viewModel,
                    onOpenHelp = { navController.navigate("help") }
                )
            }
            composable("help") {
                HelpScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
