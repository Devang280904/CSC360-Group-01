package org.wallcrack.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.swing.JPanel;

/** Aspect-ratio-preserving image preview that fits its available desktop space. */
public final class ImagePanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private transient BufferedImage image;
    private final String placeholder;

    /** @param placeholder text displayed before an image is available */
    public ImagePanel(String placeholder) {
        this.placeholder = placeholder;
        setBackground(new Color(235, 239, 244));
        setPreferredSize(new Dimension(420, 340));
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
                canvas.setColor(new Color(85, 99, 116));
                int textWidth = canvas.getFontMetrics().stringWidth(placeholder);
                canvas.drawString(placeholder, Math.max(10, (getWidth() - textWidth) / 2), getHeight() / 2);
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
}
