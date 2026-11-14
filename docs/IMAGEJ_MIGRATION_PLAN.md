# ImageJ migration plan

**Status:** Planned; not implemented.  
**Date:** 2026-11-14

The current application uses a custom, standard-library-only detector. This
document describes its proposed replacement; it does not describe capabilities
already available in the application.

## Goal and scope

Use ImageJ for the actual image-processing work, rather than merely wrapping
images in ImageJ objects. Prioritize simple implementation and a clear review
workflow over preserving the custom algorithm or adding more controls.

The recommended scope is:

- Embed ImageJ 1.x core as a library using one pinned `net.imagej:ij` dependency.
- Keep the existing Swing application and headless CLI.
- Ship one executable JAR containing the dependency; users should not need a
  separate ImageJ or Fiji installation.
- Replace custom thresholding, connected-component traversal, and shape filtering
  with ImageJ operations.
- Keep one production pipeline and one evaluated default configuration.
- Preserve existing image-loading safeguards, immutable results, and export
  no-overwrite behavior.

This plan assumes that embedding ImageJ satisfies the project requirement.
Running as an ImageJ plugin is a different integration and is outside this scope.

## Proposed processing pipeline

```text
Photograph
  -> Resize and grayscale
  -> Optional uneven-lighting correction
  -> Automatic threshold with sensitivity adjustment
  -> Optional binary gap closing
  -> Particle analysis and filtering
  -> Immutable application result
  -> Review and export
```

| Stage | ImageJ API | Intended behavior |
| --- | --- | --- |
| Preparation | `ColorProcessor`, `ImageProcessor` | Resize without enlarging, preserve aspect ratio, and convert to 8-bit grayscale with explicit RGB weights |
| Lighting correction | `BackgroundSubtracter` | Evaluate rolling-ball background subtraction for dark features on a light background |
| Thresholding | `AutoThresholder`, `ImageProcessor`, `createMask()` | Select a threshold, adjust it within valid bounds, and generate a binary candidate mask |
| Binary cleanup | `ByteProcessor` dilation and erosion | Optionally close small gaps using one closing operation |
| Region extraction | `ParticleAnalyzer`, private `ResultsTable` | Filter by minimum area and circularity; produce accepted particles and measurements |
| Application boundary | Small conversion adapter | Build the existing immutable result representation from ImageJ output |

Composite transparency on white before processing. Keep the color analysis image
for overlays; grayscale conversion and filtering must not change the source
photograph.

### Thresholding evaluation

Compare four initial configurations:

| Configuration | Purpose |
| --- | --- |
| Triangle | Candidate for sparse dark features against a dominant background |
| Otsu | Baseline for images with separable foreground/background intensities |
| Lighting correction + Triangle | Test whether normalization improves unevenly lit images |
| Lighting correction + Otsu | Compare normalization with the alternative threshold |

These are candidates, not validated recommendations for wall-crack accuracy.
Select the default method, lighting-correction setting, and parameter values
after evaluation. Do not silently change methods based on image content.

Global thresholding can segment texture or illumination variation even when an
image contains no cracks. Lighting correction may help, but can also suppress
features at unsuitable radii. Test both positive and negative examples.

ImageJ's Auto Local Threshold plugin provides methods such as Mean and Sauvola,
but is not part of the core-only dependency proposed here. Consider that extra
plugin only if evaluation demonstrates that the simpler pipeline is inadequate.
Do not implement another custom local-threshold algorithm.

### Binary processing

Start conservatively:

- Remove small components through particle-size filtering.
- Leave gap closing off by default initially.
- Do not apply opening or erosion alone by default; they can erase hairlines.
- Do not add skeletonization, watershed, or automatic hole filling.

The optional **Connect small gaps** control should perform one documented closing
operation: dilation followed by erosion, with explicit foreground polarity.
Closing can merge separate marks and increase measured area. Record its use in
the report and evaluate the effect on thin, branching, and adjacent candidates.

### Particle-analysis semantics

Use ImageJ's minimum-area and circularity filters instead of the current custom
`span² / pixelCount` filter. Circularity is not equivalent to that ratio and does
not establish that a particle is a crack.

Keep edge-touching candidates and preserve holes unless explicitly configured
otherwise. Verify diagonal connectivity and hole behavior with fixtures rather
than assuming equivalence with the old eight-neighbor traversal.

Obtain the accepted-particle mask and measurements from the same analysis.
Normalize output to actual white foreground values (`255`) on black (`0`), not
just an inverted display lookup table. Explicitly threshold foreground pixels
for analysis rather than relying on ImageJ's saved binary preferences.

## User experience

### Main workflow

**Open photograph -> Analyze -> Review -> Export**

Keep the existing previews, zoom and pan, pixel inspection, region table,
selection navigation, sample loading, drag-and-drop, and background execution.
Do not replace the application interface with ImageJ windows.

### Simplified settings

| Main controls | Advanced controls |
| --- | --- |
| Detection sensitivity | Threshold method: Triangle or Otsu |
| Minimum region size | Lighting-correction radius |
| Correct uneven lighting | Maximum circularity |
| Analyze and Reset | Analysis resolution |
| | Connect small gaps |

Sensitivity should adjust the automatically selected threshold with a bounded,
documented mapping. For dark candidates, increasing the upper intensity cutoff
includes more pixels. Display the effective threshold in analysis details.
Do not reuse the old local-darkness parameter under a new label.

Replace the existing five presets with one evaluated default initially.
Introduce additional presets only if examples support their usefulness; do not
carry forward old parameter values or imply validated structural classifications.

### Clear result wording

- Remove Minor / Moderate / Major severity labels, which currently depend only
  on bounding-box span in pixels.
- Replace CLEAN with **No candidates found**.
- Use **candidate regions**, not confirmed cracks.
- Show a short explanation: **Highlighted regions require visual review.
  Measurements are in image pixels.**
- Ensure toolbar, menu, and selection actions affect the currently displayed
  result view, including the binary mask.

### Background work and state

Keep the existing `SwingWorker` approach and permit one operation at a time.
Guard all entry points consistently, including menus, sliders, presets, and
drag-and-drop. Disable conflicting actions during work and restore controls after
success or failure. Do not publish results for an outdated image or configuration.

Changing settings should continue to invalidate results and disable export.
Avoid live analysis on every slider movement in this migration. Verify
interruption between stages; do not promise immediate cancellation inside every
ImageJ operation.

## Architecture and affected files

```text
Swing / CLI
    -> CrackDetector: orchestrates ImageJ operations
    -> DetectionResult: immutable application snapshot
    -> Existing previews and exporters
```

Keep mutable ImageJ processors and tables inside the detection layer. Avoid
exposing them through the model or using them directly from Swing controls.
Use direct APIs, not menu commands or macros. Pass a private `ResultsTable` to
`ParticleAnalyzer`; suppress ImageJ windows and avoid the global ROI Manager.
Check the selected APIs for dependence on global preferences or display state.

| Files | Planned changes |
| --- | --- |
| `pom.xml` | Add pinned ImageJ dependency and executable dependency-inclusive packaging |
| `src/main/java/org/wallcrack/detection/CrackDetector.java` | Replace custom processing with a short sequence of ImageJ calls |
| `src/main/java/org/wallcrack/model/DetectionConfig.java` | Replace old local-threshold settings with validated pipeline settings |
| `src/main/java/org/wallcrack/model/DetectionPreset.java` | Simplify to the evaluated default; remove obsolete profiles |
| `src/main/java/org/wallcrack/model/CrackRegion.java` | Adapt measurement fields, including circularity |
| `src/main/java/org/wallcrack/model/DetectionResult.java` | Preserve immutable snapshots; include effective processing metadata |
| `src/main/java/org/wallcrack/ui/DetectorWorkspace.java` | Simplify controls, revise wording, and guard background-work state |
| `src/main/java/org/wallcrack/ui/DetectorFrame.java` | Align menus, help text, and actions with the new workflow |
| `src/main/java/org/wallcrack/ui/ImagePanel.java` | Preserve interaction; fix affected active-view/navigation behavior |
| `src/main/java/org/wallcrack/cli/CommandLine.java` | Expose the new configuration and clearly reject removed options |
| `src/main/java/org/wallcrack/io/ReportExporter.java` | Update measurements, method metadata, and report schema |
| `scripts/`, `.github/workflows/ci.yml` | Use the authoritative Maven build for packaging, tests, and Javadoc |
| `src/test/java/org/wallcrack/` | Preserve application contracts and add ImageJ-specific fixtures |

Retain image decoding and existing visualization/export code where practical.
Simple loops for snapshot conversion, pixel counting, and rendering are
acceptable; the goal is to remove custom detection algorithms, not all glue code.

Do not introduce a generic pipeline framework, plugin loader, service container,
parallel legacy engine, or separate abstraction for every processing step.

## Measurements, exports, and compatibility

Keep `overlay.png`, `mask.png`, `regions.csv`, and `report.json`.

Initial measurements:

- Region identifier and bounding box.
- Accepted area in pixels.
- Bounding-box diagonal, explicitly labeled; not traced crack length.
- Circularity measured by ImageJ.
- Total accepted candidate coverage.

Use ImageJ measurements where available. Keep straightforward calculations such
as coverage and bounding-box diagonal in application code. Keep coordinates
zero-based in analysis-image pixels, with original dimensions available for
conversion. Explicitly use pixel calibration, not physical units.

The table, accepted mask, CSV, and JSON must agree. Rejected particles must not
remain in the exported mask or contribute to coverage.

Increment the JSON schema version. Record the engine and ImageJ version,
processing steps, configured and effective threshold, method, lighting-correction
parameters, cleanup settings, particle filters, polarity/connectivity policy,
dimensions, and measurement definitions. Update CSV headers where semantics change.

Old numeric outputs are comparison baselines, not mandatory targets for a
different algorithm. Do not silently reinterpret obsolete CLI flags. Document
removed options, replacements, and schema changes with new example commands.

## Build and distribution

Make Maven the authoritative build and pin a verified ImageJ release. Produce one
dependency-inclusive runnable JAR, for example through the Maven Shade Plugin.
Keep Java 17 compatibility.

Turn the shell scripts into thin Maven wrappers rather than maintaining a second
dependency-aware compilation system. Configure strict compilation and Javadoc
checks in Maven, and keep the existing regression suite wired into its test phase.
Document artifact paths consistently.

The first build requires dependency/plugin downloads. Offline builds require
those artifacts to be cached. The packaged application should run without
internet access or a separate ImageJ installation.

## Implementation phases

### Phase 1: Evaluate and choose defaults

- [ ] Capture current sample outputs and relevant regression baselines.
- [ ] Compare the four proposed threshold/lighting configurations.
- [ ] Include clean walls, gradients, texture, faint lines, branching cracks,
  broad dark marks, and edge-touching features.
- [ ] Use synthetic fixtures plus representative photographs with documented
  provenance and permission; keep tuning and evaluation images separate.
- [ ] Record configuration, candidate count, coverage, visual observations, and
  processing time; choose and document the initial default.
- [ ] Verify direct ImageJ calls work headlessly without auxiliary windows.

Synthetic fixtures verify implementation behavior, not field accuracy. Without
labeled independent photographs, report qualitative observations rather than
precision/recall or an accuracy claim.

### Phase 2: Replace the engine

- [ ] Add the pinned dependency and implement the ImageJ processing sequence.
- [ ] Adapt configuration, region measurements, and immutable result metadata.
- [ ] Verify accepted masks, measurements, polarity, connectivity, and holes.
- [ ] Remove custom thresholding, breadth-first traversal, and slenderness logic
  once the replacement is verified; do not ship both engines.

### Phase 3: Simplify the review workflow

- [ ] Replace obsolete controls and profiles with the new default and settings.
- [ ] Remove unsupported severity labels and revise result/help wording.
- [ ] Ensure binary-mask and overlay actions target the visible view.
- [ ] Guard all task entry points and prevent stale-result publication.
- [ ] Manually verify loading, tuning, analysis, navigation, and export.

### Phase 4: Complete delivery and documentation

- [ ] Update exports, schema, CLI flags, and example commands.
- [ ] Package and test the dependency-inclusive executable JAR.
- [ ] Update scripts, CI, strict Javadoc, README, architecture, and developer guide.
- [ ] Regenerate demo outputs only after defaults are selected; explain changed
  measurements rather than preserving obsolete example numbers.

## Verification and acceptance criteria

Preserve tests for input validation, resizing, transparency, unchanged source
images, immutable results, locale-independent reports, CLI errors, no-overwrite
exports, and headless preview rendering.

Add or adapt tests for:

- Both threshold methods and sensitivity direction/bounds.
- Uniform images, crack-free textured images, and smooth lighting gradients.
- Binary foreground polarity independent of display lookup tables/preferences.
- Diagonal connectivity, edge-touching candidates, and particles with holes.
- Minimum area and circularity filtering.
- Thin features, branching regions, and the merging effects of optional closing.
- Agreement between table measurements, accepted-mask area, CSV, and JSON.
- Repeatable results without contamination from previous analyses.
- Headless CLI execution from the packaged JAR and absence of ImageJ windows.
- UI busy-state guards, setting invalidation, active-view actions, and recovery
  after errors; use manual checks where headless tests cannot cover interaction.

The migration is complete when:

1. ImageJ performs thresholding, binary processing, and particle analysis.
2. Desktop and headless CLI use the same pipeline from one packaged JAR.
3. No ImageJ dialogs or auxiliary windows appear.
4. Accepted masks and exported measurements agree.
5. One evaluated default supports a minimal-tuning review workflow.
6. Documentation distinguishes candidate detection from physical measurement,
   structural severity, confidence, or safety assessment.

## References

- [ImageJ ImageProcessor source](https://github.com/imagej/ImageJ/blob/master/ij/process/ImageProcessor.java)
- [ImageJ ByteProcessor source](https://github.com/imagej/ImageJ/blob/master/ij/process/ByteProcessor.java)
- [ImageJ BackgroundSubtracter source](https://github.com/imagej/ImageJ/blob/master/ij/plugin/filter/BackgroundSubtracter.java)
- [ImageJ ParticleAnalyzer source](https://github.com/imagej/ImageJ/blob/master/ij/plugin/filter/ParticleAnalyzer.java)
- [Auto Threshold methods and caveats](https://imagej.net/plugins/auto-threshold)
- [Auto Local Threshold plugin](https://imagej.net/plugins/auto-local-threshold)
- [ImageJ core Maven metadata](https://repo.maven.apache.org/maven2/net/imagej/ij/maven-metadata.xml)

Source links may track newer code than the eventual pinned dependency. Verify
method availability and behavior against the selected release during implementation.
