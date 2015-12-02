package protoOps;

import ij.ImagePlus;
import org.junit.Before;
import org.junit.Test;
import protoOps.testImageCreators.CrossedCircleCreator;
import protoOps.testImageCreators.WireFrameCuboidCreator;
import sc.fiji.skeletonize3D.Skeletonize3D_;

import static org.junit.Assert.*;

/**
 * @author <a href="mailto:rdomander@rvc.ac.uk">Richard Domander</a>
 */
public class TriplePointAnglesTest
{
    private final static double HALF_PI = Math.PI / 2.0;
    private final static double QUARTER_PI = Math.PI / 4.0;
    private final static double NEARLY_HALF_PI = 1.58555518815571;

    private final static double[][][] CROSSED_CIRCLE_RESULT = {{
            {QUARTER_PI, QUARTER_PI, HALF_PI},
            null,
            {QUARTER_PI, HALF_PI, QUARTER_PI},
            {QUARTER_PI, HALF_PI, QUARTER_PI},
            {QUARTER_PI, QUARTER_PI, HALF_PI}
    }};

    private final double[][][] CROSSED_CIRCLE_RESULT_NTH_POINT = {{
            {HALF_PI, HALF_PI, Math.PI},
            null,
            {HALF_PI, Math.PI, HALF_PI},
            {HALF_PI, Math.PI, HALF_PI},
            {HALF_PI, HALF_PI, Math.PI}
    }};

    private final static double[][][] WIRE_FRAME_RESULT = {{
            {HALF_PI, HALF_PI, HALF_PI},
            {HALF_PI, HALF_PI, HALF_PI},
            {HALF_PI, HALF_PI, HALF_PI},
            {HALF_PI, HALF_PI, HALF_PI},
            {HALF_PI, HALF_PI, HALF_PI},
            {HALF_PI, HALF_PI, HALF_PI},
            {HALF_PI, HALF_PI, HALF_PI},
            {HALF_PI, HALF_PI, HALF_PI}}};

    private final static double[][][] WIRE_FRAME_RESULT_NTH_POINT = {{
            {NEARLY_HALF_PI, NEARLY_HALF_PI, NEARLY_HALF_PI},
            {NEARLY_HALF_PI, NEARLY_HALF_PI, NEARLY_HALF_PI},
            {NEARLY_HALF_PI, NEARLY_HALF_PI, NEARLY_HALF_PI},
            {NEARLY_HALF_PI, NEARLY_HALF_PI, NEARLY_HALF_PI},
            {NEARLY_HALF_PI, NEARLY_HALF_PI, NEARLY_HALF_PI},
            {NEARLY_HALF_PI, NEARLY_HALF_PI, NEARLY_HALF_PI},
            {NEARLY_HALF_PI, NEARLY_HALF_PI, NEARLY_HALF_PI},
            {NEARLY_HALF_PI, NEARLY_HALF_PI, NEARLY_HALF_PI}
    }};

    private TriplePointAngles triplePointAngles = null;

    @Before
    public void setUp() {
        triplePointAngles = new TriplePointAngles();
    }

    @Test
    public void testCalculateTriplePointAnglesWireFrameCuboid() {
        ImagePlus testImage = WireFrameCuboidCreator.createWireFrameCuboid(128, 128, 128, 32);

        triplePointAngles.setInputImage(testImage);
        triplePointAngles.setNthPoint(TriplePointAngles.VERTEX_TO_VERTEX);
        triplePointAngles.calculateTriplePointAngles();

        double[][][] result = triplePointAngles.getResults();
        assertNotEquals(null, result);
        assertEquals("Resulting angle array has wrong size", WIRE_FRAME_RESULT.length, result.length);

        for (int g = 0; g < WIRE_FRAME_RESULT.length; g++) {
            assertEquals("Resulting angle array has wrong size", WIRE_FRAME_RESULT[g].length, result[g].length);
            for (int v = 0; v < WIRE_FRAME_RESULT[g].length; v++) {
                assertEquals("Resulting angle array has wrong size",
                        WIRE_FRAME_RESULT[g][v].length, result[g][v].length);
                assertArrayEquals("Result array has wrong values", WIRE_FRAME_RESULT[g][v], result[g][v], 1e-12);
            }
        }
    }

    @Test
    public void testCalculateTriplePointAnglesWireFrameCuboidNthPoint() {
        ImagePlus testImage = WireFrameCuboidCreator.createWireFrameCuboid(128, 128, 128, 32);

        triplePointAngles.setInputImage(testImage);
        triplePointAngles.setNthPoint(32);
        triplePointAngles.calculateTriplePointAngles();

        double[][][] result = triplePointAngles.getResults();

        for (int g = 0; g < WIRE_FRAME_RESULT_NTH_POINT.length; g++) {
            for (int v = 0; v < WIRE_FRAME_RESULT_NTH_POINT[g].length; v++) {
                assertArrayEquals("Result array has wrong values", WIRE_FRAME_RESULT_NTH_POINT[g][v], result[g][v],
                        1e-12);
            }
        }
    }

    @Test
    public void testCalculateTriplePointAnglesCrossedCircle() {
        ImagePlus testImage = CrossedCircleCreator.createCrossedCircle(256);
        //why do we need to skeletonize the image, and why does it affect test results? Smoothing?
        Skeletonize3D_ skeletonizer = new Skeletonize3D_();
        skeletonizer.setup("", testImage);
        skeletonizer.run(null);

        triplePointAngles.setInputImage(testImage);
        triplePointAngles.setNthPoint(TriplePointAngles.VERTEX_TO_VERTEX);
        triplePointAngles.calculateTriplePointAngles();

        double[][][] result = triplePointAngles.getResults();
        assertEquals("Resulting angle array has wrong size", CROSSED_CIRCLE_RESULT.length, result.length);
        for (int g = 0; g < CROSSED_CIRCLE_RESULT.length; g++) {
            assertEquals("Resulting angle array has wrong size", CROSSED_CIRCLE_RESULT[g].length, result[g].length);
            for (int v = 0; v < CROSSED_CIRCLE_RESULT[g].length; v++) {
                if (CROSSED_CIRCLE_RESULT[g][v] != null) {
                    assertEquals("Resulting angle array has wrong size", CROSSED_CIRCLE_RESULT[g][v].length,
                            result[g][v].length);
                }
                assertArrayEquals(CROSSED_CIRCLE_RESULT[g][v], result[g][v], 1e-12);
            }
        }
    }

    @Test
    public void testCalculateTriplePointAnglesCrossedCircleNth() {
        ImagePlus testImage = CrossedCircleCreator.createCrossedCircle(256);
        Skeletonize3D_ skeletonizer = new Skeletonize3D_();
        skeletonizer.setup("", testImage);
        skeletonizer.run(null);

        triplePointAngles.setInputImage(testImage);
        triplePointAngles.setNthPoint(8);
        triplePointAngles.calculateTriplePointAngles();

        double[][][] result = triplePointAngles.getResults();
        for (int g = 0; g < CROSSED_CIRCLE_RESULT_NTH_POINT.length; g++)
            for (int v = 0; v < CROSSED_CIRCLE_RESULT_NTH_POINT[g].length; v++)
                assertArrayEquals(CROSSED_CIRCLE_RESULT_NTH_POINT[g][v], result[g][v], 1e-12);
    }
}