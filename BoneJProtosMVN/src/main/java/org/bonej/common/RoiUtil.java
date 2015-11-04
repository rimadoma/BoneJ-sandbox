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

        final int xMin = Common.clamp(limits[0], 0, sourceStack.getWidth());
        final int xMax = Common.clamp(limits[1], 0, sourceStack.getWidth());
        final int yMin = Common.clamp(limits[2], 0, sourceStack.getHeight());
        final int yMax = Common.clamp(limits[3], 0, sourceStack.getHeight());
        final int zMin = Common.clamp(limits[4], 1, sourceStack.getSize());
        final int zMax = Common.clamp(limits[5], 1, sourceStack.getSize());

        final int croppedWidth = xMax - xMin + 2 * padding;
        final int croppedHeight = yMax - yMin + 2 * padding;
        final int croppedDepth = zMax - zMin + 2 * padding + 1;

        if (croppedWidth <= 0 || croppedHeight <= 0 || croppedDepth <= 0) {
            return null;
        }

        ImageStack targetStack = new ImageStack(croppedWidth, croppedHeight);

        // copy
        ImageProcessor sourceProcessor;
        ImageProcessor targetProcessor;
        ArrayList<Roi> sliceRois;
        boolean slicesCopied = false;
        for (int sourceZ = zMin; sourceZ <= zMax; sourceZ++) {
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
            boolean copyOk = copySlice(sourceProcessor, targetProcessor, sliceRois, padding);
            slicesCopied |= copyOk;
            if (copyOk) {
                targetStack.addSlice("", targetProcessor);
            }
        }

        if (!slicesCopied) {
            return null;
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

    private static boolean copySlice(ImageProcessor sourceProcessor, ImageProcessor targetProcessor,
                                            ArrayList<Roi> sliceRois, int padding)
    {
        for (Roi sliceRoi : sliceRois) {
            Rectangle rectangle = sliceRoi.getBounds();
            int minY = rectangle.y;
            int minX = rectangle.x;
            int maxY = rectangle.y + rectangle.height;
            int maxX = rectangle.x + rectangle.width;

            int boundsType = checkRoiBounds(sourceProcessor, minX, minY, maxX, maxY);
            if (boundsType == OUT_OF_BOUNDS) {
                return false;
            } else if (boundsType == PARTLY_OUT) {
                int yLimit = targetProcessor.getHeight();
                int xLimit = targetProcessor.getWidth();
                minY = Common.clamp(minY, 0, yLimit);
                minX = Common.clamp(minX, 0, xLimit);
                maxY = Common.clamp(maxY, 0, yLimit);
                maxX = Common.clamp(maxX, 0, xLimit);
            }

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

        return true;
    }

    private static final int WITHIN_BOUNDS = 1;
    private static final int PARTLY_OUT = 0;
    private static final int OUT_OF_BOUNDS = -1;

    private static int checkRoiBounds(ImageProcessor sourceProcessor, int minX, int minY, int maxX, int maxY) {
        int width = sourceProcessor.getWidth();
        int height = sourceProcessor.getHeight();

        int xBounds = checkDimensionBounds(minX, maxX, 0, width);
        int yBounds = checkDimensionBounds(minY, maxY, 0, height);

        if (xBounds == WITHIN_BOUNDS) {
            if (yBounds == WITHIN_BOUNDS) {
                return WITHIN_BOUNDS;
            } else if (yBounds == PARTLY_OUT) {
                return PARTLY_OUT;
            }

            return OUT_OF_BOUNDS;
        } else if (xBounds == PARTLY_OUT) {
            if ((yBounds == PARTLY_OUT) || (yBounds == WITHIN_BOUNDS)) {
                return PARTLY_OUT;
            } else {
                return OUT_OF_BOUNDS;
            }
        }

        return OUT_OF_BOUNDS;
    }

    //@pre p1 < p2
    private static int checkDimensionBounds(int p1, int p2, int min, int max) {
        if (p1 < min) {
            return p2 < min ? OUT_OF_BOUNDS : PARTLY_OUT;
        } else if (p1 < max) {
            return p2 < max ? WITHIN_BOUNDS : PARTLY_OUT;
        }

        return OUT_OF_BOUNDS;
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
        final int d = zmax - zmin + 2 * padding + 1;

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
                        if (mask == null || mask.get(x, y) > 0) {
                            int sourceColor = ipSource.get(x, y);
                            ip.set(x + xOff, yyOff, sourceColor);
                        }
                    }
                }
            }
            out.addSlice(stack.getSliceLabel(z - zOff), ip);
        }

        return out;
    }
}
