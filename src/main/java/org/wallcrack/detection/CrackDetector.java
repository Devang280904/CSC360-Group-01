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
 */
public final class CrackDetector {
    /** Creates a reusable, thread-safe detector powered by ImageJ core. */
    public CrackDetector() { }

    /**
     * Finds dark candidate regions using ImageJ automatic thresholding
     * and particle analysis. Input pixels are never modified.
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

        checkCancelled();
        ColorProcessor cp = new ColorProcessor(image);
        cp.setRGBWeights(0.299, 0.587, 0.114);
        ByteProcessor bp = cp.convertToByteProcessor();

        if (config.correctLighting()) {
            checkCancelled();
            new BackgroundSubtracter().rollingBallBackground(bp, 50, false, true, false, false, true);
        }

        checkCancelled();
        AutoThresholder.Method method = AutoThresholder.Method.valueOf(config.thresholdMethod());
        int autoThreshold = new AutoThresholder().getThreshold(method, bp.getHistogram());
        int effectiveThreshold = Math.max(0, Math.min(255, autoThreshold + config.sensitivityOffset()));

        bp.setThreshold(0, effectiveThreshold, ImageProcessor.NO_LUT_UPDATE);
        ByteProcessor binaryMask = bp.createMask();

        if (config.closingRadius() > 0) {
            checkCancelled();
            for (int i = 0; i < config.closingRadius(); i++) binaryMask.dilate(1, 0);
            for (int i = 0; i < config.closingRadius(); i++) binaryMask.erode(1, 0);
        }

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
        pa.setHideOutputImage(true);

        checkCancelled();
        ImagePlus maskImp = new ImagePlus("mask", binaryMask);
        if (!pa.analyze(maskImp)) {
            throw new IllegalStateException("ImageJ particle analysis failed.");
        }

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

        return new DetectionResult(image, source.getWidth(), source.getHeight(), retained, regions, config, effectiveThreshold);
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
