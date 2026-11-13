# Development and verification

## Commands

Use JDK 17+ from the repository root:

```bash
bash scripts/build.sh
bash scripts/test.sh
bash scripts/javadoc.sh
git diff --check
```

Scripts use UTF-8, `--release 17`, strict compiler/Javadoc checks, and fail on
warnings. The test script compiles production and test sources and runs
`org.wallcrack.AnalysisTestSuite` headlessly. Assertions are explicit, so `-ea`
is unnecessary. Temporary test files are cleaned up after the suite completes.

Maven alternatives are `mvn test` and `mvn package`. The Exec Maven Plugin runs
the plain Java suite in the `test` phase; it is not a JUnit/Surefire test class.
Look for `PASS: 20 regression tests.` and build success. Test assertion failures
fail the Maven goal. `mvn package -DskipTests` explicitly skips execution.

## Regression coverage

| Area | Cases |
| --- | --- |
| Detections | Vertical, diagonal, separate, and edge-touching candidates |
| Rejections | Uniform surfaces, isolated noise, compact patches, smooth gradients |
| Parameters | Sensitivity, minimum area/span, invalid configuration |
| Input | Tiny images, alpha, resizing, PNG decoding, unsupported/missing files |
| Integrity | Bounds and area, immutable snapshots, unchanged source, cancellation |
| Exports | Pixel counts, CSV/JSON metadata, locale, empty results, no overwrite |
| Application | CLI success/errors and headless Swing preview painting |

The 20 regression tests do not certify real-world accuracy. Add labeled real
photos with documented provenance before reporting performance metrics.

## Manual desktop checks

On a workstation with a graphical display:

1. Launch without arguments. Analyze/Export should be disabled initially.
2. Load `samples/wall-cracked.png` and analyze with defaults. Expect one region,
   about 0.434% coverage, an overlay, a binary mask, and one table row.
3. Resize the window and switch tabs; verify image proportions remain correct.
4. Change a setting; confirm the old result clears and Export becomes disabled.
5. Analyze again, then export to a new folder. Compare the output PNG dimensions
   with the JSON analysis dimensions.
6. Export to the same folder again; verify existing files remain unchanged and
   the application reports the conflict.
7. Load `samples/wall-clean.png`; expect zero candidates with defaults.
8. Try an unsupported or missing file; verify readable errors and usable controls.

Headless preview painting is automated. File dialogs, window behavior, and the
complete desktop interaction flow still need these manual checks.

## Contributions and commit scope

Keep detection independent from Swing and filesystem paths. Put reusable data
in `model`, I/O in `io`, and workflow-specific behavior in `cli` or `ui`. Document
public APIs and measurement units. Preserve original photos and exclude
generated reports/build artifacts from Git.

Use cohesive feature commits, for example:

```text
feat: add illumination normalization before thresholding
test: cover shadows in the normalization stage
docs: explain normalization parameters and limitations
```

Run tests, Javadoc checks, and `git diff --check` before committing. Review the
history with `git log --reverse --oneline`.

## Continuous integration

GitHub Actions runs the offline regression suite and strict Javadoc checks on
Java 17, 21, and 25. The workflow requires no external datasets or Maven library
dependencies. Local Maven packaging is verified separately with `mvn package`.
