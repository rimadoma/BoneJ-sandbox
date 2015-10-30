package org.bonej.common;

import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Roi;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;

import java.awt.*;
import java.util.ArrayList;

/**
 * @author  Michael Doube
 * @author <a href="mailto:rdomander@rvc.ac.uk">Richard Domander</a>
 */
public class RoiUtil {
    public static final int FIRST_SLICE_NUMBER = 1;
    public static final int NO_SLICE_NUMBER = -1;
    public static final int DEFAULT_Z_MIN = 1;
    public static final int DEFAULT_Z_MAX = Integer.MAX_VALUE;


    /**
     * Returns a list of ROIs that are active in the given slice.
     *
     * @todo    Functional testing. Does the method work as intended with a real RoiManager created by ImageJ?
     * @todo    Does calling this method make sense if there's no image open?
     * @param   roiMan      The collection of all the current ROIs
     * @param   sliceNumber Number of the slice to be searched
     * @pre     roiMan != null
     * @return  In addition to the active ROIs, returns all the ROIs without
     *          a slice number (assumed to be active in all slices).
     */
    public static ArrayList<Roi> getSliceRoi(RoiManager roiMan, int sliceNumber) {
        ArrayList<Roi> roiList = new ArrayList<>();

        if (sliceNumber < FIRST_SLICE_NUMBER) {
            //@todo: find out if there's a way to check whether sliceNumber >= IJ.getImage().getStackSize()
            return roiList;
        }

        Roi[] rois = roiMan.getRoisAsArray();
        for (Roi roi : rois) {
            String roiName = roi.getName();
            int roiSliceNumber = roiMan.getSliceNumber(roiName);
            if (roiSliceNumber == sliceNumber || roiSliceNumber == NO_SLICE_NUMBER) {
                roiList.add(roi);
            }
        }
        return roiList;
    }

    /**
     * Find the x, y and z limits of the ROIs in the ROI Manager
     *
     * @todo    Set z max to last slice of the current image if there's a ROI with no slice number?
     * @param   roiMan  The collection of all the current ROIs
     * @pre     roiMan != null
     * @return int[] containing x min, x max, y min, y max, z min and z max, or
     *         null if the ROI Manager contains no ROIs.
     *         If any of the ROIs has no slice number, z min is set to
     *         DEFAULT_Z_MIN and z max is set to DEFAULT_Z_MAX
     */
    public static int[] getLimits(RoiManager roiMan) {
        if (roiMan.getCount() == 0) {
            return null;
        }

        int xMin = Integer.MAX_VALUE;
        int xMax = 0;
        int yMin = Integer.MAX_VALUE;
        int yMax = 0;
        int zMin = Integer.MAX_VALUE;
        int zMax = 1;
        boolean noZRoi = false;

        Roi[] rois = roiMan.getRoisAsArray();

        for (Roi roi : rois) {
            Rectangle r = roi.getBounds();
            xMin = Math.min(r.x, xMin);
            xMax = Math.max(r.x + r.width, xMax);
            yMin = Math.min(r.y, yMin);
            yMax = Math.max(r.y + r.height, yMax);

            int sliceNumber = roiMan.getSliceNumber(roi.getName());
            if (sliceNumber < FIRST_SLICE_NUMBER) {
                noZRoi = true;
            } else {
                zMin = Math.min(sliceNumber, zMin);
                zMax = Math.max(sliceNumber, zMax);
            }
        }

        int[] limits = { xMin, xMax, yMin, yMax, zMin, zMax };

        if (noZRoi) {
            limits[4] = DEFAULT_Z_MIN;
            limits[5] = DEFAULT_Z_MAX;
        }

        return limits;
    }


    /**
     * Crop a stack to the limits of the ROIs in the ROI Manager and optionally
     * fill the background with a single pixel value.
     *
     * @param roiMan
     *            ROI Manager containing ROIs
     * @param stack
     *            input stack
     * @param fillBackground
     *            if true, background will be set to value
     * @param fillValue
     *            value to set background to
     * @param padding
     *            empty pixels to pad faces of cropped stack with
     * @return cropped copy of input stack
     */
    public static ImageStack cropStack(RoiManager roiMan, ImageStack stack,
                                       boolean fillBackground, int fillValue, int padding) {
        int[] limits = getLimits(roiMan);
        final int xmin = limits[0];
        final int xmax = limits[1];
        final int ymin = limits[2];
        final int ymax = limits[3];
        final int zmin = limits[4];
        final int zmax = (limits[5] == DEFAULT_Z_MAX) ? stack.getSize()
                : limits[5];
        // target stack dimensions
        final int w = xmax - xmin + 2 * padding;
        final int h = ymax - ymin + 2 * padding;
        final int d = zmax - zmin + 2 * padding;

        // offset that places source stack in coordinate frame
        // of target stack (i.e. origin of source stack relative to origin of
        // target stack)
        final int xOff = padding - xmin;
        final int yOff = padding - ymin;
        final int zOff = padding - zmin;

        ImagePlus imp = new ImagePlus("title", stack);
        ImageStack out = new ImageStack(w, h);
        for (int z = 1; z <= d; z++) {
            ImageProcessor ip = imp.getProcessor().createProcessor(w, h);
            final int length = ip.getPixelCount();
            if (z - zOff < 1 || z > stack.getSize()) { // catch out of bounds
                for (int i = 0; i < length; i++)
                    ip.set(i, fillValue);
                out.addSlice("padding", ip);
                continue;
            }
            ImageProcessor ipSource = stack.getProcessor(z - zOff);
            if (fillBackground)
                for (int i = 0; i < length; i++)
                    ip.set(i, fillValue);
            ArrayList<Roi> rois = getSliceRoi(roiMan, z - zOff);
            for (Roi roi : rois) {
                ipSource.setRoi(roi);
                Rectangle r = roi.getBounds();
                ImageProcessor mask = ipSource.getMask();
                final int rh = r.y + r.height;
                final int rw = r.x + r.width;
                for (int y = r.y; y < rh; y++) {
                    final int yyOff = y + yOff;
                    for (int x = r.x; x < rw; x++) {
                        if (mask == null || mask.get(x - r.x, y - r.y) > 0)
                            ip.set(x + xOff, yyOff, ipSource.get(x, y));
                    }
                }
            }
            out.addSlice(stack.getSliceLabel(z - zOff), ip);
        }
        return out;
    }
}
