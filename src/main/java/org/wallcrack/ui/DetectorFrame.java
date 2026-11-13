package org.wallcrack.ui;

import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import javax.swing.JFrame;

/**
 * Desktop window containing the wall photograph analysis workspace.
 * @serial exclude
 */
public final class DetectorFrame extends JFrame {
    private static final long serialVersionUID = 1L;

    /** Creates the application window on the Swing event dispatch thread. */
    public DetectorFrame() {
        super("Wall Crack Detector");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        var screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        setMinimumSize(new Dimension(Math.min(1180, screen.width), Math.min(760, screen.height)));
        setSize(Math.min(1500, screen.width), Math.min(960, screen.height));
        setContentPane(new DetectorWorkspace());
        setLocationRelativeTo(null);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
    }
}
