package org.wallcrack.app;

import org.wallcrack.cli.CommandLine;

/** Launches the wall crack detector application. */
public final class Main {
    private Main() { }

    /**
     * Runs command-line analysis or displays usage instructions.
     * @param args command-line options
     */
    public static void main(String[] args) {
        System.exit(CommandLine.run(args, System.out, System.err));
    }
}
