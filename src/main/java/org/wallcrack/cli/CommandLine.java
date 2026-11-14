package org.wallcrack.cli;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.wallcrack.detection.CrackDetector;
import org.wallcrack.io.ImageFiles;
import org.wallcrack.io.ReportExporter;
import org.wallcrack.model.DetectionConfig;

/** Headless entry point with explicit error codes and configurable analysis parameters. */
public final class CommandLine {
    private static final Set<String> OPTIONS = Set.of(
            "--input", "--output", "--method", "--sensitivity",
            "--min-area", "--max-circularity", "--correct-lighting",
            "--closing", "--max-dimension"
    );

    private static final Set<String> OBSOLETE_OPTIONS = Set.of(
            "--threshold", "--radius", "--min-pixels", "--min-span"
    );

    private CommandLine() { }

    /**
     * Parses arguments, analyzes an image, and writes the report bundle.
     * @param args option/value pairs, or a single help flag
     * @param out standard output destination
     * @param err diagnostic output destination
     * @return 0 on success/help, 1 on I/O failure, 2 on invalid arguments
     */
    public static int run(String[] args, PrintStream out, PrintStream err) {
        if (args.length == 0 || (args.length == 1 && (args[0].equals("--help") || args[0].equals("-h")))) {
            printHelp(out);
            return 0;
        }
        try {
            Map<String, String> options = parse(args);
            if (!options.containsKey("--input")) throw new IllegalArgumentException("--input is required.");
            Path input = Path.of(options.get("--input"));
            Path output = Path.of(options.getOrDefault("--output", "output/analysis"));
            var defaults = DetectionConfig.defaults();

            String method = options.getOrDefault("--method", defaults.thresholdMethod());
            int sensitivity = number(options, "--sensitivity", defaults.sensitivityOffset());
            int minArea = number(options, "--min-area", defaults.minArea());
            double maxCirc = decimal(options, "--max-circularity", defaults.maxCircularity());
            boolean correctLighting = options.containsKey("--correct-lighting")
                    ? Boolean.parseBoolean(options.get("--correct-lighting"))
                    : defaults.correctLighting();
            int closing = number(options, "--closing", defaults.closingRadius());
            int maxDim = number(options, "--max-dimension", defaults.maxDimension());

            var config = new DetectionConfig(method, sensitivity, minArea, maxCirc, correctLighting, closing, maxDim);
            var result = new CrackDetector().detect(ImageFiles.read(input), config);
            ReportExporter.export(result, output);
            out.printf(Locale.ROOT, "Candidate regions: %d%nCandidate coverage: %.3f%%%nAnalysis: %d x %d pixels%nEffective threshold: %d%n",
                    result.regions().size(), result.coveragePercent(), result.width(), result.height(), result.effectiveThreshold());
            out.println("Reports saved to: " + output.toAbsolutePath().normalize());
            out.println("Candidates require visual review; this result does not assess structural safety.");
            return 0;
        } catch (IllegalArgumentException exception) {
            err.println("Argument error: " + exception.getMessage());
            err.println("Use --help for usage.");
            return 2;
        } catch (IOException exception) {
            err.println("I/O error: " + exception.getMessage());
            return 1;
        }
    }

    /**
     * Displays available options and defaults.
     * @param out destination for usage instructions
     */
    public static void printHelp(PrintStream out) {
        out.println("""
                Wall Crack Detector — ImageJ Powered
                Usage: java -jar build/wall-crack-detector.jar --input PHOTO [options]
                       java -jar build/wall-crack-detector.jar [--gui]

                  --input PATH               Input photograph (PNG/JPEG recommended)
                  --output DIR              Export directory (default: output/analysis)
                  --method NAME             Auto-threshold method: Triangle or Otsu (default: Triangle)
                  --sensitivity N           Manual threshold offset, -50 to +50 (default: 0)
                  --min-area N              Minimum particle area in pixels, 2–100000 (default: 30)
                  --max-circularity F       Maximum circularity filter, 0.01–1.0 (default: 0.50)
                  --correct-lighting [BOOL] Enable rolling-ball background correction (default: false)
                  --closing N               Closing radius to connect small gaps, 0–10 (default: 0)
                  --max-dimension N         Analysis size limit, 64–2000 (default: 1600)
                  --help, -h                Show this help

                Each run exports overlay.png, mask.png, regions.csv, and report.json.
                Existing report files are never overwritten. Coordinates use the analysis image.
                """);
    }

    private static Map<String, String> parse(String[] args) {
        Map<String, String> options = new HashMap<>();
        for (int i = 0; i < args.length; i += 2) {
            String option = args[i];
            if (OBSOLETE_OPTIONS.contains(option)) {
                throw new IllegalArgumentException("Option " + option + " is obsolete. Use the new ImageJ parameters. See --help.");
            }
            if (!OPTIONS.contains(option)) throw new IllegalArgumentException("Unknown option: " + option);
            if (i + 1 >= args.length || args[i + 1].startsWith("--") || args[i + 1].isBlank()) {
                throw new IllegalArgumentException("Missing value for " + option);
            }
            if (options.putIfAbsent(option, args[i + 1]) != null) {
                throw new IllegalArgumentException("Duplicate option: " + option);
            }
        }
        return options;
    }

    private static int number(Map<String, String> options, String name, int fallback) {
        try {
            return Integer.parseInt(options.getOrDefault(name, Integer.toString(fallback)));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(name + " must be a whole number.", exception);
        }
    }

    private static double decimal(Map<String, String> options, String name, double fallback) {
        try {
            return Double.parseDouble(options.getOrDefault(name, Double.toString(fallback)));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(name + " must be a decimal number.", exception);
        }
    }
}
