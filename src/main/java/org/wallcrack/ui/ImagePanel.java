package org.wallcrack.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import javax.swing.JPanel;
import org.wallcrack.model.CrackRegion;

/**
 * Aspect-ratio-preserving interactive image preview supporting smooth zooming, panning,
 * pixel-level inspection, and candidate region highlighting.
 *
 * @serial exclude
 */
public final class ImagePanel extends JPanel {
    private static final long serialVersionUID = 1L;

    /**
     * Pixel inspection payload containing image coordinate and color values.
     *
     * @param x horizontal coordinate in image pixels
     * @param y vertical coordinate in image pixels
     * @param red 8-bit red component (0–255)
     * @param green 8-bit green component (0–255)
     * @param blue 8-bit blue component (0–255)
     * @param luminance standard CCIR-601 perceptual luminance value (0–255)
     */
    public record PixelInfo(int x, int y, int red, int green, int blue, int luminance) { }

    private transient BufferedImage image;
    private final String placeholder;
    private double zoomScale = 1.0;
    private double panX = 0.0;
    private double panY = 0.0;
    private boolean autoFit = true;
    private transient Point dragOrigin;
    private transient List<CrackRegion> regions = List.of();
    private int focusedRegionId = -1;
    private transient Consumer<Integer> zoomChangeListener;
    private transient Consumer<PixelInfo> pixelHoverListener;
    private transient Consumer<CrackRegion> regionClickListener;

    /**
     * Creates an initially empty image preview with interactive zoom and pan support.
     *
     * @param placeholder text displayed before an image is available
     */
    public ImagePanel(String placeholder) {
        this.placeholder = Objects.requireNonNull(placeholder, "placeholder");
        setBackground(new Color(241, 245, 249));
        setFont(DesktopTheme.font(12, false));
        setPreferredSize(new Dimension(420, 340));
        setMinimumSize(new Dimension(180, 150));
        getAccessibleContext().setAccessibleName(placeholder);

        MouseHandler handler = new MouseHandler();
        addMouseListener(handler);
        addMouseMotionListener(handler);
        addMouseWheelListener(handler);
    }

    /**
     * Updates the preview image; call only on the Swing event dispatch thread.
     *
     * @param image new image, or null to show the placeholder
     */
    public void setImage(BufferedImage image) {
        this.image = image;
        this.autoFit = true;
        this.panX = 0;
        this.panY = 0;
        notifyZoomChange();
        repaint();
    }

    /**
     * Associates crack candidate regions for interactive overlays and hit-testing.
     *
     * @param regions list of candidate regions
     */
    public void setRegions(List<CrackRegion> regions) {
        this.regions = regions == null ? List.of() : List.copyOf(regions);
        repaint();
    }

    /**
     * Highlights a specific candidate region by identifier.
     *
     * @param id candidate region identifier, or -1 to clear highlighting
     */
    public void setFocusedRegionId(int id) {
        if (this.focusedRegionId != id) {
            this.focusedRegionId = id;
            repaint();
        }
    }

    /**
     * Sets a callback notified whenever the active zoom percentage changes.
     *
     * @param listener listener accepting integer percentage, or null to clear
     */
    public void setZoomChangeListener(Consumer<Integer> listener) {
        this.zoomChangeListener = listener;
        notifyZoomChange();
    }

    /**
     * Sets a callback notified with image pixel coordinates and RGB values under the cursor.
     *
     * @param listener listener accepting pixel information, or null to clear
     */
    public void setPixelHoverListener(Consumer<PixelInfo> listener) {
        this.pixelHoverListener = listener;
    }

    /**
     * Sets a callback triggered when the user clicks on an outlined crack region in the image.
     *
     * @param listener listener accepting the clicked crack region, or null to clear
     */
    public void setRegionClickListener(Consumer<CrackRegion> listener) {
        this.regionClickListener = listener;
    }

    /**
     * Increases magnification by 25%.
     */
    public void zoomIn() {
        if (image == null) return;
        applyZoom(getZoomScale() * 1.25, getWidth() / 2.0, getHeight() / 2.0);
    }

    /**
     * Decreases magnification by 20%.
     */
    public void zoomOut() {
        if (image == null) return;
        applyZoom(getZoomScale() / 1.25, getWidth() / 2.0, getHeight() / 2.0);
    }

    /**
     * Resets viewport magnification to fit the available desktop space.
     */
    public void resetZoom() {
        this.autoFit = true;
        this.panX = 0;
        this.panY = 0;
        notifyZoomChange();
        repaint();
    }

    /**
     * Sets magnification to 100% 1:1 pixel rendering.
     */
    public void actualSize() {
        if (image == null) return;
        this.autoFit = false;
        this.zoomScale = 1.0;
        this.panX = (getWidth() - image.getWidth()) / 2.0;
        this.panY = (getHeight() - image.getHeight()) / 2.0;
        notifyZoomChange();
        repaint();
    }

    /**
     * Centers the viewport magnification smoothly on a selected candidate region.
     *
     * @param region crack region to inspect
     */
    public void centerOnRegion(CrackRegion region) {
        if (image == null || region == null) return;
        this.autoFit = false;
        double targetScale = Math.max(1.5, Math.min(6.0,
                Math.min(getWidth() / (double) Math.max(30, region.width() * 2),
                         getHeight() / (double) Math.max(30, region.height() * 2))));
        this.zoomScale = targetScale;
        double centerX = region.x() + region.width() / 2.0;
        double centerY = region.y() + region.height() / 2.0;
        this.panX = getWidth() / 2.0 - centerX * zoomScale;
        this.panY = getHeight() / 2.0 - centerY * zoomScale;
        this.focusedRegionId = region.id();
        notifyZoomChange();
        repaint();
    }

    /**
     * Returns the active magnification as an integer percentage.
     *
     * @return zoom percentage (100 = 1:1 pixel size)
     */
    public int getZoomPercentage() {
        return (int) Math.round(getZoomScale() * 100.0);
    }

    private double getZoomScale() {
        if (autoFit && image != null && getWidth() > 0 && getHeight() > 0) {
            double fitW = (double) Math.max(1, getWidth() - 20) / image.getWidth();
            double fitH = (double) Math.max(1, getHeight() - 20) / image.getHeight();
            return Math.min(fitW, fitH);
        }
        return zoomScale;
    }

    private void applyZoom(double targetScale, double pivotX, double pivotY) {
        double clamped = Math.max(0.05, Math.min(20.0, targetScale));
        double currentScale = getZoomScale();
        if (autoFit) {
            autoFit = false;
            double imageW = image.getWidth() * currentScale;
            double imageH = image.getHeight() * currentScale;
            panX = (getWidth() - imageW) / 2.0;
            panY = (getHeight() - imageH) / 2.0;
        }
        double factor = clamped / currentScale;
        panX = pivotX - (pivotX - panX) * factor;
        panY = pivotY - (pivotY - panY) * factor;
        zoomScale = clamped;
        notifyZoomChange();
        repaint();
    }

    private void notifyZoomChange() {
        if (zoomChangeListener != null) {
            zoomChangeListener.accept(getZoomPercentage());
        }
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D canvas = (Graphics2D) graphics.create();
        try {
            canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (image == null) {
                paintPlaceholder(canvas);
                return;
            }

            double scale = getZoomScale();
            double drawX;
            double drawY;
            if (autoFit) {
                int drawW = Math.max(1, (int) Math.round(image.getWidth() * scale));
                int drawH = Math.max(1, (int) Math.round(image.getHeight() * scale));
                drawX = (getWidth() - drawW) / 2.0;
                drawY = (getHeight() - drawH) / 2.0;
            } else {
                drawX = panX;
                drawY = panY;
            }

            int w = Math.max(1, (int) Math.round(image.getWidth() * scale));
            int h = Math.max(1, (int) Math.round(image.getHeight() * scale));

            canvas.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    scale < 1.0 ? RenderingHints.VALUE_INTERPOLATION_BILINEAR : RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            canvas.drawImage(image, (int) Math.round(drawX), (int) Math.round(drawY), w, h, null);

            // Highlight focused candidate region
            if (focusedRegionId > 0 && !regions.isEmpty()) {
                for (CrackRegion region : regions) {
                    if (region.id() == focusedRegionId) {
                        paintFocusedRegion(canvas, region, drawX, drawY, scale);
                        break;
                    }
                }
            }
        } finally {
            canvas.dispose();
        }
    }

    private void paintFocusedRegion(Graphics2D canvas, CrackRegion region, double originX, double originY, double scale) {
        int rx = (int) Math.round(originX + region.x() * scale);
        int ry = (int) Math.round(originY + region.y() * scale);
        int rw = Math.max(4, (int) Math.round(region.width() * scale));
        int rh = Math.max(4, (int) Math.round(region.height() * scale));

        // Pulsing amber/cyan highlight box
        canvas.setColor(new Color(245, 158, 11, 45));
        canvas.fillRect(rx - 2, ry - 2, rw + 4, rh + 4);
        canvas.setColor(new Color(245, 158, 11));
        canvas.setStroke(new BasicStroke(2.5f));
        canvas.drawRect(rx - 2, ry - 2, rw + 4, rh + 4);

        // Region identifier pill badge
        String tag = "Region #" + region.id() + " (" + region.pixelCount() + " px)";
        canvas.setFont(DesktopTheme.font(11, true));
        var metrics = canvas.getFontMetrics();
        int tagW = metrics.stringWidth(tag) + 12;
        int tagH = 18;
        int tagX = rx;
        int tagY = Math.max(4, ry - tagH - 4);
        canvas.setColor(new Color(15, 23, 42, 220));
        canvas.fillRoundRect(tagX, tagY, tagW, tagH, 6, 6);
        canvas.setColor(new Color(245, 158, 11));
        canvas.drawString(tag, tagX + 6, tagY + 13);
    }

    private void paintPlaceholder(Graphics2D canvas) {
        int center = getWidth() / 2;
        int iconTop = Math.max(20, getHeight() / 2 - 50);

        // Rounded camera/inspection illustration
        canvas.setColor(new Color(226, 232, 240));
        canvas.fillRoundRect(center - 28, iconTop, 56, 44, 12, 12);
        canvas.setColor(new Color(148, 163, 184));
        canvas.setStroke(new BasicStroke(1.5f));
        canvas.drawRoundRect(center - 28, iconTop, 56, 44, 12, 12);
        canvas.drawOval(center - 12, iconTop + 10, 24, 24);
        canvas.fillOval(center + 14, iconTop + 6, 6, 6);

        canvas.setColor(DesktopTheme.MUTED);
        canvas.setFont(getFont());
        List<String> lines = wrap(placeholder, canvas, Math.max(80, getWidth() - 44));
        int baseline = iconTop + 66;
        for (String line : lines) {
            int textWidth = canvas.getFontMetrics().stringWidth(line);
            canvas.drawString(line, Math.max(10, (getWidth() - textWidth) / 2), baseline);
            baseline += 18;
        }

        // Drag & drop hint
        canvas.setColor(new Color(148, 163, 184));
        canvas.setFont(DesktopTheme.font(11, false));
        String dropHint = "Or drag & drop an image file here";
        int dropWidth = canvas.getFontMetrics().stringWidth(dropHint);
        canvas.drawString(dropHint, Math.max(10, (getWidth() - dropWidth) / 2), baseline + 4);
    }

    private static List<String> wrap(String text, Graphics2D canvas, int width) {
        List<String> lines = new ArrayList<>();
        String line = "";
        for (String word : text.split(" ")) {
            String next = line.isEmpty() ? word : line + " " + word;
            if (!line.isEmpty() && canvas.getFontMetrics().stringWidth(next) > width) {
                lines.add(line);
                line = word;
            } else {
                line = next;
            }
        }
        if (!line.isEmpty()) lines.add(line);
        return lines;
    }

    private class MouseHandler extends MouseAdapter {
        @Override
        public void mousePressed(MouseEvent event) {
            if (image == null) return;
            if (event.getButton() == MouseEvent.BUTTON1 || event.getButton() == MouseEvent.BUTTON2) {
                dragOrigin = event.getPoint();
                setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
            }
        }

        @Override
        public void mouseReleased(MouseEvent event) {
            dragOrigin = null;
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }

        @Override
        public void mouseDragged(MouseEvent event) {
            if (image == null || dragOrigin == null) return;
            if (autoFit) {
                double scale = getZoomScale();
                autoFit = false;
                panX = (getWidth() - image.getWidth() * scale) / 2.0;
                panY = (getHeight() - image.getHeight() * scale) / 2.0;
                zoomScale = scale;
            }
            panX += event.getX() - dragOrigin.x;
            panY += event.getY() - dragOrigin.y;
            dragOrigin = event.getPoint();
            notifyZoomChange();
            repaint();
        }

        @Override
        public void mouseWheelMoved(MouseWheelEvent event) {
            if (image == null) return;
            double factor = event.getWheelRotation() < 0 ? 1.15 : 1.0 / 1.15;
            applyZoom(getZoomScale() * factor, event.getX(), event.getY());
        }

        @Override
        public void mouseMoved(MouseEvent event) {
            if (image == null) return;
            Point2D imgPoint = toImagePoint(event.getPoint());
            int ix = (int) Math.floor(imgPoint.getX());
            int iy = (int) Math.floor(imgPoint.getY());

            if (ix >= 0 && ix < image.getWidth() && iy >= 0 && iy < image.getHeight()) {
                int rgb = image.getRGB(ix, iy);
                int r = (rgb >> 16) & 255;
                int g = (rgb >> 8) & 255;
                int b = rgb & 255;
                int luminance = (299 * r + 587 * g + 114 * b + 500) / 1000;
                if (pixelHoverListener != null) {
                    pixelHoverListener.accept(new PixelInfo(ix, iy, r, g, b, luminance));
                }
            } else if (pixelHoverListener != null) {
                pixelHoverListener.accept(null);
            }
        }

        @Override
        public void mouseClicked(MouseEvent event) {
            if (image == null) return;
            if (event.getClickCount() == 2) {
                resetZoom();
                return;
            }

            if (regionClickListener != null && !regions.isEmpty()) {
                Point2D imgPoint = toImagePoint(event.getPoint());
                int ix = (int) Math.floor(imgPoint.getX());
                int iy = (int) Math.floor(imgPoint.getY());
                for (CrackRegion region : regions) {
                    if (ix >= region.x() && ix < region.x() + region.width()
                            && iy >= region.y() && iy < region.y() + region.height()) {
                        regionClickListener.accept(region);
                        break;
                    }
                }
            }
        }

        /**
         * Transforms screen viewport coordinates back into original image pixel coordinates
         * by applying the inverse of the current pan and zoom AffineTransform.
         */
        private Point2D toImagePoint(Point screenPoint) {
            double scale = getZoomScale();
            double ox = autoFit ? (getWidth() - image.getWidth() * scale) / 2.0 : panX;
            double oy = autoFit ? (getHeight() - image.getHeight() * scale) / 2.0 : panY;
            AffineTransform transform = new AffineTransform();
            transform.translate(ox, oy);
            transform.scale(scale, scale);
            try {
                return transform.inverseTransform(screenPoint, null);
            } catch (NoninvertibleTransformException e) {
                return new Point2D.Double(-1, -1);
            }
        }
    }
}
