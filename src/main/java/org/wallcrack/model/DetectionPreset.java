package org.wallcrack.model;

import java.util.List;
import java.util.Objects;

/**
 * Predefined parameter configurations tailored to common wall surface and inspection scenarios.
 *
 * @param name human-readable preset label
 * @param description technical summary of intended surface conditions
 * @param config associated detection configuration
 */
public record DetectionPreset(String name, String description, DetectionConfig config) {
    /**
     * Creates a named preset with validated configuration.
     *
     * @param name human-readable preset label
     * @param description technical summary of intended surface conditions
     * @param config associated detection configuration
     */
    public DetectionPreset {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(config, "config");
    }

    /**
     * General balanced configuration suitable for typical painted plaster, concrete, or drywall surfaces.
     */
    public static final DetectionPreset BALANCED = new DetectionPreset(
            "Balanced (Default)",
            "Standard sensitivity for typical painted plaster, concrete, and drywall surfaces.",
            DetectionConfig.defaults());

    /**
     * High sensitivity profile for faint, narrow hairline fractures.
     */
    public static final DetectionPreset HAIRLINE = new DetectionPreset(
            "Hairline & Fine Cracks",
            "Lower contrast threshold and smaller region filters for detecting faint, narrow cracks.",
            new DetectionConfig(8, 8, 15, 10, 1600));

    /**
     * Strict filtering profile isolating prominent, wide, or severe structural cracks.
     */
    public static final DetectionPreset SEVERE = new DetectionPreset(
            "Prominent & Severe Cracks",
            "Higher threshold and larger minimum span to isolate major structural fractures.",
            new DetectionConfig(22, 16, 80, 30, 1600));

    /**
     * Noise-resistant profile for rough stucco, coarse brick, or unevenly lit walls.
     */
    public static final DetectionPreset TEXTURED = new DetectionPreset(
            "Textured & Rough Wall",
            "Wider neighborhood radius and higher area floor to filter out surface grain and stucco texture.",
            new DetectionConfig(18, 20, 50, 25, 1600));

    /**
     * High resolution profile processing images up to 2000 pixels maximum dimension.
     */
    public static final DetectionPreset HIGH_RES = new DetectionPreset(
            "High-Resolution Detail",
            "Expanded 2000px resolution budget for detailed high-resolution photographs.",
            new DetectionConfig(14, 12, 40, 20, 2000));

    /**
     * Returns the built-in preset configurations.
     *
     * @return immutable list of predefined detection presets
     */
    public static List<DetectionPreset> builtIns() {
        return List.of(BALANCED, HAIRLINE, SEVERE, TEXTURED, HIGH_RES);
    }

    @Override
    public String toString() {
        return name;
    }
}
