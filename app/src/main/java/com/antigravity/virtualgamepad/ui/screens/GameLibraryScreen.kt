package com.antigravity.virtualgamepad.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.virtualgamepad.model.GameAppInfo
import com.antigravity.virtualgamepad.repository.GameLibraryRepository
import com.antigravity.virtualgamepad.repository.PreferencesRepository
import com.antigravity.virtualgamepad.service.OverlayService
import com.antigravity.virtualgamepad.ui.theme.*

@Composable
fun GameLibraryScreen() {
    val context = LocalContext.current
    val prefsRepo = remember { PreferencesRepository.getInstance(context) }
    val gameRepo = remember { GameLibraryRepository.getInstance(context) }
    val selectedPackages by prefsRepo.selectedPackagesFlow.collectAsState()

    var installedApps by remember { mutableStateOf<List<GameAppInfo>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        installedApps = gameRepo.getUserInstalledApps()
        isLoading = false
    }

    val filteredApps = remember(installedApps, searchQuery, selectedPackages) {
        val list = if (searchQuery.isBlank()) {
            installedApps
        } else {
            installedApps.filter {
                it.appName.contains(searchQuery, ignoreCase = true) ||
                        it.packageName.contains(searchQuery, ignoreCase = true)
            }
        }
        // Show selected apps at the top
        list.sortedWith(
            compareByDescending<GameAppInfo> { selectedPackages.contains(it.packageName.lowercase()) }
                .thenByDescending { it.packageName.contains("kotor", ignoreCase = true) }
                .thenBy { it.appName.lowercase() }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CarbonBlack)
            .padding(16.dp)
    ) {
        Text(
            text = "SELECT GAMES FOR OVERLAY",
            color = NeonCyan,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Text(
            text = "The overlay triggers ONLY when one of your selected games is running on screen",
            color = TextSecondary,
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Info Banner
        Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "🎮 Active Targets: ${selectedPackages.size} game(s) selected. The joystick automatically appears when you open any of these, and vanishes when you leave.",
                color = AccentGold,
                fontSize = 11.sp,
                modifier = Modifier.padding(12.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search installed games (e.g. KOTOR)...", color = TextSecondary, fontSize = 12.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedBorderColor = NeonCyan,
                unfocusedBorderColor = CardDark
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NeonCyan)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredApps, key = { it.packageName }) { app ->
                    val isSelected = selectedPackages.contains(app.packageName.lowercase())
                    val isKotor = app.packageName.contains("kotor", ignoreCase = true)

                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) CardDark else SurfaceDark
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = app.appName,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    if (isKotor) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = AccentGold.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = "KOTOR",
                                                color = AccentGold,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = app.packageName,
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    maxLines = 1
                                )
                            }

                            // 1-tap Launch button
                            IconButton(
                                onClick = {
                                    prefsRepo.setGameSelected(app.packageName, true)
                                    OverlayService.show(context)
                                    gameRepo.launchGame(app.packageName)
                                }
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Launch", tint = NeonCyan)
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            // Enable / Disable switch for this game
                            Switch(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    prefsRepo.setGameSelected(app.packageName, checked)
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan)
                            )
                        }
                    }
                }
            }
        }
    }
}
