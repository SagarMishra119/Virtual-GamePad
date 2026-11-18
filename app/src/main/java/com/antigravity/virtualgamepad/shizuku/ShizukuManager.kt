package com.antigravity.virtualgamepad.shizuku

import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import rikka.shizuku.Shizuku

object ShizukuManager {

    private val _isAvailable = MutableStateFlow(false)
    val isAvailable: StateFlow<Boolean> = _isAvailable.asStateFlow()

    private val _hasPermission = MutableStateFlow(false)
    val hasPermission: StateFlow<Boolean> = _hasPermission.asStateFlow()

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        checkStatus()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        _isAvailable.value = false
        _hasPermission.value = false
    }

    private val requestPermissionResultListener =
        Shizuku.OnRequestPermissionResultListener { _, grantResult ->
            _hasPermission.value = (grantResult == PackageManager.PERMISSION_GRANTED)
        }

    fun init() {
        try {
            Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
            Shizuku.addBinderDeadListener(binderDeadListener)
            Shizuku.addRequestPermissionResultListener(requestPermissionResultListener)
            checkStatus()
        } catch (e: Exception) {
            _isAvailable.value = false
            _hasPermission.value = false
        }
    }

    fun checkStatus() {
        try {
            val alive = Shizuku.pingBinder()
            _isAvailable.value = alive
            if (alive) {
                if (Shizuku.isPreV11()) {
                    _hasPermission.value = false
                } else {
                    _hasPermission.value =
                        Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
                }
            } else {
                _hasPermission.value = false
            }
        } catch (e: Exception) {
            _isAvailable.value = false
            _hasPermission.value = false
        }
    }

    fun requestPermission(requestCode: Int = 1001): Boolean {
        return try {
            if (Shizuku.pingBinder() && !Shizuku.isPreV11()) {
                if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                    Shizuku.requestPermission(requestCode)
                    true
                } else {
                    _hasPermission.value = true
                    true
                }
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Executes a command using Shizuku's elevated shell process or falls back to su.
     */
    fun createElevatedProcess(command: Array<String>): Process? {
        try {
            if (_isAvailable.value && _hasPermission.value) {
                val method = Shizuku::class.java.getDeclaredMethod(
                    "newProcess",
                    Array<String>::class.java,
                    Array<String>::class.java,
                    String::class.java
                )
                method.isAccessible = true
                val result = method.invoke(null, command, null, null)
                if (result is Process) {
                    return result
                }
            }
        } catch (e: Exception) {
            // reflection fallback
        }

        // Fallback to su if available
        return try {
            Runtime.getRuntime().exec(arrayOf("su", "-c", command.joinToString(" ")))
        } catch (e: Exception) {
            null
        }
    }
}
