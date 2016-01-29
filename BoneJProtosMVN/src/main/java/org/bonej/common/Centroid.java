package org.bonej.common;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;

import sc.fiji.analyzeSkeleton.Point;

import javax.annotation.Nullable;

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
	 * Calculates the geometric center of all the given points
	 *
	 * @param points
	 *            A list of points in 3D
	 * @throws NullPointerException
	 *             if points == null
	 * @return An array of {x,y,z} coordinates of the centroid point Returns
	 *         {NaN, NaN, NaN} if points is empty
	 */
	public static double[] getCentroid(ArrayList<Point> points) {
		checkNotNull(points, "List of points is null");

		if (points.isEmpty()) {
			return new double[]{Double.NaN, Double.NaN, Double.NaN};
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
		return centroid;
	}

    @Nullable
    public static Point getCentroidPoint(ArrayList<Point> points) {
        if (points == null || points.isEmpty()) {
            return null;
        }

        double xSum = 0.0;
        double ySum = 0.0;
        double zSum = 0.0;

        for (Point p : points) {
            xSum += p.x;
            ySum += p.y;
            zSum += p.z;
        }

        double n = points.size();
        int x = (int) Math.round(xSum / n);
        int y = (int) Math.round(ySum / n);
        int z = (int) Math.round(zSum / n);
        return new Point(x, y, z);
    }
}
