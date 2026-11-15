package org.wallcrack.model;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Objects;

/**
 * Immutable snapshot storing an analysis image, binary candidate mask,
 * and detected region measurements.
 *
 * <p>Thread safety and integrity guarantees:
 * <ul>
 *   <li>The underlying {@link BufferedImage} is defensively copied upon creation and return.</li>
 *   <li>The candidate {@code boolean[]} mask is cloned to prevent external modification.</li>
 *   <li>The region list is wrapped in an unmodifiable {@link List#copyOf(java.util.Collection)}.</li>
 *   <li>Coordinates are stored in zero-based analysis-image pixels (top-left origin).</li>
 * </ul>
 */
public final class DetectionResult {
    private final BufferedImage analysisImage;
    private final int originalWidth;
    private final int originalHeight;
    private final boolean[] mask;
    private final List<CrackRegion> regions;
    private final DetectionConfig config;
    private final int effectiveThreshold;
    private final int candidatePixels;

    /**
     * Creates a result with defensive copies of the image, mask, and region list.
     *
     * @param image image used for analysis
     * @param originalWidth width before resizing
     * @param originalHeight height before resizing
     * @param mask row-major retained candidate mask
     * @param regions retained connected components
     * @param config parameters used for analysis
     * @param effectiveThreshold final 8-bit threshold applied after sensitivity offset
     */
    public DetectionResult(BufferedImage image, int originalWidth, int originalHeight,
                           boolean[] mask, List<CrackRegion> regions, DetectionConfig config,
                           int effectiveThreshold) {
        Objects.requireNonNull(image, "image");
        if (originalWidth < 1 || originalHeight < 1
                || mask.length != image.getWidth() * image.getHeight()) {
            throw new IllegalArgumentException("Image dimensions and mask must be consistent.");
        }
        this.analysisImage = copyImage(image);
        this.originalWidth = originalWidth;
        this.originalHeight = originalHeight;
        this.mask = mask.clone();
        this.regions = List.copyOf(regions);
        this.config = Objects.requireNonNull(config, "config");
        this.effectiveThreshold = effectiveThreshold;

        // Count candidate pixels in the accepted mask
        int count = 0;
        for (boolean value : mask) {
            if (value) count++;
        }
        this.candidatePixels = count;
    }

    /**
     * Creates a result with a default effective threshold of 0.
     *
     * @param image image used for analysis
     * @param originalWidth width before resizing
     * @param originalHeight height before resizing
     * @param mask row-major retained candidate mask
     * @param regions retained connected components
     * @param config parameters used for analysis
     */
    public DetectionResult(BufferedImage image, int originalWidth, int originalHeight,
                           boolean[] mask, List<CrackRegion> regions, DetectionConfig config) {
        this(image, originalWidth, originalHeight, mask, regions, config, 0);
    }

    /**
     * Returns an independent image copy.
     * @return a defensive copy of the analyzed image
     */
    public BufferedImage analysisImage() { return copyImage(analysisImage); }

    /**
     * Returns the analyzed width.
     * @return analysis width in pixels
     */
    public int width() { return analysisImage.getWidth(); }

    /**
     * Returns the analyzed height.
     * @return analysis height in pixels
     */
    public int height() { return analysisImage.getHeight(); }

    /**
     * Returns the width before resizing.
     * @return original input width
     */
    public int originalWidth() { return originalWidth; }

    /**
     * Returns the height before resizing.
     * @return original input height
     */
    public int originalHeight() { return originalHeight; }

    /**
     * Returns the retained components.
     * @return immutable connected candidate list
     */
    public List<CrackRegion> regions() { return regions; }

    /**
     * Returns the analysis settings.
     * @return parameters used for this result
     */
    public DetectionConfig config() { return config; }

    /**
     * Returns the effective threshold level used.
     * @return 8-bit luminance threshold (0–255)
     */
    public int effectiveThreshold() { return effectiveThreshold; }

    /**
     * Returns the retained mask area.
     * @return number of pixels in accepted candidates
     */
    public int candidatePixels() { return candidatePixels; }

    /**
     * Returns the retained area ratio.
     * @return candidate coverage as a percentage of the analyzed image
     */
    public double coveragePercent() { return 100.0 * candidatePixels / mask.length; }

    /**
     * Returns the widest estimated candidate thickness in analysis pixels.
     * @return maximum candidate thickness estimate, or zero when no candidates exist
     */
    public double maximumEstimatedThickness() {
        return regions.stream().mapToDouble(CrackRegion::estimatedThickness).max().orElse(0.0);
    }

    /**
     * Checks the retained mask at the specified analysis coordinate.
     * @param x horizontal coordinate
     * @param y vertical coordinate
     * @return whether the pixel belongs to an accepted candidate
     */
    public boolean isCandidate(int x, int y) {
        Objects.checkIndex(x, width());
        Objects.checkIndex(y, height());
        return mask[y * width() + x];
    }

    /**
     * Creates a defensive RGB deep copy of a BufferedImage.
     */
    private static BufferedImage copyImage(BufferedImage source) {
        BufferedImage copy = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        var graphics = copy.createGraphics();
        try {
            graphics.setColor(java.awt.Color.WHITE);
            graphics.fillRect(0, 0, copy.getWidth(), copy.getHeight());
            graphics.drawImage(source, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        return copy;
    }
}
