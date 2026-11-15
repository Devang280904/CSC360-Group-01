package org.wallcrack;

import org.junit.jupiter.api.Test;

/** Standard JUnit entry point for the dependency-free scenario suite. */
final class AnalysisTest {
    @Test
    void completeRegressionSuite() throws Exception {
        AnalysisTestSuite.main(new String[0]);
    }
}
