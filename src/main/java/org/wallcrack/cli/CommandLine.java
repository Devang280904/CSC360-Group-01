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
    private static final Set<String> OPTIONS = Set.of("--input", "--output", "--threshold", "--radius",
            "--min-pixels", "--min-span", "--max-dimension");

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
            var config = new DetectionConfig(number(options, "--threshold", defaults.contrastThreshold()),
                    number(options, "--radius", defaults.localRadius()),
                    number(options, "--min-pixels", defaults.minComponentPixels()),
                    number(options, "--min-span", defaults.minSpan()),
                    number(options, "--max-dimension", defaults.maxDimension()));
            var result = new CrackDetector().detect(ImageFiles.read(input), config);
            ReportExporter.export(result, output);
            out.printf(Locale.ROOT, "Possible crack regions: %d%nCandidate coverage: %.3f%%%nAnalysis: %d x %d pixels%n",
                    result.regions().size(), result.coveragePercent(), result.width(), result.height());
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

    /** @param out destination for usage instructions */
    public static void printHelp(PrintStream out) {
        out.println("""
                Wall Crack Detector — Java 17+
                Usage: java -jar build/wall-crack-detector.jar --input PHOTO [options]

                  --input PATH          Input photograph (PNG/JPEG recommended)
                  --output DIR          Export directory (default: output/analysis)
                  --threshold N         Local darkness threshold, 1–100 (default: 14)
                  --radius N            Neighborhood radius, 2–50 (default: 12)
                  --min-pixels N        Minimum connected pixels, 2–100000 (default: 30)
                  --min-span N          Minimum region diagonal, 2–1000 (default: 15)
                  --max-dimension N     Analysis size limit, 64–2000 (default: 1600)
                  --help, -h            Show this help

                Each run exports overlay.png, mask.png, regions.csv, and report.json.
                Existing report files are never overwritten. Coordinates use the analysis image.
                """);
    }

    private static Map<String, String> parse(String[] args) {
        Map<String, String> options = new HashMap<>();
        for (int i = 0; i < args.length; i += 2) {
            String option = args[i];
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
}
