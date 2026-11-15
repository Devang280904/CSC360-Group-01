# Labelled field-image evaluation

Add consented, de-identified wall photographs under `evaluation/images/`; images
are ignored by Git so potentially sensitive photographs are not committed.

Record `file,expected,notes` rows in `labels.csv`, where expected is `present` or
`absent`, then run `bash scripts/evaluate.sh`. The tool prints a confusion matrix,
sensitivity, specificity, and accuracy. These are dataset-specific screening
metrics, not structural-safety validation. It refuses to score an empty dataset.
