package com.antigravity.virtualgamepad.model

data class ElementConfig(
    val id: String,
    val type: ElementType,
    val xDp: Int,
    val yDp: Int,
    val sizeDp: Int,
    val opacity: Float = 0.85f,
    val isEnabled: Boolean = true,
    val label: String = ""
)
