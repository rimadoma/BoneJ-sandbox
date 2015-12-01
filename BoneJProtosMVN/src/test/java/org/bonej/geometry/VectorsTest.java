package org.bonej.geometry;

import static org.junit.Assert.*;
import org.junit.Test;

import javax.vecmath.Vector3d;

/**
 *  A class to test that javax.vecmath.Vector3d and bonej.geometry.Vectors
 *  calculate similar results. The rationale for these tests is to
 *  ensure that the results BoneJ returns won't change numerically
 *  (precision, rounding errors) etc.
 *
 * @author <a href="mailto:rdomander@rvc.ac.uk">Richard Domander</a>
 */
public class VectorsTest {
    double x1 = 5;
    double y1 = 3;
    double z1 = 1;

    double x2 = 8;
    double y2 = 6;
    double z2 = 4;

    Vector3d u = new Vector3d(x1, y1, z1);
    Vector3d v = new Vector3d(x2, y2, z2);

    final double EPSILON = 1e-12;

    @Test
    public void testCrossProduct()
    {
        double[] expected = { 6, -12, 6 };

        double[] result = Vectors.crossProduct(x1, y1, z1, x2, y2, z2);

        assertEquals(result.length, expected.length);
        assertArrayEquals(expected, result, EPSILON);

        Vector3d w = new Vector3d();
        double vector3dResult [] = new double[3];

        w.cross(u, v);
        w.get(vector3dResult);

        assertArrayEquals(expected, vector3dResult, EPSILON);
    }

    @Test
    public void testNorm()
    {
        Vector3d w = new Vector3d();
        w.normalize(v);

        Vector3d expected = new Vector3d(v);
        expected.normalize();

        assertEquals(expected.epsilonEquals(w, EPSILON), true);
    }

    @Test
    public void testJoinedVectorAngle() throws Exception {
        final Vector3d originalU = new Vector3d(1.0, 2.0, 3.0);
        final Vector3d originalV = new Vector3d(4.0, 5.0, 6.0);
        final Vector3d originalTail = new Vector3d(7.0, 8.0, 9.0);
        final Vector3d origin = new Vector3d(0.0, 0.0, 0.0);
        final double DELTA = 1e-12;

        Vector3d u = new Vector3d(originalU);
        Vector3d v = new Vector3d(originalV);
        Vector3d tail = new Vector3d(originalTail);

        double result = Vectors.joinedVectorAngle(u, v, tail);
        assertEquals("Vector angle was calculated wrong", 0.0, result, DELTA);
        assertEquals("Vector parameter must not change", true, originalU.equals(u));
        assertEquals("Vector parameter must not change", true, originalV.equals(v));
        assertEquals("Vector parameter must not change", true, originalTail.equals(tail));

        result  = Vectors.joinedVectorAngle(u, v, origin);
        double expected = Math.acos(32.0 / (Math.sqrt(14.0) * Math.sqrt(77.0)));
        assertEquals("Vector angle was calculated wrong", expected, result, DELTA);
    }
}