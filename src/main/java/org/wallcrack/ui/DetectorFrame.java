package org.wallcrack.ui;

import java.awt.Dimension;
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
        setMinimumSize(new Dimension(1100, 740));
        setSize(1380, 900);
        setContentPane(new DetectorWorkspace());
        setLocationRelativeTo(null);
    }
}
