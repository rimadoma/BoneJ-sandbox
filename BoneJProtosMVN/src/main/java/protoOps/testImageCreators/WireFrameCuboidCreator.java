package protoOps.testImageCreators;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import net.imagej.ImageJ;
import net.imagej.ops.Op;
import net.imagej.patcher.LegacyInjector;
import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/**
 * An Op which creates an image of a wire-frame cuboid.
 * Can be used, e.g. for testing other Ops or Plugins.
 *
 * @todo    Change output image type to Dataset or ImgPlus<>
 *
 * @author  <a href="mailto:rdomander@rvc.ac.uk">Richard Domander</a>
 * @author  Michael Doube
 */
@Plugin(type = Op.class, name = "wireFrameCuboidCreator", menuPath = "Plugins>Test Images>Wire-frame cuboid")
public class WireFrameCuboidCreator implements Op {
    static {
        LegacyInjector.preinit();
    }

    @Parameter(min = "1")
    private int cuboidWidth = 100;

    @Parameter(min = "1")
    private int cuboidHeight = 100;

    @Parameter(min = "1")
    private int cuboidDepth = 100;

    @Parameter(min = "0")
    private int cuboidPadding = 10;

    @Parameter(type = ItemIO.OUTPUT)
    private ImagePlus testImage = null;

    @Override
    public void run() {
        testImage = createWireFrameCuboid(cuboidWidth, cuboidHeight, cuboidDepth, cuboidPadding);
    }

    public static ImageStack createEmptyStack(int width, int height, int depth) {
        ImageStack stack = new ImageStack(width, height);
        for (int s = 0; s < depth; s++) {
            ImageProcessor processor = new ByteProcessor(width, height);
            processor.setColor(0x00);
            processor.fill();
            stack.addSlice(processor);
        }
        return stack;
    }

    /**
     * Draw the wire-frame model i.e. the edges of the given cuboid
     *
     * @param width     Width of the cuboid frame in pixels
     * @param height    Height of the cuboid frame in pixels
     * @param depth     Depth of the cuboid frame in pixels
     * @param padding   Number of pixels added to each side of the cuboid
     * @return Image containing a 1-pixel wide outline of a 3D box
     */
    public static ImagePlus createWireFrameCuboid(int width, int height, int depth, int padding) {
        final int totalPadding = 2 * padding;
        final int paddedWidth = width + totalPadding;
        final int paddedHeight = height + totalPadding;
        final int paddedDepth = depth + totalPadding;
        final int boxColor = 0xFF;

        ImageStack stack = createEmptyStack(paddedWidth, paddedDepth, paddedHeight);

        // Draw edges in the xy-plane
        ImageProcessor ip = new ByteProcessor(paddedWidth, paddedHeight);
        ip.setColor(boxColor);
        ip.drawRect(padding, padding, width, height);
        stack.setProcessor(ip.duplicate(), padding);
        stack.setProcessor(ip.duplicate(), padding + depth);

        // Draw edges in the xz-plane
        for (int s = padding + 1; s < padding + depth; s++) {
            ip = stack.getProcessor(s);
            ip.setColor(boxColor);
            ip.drawPixel(padding, padding);
            ip.drawPixel(padding, padding + height - 1);
            ip.drawPixel(padding + width - 1, padding);
            ip.drawPixel(padding + width - 1, padding + height - 1);
        }

        return new ImagePlus("Wire-frame cuboid", stack);
    }

    public static void main(final String... args) throws Exception {
        final ImageJ ij = new ImageJ();

        // Run our op
        final Object image = ij.op().run("wireFrameCuboidCreator", 128, 128, 128, 32);

        // And display the result!
        ij.ui().showUI();
        ij.ui().show(image);
    }
}
