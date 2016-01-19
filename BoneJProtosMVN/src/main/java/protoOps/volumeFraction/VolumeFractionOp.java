package protoOps.volumeFraction;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import net.imagej.ops.Op;

import org.bonej.common.ImageCheck;

import ij.ImagePlus;
import ij.plugin.frame.RoiManager;

/**
 * An interface for Ops which measure the volume of foreground elements over the total volume of the sample
 *
 * @author Richard Domander
 */
public interface VolumeFractionOp extends Op {
    /**
     * Checks if the given image can be used by the VolumeFraction Op
     *
     * @param image The image for the Op
     * @throws NullPointerException if image == null
     * @throws IllegalArgumentException if image is unsuitable for the Op
     */
    static void checkImage(ImagePlus image) {
        checkNotNull(image, "Must have an input image");

        int bitDepth = image.getBitDepth();
        checkArgument(bitDepth == 8 || bitDepth == 16, "Input image bit depth must be 8 or 16");

        checkArgument(ImageCheck.isBinary(image) || ImageCheck.isGrayscale(image), "Need a binary or grayscale image");
    }
    int getMinThreshold();
    int getMaxThreshold();
    double getForegroundVolume();
    double getTotalVolume();
    double getVolumeRatio();
    void setImage(ImagePlus image);
    void setThresholds(int min, int max);
    void setRoiManager(RoiManager roiManager);
    boolean needThresholds();
}