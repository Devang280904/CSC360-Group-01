package org.wallcrack.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.Callable;
import javax.imageio.ImageIO;
import javax.swing.JSpinner;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.Timeout;
import org.wallcrack.SampleImages;

/** Headless integration checks for desktop state transitions and result navigation. */
final class DetectorWorkspaceTest {
    @BeforeAll
    static void installTheme() {
        DesktopTheme.install();
    }

    @Test
    @Timeout(15)
    void loadAnalyzeNavigateAndInvalidate(@TempDir Path temporary) throws Exception {
        Path first = temporary.resolve("first.png");
        Path ignored = temporary.resolve("ignored.png");
        ImageIO.write(SampleImages.verticalCrack(), "png", first.toFile());
        ImageIO.write(SampleImages.blank(), "png", ignored.toFile());

        DetectorWorkspace workspace = onEdt(DetectorWorkspace::new);
        onEdt(() -> {
            workspace.loadFile(first);
            workspace.loadFile(ignored); // guarded while the first load is active
            return null;
        });
        await(workspace::hasImage);
        assertEquals(first, field(workspace, "currentFilePath"));

        onEdt(() -> {
            workspace.loadFile(temporary.resolve("missing.png"));
            return null;
        });
        await(() -> Boolean.FALSE.equals(field(workspace, "busy")));
        assertTrue(workspace.hasImage(), "failed load must preserve the previous image");
        assertEquals(first, field(workspace, "currentFilePath"));

        onEdt(() -> {
            workspace.analyze();
            workspace.analyze(); // duplicate request is ignored
            return null;
        });
        await(workspace::hasResult);

        onEdt(() -> {
            workspace.setViewMode("Binary Mask");
            ImagePanel mask = field(workspace, "mask");
            int before = mask.getZoomPercentage();
            workspace.zoomIn();
            assertTrue(mask.getZoomPercentage() > before);
            workspace.setViewMode("not-a-view");
            JSpinner sensitivity = field(workspace, "sensitivity");
            sensitivity.setValue(1);
            return null;
        });
        assertFalse(workspace.hasResult(), "changing analysis settings must invalidate stale results");
    }

    private static void await(Callable<Boolean> condition) throws Exception {
        long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
        while (!condition.call() && System.nanoTime() < deadline) {
            Thread.sleep(20);
            onEdt(() -> null);
        }
        assertTrue(condition.call(), "asynchronous desktop operation timed out");
    }

    private static <T> T onEdt(Callable<T> action) throws Exception {
        if (SwingUtilities.isEventDispatchThread()) return action.call();
        Object[] value = new Object[1];
        Throwable[] failure = new Throwable[1];
        SwingUtilities.invokeAndWait(() -> {
            try {
                value[0] = action.call();
            } catch (Throwable thrown) {
                failure[0] = thrown;
            }
        });
        if (failure[0] != null) throw new AssertionError(failure[0]);
        @SuppressWarnings("unchecked") T cast = (T) value[0];
        return cast;
    }

    @SuppressWarnings("unchecked")
    private static <T> T field(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return (T) field.get(target);
    }
}
