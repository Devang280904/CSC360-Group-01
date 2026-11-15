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
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.metal.MetalLookAndFeel;

/**
 * Modern high-contrast dark theme and vibrant visual styling matching TriangleFX.
 * Features deep space-navy backgrounds (#0B111E), elevated slate cards (#131B2E),
 * and radiant neon accents across data visualization and control elements.
 */
public final class DesktopTheme {

    // =========================================================================
    // CORE DARK THEME SURFACES & FOUNDATIONS (TriangleFX Style)
    // =========================================================================

    /** Deep space navy root canvas background (#0B111E). */
    public static final Color BACKGROUND = new Color(11, 17, 30);

    /** Elevated card and panel surface (#131B2E). */
    public static final Color CARD_BACKGROUND = new Color(19, 27, 46);

    /** Subtle alternating dark row surface (#0E1524). */
    public static final Color CARD_BACKGROUND_ALT = new Color(14, 21, 36);

    /** Deep midnight canvas for image and diagram viewports (#0A0E17). */
    public static final Color CANVAS_BG = new Color(10, 14, 23);

    /** Inset dark surface for inputs, comboboxes, and spinners (#0F172A). */
    public static final Color INPUT_BG = new Color(15, 23, 42);

    /** Subtle border outlining inputs (#334155). */
    public static final Color INPUT_BORDER = new Color(51, 65, 85);

    /** Crisp pure white for primary text and headings (#F8FAFC). */
    public static final Color INK = new Color(248, 250, 252);

    /** Light slate for readable labels and details (#CBD5E1). */
    public static final Color INK_SECONDARY = new Color(203, 213, 225);

    /** Muted slate tone for secondary labels and hints (#94A3B8). */
    public static final Color MUTED = new Color(148, 163, 184);

    /** Subtle dark slate line separating panels and cards (#1E293B). */
    public static final Color LINE = new Color(30, 41, 59);

    /** Slightly highlighted border for interactive boundaries (#2D3B52). */
    public static final Color LINE_LIGHT = new Color(45, 59, 82);

    // =========================================================================
    // RADIANT NEON ACCENT PALETTE (Matching TriangleFX Screenshot)
    // =========================================================================

    /** Primary Brand Blue / Electric Sky (#38BDF8). */
    public static final Color BLUE = new Color(56, 189, 248);
    /** Hover state fill for primary brand blue. */
    public static final Color BLUE_HOVER = new Color(14, 165, 233);
    /** Pressed state fill for primary brand blue. */
    public static final Color BLUE_PRESSED = new Color(2, 132, 199);

    /** Vibrant Royal Indigo / Purple for primary action buttons (#6366F1). */
    public static final Color INDIGO = new Color(99, 102, 241);
    /** Hover state fill for indigo buttons. */
    public static final Color INDIGO_HOVER = new Color(79, 70, 229);
    /** Pressed state fill for indigo buttons. */
    public static final Color INDIGO_PRESSED = new Color(67, 56, 202);
    /** Dark tinted background for indigo badges. */
    public static final Color INDIGO_DARK = new Color(30, 27, 75);
    /** Translucent highlight for indigo elements. */
    public static final Color INDIGO_LIGHT = new Color(49, 46, 129, 200);
    /** Vibrant outline border for indigo elements. */
    public static final Color INDIGO_BORDER = new Color(129, 140, 248);

    /** Radiant Sky / Electric Cyan for subtitles, metrics, and line L2 (#38BDF8). */
    public static final Color CYAN = new Color(56, 189, 248);
    /** Hover state fill for cyan buttons and controls. */
    public static final Color CYAN_HOVER = new Color(14, 165, 233);
    /** Dark tinted background for cyan badges. */
    public static final Color CYAN_DARK = new Color(8, 47, 73);
    /** Translucent highlight for cyan elements. */
    public static final Color CYAN_LIGHT = new Color(12, 74, 110, 200);
    /** Vibrant outline border for cyan elements. */
    public static final Color CYAN_BORDER = new Color(2, 132, 199);

    /** Vibrant Rose / Coral for defects, cracks, and line L1 (#F43F5E). */
    public static final Color ROSE = new Color(244, 63, 94);
    /** Hover state fill for rose buttons and controls. */
    public static final Color ROSE_HOVER = new Color(225, 29, 72);
    /** Dark tinted background for rose badges. */
    public static final Color ROSE_DARK = new Color(76, 5, 25);
    /** Translucent highlight for rose elements. */
    public static final Color ROSE_LIGHT = new Color(136, 19, 55, 200);
    /** Vibrant outline border for rose elements. */
    public static final Color ROSE_BORDER = new Color(190, 18, 60);

    /** Warm Sunset Amber / Gold for exports, spans, and line L3 (#F59E0B). */
    public static final Color AMBER_GOLD = new Color(245, 158, 11);
    /** Hover state fill for amber buttons and controls. */
    public static final Color AMBER_HOVER = new Color(217, 119, 6);
    /** Dark tinted background for amber badges. */
    public static final Color AMBER_DARK = new Color(69, 26, 3);
    /** Translucent highlight for amber elements. */
    public static final Color AMBER_LIGHT = new Color(120, 53, 15, 200);
    /** Vibrant outline border for amber elements. */
    public static final Color AMBER_BORDER = new Color(180, 83, 9);

    /** Radiant Mint / Emerald for verified banner and clean state (#34D399). */
    public static final Color EMERALD_MINT = new Color(52, 211, 153);
    /** Hover state fill for emerald buttons and controls. */
    public static final Color EMERALD_HOVER = new Color(16, 185, 129);
    /** Dark tinted background for emerald badges. */
    public static final Color EMERALD_DARK = new Color(6, 78, 59);
    /** Translucent highlight for emerald elements. */
    public static final Color EMERALD_LIGHT = new Color(6, 95, 70, 200);
    /** Vibrant outline border for emerald elements. */
    public static final Color EMERALD_BORDER = new Color(5, 150, 105);

    /** Electric Violet / Purple for algorithms and filters (#A855F7). */
    public static final Color PURPLE = new Color(168, 85, 247);
    /** Hover state fill for purple buttons and controls. */
    public static final Color PURPLE_HOVER = new Color(147, 51, 234);
    /** Dark tinted background for purple badges. */
    public static final Color PURPLE_DARK = new Color(46, 16, 101);
    /** Translucent highlight for purple elements. */
    public static final Color PURPLE_LIGHT = new Color(88, 28, 135, 200);
    /** Vibrant outline border for purple elements. */
    public static final Color PURPLE_BORDER = new Color(124, 58, 237);

    /** High-visibility magenta for candidate crack pixels (#EC4899). */
    public static final Color OVERLAY_MAGENTA = new Color(236, 72, 153);

    /** Emerald success status indicator. */
    public static final Color SUCCESS = EMERALD_MINT;

    /** Amber warning status indicator. */
    public static final Color WARNING = AMBER_GOLD;

    /** Crimson danger status indicator. */
    public static final Color DANGER = ROSE;

    private DesktopTheme() { }

    /**
     * Installs modern dark Swing styling, anti-aliasing, and customized look-and-feel.
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

        // Global surfaces
        UIManager.put("Panel.background", BACKGROUND);
        UIManager.put("Label.foreground", INK);
        UIManager.put("TextField.background", INPUT_BG);
        UIManager.put("TextField.foreground", INK);
        UIManager.put("TextField.caretForeground", CYAN);

        // Comboboxes & spinners
        UIManager.put("ComboBox.background", INPUT_BG);
        UIManager.put("ComboBox.foreground", INK);
        UIManager.put("ComboBox.selectionBackground", INDIGO);
        UIManager.put("ComboBox.selectionForeground", Color.WHITE);
        UIManager.put("Spinner.background", INPUT_BG);
        UIManager.put("Spinner.foreground", INK);

        // Tabs
        UIManager.put("TabbedPane.selected", CARD_BACKGROUND);
        UIManager.put("TabbedPane.background", BACKGROUND);
        UIManager.put("TabbedPane.contentAreaColor", CARD_BACKGROUND);
        UIManager.put("TabbedPane.contentBorderInsets", new Insets(0, 0, 0, 0));

        // Splits & scrollbars
        UIManager.put("SplitPane.background", BACKGROUND);
        UIManager.put("SplitPaneDivider.border", BorderFactory.createEmptyBorder());
        UIManager.put("ScrollBar.width", 10);
        UIManager.put("ScrollBar.thumb", new Color(51, 65, 85));
        UIManager.put("ScrollBar.track", BACKGROUND);

        // Tooltip styling
        UIManager.put("ToolTip.background", new Color(2, 6, 23));
        UIManager.put("ToolTip.foreground", INK);
        UIManager.put("ToolTip.border", BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(CYAN),
                BorderFactory.createEmptyBorder(5, 9, 5, 9)));
        UIManager.put("ToolTip.font", font(11, false));

        // Menu bar styling
        UIManager.put("MenuBar.background", BACKGROUND);
        UIManager.put("MenuBar.border", BorderFactory.createMatteBorder(0, 0, 1, 0, LINE));
        UIManager.put("Menu.foreground", INK_SECONDARY);
        UIManager.put("Menu.selectionBackground", LINE);
        UIManager.put("Menu.selectionForeground", CYAN);
        UIManager.put("MenuItem.background", INPUT_BG);
        UIManager.put("MenuItem.foreground", INK_SECONDARY);
        UIManager.put("MenuItem.selectionBackground", new Color(30, 58, 138));
        UIManager.put("MenuItem.selectionForeground", Color.WHITE);
        UIManager.put("MenuItem.acceleratorForeground", CYAN);
        UIManager.put("PopupMenu.background", INPUT_BG);
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
        return new Font(Font.SANS_SERIF, bold ? Font.BOLD : Font.PLAIN, Math.max(12, size));
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
     * Creates a custom styled button with smooth rounded corners and active states.
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
                    Color background = primary ? INDIGO : LINE;
                    Color border = primary ? INDIGO_BORDER : INPUT_BORDER;
                    Color textFg = primary ? Color.WHITE : INK_SECONDARY;

                    if (!isEnabled()) {
                        background = primary ? new Color(49, 46, 129, 120) : new Color(24, 32, 47);
                        border = new Color(30, 41, 59);
                        textFg = new Color(100, 116, 139);
                    } else if (getModel().isPressed()) {
                        background = primary ? INDIGO_PRESSED : new Color(15, 23, 42);
                    } else if (getModel().isRollover()) {
                        background = primary ? INDIGO_HOVER : new Color(51, 65, 85);
                        border = primary ? Color.WHITE : CYAN;
                    }

                    canvas.setColor(background);
                    canvas.fillRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 8, 8);
                    canvas.setColor(border);
                    canvas.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 8, 8);

                    if (isFocusOwner()) {
                        canvas.setColor(CYAN);
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
        int width = button.getFontMetrics(button.getFont()).stringWidth(text) + 28;
        button.setPreferredSize(new Dimension(Math.max(90, width), 38));
        button.setMinimumSize(button.getPreferredSize());
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    /**
     * Creates a compact button for toolbars and quick actions.
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
                    Color bg = LINE;
                    Color border = INPUT_BORDER;
                    Color fg = INK_SECONDARY;

                    if (!isEnabled()) {
                        bg = new Color(15, 23, 42);
                        fg = new Color(71, 85, 105);
                    } else if (getModel().isPressed()) {
                        bg = new Color(15, 23, 42);
                        border = CYAN;
                    } else if (getModel().isRollover()) {
                        bg = new Color(51, 65, 85);
                        border = CYAN;
                        fg = Color.WHITE;
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
        int w = button.getFontMetrics(button.getFont()).stringWidth(text) + 16;
        button.setPreferredSize(new Dimension(Math.max(34, w), 28));
        button.setMinimumSize(button.getPreferredSize());
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    /**
     * Creates an elevated dark card panel with rounded corners and subtle border.
     *
     * @return styled dark panel container
     */
    public static JPanel cardPanel() {
        JPanel panel = new JPanel() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D canvas = (Graphics2D) graphics.create();
                try {
                    canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    canvas.setColor(CARD_BACKGROUND);
                    canvas.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                    canvas.setColor(LINE);
                    canvas.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                } finally {
                    canvas.dispose();
                }
            }
        };
        panel.setOpaque(false);
        panel.setBackground(CARD_BACKGROUND);
        panel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        return panel;
    }

    /**
     * Creates an elevated card panel with a vivid top neon accent stripe and dark surface.
     *
     * @param topAccentColor vibrant header highlight bar color
     * @param cardBg dark elevated background (#131B2E)
     * @param borderColor subtle matching border color (#1E293B)
     * @return styled colorful card panel
     */
    public static JPanel accentCardPanel(Color topAccentColor, Color cardBg, Color borderColor) {
        JPanel panel = new JPanel() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D canvas = (Graphics2D) graphics.create();
                try {
                    canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    // Fill dark card background
                    canvas.setColor(cardBg);
                    canvas.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                    // Draw top glowing accent bar
                    canvas.setColor(topAccentColor);
                    canvas.fillRoundRect(0, 0, getWidth() - 1, 4, 10, 10);
                    canvas.fillRect(0, 2, getWidth() - 1, 2);
                    // Outline border
                    canvas.setColor(borderColor);
                    canvas.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                } finally {
                    canvas.dispose();
                }
            }
        };
        panel.setOpaque(false);
        panel.setBackground(cardBg);
        panel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        return panel;
    }

    /**
     * Creates a custom colorful button with rich hover, pressed, and focus visual states.
     *
     * @param text button caption
     * @param bg primary fill color
     * @param hover hover state fill
     * @param border outline border color
     * @param textFg text and icon color
     * @param radius corner roundness radius
     * @return interactive styled button
     */
    public static JButton coloredButton(String text, Color bg, Color hover, Color border, Color textFg, int radius) {
        JButton button = new JButton(text) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D canvas = (Graphics2D) graphics.create();
                try {
                    canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    Color fill = bg;
                    Color stroke = border;
                    Color fg = textFg;

                    if (!isEnabled()) {
                        fill = new Color(24, 32, 47, 180);
                        stroke = new Color(30, 41, 59);
                        fg = new Color(100, 116, 139);
                    } else if (getModel().isPressed()) {
                        fill = hover.darker();
                        stroke = border.darker();
                    } else if (getModel().isRollover()) {
                        fill = hover;
                    }

                    canvas.setColor(fill);
                    canvas.fillRoundRect(1, 1, getWidth() - 2, getHeight() - 2, radius, radius);
                    canvas.setColor(stroke);
                    canvas.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, radius, radius);

                    if (isFocusOwner()) {
                        canvas.setColor(CYAN);
                        canvas.setStroke(new BasicStroke(2f));
                        canvas.drawRoundRect(2, 2, getWidth() - 5, getHeight() - 5, radius, radius);
                    }

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

        button.setFont(font(13, true));
        int width = button.getFontMetrics(button.getFont()).stringWidth(text) + 26;
        button.setPreferredSize(new Dimension(Math.max(88, width), 36));
        button.setMinimumSize(button.getPreferredSize());
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    /**
     * Creates a pill-shaped compact action button with custom colors.
     *
     * @param text button caption
     * @param bg background fill
     * @param border border color
     * @param fg text color
     * @return styled pill button
     */
    public static JButton pillButton(String text, Color bg, Color border, Color fg) {
        JButton button = new JButton(text) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D canvas = (Graphics2D) graphics.create();
                try {
                    canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    Color fill = bg;
                    Color stroke = border;
                    Color textCol = fg;

                    if (!isEnabled()) {
                        fill = new Color(15, 23, 42);
                        stroke = new Color(30, 41, 59);
                        textCol = new Color(71, 85, 105);
                    } else if (getModel().isPressed()) {
                        fill = stroke;
                        textCol = Color.WHITE;
                    } else if (getModel().isRollover()) {
                        fill = new Color(
                                Math.min(255, bg.getRed() + 25),
                                Math.min(255, bg.getGreen() + 25),
                                Math.min(255, bg.getBlue() + 25)
                        );
                    }

                    canvas.setColor(fill);
                    canvas.fillRoundRect(1, 1, getWidth() - 2, getHeight() - 2, getHeight(), getHeight());
                    canvas.setColor(stroke);
                    canvas.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, getHeight(), getHeight());

                    canvas.setFont(getFont());
                    canvas.setColor(textCol);
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
        int w = button.getFontMetrics(button.getFont()).stringWidth(text) + 20;
        button.setPreferredSize(new Dimension(Math.max(80, w), 32));
        button.setMinimumSize(button.getPreferredSize());
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    /**
     * Creates a pill-shaped badge component for status cues with subtle glowing border.
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
                    canvas.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, getHeight(), getHeight());
                    canvas.setColor(new Color(fg.getRed(), fg.getGreen(), fg.getBlue(), 120));
                    canvas.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, getHeight(), getHeight());
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
     * Custom slider UI rendering a modern dark track, filled glowing accent rail, and circular knob.
     */
    public static class ModernSliderUI extends javax.swing.plaf.basic.BasicSliderUI {
        private final Color accentColor;

        /**
         * Constructs a modern slider UI with the specified accent track color.
         *
         * @param slider target JSlider component
         * @param accentColor vibrant fill and thumb outline color
         */
        public ModernSliderUI(JSlider slider, Color accentColor) {
            super(slider);
            this.accentColor = accentColor;
        }

        @Override
        public void paintTrack(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int trackHeight = 6;
                int trackY = trackRect.y + (trackRect.height - trackHeight) / 2;

                // Dark slot track
                g2.setColor(new Color(30, 41, 59));
                g2.fillRoundRect(trackRect.x, trackY, trackRect.width, trackHeight, 6, 6);

                // Filled vibrant accent up to the thumb
                int thumbCenter = thumbRect.x + thumbRect.width / 2;
                int fillWidth = Math.max(0, thumbCenter - trackRect.x);
                g2.setColor(accentColor);
                g2.fillRoundRect(trackRect.x, trackY, fillWidth, trackHeight, 6, 6);
            } finally {
                g2.dispose();
            }
        }

        @Override
        public void paintThumb(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Outer glowing circle
                g2.setColor(accentColor);
                g2.fillOval(thumbRect.x + 1, thumbRect.y + 1, thumbRect.width - 2, thumbRect.height - 2);
                // Inner bright center
                g2.setColor(Color.WHITE);
                g2.fillOval(thumbRect.x + 4, thumbRect.y + 4, thumbRect.width - 8, thumbRect.height - 8);
            } finally {
                g2.dispose();
            }
        }

        @Override
        protected Dimension getThumbSize() {
            return new Dimension(16, 16);
        }

        @Override
        public void paintFocus(Graphics g) {
            // Suppress default dotted focus border
        }
    }

    /**
     * Configures dark styling, custom cell renderer, and borders for a JComboBox.
     *
     * @param combo target combo box
     */
    public static void styleComboBox(JComboBox<?> combo) {
        combo.setBackground(INPUT_BG);
        combo.setForeground(INK);
        combo.setFont(font(12, false));
        combo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(INPUT_BORDER, 1, true),
                BorderFactory.createEmptyBorder(2, 6, 2, 6)));
        combo.setFocusable(false);
        combo.setRenderer(new javax.swing.DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;
            @Override
            public java.awt.Component getListCellRendererComponent(javax.swing.JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                list.setBackground(INPUT_BG);
                list.setSelectionBackground(INDIGO);
                list.setSelectionForeground(Color.WHITE);
                setBackground(isSelected ? INDIGO : INPUT_BG);
                setForeground(isSelected ? Color.WHITE : INK);
                setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
                return this;
            }
        });
    }

    /**
     * Configures dark styling, text field caret colors, and borders for a JSpinner.
     *
     * @param spinner target spinner
     */
    public static void styleSpinner(JSpinner spinner) {
        spinner.setBackground(INPUT_BG);
        spinner.setForeground(INK);
        spinner.setBorder(BorderFactory.createLineBorder(INPUT_BORDER, 1, true));
        JComponent editor = spinner.getEditor();
        if (editor instanceof JSpinner.DefaultEditor defaultEditor) {
            defaultEditor.getTextField().setBackground(INPUT_BG);
            defaultEditor.getTextField().setForeground(INK);
            defaultEditor.getTextField().setCaretColor(CYAN);
            defaultEditor.getTextField().setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        }
    }

    /**
     * Configures transparent dark styling for a JCheckBox.
     *
     * @param checkBox target checkbox
     */
    public static void styleCheckBox(JCheckBox checkBox) {
        checkBox.setBackground(CARD_BACKGROUND);
        checkBox.setForeground(INK_SECONDARY);
        checkBox.setFont(font(12, false));
        checkBox.setFocusPainted(false);
        checkBox.setOpaque(false);
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
                g.setColor(new Color(11, 17, 30));
                g.fillRoundRect(0, 0, size, size, Math.max(4, size / 4), Math.max(4, size / 4));

                g.setColor(new Color(30, 41, 59));
                g.setStroke(new BasicStroke(Math.max(1f, size / 16f)));
                g.drawLine(0, size / 2, size, size / 2);
                g.drawLine(size / 2, 0, size / 2, size / 2);

                g.setColor(new Color(56, 189, 248, 120));
                g.drawOval(size / 6, size / 6, 2 * size / 3, 2 * size / 3);

                g.setColor(new Color(244, 63, 94));
                g.setStroke(new BasicStroke(Math.max(1.5f, size / 10f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int[] xPoints = {size / 6, size / 3, size / 2, 3 * size / 5, 5 * size / 6};
                int[] yPoints = {size / 8, 3 * size / 8, 2 * size / 5, 7 * size / 10, 7 * size / 8};
                g.drawPolyline(xPoints, yPoints, xPoints.length);
            } finally {
                g.dispose();
            }
            icons.add(image);
        }
        return icons;
    }
}
