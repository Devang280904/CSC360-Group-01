package org.wallcrack.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;
import java.awt.image.BufferedImage;
import javax.swing.JPanel;

/**
 * Aspect-ratio-preserving image preview that fits its available desktop space.
 * @serial exclude
 */
public final class ImagePanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private transient BufferedImage image;
    private final String placeholder;

    /**
     * Creates an initially empty image preview.
     * @param placeholder text displayed before an image is available
     */
    public ImagePanel(String placeholder) {
        this.placeholder = placeholder;
        setBackground(new Color(239, 243, 248));
        setFont(DesktopTheme.font(12, false));
        setPreferredSize(new Dimension(420, 340));
        setMinimumSize(new Dimension(180, 150));
        getAccessibleContext().setAccessibleName(placeholder);
    }

    /**
     * Updates the preview; call only on the Swing event dispatch thread.
     * @param image new image, or null to show the placeholder
     */
    public void setImage(BufferedImage image) {
        this.image = image;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D canvas = (Graphics2D) graphics.create();
        try {
            if (image == null) {
                canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int center = getWidth() / 2;
                int iconTop = getHeight() / 2 - 48;
                canvas.setColor(new Color(205, 216, 232));
                canvas.fillRoundRect(center - 25, iconTop, 50, 44, 12, 12);
                canvas.setColor(new Color(145, 163, 188));
                canvas.drawRoundRect(center - 15, iconTop + 10, 30, 24, 4, 4);
                canvas.drawPolyline(new int[]{center - 12, center - 3, center + 3, center + 12},
                        new int[]{iconTop + 29, iconTop + 21, iconTop + 26, iconTop + 17}, 4);
                canvas.setColor(DesktopTheme.MUTED);
                canvas.setFont(getFont());
                List<String> lines = wrap(placeholder, canvas, Math.max(80, getWidth() - 44));
                int baseline = iconTop + 68;
                for (String line : lines) {
                    int textWidth = canvas.getFontMetrics().stringWidth(line);
                    canvas.drawString(line, Math.max(10, (getWidth() - textWidth) / 2), baseline);
                    baseline += 19;
                }
                return;
            }
            double scale = Math.min((double) Math.max(1, getWidth() - 20) / image.getWidth(),
                    (double) Math.max(1, getHeight() - 20) / image.getHeight());
            int width = Math.max(1, (int) Math.round(image.getWidth() * scale));
            int height = Math.max(1, (int) Math.round(image.getHeight() * scale));
            canvas.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            canvas.drawImage(image, (getWidth() - width) / 2, (getHeight() - height) / 2, width, height, null);
        } finally {
            canvas.dispose();
        }
    }

    private static List<String> wrap(String text, Graphics2D canvas, int width) {
        List<String> lines = new ArrayList<>();
        String line = "";
        for (String word : text.split(" ")) {
            String next = line.isEmpty() ? word : line + " " + word;
            if (!line.isEmpty() && canvas.getFontMetrics().stringWidth(next) > width) {
                lines.add(line);
                line = word;
            } else line = next;
        }
        if (!line.isEmpty()) lines.add(line);
        return lines;
    }
}
