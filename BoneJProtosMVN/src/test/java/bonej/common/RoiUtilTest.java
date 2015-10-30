package bonej.common;

import ij.gui.Roi;
import ij.plugin.frame.RoiManager;
import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the Common.RoiUtil class
 * @author <a href="mailto:rdomander@rvc.ac.uk">Richard Domander</a>
 */
public class RoiUtilTest {

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

        RoiManager mockRoiManager = mock(RoiManager.class);

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

        // Bad slice number
        ArrayList<Roi> resultRois = RoiUtil.getSliceRoi(mockRoiManager, BAD_SLICE_NUMBER);

        assertEquals("Bad slice number should return no ROIs", 0, resultRois.size());

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
        final int MIN_Z = 1;
        final int MAX_Z = 3;

        final String roi1Label = "000" + MIN_Z + "-0000-0001";
        final String roi2Label = "000" + MAX_Z + "-0000-0001";

        Roi roi1 = new Roi(ROI1_X, ROI1_Y, ROI1_WIDTH, ROI1_HEIGHT);
        roi1.setName(roi1Label);

        Roi roi2 = new Roi(ROI2_X, ROI2_Y, ROI2_WIDTH, ROI2_HEIGHT);
        roi2.setName(roi2Label);

        Roi rois[] = {roi1, roi2};

        RoiManager mockRoiManager = mock(RoiManager.class);
        when(mockRoiManager.getSliceNumber(anyString())).thenCallRealMethod();
        when(mockRoiManager.getRoisAsArray()).thenReturn(rois);

        // RoiManager == null
        int resultFrame[] = RoiUtil.getLimits(null);

        assertEquals(null, resultFrame);

        // Empty RoiManager
        when(mockRoiManager.getCount()).thenReturn(0);

        resultFrame = RoiUtil.getLimits(mockRoiManager);

        assertEquals(null, resultFrame);

        // All valid ROIs
        when(mockRoiManager.getCount()).thenReturn(rois.length);

        resultFrame = RoiUtil.getLimits(mockRoiManager);

        assertNotEquals(null, resultFrame);
        assertEquals(NUM_LIMITS, resultFrame.length);
        assertEquals(MIN_X, resultFrame[0]);
        assertEquals(MAX_X, resultFrame[1]);
        assertEquals(MIN_Y, resultFrame[2]);
        assertEquals(MAX_Y, resultFrame[3]);
        assertEquals(MIN_Z, resultFrame[4]);
        assertEquals(MAX_Z, resultFrame[5]);

        // A ROI without a slice number (z-index)
    }
}