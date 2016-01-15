package protoOps.volumeFraction;

import net.imagej.ops.Op;
import ij.ImagePlus;
import ij.plugin.frame.RoiManager;

/**
 * An interface for Ops which measure the volume of foreground elements over the total volume of the sample
 *
 * @author Richard Domander
 */
public interface VolumeFractionOp extends Op {
    int getMinThreshold();
    int getMaxThreshold();
    double getForegroundVolume();
    double getTotalVolume();
    double getVolumeRatio();
    void setImage(ImagePlus image);
    void setRoiManager(RoiManager roiManager);
}