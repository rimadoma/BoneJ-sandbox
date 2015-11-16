package org.bonej.common;

import ij.ImagePlus;
import ij.ImageStack;

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
        if (image == null) {
            return;
        }

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
     * Calculates the standard deviation of the pixel values, when they are used to represent some measurement.
     * For example, the method is handy for LocalThickness, where pixel values represent the thickness of the sample
     * at that point.
     * The units of the measurement are determined from the calibration of the image.
     *
     * @param image                 A 32-bit floating point image
     * @param calibratedMeanValue   The mean value of the pixels divided by pixelWidth
     * @return                      The scaled standard deviation of the pixel values
     *                              Returns Double.MIN_VALUE if image == null, or if there are no foreground pixels
     *
     * The image is assumed to be isotropic (pixelWidth == pixelHeight).
     */
    public static double calibratedStandardDeviation(ImagePlus image, double calibratedMeanValue)
    {
        if (image == null) {
            return Double.MIN_VALUE;
        }

        final int w = image.getWidth();
        final int h = image.getHeight();
        final int d = image.getStackSize();
        final int wh = w * h;
        final ImageStack stack = image.getStack();
        long pixCount = 0;

        float pixelWidth = (float) image.getCalibration().pixelWidth;

        double sumSquares = 0;
        for (int s = 1; s <= d; s++) {
            final float[] slicePixels = (float[]) stack.getPixels(s);
            for (int p = 0; p < wh; p++) {
                final float pixVal = slicePixels[p] * pixelWidth;
                if (!Float.isNaN(pixVal)) {
                    final double residual = (pixVal - calibratedMeanValue);
                    sumSquares += residual * residual;
                    pixCount++;
                }
            }
        }

        if (pixCount == 0) {
            return Double.MIN_VALUE;
        }

        final double stDev = Math.sqrt(sumSquares / pixCount);
        return stDev;
    }
}
