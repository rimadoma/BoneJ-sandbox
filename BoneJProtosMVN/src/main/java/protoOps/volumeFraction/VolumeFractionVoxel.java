package protoOps.volumeFraction;

import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Roi;
import ij.measure.Calibration;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;
import net.imagej.ops.Op;
import net.imagej.ops.OpEnvironment;
import org.bonej.common.ImageCheck;
import org.bonej.common.RoiUtil;
import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An Op which calculates the volumes of the sample by counting the voxels in the image
 *
 * @todo add thresholding
 * @author Michael Doube
 * @author Richard Domander
 */
@Plugin(type = Op.class, name = "volumeFractionVoxel")
public class VolumeFractionVoxel implements VolumeFractionOp {
    @Parameter(type = ItemIO.INPUT)
	private ImagePlus inputImage = null;

	@Parameter(type = ItemIO.INPUT, required = false)
	private RoiManager roiManager = null;

    @Parameter(type = ItemIO.OUTPUT)
	private double foregroundVolume;

	@Parameter(type = ItemIO.OUTPUT)
	private double totalVolume;

	@Parameter(type = ItemIO.OUTPUT)
	private double volumeRatio;

    // @todo make min & max threshold parameters
    private int minThreshold = 128;
    private int maxThreshold = 255;
    private int thresholdBound = 0xFF;

    public VolumeFractionVoxel() {
        reset();
    }

    // region -- Getters --
    @Override
    public double getForegroundVolume() {
        return foregroundVolume;
    }

    @Override
    public double getTotalVolume() {
        return totalVolume;
    }

    @Override
    public double getVolumeRatio() {
        return volumeRatio;
    }

    @Override
    public int getMinThreshold() {
        return minThreshold;
    }

    @Override
    public int getMaxThreshold() {
        return maxThreshold;
    }
    // endregion

    // region -- Setters --
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
        checkArgument(0 <= min && min <= thresholdBound, "Min threshold out of bounds");
        checkArgument(0 <= max && max <= thresholdBound, "Max threshold out of bounds");
        checkArgument(min <= max, "Minimum threshold must be less or equal to maximum threshold");

        minThreshold = min;
        maxThreshold = max;
    }
    // endregion

	@Override
	public OpEnvironment ops() {
		return null;
	}

	@Override
	public void setEnvironment(OpEnvironment opEnvironment) {

	}

	@Override
	public void run() {
        checkImage(inputImage);

        volumeFractionVoxel();
	}

    @Override
    public boolean needThresholds() {
        return !ImageCheck.isBinary(inputImage);
    }

    public void reset() {
        roiManager = null;
        foregroundVolume = 0.0;
        totalVolume = 0.0;
        volumeRatio = Double.NaN;
    }

    // region -- Helper methods --

    private void volumeFractionVoxel() {
        final ImageStack stack = inputImage.getStack();
        final int stackSize = stack.getSize();
        final long sliceTotalVolumes[] = new long[stackSize + 1];
        final long sliceForeGroundsVolumes[] = new long[stackSize + 1];

        if (roiManager == null) {
            voxelVolumeWithNoRois(stack, sliceTotalVolumes, sliceForeGroundsVolumes);
        } else {
            voxelVolumeWithRois(stack, sliceTotalVolumes, sliceForeGroundsVolumes);
        }

        foregroundVolume = Arrays.stream(sliceForeGroundsVolumes).sum();
        totalVolume = Arrays.stream(sliceTotalVolumes).sum();
        calibrateVolumes();
        volumeRatio = foregroundVolume / totalVolume;
    }

    private void voxelVolumeWithNoRois(ImageStack stack, long[] sliceTotalVolumes, long[] sliceForeGroundsVolumes) {
        final Roi defaultRoi = new Roi(0, 0, stack.getWidth(), stack.getHeight());
        final int stackSize = stack.getSize();

        for (int slice = 1; slice <= stackSize; slice++) {
            ImageProcessor processor = stack.getProcessor(slice);
            calculateVoxelSliceVolumes(processor, defaultRoi, sliceTotalVolumes, sliceForeGroundsVolumes, slice);
        }
    }

    private void voxelVolumeWithRois(ImageStack stack, long[] sliceTotalVolumes, long[] sliceForeGroundsVolumes) {
        final int stackSize = stack.getSize();

        for (int slice = 1; slice <= stackSize; slice++) {
            ArrayList<Roi> rois = RoiUtil.getSliceRoi(roiManager, stack, slice);

            if (rois.isEmpty()) {
                continue;
            }

            ImageProcessor processor = stack.getProcessor(slice);

            for (Roi roi : rois) {
                calculateVoxelSliceVolumes(processor, roi, sliceTotalVolumes, sliceForeGroundsVolumes, slice);
            }
        }
    }

    private void calculateVoxelSliceVolumes(ImageProcessor processor, Roi roi, long[] sliceTotalVolumes,
                                            long[] sliceForeGroundsVolumes, int sliceNumber) {
        processor.setRoi(roi);
        if (processor.getMask() != null) {
            calculateVoxelSliceVolumesWithMask(processor, sliceTotalVolumes, sliceForeGroundsVolumes, sliceNumber);
        } else {
            calculateVoxelSliceVolumesWithNoMask(processor, sliceTotalVolumes, sliceForeGroundsVolumes, sliceNumber);
        }
    }

    private void calculateVoxelSliceVolumesWithMask(ImageProcessor imageProcessor, long[] sliceTotalVolumes,
                                                    long[] sliceForegroundVolumes, int sliceNumber) {
        final Rectangle r = imageProcessor.getRoi();
        final int x0 = r.x;
        final int y0 = r.y;
        final int x1 = x0 + r.width;
        final int y1 = y0 + r.height;
        ImageProcessor mask = imageProcessor.getMask();

        for (int y = y0; y < y1; y++) {
            final int maskY = y - y0;
            for (int x = x0; x < x1; x++) {
                final int maskX = x - x0;
                if (mask.get(maskX, maskY) == 0) {
                    continue;
                }

                sliceTotalVolumes[sliceNumber]++;
                final int pixel = imageProcessor.get(x, y);
                if (pixel >= minThreshold && pixel <= maxThreshold) {
                    sliceForegroundVolumes[sliceNumber]++;
                }
            }
        }
    }

    private void calculateVoxelSliceVolumesWithNoMask(ImageProcessor imageProcessor, long[] sliceTotalVolumes,
                                                      long[] sliceForeGroundsVolumes, int sliceNumber) {
        final Rectangle r = imageProcessor.getRoi();
        final int x0 = r.x;
        final int y0 = r.y;
        final int x1 = x0 + r.width;
        final int y1 = y0 + r.height;

        for (int y = y0; y < y1; y++) {
            for (int x = x0; x < x1; x++) {
                final int pixel = imageProcessor.get(x, y);
                if (pixel >= minThreshold && pixel <= maxThreshold) {
                    sliceForeGroundsVolumes[sliceNumber]++;
                }
            }
        }

        sliceTotalVolumes[sliceNumber] = imageProcessor.getPixelCount();
    }

    private void calibrateVolumes() {
        Calibration calibration = inputImage.getCalibration();
        double volumeScale = calibration.pixelWidth * calibration.pixelHeight * calibration.pixelDepth;
        foregroundVolume *= volumeScale;
        totalVolume *= volumeScale;
    }

    private static void checkImage(ImagePlus image) {
		checkNotNull(image, "Must have an input image");

		int bitDepth = image.getBitDepth();
		checkArgument(bitDepth == 8 || bitDepth == 16, "Input image bit depth must be 8 or 16");

		checkArgument(ImageCheck.isBinary(image) || ImageCheck.isGrayscale(image), "Need a binary or grayscale image");
	}

    private void initThresholds() {
        switch (inputImage.getType()) {
            case ImagePlus.GRAY8:
                minThreshold = 128;
                maxThreshold = 255;
                thresholdBound = 0xFF;
                break;
            case ImagePlus.GRAY16:
                minThreshold = 2424;
                maxThreshold = 11_215;
                thresholdBound = 0xFFFF;
                break;
            default:
                throw new RuntimeException("Bad image type, Execution shouldn't go here!");
        }
    }
    // endregion
}
