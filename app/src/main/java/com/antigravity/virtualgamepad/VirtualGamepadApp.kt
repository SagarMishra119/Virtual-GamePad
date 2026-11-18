package com.antigravity.virtualgamepad

import android.app.Application
import com.antigravity.virtualgamepad.shizuku.ShizukuManager

class VirtualGamepadApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ShizukuManager.init()
    }
}
