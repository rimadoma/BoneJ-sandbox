package org.bonej.common;

//import ij.IJ;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Roi;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the RoiUtil class
 * @author <a href="mailto:rdomander@rvc.ac.uk">Richard Domander</a>
 */
//@PrepareForTest(IJ.class)
public class RoiUtilTest {
    RoiManager mockRoiManager = mock(RoiManager.class);
    private static ImagePlus mockImage;

    private final static int MOCK_IMAGE_WIDTH = 10;
    private final static int MOCK_IMAGE_HEIGHT = 10;
    private final static int MOCK_IMAGE_DEPTH = 4;

    @BeforeClass
    public static void oneTimeSetUp()
    {
        IJ.newImage("mockImage", "8-bit", MOCK_IMAGE_WIDTH, MOCK_IMAGE_HEIGHT, MOCK_IMAGE_DEPTH);
        mockImage = IJ.getImage();
    }

    @AfterClass
    public static void oneTimeTearDown()
    {
        if (mockImage != null) {
            mockImage.flush();
            mockImage.close();
            mockImage = null;
        }
    }

    @Before
    public void setUp()
    {
        mockRoiManager.reset();
    }

    @Test
    public void testGetSliceRoi() throws Exception {
        final int BAD_SLICE_NUMBER = 0;
        final int NO_ROI_SLICE_NO = 2;
        final int SINGLE_ROI_SLICE_NO = 3;
        final int MULTI_ROI_SLICE_NO = 4;

        // RoiManager.getSliceNumber tries to parse the number of the slice from the label of the Roi it's given.
        // It doesn't - for example - check the slice attribute of the given Roi...
        final String singleRoiLabel = "000" + SINGLE_ROI_SLICE_NO + "-0000-0001";
        final String multiRoi1Label = "000" + MULTI_ROI_SLICE_NO + "-0000-0001";
        final String multiRoi2Label = "000" + MULTI_ROI_SLICE_NO + "-0000-0002";
        final String noSliceLabel = "NO_SLICE";

        Roi singleRoi = new Roi(10, 10, 10, 10);
        singleRoi.setName(singleRoiLabel);

        Roi multiRoi1 = new Roi(10, 10, 10, 10);
        multiRoi1.setName(multiRoi1Label);

        Roi multiRoi2 = new Roi(30, 30, 10, 10);
        multiRoi2.setName(multiRoi2Label);

        Roi noSliceRoi = new Roi(50, 50, 10, 10);
        noSliceRoi.setName(noSliceLabel);

        Roi rois[] = {singleRoi, multiRoi1, multiRoi2, noSliceRoi};

        when(mockRoiManager.getSliceNumber(anyString())).thenCallRealMethod();
        when(mockRoiManager.getRoisAsArray()).thenReturn(rois);

        /* Can't test the case where no image is open,
         * because Mockito can't mock static methods (IJ.getImage),
         * and PowerMock isn't compatible with Mockito 2.0+
         */

        // Out of bounds slice number
        ArrayList<Roi> resultRois = RoiUtil.getSliceRoi(mockRoiManager, BAD_SLICE_NUMBER);
        assertEquals("Out of bounds slice number should return no ROIs", 0, resultRois.size());

        resultRois = RoiUtil.getSliceRoi(mockRoiManager, mockImage.getNSlices() + 1);
        assertEquals("Out of bounds slice number should return no ROIs", 0, resultRois.size());

        // Slice with no (associated) Rois
        resultRois = RoiUtil.getSliceRoi(mockRoiManager, NO_ROI_SLICE_NO);
        assertEquals("Wrong number of ROIs returned", 1, resultRois.size());
        assertEquals("Wrong ROI returned", noSliceLabel, resultRois.get(0).getName());

        // Slice with one Roi
        resultRois = RoiUtil.getSliceRoi(mockRoiManager, SINGLE_ROI_SLICE_NO);

        assertEquals("Wrong number of ROIs returned", 2, resultRois.size());
        assertEquals("Wrong ROI returned, or ROIs in wrong order", singleRoiLabel, resultRois.get(0).getName());
        assertEquals("Wrong ROI returned, or ROIs in wrong order", noSliceLabel, resultRois.get(1).getName());

        // Slice with multiple Rois
        resultRois = RoiUtil.getSliceRoi(mockRoiManager, MULTI_ROI_SLICE_NO);

        assertEquals("Wrong number of ROIs returned", 3, resultRois.size());
        assertEquals("Wrong ROI returned, or ROIs in wrong order", multiRoi1Label, resultRois.get(0).getName());
        assertEquals("Wrong ROI returned, or ROIs in wrong order", multiRoi2Label, resultRois.get(1).getName());
        assertEquals("Wrong ROI returned, or ROIs in wrong order", noSliceLabel, resultRois.get(2).getName());
    }

    @Test
    public void testGetLimits() throws Exception
    {
        final int NUM_LIMITS = 6;
        final int MIN_Z_INDEX = 4;
        final int MAX_Z_INDEX = 5;

        final int ROI1_X = 10;
        final int ROI1_Y = 10;
        final int ROI1_WIDTH = 30;
        final int ROI1_HEIGHT = 60;
        final int ROI2_X = 20;
        final int ROI2_Y = 5;
        final int ROI2_WIDTH = 40;
        final int ROI2_HEIGHT = 30;

        final int MIN_X = ROI1_X;
        final int MIN_Y = ROI2_Y;
        final int MAX_X = ROI2_X + ROI2_WIDTH;
        final int MAX_Y = ROI1_Y + ROI1_HEIGHT;
        final int MIN_Z = 2;
        final int MAX_Z = 3;

        final String roi1Label = "000" + MIN_Z + "-0000-0001";
        final String roi2Label = "000" + MAX_Z + "-0000-0001";

        Roi roi1 = new Roi(ROI1_X, ROI1_Y, ROI1_WIDTH, ROI1_HEIGHT);
        roi1.setName(roi1Label);

        Roi roi2 = new Roi(ROI2_X, ROI2_Y, ROI2_WIDTH, ROI2_HEIGHT);
        roi2.setName(roi2Label);

        Roi rois[] = {roi1, roi2};

        when(mockRoiManager.getSliceNumber(anyString())).thenCallRealMethod();
        when(mockRoiManager.getRoisAsArray()).thenReturn(rois);

        // Empty RoiManager
        when(mockRoiManager.getCount()).thenReturn(0);

        int limitsResult[] = RoiUtil.getLimits(mockRoiManager);
        assertEquals(null, limitsResult);

        // All valid ROIs
        when(mockRoiManager.getCount()).thenReturn(rois.length);

        limitsResult = RoiUtil.getLimits(mockRoiManager);
        assertNotEquals(null, limitsResult);
        assertEquals(NUM_LIMITS, limitsResult.length);
        assertEquals(MIN_X, limitsResult[0]);
        assertEquals(MAX_X, limitsResult[1]);
        assertEquals(MIN_Y, limitsResult[2]);
        assertEquals(MAX_Y, limitsResult[3]);
        assertEquals(MIN_Z, limitsResult[MIN_Z_INDEX]);
        assertEquals(MAX_Z, limitsResult[MAX_Z_INDEX]);

        // A ROI without a slice number (z-index)
        Roi badZRoi = new Roi(80, 80, 10, 10);
        //if the label of a roi doesn't follow a certain format, then RoiManager.getSliceNumber returns -1
        badZRoi.setName("BAD_LABEL");
        Roi roisWithBadZ[] = {roi1, roi2, badZRoi};

        when(mockRoiManager.getRoisAsArray()).thenReturn(roisWithBadZ);

        limitsResult = RoiUtil.getLimits(mockRoiManager);
        assertNotEquals(null, limitsResult);
        assertEquals(RoiUtil.DEFAULT_Z_MIN, limitsResult[MIN_Z_INDEX]);
        assertEquals(RoiUtil.DEFAULT_Z_MAX, limitsResult[MAX_Z_INDEX]);
    }

    @Test
    public void testCropStack() throws Exception
    {
        final int WIDTH = 6;
        final int HEIGHT = 3;
        final int DEPTH = 3;
        final int PADDING = 2;
        final int TOTAL_PADDING = 2 * PADDING; //total padding in each dimension
        final int ROI_WIDTH = 2;
        final int ROI_HEIGHT = 2;
        final int TEST_COLOR_COUNT = 8;
        final byte TEST_COLOR = 0x40;
        final byte BACKGROUND_COLOR = 0x00;
        final int BACKGROUND_COLOR_COUNT = 46;
        final byte FILL_COLOR = 0x10;
        final int FILL_COLOR_COUNT = BACKGROUND_COLOR_COUNT;
        final int MASKED_COUNT = 6;
        final byte MASK_BLACK = Byte.MAX_VALUE;

        int limits[] = {2, 8, 2, 5, 1, 3};

        Roi roi1 = new Roi(2, 2, ROI_WIDTH, ROI_HEIGHT);
        roi1.setName("0002-0000-0001");

        Roi roi2 = new Roi(6, 3, ROI_WIDTH, ROI_HEIGHT);
        roi2.setName("0003-0000-0001");

        Roi noColorRoi = new Roi(2, 2, ROI_WIDTH, ROI_HEIGHT);
        noColorRoi.setName("0001-0000-0001");

        Roi rois[] = {noColorRoi, roi1, roi2};

        when(mockRoiManager.getCount()).thenReturn(rois.length);
        when(mockRoiManager.getSliceNumber(anyString())).thenCallRealMethod();
        when(mockRoiManager.getRoisAsArray()).thenReturn(rois);

        ImagePlus image = TestDataMaker.createCuboid(10, 10, 10, TEST_COLOR, 1);
        ImageStack originalStack = image.getStack();

        ImageStack resultStack = RoiUtil.cropToRois(mockRoiManager, originalStack, false, 0x00, 0);
        assertEquals("Cropped stack has wrong width", WIDTH, resultStack.getWidth());
        assertEquals("Cropped stack has wrong height", HEIGHT, resultStack.getHeight());
        assertEquals("Cropped stack has wrong depth", DEPTH, resultStack.getSize());

        int foregroundCount = countColorPixels(resultStack, TEST_COLOR);
        assertEquals("Crop contains wrong part of the original image", TEST_COLOR_COUNT, foregroundCount);

        int backgroundCount = countColorPixels(resultStack, BACKGROUND_COLOR);
        assertEquals("Crop contains wrong part of the original image", BACKGROUND_COLOR_COUNT, backgroundCount);

        //padding
        ImageStack paddedResultStack = RoiUtil.cropToRois(mockRoiManager, image.getStack(), false, 0x00, PADDING);
        assertEquals("Cropped stack has wrong padded width", WIDTH + TOTAL_PADDING, paddedResultStack.getWidth());
        assertEquals("Cropped stack has wrong padded height", HEIGHT + TOTAL_PADDING, paddedResultStack.getHeight());
        assertEquals("Cropped stack has wrong padded depth", DEPTH + TOTAL_PADDING, paddedResultStack.getSize());

        assertEquals("Padding didn't shift the pixels correctly", true, pixelsShifted(resultStack, paddedResultStack,
                PADDING));

        //filling
        resultStack = RoiUtil.cropToRois(mockRoiManager, originalStack, true, FILL_COLOR, 0);

        int fillCount = countColorPixels(resultStack, FILL_COLOR);
        assertEquals("Crop area has wrong background fill color", FILL_COLOR_COUNT, fillCount);

        //@TODO: write mask functionality to cropToRois and the test case. Check how to mock masks.
    }

    /**
     * Checks that padding has moved all of the pixels to correct coordinates
     * @param   croppedStack  The cropped image without padding
     * @param   paddedStack   The same image with padding
     * @param   padding       number of padding pixels on each side of paddedStack
     * @pre     padding >= 0
     * @return true if all the pixels have shifted the correct amount
     */
    private static boolean pixelsShifted(ImageStack croppedStack, ImageStack paddedStack, int padding) {
        for (int z = 1; z <= croppedStack.getSize(); z++) {
            ImageProcessor sourceProcessor = croppedStack.getProcessor(z);
            int targetZ = z + padding;
            ImageProcessor targetProcessor = paddedStack.getProcessor(targetZ);
            for (int y = 0; y < croppedStack.getHeight(); y++) {
                int targetY = y + padding;
                for (int x = 0; x < croppedStack.getWidth(); x++) {
                    int targetX = x + padding;
                    int sourceColor = sourceProcessor.get(x, y);
                    int targetColor = targetProcessor.get(targetX, targetY);
                    if (sourceColor != targetColor) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * Counts the number of pixels that have the given color in all the slices of the given stack
     * @param stack The stack to inspect
     * @param color The color to be searched
     * @return The number of pixels that match the color
     */
    private static int countColorPixels(ImageStack stack, int color)
    {
        int count = 0;
        int height = stack.getHeight();
        int width = stack.getWidth();

        for (int z = 1; z <= stack.getSize(); z++) {
            byte pixels[] = (byte[]) stack.getPixels(z);
            for (int y = 0; y < height; y++) {
                int offset = y * width;
                for (int x = 0; x < width; x++) {
                    if (pixels[offset + x] == color) {
                        count++;
                    }
                }
            }
        }

        return count;
    }
}