package com.antigravity.virtualgamepad.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.antigravity.virtualgamepad.model.InputMode
import com.antigravity.virtualgamepad.repository.GameLibraryRepository
import com.antigravity.virtualgamepad.repository.PreferencesRepository
import com.antigravity.virtualgamepad.service.GamepadAccessibilityService
import com.antigravity.virtualgamepad.service.OverlayService
import com.antigravity.virtualgamepad.shizuku.ShizukuManager
import com.antigravity.virtualgamepad.ui.theme.*

@Composable
fun HomeScreen(
    onNavigateToGames: () -> Unit,
    onNavigateToLayout: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val prefsRepo = remember { PreferencesRepository.getInstance(context) }
    val gameRepo = remember { GameLibraryRepository.getInstance(context) }

    val settings by prefsRepo.settingsFlow.collectAsState()
    val selectedPackages by prefsRepo.selectedPackagesFlow.collectAsState()
    val isRunning by OverlayService.isRunningFlow.collectAsState()
    val isArmed by prefsRepo.isArmedFlow.collectAsState()
    val isActive = isRunning || isArmed

    val isShizukuAvailable by ShizukuManager.isAvailable.collectAsState()
    val hasShizukuPermission by ShizukuManager.hasPermission.collectAsState()

    var hasOverlayPerm by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Settings.canDrawOverlays(context) else true
        )
    }
    var hasA11yPerm by remember {
        mutableStateOf(GamepadAccessibilityService.isRunning())
    }
    var showA11yDisclosureDialog by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasOverlayPerm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Settings.canDrawOverlays(context) else true
                hasA11yPerm = GamepadAccessibilityService.isRunning()
                ShizukuManager.checkStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (showA11yDisclosureDialog) {
        AlertDialog(
            onDismissRequest = { showA11yDisclosureDialog = false },
            title = {
                Text(
                    text = "Accessibility Service Notice",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = NeonCyan
                )
            },
            text = {
                Column {
                    Text(
                        text = "Virtual Gamepad uses the AccessibilityService API for the following functionality:",
                        fontSize = 13.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "• Synthetic Touch Input: Dispatches simulated touch and drag gestures onto your game canvas when you move the on-screen virtual joystick.",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "• No Data Collection: We do NOT read screen content, inspect passwords, capture keystrokes, record personal information, or monitor other apps.",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "• Full User Control: The service operates exclusively during gameplay when the overlay is active and can be turned off at any time.",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showA11yDisclosureDialog = false
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                ) {
                    Text("Agree & Continue", color = CarbonBlack, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showA11yDisclosureDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = SurfaceDark,
            shape = RoundedCornerShape(16.dp)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CarbonBlack)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // App Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "VIRTUAL GAMEPAD",
                        color = NeonCyan,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(NeonCyan.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "1.2.1",
                            color = NeonCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(
                    text = "Floating Joystick Overlay for KOTOR",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (isActive) AccentGreen.copy(alpha = 0.2f) else CardDark)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (isActive) "ACTIVE" else "OFF",
                    color = if (isActive) AccentGreen else TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Required Permissions Card
        Text(
            text = "REQUIRED PERMISSIONS (NO ROOT / NO PC NEEDED)",
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(6.dp))

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // 1. Overlay Permission
                PermissionItem(
                    title = "1. Display Over Other Apps",
                    description = "Allows the joystick to float on screen over your game",
                    isGranted = hasOverlayPerm,
                    onGrantClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                        }
                    }
                )

                HorizontalDivider(color = CardDark, modifier = Modifier.padding(vertical = 10.dp))

                // 2. Accessibility Permission
                PermissionItem(
                    title = "2. Accessibility Service",
                    description = "Required to synthesize movement touches in the game without PC/Shizuku",
                    isGranted = hasA11yPerm,
                    onGrantClick = {
                        if (hasA11yPerm) {
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                        } else {
                            showA11yDisclosureDialog = true
                        }
                    }
                )

                HorizontalDivider(color = CardDark, modifier = Modifier.padding(vertical = 10.dp))

                // 3. Shizuku (Completely optional)
                PermissionItem(
                    title = "3. Shizuku (OPTIONAL - NOT REQUIRED)",
                    description = "Only needed if you want native virtual hardware controller emulation",
                    isGranted = isShizukuAvailable && hasShizukuPermission,
                    onGrantClick = {
                        if (isShizukuAvailable) {
                            ShizukuManager.requestPermission()
                        } else {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app"))
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Service Master Toggle (Ultimate authority button)
        Button(
            onClick = {
                if (!hasOverlayPerm) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(intent)
                    }
                } else {
                    if (isActive) {
                        // Ultimate authority: completely turns off overlay and terminates service!
                        OverlayService.stop(context)
                    } else {
                        OverlayService.start(context)
                    }
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isActive) AccentRed else NeonCyan
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Icon(
                imageVector = if (isActive) Icons.Default.PowerSettingsNew else Icons.Default.PlayArrow,
                contentDescription = null,
                tint = if (isActive) Color.White else CarbonBlack
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (!hasOverlayPerm) "GRANT OVERLAY PERMISSION" else if (isActive) "TURN OFF OVERLAY SERVICE" else "START OVERLAY SERVICE FOR GAMES",
                color = if (isActive) Color.White else CarbonBlack,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }

        if (isActive) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "✓ Service is running. The joystick is active for your game. Tap 'TURN OFF' anytime to remove it completely.",
                color = AccentGreen,
                fontSize = 11.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Input Mode Card
        Text(
            text = "INPUT INJECTION MODE",
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(6.dp))

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                // Touch Mode (Primary default)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (settings.inputMode == InputMode.TOUCH) CardDark else Color.Transparent)
                        .clickable { prefsRepo.setInputMode(InputMode.TOUCH) }
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = settings.inputMode == InputMode.TOUCH,
                        onClick = { prefsRepo.setInputMode(InputMode.TOUCH) },
                        colors = RadioButtonDefaults.colors(selectedColor = NeonCyan)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Touch Drag Simulation (Default / Standalone)", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Continuous touch drags via Accessibility. Works on all devices out of the box (NO Shizuku / NO root).", color = TextSecondary, fontSize = 11.sp)
                    }
                }

                // Gamepad Mode
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (settings.inputMode == InputMode.GAMEPAD) CardDark else Color.Transparent)
                        .clickable { prefsRepo.setInputMode(InputMode.GAMEPAD) }
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = settings.inputMode == InputMode.GAMEPAD,
                        onClick = { prefsRepo.setInputMode(InputMode.GAMEPAD) },
                        colors = RadioButtonDefaults.colors(selectedColor = NeonCyan)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Native Gamepad Mode (Requires Shizuku / Root)", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Emulates real USB HID Gamepad stick. Only use this if you have Shizuku active.", color = TextSecondary, fontSize = 11.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Selected Target Games Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "TARGET GAMES (${selectedPackages.size})",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )
            Text(
                text = "Select from Apps →",
                color = NeonCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { onNavigateToGames() }
            )
        }
        Spacer(modifier = Modifier.height(6.dp))

        if (selectedPackages.isEmpty()) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToGames() }
            ) {
                Text(
                    text = "No games selected yet. Tap here to select your games from installed apps.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(selectedPackages.toList()) { pkg ->
                    val isKotor = pkg.contains("kotor", ignoreCase = true)
                    val label = if (isKotor) "Star Wars: KOTOR" else pkg.substringAfterLast('.').capitalize()

                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        modifier = Modifier
                            .width(160.dp)
                            .clickable {
                                OverlayService.start(context)
                                gameRepo.launchGame(pkg)
                            }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = label,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                maxLines = 1
                            )
                            Text(
                                text = pkg,
                                color = TextSecondary,
                                fontSize = 10.sp,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "▶ Launch & Play",
                                color = NeonCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Customizer Link
        OutlinedButton(
            onClick = onNavigateToLayout,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(imageVector = Icons.Default.Tune, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Customize Joystick Size & Opacity", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun PermissionItem(
    title: String,
    description: String,
    isGranted: Boolean,
    onGrantClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
            contentDescription = null,
            tint = if (isGranted) AccentGreen else AccentGold,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(text = description, color = TextSecondary, fontSize = 11.sp)
        }
        if (!isGranted) {
            Button(
                onClick = onGrantClick,
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text("Grant", color = CarbonBlack, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        }
    }
}
