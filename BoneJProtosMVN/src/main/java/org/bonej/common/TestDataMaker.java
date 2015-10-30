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
     * @param foregroundColor   The color of the cuboid
     * @param padding           The amount of background (voxels) on each side of the cuboid
     * @return                  An ImagePlus object with the cuboid drawn into the center of the stack
     */
    public static ImagePlus createCuboid(int width, int height, int depth, int foregroundColor, int padding)
    {
        int totalPadding = 2 * padding;
        int paddedWidth = width + totalPadding;
        int paddedHeight = height + totalPadding;

        ImageStack stack = new ImageStack(paddedWidth, paddedHeight);
        ImageProcessor backgroundProcessor = new ByteProcessor(paddedWidth, paddedHeight);

        // depth padding
        for (int i = 0; i < padding; i++) {
            stack.addSlice(backgroundProcessor);
        }

        // draw cuboid
        ImageProcessor processor = new ByteProcessor(paddedWidth, paddedHeight);
        processor.setColor(foregroundColor);
        processor.setRoi(padding, padding, width, height);
        processor.fill();

        for (int i = 0; i < depth; i++) {
            stack.addSlice(processor);
        }

        // depth padding
        for (int i = 0; i < padding; i++) {
            stack.addSlice(backgroundProcessor);
        }

        ImagePlus image = new ImagePlus("Test cuboid", stack);
        return image;
    }
}
