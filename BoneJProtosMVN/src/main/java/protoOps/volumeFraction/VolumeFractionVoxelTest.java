package protoOps.volumeFraction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.bonej.common.Common;
import org.bonej.common.ImageCheck;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import ij.ImagePlus;
import ij.plugin.frame.RoiManager;
import ij.process.ImageStatistics;

/**
 * Unit tests for the VolumeFractionVoxel class
 *
 * @author Richard Domander
 */
public class VolumeFractionVoxelTest {
    private VolumeFractionVoxel volumeFractionVoxel;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setup() {
        volumeFractionVoxel = new VolumeFractionVoxel();
    }

    @Test
    public void testSetImageThrowsNullPointerExceptionIfImageIsNull() throws Exception {
        expectedException.expect(NullPointerException.class);
        expectedException.expectMessage("Must have an input image");

        volumeFractionVoxel.setImage(null);
    }

    @Test
    public void testSetImageThrowsIllegalArgumentExceptionIfBitDepthIsWrong() throws Exception {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("Input image bit depth must be 8 or 16");
        ImagePlus mockImage = mock(ImagePlus.class);
        when(mockImage.getBitDepth()).thenReturn(24);

        volumeFractionVoxel.setImage(mockImage);
    }

    @Test
    public void testSetImageThrowsIllegalArgumentExceptionIfColorImage() throws Exception {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("Need a binary or grayscale image");
        ImagePlus mockImage = mock(ImagePlus.class);
        when(mockImage.getBitDepth()).thenReturn(8);
        when(mockImage.getType()).thenReturn(ImagePlus.COLOR_256);

        volumeFractionVoxel.setImage(mockImage);
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

        volumeFractionVoxel.setImage(mockImage);
        assertEquals("Incorrect minimum threshold for a 8-bit grayscale image", 128, volumeFractionVoxel.getMinThreshold());
        assertEquals("Incorrect maximum threshold for a 8-bit grayscale image", 255, volumeFractionVoxel.getMaxThreshold());
    }

    @Test
    public void testSetImageSets16BitGrayscaleDefaultThresholds() throws Exception {
        // create a mock 16-bit grayscale image
        ImagePlus mockImage = mock(ImagePlus.class);
        ImageStatistics statistics = new ImageStatistics();
        statistics.histogram = new int[0xFFFF];
        statistics.pixelCount = 21342;
        when(mockImage.getBitDepth()).thenReturn(16);
        when(mockImage.getType()).thenReturn(ImagePlus.GRAY16);
        when(mockImage.getStatistics()).thenReturn(statistics);

        assertTrue("Sanity check failed: image is not grayscale", ImageCheck.isGrayscale(mockImage));
        assertEquals("Sanity check failed: image is not 16-bit", 16, mockImage.getBitDepth());

        volumeFractionVoxel.setImage(mockImage);
        assertEquals("Incorrect minimum threshold for a 16-bit grayscale image", 2424, volumeFractionVoxel.getMinThreshold());
        assertEquals("Incorrect maximum threshold for a 16-bit grayscale image", 11_215,
                volumeFractionVoxel.getMaxThreshold());
    }

    @Test
    public void testSetRoiManagerThrowsNullPointerExceptionIfRoiManagerIsNull() throws Exception {
        expectedException.expect(NullPointerException.class);
        expectedException.expectMessage("May not use a null ROI Manager");

        volumeFractionVoxel.setRoiManager(null);
    }

    @Test
    public void testSetRoiManagerThrowsIllegalArgumentExceptionIfRoiManagerIsEmpty() throws Exception {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("May not use an empty ROI Manager");
        RoiManager roiManager = mock(RoiManager.class);
        when(roiManager.getCount()).thenReturn(0);

        volumeFractionVoxel.setRoiManager(roiManager);
    }
}