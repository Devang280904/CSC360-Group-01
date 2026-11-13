package org.wallcrack.detection;

import java.awt.Color;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import org.wallcrack.model.CrackRegion;
import org.wallcrack.model.DetectionConfig;
import org.wallcrack.model.DetectionResult;

/** Stateless detector combining local darkness with eight-connected shape filtering. */
public final class CrackDetector {
    /** Creates a reusable, thread-safe detector with no external model dependencies. */
    public CrackDetector() { }

    /**
     * Finds dark, elongated candidate regions. Bright cracks and structural severity
     * cannot be inferred by this algorithm. Input pixels are never modified.
     *
     * @param source input image
     * @param config validated analysis parameters
     * @return retained mask, candidate regions, and analysis metadata
     * @throws CancellationException when the processing thread is interrupted
     */
    public DetectionResult detect(BufferedImage source, DetectionConfig config) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(config, "config");
        checkCancelled();
        BufferedImage image = prepareImage(source, config.maxDimension());
        int width = image.getWidth();
        int height = image.getHeight();
        int[] gray = new int[width * height];
        long[] integral = new long[(width + 1) * (height + 1)];
        for (int y = 0; y < height; y++) {
            checkCancelled();
            long rowSum = 0;
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                int value = (299 * ((rgb >> 16) & 255) + 587 * ((rgb >> 8) & 255)
                        + 114 * (rgb & 255) + 500) / 1000;
                gray[y * width + x] = value;
                rowSum += value;
                integral[(y + 1) * (width + 1) + x + 1] = integral[y * (width + 1) + x + 1] + rowSum;
            }
        }
        boolean[] dark = new boolean[gray.length];
        int radius = config.localRadius();
        for (int y = 0; y < height; y++) {
            checkCancelled();
            int top = Math.max(0, y - radius);
            int bottom = Math.min(height, y + radius + 1);
            for (int x = 0; x < width; x++) {
                int left = Math.max(0, x - radius);
                int right = Math.min(width, x + radius + 1);
                long sum = integral[bottom * (width + 1) + right] - integral[top * (width + 1) + right]
                        - integral[bottom * (width + 1) + left] + integral[top * (width + 1) + left];
                double mean = (double) sum / ((right - left) * (bottom - top));
                dark[y * width + x] = mean - gray[y * width + x] >= config.contrastThreshold();
            }
        }
        boolean[] retained = new boolean[dark.length];
        List<CrackRegion> regions = collectRegions(dark, retained, width, height, config);
        return new DetectionResult(image, source.getWidth(), source.getHeight(), retained, regions, config);
    }

    private static List<CrackRegion> collectRegions(boolean[] dark, boolean[] retained,
                                                    int width, int height, DetectionConfig config) {
        List<CrackRegion> regions = new ArrayList<>();
        int[] queue = new int[dark.length];
        for (int start = 0; start < dark.length; start++) {
            if (start % width == 0) checkCancelled();
            if (!dark[start]) continue;
            int head = 0;
            int tail = 1;
            queue[0] = start;
            dark[start] = false;
            int minX = width, minY = height, maxX = 0, maxY = 0;
            while (head < tail) {
                if ((head & 16383) == 0) checkCancelled();
                int index = queue[head++];
                int x = index % width;
                int y = index / width;
                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
                maxX = Math.max(maxX, x);
                maxY = Math.max(maxY, y);
                for (int ny = Math.max(0, y - 1); ny <= Math.min(height - 1, y + 1); ny++) {
                    for (int nx = Math.max(0, x - 1); nx <= Math.min(width - 1, x + 1); nx++) {
                        int neighbor = ny * width + nx;
                        if (dark[neighbor]) {
                            dark[neighbor] = false;
                            queue[tail++] = neighbor;
                        }
                    }
                }
            }
            int boxWidth = maxX - minX + 1;
            int boxHeight = maxY - minY + 1;
            double span = Math.hypot(boxWidth, boxHeight);
            // A compact filled patch has a low squared-span-to-area ratio.
            double slenderness = span * span / tail;
            if (tail >= config.minComponentPixels() && span >= config.minSpan() && slenderness >= 3.0) {
                for (int i = 0; i < tail; i++) retained[queue[i]] = true;
                regions.add(new CrackRegion(regions.size() + 1, minX, minY,
                        boxWidth, boxHeight, tail, span));
            }
        }
        return regions;
    }

    private static BufferedImage prepareImage(BufferedImage source, int maximum) {
        double scale = Math.min(1.0, (double) maximum / Math.max(source.getWidth(), source.getHeight()));
        int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(source.getHeight() * scale));
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        var graphics = image.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, width, height);
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.drawImage(source, 0, 0, width, height, null);
        } finally {
            graphics.dispose();
        }
        return image;
    }

    private static void checkCancelled() {
        if (Thread.currentThread().isInterrupted()) throw new CancellationException("Analysis cancelled.");
    }
}
