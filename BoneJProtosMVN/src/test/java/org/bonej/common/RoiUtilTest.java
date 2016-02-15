package org.bonej.common;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.awt.*;
import java.util.ArrayList;
import java.util.Optional;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import protoOps.testImageCreators.StaticTestImageHelper;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Roi;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;

/**
 * Unit tests for the RoiUtil class
 * @author Richard Domander
 */
public class RoiUtilTest {
    RoiManager mockRoiManager = mock(RoiManager.class);
    private static ImagePlus mockImage;
    private static ImageStack mockStack;

    private final static int MOCK_IMAGE_WIDTH = 100;
    private final static int MOCK_IMAGE_HEIGHT = 100;
    private final static int MOCK_IMAGE_DEPTH = 4;

    @BeforeClass
    public static void oneTimeSetUp()
    {
        IJ.newImage("mockImage", "8-bit", MOCK_IMAGE_WIDTH, MOCK_IMAGE_HEIGHT, MOCK_IMAGE_DEPTH);
        mockImage = IJ.getImage();
        mockStack = mockImage.getStack();
    }

    @AfterClass
    public static void oneTimeTearDown()
    {
        if (mockImage != null) {
            mockImage.flush();
            mockImage.close();
            mockImage = null;
            mockStack = null;
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

        // RoiMan == null
        ArrayList<Roi> resultRois = RoiUtil.getSliceRoi(null, mockStack, 1);
        assertEquals(true, resultRois.isEmpty());

        // ImageStack == null
        resultRois = RoiUtil.getSliceRoi(mockRoiManager, null, 1);
        assertEquals(true, resultRois.isEmpty());

        // Out of bounds slice number
        resultRois = RoiUtil.getSliceRoi(mockRoiManager, mockStack, BAD_SLICE_NUMBER);
        assertEquals("Out of bounds slice number should return no ROIs", 0, resultRois.size());

        resultRois = RoiUtil.getSliceRoi(mockRoiManager, mockStack, mockStack.getSize() + 1);
        assertEquals("Out of bounds slice number should return no ROIs", 0, resultRois.size());

        // Slice with no (associated) Rois
        resultRois = RoiUtil.getSliceRoi(mockRoiManager, mockStack, NO_ROI_SLICE_NO);
        assertEquals("Wrong number of ROIs returned", 1, resultRois.size());
        assertEquals("Wrong ROI returned", noSliceLabel, resultRois.get(0).getName());

        // Slice with one Roi
        resultRois = RoiUtil.getSliceRoi(mockRoiManager, mockStack, SINGLE_ROI_SLICE_NO);

        assertEquals("Wrong number of ROIs returned", 2, resultRois.size());
        assertEquals("Wrong ROI returned, or ROIs in wrong order", singleRoiLabel, resultRois.get(0).getName());
        assertEquals("Wrong ROI returned, or ROIs in wrong order", noSliceLabel, resultRois.get(1).getName());

        // Slice with multiple Rois
        resultRois = RoiUtil.getSliceRoi(mockRoiManager, mockStack, MULTI_ROI_SLICE_NO);

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

        // Null RoiManager
        Optional<int[]> optionalResult = RoiUtil.getLimits(null, mockStack);
        assertFalse(optionalResult.isPresent());

        // Null stack
        optionalResult = RoiUtil.getLimits(mockRoiManager, null);
        assertFalse(optionalResult.isPresent());

        // Empty RoiManager
        when(mockRoiManager.getCount()).thenReturn(0);

        optionalResult = RoiUtil.getLimits(mockRoiManager, mockStack);
        assertFalse(optionalResult.isPresent());

        // All valid ROIs
        when(mockRoiManager.getCount()).thenReturn(rois.length);

        optionalResult = RoiUtil.getLimits(mockRoiManager, mockStack);
        assertTrue(optionalResult.isPresent());
        int[] limitsResult = optionalResult.get();
        assertEquals(NUM_LIMITS, limitsResult.length);
        assertEquals(MIN_X, limitsResult[0]);
        assertEquals(MAX_X, limitsResult[1]);
        assertEquals(MIN_Y, limitsResult[2]);
        assertEquals(MAX_Y, limitsResult[3]);
        assertEquals(MIN_Z, limitsResult[MIN_Z_INDEX]);
        assertEquals(MAX_Z, limitsResult[MAX_Z_INDEX]);

        // Valid ROIs, and one with no slice number (active on all slices)
        Roi allActive = new Roi(80, 80, 10, 10);
        //if the label of a roi doesn't follow a certain format, then RoiManager.getSliceNumber returns -1
        allActive.setName("ALL_ACTIVE");
        Roi roisWithAllActive[] = {roi1, roi2, allActive};

        when(mockRoiManager.getRoisAsArray()).thenReturn(roisWithAllActive);

        optionalResult = RoiUtil.getLimits(mockRoiManager, mockStack);
        assertTrue(optionalResult.isPresent());
        limitsResult = optionalResult.get();
        assertEquals(1, limitsResult[MIN_Z_INDEX]);
        assertEquals(mockStack.getSize(), limitsResult[MAX_Z_INDEX]);

        // Valid ROIs, and one with a too large slice number
        Roi farZRoi = new Roi(10, 10, 10, 10);
        farZRoi.setName("9999-0000-0001"); // slice no == 9999

        Roi roisWithBadZ[] = {roi1, roi2, farZRoi};

        when(mockRoiManager.getRoisAsArray()).thenReturn(roisWithBadZ);

        optionalResult = RoiUtil.getLimits(mockRoiManager, mockStack);
        assertTrue(optionalResult.isPresent());
        limitsResult = optionalResult.get();
        assertEquals(MIN_Z, limitsResult[MIN_Z_INDEX]);
        assertEquals(MAX_Z, limitsResult[MAX_Z_INDEX]);

        // No valid ROIs
        Roi badRoi = new Roi(-100, -100, 10, 10);
        badRoi.setName("BAD_ROI");

        Roi badRois[] = {badRoi};

        when(mockRoiManager.getRoisAsArray()).thenReturn(badRois);


        optionalResult = RoiUtil.getLimits(mockRoiManager, mockStack);
        assertFalse(optionalResult.isPresent());
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
        final int ORIGINAL_BG_COLOR_COUNT = 4;
        final int FILL_COLOR_COUNT = BACKGROUND_COLOR_COUNT - ORIGINAL_BG_COLOR_COUNT;

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

        final int CUBE_SIDE = 10;
        ImagePlus image = StaticTestImageHelper.createCuboid(CUBE_SIDE, CUBE_SIDE, CUBE_SIDE, TEST_COLOR, 1);
        ImageStack originalStack = image.getStack();

        //All valid ROIs (basic cropping test)
        Optional<ImageStack> optionalResult = RoiUtil.cropToRois(mockRoiManager, originalStack, false, 0x00, 0);
        ImageStack resultStack = optionalResult.get();
        assertEquals("Cropped stack has wrong width", WIDTH, resultStack.getWidth());
        assertEquals("Cropped stack has wrong height", HEIGHT, resultStack.getHeight());
        assertEquals("Cropped stack has wrong depth", DEPTH, resultStack.getSize());

        int foregroundCount = countColorPixels(resultStack, TEST_COLOR);
        assertEquals("Cropped area has wrong amount of foreground color", TEST_COLOR_COUNT, foregroundCount);

        int backgroundCount = countColorPixels(resultStack, BACKGROUND_COLOR);
        assertEquals("Cropped area has wrong amount of background color", BACKGROUND_COLOR_COUNT, backgroundCount);

        //padding
        optionalResult = RoiUtil.cropToRois(mockRoiManager, originalStack, false, 0x00, PADDING);
        ImageStack paddedResultStack = optionalResult.get();
        assertEquals("Cropped stack has wrong padded width", WIDTH + TOTAL_PADDING, paddedResultStack.getWidth());
        assertEquals("Cropped stack has wrong padded height", HEIGHT + TOTAL_PADDING, paddedResultStack.getHeight());
        assertEquals("Cropped stack has wrong padded depth", DEPTH + TOTAL_PADDING, paddedResultStack.getSize());

        assertEquals("Padding didn't shift the pixels correctly", true, pixelsShifted(resultStack, paddedResultStack,
                PADDING));

        //fill color
        optionalResult = RoiUtil.cropToRois(mockRoiManager, originalStack, true, FILL_COLOR, 0);
        resultStack = optionalResult.get();

        foregroundCount = countColorPixels(resultStack, TEST_COLOR);
        assertEquals("Cropped area has wrong amount of foreground color", TEST_COLOR_COUNT, foregroundCount);

        backgroundCount = countColorPixels(resultStack, BACKGROUND_COLOR);
        assertEquals("Cropped area has wrong amount of \"original\" background color", ORIGINAL_BG_COLOR_COUNT,
                backgroundCount);

        int fillCount = countColorPixels(resultStack, FILL_COLOR);
        assertEquals("Cropped area has wrong amount of background fill color", FILL_COLOR_COUNT, fillCount);

        //A ROI active on all slices
        final int ALL_ACTIVE_WIDTH = 6;
        final int ALL_ACTIVE_HEIGHT = 5;
        Roi allActive = new Roi(1, 1, ALL_ACTIVE_WIDTH, ALL_ACTIVE_HEIGHT);
        allActive.setName("All active");
        Roi noZRois[] = {allActive};

        when(mockRoiManager.getRoisAsArray()).thenReturn(noZRois);

        optionalResult = RoiUtil.cropToRois(mockRoiManager, originalStack, false, 0x00, 0);
        resultStack = optionalResult.get();
        assertEquals("Cropped stack has wrong width", ALL_ACTIVE_WIDTH, resultStack.getWidth());
        assertEquals("Cropped stack has wrong height", ALL_ACTIVE_HEIGHT, resultStack.getHeight());
        assertEquals("Cropped stack has wrong depth", originalStack.getSize(), resultStack.getSize());

        foregroundCount = countColorPixels(resultStack, TEST_COLOR);
        assertEquals("Cropped area has wrong amount of foreground color", ALL_ACTIVE_HEIGHT * ALL_ACTIVE_WIDTH *
                CUBE_SIDE, foregroundCount);


        // Too large ROI
        Roi tooLargeRoi = new Roi(-10, -10, originalStack.getWidth() + 100, originalStack.getHeight() + 100);
        tooLargeRoi.setName("0001-0000-0001");
        Roi badRois[] = {tooLargeRoi};

        when(mockRoiManager.getRoisAsArray()).thenReturn(badRois);

        optionalResult = RoiUtil.cropToRois(mockRoiManager, originalStack, false, 0x00, 0);
        resultStack = optionalResult.get();
        assertEquals("Cropped stack has wrong width", originalStack.getWidth(), resultStack.getWidth());
        assertEquals("Cropped stack has wrong height", originalStack.getHeight(), resultStack.getHeight());
        assertEquals("Cropped stack has wrong depth", 1, resultStack.getSize());
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

    @Test
    public void testGetSafeRoiBounds() throws Exception {
        final int X = 10;
        final int Y = 10;
        final int WIDTH = 10;
        final int HEIGHT = 10;

        Rectangle validRectangle = new Rectangle(X, Y, WIDTH, HEIGHT);
        Rectangle outOfBoundsRectangle = new Rectangle(-10, -10, 5, 5);
        Rectangle tooLargeRectangle = new Rectangle(X, Y, mockStack.getWidth() + 100, mockStack.getHeight() + 100);

        // valid bounds
        boolean result = RoiUtil.getSafeRoiBounds(validRectangle, mockStack.getWidth(), mockStack.getHeight());
        assertEquals(true, result);
        assertEquals(X, validRectangle.x);
        assertEquals(Y, validRectangle.y);
        assertEquals(WIDTH, validRectangle.width);
        assertEquals(HEIGHT, validRectangle.height);

        // bounds completely outside the image stack
        result = RoiUtil.getSafeRoiBounds(outOfBoundsRectangle, mockStack.getWidth(), mockStack.getHeight());
        assertEquals(false, result);
        assertEquals(0, outOfBoundsRectangle.x);
        assertEquals(0, outOfBoundsRectangle.y);
        assertEquals(0, outOfBoundsRectangle.width);
        assertEquals(0, outOfBoundsRectangle.height);

        // bound partly outside the image stack
        result = RoiUtil.getSafeRoiBounds(tooLargeRectangle, mockStack.getWidth(), mockStack.getHeight());
        assertEquals(true, result);
        assertEquals(X, tooLargeRectangle.x);
        assertEquals(Y, tooLargeRectangle.y);
        assertEquals(mockStack.getWidth() - X, tooLargeRectangle.width);
        assertEquals(mockStack.getHeight() - Y, tooLargeRectangle.height);

    }

    /**
     * A test for copying from source stack to target stack with a mask
     *
     * Complements testCropStack(), because I don't know how to set up a ImageStack with a mask on one of its slices.
     * @throws Exception
     */
    @Test
    public void testCopyRoiWithMask() throws Exception
    {
        final int TEST_COLOR = 0x20;
        final int TEST_COLOR_COUNT = 75;

        // Create a mask from an L-shaped polygon
        Polygon polygon = new Polygon();
        polygon.addPoint(0, 0);
        polygon.addPoint(10, 0);
        polygon.addPoint(10, 5);
        polygon.addPoint(5, 5);
        polygon.addPoint(5, 10);
        polygon.addPoint(0, 10);
        polygon.addPoint(0, 0);

        ImageProcessor result = mockStack.getProcessor(1).createProcessor(mockImage.getWidth(),
                mockImage.getHeight());
        ImageProcessor ip = mockStack.getProcessor(1).createProcessor(mockImage.getWidth(),
                mockImage.getHeight());
        ip.setRoi(polygon);
        ImageProcessor mask = ip.getMask();

        // set up mock ImageProcessor
        ImageProcessor mockSource = mock(ImageProcessor.class);
        when(mockSource.getMask()).thenReturn(mask);
        when(mockSource.get(anyInt(), anyInt())).thenReturn(TEST_COLOR);

        // get and assert result
        RoiUtil.copyRoiWithMask(mockSource, result, 0, 0, 10, 10, 0);
        ImageStack stack = new ImageStack(result.getWidth(), result.getHeight());
        stack.addSlice(result);

        int foregroundCount = countColorPixels(stack, TEST_COLOR);
        assertEquals(TEST_COLOR_COUNT, foregroundCount);
    }
}