package org.wallcrack.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.metal.MetalLookAndFeel;

/**
 * Consistent desktop styling, modern palette, high-contrast typography, and custom widgets.
 */
public final class DesktopTheme {
    /** Primary background surface across the desktop window. */
    public static final Color BACKGROUND = new Color(245, 247, 250);

    /** High-contrast dark ink for primary text and titles. */
    public static final Color INK = new Color(15, 23, 42);

    /** Secondary muted slate tone for descriptions and secondary metrics. */
    public static final Color MUTED = new Color(100, 116, 139);

    /** Subtle border line color separating cards, panels, and table cells. */
    public static final Color LINE = new Color(226, 232, 240);

    /** Brand primary blue for primary actions and highlighted controls. */
    public static final Color BLUE = new Color(37, 99, 235);

    /** Hover state for primary buttons. */
    public static final Color BLUE_HOVER = new Color(29, 78, 216);

    /** Pressed state for primary buttons. */
    public static final Color BLUE_PRESSED = new Color(30, 64, 175);

    /** Soft surface tint for cards and elevated panels. */
    public static final Color CARD_BACKGROUND = Color.WHITE;

    /** Success emerald tint for minor severity and verified state. */
    public static final Color SUCCESS = new Color(16, 185, 129);

    /** Amber warning tint for moderate severity and caution cues. */
    public static final Color WARNING = new Color(245, 158, 11);

    /** Crimson danger tint for major severity and critical defects. */
    public static final Color DANGER = new Color(239, 68, 68);

    /** Magenta crack candidate highlight. */
    public static final Color OVERLAY_MAGENTA = new Color(220, 20, 130);

    private DesktopTheme() { }

    /**
     * Installs predictable Swing styling and font antialiasing before constructing desktop windows.
     */
    public static void install() {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        try {
            UIManager.setLookAndFeel(new MetalLookAndFeel());
        } catch (javax.swing.UnsupportedLookAndFeelException exception) {
            throw new IllegalStateException("The Java desktop theme is unavailable.", exception);
        }

        FontUIResource base = new FontUIResource(font(13, false));
        var defaults = UIManager.getDefaults();
        for (Object key : defaults.keySet().toArray()) {
            if (defaults.get(key) instanceof FontUIResource) {
                defaults.put(key, base);
            }
        }

        UIManager.put("Panel.background", BACKGROUND);
        UIManager.put("Label.foreground", INK);
        UIManager.put("TextField.background", Color.WHITE);
        UIManager.put("TextField.foreground", INK);
        UIManager.put("TextField.caretForeground", INK);
        UIManager.put("TabbedPane.selected", Color.WHITE);
        UIManager.put("TabbedPane.background", BACKGROUND);
        UIManager.put("TabbedPane.contentAreaColor", Color.WHITE);
        UIManager.put("TabbedPane.contentBorderInsets", new Insets(0, 0, 0, 0));
        UIManager.put("SplitPane.background", BACKGROUND);
        UIManager.put("SplitPaneDivider.border", BorderFactory.createEmptyBorder());
        UIManager.put("ScrollBar.width", 12);
        UIManager.put("ScrollBar.thumb", new Color(203, 213, 225));
        UIManager.put("ScrollBar.track", new Color(241, 245, 249));

        // Tooltip styling
        UIManager.put("ToolTip.background", new Color(30, 41, 59));
        UIManager.put("ToolTip.foreground", Color.WHITE);
        UIManager.put("ToolTip.border", BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(51, 65, 85)),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        UIManager.put("ToolTip.font", font(11, false));

        // Menu bar styling
        UIManager.put("MenuBar.background", Color.WHITE);
        UIManager.put("MenuBar.border", BorderFactory.createMatteBorder(0, 0, 1, 0, LINE));
        UIManager.put("Menu.foreground", INK);
        UIManager.put("Menu.selectionBackground", new Color(238, 242, 255));
        UIManager.put("Menu.selectionForeground", BLUE);
        UIManager.put("MenuItem.selectionBackground", new Color(238, 242, 255));
        UIManager.put("MenuItem.selectionForeground", BLUE);
        UIManager.put("MenuItem.acceleratorForeground", MUTED);
        UIManager.put("PopupMenu.border", BorderFactory.createLineBorder(LINE));
        UIManager.put("Separator.foreground", LINE);
    }

    /**
     * Resolves a scalable font instance.
     *
     * @param size base point size
     * @param bold true for bold weight, false for plain
     * @return font instance
     */
    public static Font font(int size, boolean bold) {
        return new Font(Font.SANS_SERIF, bold ? Font.BOLD : Font.PLAIN, Math.max(12, size + 1));
    }

    /**
     * Constructs a styled label with predetermined size and color.
     *
     * @param text label text
     * @param size base point size
     * @param bold whether to render in bold weight
     * @param color foreground text color
     * @return configured label
     */
    public static JLabel label(String text, int size, boolean bold, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(font(size, bold));
        label.setForeground(color);
        return label;
    }

    /**
     * Creates a custom styled button with rounded corners and distinct active states.
     *
     * @param text button caption
     * @param primary true for solid primary accent, false for neutral surface
     * @return interactive button
     */
    public static JButton button(String text, boolean primary) {
        JButton button = new JButton(text) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D canvas = (Graphics2D) graphics.create();
                try {
                    canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    Color background = primary ? BLUE : Color.WHITE;
                    Color border = primary ? BLUE : LINE;
                    Color textFg = primary ? Color.WHITE : INK;

                    if (!isEnabled()) {
                        background = primary ? new Color(203, 213, 225) : new Color(241, 245, 249);
                        border = new Color(226, 232, 240);
                        textFg = new Color(148, 163, 184);
                    } else if (getModel().isPressed()) {
                        background = primary ? BLUE_PRESSED : new Color(226, 232, 240);
                    } else if (getModel().isRollover()) {
                        background = primary ? BLUE_HOVER : new Color(248, 250, 252);
                        border = primary ? BLUE_HOVER : new Color(203, 213, 225);
                    }

                    canvas.setColor(background);
                    canvas.fillRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 8, 8);
                    canvas.setColor(border);
                    canvas.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 8, 8);

                    if (isFocusOwner()) {
                        canvas.setColor(new Color(147, 197, 253));
                        canvas.setStroke(new BasicStroke(2f));
                        canvas.drawRoundRect(2, 2, getWidth() - 5, getHeight() - 5, 8, 8);
                    }

                    canvas.setFont(getFont());
                    canvas.setColor(textFg);
                    var metrics = canvas.getFontMetrics();
                    int stringX = (getWidth() - metrics.stringWidth(getText())) / 2;
                    int stringY = (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent();
                    canvas.drawString(getText(), stringX, stringY);
                } finally {
                    canvas.dispose();
                }
            }
        };

        button.setFont(font(13, true));
        int width = button.getFontMetrics(button.getFont()).stringWidth(text) + 32;
        button.setPreferredSize(new Dimension(Math.max(88, width), 40));
        button.setMinimumSize(button.getPreferredSize());
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    /**
     * Creates a compact icon or symbol button for toolbars and quick actions.
     *
     * @param text symbol or short text
     * @param tooltip helpful explanation
     * @return styled compact toolbar button
     */
    public static JButton iconButton(String text, String tooltip) {
        JButton button = new JButton(text) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D canvas = (Graphics2D) graphics.create();
                try {
                    canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    Color bg = Color.WHITE;
                    Color border = LINE;
                    Color fg = INK;

                    if (!isEnabled()) {
                        bg = new Color(248, 250, 252);
                        fg = new Color(203, 213, 225);
                    } else if (getModel().isPressed()) {
                        bg = new Color(226, 232, 240);
                    } else if (getModel().isRollover()) {
                        bg = new Color(241, 245, 249);
                        border = new Color(203, 213, 225);
                    }

                    canvas.setColor(bg);
                    canvas.fillRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 6, 6);
                    canvas.setColor(border);
                    canvas.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 6, 6);

                    canvas.setFont(getFont());
                    canvas.setColor(fg);
                    var metrics = canvas.getFontMetrics();
                    int stringX = (getWidth() - metrics.stringWidth(getText())) / 2;
                    int stringY = (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent();
                    canvas.drawString(getText(), stringX, stringY);
                } finally {
                    canvas.dispose();
                }
            }
        };

        button.setFont(font(12, true));
        button.setToolTipText(tooltip);
        button.setPreferredSize(new Dimension(34, 28));
        button.setMinimumSize(button.getPreferredSize());
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    /**
     * Creates an elevated card panel with clean border and background.
     *
     * @return styled panel container
     */
    public static JPanel cardPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(CARD_BACKGROUND);
        panel.setBorder(BorderFactory.createLineBorder(LINE));
        return panel;
    }

    /**
     * Creates a pill-shaped badge component for status and severity cues.
     *
     * @param text badge text
     * @param bg background fill
     * @param fg text color
     * @return styled badge component
     */
    public static JLabel badge(String text, Color bg, Color fg) {
        JLabel label = new JLabel(text) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D canvas = (Graphics2D) graphics.create();
                try {
                    canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    canvas.setColor(bg);
                    canvas.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                    super.paintComponent(graphics);
                } finally {
                    canvas.dispose();
                }
            }
        };
        label.setFont(font(10, true));
        label.setForeground(fg);
        label.setBorder(BorderFactory.createEmptyBorder(2, 9, 2, 9));
        label.setOpaque(false);
        return label;
    }

    /**
     * Creates multi-resolution application icons for desktop window frames and taskbars.
     *
     * @return list of application icons at various standard resolutions
     */
    public static List<Image> createApplicationIcons() {
        int[] sizes = {16, 24, 32, 48, 64, 128};
        List<Image> icons = new ArrayList<>();
        for (int size : sizes) {
            BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = image.createGraphics();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Background wall tile
                g.setColor(new Color(30, 41, 59));
                g.fillRoundRect(0, 0, size, size, Math.max(4, size / 4), Math.max(4, size / 4));

                // Stylized brick mortar line
                g.setColor(new Color(51, 65, 85));
                g.setStroke(new BasicStroke(Math.max(1f, size / 16f)));
                g.drawLine(0, size / 2, size, size / 2);
                g.drawLine(size / 2, 0, size / 2, size / 2);
                g.drawLine(size / 4, size / 2, size / 4, size);
                g.drawLine(3 * size / 4, size / 2, 3 * size / 4, size);

                // Jagged crack in vibrant magenta
                g.setColor(new Color(236, 72, 153));
                g.setStroke(new BasicStroke(Math.max(1.5f, size / 10f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int[] xPoints = {size / 6, size / 3, size / 2, 3 * size / 5, 5 * size / 6};
                int[] yPoints = {size / 8, 3 * size / 8, 2 * size / 5, 7 * size / 10, 7 * size / 8};
                g.drawPolyline(xPoints, yPoints, xPoints.length);

                // Inspection crosshair/focus circle in cyan
                g.setColor(new Color(56, 189, 248));
                g.setStroke(new BasicStroke(Math.max(1f, size / 20f)));
                int circlePad = size / 5;
                g.drawOval(circlePad, circlePad, size - 2 * circlePad, size - 2 * circlePad);
            } finally {
                g.dispose();
            }
            icons.add(image);
        }
        return icons;
    }
}
