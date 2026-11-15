# Development and verification

## Commands

Use JDK 17+ and Maven from the repository root:

```bash
bash scripts/build.sh
bash scripts/test.sh
bash scripts/javadoc.sh
git diff --check
```

Scripts use UTF-8, `--release 17`, strict compiler and Javadoc checks. The test
script runs JUnit 5 headlessly, packages the app, and enforces the JaCoCo coverage
baseline. Temporary files are cleaned up. Open `target/site/jacoco/index.html`
for the detailed coverage report.

Use `mvn test` for a quick run or `mvn verify` for the full coverage gate. Look
for `PASS: 27 regression tests.`, two successful JUnit tests, and build success.

## Regression coverage

| Area | Cases |
| --- | --- |
| Detections | Vertical, diagonal, separate, and edge-touching candidates |
| Rejections | Uniform surfaces, isolated noise, compact patches, smooth gradients |
| Parameters | Triangle/Otsu, sensitivity, lighting correction, closing, area, circularity, invalid configuration |
| Input | Tiny images, alpha composite, resizing, PNG decoding, unsupported/missing files |
| Integrity | Defensive copies, immutable snapshots, unchanged source, cancellation |
| Exports | Pixel counts, CSV/JSON metadata, locale independence, empty results, no overwrite |
| Application | CLI modes, bundled samples, desktop state transitions, active-view zoom, and Swing painting |

The regression tests verify software behavior and implementation stability, not field accuracy.
Use `bash scripts/evaluate.sh` after adding labelled photographs as described in
`evaluation/README.md` to measure dataset-specific field performance.

## Manual desktop checks

On a workstation with a graphical display:

1. Launch without arguments (`java -jar build/wall-crack-detector.jar`). The window should open
   with larger controls. Analyze and Export should be disabled initially.
2. Load `samples/wall-cracked.png` and select **Analyze Wall** with defaults. Expect one region,
   about 0.434% coverage, an overlay, a binary mask, and one table row.
3. Switch view modes between Highlighted Overlay and Binary Mask; verify zoom and pan work smoothly.
4. Double-click the table row to verify centering on that region.
5. Change a setting; confirm the old result clears and Export becomes disabled.
6. Export to a new folder and verify the four exported files.
7. Load `samples/wall-clean.png`; expect zero candidate regions.
8. Test drag-and-drop of an image onto the window.
