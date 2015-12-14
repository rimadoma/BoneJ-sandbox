package org.bonej.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import org.junit.Test;

import sc.fiji.analyzeSkeleton.Point;

/**
 * Unit tests for the org.bonej.common.Centroid class
 *
 * @author Richard Domander
 */
public class CentroidTest {
	private final static double DELTA = 1E-12;

	@Test(expected = NullPointerException.class)
	public void testGetCentroidThrowsNullPointerExceptionIfPointListIsNull() throws Exception {
		Centroid.getCentroid(null);
	}

	@Test
	public void testGetCentroidReturnsNaNArrayIfPointListIsEmpty() throws Exception {
		double emptyListResult[] = Centroid.getCentroid(new ArrayList<>());

		assertEquals("Coordinate array has wrong dimensions", 3, emptyListResult.length);
		for (double coordinate : emptyListResult) {
			assertTrue("Empty list should return NaN coordinates", Double.isNaN(coordinate));
		}
	}

	@Test
	public void testGetCentroidOnePoint() throws Exception {
		Point point = new Point(3, 4, 5);
		ArrayList<Point> points = new ArrayList<>();
		points.add(point);

		double result[] = Centroid.getCentroid(points);

		assertPointCoordinatesEqual(point, result);
	}

	@Test
	public void testCentroid() throws Exception {
		ArrayList<Point> tiltedSquare = new ArrayList<>();
		tiltedSquare.add(new Point(0, 0, 0));
		tiltedSquare.add(new Point(2, 0, 0));
		tiltedSquare.add(new Point(2, 2, 2));
		tiltedSquare.add(new Point(0, 2, 2));
		Point expectedCentroid = new Point(1, 1, 1);

		double result[] = Centroid.getCentroid(tiltedSquare);

		assertPointCoordinatesEqual(expectedCentroid, result);
	}

	private void assertPointCoordinatesEqual(Point expected, double[] coordinates) {
		assertEquals("Unexpected x-coordinate", expected.x, coordinates[Centroid.X_INDEX], DELTA);
		assertEquals("Unexpected y-coordinate", expected.y, coordinates[Centroid.Y_INDEX], DELTA);
		assertEquals("Unexpected z-coordinate", expected.z, coordinates[Centroid.Z_INDEX], DELTA);
	}
}