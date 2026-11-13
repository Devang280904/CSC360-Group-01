package org.wallcrack.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
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
public final class DetectorFrame extends JFrame {
    private static final long serialVersionUID = 1L;
    private final JButton openButton = new JButton("1. Open photograph");
    private final JButton analyzeButton = new JButton("2. Analyze image");
    private final JButton exportButton = new JButton("3. Export results");
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
    private final JLabel summary = new JLabel("No analysis yet");
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

    /** Creates the main window; instantiate on the Swing event dispatch thread. */
    public DetectorFrame() {
        super("Wall Crack Detector");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(960, 650));
        setSize(1200, 800);
        setLocationRelativeTo(null);
        JPanel root = new JPanel(new BorderLayout(16, 16));
        root.setBorder(BorderFactory.createEmptyBorder(16, 16, 12, 16));
        setContentPane(root);
        root.add(header(), BorderLayout.NORTH);
        root.add(controls(), BorderLayout.WEST);
        root.add(workspace(), BorderLayout.CENTER);
        JPanel footer = new JPanel(new BorderLayout(12, 0));
        progress.setPreferredSize(new Dimension(120, 16));
        progress.setVisible(false);
        footer.add(status, BorderLayout.CENTER);
        footer.add(progress, BorderLayout.EAST);
        root.add(footer, BorderLayout.SOUTH);
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
        JPanel panel = new JPanel(new GridLayout(0, 1, 0, 6));
        JLabel title = new JLabel("Wall Crack Detector");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 26f));
        title.setForeground(new Color(28, 53, 82));
        panel.add(title);
        panel.add(new JLabel("Inspect a photograph, highlight possible cracks, and export your findings."));
        return panel;
    }

    private JPanel controls() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setPreferredSize(new Dimension(220, 500));
        for (JButton button : List.of(openButton, analyzeButton, exportButton)) {
            button.setAlignmentX(LEFT_ALIGNMENT);
            button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
            panel.add(button);
            panel.add(Box.createVerticalStrut(8));
        }
        panel.add(Box.createVerticalStrut(12));
        addSetting(panel, "Darkness threshold", threshold, "Lower values detect fainter marks but also more texture.");
        addSetting(panel, "Neighborhood radius (px)", radius, "Measure darkness against nearby pixels.");
        addSetting(panel, "Minimum connected pixels", minPixels, "Reject small isolated components.");
        addSetting(panel, "Minimum region span (px)", minSpan, "Minimum bounding-box diagonal, not physical crack length.");
        addSetting(panel, "Maximum analysis dimension", maxDimension, "Larger images are resized to this limit.");
        panel.add(Box.createVerticalGlue());
        JLabel note = new JLabel("<html>Magenta: candidate pixels<br>Red: region bounds<br><br>Review candidates visually.<br>Results do not determine<br>structural safety.</html>");
        note.setForeground(new Color(85, 99, 116));
        panel.add(note);
        return panel;
    }

    private JPanel workspace() {
        JTabbedPane results = new JTabbedPane();
        results.addTab("Highlighted candidates", overlay);
        results.addTab("Binary mask", mask);
        JPanel input = new JPanel(new BorderLayout(0, 6));
        input.add(new JLabel("Original photograph"), BorderLayout.NORTH);
        input.add(original, BorderLayout.CENTER);
        JSplitPane previews = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, input, results);
        previews.setResizeWeight(0.5);
        previews.setBorder(BorderFactory.createEmptyBorder());
        JTable regions = new JTable(table);
        regions.setAutoCreateRowSorter(true);
        regions.setFillsViewportHeight(true);
        regions.setRowHeight(25);
        JPanel measurements = new JPanel(new BorderLayout(0, 8));
        measurements.add(summary, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(regions);
        scroll.setPreferredSize(new Dimension(700, 150));
        measurements.add(scroll, BorderLayout.CENTER);
        JSplitPane content = new JSplitPane(JSplitPane.VERTICAL_SPLIT, previews, measurements);
        content.setResizeWeight(0.75);
        content.setBorder(BorderFactory.createEmptyBorder());
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(content);
        return panel;
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
            summary.setText(String.format(Locale.ROOT, "%d possible regions · %.3f%% coverage · analysis %d × %d px",
                    result.regions().size(), result.coveragePercent(), result.width(), result.height()));
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
        summary.setText("No current analysis");
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

    private static void addSetting(JPanel panel, String label, JSpinner spinner, String tooltip) {
        JLabel caption = new JLabel(label);
        caption.setLabelFor(spinner);
        panel.add(caption);
        spinner.setToolTipText(tooltip);
        spinner.setAlignmentX(LEFT_ALIGNMENT);
        spinner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        panel.add(spinner);
        panel.add(Box.createVerticalStrut(10));
    }

    private record AnalysisViews(DetectionResult result, BufferedImage overlay, BufferedImage mask) { }
}
