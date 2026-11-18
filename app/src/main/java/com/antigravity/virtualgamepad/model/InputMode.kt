package com.antigravity.virtualgamepad.model

enum class InputMode(val title: String, val subtitle: String) {
    GAMEPAD(
        title = "Native Gamepad Mode",
        subtitle = "Uses Shizuku/Root virtual HID gamepad for smooth 360° analog movement & full touch pass-through"
    ),
    TOUCH(
        title = "Touch Emulation Mode",
        subtitle = "Uses Accessibility Service to simulate touch gestures without PC or root"
    )
}
