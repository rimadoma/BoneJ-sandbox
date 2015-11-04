package org.bonej.common;

import ij.IJ;
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
     * @param   roiMan      The collection of all the current ROIs
     * @param   sliceNumber Number of the slice to be searched
     * @pre     roiMan != null
     * @pre     There's an image open (IJ.getImage() passes)
     * @return  In addition to the active ROIs, returns all the ROIs without
     *          a slice number (assumed to be active in all slices).
     *          Return an empty list sliceNumber is out of bounds
     *
     */
    public static ArrayList<Roi> getSliceRoi(RoiManager roiMan, int sliceNumber) {
        ArrayList<Roi> roiList = new ArrayList<>();
        ImagePlus currentImage = IJ.getImage();

        if (sliceNumber < FIRST_SLICE_NUMBER || sliceNumber > currentImage.getNSlices()) {
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
     * @todo    special z
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
     * @param roiMan                The manager containing the ROIs
     * @param sourceStack           The image to be cropped
     * @param fillBackground        If true, fill the background of the resulting image
     * @param fillColor             Color of the background of the resulting image
     * @param padding               Number of pixels added to the each side of the resulting image
     * @pre All ROIs in the manager must fit inside the source stack in (x,y)
     * @return  A new image stack containing the cropped version of the given image.
     *
     */
    public static ImageStack cropToRois(RoiManager roiMan, ImageStack sourceStack, boolean fillBackground,
                                        int fillColor, int padding)
    {
        int[] limits = getLimits(roiMan);

        if (limits == null) {
            return null;
        }

        final int xMin = limits[0];
        final int xMax = limits[1];
        final int yMin = limits[2];
        final int yMax = limits[3];
        final int zMin = limits[4];
        final int zMax = (limits[5] == DEFAULT_Z_MAX) ? sourceStack.getSize() : limits[5];

        final int croppedWidth = xMax - xMin + 2 * padding;
        final int croppedHeight = yMax - yMin + 2 * padding;

        ImageStack targetStack = new ImageStack(croppedWidth, croppedHeight);

        // copy
        ImageProcessor sourceProcessor;
        ImageProcessor targetProcessor;
        ArrayList<Roi> sliceRois;

        for (int sourceZ = zMin; sourceZ <= zMax; sourceZ++) {
            //@todo: check that ROIs with NO_SLICE_NUMBER work OK
            sliceRois = getSliceRoi(roiMan, sourceZ);
            if (sliceRois.size() == 0) {
                continue;
            }

            sourceProcessor = sourceStack.getProcessor(sourceZ);
            targetProcessor = sourceProcessor.createProcessor(croppedWidth, croppedHeight);

            if (fillBackground) {
                targetProcessor.setColor(fillColor);
                targetProcessor.fill();
            }

            copySlice(sourceProcessor, targetProcessor, sliceRois, padding);
            targetStack.addSlice("", targetProcessor);
        }

        // z padding
        targetProcessor = targetStack.getProcessor(1).createProcessor(croppedWidth, croppedHeight);
        if (fillBackground) {
            targetProcessor.setColor(fillColor);
            targetProcessor.fill();
        }
        for (int i = 0; i < padding; i++) {
            targetStack.addSlice("", targetProcessor, 0);
            targetStack.addSlice(targetProcessor);
        }

        return targetStack;
    }

    private static void copySlice(ImageProcessor sourceProcessor, ImageProcessor targetProcessor,
                                  ArrayList<Roi> sliceRois, int padding)
    {
        for (Roi sliceRoi : sliceRois) {
            Rectangle rectangle = sliceRoi.getBounds();
            int minY = rectangle.y;
            int minX = rectangle.x;
            int maxY = rectangle.y + rectangle.height;
            int maxX = rectangle.x + rectangle.width;

            ImageProcessor mask = sourceProcessor.getMask();
            if (mask == null) {
                copyRoi(sourceProcessor, targetProcessor, minX, minY, maxX, maxY, padding);
            } else {
                copyRoiWithMask(sourceProcessor, targetProcessor, minX, minY, maxX, maxY, padding);
            }
        }
    }

    /**
     * Copies the pixels in the given ROI from the source image to the target image.
     * Copies only those pixels where the color of the given mask > 0.
     *
     * @param   sourceProcessor Copy source
     * @param   targetProcessor Copy target
     * @param   minX            Horizontal start of the copy area 0 <= minX < width
     * @param   minY            Vertical start of the copy area 0 <= minY < height
     * @param   maxX            Horizontal end of the copy area 0 <= maxX <= width
     * @param   maxY            Vertical end of the copy area 0 <= maxY <= height
     * @param   padding         Number pixels added to each side of the copy target
     * @pre     sourceProcessor != null
     * @pre     targetProcessor != null
     *
     * Calls copyRoi with the given parameters if sourceProcessor.getMask() == null
     */
    private static void copyRoiWithMask(ImageProcessor sourceProcessor, ImageProcessor targetProcessor, final int minX,
                                        final int minY, final int maxX, final int maxY, final int padding)
    {
        ImageProcessor mask = sourceProcessor.getMask();
        if (mask == null) {
            copyRoi(sourceProcessor, targetProcessor, minX, minY, maxX, maxY, padding);
            return;
        }

        int targetY = padding;
        for (int sourceY = minY; sourceY < maxY; sourceY++) {
            int targetX = padding;
            for (int sourceX = minX; sourceX < maxX; sourceX++) {
                int maskColor = mask.get(sourceX, sourceY);
                if (maskColor > 0) {
                    int sourceColor = sourceProcessor.get(sourceX, sourceY);
                    targetProcessor.set(targetX, targetY, sourceColor);
                }
                targetX++;
            }
            targetY++;
        }
    }

    /**
     * Copies the pixels in the given ROI from the source image to the target image.
     *
     * @param   sourceProcessor Copy source
     * @param   targetProcessor Copy target
     * @param   minX            Horizontal start of the copy area 0 <= minX < width
     * @param   minY            Vertical start of the copy area 0 <= minY < height
     * @param   maxX            Horizontal end of the copy area 0 <= maxX <= width
     * @param   maxY            Vertical end of the copy area 0 <= maxY <= height
     * @param   padding         Number pixels added to each side of the copy target
     * @pre     sourceProcessor != null
     * @pre     targetProcessor != null
     */
    private static void copyRoi(ImageProcessor sourceProcessor, ImageProcessor targetProcessor, final int minX,
                                final int minY, final int maxX, final int maxY, final int padding)
    {
        int targetY = padding;
        for (int sourceY = minY; sourceY < maxY; sourceY++) {
            int targetX = padding;
            for (int sourceX = minX; sourceX < maxX; sourceX++) {
                int sourceColor = sourceProcessor.get(sourceX, sourceY);
                targetProcessor.set(targetX, targetY, sourceColor);
                targetX++;
            }
            targetY++;
        }
    }
}
