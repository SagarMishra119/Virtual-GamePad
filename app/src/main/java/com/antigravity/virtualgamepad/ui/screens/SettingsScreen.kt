package com.antigravity.virtualgamepad.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.virtualgamepad.repository.PreferencesRepository
import com.antigravity.virtualgamepad.shizuku.ShizukuManager
import com.antigravity.virtualgamepad.ui.theme.*
import kotlin.math.roundToInt

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val prefsRepo = remember { PreferencesRepository.getInstance(context) }
    val settings by prefsRepo.settingsFlow.collectAsState()

    val isShizukuAvailable by ShizukuManager.isAvailable.collectAsState()
    val hasShizukuPermission by ShizukuManager.hasPermission.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CarbonBlack)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "SETTINGS",
            color = NeonCyan,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Text(
            text = "Deadzone calibration and elevated input configuration",
            color = TextSecondary,
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Deadzone Calibration Card
        Text(
            text = "JOYSTICK CALIBRATION",
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Thumbstick Deadzone", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Text("${(settings.deadzone * 100).roundToInt()}%", color = NeonCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    text = "Prevents unwanted movement when your thumb is resting near center",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
                Slider(
                    value = settings.deadzone,
                    onValueChange = { newDeadzone ->
                        prefsRepo.updateSettings { it.copy(deadzone = newDeadzone) }
                    },
                    valueRange = 0.02f..0.25f,
                    colors = SliderDefaults.colors(
                        thumbColor = NeonCyan,
                        activeTrackColor = NeonCyan,
                        inactiveTrackColor = CardDark
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Shizuku Setup Card
        Text(
            text = "SHIZUKU (NATIVE GAMEPAD CONTROLLER)",
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = if (isShizukuAvailable && hasShizukuPermission) AccentGreen else AccentGold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isShizukuAvailable && hasShizukuPermission) {
                            "Shizuku Connected & Authorized"
                        } else if (isShizukuAvailable) {
                            "Shizuku Running (Tap to Authorize)"
                        } else {
                            "Shizuku Not Active (Optional)"
                        },
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Shizuku allows Virtual Gamepad to emulate a real USB gamepad kernel device (/system/bin/hid) without root, giving native 360° analog movement in KOTOR.",
                    color = TextSecondary,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { ShizukuManager.requestPermission() },
                        enabled = isShizukuAvailable,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Authorize Shizuku", color = CarbonBlack, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app"))
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                    ) {
                        Text("Get Shizuku", fontSize = 11.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // About Card
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = NeonCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Virtual Gamepad v1.1",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Specialized overlay for Star Wars: Knights of the Old Republic with simultaneous screen-touch pass-through.",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }
    }
}
