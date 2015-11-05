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
 * @todo    Functional testing. Does the class work as intended with a real RoiManager created by ImageJ?
 */
public class RoiUtil {
    public static final int FIRST_SLICE_NUMBER = 1;
    public static final int NO_SLICE_NUMBER = -1;


    /**
     * Returns a list of ROIs that are active in the given slice.
     *
     * @param   roiMan      The collection of all the current ROIs
     * @param   sliceNumber Number of the slice to be searched
     * @return  In addition to the active ROIs, returns all the ROIs without
     *          a slice number (assumed to be active in all slices).
     *          Return an empty list sliceNumber is out of bounds
     *
     */
    public static ArrayList<Roi> getSliceRoi(RoiManager roiMan, ImageStack stack, int sliceNumber) {
        ArrayList<Roi> roiList = new ArrayList<>();

        if (roiMan == null || stack == null) {
            return roiList;
        }

        if (sliceNumber < FIRST_SLICE_NUMBER || sliceNumber > stack.getSize()) {
            return roiList;
        }

        Roi[] rois = roiMan.getRoisAsArray();
        for (Roi roi : rois) {
            String roiName = roi.getName();
            if (roiName == null) {
                continue;
            }
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
     * @param   roiMan  The collection of all the current ROIs
     * @param   stack   The stack inside which the ROIs must fit (max limits).
     * @return int[] containing x min, x max, y min, y max, z min and z max.
     *         Returns null if the roiMan == null, or contains no ROIs.
     *         Returns null if stack == null
     *
     * If for any ROI isActiveOnAllSlices == true, then z min is set to
     * 1 and z max is set to stack.getSize().
     * If no ROI in roiMan fits inside stack, then limits are the same as the dimensions of the stack.
     */
    public static int[] getLimits(RoiManager roiMan, ImageStack stack) {
        if (roiMan == null || stack == null) {
            return null;
        }

        if (roiMan.getCount() == 0) {
            return null;
        }

        final int DEFAULT_Z_MIN = 1;
        final int DEFAULT_Z_MAX = stack.getSize();

        int xMin = stack.getWidth();
        int xMax = 0;
        int yMin = stack.getHeight();
        int yMax = 0;
        int zMin = DEFAULT_Z_MAX;
        int zMax = DEFAULT_Z_MIN;

        Roi[] rois = roiMan.getRoisAsArray();
        boolean allSlices = false;
        boolean noValidRois = true;

        for (Roi roi : rois) {
            Rectangle r = roi.getBounds();
            boolean valid = getSafeRoiBounds(r, stack.getWidth(), stack.getHeight());

            if (!valid) {
                continue;
            }
            
            xMin = Math.min(r.x, xMin);
            xMax = Math.max(r.x + r.width, xMax);
            yMin = Math.min(r.y, yMin);
            yMax = Math.max(r.y + r.height, yMax);

            int sliceNumber = roiMan.getSliceNumber(roi.getName());
            if (sliceNumber >= FIRST_SLICE_NUMBER && sliceNumber <= stack.getSize()) {
                zMin = Math.min(sliceNumber, zMin);
                zMax = Math.max(sliceNumber, zMax);
                noValidRois = false;
            } else if (isActiveOnAllSlices(sliceNumber)) {
                allSlices = true;
                noValidRois = false;
            }
        }

        if (noValidRois) {
            int defaultLimits[] = {0, stack.getWidth(), 0, stack.getHeight(), DEFAULT_Z_MIN, DEFAULT_Z_MAX};
            return  defaultLimits;
        }

        int[] limits = { xMin, xMax, yMin, yMax, zMin, zMax };

        if (allSlices) {
            limits[4] = DEFAULT_Z_MIN;
            limits[5] = DEFAULT_Z_MAX;
        }

        return limits;
    }

    private static boolean isActiveOnAllSlices(int sliceNumber)
    {
        return sliceNumber == NO_SLICE_NUMBER;
    }

    /**
     * Crops the given rectangle to the area [0, 0, width, height]
     *
     * @param bounds    The rectangle to be fitted
     * @param width     Maximum width of the rectangle
     * @param height    Maximum height of the rectangle
     * @return          false if the height or width of the fitted rectangle is 0
     *                  (Couldn't be cropped inside the area).
     */
    public static boolean getSafeRoiBounds(Rectangle bounds, int width, int height)
    {
        int xMin = Common.clamp(bounds.x, 0, width);
        int xMax = Common.clamp(bounds.x + bounds.width, 0, width);
        int yMin = Common.clamp(bounds.y, 0, height);
        int yMax = Common.clamp(bounds.y + bounds.height, 0, height);
        int newWidth = xMax - xMin;
        int newHeight = yMax - yMin;

        bounds.setBounds(xMin, yMin, newWidth, newHeight);
        
        return newWidth > 0 && newHeight > 0;
    }

    /**
     * Crop a stack to the limits of the ROIs in the ROI Manager and optionally
     * fill the background with a single pixel value.
     *
     * @param roiMan                The manager containing the ROIs
     * @param sourceStack           The image to be cropped
     * @param fillBackground        If true, fill the background of the cropped image
     * @param fillColor             Color of the background of the cropped image
     * @param padding               Number of pixels added to the each side of the resulting image
     * @return  A new image stack containing the cropped version of the given image.
     *          Returns null if roiMan is null or empty.
     *          Returns null if sourceStack == null
     *
     */
    public static ImageStack cropToRois(RoiManager roiMan, ImageStack sourceStack, boolean fillBackground,
                                        int fillColor, int padding)
    {
        int[] limits = getLimits(roiMan, sourceStack);

        if (limits == null) {
            return null;
        }

        final int xMin = limits[0];
        final int xMax = limits[1];
        final int yMin = limits[2];
        final int yMax = limits[3];
        final int zMin = limits[4];
        final int zMax = limits[5];

        final int croppedWidth = xMax - xMin + 2 * padding;
        final int croppedHeight = yMax - yMin + 2 * padding;

        ImageStack targetStack = new ImageStack(croppedWidth, croppedHeight);

        // copy
        ImageProcessor sourceProcessor;
        ImageProcessor targetProcessor;
        ArrayList<Roi> sliceRois;

        for (int sourceZ = zMin; sourceZ <= zMax; sourceZ++) {
            //@todo: check that ROIs with NO_SLICE_NUMBER work OK
            sliceRois = getSliceRoi(roiMan, sourceStack, sourceZ);
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

    /**
     * Copies pixels under all the ROIs on a slide
     *
     * @param sourceProcessor   The source image slide
     * @param targetProcessor   The target slide
     * @param sliceRois         List of all the ROIs on the source slide
     * @param padding           Number of pixels added on each side of the target slide
     */
    private static void copySlice(ImageProcessor sourceProcessor, ImageProcessor targetProcessor,
                                  ArrayList<Roi> sliceRois, int padding)
    {
        for (Roi sliceRoi : sliceRois) {
            Rectangle rectangle = sliceRoi.getBounds();
            boolean valid = getSafeRoiBounds(rectangle, sourceProcessor.getWidth(), sourceProcessor.getHeight());

            if (!valid) {
                continue;
            }

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
