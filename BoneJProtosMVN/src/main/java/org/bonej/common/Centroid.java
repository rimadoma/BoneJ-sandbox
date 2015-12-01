package org.bonej.common;

import sc.fiji.analyzeSkeleton.Point;

import java.util.ArrayList;

/**
 * @author Michael Doube
 * @author <a href="mailto:rdomander@rvc.ac.uk">Richard Domander</a>
 */
public class Centroid
{
    public static double[] getCentroid(ArrayList<Point> points) {
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
