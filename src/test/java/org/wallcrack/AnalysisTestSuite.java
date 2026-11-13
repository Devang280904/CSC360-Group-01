package org.wallcrack;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Locale;
import java.util.concurrent.CancellationException;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import org.wallcrack.cli.CommandLine;
import org.wallcrack.detection.CrackDetector;
import org.wallcrack.io.ImageFiles;
import org.wallcrack.io.ReportExporter;
import org.wallcrack.io.ResultImages;
import org.wallcrack.model.DetectionConfig;
import org.wallcrack.model.DetectionResult;
import org.wallcrack.ui.ImagePanel;

/** Dependency-free regression suite that fails with a nonzero exit on any incorrect behavior. */
public final class AnalysisTestSuite {
    private static final DetectionConfig DEFAULTS = DetectionConfig.defaults();
    private static int passed;

    private AnalysisTestSuite() { }

    /**
     * Executes detector, I/O, CLI, and headless preview integration tests.
     * @param args unused
     * @throws Exception on a test failure
     */
    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        test("uniform wall has no candidates", () -> check(detect(SampleImages.blank()).regions().isEmpty(), "blank wall"));
        test("vertical crack is located and measured", () -> {
            var result = detect(SampleImages.verticalCrack());
            check(result.regions().size() == 1, "one connected crack");
            var region = result.regions().get(0);
            check(region.x() == 158 && region.y() == 35 && region.width() == 3 && region.height() == 170, "bounds");
            check(region.pixelCount() == 510 && result.candidatePixels() == 510, "mask area");
            check(result.isCandidate(159, 100) && !result.isCandidate(20, 20), "mask positions");
        });
        test("diagonal pixels use eight-connectivity", () -> {
            var image = SampleImages.blank();
            for (int i = 40; i < 180; i++) image.setRGB(i, i, 0);
            check(detect(image).regions().size() == 1, "diagonal must remain connected");
        });
        test("separate cracks remain separate", () -> {
            var image = SampleImages.verticalCrack();
            for (int y = 40; y < 190; y++) image.setRGB(40, y, 0);
            check(detect(image).regions().size() == 2, "two regions");
        });
        test("isolated noise is rejected", () -> {
            var image = SampleImages.blank();
            for (int y = 10; y < 230; y += 20) {
                for (int x = 10; x < 310; x += 20) image.setRGB(x, y, 0);
            }
            check(detect(image).regions().isEmpty(), "speckles");
        });
        test("compact dark patch is rejected", () -> {
            var image = SampleImages.blank();
            for (int y = 80; y < 92; y++) {
                for (int x = 80; x < 92; x++) image.setRGB(x, y, 0);
            }
            check(detect(image).regions().isEmpty(), "compact shape");
        });
        test("smooth lighting gradient is rejected", () -> {
            var image = SampleImages.blank();
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    int gray = 140 + x / 4;
                    image.setRGB(x, y, (gray << 16) | (gray << 8) | gray);
                }
            }
            check(detect(image).regions().isEmpty(), "smooth gradient");
        });
        test("threshold controls faint line sensitivity", () -> {
            var image = SampleImages.blank();
            for (int y = 30; y < 200; y++) image.setRGB(150, y, 0xDCDCDC);
            check(detect(image).regions().isEmpty(), "default threshold rejects faint line");
            var sensitive = new DetectionConfig(3, 12, 30, 15, 1600);
            check(new CrackDetector().detect(image, sensitive).regions().size() == 1, "low threshold accepts line");
        });
        test("edge cracks do not wrap rows", () -> {
            var image = SampleImages.blank();
            for (int y = 20; y < 210; y++) image.setRGB(0, y, 0);
            var result = detect(image);
            check(result.regions().size() == 1 && result.regions().get(0).width() == 1, "left-edge bounds");
        });
        test("tiny and transparent inputs are supported", () -> {
            check(detect(SampleImages.solid(1, 1, 0)).regions().isEmpty(), "one pixel");
            var transparent = new BufferedImage(80, 80, BufferedImage.TYPE_INT_ARGB);
            check(detect(transparent).regions().isEmpty(), "transparency composited on white");
            check((detect(transparent).analysisImage().getRGB(0, 0) & 0xFFFFFF) == 0xFFFFFF, "white composite");
        });
        test("analysis resizing preserves original dimensions", () -> {
            var result = new CrackDetector().detect(SampleImages.solid(640, 320, 230),
                    new DetectionConfig(14, 12, 30, 15, 160));
            check(result.width() == 160 && result.height() == 80, "analysis limit");
            check(result.originalWidth() == 640 && result.originalHeight() == 320, "original metadata");
        });
        test("minimum area and span filters are honored", () -> {
            var image = SampleImages.verticalCrack();
            check(new CrackDetector().detect(image, new DetectionConfig(14, 12, 600, 15, 1600)).regions().isEmpty(), "area");
            check(new CrackDetector().detect(image, new DetectionConfig(14, 12, 30, 200, 1600)).regions().isEmpty(), "span");
        });
        test("results protect image and list state", () -> {
            var image = SampleImages.verticalCrack();
            int before = image.getRGB(159, 100);
            var result = detect(image);
            check(image.getRGB(159, 100) == before, "detector does not mutate input");
            image.setRGB(159, 100, 0xFFFFFF);
            var copy = result.analysisImage();
            copy.setRGB(159, 100, 0xFFFFFF);
            check(result.analysisImage().getRGB(159, 100) == before, "defensive image copy");
            expect(UnsupportedOperationException.class, () -> result.regions().clear());
            expect(IndexOutOfBoundsException.class, () -> result.isCandidate(-1, 0));
        });
        test("invalid configuration is rejected", () -> {
            expect(IllegalArgumentException.class, () -> new DetectionConfig(0, 12, 30, 15, 1600));
            expect(IllegalArgumentException.class, () -> new DetectionConfig(14, 1, 30, 15, 1600));
            expect(IllegalArgumentException.class, () -> new DetectionConfig(14, 12, 1, 15, 1600));
            expect(IllegalArgumentException.class, () -> new DetectionConfig(14, 12, 30, 1, 1600));
            expect(IllegalArgumentException.class, () -> new DetectionConfig(14, 12, 30, 15, 2001));
        });
        test("interrupted analysis cancels promptly", () -> {
            Thread.currentThread().interrupt();
            try { expect(CancellationException.class, () -> detect(SampleImages.blank())); }
            finally { Thread.interrupted(); }
        });
        Path temporary = Files.createTempDirectory("wallcrack-tests-");
        try {
            test("PNG round-trip and corrupt input handling", () -> {
                Path photo = temporary.resolve("wall.png");
                ImageIO.write(SampleImages.verticalCrack(), "png", photo.toFile());
                check(detect(ImageFiles.read(photo)).regions().size() == 1, "decoded crack");
                Path invalid = temporary.resolve("bad.png");
                Files.writeString(invalid, "not an image");
                expect(IOException.class, () -> ImageFiles.read(invalid));
                expect(IOException.class, () -> ImageFiles.read(temporary.resolve("missing.png")));
            });
            test("exports contain consistent masks and numeric reports", () -> {
                var result = detect(SampleImages.verticalCrack());
                Path output = temporary.resolve("exports");
                Locale previous = Locale.getDefault();
                try {
                    Locale.setDefault(Locale.GERMANY);
                    ReportExporter.export(result, output);
                } finally { Locale.setDefault(previous); }
                for (String name : ReportExporter.FILE_NAMES) check(Files.size(output.resolve(name)) > 0, name);
                var binary = ImageIO.read(output.resolve("mask.png").toFile());
                int whitePixels = 0;
                for (int y = 0; y < result.height(); y++) {
                    for (int x = 0; x < result.width(); x++) {
                        if ((binary.getRGB(x, y) & 0xFFFFFF) == 0xFFFFFF) whitePixels++;
                    }
                }
                check(whitePixels == result.candidatePixels(), "exported mask count");
                String csv = Files.readString(output.resolve("regions.csv"));
                check(csv.lines().count() == 2 && csv.contains("1,158,35,3,170,510,"), "CSV region");
                String json = Files.readString(output.resolve("report.json"));
                check(json.contains("\"candidateCount\": 1") && json.contains("\"coveragePercent\": 0.664063"), "JSON measurements");
                byte[] before = Files.readAllBytes(output.resolve("overlay.png"));
                expect(IOException.class, () -> ReportExporter.export(result, output));
                check(java.util.Arrays.equals(before, Files.readAllBytes(output.resolve("overlay.png"))), "no overwrite");
                check(ResultImages.overlay(result).getWidth() == result.width(), "overlay dimensions");
            });
            test("empty result exports a valid empty region collection", () -> {
                Path output = temporary.resolve("empty");
                ReportExporter.export(detect(SampleImages.blank()), output);
                check(Files.readAllLines(output.resolve("regions.csv")).size() == 1, "header-only CSV");
                check(Files.readString(output.resolve("report.json")).contains("\"candidateCount\": 0"), "empty JSON");
            });
            test("CLI success, argument validation, and I/O exit codes", () -> {
                Path output = temporary.resolve("cli");
                var bytes = new ByteArrayOutputStream();
                try (var stream = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
                    check(CommandLine.run(new String[]{"--help"}, stream, stream) == 0, "help");
                    check(CommandLine.run(new String[]{"--input", temporary.resolve("wall.png").toString(),
                            "--output", output.toString()}, stream, stream) == 0, "successful analysis");
                    check(Files.isRegularFile(output.resolve("report.json")), "CLI report");
                    check(CommandLine.run(new String[]{"--input", "missing"}, stream, stream) == 1, "missing file");
                    check(CommandLine.run(new String[]{"--unknown", "x"}, stream, stream) == 2, "unknown flag");
                    check(CommandLine.run(new String[]{"--input"}, stream, stream) == 2, "missing value");
                    check(CommandLine.run(new String[]{"--output", "x"}, stream, stream) == 2, "missing input");
                    check(CommandLine.run(new String[]{"--input", "x", "--input", "y"}, stream, stream) == 2, "duplicate");
                    check(CommandLine.run(new String[]{"--input", "x", "--threshold", "abc"}, stream, stream) == 2, "not integer");
                    check(CommandLine.run(new String[]{"--input", "x", "--threshold", "101"}, stream, stream) == 2, "out of range");
                }
            });
            test("Swing preview paints images and placeholders headlessly", () -> SwingUtilities.invokeAndWait(() -> {
                var panel = new ImagePanel("Load a photograph");
                panel.setSize(300, 200);
                BufferedImage canvas = new BufferedImage(300, 200, BufferedImage.TYPE_INT_RGB);
                Graphics2D graphics = canvas.createGraphics();
                try {
                    panel.paint(graphics);
                    panel.setImage(SampleImages.verticalCrack());
                    panel.paint(graphics);
                    check((canvas.getRGB(150, 100) & 0xFFFFFF) != 0, "preview painted");
                } finally { graphics.dispose(); }
            }));
        } finally {
            try (var paths = Files.walk(temporary)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
            }
        }
        System.out.println("PASS: " + passed + " regression tests.");
    }

    private static DetectionResult detect(BufferedImage image) { return new CrackDetector().detect(image, DEFAULTS); }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    private static void test(String name, CheckedRunnable task) throws Exception {
        task.run();
        passed++;
        System.out.println("  PASS " + name);
    }
    private static void expect(Class<? extends Throwable> type, CheckedRunnable task) throws Exception {
        try { task.run(); }
        catch (Throwable failure) {
            if (type.isInstance(failure)) return;
            throw new AssertionError("Expected " + type.getSimpleName() + " but got " + failure, failure);
        }
        throw new AssertionError("Expected " + type.getSimpleName());
    }
    @FunctionalInterface private interface CheckedRunnable { void run() throws Exception; }
}
