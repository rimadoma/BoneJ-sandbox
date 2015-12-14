package org.bonej.common;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;

import ij.ImagePlus;
import ij.measure.Calibration;
import ij.process.ImageStatistics;

/**
 * @author Richard Domander
 */
public class ImageCheckTest {

	@Test
	public void testIsBinaryReturnsFalseIfImageIsNull() throws Exception {
		boolean result = ImageCheck.isBinary(null);
		assertFalse("Null image should not be binary", result);
	}

	@Test
	public void testIsBinaryReturnsFalseIfImageHasWrongType() throws Exception {
		ImagePlus testImage = mock(ImagePlus.class);
		int wrongTypes[] = {ImagePlus.COLOR_256, ImagePlus.COLOR_RGB, ImagePlus.GRAY16, ImagePlus.GRAY32};

		for (int wrongType : wrongTypes) {
			when(testImage.getType()).thenReturn(wrongType);

			boolean result = ImageCheck.isBinary(testImage);
			assertFalse("Only ImagePlus.GRAY8 type should be binary", result);
		}
	}

	@Test
	public void testIsBinaryUsesHistogramCorrectly() throws Exception {
		final int EIGHT_BYTES = 256;

		// more than two colors
		ImageStatistics nonBinaryStats = new ImageStatistics();
		nonBinaryStats.pixelCount = 3;
		nonBinaryStats.histogram = new int[EIGHT_BYTES];
		nonBinaryStats.histogram[Common.BINARY_BLACK] = 1;
		nonBinaryStats.histogram[Common.BINARY_WHITE] = 1;

		ImagePlus testImage = mock(ImagePlus.class);
		when(testImage.getStatistics()).thenReturn(nonBinaryStats);

		boolean result = ImageCheck.isBinary(testImage);
		assertFalse("Image with more than two colors must not be binary", result);

		// wrong two colors
		ImageStatistics wrongBinaryStats = new ImageStatistics();
		wrongBinaryStats.pixelCount = 2;
		wrongBinaryStats.histogram = new int[EIGHT_BYTES];
		wrongBinaryStats.histogram[Common.BINARY_BLACK] = 1;
		wrongBinaryStats.histogram[Common.BINARY_BLACK + 1] = 1;

		when(testImage.getStatistics()).thenReturn(wrongBinaryStats);

		result = ImageCheck.isBinary(testImage);
		assertFalse("Image with wrong two colors (not " + Common.BINARY_BLACK + " & " + Common.BINARY_WHITE
				+ ") must not be binary", result);

		// binary colors
		ImageStatistics binaryStats = new ImageStatistics();
		binaryStats.pixelCount = 2;
		binaryStats.histogram = new int[EIGHT_BYTES];
		binaryStats.histogram[Common.BINARY_BLACK] = 1;
		binaryStats.histogram[Common.BINARY_WHITE] = 1;

		when(testImage.getStatistics()).thenReturn(binaryStats);

		result = ImageCheck.isBinary(testImage);
		assertTrue("Image with two colors (" + Common.BINARY_BLACK + " & " + Common.BINARY_WHITE + ") should be binary",
				result);
	}

	@Test
	public void testIsVoxelIsotropicReturnsFalseIfImageIsNull() throws Exception {
		boolean result = ImageCheck.isVoxelIsotropic(null, 0.0);
		assertFalse("Null image should not be isotropic", result);
	}

	@Test
	public void testIsVoxelIsotropic() throws Exception {
		ImagePlus testImage = mock(ImagePlus.class);
		Calibration anisotropicCalibration = new Calibration();

		// 2D anisotropic image with 0 tolerance
		anisotropicCalibration.pixelWidth = 2;
		anisotropicCalibration.pixelHeight = 1;

		when(testImage.getCalibration()).thenReturn(anisotropicCalibration);
		when(testImage.getStackSize()).thenReturn(1);

		boolean result = ImageCheck.isVoxelIsotropic(testImage, 0.0);
		assertFalse("Image where width > height should not be isotropic", result);

		// 2D image where anisotropy is within tolerance
		result = ImageCheck.isVoxelIsotropic(testImage, 1.0);
		assertTrue("Image should be isotropic if anisotropy is within tolerance", result);

		// 3D image where depth anisotropy is beyond tolerance
		anisotropicCalibration.pixelDepth = 1000;
		when(testImage.getStackSize()).thenReturn(100);

		result = ImageCheck.isVoxelIsotropic(testImage, 1.0);
		assertFalse("Pixel depth too great to be anisotropic within tolerance", result);
	}

	@Test
	public void testIsGrayscaleReturnsFalseIfImageIsNull() throws Exception {
		boolean result = ImageCheck.isGrayscale(null);
		assertFalse("Null image should not be grayscale", result);
	}

	@Test
	public void testIsGrayscale() throws Exception {
		final int colorTypes[] = {ImagePlus.COLOR_RGB, ImagePlus.COLOR_256};
		final int grayscaleTypes[] = {ImagePlus.GRAY8, ImagePlus.GRAY16, ImagePlus.GRAY32};
		ImagePlus testImage = mock(ImagePlus.class);

		for (int colorType : colorTypes) {
			when(testImage.getType()).thenReturn(colorType);
			assertFalse("Color type should not be grayscale", ImageCheck.isGrayscale(testImage));
		}

		for (int grayscaleType : grayscaleTypes) {
			when(testImage.getType()).thenReturn(grayscaleType);
			assertTrue("Grayscale type should be grayscale", ImageCheck.isGrayscale(testImage));
		}
	}
}