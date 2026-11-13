package org.wallcrack.model;

/**
 * Parameters expressed in pixels of the analysis image, which may be resized.
 *
 * @param contrastThreshold minimum darkness below the neighborhood mean (1–100)
 * @param localRadius radius of the square neighborhood (2–50)
 * @param minComponentPixels minimum number of connected dark pixels (2–100000)
 * @param minSpan minimum bounding-box diagonal in analysis pixels (2–1000)
 * @param maxDimension maximum analysis width or height (64–2000)
 */
public record DetectionConfig(int contrastThreshold, int localRadius,
                              int minComponentPixels, int minSpan, int maxDimension) {
    /**
     * Validates every parameter before processing can begin.
     * @param contrastThreshold minimum darkness below the local mean (1–100)
     * @param localRadius neighborhood radius in analysis pixels (2–50)
     * @param minComponentPixels minimum connected candidate pixels (2–100000)
     * @param minSpan minimum region diagonal in analysis pixels (2–1000)
     * @param maxDimension maximum analysis width or height (64–2000)
     * @throws IllegalArgumentException if any parameter is outside its allowed range
     */
    public DetectionConfig {
        requireRange(contrastThreshold, 1, 100, "Contrast threshold");
        requireRange(localRadius, 2, 50, "Local radius");
        requireRange(minComponentPixels, 2, 100_000, "Minimum component pixels");
        requireRange(minSpan, 2, 1000, "Minimum span");
        requireRange(maxDimension, 64, 2000, "Maximum dimension");
    }

    /**
     * Creates the standard analysis settings.
     * @return balanced defaults for dark, thin cracks on lighter wall surfaces
     */
    public static DetectionConfig defaults() {
        return new DetectionConfig(14, 12, 30, 15, 1600);
    }

    private static void requireRange(int value, int min, int max, String name) {
        if (value < min || value > max) {
            throw new IllegalArgumentException(name + " must be between " + min + " and " + max + ".");
        }
    }
}
