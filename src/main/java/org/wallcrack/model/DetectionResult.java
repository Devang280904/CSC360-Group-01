package org.wallcrack.model;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Objects;

/** Stores an analysis image, an immutable mask, and the corresponding candidate measurements. */
public final class DetectionResult {
    private final BufferedImage analysisImage;
    private final int originalWidth;
    private final int originalHeight;
    private final boolean[] mask;
    private final List<CrackRegion> regions;
    private final DetectionConfig config;
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
     */
    public DetectionResult(BufferedImage image, int originalWidth, int originalHeight,
                           boolean[] mask, List<CrackRegion> regions, DetectionConfig config) {
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
        int count = 0;
        for (boolean value : mask) {
            if (value) count++;
        }
        this.candidatePixels = count;
    }

    /** @return a defensive copy of the analyzed image */
    public BufferedImage analysisImage() { return copyImage(analysisImage); }
    /** @return analysis width in pixels */
    public int width() { return analysisImage.getWidth(); }
    /** @return analysis height in pixels */
    public int height() { return analysisImage.getHeight(); }
    /** @return original input width */
    public int originalWidth() { return originalWidth; }
    /** @return original input height */
    public int originalHeight() { return originalHeight; }
    /** @return immutable connected candidate list */
    public List<CrackRegion> regions() { return regions; }
    /** @return parameters used for this result */
    public DetectionConfig config() { return config; }
    /** @return number of pixels in accepted candidates */
    public int candidatePixels() { return candidatePixels; }
    /** @return candidate coverage as a percentage of the analyzed image */
    public double coveragePercent() { return 100.0 * candidatePixels / mask.length; }

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
