package common;

import ij.gui.Roi;
import ij.plugin.frame.RoiManager;
import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
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
}