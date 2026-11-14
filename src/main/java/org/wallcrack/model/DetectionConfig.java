package org.wallcrack.model;

import java.util.Objects;

/**
 * Parameters for the ImageJ wall crack detection pipeline.
 *
 * @param thresholdMethod automatic thresholding method ("Triangle" or "Otsu")
 * @param sensitivityOffset manual adjustment added to the auto-threshold (-50 to +50)
 * @param minArea minimum candidate particle area in pixels (2 to 100000)
 * @param maxCircularity maximum circularity filter (0.01 to 1.0)
 * @param correctLighting whether to apply rolling-ball background correction
 * @param closingRadius radius for morphological closing to connect small gaps (0 to 10)
 * @param maxDimension maximum analysis width or height in pixels (64 to 2000)
 */
public record DetectionConfig(
        String thresholdMethod,
        int sensitivityOffset,
        int minArea,
        double maxCircularity,
        boolean correctLighting,
        int closingRadius,
        int maxDimension
) {
    /**
     * Validates configuration parameters.
     *
     * @param thresholdMethod automatic thresholding method ("Triangle" or "Otsu")
     * @param sensitivityOffset manual adjustment added to the auto-threshold (-50 to +50)
     * @param minArea minimum candidate particle area in pixels (2 to 100000)
     * @param maxCircularity maximum circularity filter (0.01 to 1.0)
     * @param correctLighting whether to apply rolling-ball background correction
     * @param closingRadius radius for morphological closing to connect small gaps (0 to 10)
     * @param maxDimension maximum analysis width or height in pixels (64 to 2000)
     * @throws IllegalArgumentException if any parameter is outside its allowed range
     */
    public DetectionConfig {
        Objects.requireNonNull(thresholdMethod, "thresholdMethod");
        thresholdMethod = normalizeMethod(thresholdMethod);
        requireRange(sensitivityOffset, -50, 50, "Sensitivity offset");
        requireRange(minArea, 2, 100_000, "Minimum area");
        if (maxCircularity < 0.01 || maxCircularity > 1.0) {
            throw new IllegalArgumentException("Maximum circularity must be between 0.01 and 1.0.");
        }
        requireRange(closingRadius, 0, 10, "Closing radius");
        requireRange(maxDimension, 64, 2000, "Maximum dimension");
    }

    /**
     * Standard evaluated detection settings for wall crack review.
     *
     * @return standard default configuration
     */
    public static DetectionConfig defaults() {
        return new DetectionConfig("Triangle", 0, 30, 0.50, false, 0, 1600);
    }

    private static String normalizeMethod(String method) {
        String trimmed = method.trim();
        if (trimmed.equalsIgnoreCase("Triangle")) return "Triangle";
        if (trimmed.equalsIgnoreCase("Otsu")) return "Otsu";
        throw new IllegalArgumentException("Unsupported threshold method: " + method + ". Use Triangle or Otsu.");
    }

    private static void requireRange(int value, int min, int max, String name) {
        if (value < min || value > max) {
            throw new IllegalArgumentException(name + " must be between " + min + " and " + max + ".");
        }
    }
}
