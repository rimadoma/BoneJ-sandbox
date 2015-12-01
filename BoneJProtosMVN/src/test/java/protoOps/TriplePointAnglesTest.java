package protoOps;

import ij.ImagePlus;
import org.bonej.common.TestDataMaker;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author <a href="mailto:rdomander@rvc.ac.uk">Richard Domander</a>
 */
public class TriplePointAnglesTest
{
    private TriplePointAngles triplePointAngles = null;
    private final static double HALF_PI = Math.PI / 2;
    private final static double[][][] HOLLOW_CUBOID_RESULT = {{
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
    public void testCalculateTriplePointAnglesHollowCuboid() {
        ImagePlus testImage = TestDataMaker.boxFrame(128, 128, 128);

        triplePointAngles.setInputImage(testImage);
        triplePointAngles.setNthPoint(TriplePointAngles.VERTEX_TO_VERTEX);
        triplePointAngles.calculateTriplePointAngles();

        double[][][] result = triplePointAngles.getResults();
        assertNotEquals(null, result);
        assertEquals("Resulting angle array has wrong size", HOLLOW_CUBOID_RESULT.length, result.length);

        for (int g = 0; g < HOLLOW_CUBOID_RESULT.length; g++) {
            assertEquals("Resulting angle array has wrong size", HOLLOW_CUBOID_RESULT[g].length, result[g].length);
            for (int v = 0; v < HOLLOW_CUBOID_RESULT[g].length; v++) {
                assertEquals("Resulting angle array has wrong size",
                        HOLLOW_CUBOID_RESULT[g][v].length, result[g][v].length);
                assertArrayEquals("Result array has wrong values", HOLLOW_CUBOID_RESULT[g][v], result[g][v], 1e-12);
            }
        }
    }
}