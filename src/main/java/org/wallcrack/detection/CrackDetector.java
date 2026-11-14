package org.wallcrack.detection;

import ij.ImagePlus;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.filter.BackgroundSubtracter;
import ij.plugin.filter.ParticleAnalyzer;
import ij.process.AutoThresholder;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
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

/**
 * Executes wall crack candidate detection using ImageJ thresholding,
 * binary morphological operations, and particle analysis.
 *
 * <p>Pipeline overview:
 * <ol>
 *   <li>Resizes large images proportionally and composites transparency onto white.</li>
 *   <li>Converts RGB to 8-bit grayscale using standard CCIR-601 luminance weights.</li>
 *   <li>Optionally applies rolling-ball background correction to remove illumination gradients.</li>
 *   <li>Computes an automatic threshold (Triangle or Otsu) and applies a sensitivity offset.</li>
 *   <li>Optionally applies morphological closing (dilation then erosion) to bridge hairline gaps.</li>
 *   <li>Runs ImageJ ParticleAnalyzer with area and circularity filters to reject noise and compact spots.</li>
 *   <li>Packages accepted particles into an immutable {@link DetectionResult} snapshot.</li>
 * </ol>
 */
public final class CrackDetector {

    /** Creates a reusable, thread-safe detector powered by ImageJ core. */
    public CrackDetector() { }

    /**
     * Finds dark candidate regions using ImageJ automatic thresholding
     * and particle analysis. Input pixels are never modified.
     *
     * @param source input photograph
     * @param config validated analysis parameters
     * @return retained mask, candidate regions, and analysis metadata
     * @throws CancellationException when the processing thread is interrupted
     */
    public DetectionResult detect(BufferedImage source, DetectionConfig config) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(config, "config");
        checkCancelled();

        // 1. Prepare image: proportionally scale down large photos and composite transparency on white
        BufferedImage image = prepareImage(source, config.maxDimension());
        int width = image.getWidth();
        int height = image.getHeight();

        // 2. Grayscale conversion: ImageJ ColorProcessor with standard perceptual weights:
        //    luminance = 0.299 * R + 0.587 * G + 0.114 * B
        checkCancelled();
        ColorProcessor cp = new ColorProcessor(image);
        cp.setRGBWeights(0.299, 0.587, 0.114);
        ByteProcessor bp = cp.convertToByteProcessor();

        // 3. Optional illumination normalization: ImageJ rolling-ball background subtraction
        //    isolates local dark marks from gradual wall shadows or lighting gradients.
        if (config.correctLighting()) {
            checkCancelled();
            new BackgroundSubtracter().rollingBallBackground(bp, 50, false, true, false, false, true);
        }

        // 4. Automatic threshold calculation: ImageJ AutoThresholder evaluates the histogram
        //    (Triangle algorithm favors sparse dark lines; Otsu clusters bimodal distributions).
        checkCancelled();
        AutoThresholder.Method method = AutoThresholder.Method.valueOf(config.thresholdMethod());
        int autoThreshold = new AutoThresholder().getThreshold(method, bp.getHistogram());

        // Apply user-defined sensitivity offset (clamped to valid 8-bit range [0, 255])
        int effectiveThreshold = Math.max(0, Math.min(255, autoThreshold + config.sensitivityOffset()));

        // Binarize: dark pixels [0 .. effectiveThreshold] are marked as foreground (white, value 255)
        bp.setThreshold(0, effectiveThreshold, ImageProcessor.NO_LUT_UPDATE);
        ByteProcessor binaryMask = bp.createMask();

        // 5. Optional morphological closing: dilate then erode to bridge minor hairline discontinuities
        if (config.closingRadius() > 0) {
            checkCancelled();
            for (int i = 0; i < config.closingRadius(); i++) binaryMask.dilate(1, 0);
            for (int i = 0; i < config.closingRadius(); i++) binaryMask.erode(1, 0);
        }

        // 6. Particle analysis: ImageJ ParticleAnalyzer groups connected white pixels (255)
        //    Filters out noise (< minArea) and compact non-crack spots (> maxCircularity).
        //    Circularity = 4 * pi * (area / perimeter^2); thin lines approach 0.0, circles approach 1.0.
        checkCancelled();
        binaryMask.setThreshold(255, 255, ImageProcessor.NO_LUT_UPDATE);
        ResultsTable rt = new ResultsTable();
        int measurements = Measurements.AREA | Measurements.RECT | Measurements.SHAPE_DESCRIPTORS;
        ParticleAnalyzer pa = new ParticleAnalyzer(
                ParticleAnalyzer.SHOW_MASKS,
                measurements,
                rt,
                config.minArea(),
                Double.POSITIVE_INFINITY,
                0.0,
                config.maxCircularity()
        );
        pa.setHideOutputImage(true); // Headless operation: suppress GUI windows

        checkCancelled();
        ImagePlus maskImp = new ImagePlus("mask", binaryMask);
        if (!pa.analyze(maskImp)) {
            throw new IllegalStateException("ImageJ particle analysis failed.");
        }

        // 7. Extract accepted particles: SHOW_MASKS produces an ImageProcessor containing ONLY
        //    particles that passed both area and circularity filters.
        checkCancelled();
        ImagePlus outputImp = pa.getOutputImage();
        ImageProcessor acceptedIp = outputImp != null ? outputImp.getProcessor() : new ByteProcessor(width, height);

        boolean[] retained = new boolean[width * height];
        for (int y = 0; y < height; y++) {
            int row = y * width;
            for (int x = 0; x < width; x++) {
                retained[row + x] = (acceptedIp.get(x, y) == 255);
            }
        }

        // 8. Collect measured region bounds and shape descriptors
        List<CrackRegion> regions = new ArrayList<>(rt.size());
        for (int i = 0; i < rt.size(); i++) {
            int rx = (int) Math.round(rt.getValue("BX", i));
            int ry = (int) Math.round(rt.getValue("BY", i));
            int rw = (int) Math.round(rt.getValue("Width", i));
            int rh = (int) Math.round(rt.getValue("Height", i));
            int area = (int) Math.round(rt.getValue("Area", i));
            double circularity = rt.getValue("Circ.", i);
            double span = Math.hypot(rw, rh);
            regions.add(new CrackRegion(i + 1, rx, ry, rw, rh, area, span, circularity));
        }

        // 9. Return immutable result snapshot with defensive copies
        return new DetectionResult(image, source.getWidth(), source.getHeight(), retained, regions, config, effectiveThreshold);
    }

    /**
     * Resizes the source image if either dimension exceeds the maximum allowable size,
     * maintaining the original aspect ratio and painting transparency onto white.
     */
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

    /**
     * Throws CancellationException if the current thread has been interrupted.
     */
    private static void checkCancelled() {
        if (Thread.currentThread().isInterrupted()) {
            throw new CancellationException("Analysis cancelled.");
        }
    }
}
