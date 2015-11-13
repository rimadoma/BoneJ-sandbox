package org.bonej.common;

import ij.ImagePlus;
import ij.ImageStack;

/**
 * @author <a href="mailto:rdomander@rvc.ac.uk">Richard Domander</a>
 * @date 09/11/15
 */
public class StackStatistics
{

    /**
     * @param image A 32-bit floating point image
     * @return
     */
    public static double[] standardDeviation(ImagePlus image)
    {
        final int w = image.getWidth();
        final int h = image.getHeight();
        final int d = image.getStackSize();
        final int wh = w * h;
        final ImageStack stack = image.getStack();
        long pixCount = 0;
        double sumThick = 0;
        double maxThick = 0;

        for (int s = 1; s <= d; s++) {
            final float[] slicePixels = (float[]) stack.getPixels(s);
            for (int p = 0; p < wh; p++) {
                final double pixVal = slicePixels[p];
                if (pixVal > 0) {
                    sumThick += pixVal  / 127.0;
                    maxThick = Math.max(maxThick, pixVal / 127.0);
                    pixCount++;
                }
            }
        }
        final double meanThick = sumThick / pixCount;

        double sumSquares = 0;
        for (int s = 1; s <= d; s++) {
            final float[] slicePixels = (float[]) stack.getPixels(s);
            for (int p = 0; p < wh; p++) {
                final double pixVal = slicePixels[p];
                if (pixVal > 0) {
                    final double residual = (meanThick - pixVal) / 127.0;
                    sumSquares += residual * residual;
                }
            }
        }
        final double stDev = Math.sqrt(sumSquares / pixCount);
        double[] stats = { meanThick, stDev, maxThick };
        return stats;
    }
}
