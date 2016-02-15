package org.bonej.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Optional;

import org.junit.Test;

import sc.fiji.analyzeSkeleton.Point;

/**
 * Unit tests for the org.bonej.common.Centroid class
 *
 * @author Richard Domander
 */
public class CentroidTest {
	private final static double DELTA = 1E-12;

	@Test
	public void testGetCentroidCoordinatesReturnsEmptyOptionalIfPointListIsNull() throws Exception {
		Optional<double[]> coordinates = Centroid.getCentroidCoordinates(null);
        assertFalse(coordinates.isPresent());
	}

	@Test
	public void testGetCentroidCoordinatesReturnsEmptyOptionalIfPointListIsEmpty() throws Exception {
        Optional<double[]> coordinates = Centroid.getCentroidCoordinates(new ArrayList<>());
        assertFalse(coordinates.isPresent());
	}

	@Test
	public void testGetCentroidCoordinatesOnePoint() throws Exception {
		Point point = new Point(3, 4, 5);
		ArrayList<Point> points = new ArrayList<>();
		points.add(point);

        Optional<double[]> optional = Centroid.getCentroidCoordinates(points);
        assertTrue(optional.isPresent());
		double result[] = optional.get();
		assertPointCoordinatesEqual(point, result);
	}

	@Test
	public void testGetCentroidCoordinates() throws Exception {
		ArrayList<Point> tiltedSquare = new ArrayList<>();
		tiltedSquare.add(new Point(0, 0, 0));
		tiltedSquare.add(new Point(2, 0, 0));
		tiltedSquare.add(new Point(2, 2, 2));
		tiltedSquare.add(new Point(0, 2, 2));
		Point expectedCentroid = new Point(1, 1, 1);

        Optional<double[]> optional = Centroid.getCentroidCoordinates(tiltedSquare);
        assertTrue(optional.isPresent());
        double result[] = optional.get();
		assertPointCoordinatesEqual(expectedCentroid, result);
	}

    @Test
    public void testGetCentroidPoint() throws Exception {
        ArrayList<Point> tiltedSquare = new ArrayList<>();
        tiltedSquare.add(new Point(0, 0, 0));
        tiltedSquare.add(new Point(3, 0, 0));
        tiltedSquare.add(new Point(3, 3, 3));
        tiltedSquare.add(new Point(0, 3, 3));
        Point expected = new Point(2, 2, 2); // the coordinates are rounded

        Optional<Point> optional = Centroid.getCentroidPoint(tiltedSquare);
        assertTrue(optional.isPresent());
        Point result = optional.get();
        assertEquals("Incorrect coordinates", expected, result);
    }

	private void assertPointCoordinatesEqual(final Point expected, final double[] coordinates) {
		assertEquals("Unexpected x-coordinate", expected.x, coordinates[Centroid.X_INDEX], DELTA);
		assertEquals("Unexpected y-coordinate", expected.y, coordinates[Centroid.Y_INDEX], DELTA);
		assertEquals("Unexpected z-coordinate", expected.z, coordinates[Centroid.Z_INDEX], DELTA);
	}
}