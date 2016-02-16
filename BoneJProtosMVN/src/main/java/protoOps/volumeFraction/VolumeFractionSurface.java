package protoOps.volumeFraction;

import static com.google.common.base.Preconditions.checkArgument;

import java.awt.*;
import java.util.ArrayList;
import java.util.stream.IntStream;

import javax.vecmath.Color3f;

import marchingcubes.MCTriangulator;
import net.imagej.ops.Op;
import net.imagej.ops.OpEnvironment;

import org.bonej.common.RoiUtil;
import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import customnode.CustomTriangleMesh;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Roi;
import ij.measure.Calibration;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;

/**
 * An Op which calculates the volumes of the sample by generating a surface mesh
 *
 * @todo Migrate to imagej-ops DefaultMesh & MarchingCubes
 * @todo Solve issues with Fiji 20.0.0
 * @todo check that plugin works when run trough OpService
 * @todo Fix run with RoiManager
 * @author Michael Doube
 * @author Richard Domander
 */
@Plugin(type = Op.class, name = "volumeFractionSurface")
public final class VolumeFractionSurface extends VolumeFractionOp {
    public static final int DEFAULT_SURFACE_RESAMPLING = 6;

    @Parameter(type = ItemIO.INPUT, required = false, min = "0")
    private int surfaceResampling = DEFAULT_SURFACE_RESAMPLING;

    @Parameter(type = ItemIO.OUTPUT)
    private CustomTriangleMesh foregroundSurface;

    @Parameter(type = ItemIO.OUTPUT)
    private CustomTriangleMesh totalSurface;
    
    // region -- Getters --
    public CustomTriangleMesh getForegroundSurface() {
        return foregroundSurface;
    }

    public CustomTriangleMesh getTotalSurface() {
        return totalSurface;
    }
    // endregion

    // region -- Setters --
    public void setSurfaceResampling(int resampling) {
        checkArgument(resampling >= 0, "Resampling value must be >= 0");

        surfaceResampling = resampling;
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
        checkInputs();

        volumeFractionSurface();
    }

    // region -- Helper methods --

    private void volumeFractionSurface() {
        final ImageStack stack = getImage().get().getImageStack();
        final int xMin = 0;
        final int xMax = stack.getWidth();
        final int yMin = 0;
        final int yMax = stack.getHeight();
        final int zMin = 1;
        final int zMax = stack.getSize();

        final int width = xMax - xMin;
        final int height = yMax - yMin;
        final int depth = zMax - zMin + 1;

        ImagePlus outImp = IJ.createImage("Out", "8black", width, height, depth);
        ImagePlus maskImp = IJ.createImage("Mask", "8black", width, height, depth);

        Calibration calibration = getImage().get().getCalibration();
        outImp.setCalibration(calibration);
        maskImp.setCalibration(calibration);

        final ImageStack outStack = outImp.getStack();
        final ImageStack maskStack = maskImp.getStack();

        if (getRoiManager().isPresent()) {
            drawSurfaceMasksWithRois(zMin, zMax, xMin, yMin, stack, maskStack, outStack);
        } else {
            drawSurfaceMasksWithNoRoi(zMin, zMax, xMin, yMin, stack, maskStack, outStack);
        }

        analyzeMask("Foreground mask", outStack, 1 , 1);
        analyzeMask("Total sample mask", maskStack, 1, 1);

        Color3f yellow = new Color3f(1.0f, 1.0f, 0.0f);
        boolean[] channels = { true, false, false };
        MCTriangulator mct = new MCTriangulator();
        java.util.List points = mct.getTriangles(outImp, 128, channels, surfaceResampling);
        foregroundSurface = new CustomTriangleMesh(points, yellow, 0.4f);
        float foregroundVolume = Math.abs(foregroundSurface.getVolume());
        setForegroundVolume(foregroundVolume);

        Color3f blue = new Color3f(0.0f, 0.0f, 1.0f);
        points = mct.getTriangles(maskImp, 128, channels, surfaceResampling);
        totalSurface = new CustomTriangleMesh(points, blue, 0.65f);
        float totalVolume = Math.abs(totalSurface.getVolume());
        setTotalVolume(totalVolume);

        setVolumeRatio();
    }

    /**
     * Code for print debugging
     * @todo Remove when no longer necessary
     */
    private void analyzeMask(final String maskName, final ImageStack maskStack, final int zMin, final int zMax) {
        System.out.println(maskName);

        int fgPixels = 0;
        int totalPixels = 0;
        for (int z = zMin; z <= zMax; z++) {
            final byte[] slice = (byte[]) maskStack.getPixels(z);
            for (byte pixel : slice) {
                if (pixel != 0) {
                    fgPixels++;
                }
            }
            totalPixels += slice.length;
        }

        System.out.println("Foreground / total pixels: " + fgPixels + "/" + totalPixels);
        System.out.println();
    }

    private void drawSurfaceMasksWithNoRoi(final int zMin, final int zMax, final int xMin, final int yMin,
                                           final ImageStack inputStack, final ImageStack maskStack,
                                           final ImageStack outStack) {
        final Roi defaultRoi = new Roi(0, 0, inputStack.getWidth(), inputStack.getHeight());

        IntStream.rangeClosed(zMin, zMax).parallel().forEach( z -> {
            final ImageProcessor slice = inputStack.getProcessor(z);
            slice.setRoi(defaultRoi);

            final ImageProcessor mask = slice.getMask();
            if (mask == null) {
                drawSurfaceMasks(slice, maskStack, outStack, z, xMin, yMin, zMin);
            } else {
                drawSurfaceMasksWithProcessorMask(slice, mask, maskStack, outStack, z, xMin, yMin, zMin);
            }
        });
    }

    private void drawSurfaceMasksWithRois(final int zMin, final int zMax, final int xMin, final int yMin,
                                          final ImageStack inputStack, final ImageStack maskStack,
                                          final ImageStack outStack) {
        final RoiManager roiManager = getRoiManager().get();

		IntStream.rangeClosed(zMin, zMax).parallel().forEach(z -> {
			final ArrayList<Roi> rois = RoiUtil.getSliceRoi(roiManager, inputStack, z);
			if (rois.isEmpty()) {
				return;
			}

            final ImageProcessor slice = inputStack.getProcessor(z);
            for (Roi roi : rois) {
                slice.setRoi(roi);

                final ImageProcessor mask = slice.getMask();
                if (mask == null) {
                    drawSurfaceMasks(slice, maskStack, outStack, z, xMin, yMin, zMin);
                } else {
                    drawSurfaceMasksWithProcessorMask(slice, mask, maskStack, outStack, z, xMin, yMin, zMin);
                }
            }
		});
    }

    /**
     * @todo Test results against BoneJ1 with a polygonal ROI (a ROI with a mask)
     */
    private void drawSurfaceMasksWithProcessorMask(final ImageProcessor slice, final ImageProcessor mask,
                                                   final ImageStack maskStack, final ImageStack outStack,
                                                   final int sliceNumber, final int xMin, final int yMin,
                                                   final int zMin) {
        final int white = 255;
        final int minThreshold = getMinThreshold();
        final int maxThreshold = getMaxThreshold();

        final Rectangle r = slice.getRoi();
        final int x0 = r.x;
        final int y0 = r.y;
        final int x1 = x0 + r.width;
        final int y1 = y0 + r.height;

        final int outSlice = sliceNumber - zMin + 1;
        final ImageProcessor maskProcessor = maskStack.getProcessor(outSlice);
        final ImageProcessor outProcessor = outStack.getProcessor(outSlice);


        for (int y = y0; y < y1; y++) {
            final int maskY = y - y0;
            final int outY = y - yMin;
            for (int x = x0; x < x1; x++) {
                final int outX = x - xMin;
                final int maskX = x - x0;
                if (mask.get(maskX, maskY) == 0) {
                    continue;
                }
                maskProcessor.set(outX, outY, white);
                final int pixel = slice.get(x, y);
                if (pixel >= minThreshold && pixel <= maxThreshold) {
                    outProcessor.set(outX, outY, white);
                }
            }
        }
    }

    private void drawSurfaceMasks(final ImageProcessor slice, final ImageStack maskStack, final ImageStack outStack,
                                  final int sliceNumber, final int xMin, final int yMin, final int zMin) {
        final int white = 255;
        final int minThreshold = getMinThreshold();
        final int maxThreshold = getMaxThreshold();

        final Rectangle r = slice.getRoi();
        final int x0 = r.x;
        final int y0 = r.y;
        final int x1 = x0 + r.width;
        final int y1 = y0 + r.height;

        final int outSlice = sliceNumber - zMin + 1;
        final ImageProcessor maskProcessor = maskStack.getProcessor(outSlice);
        final ImageProcessor outProcessor = outStack.getProcessor(outSlice);

        for (int y = y0; y < y1; y++) {
            final int outY = y - yMin;
            for (int x = x0; x < x1; x++) {
                final int outX = x - xMin;
                maskProcessor.set(outX, outY, white);
                final int pixel = slice.get(x, y);
                if (pixel >= minThreshold && pixel <= maxThreshold) {
                    outProcessor.set(outX, outY, white);
                }
            }
        }
    }
    //endregion
}
