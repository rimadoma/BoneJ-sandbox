package protoOps;

import ij.ImagePlus;
import org.junit.Before;
import org.junit.Test;
import protoOps.testImageCreators.WireFrameCuboidCreator;

import static org.junit.Assert.*;

/**
 * @author <a href="mailto:rdomander@rvc.ac.uk">Richard Domander</a>
 */
public class TriplePointAnglesTest
{
    private TriplePointAngles triplePointAngles = null;
    private final static double HALF_PI = Math.PI / 2;
    private final static double[][][] WIRE_FRAME_RESULT = {{
            {HALF_PI, HALF_PI, HALF_PI},
            {HALF_PI, HALF_PI, HALF_PI},
            {HALF_PI, HALF_PI, HALF_PI},
            {HALF_PI, HALF_PI, HALF_PI},
            {HALF_PI, HALF_PI, HALF_PI},
            {HALF_PI, HALF_PI, HALF_PI},
            {HALF_PI, HALF_PI, HALF_PI},
            {HALF_PI, HALF_PI, HALF_PI}}};

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
}