package protoOps.volumeFraction;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import net.imagej.ops.Op;

import org.bonej.common.ImageCheck;
import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;

import ij.ImagePlus;
import ij.plugin.frame.RoiManager;

/**
 * An interface for Ops which measure the volume of foreground elements over the total volume of the sample
 *
 * @author Richard Domander
 */
public abstract class VolumeFractionOp implements Op {
    @Parameter(type = ItemIO.INPUT)
    private ImagePlus inputImage = null;

    @Parameter(type = ItemIO.INPUT)
    private int minThreshold;

    @Parameter(type = ItemIO.INPUT)
    private int maxThreshold;

    @Parameter(type = ItemIO.INPUT, required = false)
    private RoiManager roiManager = null;

    @Parameter(type = ItemIO.OUTPUT)
    private double foregroundVolume;

    @Parameter(type = ItemIO.OUTPUT)
    private double totalVolume;

    @Parameter(type = ItemIO.OUTPUT)
    private double volumeRatio = Double.NaN;

    //region -- Getters --
    public double getForegroundVolume() {
        return foregroundVolume;
    }

    public double getTotalVolume() {
        return totalVolume;
    }

    public double getVolumeRatio() {
        return volumeRatio;
    }

    public int getMinThreshold() {
        return minThreshold;
    }

    public int getMaxThreshold() {
        return maxThreshold;
    }

    public ImagePlus getImage() {
        return inputImage;
    }

    public RoiManager getRoiManager() {
        return roiManager;
    }
    //endregion

    //region -- Setters --
    public void setForegroundVolume(double volume) {
        foregroundVolume = volume;
    }

    public void setTotalVolume(double volume) {
        totalVolume = volume;
    }

    public final void setVolumeRatio() {
        volumeRatio = foregroundVolume / totalVolume;
    }

    public void setImage(ImagePlus image) {
        checkImage(image);

        inputImage = image;

        initThresholds();
    }

    public void setRoiManager(RoiManager roiManager) {
        checkNotNull(roiManager, "May not use a null ROI Manager");
        checkArgument(roiManager.getCount() != 0, "May not use an empty ROI Manager");

        this.roiManager = roiManager;
    }

    public void setThresholds(int min, int max) {
        checkThresholds(min, max);

        minThreshold = min;
        maxThreshold = max;
    }
    //endregion

    /**
     * Checks if min & max threshold need to be set before run()
     *
     * @return  true if the plugin needs thresholds as inputs,
     *          false if thresholds can be automatically determined
     */
    public boolean needThresholds() {
        return !ImageCheck.isBinary(inputImage);
    }

    public void reset() {
        roiManager = null;
        foregroundVolume = 0.0;
        totalVolume = 0.0;
        volumeRatio = Double.NaN;
    }

    public void checkThresholds(int min, int max) {
        checkNotNull(inputImage, "Cannot determine threshold values without an image");

        int thresholdUpperBound = 0x00;

        switch (inputImage.getType()) {
            case ImagePlus.GRAY8:
                thresholdUpperBound = 0xFF;
                break;
            case ImagePlus.GRAY16:
                thresholdUpperBound = 0xFFFF;
                break;
        }

        checkArgument(0 <= min && min <= thresholdUpperBound, "Min threshold out of bounds");
        checkArgument(0 <= max && max <= thresholdUpperBound, "Max threshold out of bounds");
        checkArgument(min <= max, "Minimum threshold must be less or equal to maximum threshold");
    }

    //region -- Utility methods --
    /**
     * Checks if the given image can be used by the VolumeFraction Op
     *
     * @param image The image for the Op
     * @throws NullPointerException if image == null
     * @throws IllegalArgumentException if image is unsuitable for the Op
     */
    public static void checkImage(ImagePlus image) {
        checkNotNull(image, "Must have an input image");

        int bitDepth = image.getBitDepth();
        checkArgument(bitDepth == 8 || bitDepth == 16, "Input image must be 8-bit or 16-bit");

        checkArgument(ImageCheck.isBinary(image) || ImageCheck.isGrayscale(image), "Need a binary or grayscale image");
    }
    //endregion

    //region -- Internal methods --
    protected final void checkImage() {
        checkImage(inputImage);
    }

    protected final void checkThresholds() {
        checkThresholds(minThreshold, maxThreshold);
    }

    /**
     * Check that input @Parameters are valid.
     *
     * Needed when the Op is run via an opService, and the setter methods have not been called before calling run()
     */
    protected void checkOpInputs() {
        checkImage();
        checkThresholds();
    }
    //endregion

    //region -- Helper methods --
    /**
     * Sets the initial values for min & max threshold based on the type of the input image
     */
    private void initThresholds() {
        switch (inputImage.getType()) {
            case ImagePlus.GRAY8:
                minThreshold = 128;
                maxThreshold = 255;
                break;
            case ImagePlus.GRAY16:
                minThreshold = 2424;
                maxThreshold = 11_215;
                break;
        }
    }
    //endregion
}