package org.wallcrack.model;

/**
 * One connected candidate region, with coordinates in the analysis image.
 *
 * @param id one-based candidate identifier
 * @param x left edge of the bounding box in analysis pixels
 * @param y top edge of the bounding box in analysis pixels
 * @param width bounding-box width including both edge pixels
 * @param height bounding-box height including both edge pixels
 * @param pixelCount number of retained candidate pixels
 * @param span bounding-box diagonal in analysis pixels; not a measured physical crack length
 * @param circularity ImageJ circularity metric: 4 * pi * (area / perimeter^2)
 */
public record CrackRegion(int id, int x, int y, int width, int height,
                          int pixelCount, double span, double circularity) { }
