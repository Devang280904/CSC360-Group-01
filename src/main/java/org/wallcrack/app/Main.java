package org.wallcrack.app;

import java.awt.GraphicsEnvironment;
import javax.swing.SwingUtilities;
import org.wallcrack.cli.CommandLine;
import org.wallcrack.ui.DetectorFrame;
import org.wallcrack.ui.DesktopTheme;

/**
 * Application entry point for Wall Crack Detector.
 *
 * <p>Execution modes:
 * <ul>
 *   <li><b>Desktop GUI mode</b>: Invoked with no arguments or {@code --gui}. Checks for a graphical
 *       display, installs the unified desktop theme, and opens the Swing workspace window.</li>
 *   <li><b>Command-line mode</b>: Invoked with {@code --input PHOTO [options]}. Forces headless AWT
 *       operation so rendering and export work in headless CI and server environments without an X11/Wayland display.</li>
 * </ul>
 */
public final class Main {
    private Main() { }

    /**
     * Application entry point.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        if (args.length == 0 || (args.length == 1 && args[0].equals("--gui"))) {
            // Check whether a display device is accessible for Swing GUI
            if (GraphicsEnvironment.isHeadless()) {
                System.err.println("A graphical display is unavailable. Use --input PHOTO for command-line analysis.");
                System.exit(2);
                return;
            }

            // Launch Swing UI safely on the Event Dispatch Thread (EDT)
            SwingUtilities.invokeLater(() -> {
                DesktopTheme.install();
                new DetectorFrame().setVisible(true);
            });
        } else {
            // Explicitly enable headless mode for CLI so operations work on headless servers
            System.setProperty("java.awt.headless", "true");
            System.exit(CommandLine.run(args, System.out, System.err));
        }
    }
}
