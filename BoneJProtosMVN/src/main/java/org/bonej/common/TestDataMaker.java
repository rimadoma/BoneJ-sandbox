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

    /**
     * Draw the edges of a brick with 32 pixels of padding on all faces
     *
     * @param width
     *            Width of the box frame in pixels
     * @param height
     *            Height of the box frame in pixels
     * @param depth
     *            Depth of the box frame in pixels
     * @return Image containing a 1-pixel wide outline of a 3D box
     */
    public static ImagePlus boxFrame(int width, int height, int depth) {
        ImageStack stack = new ImageStack(width + 64, height + 64);
        for (int s = 1; s <= depth + 64; s++) {
            ImageProcessor ip = new ByteProcessor(width + 64, height + 64);
            ip.setColor(0);
            ip.fill();
            stack.addSlice(ip);
        }
        ImageProcessor ip = stack.getProcessor(32);
        ip.setColor(255);
        ip.drawRect(32, 32, width, height);
        ip = stack.getProcessor(32 + depth);
        ip.setColor(255);
        ip.drawRect(32, 32, width, height);
        for (int s = 33; s < 32 + depth; s++) {
            ip = stack.getProcessor(s);
            ip.setColor(255);
            ip.drawPixel(32, 32);
            ip.drawPixel(32, 31 + height);
            ip.drawPixel(31 + width, 32);
            ip.drawPixel(31 + width, 31 + height);
        }
        ImagePlus imp = new ImagePlus("box-frame", stack);
        return imp;
    }

    public static ImagePlus createCuboid(int paddedWidth, int paddedHeight, int paddedDepth, int padding,
                                         ImageProcessor cuboidProcessor) {
        ImageStack cuboidStack = new ImageStack(paddedWidth, paddedHeight);

        for (int i = 0; i < paddedDepth - padding; i++) {
            cuboidStack.addSlice(cuboidProcessor);
        }

        // depth padding
        ImageProcessor paddingProcessor = new ByteProcessor(paddedWidth, paddedHeight);
        for (int i = 0; i < padding; i++) {
            cuboidStack.addSlice("", paddingProcessor, 0);
            cuboidStack.addSlice(paddingProcessor);
        }

        ImagePlus image = new ImagePlus("Test cuboid", cuboidStack);
        return image;
    }
}
