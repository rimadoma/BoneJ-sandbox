package protoOps.triplePointAngles;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import protoOps.testImageCreators.StaticTestImageHelper;
import ij.IJ;
import ij.ImagePlus;

import java.util.Optional;

/**
 * Unit tests for the TriplePointAngles Op
 *
 * @author Michael Doube
 * @author Richard Domander
 */
public class TriplePointAnglesTest {
	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	private final static double HALF_PI = Math.PI / 2.0;
	private final static double QUARTER_PI = Math.PI / 4.0;

	// Skeletonization of the cuboid images thins them so that they lose their
	// corner pixels.
	// This moves the centroid of the triple point enough so that angles aren't
	// exactly PI / 2 anymore,
	// at least when measured some pixels away from the vertex
	// (WIRE_FRAME_RESULT_NTH_POINT)
	private final static double NEARLY_HALF_PI = 1.5904976894727854;

	private final static double[][][] CROSSED_CIRCLE_RESULT = {{{QUARTER_PI, QUARTER_PI, HALF_PI},
			{QUARTER_PI, HALF_PI, QUARTER_PI}, {QUARTER_PI, HALF_PI, QUARTER_PI}, {QUARTER_PI, QUARTER_PI, HALF_PI}}};

	private final double[][][] CROSSED_CIRCLE_RESULT_NTH_POINT = {{{HALF_PI, HALF_PI, Math.PI},
			{HALF_PI, Math.PI, HALF_PI}, {HALF_PI, Math.PI, HALF_PI}, {HALF_PI, HALF_PI, Math.PI}}};

	private final static double[][][] WIRE_FRAME_RESULT = {{{HALF_PI, HALF_PI, HALF_PI}, {HALF_PI, HALF_PI, HALF_PI},
			{HALF_PI, HALF_PI, HALF_PI}, {HALF_PI, HALF_PI, HALF_PI}, {HALF_PI, HALF_PI, HALF_PI},
			{HALF_PI, HALF_PI, HALF_PI}, {HALF_PI, HALF_PI, HALF_PI}, {HALF_PI, HALF_PI, HALF_PI}}};

	private final static double[][][] WIRE_FRAME_RESULT_NTH_POINT = {{{NEARLY_HALF_PI, NEARLY_HALF_PI, NEARLY_HALF_PI},
			{NEARLY_HALF_PI, NEARLY_HALF_PI, NEARLY_HALF_PI}, {NEARLY_HALF_PI, NEARLY_HALF_PI, NEARLY_HALF_PI},
			{NEARLY_HALF_PI, NEARLY_HALF_PI, NEARLY_HALF_PI}, {NEARLY_HALF_PI, NEARLY_HALF_PI, NEARLY_HALF_PI},
			{NEARLY_HALF_PI, NEARLY_HALF_PI, NEARLY_HALF_PI}, {NEARLY_HALF_PI, NEARLY_HALF_PI, NEARLY_HALF_PI},
			{NEARLY_HALF_PI, NEARLY_HALF_PI, NEARLY_HALF_PI}}};

	private TriplePointAngles triplePointAngles = null;

	@Before
	public void setUp() {
		triplePointAngles = new TriplePointAngles();
	}

	@Test
	public void testCalculateTriplePointAnglesWireFrameCuboid() {
		ImagePlus testImage = StaticTestImageHelper.createWireFrameCuboid(128, 128, 128, 32);

		triplePointAngles.setInputImage(testImage);
		triplePointAngles.setNthPoint(TriplePointAngles.VERTEX_TO_VERTEX);
		triplePointAngles.calculateTriplePointAngles();

        Optional<double[][][]> optional = triplePointAngles.getResults();
        assertTrue("Triple point angles not available", optional.isPresent());
		double[][][] result = optional.get();
		assertNotEquals(null, result);
		assertEquals("Resulting angle array has wrong size", WIRE_FRAME_RESULT.length, result.length);

		for (int g = 0; g < WIRE_FRAME_RESULT.length; g++) {
			assertEquals("Resulting angle array has wrong size", WIRE_FRAME_RESULT[g].length, result[g].length);
			for (int v = 0; v < WIRE_FRAME_RESULT[g].length; v++) {
				assertEquals("Resulting angle array has wrong size", WIRE_FRAME_RESULT[g][v].length,
						result[g][v].length);
				assertArrayEquals("Result array has wrong values", WIRE_FRAME_RESULT[g][v], result[g][v], 1e-12);
			}
		}
	}

	/**
	 * Not a proper test, because the expected results are just magic numbers,
	 * but at least it alerts if the results change
	 */
	@Test
	public void testCalculateTriplePointAnglesWireFrameCuboidNthPoint() {
		ImagePlus testImage = StaticTestImageHelper.createWireFrameCuboid(128, 128, 128, 32);

		triplePointAngles.setInputImage(testImage);
		triplePointAngles.setNthPoint(32);
		triplePointAngles.calculateTriplePointAngles();

        Optional<double[][][]> optional = triplePointAngles.getResults();
        assertTrue("Triple point angles not available", optional.isPresent());
        double[][][] result = optional.get();

		for (int g = 0; g < WIRE_FRAME_RESULT_NTH_POINT.length; g++) {
			for (int v = 0; v < WIRE_FRAME_RESULT_NTH_POINT[g].length; v++) {
				assertArrayEquals("Result array has wrong values", WIRE_FRAME_RESULT_NTH_POINT[g][v], result[g][v],
						1e-12);
			}
		}
	}

	@Test
	public void testCalculateTriplePointAnglesCrossedCircle() {
		ImagePlus testImage = StaticTestImageHelper.createCrossedCircle(256);

		triplePointAngles.setInputImage(testImage);
		triplePointAngles.setNthPoint(TriplePointAngles.VERTEX_TO_VERTEX);
		triplePointAngles.calculateTriplePointAngles();

        Optional<double[][][]> optional = triplePointAngles.getResults();
        assertTrue("Triple point angles not available", optional.isPresent());
        double[][][] result = optional.get();

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
		ImagePlus testImage = StaticTestImageHelper.createCrossedCircle(256);

		triplePointAngles.setInputImage(testImage);
		triplePointAngles.setNthPoint(8);
		triplePointAngles.calculateTriplePointAngles();

        Optional<double[][][]> optional = triplePointAngles.getResults();
        assertTrue("Triple point angles not available", optional.isPresent());
        double[][][] result = optional.get();

		for (int g = 0; g < CROSSED_CIRCLE_RESULT_NTH_POINT.length; g++)
			for (int v = 0; v < CROSSED_CIRCLE_RESULT_NTH_POINT[g].length; v++)
				assertArrayEquals(CROSSED_CIRCLE_RESULT_NTH_POINT[g][v], result[g][v], 1e-12);
	}

	@Test
	public void testSetInputImageThrowsNullPointerExceptionIfImageIsNull() throws Exception {
		expectedException.expect(NullPointerException.class);
		expectedException.expectMessage("Must have an input image");

		triplePointAngles.setInputImage(null);
	}

	@Test
	public void testSetInputImageThrowsIllegalArgumentExceptionIfImageIsNotBinary() throws Exception {
		expectedException.expect(IllegalArgumentException.class);
		expectedException.expectMessage("Input image must be binary");
		ImagePlus imagePlus = IJ.createImage("test", 200, 200, 200, 32);

		triplePointAngles.setInputImage(imagePlus);
	}

	@Test
	public void testSetNthPointThrowsIllegalArgumentExceptionIfValueIsInvalid() throws Exception {
		expectedException.expect(IllegalArgumentException.class);
		expectedException.expectMessage("Invalid nth point value");

		triplePointAngles.setNthPoint(-2);
	}

	@Test
	public void testCalculateTriplePointsThrowsIllegalArgumentExceptionIfImageCannotBeSkeletonized() throws Exception {
		expectedException.expect(IllegalArgumentException.class);
		expectedException.expectMessage("Input image could not be skeletonized");
		ImagePlus solidCuboid = StaticTestImageHelper.createCuboid(100, 100, 100, 0xFF, 10);
		triplePointAngles.setInputImage(solidCuboid);

		triplePointAngles.calculateTriplePointAngles();
		assertNull("Results must be null after calculateTriplePointAngles() has terminated unsuccessfully",
				triplePointAngles.getResults());
	}
}