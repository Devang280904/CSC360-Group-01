package org.wallcrack.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.metal.MetalLookAndFeel;

/** Consistent desktop colors, readable typography, and controls across operating systems. */
public final class DesktopTheme {
    static final Color BACKGROUND = new Color(243, 246, 250);
    static final Color INK = new Color(27, 44, 66);
    static final Color MUTED = new Color(100, 115, 136);
    static final Color LINE = new Color(222, 229, 238);
    private static final Color BLUE = new Color(42, 91, 215);

    private DesktopTheme() { }

    /** Installs predictable Swing styling before constructing the desktop window. */
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
            if (defaults.get(key) instanceof FontUIResource) defaults.put(key, base);
        }
        UIManager.put("Panel.background", BACKGROUND);
        UIManager.put("Label.foreground", INK);
        UIManager.put("TextField.background", Color.WHITE);
        UIManager.put("TextField.foreground", INK);
        UIManager.put("TabbedPane.selected", Color.WHITE);
        UIManager.put("TabbedPane.background", BACKGROUND);
        UIManager.put("TabbedPane.contentAreaColor", Color.WHITE);
        UIManager.put("TabbedPane.contentBorderInsets", new java.awt.Insets(0, 0, 0, 0));
        UIManager.put("SplitPane.background", BACKGROUND);
        UIManager.put("SplitPaneDivider.border", javax.swing.BorderFactory.createEmptyBorder());
        UIManager.put("ScrollBar.width", 10);
    }

    static Font font(int size, boolean bold) {
        return new Font(Font.SANS_SERIF, bold ? Font.BOLD : Font.PLAIN, size);
    }

    static JLabel label(String text, int size, boolean bold, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(font(size, bold));
        label.setForeground(color);
        return label;
    }

    static JButton button(String text, boolean primary) {
        JButton button = new JButton(text) {
            private static final long serialVersionUID = 1L;

            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D canvas = (Graphics2D) graphics.create();
                try {
                    canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    Color background = primary ? BLUE : Color.WHITE;
                    if (!isEnabled()) background = primary ? new Color(219, 226, 239) : new Color(239, 242, 247);
                    else if (getModel().isPressed()) background = primary ? BLUE.darker() : new Color(226, 233, 245);
                    else if (getModel().isRollover()) background = primary ? new Color(34, 77, 187) : new Color(246, 249, 255);
                    canvas.setColor(background);
                    canvas.fillRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 10, 10);
                    canvas.setColor(primary ? background : LINE);
                    canvas.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 10, 10);
                    if (isFocusOwner()) {
                        canvas.setColor(new Color(111, 153, 248));
                        canvas.setStroke(new BasicStroke(2));
                        canvas.drawRoundRect(2, 2, getWidth() - 5, getHeight() - 5, 10, 10);
                    }
                    canvas.setFont(getFont());
                    canvas.setColor(!isEnabled() ? new Color(125, 138, 158) : primary ? Color.WHITE : INK);
                    var metrics = canvas.getFontMetrics();
                    canvas.drawString(getText(), (getWidth() - metrics.stringWidth(getText())) / 2,
                            (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent());
                } finally { canvas.dispose(); }
            }
        };
        button.setFont(font(13, true));
        button.setPreferredSize(new Dimension(button.getFontMetrics(button.getFont()).stringWidth(text) + 36, 42));
        button.setMinimumSize(button.getPreferredSize());
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }
}
