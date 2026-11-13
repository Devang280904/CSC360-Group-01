package org.wallcrack.app;

import java.awt.GraphicsEnvironment;
import javax.swing.SwingUtilities;
import org.wallcrack.cli.CommandLine;
import org.wallcrack.ui.DetectorFrame;
import org.wallcrack.ui.DesktopTheme;

/** Launches the wall crack detector application. */
public final class Main {
    private Main() { }

    /**
     * Opens the desktop with no arguments or {@code --gui}; otherwise runs the CLI.
     * @param args command-line options
     */
    public static void main(String[] args) {
        if (args.length == 0 || (args.length == 1 && args[0].equals("--gui"))) {
            if (GraphicsEnvironment.isHeadless()) {
                System.err.println("A graphical display is unavailable. Use --input PHOTO for command-line analysis.");
                System.exit(2);
                return;
            }
            SwingUtilities.invokeLater(() -> {
                DesktopTheme.install();
                new DetectorFrame().setVisible(true);
            });
        } else {
            // CLI image rendering must work even when DISPLAY is unset or unreachable.
            System.setProperty("java.awt.headless", "true");
            System.exit(CommandLine.run(args, System.out, System.err));
        }
    }
}
