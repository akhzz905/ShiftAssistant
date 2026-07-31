package cn.akhzz.shiftassistant.util;

import android.graphics.Color;

public class ColorUtils {

    /** Predefined Material Design colors for shift groups */
    public static final int[] GROUP_COLORS = {
            0xFF4CAF50, // Green
            0xFF2196F3, // Blue
            0xFFFF9800, // Orange
            0xFF9C27B0, // Purple
            0xFFE91E63, // Pink
            0xFF00BCD4, // Cyan
            0xFFFF5722, // Deep Orange
            0xFF3F51B5, // Indigo
            0xFF8BC34A, // Light Green
            0xFFFFC107, // Amber
            0xFF009688, // Teal
            0xFF795548, // Brown
            0xFF607D8B, // Blue Grey
            0xFFCDDC39, // Lime
            0xFF673AB7, // Deep Purple
            0xFFF44336, // Red
    };

    /**
     * Get a default color based on an index (cycles through predefined colors).
     */
    public static int getDefaultColor(int index) {
        return GROUP_COLORS[Math.abs(index) % GROUP_COLORS.length];
    }

    /**
     * Get a contrasting text color (black or white) for the given background.
     * Uses relative luminance calculation.
     */
    public static int getContrastColor(int backgroundColor) {
        double luminance = (0.299 * Color.red(backgroundColor)
                + 0.587 * Color.green(backgroundColor)
                + 0.114 * Color.blue(backgroundColor)) / 255.0;
        return luminance > 0.5 ? Color.BLACK : Color.WHITE;
    }

    /**
     * Create a lighter version of a color.
     *
     * @param color  original color
     * @param factor 0.0 = original, 1.0 = white
     */
    public static int lighten(int color, float factor) {
        int r = Math.min(255, (int) (Color.red(color) + (255 - Color.red(color)) * factor));
        int g = Math.min(255, (int) (Color.green(color) + (255 - Color.green(color)) * factor));
        int b = Math.min(255, (int) (Color.blue(color) + (255 - Color.blue(color)) * factor));
        return Color.argb(Color.alpha(color), r, g, b);
    }
}
