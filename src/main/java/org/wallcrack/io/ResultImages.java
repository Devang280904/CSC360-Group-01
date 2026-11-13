package org.wallcrack.io;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.image.BufferedImage;
import org.wallcrack.model.DetectionResult;

/** Renders lossless visualizations in analysis-image coordinates. */
public final class ResultImages {
    private ResultImages() { }

    /**
     * Highlights retained pixels in magenta and outlines numbered candidate regions.
     * @param result completed analysis
     * @return new RGB overlay image
     */
    public static BufferedImage overlay(DetectionResult result) {
        BufferedImage image = result.analysisImage();
        for (int y = 0; y < result.height(); y++) {
            for (int x = 0; x < result.width(); x++) {
                if (result.isCandidate(x, y)) image.setRGB(x, y, 0xFF00AA);
            }
        }
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
     * @param result completed analysis
     * @return new binary mask image
     */
    public static BufferedImage mask(DetectionResult result) {
        BufferedImage image = new BufferedImage(result.width(), result.height(), BufferedImage.TYPE_BYTE_BINARY);
        for (int y = 0; y < result.height(); y++) {
            for (int x = 0; x < result.width(); x++) {
                if (result.isCandidate(x, y)) image.setRGB(x, y, 0xFFFFFF);
            }
        }
        return image;
    }
}
