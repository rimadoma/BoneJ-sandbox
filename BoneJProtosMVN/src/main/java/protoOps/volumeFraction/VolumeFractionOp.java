package protoOps.volumeFraction;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import net.imagej.ops.Op;

import org.bonej.common.ImageCheck;
import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;

import ij.ImagePlus;
import ij.plugin.frame.RoiManager;

import java.util.Optional;

/**
 * An interface for Ops which measure the volume of foreground elements over the total volume of the sample
 *
 * @author Richard Domander
 */
public abstract class VolumeFractionOp implements Op {
    @Parameter(type = ItemIO.INPUT)
    private ImagePlus inputImage;

    @Parameter(type = ItemIO.INPUT)
    private int minThreshold;

    @Parameter(type = ItemIO.INPUT)
    private int maxThreshold;

    @Parameter(type = ItemIO.INPUT, required = false)
    private RoiManager roiManager;

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

    public Optional<ImagePlus> getImage() {
        return Optional.ofNullable(inputImage);
    }

    public Optional<RoiManager> getRoiManager() {
        return Optional.ofNullable(roiManager);
    }
    //endregion

    //region -- Setters --
    public void setForegroundVolume(final double volume) {
        foregroundVolume = volume;
    }

    public void setTotalVolume(final double volume) {
        totalVolume = volume;
    }

    public final void setVolumeRatio() {
        volumeRatio = foregroundVolume / totalVolume;
    }

    /**
     * Sets the input image for the Op
     *
     * @throws NullPointerException if image is null
     * @throws IllegalArgumentException if image is incompatible
     */
    public void setImage(final ImagePlus image) throws NullPointerException, IllegalArgumentException {
        checkImage(image);

        inputImage = image;

        initThresholds();
    }

    /**
     * Sets the RoiManager used the limit the area of the volume calculations
     *
     * @throws NullPointerException if roiManager is null
     * @throws IllegalArgumentException if roiManager is empty
     */
    public void setRoiManager(final RoiManager roiManager) throws NullPointerException, IllegalArgumentException  {
        checkNotNull(roiManager, "May not use a null ROI Manager");
        checkArgument(roiManager.getCount() != 0, "May not use an empty ROI Manager");

        this.roiManager = roiManager;
    }


    /**
     * Sets the lower and upper values used for thresholding the input image
     *
     * @throws NullPointerException if inputImage == null (max threshold value is determined by the type of the image)
     * @throws IllegalArgumentException if either threshold is below the minimum pixel value or above the maximum
     */
    public void setThresholds(final int min, final int max) throws NullPointerException, IllegalArgumentException {
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

    //region -- Utility methods --
    /**
     * Checks if the given image can be used by the VolumeFraction Op
     *
     * @param image The image for the Op
     * @throws NullPointerException if image == null
     * @throws IllegalArgumentException if image is unsuitable for the Op
     */
    public static void checkImage(final ImagePlus image) throws NullPointerException, IllegalArgumentException{
        checkNotNull(image, "Must have an input image");

        final int bitDepth = image.getBitDepth();
        checkArgument(bitDepth == 8 || bitDepth == 16, "Input image must be 8-bit or 16-bit");

        checkArgument(ImageCheck.isBinary(image) || ImageCheck.isGrayscale(image), "Need a binary or grayscale image");
    }
    //endregion

    //region -- Internal methods --
    /**
     * Check that input @Parameters are valid.
     *
     * Needed when the Op is run via an opService, and the setter methods have not been called
     */
    protected final void checkInputs() throws NullPointerException, IllegalArgumentException{
        checkImage(inputImage);
        checkThresholds(minThreshold, maxThreshold);
    }
    //endregion

    //region -- Helper methods --
    private void checkThresholds(final int min, final int max) throws NullPointerException, IllegalArgumentException {
        checkNotNull(inputImage, "Cannot determine threshold values without an image");

        int thresholdUpperBound;

        switch (inputImage.getType()) {
            case ImagePlus.GRAY8:
                thresholdUpperBound = 0xFF;
                break;
            case ImagePlus.GRAY16:
                thresholdUpperBound = 0xFFFF;
                break;
            default:
                throw new AssertionError("Input image has wrong type");
        }

        checkArgument(0 <= min && min <= thresholdUpperBound, "Min threshold out of bounds");
        checkArgument(0 <= max && max <= thresholdUpperBound, "Max threshold out of bounds");
        checkArgument(min <= max, "Minimum threshold must be less or equal to maximum threshold");
    }

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
            default:
                throw new AssertionError("Input image has wrong type");
        }
    }
    //endregion
}