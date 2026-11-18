package com.antigravity.virtualgamepad.model

data class LayoutSettings(
    val elements: List<ElementConfig> = emptyList(),
    val deadzone: Float = 0.08f,
    val inputMode: InputMode = InputMode.TOUCH
) {
    companion object {
        fun createDefault(): LayoutSettings {
            return LayoutSettings(
                elements = listOf(
                    ElementConfig(
                        id = "joystick_movement",
                        type = ElementType.JOYSTICK,
                        xDp = 32,
                        yDp = 180,
                        sizeDp = 140,
                        opacity = 0.85f,
                        isEnabled = true,
                        label = "Movement Stick"
                    ),
                    ElementConfig(
                        id = "button_pause",
                        type = ElementType.BUTTON_PAUSE,
                        xDp = 32,
                        yDp = 32,
                        sizeDp = 56,
                        opacity = 0.80f,
                        isEnabled = false,
                        label = "PAUSE"
                    )
                ),
                deadzone = 0.08f,
                inputMode = InputMode.TOUCH
            )
        }
    }
}
