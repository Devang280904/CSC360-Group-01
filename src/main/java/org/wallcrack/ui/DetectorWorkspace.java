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
import javax.swing.JCheckBox;
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
import org.wallcrack.model.DetectionResult;

/**
 * Modern, uncluttered desktop workspace providing wall photograph loading,
 * ImageJ detection tuning,
 * interactive zoom/pan visual review, and multi-format report exports.
 *
 * @serial exclude
 */
public final class DetectorWorkspace extends JPanel {
    private static final long serialVersionUID = 1L;

    // Action buttons
    private final JButton openButton = DesktopTheme.coloredButton("Open Photograph", DesktopTheme.BLUE,
            DesktopTheme.BLUE_HOVER, DesktopTheme.BLUE, Color.WHITE, 8);
    private final JButton analyzeButton = DesktopTheme.coloredButton("⚡ Analyze Wall", DesktopTheme.INDIGO,
            DesktopTheme.INDIGO_HOVER, DesktopTheme.INDIGO, Color.WHITE, 8);
    private final JButton exportButton = DesktopTheme.coloredButton("💾 Export Bundle", DesktopTheme.AMBER_GOLD,
            DesktopTheme.AMBER_HOVER, DesktopTheme.AMBER_GOLD, Color.WHITE, 8);
    private final JButton resetDefaultsButton = DesktopTheme.coloredButton("↺ Reset Defaults", DesktopTheme.LINE,
            new Color(51, 65, 85), DesktopTheme.INPUT_BORDER, DesktopTheme.INK_SECONDARY, 6);
    private final JButton sampleCrackedButton = DesktopTheme.pillButton("● Cracked Wall", DesktopTheme.ROSE_DARK,
            DesktopTheme.ROSE_BORDER, DesktopTheme.ROSE);
    private final JButton sampleCleanButton = DesktopTheme.pillButton("● Clean Wall", DesktopTheme.EMERALD_DARK,
            DesktopTheme.EMERALD_BORDER, DesktopTheme.EMERALD_MINT);

    // Detection settings with radiant neon badges
    private final JComboBox<String> methodCombo = new JComboBox<>(new String[] { "Triangle", "Otsu" });
    private final JSpinner sensitivity = spinner(0, -50, 50);
    private final JLabel sensitivityBadge = DesktopTheme.badge("0", DesktopTheme.CYAN_DARK, DesktopTheme.CYAN);
    private final JSpinner minArea = spinner(30, 2, 100_000);
    private final JLabel minAreaBadge = DesktopTheme.badge("30 px", DesktopTheme.PURPLE_DARK,
            DesktopTheme.PURPLE);
    private final JSpinner maxCircularity = new JSpinner(new SpinnerNumberModel(0.50, 0.01, 1.00, 0.05));
    private final JLabel maxCircBadge = DesktopTheme.badge("0.50", DesktopTheme.AMBER_DARK, DesktopTheme.AMBER_GOLD);
    private final JCheckBox correctLighting = new JCheckBox("Flatten uneven surface lighting");
    private final JSpinner closingRadius = spinner(0, 0, 10);
    private final JLabel closingBadge = DesktopTheme.badge("0 px", DesktopTheme.ROSE_DARK, DesktopTheme.ROSE);
    private final JSpinner maxDimension = spinner(1600, 64, 2000);
    private final JLabel maxDimBadge = DesktopTheme.badge("1600 px", DesktopTheme.CYAN_DARK, DesktopTheme.CYAN);
    private final transient List<JSpinner> settings = List.of(sensitivity, minArea, maxCircularity, closingRadius,
            maxDimension);
    private boolean resettingSettings;

    // Image previews
    private final ImagePanel original = new ImagePanel("Open a wall photograph to begin inspection");
    private final ImagePanel overlay = new ImagePanel("Run analysis to see highlighted crack candidates");
    private final ImagePanel mask = new ImagePanel("Accepted candidate pixels appear in white");
    private final java.awt.CardLayout viewCards = new java.awt.CardLayout();
    private final JPanel resultsCardPanel = new JPanel(viewCards);
    private final JButton overlayTabButton = DesktopTheme.iconButton("Highlighted Overlay",
            "Show photograph with highlighted crack candidates");
    private final JButton maskTabButton = DesktopTheme.iconButton("Binary Mask", "Show clean binary edge mask");
    private String activeViewMode = "Highlighted Overlay";

    // Zoom badges
    private final JLabel originalZoomBadge = DesktopTheme.badge("Fit", DesktopTheme.CYAN_DARK, DesktopTheme.CYAN);
    private final JLabel resultZoomBadge = DesktopTheme.badge("Fit", DesktopTheme.PURPLE_DARK, DesktopTheme.PURPLE);

    // Status & KPI indicators
    private final JLabel statusDot = new JLabel("●");
    private final JLabel status = new JLabel("Ready. Open a wall photograph or sample to begin.");
    private final JLabel pixelInspector = new JLabel("Cursor: —");
    private final JLabel fileDimensionsBadge = DesktopTheme.badge("No image", DesktopTheme.CYAN_DARK,
            DesktopTheme.CYAN);
    private final JLabel regionCount = metricValue("—");
    private final JLabel regionBadge = DesktopTheme.badge("IDLE", DesktopTheme.ROSE_DARK, DesktopTheme.ROSE);
    private final JLabel regionDetail = DesktopTheme.label("Waiting for analysis", 11, false, DesktopTheme.MUTED);
    private final JLabel coverage = metricValue("—");
    private final JLabel coverageDetail = DesktopTheme.label("0 pixels flagged", 11, false, DesktopTheme.MUTED);
    private final JLabel maxSpanMetric = metricValue("—");
    private final JLabel maxSpanDetail = DesktopTheme.label("Estimated in analysis pixels", 11, false,
            DesktopTheme.MUTED);
    private final JLabel analysisSize = metricValue("—");
    private final JLabel analysisScaleDetail = DesktopTheme.label("Analysis limit: 1600 px", 11, false,
            DesktopTheme.MUTED);
    private final JProgressBar progress = new JProgressBar();
    private final JLabel summary = new JLabel("Double-click row to center viewport on crack");

    // Region measurements table
    private final DefaultTableModel table = new DefaultTableModel(
            new Object[] { "ID", "X (px)", "Y (px)", "Width (px)", "Height (px)", "Pixels", "Box diagonal (px)",
                    "Circularity", "Est. thickness (px)", "Orientation" },
            0) {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }

        @Override
        public Class<?> getColumnClass(int column) {
            return switch (column) {
                case 0, 1, 2, 3, 4, 5 -> Integer.class;
                case 6, 7, 8 -> Double.class;
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
     * Creates the desktop workspace layout; instantiate on the Swing event dispatch
     * thread.
     */
    public DetectorWorkspace() {
        super(new BorderLayout(0, 12));
        setBackground(DesktopTheme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(14, 16, 12, 16));

        add(createHeaderPanel(), BorderLayout.NORTH);
        add(createMainSplit(), BorderLayout.CENTER);
        add(createStatusBar(), BorderLayout.SOUTH);

        configureInteractions();
        configureDragAndDrop();
        updateControls();
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);

        // Row 1: Title & Badges (West) + Primary Action Buttons (East)
        JPanel topRow = new JPanel(new BorderLayout(16, 0));
        topRow.setOpaque(false);
        topRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel titleArea = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        titleArea.setOpaque(false);
        titleArea.add(DesktopTheme.label("Wall Crack Detector", 22, true, Color.WHITE));
        titleArea.add(DesktopTheme.badge("ImageJ Core 1.54p", DesktopTheme.PURPLE_DARK, DesktopTheme.PURPLE));
        titleArea.add(DesktopTheme.badge("● Real-Time Vision", DesktopTheme.EMERALD_DARK, DesktopTheme.EMERALD_MINT));
        topRow.add(titleArea, BorderLayout.WEST);

        JPanel mainActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        mainActions.setOpaque(false);
        mainActions.add(openButton);
        mainActions.add(analyzeButton);
        mainActions.add(exportButton);
        topRow.add(mainActions, BorderLayout.EAST);

        // Row 2: Subtitle (West) + Quick Samples (East)
        JPanel bottomRow = new JPanel(new BorderLayout(16, 0));
        bottomRow.setOpaque(false);
        bottomRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = DesktopTheme.label(
                "Automated sub-pixel structural defect inspection. Measurements in analysis image pixels.",
                12, false, DesktopTheme.CYAN);
        JPanel subtitleArea = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        subtitleArea.setOpaque(false);
        subtitleArea.add(subtitle);
        bottomRow.add(subtitleArea, BorderLayout.WEST);

        JPanel quickSamplesArea = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        quickSamplesArea.setOpaque(false);
        quickSamplesArea.add(DesktopTheme.label("Quick Samples:", 11, true, DesktopTheme.INK_SECONDARY));
        sampleCrackedButton.setPreferredSize(new Dimension(116, 30));
        sampleCleanButton.setPreferredSize(new Dimension(106, 30));
        quickSamplesArea.add(sampleCrackedButton);
        quickSamplesArea.add(sampleCleanButton);
        bottomRow.add(quickSamplesArea, BorderLayout.EAST);

        header.add(topRow);
        header.add(Box.createVerticalStrut(6));
        header.add(bottomRow);
        return header;
    }

    private JPanel createMainSplit() {
        JPanel container = new JPanel(new BorderLayout(0, 10));
        container.setOpaque(false);
        container.add(createMetricsRow(), BorderLayout.NORTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, createSidebar(), createInspectionArea());
        splitPane.setResizeWeight(0.0);
        splitPane.setDividerLocation(310);
        splitPane.setDividerSize(8);
        splitPane.setContinuousLayout(true);
        splitPane.setBorder(BorderFactory.createEmptyBorder());
        splitPane.setOpaque(false);

        container.add(splitPane, BorderLayout.CENTER);
        return container;
    }

    private JPanel createMetricsRow() {
        JPanel metrics = new JPanel(new GridLayout(1, 4, 10, 0));
        metrics.setOpaque(false);
        metrics.setPreferredSize(new Dimension(100, 82));

        metrics.add(metricCard("CANDIDATE REGIONS", regionCount, regionBadge, regionDetail,
                DesktopTheme.ROSE, DesktopTheme.CARD_BACKGROUND, DesktopTheme.LINE));
        metrics.add(metricCard("SURFACE COVERAGE", coverage, null, coverageDetail,
                DesktopTheme.CYAN, DesktopTheme.CARD_BACKGROUND, DesktopTheme.LINE));
        metrics.add(metricCard("WIDEST CANDIDATE (EST.)", maxSpanMetric, null, maxSpanDetail,
                DesktopTheme.AMBER_GOLD, DesktopTheme.CARD_BACKGROUND, DesktopTheme.LINE));
        metrics.add(metricCard("ANALYSIS SIZE & LIMIT", analysisSize, null, analysisScaleDetail,
                DesktopTheme.EMERALD_MINT, DesktopTheme.CARD_BACKGROUND, DesktopTheme.LINE));
        return metrics;
    }

    private JPanel metricCard(String title, JLabel mainValue, JLabel rightBadge, JLabel bottomDetail,
            Color topAccent, Color bgTint, Color borderColor) {
        JPanel card = DesktopTheme.accentCardPanel(topAccent, bgTint, borderColor);
        card.setLayout(new BorderLayout(0, 2));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(1, 1, 1, 1),
                BorderFactory.createEmptyBorder(8, 14, 8, 14)));

        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);
        JLabel titleLabel = DesktopTheme.label(title, 10, true, topAccent);
        topRow.add(titleLabel, BorderLayout.WEST);
        if (rightBadge != null) {
            topRow.add(rightBadge, BorderLayout.EAST);
        }
        card.add(topRow, BorderLayout.NORTH);

        mainValue.setFont(DesktopTheme.font(18, true));
        mainValue.setForeground(topAccent);
        card.add(mainValue, BorderLayout.CENTER);

        if (bottomDetail != null) {
            bottomDetail.setFont(DesktopTheme.font(11, false));
            bottomDetail.setForeground(DesktopTheme.MUTED);
            card.add(bottomDetail, BorderLayout.SOUTH);
        }
        return card;
    }

    private static JLabel metricValue(String text) {
        return DesktopTheme.label(text, 18, true, Color.WHITE);
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout(0, 8));
        sidebar.setOpaque(false);
        sidebar.setPreferredSize(new Dimension(310, 500));
        sidebar.setMinimumSize(new Dimension(270, 400));

        JPanel settingsPanel = new JPanel(new GridBagLayout());
        settingsPanel.setBackground(DesktopTheme.CARD_BACKGROUND);
        settingsPanel.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(0, 0, 3, 0);

        // Section 1: Thresholding
        settingsPanel.add(DesktopTheme.label("✦ DETECTION SENSITIVITY", 10, true, DesktopTheme.CYAN), gbc);

        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 2, 0);
        settingsPanel.add(DesktopTheme.label("Threshold Method", 11, true, DesktopTheme.INK_SECONDARY), gbc);

        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 8, 0);
        DesktopTheme.styleComboBox(methodCombo);
        methodCombo.setPreferredSize(new Dimension(240, 30));
        settingsPanel.add(methodCombo, gbc);

        DesktopTheme.styleSpinner(sensitivity);
        addSliderControl(settingsPanel, gbc, "Sensitivity Offset", sensitivity, sensitivityBadge,
                "Manual offset added to auto-threshold (-50 to +50; higher = more pixels)", -50, 50,
                val -> String.format("%+d", val), DesktopTheme.CYAN);

        gbc.gridy++;
        gbc.insets = new Insets(4, 0, 10, 0);
        settingsPanel.add(createSeparator(), gbc);

        // Section 2: Filtering
        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 3, 0);
        settingsPanel.add(DesktopTheme.label("✦ REGION FILTERING", 10, true, DesktopTheme.PURPLE), gbc);

        DesktopTheme.styleSpinner(minArea);
        addSliderControl(settingsPanel, gbc, "Minimum Particle Area", minArea, minAreaBadge,
                "Rejects isolated speckles smaller than this size (pixels)", 2, 500, val -> val + " px",
                DesktopTheme.PURPLE);

        DesktopTheme.styleSpinner(maxCircularity);
        addDoubleSliderControl(settingsPanel, gbc, "Maximum Circularity", maxCircularity, maxCircBadge,
                "Filters out compact round spots (0.01 = thin line, 1.00 = circle)", 0.01, 1.00,
                DesktopTheme.AMBER_GOLD);

        gbc.gridy++;
        gbc.insets = new Insets(4, 0, 10, 0);
        settingsPanel.add(createSeparator(), gbc);

        // Section 3: Processing
        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 3, 0);
        settingsPanel.add(DesktopTheme.label("✦ IMAGE ENHANCEMENT", 10, true, DesktopTheme.AMBER_GOLD), gbc);

        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 4, 0);
        DesktopTheme.styleCheckBox(correctLighting);
        correctLighting.setToolTipText("ImageJ rolling-ball background subtraction for uneven wall lighting");
        settingsPanel.add(correctLighting, gbc);

        DesktopTheme.styleSpinner(closingRadius);
        addSliderControl(settingsPanel, gbc, "Connect Small Gaps (Closing)", closingRadius, closingBadge,
                "Morphological closing radius to bridge small hairline gaps (0 = off)", 0, 10,
                val -> val == 0 ? "Off" : val + " px", DesktopTheme.ROSE);

        DesktopTheme.styleSpinner(maxDimension);
        addSliderControl(settingsPanel, gbc, "Analysis Size Limit", maxDimension, maxDimBadge,
                "Resizes large photos down to this dimension limit (64–2000 px)", 64, 2000, val -> val + " px",
                DesktopTheme.CYAN);

        // Spacer
        gbc.gridy++;
        gbc.weighty = 1.0;
        JPanel spacer = new JPanel();
        spacer.setOpaque(false);
        settingsPanel.add(spacer, gbc);

        // Reset button
        gbc.gridy++;
        gbc.weighty = 0;
        gbc.insets = new Insets(6, 0, 0, 0);
        resetDefaultsButton.setPreferredSize(new Dimension(240, 32));
        settingsPanel.add(resetDefaultsButton, gbc);

        JScrollPane scroll = new JScrollPane(settingsPanel);
        scroll.setBorder(BorderFactory.createLineBorder(DesktopTheme.LINE));
        scroll.getViewport().setBackground(DesktopTheme.CARD_BACKGROUND);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        sidebar.add(scroll, BorderLayout.CENTER);

        // Legend card
        JPanel guideCard = DesktopTheme.cardPanel();
        guideCard.setLayout(new GridLayout(0, 1, 0, 3));
        guideCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(1, 1, 1, 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));

        guideCard.add(DesktopTheme.label("INSPECTION KEY", 10, true, DesktopTheme.MUTED));
        guideCard.add(DesktopTheme.label("●  Magenta: Candidate crack pixels", 11, false, DesktopTheme.OVERLAY_MAGENTA));
        guideCard.add(DesktopTheme.label("□  Red: Candidate bounding box", 11, false, DesktopTheme.ROSE));
        guideCard.add(DesktopTheme.label("□  Amber: Focused / Selected crack region", 11, false, DesktopTheme.AMBER_GOLD));

        sidebar.add(guideCard, BorderLayout.SOUTH);
        return sidebar;
    }

    private JPanel createInspectionArea() {
        JPanel area = new JPanel(new BorderLayout(0, 10));
        area.setOpaque(false);

        resultsCardPanel.add(overlay, "Highlighted Overlay");
        resultsCardPanel.add(mask, "Binary Mask");

        // Original image card
        JPanel originalCard = createPreviewCard("Original Photograph", fileDimensionsBadge, original,
                createZoomToolbar(original, originalZoomBadge));

        // Result inspection card with segmented view switcher
        JPanel resultHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        resultHeader.setOpaque(false);
        overlayTabButton.setPreferredSize(new Dimension(136, 26));
        maskTabButton.setPreferredSize(new Dimension(96, 26));
        updateViewTabs();

        overlayTabButton.addActionListener(e -> setViewMode("Highlighted Overlay"));
        maskTabButton.addActionListener(e -> setViewMode("Binary Mask"));
        resultHeader.add(overlayTabButton);
        resultHeader.add(maskTabButton);

        JPanel resultCard = createPreviewCardComponent(resultHeader, resultsCardPanel,
                createResultZoomToolbar());

        JSplitPane previewSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, originalCard, resultCard);
        previewSplit.setResizeWeight(0.5);
        previewSplit.setDividerLocation(0.5);
        previewSplit.setDividerSize(8);
        previewSplit.setContinuousLayout(true);
        previewSplit.setBorder(BorderFactory.createEmptyBorder());
        previewSplit.setOpaque(false);

        JSplitPane verticalSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, previewSplit, createTablePanel());
        verticalSplit.setResizeWeight(0.72);
        verticalSplit.setDividerSize(8);
        verticalSplit.setContinuousLayout(true);
        verticalSplit.setBorder(BorderFactory.createEmptyBorder());
        verticalSplit.setOpaque(false);

        area.add(verticalSplit, BorderLayout.CENTER);
        return area;
    }

    private void updateViewTabs() {
        boolean isOverlay = "Highlighted Overlay".equals(activeViewMode);
        overlayTabButton.setBackground(isOverlay ? DesktopTheme.PURPLE : DesktopTheme.LINE);
        overlayTabButton.setForeground(isOverlay ? Color.WHITE : DesktopTheme.MUTED);
        maskTabButton.setBackground(!isOverlay ? DesktopTheme.INDIGO : DesktopTheme.LINE);
        maskTabButton.setForeground(!isOverlay ? Color.WHITE : DesktopTheme.MUTED);
    }

    private JPanel createPreviewCard(String title, JLabel badge, JComponent content, JComponent headerActions) {
        JPanel headerLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        headerLeft.setOpaque(false);
        headerLeft.add(DesktopTheme.label(title, 12, true, Color.WHITE));
        if (badge != null)
            headerLeft.add(badge);
        return createPreviewCardComponent(headerLeft, content, headerActions);
    }

    private JPanel createPreviewCardComponent(JComponent headerLeft, JComponent content, JComponent headerActions) {
        JPanel card = DesktopTheme.cardPanel();
        card.setLayout(new BorderLayout());
        card.setMinimumSize(new Dimension(240, 180));

        JPanel header = new JPanel(new BorderLayout(8, 0));
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

        if (headerLeft != null)
            header.add(headerLeft, BorderLayout.WEST);
        if (headerActions != null)
            header.add(headerActions, BorderLayout.EAST);

        card.add(header, BorderLayout.NORTH);
        card.add(content, BorderLayout.CENTER);
        return card;
    }

    private JPanel createZoomToolbar(ImagePanel panel, JLabel zoomBadge) {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 3, 0));
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

    private JPanel createResultZoomToolbar() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 3, 0));
        toolbar.setOpaque(false);
        JButton zoomOut = DesktopTheme.iconButton("−", "Zoom Out");
        JButton zoomIn = DesktopTheme.iconButton("+", "Zoom In");
        JButton actualSize = DesktopTheme.iconButton("1:1", "Actual Size 100%");
        JButton fit = DesktopTheme.iconButton("Fit", "Reset to Fit Window");
        zoomOut.addActionListener(e -> activeResultPanel().zoomOut());
        zoomIn.addActionListener(e -> activeResultPanel().zoomIn());
        actualSize.addActionListener(e -> activeResultPanel().actualSize());
        fit.addActionListener(e -> activeResultPanel().resetZoom());
        overlay.setZoomChangeListener(percentage -> updateResultZoomBadge());
        mask.setZoomChangeListener(percentage -> updateResultZoomBadge());
        toolbar.add(zoomOut);
        toolbar.add(resultZoomBadge);
        toolbar.add(zoomIn);
        toolbar.add(actualSize);
        toolbar.add(fit);
        return toolbar;
    }

    private ImagePanel activeResultPanel() {
        return "Binary Mask".equals(activeViewMode) ? mask : overlay;
    }

    private void updateResultZoomBadge() {
        resultZoomBadge.setText(activeResultPanel().getZoomPercentage() + "%");
    }

    private JPanel createTablePanel() {
        JPanel panel = DesktopTheme.cardPanel();
        panel.setLayout(new BorderLayout());
        panel.setMinimumSize(new Dimension(300, 140));
        panel.setPreferredSize(new Dimension(600, 170));

        JPanel heading = new JPanel(new BorderLayout(12, 0));
        heading.setOpaque(false);
        heading.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        heading.add(DesktopTheme.label("Candidate Region Measurements", 12, true, Color.WHITE), BorderLayout.WEST);

        summary.setFont(DesktopTheme.font(11, false));
        summary.setForeground(DesktopTheme.CYAN);
        heading.add(summary, BorderLayout.CENTER);

        JButton copyCsvButton = DesktopTheme.iconButton("Copy CSV", "Copy table contents to clipboard as CSV");
        copyCsvButton.setPreferredSize(new Dimension(84, 26));
        copyCsvButton.addActionListener(e -> copyTableCsv());
        heading.add(copyCsvButton, BorderLayout.EAST);
        panel.add(heading, BorderLayout.NORTH);

        regionTable.setAutoCreateRowSorter(true);
        regionTable.setFillsViewportHeight(true);
        regionTable.setRowHeight(28);
        regionTable.setShowVerticalLines(false);
        regionTable.setBackground(DesktopTheme.CARD_BACKGROUND);
        regionTable.setForeground(DesktopTheme.INK);
        regionTable.setGridColor(new Color(30, 41, 59));
        regionTable.setIntercellSpacing(new Dimension(0, 1));
        regionTable.setSelectionBackground(new Color(30, 58, 138));
        regionTable.setSelectionForeground(Color.WHITE);

        regionTable.getTableHeader().setFont(DesktopTheme.font(11, true));
        regionTable.getTableHeader().setBackground(new Color(15, 23, 42));
        regionTable.getTableHeader().setForeground(DesktopTheme.CYAN);
        regionTable.getTableHeader().setPreferredSize(new Dimension(100, 32));
        regionTable.getTableHeader().setReorderingAllowed(false);

        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public java.awt.Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setBackground(new Color(15, 23, 42));
                setForeground(DesktopTheme.CYAN);
                setFont(DesktopTheme.font(11, true));
                setHorizontalAlignment(JLabel.CENTER);
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 1, DesktopTheme.LINE),
                        BorderFactory.createEmptyBorder(4, 6, 4, 6)));
                return this;
            }
        };
        regionTable.getTableHeader().setDefaultRenderer(headerRenderer);

        DefaultTableCellRenderer tableRenderer = new DefaultTableCellRenderer() {
            @Override
            public java.awt.Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                java.awt.Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row,
                        column);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? DesktopTheme.CARD_BACKGROUND : DesktopTheme.CARD_BACKGROUND_ALT);
                    c.setForeground(DesktopTheme.INK_SECONDARY);
                }
                if (column == 0 && value != null) {
                    setHorizontalAlignment(JLabel.CENTER);
                    setFont(DesktopTheme.font(11, true));
                    if (!isSelected) {
                        c.setForeground(DesktopTheme.ROSE);
                    }
                } else {
                    setHorizontalAlignment(JLabel.CENTER);
                    setFont(DesktopTheme.font(11, false));
                }
                return c;
            }
        };
        regionTable.setDefaultRenderer(Integer.class, tableRenderer);
        regionTable.setDefaultRenderer(Double.class, tableRenderer);
        regionTable.setDefaultRenderer(Object.class, tableRenderer);

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
        tableScroll.setColumnHeaderView(regionTable.getTableHeader());
        tableScroll.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, DesktopTheme.LINE));
        tableScroll.getViewport().setBackground(DesktopTheme.CARD_BACKGROUND);
        panel.add(tableScroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createStatusBar() {
        JPanel bar = new JPanel(new BorderLayout(12, 0));
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createEmptyBorder(6, 6, 4, 6));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);
        statusDot.setForeground(DesktopTheme.EMERALD_MINT);
        statusDot.setFont(DesktopTheme.font(12, true));
        status.setFont(DesktopTheme.font(11, false));
        status.setForeground(DesktopTheme.INK_SECONDARY);
        left.add(statusDot);
        left.add(status);
        bar.add(left, BorderLayout.WEST);

        JPanel center = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        center.setOpaque(false);
        pixelInspector.setFont(DesktopTheme.font(11, false));
        pixelInspector.setForeground(DesktopTheme.CYAN);
        center.add(pixelInspector);
        bar.add(center, BorderLayout.CENTER);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);
        progress.setPreferredSize(new Dimension(100, 6));
        progress.setForeground(DesktopTheme.INDIGO);
        progress.setBackground(DesktopTheme.INPUT_BG);
        progress.setBorder(BorderFactory.createLineBorder(DesktopTheme.LINE));
        progress.setVisible(false);
        right.add(progress);
        bar.add(right, BorderLayout.EAST);

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
                if (!resettingSettings)
                    invalidateAnalysis();
            });
        }
        methodCombo.addActionListener(e -> {
            if (!resettingSettings)
                invalidateAnalysis();
        });
        correctLighting.addActionListener(e -> {
            if (!resettingSettings)
                invalidateAnalysis();
        });
    }

    private void configureDragAndDrop() {
        if (java.awt.GraphicsEnvironment.isHeadless())
            return;
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

    /** Prompts the user to select an image file via desktop file chooser. */
    public void openImage() {
        if (busy)
            return;
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Choose a wall photograph for crack inspection");
        chooser.setFileFilter(
                new FileNameExtensionFilter("Photographs (*.png, *.jpg, *.jpeg, *.bmp)", "png", "jpg", "jpeg", "bmp"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION)
            return;
        loadFile(chooser.getSelectedFile().toPath());
    }

    /**
     * Loads a file path and initializes the image previews.
     *
     * @param file path to image file
     */
    public void loadFile(Path file) {
        if (busy)
            return;
        runTask("Loading photograph...", () -> ImageFiles.read(file), image -> {
            currentFilePath = file;
            source = image;
            original.setImage(image);
            fileDimensionsBadge.setText(image.getWidth() + " × " + image.getHeight() + " px");
            fileDimensionsBadge.setToolTipText(file.toAbsolutePath().toString());
            invalidateAnalysis();
            statusDot.setText("●");
            statusDot.setForeground(DesktopTheme.SUCCESS);
            status.setText("Loaded " + file.getFileName() + " (" + image.getWidth() + " × " + image.getHeight()
                    + " px). Ready for analysis.");
            status.setForeground(DesktopTheme.INK_SECONDARY);
        });
    }

    /**
     * Loads one of the sample wall photographs bundled in the repository.
     *
     * @param sampleName sample file name (e.g. "wall-cracked.png" or
     *                   "wall-clean.png")
     */
    public void loadSampleFile(String sampleName) {
        if (busy)
            return;
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
        runTask("Loading sample photograph...", () -> ImageFiles.read(
                DetectorWorkspace.class.getResourceAsStream("/samples/" + sampleName), sampleName), image -> {
                    currentFilePath = null;
                    source = image;
                    original.setImage(image);
                    fileDimensionsBadge.setText(image.getWidth() + " × " + image.getHeight() + " px");
                    fileDimensionsBadge.setToolTipText("Bundled sample: " + sampleName);
                    invalidateAnalysis();
                    statusDot.setText("●");
                    statusDot.setForeground(DesktopTheme.SUCCESS);
                    status.setText("Loaded bundled sample " + sampleName + ". Ready for analysis.");
                    status.setForeground(DesktopTheme.INK_SECONDARY);
                });
    }

    /**
     * Executes the wall crack detection algorithm in a background SwingWorker
     * thread.
     */
    public void analyze() {
        if (busy)
            return;
        if (source == null) {
            statusDot.setForeground(DesktopTheme.WARNING);
            status.setText("Open a wall photograph before running analysis.");
            return;
        }
        try {
            for (JSpinner setting : settings)
                setting.commitEdit();
        } catch (java.text.ParseException exception) {
            showError("Enter valid numeric values within allowed ranges.");
            return;
        }

        DetectionConfig config = new DetectionConfig(
                (String) methodCombo.getSelectedItem(),
                value(sensitivity),
                value(minArea),
                ((Number) maxCircularity.getValue()).doubleValue(),
                correctLighting.isSelected(),
                value(closingRadius),
                value(maxDimension));
        BufferedImage input = source;

        runTask("Analyzing wall surface using ImageJ thresholding and particle analysis...", () -> {
            DetectionResult detection = new CrackDetector().detect(input, config);
            return new AnalysisViews(detection, ResultImages.overlay(detection), ResultImages.mask(detection));
        }, views -> {
            result = views.result();
            overlay.setImage(views.overlay());
            overlay.setRegions(result.regions());
            mask.setImage(views.mask());

            table.setRowCount(0);
            for (CrackRegion r : result.regions()) {
                table.addRow(new Object[] {
                        r.id(), r.x(), r.y(), r.width(), r.height(),
                        r.pixelCount(), Math.round(r.span() * 100.0) / 100.0,
                        Math.round(r.circularity() * 1000.0) / 1000.0,
                        Math.round(r.estimatedThickness() * 100.0) / 100.0,
                        r.orientation()
                });
            }

            int count = result.regions().size();
            regionCount.setText(count == 1 ? "1 region" : count + " regions");
            if (count == 0) {
                regionBadge.setText("CLEAN");
                regionBadge.setForeground(DesktopTheme.EMERALD_MINT);
                regionBadge.setBackground(DesktopTheme.EMERALD_DARK);
                regionDetail.setText("None at current settings");
                regionCount.setForeground(DesktopTheme.EMERALD_MINT);
                maxSpanMetric.setText("0.0 px");
            } else {
                regionBadge.setText(count + " DETECTED");
                regionBadge.setForeground(DesktopTheme.ROSE);
                regionBadge.setBackground(DesktopTheme.ROSE_DARK);
                regionDetail.setText(count == 1 ? "1 connected crack" : count + " connected cracks");
                regionCount.setForeground(DesktopTheme.ROSE);
                maxSpanMetric.setText(String.format(Locale.ROOT, "~%.1f px", result.maximumEstimatedThickness()));
            }

            coverage.setText(String.format(Locale.ROOT, "%.3f%%", result.coveragePercent()));
            coverage.setForeground(DesktopTheme.CYAN);
            coverageDetail.setText(String.format(Locale.ROOT, "%,d candidate pixels", result.candidatePixels()));

            maxSpanMetric.setForeground(DesktopTheme.AMBER_GOLD);
            analysisSize.setText(result.width() + " × " + result.height() + " px");
            analysisSize.setForeground(DesktopTheme.EMERALD_MINT);
            double scale = (double) result.width() / result.originalWidth();
            analysisScaleDetail.setText(String.format(Locale.ROOT, "Scale: %.2f × | Auto-threshold: %d",
                    scale, result.effectiveThreshold()));

            summary.setText(count == 0 ? "No candidates found; this does not prove the surface is crack-free"
                    : "Found " + count + " candidate regions. Double-click row to center viewport.");
            statusDot.setText("✓");
            statusDot.setForeground(DesktopTheme.EMERALD_MINT);
            status.setText("Analysis complete: " + count + " candidate regions found (threshold "
                    + result.effectiveThreshold() + "). Ready to review and export.");
            status.setForeground(DesktopTheme.EMERALD_MINT);
        });
    }

    /**
     * Exports the completed detection overlay, mask, CSV, and JSON report bundle.
     */
    public void export() {
        if (busy || result == null)
            return;
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select export directory for report bundle");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
            return;
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

    /** Copies region measurements table to system clipboard as CSV text. */
    public void copyTableCsv() {
        if (table.getRowCount() == 0) {
            Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new StringSelection("No crack regions available."), null);
            status.setText("No crack regions to copy.");
            return;
        }

        StringBuilder sb = new StringBuilder(
                "ID,X,Y,Width,Height,Pixels,Box_diagonal_px,Circularity,Estimated_thickness_px,Orientation\n");
        for (int r = 0; r < table.getRowCount(); r++) {
            for (int c = 0; c < table.getColumnCount(); c++) {
                sb.append(table.getValueAt(r, c));
                if (c < table.getColumnCount() - 1)
                    sb.append(",");
            }
            sb.append("\n");
        }
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(sb.toString()), null);
        status.setText("Copied " + table.getRowCount() + " region rows to clipboard as CSV.");
    }

    /** Copies summary metrics to clipboard. */
    public void copySummary() {
        if (result == null) {
            status.setText("Run analysis before copying summary.");
            return;
        }
        String text = String.format(Locale.ROOT,
                "Wall Crack Detection Summary (ImageJ)\n"
                        + "Candidate Regions: %d\n"
                        + "Coverage: %.4f%%\n"
                        + "Candidate Pixels: %d\n"
                        + "Widest Estimated Candidate: %.2f analysis px\n"
                        + "Effective Threshold: %d\n"
                        + "Threshold Method: %s\n"
                        + "Analysis Dimensions: %d × %d px\n"
                        + "Original Dimensions: %d × %d px\n"
                        + "Source: %s\n",
                result.regions().size(), result.coveragePercent(), result.candidatePixels(),
                result.maximumEstimatedThickness(),
                result.effectiveThreshold(), result.config().thresholdMethod(),
                result.width(), result.height(), result.originalWidth(), result.originalHeight(),
                currentFilePath != null ? currentFilePath.getFileName().toString() : "Unknown");
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
        status.setText("Analysis summary copied to clipboard.");
    }

    /** Resets parameters back to the standard defaults. */
    public void resetDefaults() {
        if (busy)
            return;
        DetectionConfig config = DetectionConfig.defaults();
        resettingSettings = true;
        try {
            methodCombo.setSelectedItem(config.thresholdMethod());
            sensitivity.setValue(config.sensitivityOffset());
            minArea.setValue(config.minArea());
            maxCircularity.setValue(config.maxCircularity());
            correctLighting.setSelected(config.correctLighting());
            closingRadius.setValue(config.closingRadius());
            maxDimension.setValue(config.maxDimension());
        } finally {
            resettingSettings = false;
        }
        invalidateAnalysis();
    }

    /**
     * Switches active inspection view mode ("Highlighted Overlay" or "Binary
     * Mask").
     * 
     * @param mode inspection view mode name
     */
    public void setViewMode(String mode) {
        if (!"Highlighted Overlay".equals(mode) && !"Binary Mask".equals(mode))
            return;
        this.activeViewMode = mode;
        updateViewTabs();
        viewCards.show(resultsCardPanel, mode);
        updateResultZoomBadge();
    }

    /** Zooms in on previews. */
    public void zoomIn() {
        activeResultPanel().zoomIn();
        original.zoomIn();
    }

    /** Zooms out on previews. */
    public void zoomOut() {
        activeResultPanel().zoomOut();
        original.zoomOut();
    }

    /** Resets zoom to fit window on previews. */
    public void resetZoom() {
        activeResultPanel().resetZoom();
        original.resetZoom();
    }

    /** Sets magnification to 1:1 actual pixels on previews. */
    public void actualSize() {
        activeResultPanel().actualSize();
        original.actualSize();
    }

    /**
     * Whether an image is currently loaded.
     * 
     * @return true if an image is loaded
     */
    public boolean hasImage() {
        return source != null;
    }

    /**
     * Whether analysis has been performed.
     * 
     * @return true if a valid analysis result is available
     */
    public boolean hasResult() {
        return result != null;
    }

    private <T> void runTask(String message, Callable<T> task, Consumer<T> success) {
        if (busy)
            return;
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
        regionCount.setForeground(DesktopTheme.ROSE);
        regionBadge.setText("IDLE");
        regionBadge.setForeground(DesktopTheme.ROSE);
        regionBadge.setBackground(DesktopTheme.ROSE_DARK);
        regionDetail.setText("Waiting for analysis");
        coverage.setText("—");
        coverage.setForeground(DesktopTheme.CYAN);
        coverageDetail.setText("0 pixels flagged");
        maxSpanMetric.setText("—");
        maxSpanMetric.setForeground(DesktopTheme.AMBER_GOLD);
        analysisSize.setText("—");
        analysisSize.setForeground(DesktopTheme.EMERALD_MINT);
        analysisSize.setText("—");
        analysisScaleDetail.setText(source != null ? source.getWidth() + " × " + source.getHeight() + " px loaded"
                : "Analysis limit: 1600 px");
        if (source != null) {
            statusDot.setText("●");
            statusDot.setForeground(DesktopTheme.WARNING);
            status.setText("Parameters modified. Click 'Analyze Wall' to update results.");
            status.setForeground(DesktopTheme.INK_SECONDARY);
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
        methodCombo.setEnabled(!busy);
        correctLighting.setEnabled(!busy);
        for (JSpinner setting : settings)
            setting.setEnabled(!busy);
    }

    private void showError(String message) {
        statusDot.setText("✕");
        statusDot.setForeground(DesktopTheme.DANGER);
        status.setText("Operation failed: " + message);
        status.setForeground(DesktopTheme.DANGER);
        if (!java.awt.GraphicsEnvironment.isHeadless()) {
            JOptionPane.showMessageDialog(this, message, "Wall Crack Detector", JOptionPane.ERROR_MESSAGE);
        }
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

    private static void addSliderControl(JPanel panel, GridBagConstraints gbc,
            String label, JSpinner spinner, JLabel badge,
            String tooltip, int min, int max,
            java.util.function.IntFunction<String> formatter,
            Color accentColor) {
        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 2, 0);

        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);
        JLabel caption = DesktopTheme.label(label, 11, true, DesktopTheme.INK_SECONDARY);
        caption.setToolTipText(tooltip);
        headerRow.add(caption, BorderLayout.WEST);
        headerRow.add(badge, BorderLayout.EAST);
        panel.add(headerRow, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 6, 0);

        JSlider slider = new JSlider(min, max, value(spinner));
        slider.setOpaque(false);
        slider.setUI(new DesktopTheme.ModernSliderUI(slider, accentColor));
        slider.setToolTipText(tooltip);

        badge.setText(formatter.apply(value(spinner)));

        slider.addChangeListener(e -> {
            int v = slider.getValue();
            spinner.setValue(v);
            badge.setText(formatter.apply(v));
        });

        spinner.addChangeListener(e -> {
            int v = value(spinner);
            if (v >= min && v <= max && slider.getValue() != v) {
                slider.setValue(v);
            }
            badge.setText(formatter.apply(v));
        });

        panel.add(slider, gbc);
    }

    private static void addDoubleSliderControl(JPanel panel, GridBagConstraints gbc,
            String label, JSpinner spinner, JLabel badge,
            String tooltip, double min, double max,
            Color accentColor) {
        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 2, 0);

        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);
        JLabel caption = DesktopTheme.label(label, 11, true, DesktopTheme.INK_SECONDARY);
        caption.setToolTipText(tooltip);
        headerRow.add(caption, BorderLayout.WEST);
        headerRow.add(badge, BorderLayout.EAST);
        panel.add(headerRow, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 6, 0);

        int minInt = (int) Math.round(min * 100);
        int maxInt = (int) Math.round(max * 100);
        int initialInt = (int) Math.round(((Number) spinner.getValue()).doubleValue() * 100);

        JSlider slider = new JSlider(minInt, maxInt, initialInt);
        slider.setOpaque(false);
        slider.setUI(new DesktopTheme.ModernSliderUI(slider, accentColor));
        slider.setToolTipText(tooltip);

        badge.setText(String.format(Locale.ROOT, "%.2f", ((Number) spinner.getValue()).doubleValue()));

        slider.addChangeListener(e -> {
            double v = slider.getValue() / 100.0;
            spinner.setValue(v);
            badge.setText(String.format(Locale.ROOT, "%.2f", v));
        });

        spinner.addChangeListener(e -> {
            double v = ((Number) spinner.getValue()).doubleValue();
            int intVal = (int) Math.round(v * 100);
            if (slider.getValue() != intVal) {
                slider.setValue(intVal);
            }
            badge.setText(String.format(Locale.ROOT, "%.2f", v));
        });

        panel.add(slider, gbc);
    }

    private record AnalysisViews(DetectionResult result, BufferedImage overlay, BufferedImage mask) {
    }
}
