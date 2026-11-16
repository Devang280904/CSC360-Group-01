# Synthetic demonstration images

These PNGs are generated locally by `org.wallcrack.SampleImages`. They are
synthetic fixtures, not real photographs or a dataset for measuring accuracy.

| File | Contents | Expected default result |
| --- | --- | --- |
| `wall-clean.png` | Light gradient with deterministic fine texture | No candidates |
| `wall-cracked.png` | The same surface with a dark branching line | One connected candidate |

Try the cracked example:

```bash
bash scripts/build.sh
java -jar build/wall-crack-detector.jar --input samples/wall-cracked.png --output output/cracked-demo
```

Regenerate both images after compiling the test sources:

```bash
bash scripts/test.sh
java -cp build/classes:build/test-classes org.wallcrack.SampleImages samples
```

On Windows, use `;` instead of `:` in the Java classpath.
