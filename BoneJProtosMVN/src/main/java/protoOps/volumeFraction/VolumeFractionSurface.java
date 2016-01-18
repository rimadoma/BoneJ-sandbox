package protoOps.volumeFraction;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.awt.*;
import java.util.ArrayList;

import marchingcubes.MCTriangulator;
import net.imagej.ops.Op;
import net.imagej.ops.OpEnvironment;

import org.bonej.common.ImageCheck;
import org.bonej.common.RoiUtil;
import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import customnode.CustomTriangleMesh;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Roi;
import ij.measure.Calibration;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;

import javax.vecmath.Color3f;

/**
 * An Op which calculates the volumes of the sample by generating a surface mesh
 *
 * @todo Solve issues with Fiji 20.0.0
 * @todo add thresholding
 * @author Richard Domander
 */
@Plugin(type = Op.class, name = "volumeFractionVoxel")
public class VolumeFractionSurface implements VolumeFractionOp {
    public static final int DEFAULT_SURFACE_RESAMPLING = 6;

    @Parameter(type = ItemIO.INPUT)
    private ImagePlus inputImage = null;

    @Parameter(type = ItemIO.INPUT, required = false)
    private RoiManager roiManager = null;

    @Parameter(type = ItemIO.INPUT, required = false, min = "0")
    private int surfaceResampling = DEFAULT_SURFACE_RESAMPLING;

    @Parameter(type = ItemIO.OUTPUT)
    private double foregroundVolume;

    @Parameter(type = ItemIO.OUTPUT)
    private double totalVolume;

    @Parameter(type = ItemIO.OUTPUT)
    private double volumeRatio;

    @Parameter(type = ItemIO.OUTPUT)
    private CustomTriangleMesh foregroundSurface;

    @Parameter(type = ItemIO.OUTPUT)
    private CustomTriangleMesh totalSurface;

    // @todo make min & max threshold parameters
    // @todo write setters for thresholds
    private int minThreshold = 127;
    private int maxThreshold = 255;

    public VolumeFractionSurface() {
        reset();
    }

    // region -- Getters --
    @Override
    public int getMinThreshold() {
        return minThreshold;
    }

    @Override
    public int getMaxThreshold() {
        return maxThreshold;
    }

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

    @SuppressWarnings("unused")
    public CustomTriangleMesh getForegroundSurface() {
        return foregroundSurface;
    }

    @SuppressWarnings("unused")
    public CustomTriangleMesh getTotalSurface() {
        return totalSurface;
    }
    // endregion

    // region -- Setters --
    public void setSurfaceResampling(int resampling) {
        checkArgument(resampling >= 0, "Resampling value must be >= 0");

        surfaceResampling = resampling;
    }

    public void setImage(ImagePlus image) {
        checkImage(image);

        inputImage = image;

        setThresholds();
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
        volumeFractionSurface();
    }

    public void reset() {
        roiManager = null;
        foregroundVolume = 0.0;
        totalVolume = 0.0;
        volumeRatio = Double.NaN;
        foregroundSurface = null;
        totalSurface = null;
    }

    // region -- Helper methods --

    private void volumeFractionSurface() {
        final ImageStack stack = inputImage.getImageStack();
        int xMin = 0;
        int xMax = stack.getWidth();
        int yMin = 0;
        int yMax = stack.getHeight();
        int zMin = 1;
        int zMax = stack.getSize();

        final int width = xMax - xMin;
        final int height = yMax - yMin;
        final int depth = zMax - zMin + 1;

        ImageStack outStack = new ImageStack(width, height);
        ImageStack maskStack = new ImageStack(width, height);
        ImageProcessor processor = stack.getProcessor(1).createProcessor(width, height);
        for (int i = 0; i < depth; i++) {
            outStack.addSlice(processor.duplicate());
            maskStack.addSlice(processor.duplicate());
        }

        totalVoxels = 0;
        fgVoxels = 0;

        if (roiManager != null) {
            drawSurfaceMasksWithRois(zMin, zMax, xMin, yMin, stack, maskStack, outStack);
        } else {
            drawSurfaceMasksWithNoRoi(zMin, zMax, xMin, yMin, stack, maskStack, outStack);
        }

        System.out.println("Total voxels: " + totalVoxels);
        System.out.println("Foreground voxels: " + fgVoxels);

        Calibration calibration = inputImage.getCalibration();

        ImagePlus outImp = new ImagePlus();
        outImp.setStack("Out", outStack);
        outImp.setCalibration(calibration);

        ImagePlus maskImp = new ImagePlus();
        maskImp.setStack("Mask", maskStack);
        maskImp.setCalibration(calibration);

        Color3f yellow = new Color3f(1.0f, 1.0f, 0.0f);
        boolean[] channels = { true, false, false };
        MCTriangulator mct = new MCTriangulator();
        java.util.List points = mct.getTriangles(outImp, 128, channels, surfaceResampling);
        foregroundSurface = new CustomTriangleMesh(points, yellow, 0.4f);
        foregroundVolume = Math.abs(foregroundSurface.getVolume());

        Color3f blue = new Color3f(0.0f, 0.0f, 1.0f);
        points = mct.getTriangles(maskImp, 128, channels, surfaceResampling);
        totalSurface = new CustomTriangleMesh(points, blue, 0.65f);
        totalVolume = Math.abs(totalSurface.getVolume());

        volumeRatio = foregroundVolume / totalVolume;
    }

    private void drawSurfaceMasksWithNoRoi(final int zMin, final int zMax, final int xMin, final int yMin,
                                           final ImageStack inputStack, final ImageStack maskStack,
                                           final ImageStack outStack) {
        final Roi defaultRoi = new Roi(0, 0, inputStack.getWidth(), inputStack.getHeight());

        for (int sliceNumber = zMin; sliceNumber <= zMax; sliceNumber++) {
            final ImageProcessor slice = inputStack.getProcessor(sliceNumber);
            slice.setRoi(defaultRoi);
            ImageProcessor mask = slice.getMask();
            if (mask == null) {
                drawSurfaceMasks(slice, maskStack, outStack, sliceNumber, xMin, yMin, zMin);
            } else {
                drawSurfaceMasksWithProcessorMask(slice, mask, maskStack, outStack, sliceNumber, xMin, yMin, zMin);
            }
        }
    }

    private void drawSurfaceMasksWithRois(final int zMin, final int zMax, final int xMin, final int yMin,
                                          final ImageStack inputStack, final ImageStack maskStack,
                                          final ImageStack outStack) {
        for (int sliceNumber = zMin; sliceNumber <= zMax; sliceNumber++) {
            ArrayList<Roi> rois = RoiUtil.getSliceRoi(roiManager, inputStack, sliceNumber);
            if (rois.isEmpty()) {
                continue;
            }

            final ImageProcessor slice = inputStack.getProcessor(sliceNumber);

            for (Roi roi : rois) {
                slice.setRoi(roi);

                ImageProcessor mask = slice.getMask();
                if (mask == null) {
                    drawSurfaceMasks(slice, maskStack, outStack, sliceNumber, xMin, yMin, zMin);
                } else {
                    drawSurfaceMasksWithProcessorMask(slice, mask, maskStack, outStack, sliceNumber, xMin, yMin, zMin);
                }
            }
        }
    }

    private void drawSurfaceMasksWithProcessorMask(final ImageProcessor slice, final ImageProcessor mask,
                                                   final ImageStack maskStack, final ImageStack outStack,
                                                   final int sliceNumber, final int xMin, final int yMin,
                                                   final int zMin) {
        final int white = 255;

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

    private int totalVoxels = 0;
    private int fgVoxels = 0;

    private void drawSurfaceMasks(final ImageProcessor slice, final ImageStack maskStack, final ImageStack outStack,
                                  final int sliceNumber, final int xMin, final int yMin, final int zMin) {
        final int white = 255;

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
                totalVoxels++;
                final double pixel = slice.get(x, y);
                if (pixel >= minThreshold && pixel <= maxThreshold) {
                    outProcessor.set(outX, outY, white);
                    fgVoxels++;
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
        //endregion
    }
}
