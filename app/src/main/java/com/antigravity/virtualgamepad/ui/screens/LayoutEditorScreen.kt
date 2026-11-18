package com.antigravity.virtualgamepad.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.virtualgamepad.model.ElementConfig
import com.antigravity.virtualgamepad.model.LayoutSettings
import com.antigravity.virtualgamepad.repository.PreferencesRepository
import com.antigravity.virtualgamepad.ui.theme.*
import kotlin.math.roundToInt

@Composable
fun LayoutEditorScreen() {
    val context = LocalContext.current
    val prefsRepo = remember { PreferencesRepository.getInstance(context) }
    val settings by prefsRepo.settingsFlow.collectAsState()

    var selectedElementId by remember { mutableStateOf("joystick_movement") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CarbonBlack)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "CONTROLS & LAYOUT",
                    color = NeonCyan,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Changes take effect immediately on screen",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
            IconButton(
                onClick = {
                    prefsRepo.updateSettings { LayoutSettings.createDefault() }
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reset Layout",
                    tint = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Tip Card
        Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "💡 Tap '🔓 EDIT' on the floating overlay toolbar in-game to drag & reposition the joystick anywhere on screen.",
                color = AccentGold,
                fontSize = 11.sp,
                modifier = Modifier.padding(12.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(settings.elements, key = { it.id }) { element ->
                val isSelected = element.id == selectedElementId

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) CardDark else SurfaceDark
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedElementId = element.id }
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = element.label.ifEmpty { element.id },
                                    color = if (element.isEnabled) TextPrimary else TextSecondary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${element.sizeDp} dp size • ${(element.opacity * 100).roundToInt()}% opacity",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }

                            Switch(
                                checked = element.isEnabled,
                                onCheckedChange = { isChecked ->
                                    prefsRepo.updateElement(element.id) { it.copy(isEnabled = isChecked) }
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan)
                            )
                        }

                        if (isSelected && element.isEnabled) {
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = CarbonBlack)
                            Spacer(modifier = Modifier.height(12.dp))

                            // Size Slider
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Size (decrease / increase)", color = TextSecondary, fontSize = 12.sp)
                                Text("${element.sizeDp} dp", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = element.sizeDp.toFloat(),
                                onValueChange = { newSize ->
                                    prefsRepo.updateElement(element.id) { it.copy(sizeDp = newSize.roundToInt()) }
                                },
                                valueRange = 70f..240f,
                                colors = SliderDefaults.colors(
                                    thumbColor = NeonCyan,
                                    activeTrackColor = NeonCyan,
                                    inactiveTrackColor = CarbonBlack
                                )
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // Opacity Slider
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Opacity (transparency)", color = TextSecondary, fontSize = 12.sp)
                                Text("${(element.opacity * 100).roundToInt()}%", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = element.opacity,
                                onValueChange = { newOpacity ->
                                    prefsRepo.updateElement(element.id) { it.copy(opacity = newOpacity) }
                                },
                                valueRange = 0.20f..1.0f,
                                colors = SliderDefaults.colors(
                                    thumbColor = NeonCyan,
                                    activeTrackColor = NeonCyan,
                                    inactiveTrackColor = CarbonBlack
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
