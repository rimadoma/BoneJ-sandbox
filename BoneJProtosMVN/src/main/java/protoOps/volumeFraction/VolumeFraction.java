package protoOps.volumeFraction;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import ij.ImageStack;
import ij.measure.Calibration;
import ij.process.ImageProcessor;
import net.imagej.ops.Op;
import net.imagej.ops.OpEnvironment;

import org.bonej.common.ImageCheck;
import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import customnode.CustomTriangleMesh;
import ij.ImagePlus;
import ij.plugin.frame.RoiManager;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.awt.*;
import java.util.Arrays;

/**
 * @author Michael Doube
 * @author Richard Domander
 *
 * @todo RoiManager input @Parameter
 */
@Plugin(type = Op.class, name = "volumeFraction")
public class VolumeFraction implements Op {
	public static final int VOXEL_ALGORITHM = 0;
	public static final int SURFACE_ALGORITHM = 1;
	public static final int DEFAULT_VOLUME_ALGORITHM = VOXEL_ALGORITHM;
	public static final int DEFAULT_SURFACE_RESAMPLING = 6;

    // @todo make min & max threshold parameters
    // @todo write setters for thresholds
    private int minThreshold = 127;
    private int maxThreshold = 255;

    @Parameter(type = ItemIO.INPUT)
	private ImagePlus inputImage = null;

	@Parameter(type = ItemIO.INPUT, required = false, min = "0", max = "1")
	private int volumeAlgorithm = DEFAULT_VOLUME_ALGORITHM;

	@Parameter(type = ItemIO.INPUT, required = false, min = "0")
	private int surfaceResampling = DEFAULT_SURFACE_RESAMPLING;

	@Parameter(type = ItemIO.INPUT, required = false)
	private RoiManager roiManager = null;

    @Parameter(type = ItemIO.OUTPUT)
	private double foregroundVolume;

	@Parameter(type = ItemIO.OUTPUT)
	private double totalVolume;

	@Parameter(type = ItemIO.OUTPUT)
	private double volumeRatio;

	@Parameter(type = ItemIO.OUTPUT)
	CustomTriangleMesh foregroundSurface;

	@Parameter(type = ItemIO.OUTPUT)
	CustomTriangleMesh totalSurface;

    public VolumeFraction() {
        resetResults();
    }

    // region -- Getters --
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
    // endregion

    // region -- Setters --
	public void setImage(ImagePlus image) {
		checkImage(image);

		inputImage = image;

        setThresholds();
	}

    public void setSurfaceResampling(int resampling) {
		checkArgument(resampling >= 0, "Resampling value must be >= 0");

		surfaceResampling = resampling;
	}

	public void setVolumeAlgorithm(int algorithm) {
		checkArgument(algorithm == VOXEL_ALGORITHM || algorithm == SURFACE_ALGORITHM, "No such surface algorithm");

		volumeAlgorithm = algorithm;
	}

	public void setRoiManager(RoiManager roiManager) {
		checkNotNull(roiManager, "May not use a null ROI Manager");
		checkArgument(roiManager.getCount() != 0, "May not use an empty ROI Manager");

		this.roiManager = roiManager;
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

        setThresholds();
        resetResults();

        if (volumeAlgorithm == VOXEL_ALGORITHM) {
            volumeFractionVoxel();
        } else if (volumeAlgorithm == SURFACE_ALGORITHM) {
            volumeFractionSurface();
        }
	}

    // region -- Helper methods --
    private void volumeFractionSurface() {
        throw new NotImplementedException();
    }

    private void resetResults() {
        foregroundVolume = 0.0;
        totalVolume = 0.0;
        volumeRatio = Double.NaN;
        foregroundSurface = null;
        totalSurface = null;
    }

    /**
     * @todo Add support for ROIs
     */
    private void volumeFractionVoxel() {
        final ImageStack stack = inputImage.getStack();
        final int stackSize = stack.getSize();
        final long sliceTotalVolumes[] = new long[stackSize + 1];
        final long sliceForeGroundsVolumes[] = new long[stackSize + 1];

        for (int s = 1; s <= stackSize; s++) {
            ImageProcessor ipSlice = stack.getProcessor(s);
            ipSlice.setRoi(inputImage.getRoi()); // if getRoi == null, ROI will be set to (0, 0, width, height)
            calculateVoxelSliceVolumes(ipSlice, sliceTotalVolumes, sliceForeGroundsVolumes, s);
        }

        foregroundVolume = Arrays.stream(sliceForeGroundsVolumes).sum();
        totalVolume = Arrays.stream(sliceTotalVolumes).sum();
        calibrateVolumes();
    }

    private void calibrateVolumes() {
        Calibration calibration = inputImage.getCalibration();
        double volumeScale = calibration.pixelWidth * calibration.pixelHeight * calibration.pixelDepth;
        foregroundVolume *= volumeScale;
        totalVolume *= volumeScale;
        volumeRatio = foregroundVolume / totalVolume;
    }

    private void calculateVoxelSliceVolumes(ImageProcessor imageProcessor, long[] sliceTotalVolumes,
                                            long[] sliceForeGroundsVolumes, int sliceNumber) {
        final Rectangle r = imageProcessor.getRoi();
        final int rLeft = r.x;
        final int rTop = r.y;
        final int rRight = rLeft + r.width;
        final int rBottom = rTop + r.height;
        ImageProcessor mask = imageProcessor.getMask();
        final boolean hasMask = (mask != null);

        for (int y = rTop; y < rBottom; y++) {
            final int maskY = y - rTop;
            for (int x = rLeft; x < rRight; x++) {
                final int maskX = x - rLeft;
                if (!hasMask || mask.get(maskX, maskY) > 0) {
                    sliceTotalVolumes[sliceNumber]++;
                    final double pixel = imageProcessor.get(x, y);
                    if (pixel >= minThreshold && pixel <= maxThreshold) {
                        sliceForeGroundsVolumes[sliceNumber]++;
                    }
                }
            }
        }
    }

    private static void checkImage(ImagePlus image) {
		checkNotNull(image, "Must have an input image");

		int bitDepth = image.getBitDepth();
		checkArgument(bitDepth == 8 || bitDepth == 16, "Input image bit depth must be 8 or 16");

		checkArgument(ImageCheck.isBinary(image) || ImageCheck.isGrayscale(image), "Need a binary or grayscale image");
	}

    private void setThresholds() {
        if (ImageCheck.isBinary(inputImage)) {
            minThreshold = 127;
            maxThreshold = 255;
            return;
        }

        switch (inputImage.getType()) {
            case ImagePlus.GRAY8:
                minThreshold = 0;
                maxThreshold = 255;
                break;
            case ImagePlus.GRAY16:
                minThreshold = 0;
                maxThreshold = 65_535;
                break;
            default:
                throw new RuntimeException("Bad image type, Execution shouldn't go here!");
        }
    }
    // endregion
}
