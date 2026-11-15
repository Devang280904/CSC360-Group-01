package org.wallcrack;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import org.wallcrack.detection.CrackDetector;
import org.wallcrack.io.ImageFiles;
import org.wallcrack.model.DetectionConfig;

/** Runs presence/absence evaluation over locally supplied labelled photographs. */
public final class FieldEvaluation {
    private FieldEvaluation() { }

    /**
     * Evaluates the manifest using default detector settings.
     * @param args manifest CSV and image-directory paths
     * @throws Exception when labels or images cannot be read
     */
    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("Expected: LABELS.csv IMAGE_DIRECTORY");
        Path manifest = Path.of(args[0]);
        Path images = Path.of(args[1]).normalize();
        int tp = 0, tn = 0, fp = 0, fn = 0, rows = 0;
        for (String line : Files.readAllLines(manifest)) {
            if (line.isBlank() || line.startsWith("#") || line.startsWith("file,")) continue;
            String[] columns = line.split(",", 3);
            if (columns.length < 2) throw new IllegalArgumentException("Invalid label row: " + line);
            boolean expected = switch (columns[1].trim().toLowerCase(Locale.ROOT)) {
                case "present" -> true;
                case "absent" -> false;
                default -> throw new IllegalArgumentException("Expected present or absent: " + line);
            };
            Path image = images.resolve(columns[0].trim()).normalize();
            if (!image.startsWith(images)) throw new IllegalArgumentException("Image escapes evaluation directory: " + image);
            boolean detected = !new CrackDetector().detect(
                    ImageFiles.read(image), DetectionConfig.defaults()).regions().isEmpty();
            if (expected && detected) tp++;
            else if (!expected && !detected) tn++;
            else if (!expected) fp++;
            else fn++;
            rows++;
            System.out.printf("%s expected=%s detected=%s%n", columns[0], expected, detected);
        }
        if (rows == 0) throw new IllegalStateException("No labelled field images. Add rows to " + manifest + ".");
        System.out.printf(Locale.ROOT,
                "TP=%d TN=%d FP=%d FN=%d%nSensitivity=%.3f Specificity=%.3f Accuracy=%.3f%n",
                tp, tn, fp, fn, ratio(tp, tp + fn), ratio(tn, tn + fp), ratio(tp + tn, rows));
    }

    private static double ratio(int numerator, int denominator) {
        return denominator == 0 ? Double.NaN : (double) numerator / denominator;
    }
}
