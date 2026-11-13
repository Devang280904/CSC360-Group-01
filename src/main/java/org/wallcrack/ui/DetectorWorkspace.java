package org.wallcrack.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
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
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import org.wallcrack.detection.CrackDetector;
import org.wallcrack.io.ImageFiles;
import org.wallcrack.io.ReportExporter;
import org.wallcrack.io.ResultImages;
import org.wallcrack.model.CrackRegion;
import org.wallcrack.model.DetectionConfig;
import org.wallcrack.model.DetectionPreset;
import org.wallcrack.model.DetectionResult;

/**
 * Desktop workflow providing image selection, parameter tuning, background detection,
 * interactive inspection, and multi-format report exports.
 *
 * @serial exclude
 */
public final class DetectorWorkspace extends JPanel {
    private static final long serialVersionUID = 1L;

    // Action buttons
    private final JButton openButton = DesktopTheme.button("Open Photograph", false);
    private final JButton analyzeButton = DesktopTheme.button("Analyze Wall", true);
    private final JButton exportButton = DesktopTheme.button("Export Bundle", false);
    private final JButton resetDefaultsButton = DesktopTheme.button("Reset Defaults", false);
    private final JButton sampleCrackedButton = DesktopTheme.iconButton("Cracked Wall", "Load sample wall photograph with visible crack");
    private final JButton sampleCleanButton = DesktopTheme.iconButton("Clean Wall", "Load sample uniform wall photograph");

    // Detection settings
    private final JSpinner threshold = spinner(14, 1, 100);
    private final JSpinner radius = spinner(12, 2, 50);
    private final JSpinner minPixels = spinner(30, 2, 100_000);
    private final JSpinner minSpan = spinner(15, 2, 1000);
    private final JSpinner maxDimension = spinner(1600, 64, 2000);
    private final transient List<JSpinner> settings = List.of(threshold, radius, minPixels, minSpan, maxDimension);
    private final JComboBox<String> presetCombo = new JComboBox<>(new String[]{
            "Balanced (Default)", "Hairline & Fine Cracks", "Prominent & Severe Cracks",
            "Textured & Rough Wall", "High-Resolution Detail", "Custom"
    });
    private boolean adjustingPresets;

    // Image previews
    private final ImagePanel original = new ImagePanel("Open a wall photograph to begin inspection");
    private final ImagePanel overlay = new ImagePanel("Run analysis to see highlighted crack candidates");
    private final ImagePanel mask = new ImagePanel("Accepted crack pixels appear in white");
    private final java.awt.CardLayout viewCards = new java.awt.CardLayout();
    private final JPanel resultsCardPanel = new JPanel(viewCards);
    private final JComboBox<String> viewMode = new JComboBox<>(new String[]{"Highlighted Overlay", "Binary Mask"});

    // Zoom badges
    private final JLabel originalZoomBadge = DesktopTheme.badge("Fit", new Color(241, 245, 249), DesktopTheme.MUTED);
    private final JLabel resultZoomBadge = DesktopTheme.badge("Fit", new Color(241, 245, 249), DesktopTheme.MUTED);

    // Status & KPI indicators
    private final JLabel statusDot = new JLabel("●");
    private final JLabel status = new JLabel("Ready. Open a wall photograph or sample to begin.");
    private final JLabel pixelInspector = new JLabel("Cursor: —");
    private final JLabel fileInfo = new JLabel("No photograph selected");
    private final JLabel regionCount = metricValue("—");
    private final JLabel regionBadge = DesktopTheme.badge("IDLE", new Color(241, 245, 249), DesktopTheme.MUTED);
    private final JLabel coverage = metricValue("—");
    private final JLabel coverageDetail = DesktopTheme.label("0 pixels flagged", 11, false, DesktopTheme.MUTED);
    private final JLabel maxSpanMetric = metricValue("—");
    private final JLabel maxSpanBadge = DesktopTheme.badge("NONE", new Color(241, 245, 249), DesktopTheme.MUTED);
    private final JLabel analysisSize = metricValue("—");
    private final JLabel analysisScaleDetail = DesktopTheme.label("No active image", 11, false, DesktopTheme.MUTED);
    private final JProgressBar progress = new JProgressBar();
    private final JLabel summary = new JLabel("Analyze a photograph to view region measurements");

    // Region measurements table
    private final DefaultTableModel table = new DefaultTableModel(
            new Object[]{"ID", "Severity", "X (px)", "Y (px)", "Width (px)", "Height (px)", "Pixels", "Span (px)"}, 0) {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }

        @Override
        public Class<?> getColumnClass(int column) {
            return switch (column) {
                case 0, 2, 3, 4, 5, 6 -> Integer.class;
                case 1 -> String.class;
                case 7 -> Double.class;
                default -> Object.class;
            };
        }
    };
    private final JTable regionTable = new JTable(table);

    private transient BufferedImage source;
    private transient Path currentFilePath;
    private transient DetectionResult result;
    private boolean busy;

    /**
     * Creates the desktop workspace layout; instantiate on the Swing event dispatch thread.
     */
    public DetectorWorkspace() {
        super(new BorderLayout(0, 16));
        setBackground(DesktopTheme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(18, 20, 12, 20));

        add(createHeaderPanel(), BorderLayout.NORTH);
        add(createMainSplit(), BorderLayout.CENTER);
        add(createStatusBar(), BorderLayout.SOUTH);

        configureInteractions();
        configureDragAndDrop();
        updateControls();
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout(16, 0));
        header.setOpaque(false);

        // Title and tagline
        JPanel titleArea = new JPanel();
        titleArea.setLayout(new BoxLayout(titleArea, BoxLayout.Y_AXIS));
        titleArea.setOpaque(false);

        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        titleRow.setOpaque(false);
        JLabel title = DesktopTheme.label("Wall Crack Detector", 24, true, DesktopTheme.INK);
        JLabel versionBadge = DesktopTheme.badge("v1.0.0", new Color(238, 242, 255), DesktopTheme.BLUE);
        titleRow.add(title);
        titleRow.add(versionBadge);

        JLabel subtitle = DesktopTheme.label("High-precision edge contrast and connected region inspection for wall surfaces.",
                13, false, DesktopTheme.MUTED);
        titleArea.add(titleRow);
        titleArea.add(Box.createVerticalStrut(4));
        titleArea.add(subtitle);
        header.add(titleArea, BorderLayout.WEST);

        // Top quick action buttons
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        actions.setOpaque(false);

        JLabel sampleLabel = DesktopTheme.label("Samples:", 12, true, DesktopTheme.MUTED);
        sampleCrackedButton.setPreferredSize(new Dimension(104, 38));
        sampleCleanButton.setPreferredSize(new Dimension(94, 38));
        actions.add(sampleLabel);
        actions.add(sampleCrackedButton);
        actions.add(sampleCleanButton);
        actions.add(Box.createHorizontalStrut(8));
        actions.add(openButton);
        actions.add(analyzeButton);
        actions.add(exportButton);

        header.add(actions, BorderLayout.EAST);
        return header;
    }

    private JPanel createMainSplit() {
        JPanel container = new JPanel(new BorderLayout(0, 14));
        container.setOpaque(false);

        // Top KPI metrics cards
        container.add(createMetricsRow(), BorderLayout.NORTH);

        // Split between left settings sidebar and right visual inspection area
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, createSidebar(), createInspectionArea());
        splitPane.setResizeWeight(0.0);
        splitPane.setDividerLocation(340);
        splitPane.setDividerSize(10);
        splitPane.setContinuousLayout(true);
        splitPane.setBorder(BorderFactory.createEmptyBorder());
        splitPane.setOpaque(false);

        container.add(splitPane, BorderLayout.CENTER);
        return container;
    }

    private JPanel createMetricsRow() {
        JPanel metrics = new JPanel(new GridLayout(1, 4, 12, 0));
        metrics.setOpaque(false);
        metrics.setPreferredSize(new Dimension(100, 84));

        metrics.add(metricCard("CRACK CANDIDATES", regionCount, regionBadge));
        metrics.add(metricCard("SURFACE COVERAGE", coverage, coverageDetail));
        metrics.add(metricCard("MAX CRACK SPAN", maxSpanMetric, maxSpanBadge));
        metrics.add(metricCard("RESOLUTION / BUDGET", analysisSize, analysisScaleDetail));
        return metrics;
    }

    private JPanel metricCard(String title, JLabel mainValue, JComponent subComponent) {
        JPanel card = DesktopTheme.cardPanel();
        card.setLayout(new BorderLayout(0, 4));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(DesktopTheme.LINE),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)));

        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);
        topRow.add(DesktopTheme.label(title, 10, true, DesktopTheme.MUTED), BorderLayout.WEST);
        if (subComponent instanceof JLabel badge && badge.isOpaque() == false) {
            topRow.add(subComponent, BorderLayout.EAST);
        }

        card.add(topRow, BorderLayout.NORTH);
        card.add(mainValue, BorderLayout.CENTER);
        if (!(subComponent instanceof JLabel badge && badge.isOpaque() == false)) {
            card.add(subComponent, BorderLayout.SOUTH);
        }
        return card;
    }

    private static JLabel metricValue(String text) {
        return DesktopTheme.label(text, 22, true, DesktopTheme.INK);
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout(0, 10));
        sidebar.setOpaque(false);
        sidebar.setPreferredSize(new Dimension(340, 500));
        sidebar.setMinimumSize(new Dimension(280, 400));

        JPanel settingsPanel = new JPanel(new GridBagLayout());
        settingsPanel.setBackground(Color.WHITE);
        settingsPanel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(0, 0, 4, 0);

        // Preset selector section
        settingsPanel.add(DesktopTheme.label("DETECTION PROFILE", 11, true, DesktopTheme.MUTED), gbc);
        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 8, 0);
        presetCombo.setFont(DesktopTheme.font(12, true));
        presetCombo.setPreferredSize(new Dimension(260, 36));
        settingsPanel.add(presetCombo, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(4, 0, 12, 0);
        settingsPanel.add(createSeparator(), gbc);

        // Parameters section
        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 4, 0);
        settingsPanel.add(DesktopTheme.label("DETECTION SENSITIVITY", 11, true, DesktopTheme.MUTED), gbc);

        addSetting(settingsPanel, gbc, "Darkness Threshold", threshold,
                "Luminance drop below local neighborhood mean (1–100).", 1, 100);
        addSetting(settingsPanel, gbc, "Neighborhood Radius", radius,
                "Local contrast window radius around each pixel (2–50 px).", 2, 50);

        gbc.gridy++;
        gbc.insets = new Insets(6, 0, 12, 0);
        settingsPanel.add(createSeparator(), gbc);

        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 4, 0);
        settingsPanel.add(DesktopTheme.label("REGION FILTERING", 11, true, DesktopTheme.MUTED), gbc);

        addSetting(settingsPanel, gbc, "Minimum Component Pixels", minPixels,
                "Rejects isolated speckles smaller than this size.", 2, 1000);
        addSetting(settingsPanel, gbc, "Minimum Region Span", minSpan,
                "Bounding-box diagonal floor rejecting compact marks (2–1000 px).", 2, 500);

        gbc.gridy++;
        gbc.insets = new Insets(6, 0, 12, 0);
        settingsPanel.add(createSeparator(), gbc);

        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 4, 0);
        settingsPanel.add(DesktopTheme.label("PERFORMANCE & SCALING", 11, true, DesktopTheme.MUTED), gbc);

        addSetting(settingsPanel, gbc, "Analysis Size Limit", maxDimension,
                "Resizes large photos down to this dimension limit (64–2000 px).", 64, 2000);

        // Vertical spacer
        gbc.gridy++;
        gbc.weighty = 1.0;
        JPanel spacer = new JPanel();
        spacer.setOpaque(false);
        settingsPanel.add(spacer, gbc);

        // Reset defaults button
        gbc.gridy++;
        gbc.weighty = 0;
        gbc.insets = new Insets(8, 0, 0, 0);
        resetDefaultsButton.setPreferredSize(new Dimension(260, 34));
        settingsPanel.add(resetDefaultsButton, gbc);

        JScrollPane scroll = new JScrollPane(settingsPanel);
        scroll.setBorder(BorderFactory.createLineBorder(DesktopTheme.LINE));
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        sidebar.add(scroll, BorderLayout.CENTER);

        // Legend / interpretation guide card
        JPanel guideCard = DesktopTheme.cardPanel();
        guideCard.setLayout(new GridLayout(0, 1, 0, 4));
        guideCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(DesktopTheme.LINE),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)));
        guideCard.setBackground(new Color(248, 250, 252));

        guideCard.add(DesktopTheme.label("VISUAL INSPECTION KEY", 11, true, DesktopTheme.INK));
        guideCard.add(DesktopTheme.label("●  Magenta: Candidate crack pixels", 11, false, DesktopTheme.OVERLAY_MAGENTA));
        guideCard.add(DesktopTheme.label("□  Red: 8-connected candidate boundary", 11, false, new Color(220, 38, 38)));
        guideCard.add(DesktopTheme.label("□  Amber: Focused / Selected crack region", 11, false, DesktopTheme.WARNING));
        guideCard.add(DesktopTheme.label("Double-click table row to zoom & center crack.", 10, false, DesktopTheme.MUTED));

        sidebar.add(guideCard, BorderLayout.SOUTH);
        return sidebar;
    }

    private JPanel createInspectionArea() {
        JPanel area = new JPanel(new BorderLayout(0, 12));
        area.setOpaque(false);

        // Prepare results cards
        resultsCardPanel.add(overlay, "Highlighted Overlay");
        resultsCardPanel.add(mask, "Binary Mask");
        viewMode.setFont(DesktopTheme.font(11, true));
        viewMode.addActionListener(e -> viewCards.show(resultsCardPanel, (String) viewMode.getSelectedItem()));

        // Preview panels
        JPanel originalCard = createPreviewCard("Original Photograph", fileInfo, original,
                createZoomToolbar(original, originalZoomBadge));
        JPanel resultCard = createPreviewCard("Detection Inspection",
                DesktopTheme.label("Cracks highlighted in magenta", 11, false, DesktopTheme.MUTED),
                resultsCardPanel, createResultToolbar());

        JSplitPane previewSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, originalCard, resultCard);
        previewSplit.setResizeWeight(0.5);
        previewSplit.setDividerLocation(0.5);
        previewSplit.setDividerSize(10);
        previewSplit.setContinuousLayout(true);
        previewSplit.setBorder(BorderFactory.createEmptyBorder());
        previewSplit.setOpaque(false);

        // Region measurements table panel
        JPanel tablePanel = createTablePanel();

        JSplitPane verticalSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, previewSplit, tablePanel);
        verticalSplit.setResizeWeight(0.72);
        verticalSplit.setDividerSize(10);
        verticalSplit.setContinuousLayout(true);
        verticalSplit.setBorder(BorderFactory.createEmptyBorder());
        verticalSplit.setOpaque(false);

        area.add(verticalSplit, BorderLayout.CENTER);
        return area;
    }

    private JPanel createPreviewCard(String title, JLabel subtitle, JComponent content, JComponent headerActions) {
        JPanel card = DesktopTheme.cardPanel();
        card.setLayout(new BorderLayout());
        card.setMinimumSize(new Dimension(240, 180));

        JPanel header = new JPanel(new BorderLayout(8, 2));
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));

        JPanel titleBox = new JPanel(new GridLayout(0, 1, 0, 2));
        titleBox.setOpaque(false);
        titleBox.add(DesktopTheme.label(title, 13, true, DesktopTheme.INK));
        subtitle.setFont(DesktopTheme.font(11, false));
        subtitle.setForeground(DesktopTheme.MUTED);
        titleBox.add(subtitle);
        header.add(titleBox, BorderLayout.WEST);

        if (headerActions != null) {
            header.add(headerActions, BorderLayout.EAST);
        }

        card.add(header, BorderLayout.NORTH);
        card.add(content, BorderLayout.CENTER);
        return card;
    }

    private JPanel createZoomToolbar(ImagePanel panel, JLabel zoomBadge) {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        toolbar.setOpaque(false);

        JButton zoomOut = DesktopTheme.iconButton("−", "Zoom Out");
        JButton zoomIn = DesktopTheme.iconButton("+", "Zoom In");
        JButton actualSize = DesktopTheme.iconButton("1:1", "Actual Size 100%");
        JButton fit = DesktopTheme.iconButton("Fit", "Reset to Fit Window");

        zoomOut.addActionListener(e -> panel.zoomOut());
        zoomIn.addActionListener(e -> panel.zoomIn());
        actualSize.addActionListener(e -> panel.actualSize());
        fit.addActionListener(e -> panel.resetZoom());

        panel.setZoomChangeListener(percentage -> zoomBadge.setText(percentage + "%"));

        toolbar.add(zoomOut);
        toolbar.add(zoomBadge);
        toolbar.add(zoomIn);
        toolbar.add(actualSize);
        toolbar.add(fit);
        return toolbar;
    }

    private JPanel createResultToolbar() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        toolbar.setOpaque(false);

        toolbar.add(viewMode);
        toolbar.add(createZoomToolbar(overlay, resultZoomBadge));
        return toolbar;
    }

    private JPanel createTablePanel() {
        JPanel panel = DesktopTheme.cardPanel();
        panel.setLayout(new BorderLayout());
        panel.setMinimumSize(new Dimension(300, 140));
        panel.setPreferredSize(new Dimension(600, 180));

        // Table header bar
        JPanel heading = new JPanel(new BorderLayout(12, 0));
        heading.setOpaque(false);
        heading.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        heading.add(DesktopTheme.label("Detected Crack Measurements", 13, true, DesktopTheme.INK), BorderLayout.WEST);

        summary.setFont(DesktopTheme.font(11, false));
        summary.setForeground(DesktopTheme.MUTED);
        heading.add(summary, BorderLayout.CENTER);

        JButton copyCsvButton = DesktopTheme.iconButton("Copy CSV", "Copy table contents to clipboard as CSV");
        copyCsvButton.setPreferredSize(new Dimension(80, 28));
        copyCsvButton.addActionListener(e -> copyTableCsv());
        heading.add(copyCsvButton, BorderLayout.EAST);
        panel.add(heading, BorderLayout.NORTH);

        // Configure table
        regionTable.setAutoCreateRowSorter(true);
        regionTable.setFillsViewportHeight(true);
        regionTable.setRowHeight(32);
        regionTable.setShowVerticalLines(false);
        regionTable.setGridColor(DesktopTheme.LINE);
        regionTable.setIntercellSpacing(new Dimension(0, 1));
        regionTable.setSelectionBackground(new Color(238, 242, 255));
        regionTable.setSelectionForeground(DesktopTheme.INK);

        regionTable.getTableHeader().setFont(DesktopTheme.font(11, true));
        regionTable.getTableHeader().setBackground(new Color(248, 250, 252));
        regionTable.getTableHeader().setForeground(DesktopTheme.MUTED);
        regionTable.getTableHeader().setPreferredSize(new Dimension(100, 32));
        regionTable.getTableHeader().setReorderingAllowed(false);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        regionTable.setDefaultRenderer(Integer.class, centerRenderer);
        regionTable.setDefaultRenderer(Double.class, centerRenderer);

        // Custom severity badge renderer
        regionTable.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                String severity = String.valueOf(value);
                JLabel badge;
                if ("Major".equals(severity)) {
                    badge = DesktopTheme.badge("MAJOR", new Color(254, 242, 242), DesktopTheme.DANGER);
                } else if ("Moderate".equals(severity)) {
                    badge = DesktopTheme.badge("MODERATE", new Color(254, 243, 199), DesktopTheme.WARNING);
                } else {
                    badge = DesktopTheme.badge("MINOR", new Color(236, 253, 245), DesktopTheme.SUCCESS);
                }
                badge.setHorizontalAlignment(JLabel.CENTER);
                JPanel wrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 4));
                wrap.setBackground(isSelected ? table.getSelectionBackground() : Color.WHITE);
                wrap.add(badge);
                return wrap;
            }
        });

        // Table interactions: Row selection highlights crack in overlay; double click centers viewport on crack
        regionTable.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                int selectedRow = regionTable.getSelectedRow();
                if (selectedRow >= 0 && result != null) {
                    int modelRow = regionTable.convertRowIndexToModel(selectedRow);
                    int regionId = (int) table.getValueAt(modelRow, 0);
                    overlay.setFocusedRegionId(regionId);
                } else {
                    overlay.setFocusedRegionId(-1);
                }
            }
        });

        regionTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2 && regionTable.getSelectedRow() >= 0 && result != null) {
                    int modelRow = regionTable.convertRowIndexToModel(regionTable.getSelectedRow());
                    int regionId = (int) table.getValueAt(modelRow, 0);
                    for (CrackRegion r : result.regions()) {
                        if (r.id() == regionId) {
                            overlay.centerOnRegion(r);
                            break;
                        }
                    }
                }
            }
        });

        // Context popup menu
        JPopupMenu popup = new JPopupMenu();
        JMenuItem centerItem = new JMenuItem("Center View on this Crack");
        centerItem.addActionListener(e -> {
            int selectedRow = regionTable.getSelectedRow();
            if (selectedRow >= 0 && result != null) {
                int modelRow = regionTable.convertRowIndexToModel(selectedRow);
                int regionId = (int) table.getValueAt(modelRow, 0);
                for (CrackRegion r : result.regions()) {
                    if (r.id() == regionId) {
                        overlay.centerOnRegion(r);
                        break;
                    }
                }
            }
        });
        JMenuItem copyRowItem = new JMenuItem("Copy Region Details");
        copyRowItem.addActionListener(e -> {
            int selectedRow = regionTable.getSelectedRow();
            if (selectedRow >= 0) {
                int modelRow = regionTable.convertRowIndexToModel(selectedRow);
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < table.getColumnCount(); i++) {
                    sb.append(table.getColumnName(i)).append(": ").append(table.getValueAt(modelRow, i)).append(" | ");
                }
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(sb.toString()), null);
            }
        });
        JMenuItem copyAllItem = new JMenuItem("Copy All Regions as CSV");
        copyAllItem.addActionListener(e -> copyTableCsv());

        popup.add(centerItem);
        popup.add(copyRowItem);
        popup.addSeparator();
        popup.add(copyAllItem);
        regionTable.setComponentPopupMenu(popup);

        JScrollPane tableScroll = new JScrollPane(regionTable);
        tableScroll.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, DesktopTheme.LINE));
        tableScroll.getViewport().setBackground(Color.WHITE);
        panel.add(tableScroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createStatusBar() {
        JPanel bar = new JPanel(new BorderLayout(12, 0));
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createEmptyBorder(6, 4, 2, 4));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        left.setOpaque(false);
        statusDot.setForeground(DesktopTheme.SUCCESS);
        status.setFont(DesktopTheme.font(12, false));
        status.setForeground(DesktopTheme.MUTED);
        left.add(statusDot);
        left.add(status);
        bar.add(left, BorderLayout.WEST);

        JPanel center = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        center.setOpaque(false);
        pixelInspector.setFont(DesktopTheme.font(11, false));
        pixelInspector.setForeground(DesktopTheme.MUTED);
        center.add(pixelInspector);
        bar.add(center, BorderLayout.CENTER);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);
        progress.setPreferredSize(new Dimension(120, 6));
        progress.setVisible(false);
        right.add(progress);
        bar.add(right, BorderLayout.EAST);

        // Pixel inspector tracking from both panels
        Consumer<ImagePanel.PixelInfo> hoverAction = pixel -> {
            if (pixel != null) {
                pixelInspector.setText(String.format(Locale.ROOT,
                        "X: %d, Y: %d  |  RGB: (%d, %d, %d)  |  Luminance: %d",
                        pixel.x(), pixel.y(), pixel.red(), pixel.green(), pixel.blue(), pixel.luminance()));
            } else {
                pixelInspector.setText("Cursor: —");
            }
        };
        original.setPixelHoverListener(hoverAction);
        overlay.setPixelHoverListener(hoverAction);
        mask.setPixelHoverListener(hoverAction);

        // Clicking on an outlined crack region in overlay automatically selects it in table
        overlay.setRegionClickListener(region -> {
            for (int r = 0; r < table.getRowCount(); r++) {
                if ((int) table.getValueAt(r, 0) == region.id()) {
                    int viewRow = regionTable.convertRowIndexToView(r);
                    regionTable.setRowSelectionInterval(viewRow, viewRow);
                    regionTable.scrollRectToVisible(regionTable.getCellRect(viewRow, 0, true));
                    break;
                }
            }
        });

        return bar;
    }

    private void configureInteractions() {
        openButton.setMnemonic('O');
        analyzeButton.setMnemonic('A');
        exportButton.setMnemonic('E');

        openButton.addActionListener(e -> openImage());
        analyzeButton.addActionListener(e -> analyze());
        exportButton.addActionListener(e -> export());
        resetDefaultsButton.addActionListener(e -> resetDefaults());

        sampleCrackedButton.addActionListener(e -> loadSampleFile("wall-cracked.png"));
        sampleCleanButton.addActionListener(e -> loadSampleFile("wall-clean.png"));

        for (JSpinner setting : settings) {
            setting.addChangeListener(e -> {
                if (!adjustingPresets) {
                    presetCombo.setSelectedItem("Custom");
                    invalidateAnalysis();
                }
            });
        }

        presetCombo.addActionListener(e -> {
            if (adjustingPresets) return;
            String selected = (String) presetCombo.getSelectedItem();
            if (selected == null || "Custom".equals(selected)) return;

            DetectionPreset matched = null;
            for (DetectionPreset p : DetectionPreset.builtIns()) {
                if (p.name().equals(selected)) {
                    matched = p;
                    break;
                }
            }
            if (matched != null) {
                applyPreset(matched.config());
            }
        });
    }

    private void configureDragAndDrop() {
        DropTargetAdapter dropAdapter = new DropTargetAdapter() {
            @Override
            public void drop(DropTargetDropEvent event) {
                try {
                    event.acceptDrop(DnDConstants.ACTION_COPY);
                    @SuppressWarnings("unchecked")
                    List<File> droppedFiles = (List<File>) event.getTransferable()
                            .getTransferData(java.awt.datatransfer.DataFlavor.javaFileListFlavor);
                    if (!droppedFiles.isEmpty()) {
                        File file = droppedFiles.get(0);
                        loadFile(file.toPath());
                    }
                    event.dropComplete(true);
                } catch (Exception ex) {
                    event.dropComplete(false);
                }
            }
        };

        new DropTarget(this, dropAdapter);
        new DropTarget(original, dropAdapter);
        new DropTarget(overlay, dropAdapter);
    }

    /**
     * Prompts the user to select an image file via desktop file chooser.
     */
    public void openImage() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Choose a wall photograph for crack inspection");
        chooser.setFileFilter(new FileNameExtensionFilter("Photographs (*.png, *.jpg, *.jpeg, *.bmp)", "png", "jpg", "jpeg", "bmp"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        loadFile(chooser.getSelectedFile().toPath());
    }

    /**
     * Loads a file path and initializes the image previews.
     *
     * @param file path to image file
     */
    public void loadFile(Path file) {
        currentFilePath = file;
        runTask("Loading photograph...", () -> ImageFiles.read(file), image -> {
            source = image;
            original.setImage(image);
            fileInfo.setText(file.getFileName() + "  ·  " + image.getWidth() + " × " + image.getHeight() + " px");
            fileInfo.setToolTipText(file.toAbsolutePath().toString());
            invalidateAnalysis();
            statusDot.setForeground(DesktopTheme.SUCCESS);
            status.setText("Loaded " + file.getFileName() + " (" + image.getWidth() + " × " + image.getHeight() + " px). Ready for analysis.");
        });
    }

    /**
     * Loads one of the sample wall photographs bundled in the repository.
     *
     * @param sampleName sample file name (e.g. "wall-cracked.png" or "wall-clean.png")
     */
    public void loadSampleFile(String sampleName) {
        Path direct = Path.of("samples").resolve(sampleName);
        if (Files.exists(direct)) {
            loadFile(direct);
            return;
        }
        Path alt = Path.of(sampleName);
        if (Files.exists(alt)) {
            loadFile(alt);
            return;
        }
        showError("Could not locate sample file: " + sampleName + ". Ensure you are running from the project root.");
    }

    /**
     * Executes the wall crack detection algorithm in a background SwingWorker thread.
     */
    public void analyze() {
        try {
            for (JSpinner setting : settings) setting.commitEdit();
        } catch (java.text.ParseException exception) {
            showError("Enter integer numbers within the allowed ranges for all settings.");
            return;
        }

        DetectionConfig config = new DetectionConfig(value(threshold), value(radius), value(minPixels),
                value(minSpan), value(maxDimension));
        BufferedImage input = source;

        runTask("Analyzing wall surface local contrast and connected regions...", () -> {
            DetectionResult detection = new CrackDetector().detect(input, config);
            return new AnalysisViews(detection, ResultImages.overlay(detection), ResultImages.mask(detection));
        }, views -> {
            result = views.result();
            overlay.setImage(views.overlay());
            overlay.setRegions(result.regions());
            mask.setImage(views.mask());

            table.setRowCount(0);
            double maxSpan = 0.0;
            for (CrackRegion r : result.regions()) {
                String severity = r.span() >= 150 ? "Major" : (r.span() >= 50 ? "Moderate" : "Minor");
                table.addRow(new Object[]{
                        r.id(), severity, r.x(), r.y(), r.width(), r.height(),
                        r.pixelCount(), Math.round(r.span() * 100.0) / 100.0
                });
                if (r.span() > maxSpan) maxSpan = r.span();
            }

            int count = result.regions().size();
            regionCount.setText(Integer.toString(count));
            if (count == 0) {
                regionBadge.setText("CLEAN");
                regionBadge.setForeground(DesktopTheme.SUCCESS);
                regionBadge.setBackground(new Color(236, 253, 245));
                maxSpanMetric.setText("0.0 px");
                maxSpanBadge.setText("NONE");
                maxSpanBadge.setForeground(DesktopTheme.MUTED);
                maxSpanBadge.setBackground(new Color(241, 245, 249));
            } else {
                regionBadge.setText(count + " DETECTED");
                regionBadge.setForeground(DesktopTheme.DANGER);
                regionBadge.setBackground(new Color(254, 242, 242));
                maxSpanMetric.setText(String.format(Locale.ROOT, "%.1f px", maxSpan));
                String maxSeverity = maxSpan >= 150 ? "MAJOR" : (maxSpan >= 50 ? "MODERATE" : "MINOR");
                maxSpanBadge.setText(maxSeverity);
                maxSpanBadge.setForeground(maxSpan >= 150 ? DesktopTheme.DANGER : (maxSpan >= 50 ? DesktopTheme.WARNING : DesktopTheme.SUCCESS));
                maxSpanBadge.setBackground(maxSpan >= 150 ? new Color(254, 242, 242) : (maxSpan >= 50 ? new Color(254, 243, 199) : new Color(236, 253, 245)));
            }

            coverage.setText(String.format(Locale.ROOT, "%.3f%%", result.coveragePercent()));
            coverageDetail.setText(String.format(Locale.ROOT, "%d candidate px", result.candidatePixels()));

            analysisSize.setText(result.width() + " × " + result.height());
            double scale = (double) result.width() / result.originalWidth();
            analysisScaleDetail.setText(String.format(Locale.ROOT, "Scale: %.2f × Orig (%d × %d)",
                    scale, result.originalWidth(), result.originalHeight()));

            summary.setText(count == 0 ? "No cracks detected with current sensitivity settings"
                    : "Found " + count + " candidate regions. Double-click row to center viewport.");
            statusDot.setForeground(DesktopTheme.SUCCESS);
            status.setText("Analysis complete: " + count + " candidate regions found. Ready to review and export.");
        });
    }

    /**
     * Exports the completed detection overlay, mask, CSV, and JSON report bundle.
     */
    public void export() {
        if (result == null) return;
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select export directory for report bundle");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        Path directory = chooser.getSelectedFile().toPath();
        DetectionResult completed = result;

        runTask("Exporting overlay, binary mask, CSV, and JSON reports...", () -> {
            ReportExporter.export(completed, directory);
            return directory;
        }, destination -> {
            statusDot.setForeground(DesktopTheme.SUCCESS);
            status.setText("Export complete. Saved reports to " + destination.toAbsolutePath());
            JOptionPane.showMessageDialog(this,
                    "Analysis bundle successfully exported:\n• overlay.png\n• mask.png\n• regions.csv\n• report.json\n\nLocation: "
                            + destination.toAbsolutePath(),
                    "Export Successful", JOptionPane.INFORMATION_MESSAGE);
        });
    }

    /**
     * Copies region measurements table to system clipboard as CSV text.
     */
    public void copyTableCsv() {
        if (table.getRowCount() == 0) {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection("No crack regions available."), null);
            status.setText("No crack regions to copy.");
            return;
        }

        StringBuilder sb = new StringBuilder("ID,Severity,X,Y,Width,Height,Pixels,Span_px\n");
        for (int r = 0; r < table.getRowCount(); r++) {
            for (int c = 0; c < table.getColumnCount(); c++) {
                sb.append(table.getValueAt(r, c));
                if (c < table.getColumnCount() - 1) sb.append(",");
            }
            sb.append("\n");
        }
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(sb.toString()), null);
        status.setText("Copied " + table.getRowCount() + " region rows to clipboard as CSV.");
    }

    /**
     * Copies summary metrics to clipboard.
     */
    public void copySummary() {
        if (result == null) {
            status.setText("Run analysis before copying summary.");
            return;
        }
        String text = String.format(Locale.ROOT,
                "Wall Crack Detection Summary\n"
                        + "Candidate Regions: %d\n"
                        + "Coverage: %.4f%%\n"
                        + "Candidate Pixels: %d\n"
                        + "Analysis Dimensions: %d × %d px\n"
                        + "Original Dimensions: %d × %d px\n"
                        + "Source: %s\n",
                result.regions().size(), result.coveragePercent(), result.candidatePixels(),
                result.width(), result.height(), result.originalWidth(), result.originalHeight(),
                currentFilePath != null ? currentFilePath.getFileName().toString() : "Unknown");
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
        status.setText("Analysis summary copied to clipboard.");
    }

    /**
     * Resets parameters back to balanced defaults.
     */
    public void resetDefaults() {
        applyPreset(DetectionConfig.defaults());
        presetCombo.setSelectedItem("Balanced (Default)");
    }

    /**
     * Applies a specific detection configuration across all UI controls.
     *
     * @param config detection parameters
     */
    public void applyPreset(DetectionConfig config) {
        adjustingPresets = true;
        try {
            threshold.setValue(config.contrastThreshold());
            radius.setValue(config.localRadius());
            minPixels.setValue(config.minComponentPixels());
            minSpan.setValue(config.minSpan());
            maxDimension.setValue(config.maxDimension());
        } finally {
            adjustingPresets = false;
        }
        invalidateAnalysis();
    }

    /**
     * Switches the active inspection view mode.
     *
     * @param mode "Highlighted Overlay" or "Binary Mask"
     */
    public void setViewMode(String mode) {
        viewMode.setSelectedItem(mode);
        viewCards.show(resultsCardPanel, mode);
    }

    /**
     * Zooms in on the primary inspection view.
     */
    public void zoomIn() {
        overlay.zoomIn();
        original.zoomIn();
    }

    /**
     * Zooms out on the primary inspection view.
     */
    public void zoomOut() {
        overlay.zoomOut();
        original.zoomOut();
    }

    /**
     * Resets zoom to fit window on both previews.
     */
    public void resetZoom() {
        overlay.resetZoom();
        original.resetZoom();
    }

    /**
     * Sets magnification to 1:1 actual pixels on both previews.
     */
    public void actualSize() {
        overlay.actualSize();
        original.actualSize();
    }

    /**
     * Whether an image is currently loaded.
     *
     * @return true if source image exists
     */
    public boolean hasImage() {
        return source != null;
    }

    /**
     * Whether analysis has been performed.
     *
     * @return true if results are available
     */
    public boolean hasResult() {
        return result != null;
    }

    private <T> void runTask(String message, Callable<T> task, Consumer<T> success) {
        busy = true;
        statusDot.setForeground(DesktopTheme.BLUE);
        status.setText(message);
        progress.setVisible(true);
        progress.setIndeterminate(true);
        updateControls();

        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return task.call();
            }

            @Override
            protected void done() {
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
        overlay.setRegions(List.of());
        overlay.setFocusedRegionId(-1);
        mask.setImage(null);
        table.setRowCount(0);
        summary.setText("Analyze a photograph to view measurements");
        regionCount.setText("—");
        regionBadge.setText("IDLE");
        regionBadge.setForeground(DesktopTheme.MUTED);
        regionBadge.setBackground(new Color(241, 245, 249));
        coverage.setText("—");
        coverageDetail.setText("0 pixels flagged");
        maxSpanMetric.setText("—");
        maxSpanBadge.setText("NONE");
        maxSpanBadge.setForeground(DesktopTheme.MUTED);
        maxSpanBadge.setBackground(new Color(241, 245, 249));
        analysisSize.setText("—");
        analysisScaleDetail.setText(source != null ? source.getWidth() + " × " + source.getHeight() + " px loaded" : "No active image");
        if (source != null) {
            statusDot.setForeground(DesktopTheme.WARNING);
            status.setText("Parameters modified. Click 'Analyze Wall' to update results.");
        }
        updateControls();
    }

    private void updateControls() {
        openButton.setEnabled(!busy);
        sampleCrackedButton.setEnabled(!busy);
        sampleCleanButton.setEnabled(!busy);
        analyzeButton.setEnabled(!busy && source != null);
        exportButton.setEnabled(!busy && result != null);
        resetDefaultsButton.setEnabled(!busy);
        presetCombo.setEnabled(!busy);
        for (JSpinner setting : settings) setting.setEnabled(!busy);
    }

    private void showError(String message) {
        statusDot.setForeground(DesktopTheme.DANGER);
        status.setText("Operation failed: " + message);
        JOptionPane.showMessageDialog(this, message, "Wall Crack Detector", JOptionPane.ERROR_MESSAGE);
    }

    private static JSpinner spinner(int initial, int min, int max) {
        return new JSpinner(new SpinnerNumberModel(initial, min, max, 1));
    }

    private static int value(JSpinner spinner) {
        return ((Number) spinner.getValue()).intValue();
    }

    private static JPanel createSeparator() {
        JPanel sep = new JPanel();
        sep.setBackground(DesktopTheme.LINE);
        sep.setPreferredSize(new Dimension(100, 1));
        return sep;
    }

    private static void addSetting(JPanel panel, GridBagConstraints gbc,
                                   String label, JSpinner spinner, String description,
                                   int sliderMin, int sliderMax) {
        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 3, 0);
        JLabel caption = DesktopTheme.label(label, 12, true, DesktopTheme.INK);
        caption.setLabelFor(spinner);
        panel.add(caption, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 4, 0);

        JPanel inputRow = new JPanel(new BorderLayout(8, 0));
        inputRow.setOpaque(false);

        spinner.setToolTipText(description);
        spinner.setPreferredSize(new Dimension(80, 32));
        spinner.setEditor(new JSpinner.NumberEditor(spinner, "0"));
        var editor = (JSpinner.NumberEditor) spinner.getEditor();
        editor.getTextField().setFont(DesktopTheme.font(13, false));
        editor.getTextField().setHorizontalAlignment(JLabel.CENTER);

        JSlider slider = new JSlider(sliderMin, sliderMax, value(spinner));
        slider.setOpaque(false);
        slider.addChangeListener(e -> {
            if (slider.getValueIsAdjusting()) {
                spinner.setValue(slider.getValue());
            }
        });
        spinner.addChangeListener(e -> {
            int v = value(spinner);
            if (v >= sliderMin && v <= sliderMax && slider.getValue() != v) {
                slider.setValue(v);
            }
        });

        inputRow.add(slider, BorderLayout.CENTER);
        inputRow.add(spinner, BorderLayout.EAST);
        panel.add(inputRow, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 8, 0);
        JLabel desc = DesktopTheme.label(description, 10, false, DesktopTheme.MUTED);
        panel.add(desc, gbc);
    }

    private record AnalysisViews(DetectionResult result, BufferedImage overlay, BufferedImage mask) { }
}
