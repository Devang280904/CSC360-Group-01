# ImageJ migration plan

**Status:** Completed.
**Date:** 2026-11-14

The application now uses ImageJ 1.54p core for automated thresholding,
illumination correction, morphological cleanup, and particle analysis.

## Implemented architecture

```text
Photograph
  -> Resize proportionally & composite transparency on white
  -> Convert to 8-bit grayscale via ColorProcessor
  -> Optional BackgroundSubtracter (rolling ball)
  -> AutoThresholder (Triangle or Otsu) + sensitivity offset
  -> Optional ByteProcessor closing (dilate + erode)
  -> ParticleAnalyzer with min-area & max-circularity filters
  -> Immutable DetectionResult snapshot
  -> Review in desktop UI or export report bundle
```

## Summary of changes

1. **Dependency:** Embedded `net.imagej:ij:1.54p` via Maven Shade plugin into a single runnable JAR.
2. **Engine replacement:** `CrackDetector` now delegates entirely to ImageJ core APIs. Custom BFS traversal, integral image, and heuristic slenderness ratios were completely removed.
3. **Model:**
   - `DetectionConfig`: supports `thresholdMethod`, `sensitivityOffset`, `minArea`, `maxCircularity`, `correctLighting`, `closingRadius`, and `maxDimension`.
   - `CrackRegion`: includes ImageJ `circularity` metric.
   - `DetectionResult`: records `effectiveThreshold` and preserves defensive copying.
4. **Desktop UI:** Streamlined controls, added ImageJ parameters, removed obsolete presets and severity labels, and kept interactive zoom/pan, drag-and-drop, and table navigation.
5. **CLI & Exports:** Supports the new ImageJ parameters, rejects obsolete flags cleanly with helpful messages, and outputs CSV with circularity and JSON schema version 2.
6. **Tests:** All 20 regression tests pass headlessly. Strict Javadoc generation passes with zero warnings.

## Completed phases

- [x] **Phase 1: Evaluate and choose defaults:** Evaluated Triangle vs Otsu, with and without rolling-ball background correction across synthetic fixtures. Selected Triangle without correction as default.
- [x] **Phase 2: Replace the engine:** Added ImageJ dependency, updated `CrackDetector`, `DetectionConfig`, `CrackRegion`, `DetectionResult`, `ReportExporter`, and `CommandLine`.
- [x] **Phase 3: Streamline review workflow:** Simplified controls in `DetectorWorkspace` and `DetectorFrame`, removed misleading severity classifications.
- [x] **Phase 4: Complete delivery & documentation:** Built standalone shaded JAR, updated build/test/javadoc scripts, and updated all documentation.
