package org.bonej.geometry;

import static org.junit.Assert.assertEquals;

import javax.vecmath.Vector3d;

import org.junit.Test;

/**
 * Unit tests for BoneJ Vectors class
 *
 * @author Richard Domander
 */
public class VectorsTest {
    @Test(expected = NullPointerException.class)
    public void testJoinedVectorAngleThrowsNullPointerExceptionIfVectorIsNull() throws Exception {
        Vectors.joinedVectorAngle(null, new Vector3d(1.0, 0.0, 0.0), new Vector3d(0.0, 1.0, 0.0));
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

        result = Vectors.joinedVectorAngle(u, v, origin);
        double expected = Math.acos(32.0 / (Math.sqrt(14.0) * Math.sqrt(77.0)));
        assertEquals("Vector angle was calculated wrong", expected, result, DELTA);
    }
}