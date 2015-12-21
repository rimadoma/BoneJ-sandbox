package protoOps.volumeFraction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ij.process.ImageStatistics;
import org.bonej.common.Common;
import org.bonej.common.ImageCheck;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import ij.ImagePlus;
import ij.plugin.frame.RoiManager;

/**
 * Unit tests for the VolumeFraction class
 *
 * @author Richard Domander
 */
public class VolumeFractionTest {
    private VolumeFraction volumeFraction;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setup() {
        volumeFraction = new VolumeFraction();
    }

    @Test
    public void testSetImageThrowsNullPointerExceptionIfImageIsNull() throws Exception {
        expectedException.expect(NullPointerException.class);
        expectedException.expectMessage("Must have an input image");

        volumeFraction.setImage(null);
    }

    @Test
    public void testSetImageThrowsIllegalArgumentExceptionIfBitDepthIsWrong() throws Exception {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("Input image bit depth must be 8 or 16");
        ImagePlus mockImage = mock(ImagePlus.class);
        when(mockImage.getBitDepth()).thenReturn(24);

        volumeFraction.setImage(mockImage);
    }

    @Test
    public void testSetImageThrowsIllegalArgumentExceptionIfColorImage() throws Exception {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("Need a binary or grayscale image");
        ImagePlus mockImage = mock(ImagePlus.class);
        when(mockImage.getBitDepth()).thenReturn(8);
        when(mockImage.getType()).thenReturn(ImagePlus.COLOR_256);

        volumeFraction.setImage(mockImage);
    }

    @Test
    public void testSetImageSetsBinaryDefaultThresholds() throws Exception {
        // create a mock binary image
        ImagePlus mockImage = mock(ImagePlus.class);
        ImageStatistics statistics = new ImageStatistics();
        statistics.pixelCount = 2;
        statistics.histogram = new int[256];
        statistics.histogram[Common.BINARY_BLACK] = 1;
        statistics.histogram[Common.BINARY_WHITE] = 1;
        when(mockImage.getBitDepth()).thenReturn(8);
        when(mockImage.getType()).thenReturn(ImagePlus.GRAY8);
        when(mockImage.getStatistics()).thenReturn(statistics);

        assertTrue("Sanity check failed: image is not binary", ImageCheck.isBinary(mockImage));

        volumeFraction.setImage(mockImage);
        assertEquals("Incorrect minimum threshold for a binary image", 127, volumeFraction.getMinThreshold());
        assertEquals("Incorrect maximum threshold for a binary image", 255, volumeFraction.getMaxThreshold());
    }

    @Test
    public void testSetImageSets8BitGrayscaleDefaultThresholds() throws Exception {
        // create a mock 8-bit grayscale image
        ImagePlus mockImage = mock(ImagePlus.class);
        ImageStatistics statistics = new ImageStatistics();
        statistics.histogram = new int[256];
        statistics.pixelCount = 234;
        when(mockImage.getBitDepth()).thenReturn(8);
        when(mockImage.getType()).thenReturn(ImagePlus.GRAY8);
        when(mockImage.getStatistics()).thenReturn(statistics);

        assertTrue("Sanity check failed: image is not grayscale", ImageCheck.isGrayscale(mockImage));
        assertEquals("Sanity check failed: image is not 8-bit", 8, mockImage.getBitDepth());

        volumeFraction.setImage(mockImage);
        assertEquals("Incorrect minimum threshold for a 8-bit grayscale image", 0, volumeFraction.getMinThreshold());
        assertEquals("Incorrect maximum threshold for a 8-bit grayscale image", 255, volumeFraction.getMaxThreshold());
    }

    @Test
    public void testSetImageSets16BitGrayscaleDefaultThresholds() throws Exception {
        // create a mock 16-bit grayscale image
        ImagePlus mockImage = mock(ImagePlus.class);
        ImageStatistics statistics = new ImageStatistics();
        statistics.histogram = new int[65_535];
        statistics.pixelCount = 21342;
        when(mockImage.getBitDepth()).thenReturn(16);
        when(mockImage.getType()).thenReturn(ImagePlus.GRAY16);
        when(mockImage.getStatistics()).thenReturn(statistics);

        assertTrue("Sanity check failed: image is not grayscale", ImageCheck.isGrayscale(mockImage));
        assertEquals("Sanity check failed: image is not 16-bit", 16, mockImage.getBitDepth());

        volumeFraction.setImage(mockImage);
        assertEquals("Incorrect minimum threshold for a 16-bit grayscale image", 0, volumeFraction.getMinThreshold());
        assertEquals("Incorrect maximum threshold for a 16-bit grayscale image", 65_535,
                volumeFraction.getMaxThreshold());
    }

    @Test
    public void testSetVolumeAlgorithmThrowsIllegalArgumentExceptionIfAlgorithmIsInvalid() throws Exception {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("No such surface algorithm");

        volumeFraction.setVolumeAlgorithm(-1);
    }

    @Test
    public void testSetSurfaceResamplingThrowsIllegalArgumentExceptionIfValueIsNegative() throws Exception {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("Resampling value must be >= 0");

        volumeFraction.setSurfaceResampling(-1);
    }

    @Test
    public void testSetRoiManagerThrowsNullPointerExceptionIfRoiManagerIsNull() throws Exception {
        expectedException.expect(NullPointerException.class);
        expectedException.expectMessage("May not use a null ROI Manager");

        volumeFraction.setRoiManager(null);
    }

    @Test
    public void testSetRoiManagerThrowsIllegalArgumentExceptionIfRoiManagerIsEmpty() throws Exception {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("May not use an empty ROI Manager");
        RoiManager roiManager = mock(RoiManager.class);
        when(roiManager.getCount()).thenReturn(0);

        volumeFraction.setRoiManager(roiManager);
    }
}