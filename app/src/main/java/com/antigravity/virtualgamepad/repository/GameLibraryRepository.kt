package com.antigravity.virtualgamepad.repository

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log
import com.antigravity.virtualgamepad.model.GameAppInfo

class GameLibraryRepository(private val context: Context) {

    private val tag = "GameLibraryRepo"

    /**
     * Retrieves all user-installed applications (minus system apps).
     * Includes known presets like KOTOR so they can be selected even if query is limited.
     */
    fun getUserInstalledApps(): List<GameAppInfo> {
        val list = mutableListOf<GameAppInfo>()
        val seenPackages = mutableSetOf<String>()

        try {
            val pm = context.packageManager
            val installedApps = pm.getInstalledApplications(0)

            for (app in installedApps) {
                // Filter out system apps, but keep KOTOR or user-installed apps
                val isSystem = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val isKotor = app.packageName.contains("swkotor", ignoreCase = true)

                if (!isSystem || isKotor) {
                    val label = try {
                        pm.getApplicationLabel(app).toString()
                    } catch (e: Exception) {
                        app.packageName
                    }
                    if (app.packageName != context.packageName && seenPackages.add(app.packageName)) {
                        list.add(GameAppInfo(packageName = app.packageName, appName = label))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error loading installed apps", e)
        }

        // Always ensure standard KOTOR presets are available to select
        val presets = listOf(
            GameAppInfo("com.aspyr.swkotor", "Star Wars: KOTOR"),
            GameAppInfo("com.aspyr.swkotorii", "Star Wars: KOTOR II"),
            GameAppInfo("com.mojang.minecraftpe", "Minecraft"),
            GameAppInfo("com.pubg.imobile", "BGMI")
        )
        for (preset in presets) {
            if (seenPackages.add(preset.packageName)) {
                list.add(preset)
            }
        }

        return list.sortedWith(
            compareByDescending<GameAppInfo> { it.packageName.contains("kotor", ignoreCase = true) }
                .thenBy { it.appName.lowercase() }
        )
    }

    fun launchGame(packageName: String): Boolean {
        return try {
            val pm = context.packageManager
            val launchIntent = pm.getLaunchIntentForPackage(packageName) ?: return false
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            true
        } catch (e: Exception) {
            false
        }
    }

    companion object {
        @Volatile
        private var instance: GameLibraryRepository? = null

        fun getInstance(context: Context): GameLibraryRepository {
            return instance ?: synchronized(this) {
                instance ?: GameLibraryRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
