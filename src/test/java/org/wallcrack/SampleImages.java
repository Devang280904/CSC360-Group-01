package org.wallcrack;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;
import javax.imageio.ImageIO;

/** Reproducible, synthetic fixtures; these are illustrations rather than real wall photographs. */
public final class SampleImages {
    private SampleImages() { }

    /** @return a uniformly lit, unmarked surface */
    public static BufferedImage blank() { return solid(320, 240, 230); }

    /** @return an unambiguous dark vertical line on a light surface */
    public static BufferedImage verticalCrack() {
        BufferedImage image = blank();
        for (int y = 35; y < 205; y++) {
            for (int x = 158; x <= 160; x++) image.setRGB(x, y, 0x252525);
        }
        return image;
    }

    /**
     * Creates a grayscale surface.
     * @param width width in pixels
     * @param height height in pixels
     * @param gray intensity from 0 to 255
     * @return new RGB image
     */
    public static BufferedImage solid(int width, int height, int gray) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        int rgb = (gray << 16) | (gray << 8) | gray;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) image.setRGB(x, y, rgb);
        }
        return image;
    }

    /**
     * Writes deterministic demonstration files into the requested directory.
     * @param args optional output directory, defaulting to src/main/resources/samples
     * @throws Exception when a sample cannot be written
     */
    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        Path directory = Path.of(args.length == 0 ? "src/main/resources/samples" : args[0]);
        Files.createDirectories(directory);
        BufferedImage clean = solid(800, 600, 230);
        Random random = new Random(360);
        for (int y = 0; y < clean.getHeight(); y++) {
            for (int x = 0; x < clean.getWidth(); x++) {
                int gray = 217 + x * 15 / clean.getWidth() + random.nextInt(7);
                clean.setRGB(x, y, (gray << 16) | (gray << 8) | gray);
            }
        }
        ImageIO.write(clean, "png", directory.resolve("wall-clean.png").toFile());
        var graphics = clean.createGraphics();
        try {
            graphics.setColor(new Color(45, 43, 42));
            graphics.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            graphics.drawPolyline(new int[]{390, 382, 401, 395, 418, 408, 430, 423},
                    new int[]{60, 120, 182, 246, 315, 378, 458, 540}, 8);
            graphics.setStroke(new BasicStroke(2f));
            graphics.drawPolyline(new int[]{395, 350, 336, 301, 288}, new int[]{246, 270, 310, 335, 381}, 5);
            graphics.drawPolyline(new int[]{408, 452, 469, 493}, new int[]{378, 360, 324, 310}, 4);
        } finally {
            graphics.dispose();
        }
        ImageIO.write(clean, "png", directory.resolve("wall-cracked.png").toFile());
        System.out.println("Synthetic samples written to " + directory.toAbsolutePath());
    }
}
