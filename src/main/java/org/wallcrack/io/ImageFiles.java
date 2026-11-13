package org.wallcrack.io;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

/** Image decoding with format validation and a pre-decode pixel limit. */
public final class ImageFiles {
    /** Maximum decoded input size; larger photos must be resized before loading. */
    public static final long MAX_INPUT_PIXELS = 40_000_000L;

    private ImageFiles() { }

    /**
     * Reads the first frame of an image supported by the Java ImageIO runtime.
     * @param path input file, normally PNG or JPEG
     * @return decoded image
     * @throws IOException if the file is missing, unsupported, corrupt, or too large
     */
    public static BufferedImage read(Path path) throws IOException {
        if (!Files.isRegularFile(path)) throw new IOException("Image file does not exist: " + path);
        try (ImageInputStream input = ImageIO.createImageInputStream(path.toFile())) {
            if (input == null) throw new IOException("Cannot open image: " + path);
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new IOException("Unsupported image. Choose a PNG or JPEG photograph.");
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width < 1 || height < 1 || (long) width * height > MAX_INPUT_PIXELS) {
                    throw new IOException("Image exceeds the 40 megapixel limit. Resize it before loading.");
                }
                BufferedImage image = reader.read(0);
                if (image == null) throw new IOException("The image could not be decoded.");
                return image;
            } finally {
                reader.dispose();
            }
        }
    }
}
