package com.example.ui.util

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View

/**
 * Utility helper providing subtle tactile haptic feedback across interactive components
 * and dashboard state transitions in SkyGlass Weather.
 */
object HapticUtils {

    /**
     * Subtle click feedback for primary interactive buttons (search, city selections, FAB).
     */
    fun performClick(view: View?) {
        view?.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
    }

    /**
     * Light tick feedback for toggles, filters, and icon interactions.
     */
    fun performTick(view: View?) {
        view?.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }

    /**
     * Confirmatory feedback when weather dashboard updates or finishes refreshing.
     */
    fun performUpdateSuccess(view: View?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view?.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        } else {
            view?.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        }
    }

    /**
     * Subtle keyboard tap feedback for search inputs and text entries.
     */
    fun performKeyTap(view: View?) {
        view?.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }
}
