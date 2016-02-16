package protoOps.volumeFraction;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.IntStream;

import net.imagej.ops.Op;
import net.imagej.ops.OpEnvironment;

import org.bonej.common.RoiUtil;
import org.scijava.plugin.Plugin;

import ij.ImageStack;
import ij.gui.Roi;
import ij.measure.Calibration;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;

/**
 * An Op which calculates the volumes of the sample by counting the voxels in the image
 *
 * @author Michael Doube
 * @author Richard Domander
 */
@Plugin(type = Op.class, name = "volumeFractionVoxel")
public class VolumeFractionVoxel extends VolumeFractionOp {
    @Override
	public OpEnvironment ops() {
		return null;
	}

	@Override
	public void setEnvironment(OpEnvironment opEnvironment) {

	}

	@Override
	public void run() throws NullPointerException, IllegalArgumentException {
        checkInputs();

        volumeFractionVoxel();
	}

    // region -- Helper methods --
    private void volumeFractionVoxel() {
        final ImageStack stack = getImage().get().getStack();
        final int stackSize = stack.getSize();
        final long sliceTotalVolumes[] = new long[stackSize + 1];
        final long sliceForeGroundsVolumes[] = new long[stackSize + 1];

        if (getRoiManager().isPresent()) {
            voxelVolumeWithRois(stack, sliceTotalVolumes, sliceForeGroundsVolumes);
        } else {
            voxelVolumeWithNoRois(stack, sliceTotalVolumes, sliceForeGroundsVolumes);
        }

        final long foregroundVolume = Arrays.stream(sliceForeGroundsVolumes).sum();
        setForegroundVolume(foregroundVolume);
        final long totalVolume = Arrays.stream(sliceTotalVolumes).sum();
        setTotalVolume(totalVolume);
        calibrateVolumes();
        setVolumeRatio();
    }

    private void voxelVolumeWithNoRois(final ImageStack stack, final long[] sliceTotalVolumes,
                                       final long[] sliceForeGroundsVolumes) {
        final Roi defaultRoi = new Roi(0, 0, stack.getWidth(), stack.getHeight());
        final int stackSize = stack.getSize();

        IntStream.rangeClosed(1, stackSize).parallel().forEach(z -> {
            final ImageProcessor processor = stack.getProcessor(z);
            calculateVoxelSliceVolumes(processor, defaultRoi, sliceTotalVolumes, sliceForeGroundsVolumes, z);
        });
    }

    private void voxelVolumeWithRois(final ImageStack stack, final long[] sliceTotalVolumes,
                                     final long[] sliceForeGroundsVolumes) {
        final int stackSize = stack.getSize();
        final RoiManager roiManager = getRoiManager().get();

		IntStream.rangeClosed(1, stackSize).parallel().forEach(z -> {
			final ArrayList<Roi> rois = RoiUtil.getSliceRoi(roiManager, stack, z);

			if (rois.isEmpty()) {
				return;
			}

			final ImageProcessor processor = stack.getProcessor(z);
			for (Roi roi : rois) {
				calculateVoxelSliceVolumes(processor, roi, sliceTotalVolumes, sliceForeGroundsVolumes, z);
			}
		});
    }

    private void calculateVoxelSliceVolumes(final ImageProcessor processor, final Roi roi,
                                            final long[] sliceTotalVolumes, final long[] sliceForegroundVolumes,
                                            final int sliceNumber) {
        processor.setRoi(roi);
        if (processor.getMask() != null) {
            calculateVoxelSliceVolumesWithMask(processor, sliceTotalVolumes, sliceForegroundVolumes, sliceNumber);
        } else {
            calculateVoxelSliceVolumesWithNoMask(processor, sliceTotalVolumes, sliceForegroundVolumes, sliceNumber);
        }
    }

    private void calculateVoxelSliceVolumesWithMask(final ImageProcessor imageProcessor, final long[] sliceTotalVolumes,
                                                    final long[] sliceForegroundVolumes, final int sliceNumber) {
        final Rectangle r = imageProcessor.getRoi();
        final int x0 = r.x;
        final int y0 = r.y;
        final int x1 = x0 + r.width;
        final int y1 = y0 + r.height;
        final  ImageProcessor mask = imageProcessor.getMask();
        final int minThreshold = getMinThreshold();
        final int maxThreshold = getMaxThreshold();

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

    private void calculateVoxelSliceVolumesWithNoMask(final ImageProcessor imageProcessor,
                                                      final long[] sliceTotalVolumes,
                                                      final long[] sliceForegroundVolumes, final int sliceNumber) {
        final Rectangle r = imageProcessor.getRoi();
        final int x0 = r.x;
        final int y0 = r.y;
        final int x1 = x0 + r.width;
        final int y1 = y0 + r.height;
        final int minThreshold = getMinThreshold();
        final int maxThreshold = getMaxThreshold();

        for (int y = y0; y < y1; y++) {
            for (int x = x0; x < x1; x++) {
                final int pixel = imageProcessor.get(x, y);
                if (pixel >= minThreshold && pixel <= maxThreshold) {
                    sliceForegroundVolumes[sliceNumber]++;
                }
            }
        }

        sliceTotalVolumes[sliceNumber] = imageProcessor.getPixelCount();
    }

    private void calibrateVolumes() {
        Calibration calibration = getImage().get().getCalibration();
        double volumeScale = calibration.pixelWidth * calibration.pixelHeight * calibration.pixelDepth;
        double scaledForegroundVolume = volumeScale * getForegroundVolume();
        setForegroundVolume(scaledForegroundVolume);
        double scaledTotalVolume = volumeScale * getTotalVolume();
        setTotalVolume(scaledTotalVolume);
    }
    // endregion
}
