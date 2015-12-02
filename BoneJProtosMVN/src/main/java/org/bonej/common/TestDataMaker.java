package org.bonej.common;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

/**
 * @author Michael Doube
 * @author <a href="mailto:rdomander@rvc.ac.uk">Richard Domander</a>
 */
public class TestDataMaker {

    /**
     * Creates an ImagePlus with a cuboid in it
     *
     * @param width             The width of the cuboid
     * @param height            The height of the cuboid
     * @param depth             The depth of the cuboid (slices)
     * @param cuboidColor       The color of the cuboid
     * @param padding           The amount of background (voxels) on each side of the cuboid
     * @return                  An ImagePlus object with the cuboid drawn into the center of the stack
     */
    public static ImagePlus createCuboid(int width, int height, int depth, int cuboidColor, int padding)
    {
        int totalPadding = 2 * padding;
        int paddedWidth = width + totalPadding;
        int paddedHeight = height + totalPadding;
        int paddedDepth = depth + totalPadding;

        ImageProcessor processor = new ByteProcessor(paddedWidth, paddedHeight);
        processor.setColor(cuboidColor);
        processor.setRoi(padding, padding, width, height);
        processor.fill();

        return createCuboid(paddedWidth, paddedHeight, paddedDepth, padding, processor);
    }

    public static ImagePlus createCuboid(int paddedWidth, int paddedHeight, int paddedDepth, int padding,
                                         ImageProcessor cuboidProcessor) {
        ImageStack cuboidStack = new ImageStack(paddedWidth, paddedHeight);

        for (int i = 0; i < paddedDepth - padding; i++) {
            cuboidStack.addSlice(cuboidProcessor.duplicate());
        }

        // depth padding
        ImageProcessor paddingProcessor = new ByteProcessor(paddedWidth, paddedHeight);
        for (int i = 0; i < padding; i++) {
            cuboidStack.addSlice("", paddingProcessor.duplicate(), 0);
            cuboidStack.addSlice(paddingProcessor.duplicate());
        }

        ImagePlus image = new ImagePlus("Test cuboid", cuboidStack);
        return image;
    }
}
