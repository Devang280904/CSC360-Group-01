package org.wallcrack.model;

import java.util.Objects;

/**
 * Immutable parameters configuration for the ImageJ wall crack detection pipeline.
 *
 * <p>Each parameter controls a specific stage of image processing:
 * <ul>
 *   <li><b>thresholdMethod</b>: The histogram clustering algorithm ("Triangle" for sparse dark cracks;
 *       "Otsu" for bimodal surfaces).</li>
 *   <li><b>sensitivityOffset</b>: Adjustment offset added to the computed threshold. A positive offset
 *       raises the threshold to capture fainter marks; a negative offset lowers it for higher specificity.</li>
 *   <li><b>minArea</b>: Minimum number of contiguous candidate pixels required to retain a region (rejects speckle noise).</li>
 *   <li><b>maxCircularity</b>: Maximum circularity (4*pi*area/perimeter^2). Cracks are elongated and narrow (low circularity,
 *       typically &lt; 0.2), whereas paint bubbles or circular stains have circularity near 1.0.</li>
 *   <li><b>correctLighting</b>: Enables ImageJ rolling-ball background subtraction to flatten uneven wall lighting.</li>
 *   <li><b>closingRadius</b>: Applies mathematical morphology (dilation followed by erosion) to reconnect hairline breaks.</li>
 *   <li><b>maxDimension</b>: Maximum allowable width or height; larger images are scaled down proportionally to bound memory usage.</li>
 * </ul>
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

    /**
     * Validates and canonicalizes the threshold method name.
     */
    private static String normalizeMethod(String method) {
        String trimmed = method.trim();
        if (trimmed.equalsIgnoreCase("Triangle")) return "Triangle";
        if (trimmed.equalsIgnoreCase("Otsu")) return "Otsu";
        throw new IllegalArgumentException("Unsupported threshold method: " + method + ". Use Triangle or Otsu.");
    }

    /**
     * Enforces integer parameter range bounds.
     */
    private static void requireRange(int value, int min, int max, String name) {
        if (value < min || value > max) {
            throw new IllegalArgumentException(name + " must be between " + min + " and " + max + ".");
        }
    }
}
