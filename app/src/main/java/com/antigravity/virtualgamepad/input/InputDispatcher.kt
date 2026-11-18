package com.antigravity.virtualgamepad.input

interface InputDispatcher {
    /**
     * Called when the joystick is moved.
     * @param normX Normalized X in range [-1.0, 1.0] (negative = left, positive = right)
     * @param normY Normalized Y in range [-1.0, 1.0] (negative = up, positive = down)
     * @param magnitude Magnitude in range [0.0, 1.0]
     */
    fun onJoystickMoved(normX: Float, normY: Float, magnitude: Float)

    /**
     * Called when the joystick is released back to center.
     */
    fun onJoystickReleased()

    /**
     * Called when a floating action button is pressed down.
     */
    fun onButtonPressed(action: ButtonAction)

    /**
     * Called when a floating action button is released.
     */
    fun onButtonReleased(action: ButtonAction)

    /**
     * Release all resources.
     */
    fun release()
}
