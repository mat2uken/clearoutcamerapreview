package app.mat2uken.android.app.clearoutcamerapreview.utils

import android.content.Context
import android.hardware.display.DisplayManager
import android.view.Display

/**
 * Helper class for display management operations
 * Separates display logic from UI components for better testability
 */
class DisplayManagerHelper(
    private val displayManager: DisplayManager,
    private val context: Context
) {
    
    /**
     * Gets all available display information
     */
    fun getAllDisplayInfo(): List<DisplayInfo> {
        return DisplayUtils.getAllDisplayInfo(displayManager, context)
    }
    
    /**
     * Finds all external displays
     */
    fun getExternalDisplays(): List<Display> {
        return DisplayUtils.findAllExternalDisplays(displayManager.displays)
    }
    
    /**
     * Gets information about external displays only
     */
    fun getExternalDisplayInfo(): List<DisplayInfo> {
        return getAllDisplayInfo().filter { !it.isDefaultDisplay }
    }
    
    /**
     * Finds a specific display by ID
     */
    fun findDisplayById(displayId: Int): Display? {
        return displayManager.displays.find { it.displayId == displayId }
    }
    
    /**
     * Gets display info for a specific display ID
     */
    fun getDisplayInfoById(displayId: Int): DisplayInfo? {
        val display = findDisplayById(displayId)
        return display?.let { DisplayUtils.getDisplayInfo(it, context) }
    }
    
    /**
     * Checks if external displays are available
     */
    fun hasExternalDisplays(): Boolean {
        return getExternalDisplays().isNotEmpty()
    }
    
    /**
     * Gets the first available external display
     */
    fun getFirstExternalDisplay(): Display? {
        return DisplayUtils.findExternalDisplay(displayManager.displays)
    }
    
    /**
     * Gets the target display based on preference
     * If selectedDisplayId is provided and exists, use that; otherwise use first external display
     */
    fun getTargetDisplay(selectedDisplayId: Int?): Display? {
        return if (selectedDisplayId != null) {
            findDisplayById(selectedDisplayId) ?: getFirstExternalDisplay()
        } else {
            getFirstExternalDisplay()
        }
    }
    
    /**
     * Validates if a display ID is valid and external
     */
    fun isValidExternalDisplayId(displayId: Int): Boolean {
        val display = findDisplayById(displayId)
        return display != null && DisplayUtils.isExternalDisplay(display)
    }
    
    /**
     * Gets a human-readable summary of available displays
     */
    fun getDisplaySummary(): String {
        val allDisplays = getAllDisplayInfo()
        val externalCount = allDisplays.count { !it.isDefaultDisplay }
        return "Total displays: ${allDisplays.size}, External: $externalCount"
    }
}