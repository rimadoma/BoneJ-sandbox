package org.bonej.common;

import java.awt.*;
import java.util.ArrayList;
import java.util.Optional;

import javax.annotation.Nullable;

import ij.ImageStack;
import ij.gui.Roi;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;

/**
 * A class containing utility methods for a ImageJ RoiManager
 *
 * @author Michael Doube
 * @author Richard Domander
 */
public class RoiUtil {
	private static final int FIRST_SLICE_NUMBER = 1;
	private static final int NO_SLICE_NUMBER = -1;

    //region -- Utility methods --
	/**
	 * Returns a list of ROIs that are active in the given slice.
	 *
	 * @param roiMan
	 *            The collection of all the current ROIs
	 * @param sliceNumber
	 *            Number of the slice to be searched
	 * @return In addition to the active ROIs, returns all the ROIs without a
	 *         slice number (assumed to be active in all slices). Return an
	 *         empty list if sliceNumber is out of bounds, or roiMan == null or
	 *         stack == null
	 *
	 */
	public static ArrayList<Roi> getSliceRoi(@Nullable final RoiManager roiMan, @Nullable final ImageStack stack,
                                             final int sliceNumber) {
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
	 * Find the x, y and z limits of the stack defined by the ROIs in the ROI Manager
	 *
     * @implNote If for any ROI isActiveOnAllSlices == true, then z0 == 1 and z1 == stack.getSize().
	 * @param roiMan
	 *            The collection of all the current ROIs
	 * @param stack
	 *            The stack inside which the ROIs must fit (max limits).
	 * @return  Returns an Optional with the limits in an int array {x0, x1, y0, y1, z0, z1}.
     *          Returns an empty Optional if roiMan == null or stack == null or roiMan is empty
	 */
	public static Optional<int[]> getLimits(@Nullable final RoiManager roiMan, @Nullable final ImageStack stack) {
		if (roiMan == null || stack == null) {
			return Optional.empty();
		}

		if (roiMan.getCount() == 0) {
			return Optional.empty();
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
			return Optional.empty();
		}

		int[] limits = {xMin, xMax, yMin, yMax, zMin, zMax};

		if (allSlices) {
			limits[4] = DEFAULT_Z_MIN;
			limits[5] = DEFAULT_Z_MAX;
		}

		return Optional.of(limits);
	}

    /**
	 * Crops the given rectangle to the area [0, 0, width, height]
	 *
	 * @param bounds
	 *            The rectangle to be fitted
	 * @param width
	 *            Maximum width of the rectangle
	 * @param height
	 *            Maximum height of the rectangle
	 * @return false if the height or width of the fitted rectangle is 0
	 *         (Couldn't be cropped inside the area).
	 */
	public static boolean getSafeRoiBounds(final Rectangle bounds, final int width, final int height) {
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
	 * Same as @see RoiUtil.cropToRois, but with default padding of 0.
	 */
	public static Optional<ImageStack> cropToRois(final RoiManager roiMan, final ImageStack sourceStack,
                                                  final boolean fillBackground, final int fillColor) {
		return cropToRois(roiMan, sourceStack, fillBackground, fillColor, 0);
	}

	/**
	 * Crop a stack to the limits defined by the ROIs in the ROI Manager and optionally
	 * fill the background with a single pixel value.
	 *
	 * @param roiMan
	 *            The manager containing the ROIs
	 * @param sourceStack
	 *            The image to be cropped
	 * @param fillBackground
	 *            If true, fill the background of the cropped image
	 * @param fillColor
	 *            Color of the background of the cropped image
	 * @param padding
	 *            Number of pixels added to the each side of the resulting image
     * @return  An Optional with the cropped stack of the given image.
     *          The Optional is empty if roiMan == null, or sourceStack == null, or roiMan is empty
	 */
	public static Optional<ImageStack> cropToRois(@Nullable final RoiManager roiMan,
                                                  @Nullable final ImageStack sourceStack, final boolean fillBackground,
                                                  final int fillColor, final int padding) {
        if (roiMan == null || sourceStack == null) {
            return Optional.empty();
        }

        Optional<int[]> optionalLimits = getLimits(roiMan, sourceStack);
        if (!optionalLimits.isPresent()) {
            return Optional.empty();
        }

		int[] limits = optionalLimits.get();

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
			targetStack.addSlice("", targetProcessor.duplicate(), 0);
			targetStack.addSlice(targetProcessor.duplicate());
		}

		return Optional.of(targetStack);
	}
    //endregion

    //region -- Helper methods --
	/**
	 * Copies pixels under all the ROIs on a slide
	 *
	 * @param sourceProcessor
	 *            The source image slide
	 * @param targetProcessor
	 *            The target slide
	 * @param sliceRois
	 *            List of all the ROIs on the source slide
	 * @param padding
	 *            Number of pixels added on each side of the target slide
	 */
	private static void copySlice(final ImageProcessor sourceProcessor, final ImageProcessor targetProcessor,
			final ArrayList<Roi> sliceRois, final int padding) {
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
	 * Copies the pixels in the given ROI from the source image to the target
	 * image. Copies only those pixels where the color of the given mask > 0.
	 *
     * @implNote Calls copyRoi with the given parameters if sourceProcessor.getMask() == null
	 * @param sourceProcessor
	 *            Copy source
	 * @param targetProcessor
	 *            Copy target
	 * @param minX
	 *            Horizontal start of the copy area 0 <= minX < width
	 * @param minY
	 *            Vertical start of the copy area 0 <= minY < height
	 * @param maxX
	 *            Horizontal end of the copy area 0 <= maxX <= width
	 * @param maxY
	 *            Vertical end of the copy area 0 <= maxY <= height
	 * @param padding
	 *            Number pixels added to each side of the copy target
	 */
	private static void copyRoiWithMask(final ImageProcessor sourceProcessor, final ImageProcessor targetProcessor,
                                       final int minX, final int minY, final int maxX, final int maxY,
                                       final int padding) {
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
	 * Copies the pixels in the given ROI from the source image to the target
	 * image.
	 *
	 * @param sourceProcessor
	 *            Copy source
	 * @param targetProcessor
	 *            Copy target
	 * @param minX
	 *            Horizontal start of the copy area 0 <= minX < width
	 * @param minY
	 *            Vertical start of the copy area 0 <= minY < height
	 * @param maxX
	 *            Horizontal end of the copy area 0 <= maxX <= width
	 * @param maxY
	 *            Vertical end of the copy area 0 <= maxY <= height
	 * @param padding
	 *            Number pixels added to each side of the copy target
	 * @pre sourceProcessor != null
	 * @pre targetProcessor != null
	 */
	private static void copyRoi(ImageProcessor sourceProcessor, ImageProcessor targetProcessor, final int minX,
			final int minY, final int maxX, final int maxY, final int padding) {
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

    private static boolean isActiveOnAllSlices(final int sliceNumber) {
        return sliceNumber == NO_SLICE_NUMBER;
    }
    //endregion
}
