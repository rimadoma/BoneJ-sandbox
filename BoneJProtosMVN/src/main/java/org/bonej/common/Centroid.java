package org.bonej.common;

import java.util.ArrayList;
import java.util.Optional;

import sc.fiji.analyzeSkeleton.Point;

/**
 * @author Michael Doube
 * @author Richard Domander
 */
public class Centroid {
	// Indices of the coordinates in the Centroid array
	public final static int X_INDEX = 0;
	public final static int Y_INDEX = 1;
	public final static int Z_INDEX = 2;

	/**
	 * Calculates the coordinates of the geometric center of the given points
	 *
	 * @param points A list of points in 3D
	 * @return  An Optional of an array containing {x,y,z}
     *          Returns an empty Optional if points == null or points.isEmpty()
	 */
	public static Optional<double[]> getCentroidCoordinates(final ArrayList<Point> points) {
        if (points == null || points.isEmpty()) {
            return Optional.empty();
        }

		double xSum = 0;
		double ySum = 0;
		double zSum = 0;
		double n = points.size();

		for (Point p : points) {
			xSum += p.x;
			ySum += p.y;
			zSum += p.z;
		}

		double[] centroid = {xSum / n, ySum / n, zSum / n};
		return Optional.of(centroid);
	}

    /**
     * Returns the centroid Point of the given points. A centroid marks the geometric centre of the points.
     *
     * @implNote The Centroid Point may not be exact because its coordinates are rounded to integers.
     * @param points A list of points in 3D
     * @return An Optional containing the centroid Point. The Optional is empty if points == null or points.isEmpty()
     */
    public static Optional<Point> getCentroidPoint(final ArrayList<Point> points) {
        Optional<double[]> optional = getCentroidCoordinates(points);
        if (!optional.isPresent()) {
            return Optional.empty();
        }

        double[] coordinates = optional.get();
        int x = (int) Math.round(coordinates[0]);
        int y = (int) Math.round(coordinates[1]);
        int z = (int) Math.round(coordinates[2]);

        return Optional.of(new Point(x, y, z));
    }
}
