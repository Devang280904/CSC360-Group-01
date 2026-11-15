# Wall Crack Detector

A Java desktop and command-line application that highlights **possible cracks
in wall photographs** using ImageJ-powered automated thresholding and particle
analysis. It runs locally with an embedded ImageJ core library: no Python,
OpenCV, trained model, or external service is required.

This is an educational image-processing project. Detections need visual review;
the application does not determine structural safety or crack severity.

## Features

- Load photographs and inspect original, highlighted overlay, and binary mask views.
- Interactive pan and zoom with mouse wheel, centering, and 1:1 pixel inspection.
- Real-time cursor coordinates and RGB/luminance pixel inspector.
- ImageJ thresholding with automatic method selection (Triangle or Otsu) and sensitivity tuning.
- Region filtering with minimum area floor and maximum circularity constraint.
- Optional rolling-ball background correction and morphological gap closing.
- Sortable measurement table with candidate bounds, pixel counts, diagonals, and circularity.
- Estimated candidate thickness and dominant orientation for faster image-space review.
- Bi-directional highlighting: selecting or double-clicking table rows centers on that crack.
- Desktop menu bar with standard keyboard accelerators (Ctrl+O, F5, Ctrl+E, etc.) and Drag & Drop.
- One-click sample loaders for instant testing with bundled wall photographs.
- Export PNG visualizations, CSV measurements, and a JSON analysis report.
- Analyze images from the command line without a graphical display.
- Keep the desktop responsive with background loading, analysis, and export.
- Run regression tests and generate strict Javadoc.

## Example

These images are **synthetic**, not real wall photographs or evidence of
real-world accuracy. The default detector finds one connected candidate in the
cracked sample and none in the clean sample.

| Input example | Highlighted candidate |
| --- | --- |
| ![Synthetic wall with a branching dark line](samples/wall-cracked.png) | ![Magenta candidate pixels and a red region boundary](docs/demo-overlay.png) |

See [sample generation](samples/README.md).

## Requirements and build

- **JDK 17+**, including `java`, `javac`, `jar`, and `javadoc`.
- Maven 3.9+ (or Bash scripts that invoke Maven).
- A graphical desktop for Swing; command-line operation is headless.

From the repository root, build and launch:

```bash
bash scripts/build.sh
java -jar build/wall-crack-detector.jar
```

Alternatively, use Maven directly:

```bash
mvn package
java -jar target/wall-crack-detector-1.0.0.jar
```

`mvn package` compiles, runs the regression suite, and creates the shaded,
executable JAR with embedded ImageJ dependencies.

## Desktop usage

1. Select **Open photograph** (or a sample button) and choose a wall photo.
2. Adjust settings if necessary, then select **Analyze Wall**.
3. Use the view selector to switch between **Highlighted Overlay** and **Binary Mask**,
   then review the measurements table and summary cards.
4. Select **Export Bundle** and choose an export folder.

Magenta marks accepted pixels; red rectangles identify numbered candidate regions.
Changing settings clears the previous result and disables export until
reanalysis. No arguments, or `--gui`, opens the desktop.

## Command-line usage

```bash
java -jar build/wall-crack-detector.jar \
  --input samples/wall-cracked.png \
  --output output/cracked-demo
```

Expected summary:

```text
Candidate regions: 1
Candidate coverage: 0.434%
Analysis: 800 x 600 pixels
Effective threshold: 215
Widest estimated candidate: 3.00 analysis pixels
```

For faint or custom marks:

```bash
java -jar build/wall-crack-detector.jar \
  --input "path/to/wall.jpg" --output output/wall-review \
  --method Triangle --sensitivity 5 --min-area 40 --max-circularity 0.4
```

| Option | Default | Allowed range / behavior |
| --- | --- | --- |
| `--input PATH` | Required in CLI mode | PNG/JPEG recommended; ImageIO formats supported |
| `--output DIR` | `output/analysis` | Created if necessary; existing reports never replaced |
| `--method NAME` | `Triangle` | `Triangle` or `Otsu` auto-threshold algorithms |
| `--sensitivity N` | `0` | -50 to +50 offset added to the automatic threshold |
| `--min-area N` | `30` | 2–100000 connected candidate pixels |
| `--max-circularity F` | `0.50` | 0.01–1.00; filters out compact circular shapes |
| `--correct-lighting [BOOL]` | `false` | Enable rolling-ball background correction |
| `--closing N` | `0` | 0–10 px morphological closing radius to bridge small gaps |
| `--max-dimension N` | `1600` | 64–2000; maximum analysis width or height |
| `--gui` | No arguments also opens desktop | Requires a graphical display |
| `--help`, `-h` | — | Show usage instructions |

Use a different output folder for each run. Exit codes are `0` for success/help,
`1` for I/O failures, and `2` for invalid arguments or an unavailable headless
desktop. Zero candidates is a successful analysis, not an error.

## Exports and measurement units

| File | Contents |
| --- | --- |
| `overlay.png` | Analysis image with magenta candidate pixels and numbered red bounds |
| `mask.png` | White accepted pixels on a black background |
| `regions.csv` | Bounds, pixel count, box diagonal, circularity, estimated thickness, and orientation |
| `report.json` | Method, dimensions, ImageJ threshold, settings, coverage, and regions |

Coordinates are **zero-based analysis-image pixels**, with the origin at the
top left. Images larger than the configured limit are resized proportionally;
small images are not enlarged.

Coverage is `100 × candidate pixels / analysis image pixels`. Diagonal is the
bounding-box diagonal `sqrt(width² + height²)`, not traced crack length.
Circularity is `4 × pi × (area / perimeter²)`. Measurements are not in millimeters.
Estimated thickness is `candidate pixel count ÷ longest bounding-box side`; it is a screening
aid in the resized analysis image, not a calibrated physical crack width.
Neither coverage nor count represents severity or prediction confidence.

## Detection algorithm

1. Resize proportionally to the analysis limit and composite transparency on white.
2. Convert RGB to weighted grayscale luminance: `(299R + 587G + 114B + 500) / 1000`.
3. Optionally correct uneven illumination using rolling-ball background subtraction.
4. Calculate automatic threshold using ImageJ (`Triangle` or `Otsu`) and apply sensitivity offset.
5. Optionally perform morphological closing (`dilate` followed by `erode`) to bridge hairline gaps.
6. Run ImageJ `ParticleAnalyzer` with minimum area and maximum circularity constraints.
7. Render the accepted regions and export measurements.

See [architecture details](docs/ARCHITECTURE.md).

## Project structure

```text
.
├── pom.xml                         # Maven build and ImageJ dependency integration
├── .github/workflows/ci.yml        # Java compatibility and Javadoc checks
├── docs/                          # Architecture, development guide, evaluation
├── samples/                       # Synthetic clean/cracked examples
├── scripts/
│   ├── build.sh                   # Maven compile and executable shaded JAR
│   ├── test.sh                    # Regression suite
│   └── javadoc.sh                 # Strict API documentation generation
└── src/
    ├── main/java/org/wallcrack/
    │   ├── app/                   # Application entry point
    │   ├── cli/                   # Argument parsing and CLI workflow
    │   ├── detection/             # ImageJ thresholding and particle analysis
    │   ├── io/                    # Image loading, rendering, and exports
    │   ├── model/                 # Configuration and analysis results
    │   └── ui/                    # Swing desktop and previews
    └── test/java/org/wallcrack/    # Regression suite and sample generator
```

## Tests and Javadoc

```bash
bash scripts/test.sh
bash scripts/javadoc.sh
```

Open `target/site/jacoco/index.html` for coverage and `build/docs/index.html` for
API documentation. `mvn test` runs JUnit quickly; `mvn verify` also enforces the
coverage baseline. Field-image evaluation is documented in `evaluation/README.md`.

## Limitations and future work

- Shadows, seams, cables, brick joints, stains, and textured paint may resemble cracks.
- Bright or faint cracks, blur, and low-resolution images may be missed.
- Resizing may remove fine cracks; parameters refer to the resized image.
- Only the first frame is read; EXIF orientation is not automatically applied.
- Inputs above 40 megapixels must be resized before loading.
- No labeled real-world dataset has been evaluated, so no accuracy claim is made.
