package app.mat2uken.android.app.clearoutcamerapreview.model

/**
 * Custom Size class to replace android.util.Size for better testability
 */
data class Size(
    val width: Int,
    val height: Int
) {
    init {
        require(width >= 0) { "Width must be non-negative" }
        require(height >= 0) { "Height must be non-negative" }
    }
    
    override fun toString(): String {
        return "${width}x${height}"
    }
    
    companion object {
        /**
         * Converts android.util.Size to our custom Size
         */
        @JvmStatic
        fun fromAndroidSize(androidSize: android.util.Size): Size {
            return Size(androidSize.width, androidSize.height)
        }
    }
    
    /**
     * Extension function to convert our custom Size to android.util.Size
     */
    fun toAndroidSize(): android.util.Size {
        return android.util.Size(width, height)
    }
}