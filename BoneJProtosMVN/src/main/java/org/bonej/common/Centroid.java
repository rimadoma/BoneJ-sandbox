package org.bonej.common;

import sc.fiji.analyzeSkeleton.Point;

import java.util.ArrayList;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author  Michael Doube
 * @author  Richard Domander
 */
public class Centroid
{
    //Indices of the coordinates in the Centroid array
    public final static int X_INDEX = 0;
    public final static int Y_INDEX = 1;
    public final static int Z_INDEX = 2;

    /**
     * Calculates the geometric center of all the given points
     *
     * @param   points  A list of points in 3D
     * @throws  NullPointerException if points == null
     * @return  An array of {x,y,z} coordinates of the centroid point
     *          Returns {NaN, NaN, NaN} if points is empty
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

        double[] centroid = { xSum / n, ySum / n, zSum / n };
        return centroid;
    }
}
