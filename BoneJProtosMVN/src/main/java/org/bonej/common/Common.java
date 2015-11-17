package org.bonej.common;

import ij.ImagePlus;
import ij.ImageStack;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author <a href="mailto:rdomander@rvc.ac.uk">Richard Domander</a>
 */
public class Common {
    public static final int BINARY_BLACK = 0x00;
    public static final int BINARY_WHITE = 0xFF;

    public static final String ANISOTROPY_WARNING = "This image contains anisotropic voxels, which will\n"
            + "result in incorrect thickness calculation.\n\n"
            + "Consider rescaling your data so that voxels are isotropic\n"
            + "(Image > Scale...).\n\n" + "Continue anyway?";

    public static double clamp(double value, double min, double max)
    {
        if (Double.compare(value, min) < 0) {
            return min;
        }
        if (Double.compare(value, max) > 0) {
            return max;
        }
        return value;
    }

    public static int clamp(int value, int min, int max)
    {
        if (Integer.compare(value, min) < 0) {
            return min;
        }
        if (Integer.compare(value, max) > 0) {
            return max;
        }
        return value;
    }

    /**
     * Sets the value of the background pixels to Float.NaN
     *
     * @param image             A 32-bit floating point image
     * @param backgroundColor   The color used to identify background pixels (usually 0.0f)
     */
    public static void backgroundToNaN(ImagePlus image, float backgroundColor)
    {
        checkNotNull(image, "Image must not be null");
        checkArgument(image.getBitDepth() == 32, "Not a 32-bit image");

        final int depth = image.getNSlices();
        final int pixelsPerSlice = image.getWidth() * image.getHeight();
        final ImageStack stack = image.getStack();

        for (int z = 1; z <= depth; z++) {
            float pixels[] = (float[]) stack.getPixels(z);
            for (int i = 0; i < pixelsPerSlice; i++) {
                if (Float.compare(pixels[i], backgroundColor) == 0) {
                    pixels[i] = Float.NaN;
                }
            }
        }
    }

    /**
     * Multiplies all pixel values of the given image by image.getCalibration().pixelWidth
     *
     * @param image A 32-bit floating point image
     *
     * Handy when the pixel values of an image represent some measurement.
     * For example the pixel values of a local thickness map represent the thickness of the sample in that location.
     * Multiplying the pixel values by pixel width then gives you the thickness in real units, e.g. millimetres.
     */

    public static void pixelValuesToCalibratedValues(ImagePlus image)
    {
        checkNotNull(image, "Image must not be null");
        checkArgument(image.getBitDepth() == 32, "Not a 32-bit image");

        double pixelWidth = image.getCalibration().pixelWidth;
        ImageStack stack = image.getStack();
        final int depth = stack.getSize();

        for (int z = 1; z <= depth; z++) {
            stack.getProcessor(z).multiply(pixelWidth);
        }
    }
}
