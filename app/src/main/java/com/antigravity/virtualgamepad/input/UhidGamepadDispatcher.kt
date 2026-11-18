package com.antigravity.virtualgamepad.input

import android.util.Log
import com.antigravity.virtualgamepad.shizuku.ShizukuManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import kotlin.math.roundToInt

class UhidGamepadDispatcher : InputDispatcher {

    private val tag = "UhidGamepadDispatcher"
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    private var process: Process? = null
    private var writer: BufferedWriter? = null
    private var isRegistered = false

    private var currentX = 128
    private var currentY = 128
    private var buttonMask = 0

    init {
        initUhidDevice()
    }

    private fun initUhidDevice() {
        scope.launch {
            try {
                val proc = ShizukuManager.createElevatedProcess(arrayOf("/system/bin/hid", "-"))
                if (proc == null) {
                    Log.w(tag, "Failed to start /system/bin/hid process")
                    return@launch
                }
                process = proc
                writer = BufferedWriter(OutputStreamWriter(proc.outputStream))

                // Standard USB Gamepad HID Report Descriptor: 2 Axes (X, Y) + 8 Buttons
                val descriptor = intArrayOf(
                    0x05, 0x01,        // USAGE_PAGE (Generic Desktop)
                    0x09, 0x05,        // USAGE (Gamepad)
                    0xA1, 0x01,        // COLLECTION (Application)
                    0xA1, 0x00,        //   COLLECTION (Physical)
                    0x05, 0x01,        //     USAGE_PAGE (Generic Desktop)
                    0x09, 0x30,        //     USAGE (X)
                    0x09, 0x31,        //     USAGE (Y)
                    0x15, 0x00,        //     LOGICAL_MINIMUM (0)
                    0x26, 0xFF, 0x00,  //     LOGICAL_MAXIMUM (255)
                    0x75, 0x08,        //     REPORT_SIZE (8)
                    0x95, 0x02,        //     REPORT_COUNT (2)
                    0x81, 0x02,        //     INPUT (Data,Var,Abs)
                    0x05, 0x09,        //     USAGE_PAGE (Button)
                    0x19, 0x01,        //     USAGE_MINIMUM (Button 1)
                    0x29, 0x08,        //     USAGE_MAXIMUM (Button 8)
                    0x15, 0x00,        //     LOGICAL_MINIMUM (0)
                    0x25, 0x01,        //     LOGICAL_MAXIMUM (1)
                    0x75, 0x01,        //     REPORT_SIZE (1)
                    0x95, 0x08,        //     REPORT_COUNT (8)
                    0x81, 0x02,        //     INPUT (Data,Var,Abs)
                    0xC0,              //   END_COLLECTION
                    0xC0               // END_COLLECTION
                )

                val descJson = descriptor.joinToString(",")
                val registerCmd = """{"id":1,"command":"register","name":"Virtual Gamepad","vid":1118,"pid":654,"descriptor":[$descJson]}"""

                writer?.write(registerCmd)
                writer?.newLine()
                writer?.flush()

                Thread.sleep(120)
                isRegistered = true
                Log.i(tag, "Uhid Gamepad registered")

                sendReport(128, 128, 0)
            } catch (e: Exception) {
                Log.e(tag, "Error initializing UHID Gamepad", e)
                isRegistered = false
            }
        }
    }

    override fun onJoystickMoved(normX: Float, normY: Float, magnitude: Float) {
        val rawX = (128 + normX * 127).roundToInt().coerceIn(0, 255)
        val rawY = (128 + normY * 127).roundToInt().coerceIn(0, 255)

        if (rawX != currentX || rawY != currentY) {
            currentX = rawX
            currentY = rawY
            sendReport(currentX, currentY, buttonMask)
        }
    }

    override fun onJoystickReleased() {
        currentX = 128
        currentY = 128
        sendReport(currentX, currentY, buttonMask)
    }

    override fun onButtonPressed(action: ButtonAction) {
        val bit = getButtonBit(action)
        buttonMask = buttonMask or bit
        sendReport(currentX, currentY, buttonMask)
    }

    override fun onButtonReleased(action: ButtonAction) {
        val bit = getButtonBit(action)
        buttonMask = buttonMask and bit.inv()
        sendReport(currentX, currentY, buttonMask)
    }

    private fun getButtonBit(action: ButtonAction): Int {
        return when (action) {
            ButtonAction.PAUSE -> 1 shl 3 // Button 4 (Y / Space)
        }
    }

    private fun sendReport(x: Int, y: Int, buttons: Int) {
        if (!isRegistered) return
        scope.launch {
            try {
                val reportCmd = """{"id":1,"command":"report","data":[$x,$y,$buttons]}"""
                writer?.write(reportCmd)
                writer?.newLine()
                writer?.flush()
            } catch (e: Exception) {
                Log.e(tag, "Error sending report", e)
            }
        }
    }

    override fun release() {
        try {
            sendReport(128, 128, 0)
            writer?.close()
            process?.destroy()
        } catch (e: Exception) {
            // ignore
        } finally {
            writer = null
            process = null
            isRegistered = false
        }
    }
}
