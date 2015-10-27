package geometry;

import static org.junit.Assert.*;
import org.junit.Test;

import javax.vecmath.Vector3d;

public class VectorsTest {
    double x1 = 5;
    double y1 = 3;
    double z1 = 1;

    double x2 = 8;
    double y2 = 6;
    double z2 = 4;

    @Test
    public void testCrossProductReplacement()
    {
        final double EPSILON = 1e-12;
        double[] expected = { 6, -12, 6 };

        double[] result = Vectors.crossProduct(x1, y1, z1, x2, y2, z2);

        assertEquals(result.length, expected.length);
        assertArrayEquals(expected, result, EPSILON);

        Vector3d u = new Vector3d(x1, y1, z1);
        Vector3d v = new Vector3d(x2, y2, z2);
        Vector3d w = new Vector3d();
        double vector3dResult [] = new double[3];

        w.cross(u, v);
        w.get(vector3dResult);

        assertArrayEquals(expected, vector3dResult, EPSILON);
    }
}