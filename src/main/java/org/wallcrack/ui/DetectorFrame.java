package org.wallcrack.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

/**
 * Desktop application window containing menu actions, accelerators, application icons,
 * and the wall photograph analysis workspace.
 *
 * @serial exclude
 */
public final class DetectorFrame extends JFrame {
    private static final long serialVersionUID = 1L;

    private final DetectorWorkspace workspace;

    /**
     * Creates and initializes the application window on the Swing event dispatch thread.
     */
    public DetectorFrame() {
        super("Wall Crack Detector — Surface Inspection & Edge Analysis");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Multi-resolution application icons
        setIconImages(DesktopTheme.createApplicationIcons());

        // Size relative to the usable screen area. If the restored ("normal") size equals the
        // maximized size, the title-bar maximize/restore button appears to do nothing, which
        // happens on small or HiDPI-scaled screens when fixed pixel sizes are clamped to the screen.
        var screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        setMinimumSize(new Dimension(Math.min(1180, screen.width * 3 / 4), Math.min(760, screen.height * 3 / 4)));
        setSize(Math.min(1500, screen.width * 17 / 20), Math.min(960, screen.height * 17 / 20));

        workspace = new DetectorWorkspace();
        setContentPane(workspace);

        setJMenuBar(createMenuBar());
        setLocationRelativeTo(null);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
    }

    private JMenuBar createMenuBar() {
        JMenuBar bar = new JMenuBar();
        bar.setBackground(DesktopTheme.BACKGROUND);
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, DesktopTheme.LINE));
        bar.add(createFileMenu());
        bar.add(createEditMenu());
        bar.add(createViewMenu());
        bar.add(createAnalysisMenu());
        bar.add(createHelpMenu());
        return bar;
    }

    private JMenu createFileMenu() {
        JMenu menu = new JMenu("File");
        menu.setMnemonic(KeyEvent.VK_F);

        JMenuItem openItem = new JMenuItem("Open Photograph...");
        openItem.setMnemonic(KeyEvent.VK_O);
        openItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
        openItem.addActionListener(e -> workspace.openImage());

        JMenu samplesMenu = new JMenu("Load Sample Photo");
        JMenuItem sampleCracked = new JMenuItem("Wall with Crack (wall-cracked.png)");
        sampleCracked.addActionListener(e -> workspace.loadSampleFile("wall-cracked.png"));
        JMenuItem sampleClean = new JMenuItem("Clean Uniform Wall (wall-clean.png)");
        sampleClean.addActionListener(e -> workspace.loadSampleFile("wall-clean.png"));
        samplesMenu.add(sampleCracked);
        samplesMenu.add(sampleClean);

        JMenuItem exportItem = new JMenuItem("Export Report Bundle...");
        exportItem.setMnemonic(KeyEvent.VK_E);
        exportItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_DOWN_MASK));
        exportItem.addActionListener(e -> workspace.export());

        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.setMnemonic(KeyEvent.VK_X);
        exitItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_DOWN_MASK));
        exitItem.addActionListener(e -> {
            dispose();
            System.exit(0);
        });

        menu.add(openItem);
        menu.add(samplesMenu);
        menu.addSeparator();
        menu.add(exportItem);
        menu.addSeparator();
        menu.add(exitItem);
        return menu;
    }

    private JMenu createEditMenu() {
        JMenu menu = new JMenu("Edit");
        menu.setMnemonic(KeyEvent.VK_E);

        JMenuItem copyCsv = new JMenuItem("Copy Regions as CSV");
        copyCsv.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
        copyCsv.addActionListener(e -> workspace.copyTableCsv());

        JMenuItem copySummary = new JMenuItem("Copy Analysis Summary");
        copySummary.addActionListener(e -> workspace.copySummary());

        menu.add(copyCsv);
        menu.add(copySummary);
        return menu;
    }

    private JMenu createViewMenu() {
        JMenu menu = new JMenu("View");
        menu.setMnemonic(KeyEvent.VK_V);

        JMenuItem overlayView = new JMenuItem("Highlighted Overlay View");
        overlayView.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_1, InputEvent.CTRL_DOWN_MASK));
        overlayView.addActionListener(e -> workspace.setViewMode("Highlighted Overlay"));

        JMenuItem maskView = new JMenuItem("Binary Mask View");
        maskView.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_2, InputEvent.CTRL_DOWN_MASK));
        maskView.addActionListener(e -> workspace.setViewMode("Binary Mask"));

        JMenuItem zoomIn = new JMenuItem("Zoom In");
        zoomIn.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, InputEvent.CTRL_DOWN_MASK));
        zoomIn.addActionListener(e -> workspace.zoomIn());

        JMenuItem zoomOut = new JMenuItem("Zoom Out");
        zoomOut.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, InputEvent.CTRL_DOWN_MASK));
        zoomOut.addActionListener(e -> workspace.zoomOut());

        JMenuItem actualSize = new JMenuItem("Actual Size (100%)");
        actualSize.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_0, InputEvent.CTRL_DOWN_MASK));
        actualSize.addActionListener(e -> workspace.actualSize());

        JMenuItem fitWindow = new JMenuItem("Fit to Window");
        fitWindow.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK));
        fitWindow.addActionListener(e -> workspace.resetZoom());

        menu.add(overlayView);
        menu.add(maskView);
        menu.addSeparator();
        menu.add(zoomIn);
        menu.add(zoomOut);
        menu.add(actualSize);
        menu.add(fitWindow);
        return menu;
    }

    private JMenu createAnalysisMenu() {
        JMenu menu = new JMenu("Analysis");
        menu.setMnemonic(KeyEvent.VK_A);

        JMenuItem runDetection = new JMenuItem("Run Crack Detection");
        runDetection.setMnemonic(KeyEvent.VK_R);
        runDetection.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
        runDetection.addActionListener(e -> workspace.analyze());

        JMenuItem resetItem = new JMenuItem("Reset Settings to Default");
        resetItem.addActionListener(e -> workspace.resetDefaults());

        menu.add(runDetection);
        menu.addSeparator();
        menu.add(resetItem);
        return menu;
    }

    private JMenu createHelpMenu() {
        JMenu menu = new JMenu("Help");
        menu.setMnemonic(KeyEvent.VK_H);

        JMenuItem guideItem = new JMenuItem("Wall Inspection & Photography Guide");
        guideItem.addActionListener(e -> showInspectionGuide());

        JMenuItem algoItem = new JMenuItem("Detection Methodology & Algorithm");
        algoItem.addActionListener(e -> showMethodology());

        JMenuItem shortcutsItem = new JMenuItem("Keyboard Shortcuts");
        shortcutsItem.addActionListener(e -> showShortcuts());

        JMenuItem aboutItem = new JMenuItem("About Wall Crack Detector");
        aboutItem.addActionListener(e -> showAboutDialog());

        menu.add(guideItem);
        menu.add(algoItem);
        menu.add(shortcutsItem);
        menu.addSeparator();
        menu.add(aboutItem);
        return menu;
    }

    private void showInspectionGuide() {
        String message = """
                WALL INSPECTION & PHOTOGRAPHY BEST PRACTICES

                For optimal edge detection results:
                1. Lighting: Ensure uniform, diffuse illumination across the wall surface.
                   Avoid direct flash casting harsh artificial shadows.
                2. Framing: Position camera parallel to the wall surface to avoid perspective distortion.
                3. Surface Texture: For stucco, brick, or heavily textured paint, increase
                   'Minimum Particle Area' and lower 'Maximum Circularity' to suppress compact noise.
                4. Hairline Cracks: For faint marks, raise 'Sensitivity Offset' gradually and
                   compare the highlighted overlay against the original photograph.
                5. Resolution: High megapixel photos are automatically resized to fit within
                   the 'Analysis Size Limit' while preserving aspect ratio.

                Candidate thickness is an estimate in resized analysis pixels. It is not a
                millimetre measurement, confidence score, or structural-safety assessment.
                """;
        JOptionPane.showMessageDialog(this, message, "Wall Inspection Guide", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showMethodology() {
        String message = """
                DETECTION ALGORITHM SPECIFICATION (ImageJ Core)

                1. Grayscale Conversion:
                   Standard luminance: gray = (299*R + 587*G + 114*B + 500) / 1000.
                2. Optional Uneven-Lighting Correction:
                   ImageJ rolling-ball background subtraction isolates dark surface marks.
                3. Automatic Thresholding:
                   ImageJ AutoThresholder (Triangle or Otsu) with sensitivity adjustment.
                4. Optional Binary Closing:
                   ImageJ morphological dilation and erosion to bridge minor hairline discontinuities.
                5. Particle Analysis:
                   ImageJ ParticleAnalyzer extracts candidate regions with area and circularity filters.
                """;
        JOptionPane.showMessageDialog(this, message, "Detection Methodology", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showShortcuts() {
        String message = """
                KEYBOARD SHORTCUTS

                Ctrl + O           Open photograph
                F5                 Run wall crack detection
                Ctrl + E           Export report bundle
                Ctrl + Shift + C   Copy region measurements as CSV
                Ctrl + 1           Switch to Highlighted Overlay view
                Ctrl + 2           Switch to Binary Mask view
                Ctrl + Plus / =    Zoom In
                Ctrl + Minus       Zoom Out
                Ctrl + 0           Actual Size (100% 1:1)
                Ctrl + F           Reset View / Fit to Window
                Ctrl + Q           Exit application

                Double-click row in measurements table to zoom & center that crack!
                """;
        JOptionPane.showMessageDialog(this, message, "Keyboard Shortcuts", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showAboutDialog() {
        JPanel panel = new JPanel(new BorderLayout(16, 12));
        panel.setBackground(DesktopTheme.CARD_BACKGROUND);
        panel.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        JLabel title = DesktopTheme.label("Wall Crack Detector", 20, true, Color.WHITE);
        JLabel version = DesktopTheme.label("Version 1.0.0 — Java 17+ & ImageJ 1.54p", 12, false, DesktopTheme.CYAN);
        JLabel desc = DesktopTheme.label("Desktop & CLI application for reviewing possible cracks in wall photographs.",
                12, false, DesktopTheme.INK_SECONDARY);
        JLabel note = DesktopTheme.label("Engineered with high-contrast vibrant theme and ImageJ particle vision.", 11, false, DesktopTheme.MUTED);

        content.add(title);
        content.add(Box.createVerticalStrut(4));
        content.add(version);
        content.add(Box.createVerticalStrut(10));
        content.add(desc);
        content.add(Box.createVerticalStrut(6));
        content.add(note);

        panel.add(content, BorderLayout.CENTER);
        JOptionPane.showMessageDialog(this, panel, "About Wall Crack Detector", JOptionPane.PLAIN_MESSAGE);
    }
}
