# ImageJ evaluation and migration verification

## Segment 1: baseline and default selection

Environment: ImageJ 1.54p, JDK 26, headless execution, Java 17 target.
The 20 existing regression tests passed before engine replacement.
The original branching fixture has 1 region, 2084 pixels, and 0.434% coverage.

Compared Triangle and Otsu, with and without rolling-ball correction (radius 50,
light background, presmoothing off). Particle filtering used minimum area 30 and
maximum circularity 0.5. Gap closing was off.

Results below are **regions / accepted pixels**:

| Synthetic fixture | Triangle | Triangle + correction | Otsu | Otsu + correction |
| --- | --- | --- | --- | --- |
| Bundled clean texture | 0 / 0 | 0 / 0 | 57 / 211972 | 72 / 198365 |
| Bundled branching line | 1 / 2084 | 1 / 2084 | 1 / 2084 | 1 / 2084 |
| Uniform surface | 0 / 0 | 0 / 0 | 0 / 0 | 0 / 0 |
| Smooth gradient | 0 / 0 | 0 / 0 | 0 / 0 | 0 / 0 |
| Faint one-pixel line | 1 / 170 | 1 / 170 | 1 / 170 | 1 / 170 |
| Solid 100 x 100 dark patch | 0 / 0 | 1 / 1572 | 0 / 0 | 1 / 788 |
| Left-edge one-pixel line | 1 / 190 | 1 / 190 | 1 / 190 | 1 / 190 |

Fixture provenance: bundled samples are generated with seed 360 by `SampleImages`.
Other fixtures are generated locally: uniform 320 x 240 intensity 230, horizontal
gradient `140 + x/4`, line intensity 220 at x=150/y=30..199, black patch
x=100..199/y=70..169, and black edge line x=0/y=20..209.

Single-run timings ranged from approximately 0.6–89 ms without correction and
4–143 ms with correction, including JVM warm-up effects. These are observations,
not a benchmark. All runs completed headlessly without ImageJ windows.

**Initial defaults:** Triangle, sensitivity offset 0, minimum area 30, maximum
circularity 0.5, correction off, closing off, maximum dimension 1600.
Correction remains optional because it can turn broad patches into edge-like
candidates. Both methods are exposed in Advanced settings.

Global thresholding can classify entire surfaces as foreground; particle
circularity filtering rejected some such surfaces in these fixtures. Zero
accepted particles does not prove that thresholding correctly separated cracks.
These fixtures establish implementation behavior only.

**Pending:** representative real-wall photographs with documented provenance and
independent annotations. None are present in the repository; no real-world
accuracy claim or independently validated preset is made.
