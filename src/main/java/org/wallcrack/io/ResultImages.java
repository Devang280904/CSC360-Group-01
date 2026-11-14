package org.wallcrack.io;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.image.BufferedImage;
import org.wallcrack.model.DetectionResult;

/**
 * Renders lossless visualizations in analysis-image coordinates.
 *
 * <p>Produces:
 * <ul>
 *   <li><b>Overlay view</b>: Original photo tinted with vibrant magenta candidate pixels
 *       and outlined red bounding boxes with candidate IDs.</li>
 *   <li><b>Binary mask view</b>: High-contrast 1-bit binary image where white pixels (0xFFFFFF)
 *       represent accepted candidate regions and black pixels (0x000000) represent wall surface.</li>
 * </ul>
 */
public final class ResultImages {
    private ResultImages() { }

    /**
     * Highlights retained candidate pixels in magenta and outlines numbered candidate regions.
     *
     * @param result completed detection result
     * @return new RGB overlay image
     */
    public static BufferedImage overlay(DetectionResult result) {
        // Start from a defensive copy of the analysis image
        BufferedImage image = result.analysisImage();

        // 1. Tint accepted candidate pixels with high-visibility magenta (0xFF00AA)
        for (int y = 0; y < result.height(); y++) {
            for (int x = 0; x < result.width(); x++) {
                if (result.isCandidate(x, y)) image.setRGB(x, y, 0xFF00AA);
            }
        }

        // 2. Draw red bounding boxes and region ID tags around each candidate
        var graphics = image.createGraphics();
        try {
            graphics.setColor(new Color(220, 30, 45));
            graphics.setStroke(new BasicStroke(1.5f));
            for (var region : result.regions()) {
                graphics.drawRect(region.x(), region.y(), region.width() - 1, region.height() - 1);
                graphics.drawString("#" + region.id(), region.x(), Math.max(12, region.y() - 4));
            }
        } finally {
            graphics.dispose();
        }
        return image;
    }

    /**
     * Produces a black background with accepted candidate pixels in white.
     *
     * @param result completed detection result
     * @return new binary mask image (TYPE_BYTE_BINARY)
     */
    public static BufferedImage mask(DetectionResult result) {
        // Construct a compact binary 1-bit raster
        BufferedImage image = new BufferedImage(result.width(), result.height(), BufferedImage.TYPE_BYTE_BINARY);
        for (int y = 0; y < result.height(); y++) {
            for (int x = 0; x < result.width(); x++) {
                if (result.isCandidate(x, y)) image.setRGB(x, y, 0xFFFFFF);
            }
        }
        return image;
    }
}
