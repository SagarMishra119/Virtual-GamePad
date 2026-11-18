package com.antigravity.virtualgamepad

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.content.ContextCompat
import com.antigravity.virtualgamepad.ui.screens.GameLibraryScreen
import com.antigravity.virtualgamepad.ui.screens.HomeScreen
import com.antigravity.virtualgamepad.ui.screens.LayoutEditorScreen
import com.antigravity.virtualgamepad.ui.screens.SettingsScreen
import com.antigravity.virtualgamepad.ui.theme.CarbonBlack
import com.antigravity.virtualgamepad.ui.theme.CardDark
import com.antigravity.virtualgamepad.ui.theme.NeonCyan
import com.antigravity.virtualgamepad.ui.theme.SurfaceDark
import com.antigravity.virtualgamepad.ui.theme.TextSecondary
import com.antigravity.virtualgamepad.ui.theme.VirtualGamepadTheme

enum class Screen(val title: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    GAMES("Games", Icons.Default.SportsEsports),
    LAYOUT("Layout", Icons.Default.Tune),
    SETTINGS("Settings", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {

    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Notification permission callback
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            VirtualGamepadTheme {
                var currentScreen by remember { mutableStateOf(Screen.HOME) }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = CarbonBlack,
                    bottomBar = {
                        NavigationBar(
                            containerColor = SurfaceDark,
                            contentColor = NeonCyan
                        ) {
                            Screen.values().forEach { screen ->
                                NavigationBarItem(
                                    selected = currentScreen == screen,
                                    onClick = { currentScreen = screen },
                                    icon = {
                                        Icon(
                                            imageVector = screen.icon,
                                            contentDescription = screen.title
                                        )
                                    },
                                    label = { Text(screen.title) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = CarbonBlack,
                                        selectedTextColor = NeonCyan,
                                        indicatorColor = NeonCyan,
                                        unselectedIconColor = TextSecondary,
                                        unselectedTextColor = TextSecondary
                                    )
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        color = CarbonBlack
                    ) {
                        when (currentScreen) {
                            Screen.HOME -> HomeScreen(
                                onNavigateToGames = { currentScreen = Screen.GAMES },
                                onNavigateToLayout = { currentScreen = Screen.LAYOUT }
                            )
                            Screen.GAMES -> GameLibraryScreen()
                            Screen.LAYOUT -> LayoutEditorScreen()
                            Screen.SETTINGS -> SettingsScreen()
                        }
                    }
                }
            }
        }
    }
}
