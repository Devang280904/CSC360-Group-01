package org.wallcrack.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import org.wallcrack.detection.CrackDetector;
import org.wallcrack.io.ImageFiles;
import org.wallcrack.io.ReportExporter;
import org.wallcrack.io.ResultImages;
import org.wallcrack.model.DetectionConfig;
import org.wallcrack.model.DetectionResult;

/**
 * Desktop workflow for image selection, background analysis, visual review, and export.
 * @serial exclude
 */
final class DetectorWorkspace extends JPanel {
    private static final long serialVersionUID = 1L;
    private final JButton openButton = DesktopTheme.button("Open photograph", false);
    private final JButton analyzeButton = DesktopTheme.button("Analyze image", true);
    private final JButton exportButton = DesktopTheme.button("Export results", false);
    private final JSpinner threshold = spinner(14, 1, 100);
    private final JSpinner radius = spinner(12, 2, 50);
    private final JSpinner minPixels = spinner(30, 2, 100_000);
    private final JSpinner minSpan = spinner(15, 2, 1000);
    private final JSpinner maxDimension = spinner(1600, 64, 2000);
    private final transient List<JSpinner> settings = List.of(threshold, radius, minPixels, minSpan, maxDimension);
    private final ImagePanel original = new ImagePanel("Open a wall photograph to begin");
    private final ImagePanel overlay = new ImagePanel("Run analysis to see highlighted candidates");
    private final ImagePanel mask = new ImagePanel("Accepted pixels appear in white");
    private final JLabel status = new JLabel("Ready. Open a well-lit photograph of a wall.");
    private final JLabel summary = new JLabel("Analyze a photograph to view measurements");
    private final JLabel fileInfo = new JLabel("No photograph selected");
    private final JLabel regionCount = metricValue("—");
    private final JLabel coverage = metricValue("—");
    private final JLabel analysisSize = metricValue("—");
    private final JProgressBar progress = new JProgressBar();
    private final DefaultTableModel table = new DefaultTableModel(
            new Object[]{"Region", "X", "Y", "Width", "Height", "Pixels", "Span (px)"}, 0) {
        private static final long serialVersionUID = 1L;
        @Override public boolean isCellEditable(int row, int column) { return false; }
        @Override public Class<?> getColumnClass(int column) { return column == 6 ? Double.class : Integer.class; }
    };
    private transient BufferedImage source;
    private transient DetectionResult result;
    private boolean busy;

    /** Creates the workspace; instantiate on the Swing event dispatch thread. */
    DetectorWorkspace() {
        super(new BorderLayout(20, 20));
        setBackground(DesktopTheme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(24, 24, 14, 24));
        add(header(), BorderLayout.NORTH);
        add(controls(), BorderLayout.WEST);
        add(workspace(), BorderLayout.CENTER);
        JPanel footer = new JPanel(new BorderLayout(12, 0));
        footer.setOpaque(false);
        status.setFont(DesktopTheme.font(12, false));
        status.setForeground(DesktopTheme.MUTED);
        progress.setPreferredSize(new Dimension(110, 5));
        progress.setVisible(false);
        footer.add(status, BorderLayout.CENTER);
        footer.add(progress, BorderLayout.EAST);
        add(footer, BorderLayout.SOUTH);
        openButton.setMnemonic('O');
        analyzeButton.setMnemonic('A');
        exportButton.setMnemonic('E');
        openButton.addActionListener(event -> openImage());
        analyzeButton.addActionListener(event -> analyze());
        exportButton.addActionListener(event -> export());
        for (JSpinner setting : settings) setting.addChangeListener(event -> invalidateAnalysis());
        updateControls();
    }

    private JPanel header() {
        JPanel panel = new JPanel(new BorderLayout(20, 0));
        panel.setOpaque(false);
        JPanel heading = new JPanel(new GridLayout(0, 1, 0, 6));
        heading.setOpaque(false);
        JLabel title = DesktopTheme.label("Wall Crack Detector", 28, true, DesktopTheme.INK);
        heading.add(title);
        heading.add(DesktopTheme.label("A clearer view of your wall's condition.", 14, false, DesktopTheme.MUTED));
        panel.add(heading, BorderLayout.CENTER);
        JPanel actions = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 10, 5));
        actions.setOpaque(false);
        actions.add(openButton);
        actions.add(exportButton);
        panel.add(actions, BorderLayout.EAST);
        return panel;
    }

    private JPanel controls() {
        JPanel sidebar = new JPanel(new BorderLayout(0, 12));
        sidebar.setOpaque(false);
        sidebar.setPreferredSize(new Dimension(350, 500));

        JPanel settingsPanel = new JPanel(new java.awt.GridBagLayout());
        settingsPanel.setBackground(Color.WHITE);
        settingsPanel.setBorder(BorderFactory.createEmptyBorder(20, 18, 12, 18));
        var constraints = new java.awt.GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 1;
        constraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        constraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        constraints.insets = new java.awt.Insets(0, 0, 6, 0);
        settingsPanel.add(DesktopTheme.label("DETECTION SETTINGS", 12, true, DesktopTheme.MUTED), constraints);
        constraints.gridy++;
        constraints.insets = new java.awt.Insets(0, 0, 14, 0);
        settingsPanel.add(DesktopTheme.label("Fine-tune your analysis", 17, true, DesktopTheme.INK), constraints);
        addSetting(settingsPanel, constraints, "Darkness threshold", threshold,
                "Lower values pick up fainter marks.");
        addSetting(settingsPanel, constraints, "Neighborhood radius", radius,
                "Area around each pixel, in pixels.");
        addSetting(settingsPanel, constraints, "Minimum connected pixels", minPixels,
                "Ignore smaller isolated marks.");
        addSetting(settingsPanel, constraints, "Minimum region span", minSpan,
                "Bounding-box diagonal, in pixels.");
        addSetting(settingsPanel, constraints, "Analysis size limit", maxDimension,
                "Maximum image dimension, in pixels.");
        constraints.gridy++;
        constraints.weighty = 1;
        JPanel spacer = new JPanel();
        spacer.setOpaque(false);
        settingsPanel.add(spacer, constraints);

        JScrollPane scroll = new JScrollPane(settingsPanel);
        scroll.setBorder(BorderFactory.createLineBorder(DesktopTheme.LINE));
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        scroll.setMinimumSize(new Dimension(350, 160));
        sidebar.add(scroll, BorderLayout.CENTER);

        JPanel guide = new JPanel(new GridLayout(0, 1, 0, 5));
        guide.setBackground(new Color(232, 239, 249));
        guide.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        guide.add(DesktopTheme.label("Reading your results", 13, true, DesktopTheme.INK));
        guide.add(DesktopTheme.label("●  Candidate pixels", 12, false, new Color(170, 20, 113)));
        guide.add(DesktopTheme.label("□  Region boundaries", 12, false, new Color(177, 47, 56)));
        guide.add(DesktopTheme.label("No structural safety assessment.", 11, false, DesktopTheme.MUTED));
        JPanel actions = new JPanel(new BorderLayout(0, 12));
        actions.setOpaque(false);
        actions.add(analyzeButton, BorderLayout.NORTH);
        actions.add(guide, BorderLayout.CENTER);
        sidebar.add(actions, BorderLayout.SOUTH);
        return sidebar;
    }

    private JPanel workspace() {
        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setOpaque(false);
        JPanel metrics = new JPanel(new GridLayout(1, 3, 12, 0));
        metrics.setOpaque(false);
        metrics.add(metric("POSSIBLE REGIONS", regionCount));
        metrics.add(metric("CANDIDATE COVERAGE", coverage));
        metrics.add(metric("ANALYSIS SIZE", analysisSize));
        panel.add(metrics, BorderLayout.NORTH);

        java.awt.CardLayout viewCards = new java.awt.CardLayout();
        JPanel results = new JPanel(viewCards);
        results.add(overlay, "Highlighted");
        results.add(mask, "Binary mask");
        JComboBox<String> viewMode = new JComboBox<>(new String[]{"Highlighted", "Binary mask"});
        viewMode.setFont(DesktopTheme.font(11, false));
        viewMode.getAccessibleContext().setAccessibleName("Result preview mode");
        viewMode.addActionListener(event -> viewCards.show(results, (String) viewMode.getSelectedItem()));
        JPanel input = previewCard("Original photograph", fileInfo, original, null);
        JPanel output = previewCard("Detection result",
                DesktopTheme.label("Possible cracks shown in magenta", 11, false, DesktopTheme.MUTED), results, viewMode);
        JSplitPane previews = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, input, output);
        previews.setResizeWeight(0.5);
        previews.setDividerLocation(0.5);
        previews.setDividerSize(12);
        previews.setContinuousLayout(true);
        previews.setBorder(BorderFactory.createEmptyBorder());
        previews.setOpaque(false);

        JTable regions = new JTable(table);
        regions.setAutoCreateRowSorter(true);
        regions.setFillsViewportHeight(true);
        regions.setRowHeight(40);
        regions.setShowVerticalLines(false);
        regions.setGridColor(DesktopTheme.LINE);
        regions.setIntercellSpacing(new Dimension(0, 1));
        regions.setSelectionBackground(new Color(225, 235, 255));
        regions.setSelectionForeground(DesktopTheme.INK);
        regions.getTableHeader().setFont(DesktopTheme.font(12, true));
        regions.getTableHeader().setBackground(new Color(247, 249, 252));
        regions.getTableHeader().setForeground(DesktopTheme.MUTED);
        regions.getTableHeader().setPreferredSize(new Dimension(100, 40));
        regions.getTableHeader().setReorderingAllowed(false);
        var cells = new javax.swing.table.DefaultTableCellRenderer();
        cells.setHorizontalAlignment(JLabel.CENTER);
        regions.setDefaultRenderer(Integer.class, cells);
        regions.setDefaultRenderer(Double.class, cells);
        JScrollPane rows = new JScrollPane(regions);
        rows.setColumnHeaderView(regions.getTableHeader());
        rows.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, DesktopTheme.LINE));
        rows.getViewport().setBackground(Color.WHITE);
        rows.setPreferredSize(new Dimension(600, 116));
        JPanel measurements = new JPanel(new BorderLayout(0, 0));
        measurements.setBackground(Color.WHITE);
        measurements.setBorder(BorderFactory.createLineBorder(DesktopTheme.LINE));
        JPanel tableHeading = new JPanel(new BorderLayout(12, 0));
        tableHeading.setOpaque(false);
        tableHeading.setBorder(BorderFactory.createEmptyBorder(13, 16, 13, 16));
        tableHeading.add(DesktopTheme.label("Region measurements", 14, true, DesktopTheme.INK), BorderLayout.WEST);
        summary.setFont(DesktopTheme.font(11, false));
        summary.setForeground(DesktopTheme.MUTED);
        tableHeading.add(summary, BorderLayout.CENTER);
        measurements.add(tableHeading, BorderLayout.NORTH);
        measurements.add(rows, BorderLayout.CENTER);
        measurements.setMinimumSize(new Dimension(400, 145));
        JSplitPane content = new JSplitPane(JSplitPane.VERTICAL_SPLIT, previews, measurements);
        content.setResizeWeight(1.0);
        content.setDividerSize(14);
        content.setContinuousLayout(true);
        content.setBorder(BorderFactory.createEmptyBorder());
        content.setOpaque(false);
        panel.add(content, BorderLayout.CENTER);
        return panel;
    }

    private static JPanel previewCard(String title, JLabel detail, java.awt.Component content,
                                     java.awt.Component action) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createLineBorder(DesktopTheme.LINE));
        card.setMinimumSize(new Dimension(200, 180));
        JPanel heading = new JPanel(new BorderLayout(0, 5));
        heading.setOpaque(false);
        heading.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        JPanel titleRow = new JPanel(new BorderLayout(8, 0));
        titleRow.setOpaque(false);
        titleRow.setPreferredSize(new Dimension(200, 28));
        titleRow.add(DesktopTheme.label(title, 14, true, DesktopTheme.INK), BorderLayout.CENTER);
        if (action != null) titleRow.add(action, BorderLayout.EAST);
        heading.add(titleRow, BorderLayout.CENTER);
        detail.setFont(DesktopTheme.font(11, false));
        detail.setForeground(DesktopTheme.MUTED);
        heading.add(detail, BorderLayout.SOUTH);
        card.add(heading, BorderLayout.NORTH);
        card.add(content, BorderLayout.CENTER);
        return card;
    }

    private static JPanel metric(String caption, JLabel value) {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(DesktopTheme.LINE),
                BorderFactory.createEmptyBorder(14, 16, 14, 16)));
        card.add(DesktopTheme.label(caption, 10, true, DesktopTheme.MUTED), BorderLayout.NORTH);
        card.add(value, BorderLayout.CENTER);
        return card;
    }

    private static JLabel metricValue(String text) {
        return DesktopTheme.label(text, 24, true, DesktopTheme.INK);
    }

    private void openImage() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Choose a wall photograph");
        chooser.setFileFilter(new FileNameExtensionFilter("Photographs (PNG, JPEG, BMP)", "png", "jpg", "jpeg", "bmp"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        Path file = chooser.getSelectedFile().toPath();
        runTask("Loading photograph...", () -> ImageFiles.read(file), image -> {
            source = image;
            original.setImage(image);
            fileInfo.setText(file.getFileName() + "  ·  " + image.getWidth() + " × " + image.getHeight() + " px");
            fileInfo.setToolTipText(file.toAbsolutePath().toString());
            invalidateAnalysis();
            status.setText("Loaded " + file.getFileName() + " — " + image.getWidth() + " × " + image.getHeight() + " pixels.");
        });
    }

    private void analyze() {
        try {
            // Commit typed spinner values even if focus has not left the editor.
            for (JSpinner setting : settings) setting.commitEdit();
        } catch (java.text.ParseException exception) {
            showError("Enter whole numbers within the settings' allowed ranges.");
            return;
        }
        var config = new DetectionConfig(value(threshold), value(radius), value(minPixels),
                value(minSpan), value(maxDimension));
        BufferedImage input = source;
        runTask("Analyzing local contrast and connected regions...", () -> {
            var detection = new CrackDetector().detect(input, config);
            return new AnalysisViews(detection, ResultImages.overlay(detection), ResultImages.mask(detection));
        }, views -> {
            result = views.result();
            overlay.setImage(views.overlay());
            mask.setImage(views.mask());
            table.setRowCount(0);
            for (var region : result.regions()) {
                table.addRow(new Object[]{region.id(), region.x(), region.y(), region.width(),
                        region.height(), region.pixelCount(), Math.round(region.span() * 100.0) / 100.0});
            }
            regionCount.setText(Integer.toString(result.regions().size()));
            coverage.setText(String.format(Locale.ROOT, "%.3f%%", result.coveragePercent()));
            analysisSize.setText(result.width() + " × " + result.height());
            summary.setText(result.regions().isEmpty() ? "No candidates found with these settings" : "Coordinates in analysis pixels");
            status.setText("Analysis complete. Table coordinates use the analysis image. Review candidates before exporting.");
        });
    }

    private void export() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Choose a results folder (existing reports will not be replaced)");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        Path directory = chooser.getSelectedFile().toPath();
        DetectionResult completed = result;
        runTask("Exporting images and reports...", () -> {
            ReportExporter.export(completed, directory);
            return directory;
        }, destination -> status.setText("Saved overlay, mask, CSV, and JSON to " + destination.toAbsolutePath()));
    }

    private <T> void runTask(String message, Callable<T> task, Consumer<T> success) {
        busy = true;
        status.setText(message);
        progress.setVisible(true);
        progress.setIndeterminate(true);
        updateControls();
        new SwingWorker<T, Void>() {
            @Override protected T doInBackground() throws Exception { return task.call(); }
            @Override protected void done() {
                try {
                    success.accept(get());
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showError("Operation interrupted.");
                } catch (ExecutionException exception) {
                    Throwable cause = exception.getCause();
                    showError(cause.getMessage() == null ? cause.toString() : cause.getMessage());
                } finally {
                    busy = false;
                    progress.setIndeterminate(false);
                    progress.setVisible(false);
                    updateControls();
                }
            }
        }.execute();
    }

    private void invalidateAnalysis() {
        result = null;
        overlay.setImage(null);
        mask.setImage(null);
        table.setRowCount(0);
        summary.setText("Analyze a photograph to view measurements");
        regionCount.setText("—");
        coverage.setText("—");
        analysisSize.setText("—");
        if (source != null) status.setText("Settings changed. Run analysis to update the results.");
        updateControls();
    }

    private void updateControls() {
        openButton.setEnabled(!busy);
        analyzeButton.setEnabled(!busy && source != null);
        exportButton.setEnabled(!busy && result != null);
        for (JSpinner setting : settings) setting.setEnabled(!busy);
    }

    private void showError(String message) {
        status.setText("Operation failed. " + message);
        JOptionPane.showMessageDialog(this, message, "Wall Crack Detector", JOptionPane.ERROR_MESSAGE);
    }

    private static JSpinner spinner(int initial, int min, int max) {
        return new JSpinner(new SpinnerNumberModel(initial, min, max, 1));
    }

    private static int value(JSpinner spinner) { return ((Number) spinner.getValue()).intValue(); }

    private static void addSetting(JPanel panel, java.awt.GridBagConstraints constraints,
                                   String label, JSpinner spinner, String description) {
        constraints.gridy++;
        constraints.insets = new java.awt.Insets(0, 0, 5, 0);
        JLabel caption = DesktopTheme.label(label, 12, true, DesktopTheme.INK);
        caption.setLabelFor(spinner);
        panel.add(caption, constraints);
        constraints.gridy++;
        constraints.insets = new java.awt.Insets(0, 0, 4, 0);
        spinner.setToolTipText(description);
        spinner.setPreferredSize(new Dimension(260, 42));
        spinner.setEditor(new JSpinner.NumberEditor(spinner, "0"));
        var editor = (JSpinner.NumberEditor) spinner.getEditor();
        editor.getTextField().setFont(DesktopTheme.font(15, false));
        editor.getTextField().setHorizontalAlignment(JLabel.LEFT);
        panel.add(spinner, constraints);
        constraints.gridy++;
        constraints.insets = new java.awt.Insets(0, 0, 8, 0);
        panel.add(DesktopTheme.label(description, 11, false, DesktopTheme.MUTED), constraints);
    }

    private record AnalysisViews(DetectionResult result, BufferedImage overlay, BufferedImage mask) { }
}
