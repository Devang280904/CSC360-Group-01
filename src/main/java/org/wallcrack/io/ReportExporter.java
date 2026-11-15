package org.wallcrack.io;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Locale;
import javax.imageio.ImageIO;
import org.wallcrack.model.DetectionResult;

/** Writes a human-viewable overlay, binary mask, CSV regions, and JSON analysis report. */
public final class ReportExporter {
    /** File names created by each export. Existing files with these names are never replaced. */
    public static final List<String> FILE_NAMES = List.of("overlay.png", "mask.png", "regions.csv", "report.json");

    private ReportExporter() { }

    /**
     * Exports one analysis into a directory. Use a different directory for each run.
     * An I/O failure can leave partial output; successful earlier files are preserved.
     * @param result completed analysis
     * @param directory destination directory, created if necessary
     * @throws IOException if files already exist or output cannot be written
     */
    public static void export(DetectionResult result, Path directory) throws IOException {
        Files.createDirectories(directory);
        for (String name : FILE_NAMES) {
            if (Files.exists(directory.resolve(name))) {
                throw new IOException("Output already exists: " + directory.resolve(name)
                        + ". Choose a new output directory.");
            }
        }
        writePng(ResultImages.overlay(result), directory.resolve("overlay.png"));
        writePng(ResultImages.mask(result), directory.resolve("mask.png"));
        Files.writeString(directory.resolve("regions.csv"), csv(result), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW);
        Files.writeString(directory.resolve("report.json"), json(result), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW);
    }

    private static void writePng(BufferedImage image, Path path) throws IOException {
        try (var output = Files.newOutputStream(path, StandardOpenOption.CREATE_NEW)) {
            if (!ImageIO.write(image, "png", output)) throw new IOException("No PNG writer is available.");
        }
    }

    private static String csv(DetectionResult result) {
        StringBuilder text = new StringBuilder("id,x,y,width,height,pixel_count,box_diagonal_pixels,circularity,estimated_thickness_pixels,orientation\n");
        for (var region : result.regions()) {
            text.append(String.format(Locale.ROOT, "%d,%d,%d,%d,%d,%d,%.3f,%.3f,%.3f,%s%n", region.id(),
                    region.x(), region.y(), region.width(), region.height(), region.pixelCount(), region.span(),
                    region.circularity(), region.estimatedThickness(), region.orientation()));
        }
        return text.toString();
    }

    private static String json(DetectionResult result) {
        var config = result.config();
        StringBuilder text = new StringBuilder(String.format(Locale.ROOT, """
                {
                  "schemaVersion": 2,
                  "engine": "ImageJ 1.54p",
                  "method": "imagej-autothreshold-particle-analysis",
                  "coordinateSystem": "analysis image pixels; origin at top left",
                  "originalWidth": %d,
                  "originalHeight": %d,
                  "analysisWidth": %d,
                  "analysisHeight": %d,
                  "effectiveThreshold": %d,
                  "candidateCount": %d,
                  "candidatePixels": %d,
                  "coveragePercent": %.6f,
                  "maximumEstimatedThicknessPixels": %.3f,
                  "config": {
                    "thresholdMethod": "%s",
                    "sensitivityOffset": %d,
                    "minArea": %d,
                    "maxCircularity": %.2f,
                    "correctLighting": %b,
                    "closingRadius": %d,
                    "maxDimension": %d
                  },
                  "regions": [
                """, result.originalWidth(), result.originalHeight(), result.width(), result.height(),
                result.effectiveThreshold(), result.regions().size(), result.candidatePixels(), result.coveragePercent(),
                result.maximumEstimatedThickness(),
                config.thresholdMethod(), config.sensitivityOffset(), config.minArea(), config.maxCircularity(),
                config.correctLighting(), config.closingRadius(), config.maxDimension()));
        for (int i = 0; i < result.regions().size(); i++) {
            var region = result.regions().get(i);
            text.append(String.format(Locale.ROOT,
                    "    {\"id\": %d, \"x\": %d, \"y\": %d, \"width\": %d, \"height\": %d, "
                    + "\"pixelCount\": %d, \"boxDiagonal\": %.3f, \"circularity\": %.3f, "
                    + "\"estimatedThicknessPixels\": %.3f, \"orientation\": \"%s\"}%s%n",
                    region.id(), region.x(), region.y(), region.width(), region.height(),
                    region.pixelCount(), region.span(), region.circularity(), region.estimatedThickness(), region.orientation(),
                    i + 1 < result.regions().size() ? "," : ""));
        }
        return text.append("  ]\n}\n").toString();
    }
}
