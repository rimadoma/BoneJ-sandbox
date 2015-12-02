package protoOps.testImageCreators;

import ij.ImagePlus;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import net.imagej.ImageJ;
import net.imagej.ops.Op;
import net.imagej.patcher.LegacyInjector;
import org.bonej.wrapperPlugins.Skeletonize3D;
import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.skeletonize3D.Skeletonize3D_;

/**
 * @author  <a href="mailto:rdomander@rvc.ac.uk">Richard Domander</a>
 * @author   Michael Doube
 */
@Plugin(type = Op.class, name = "crossedCircleCreator", menuPath = "Plugins>Test Images>Crossed circle")
public class CrossedCircleCreator implements Op {
    static {
        LegacyInjector.preinit();
    }

    @Parameter(min = "10", required = false)
    private int imageSize = 100;

    @Parameter(type = ItemIO.OUTPUT)
    private ImagePlus testImage = null;

    @Override
    public void run() {
        testImage = createCrossedCircle(imageSize);
    }

    public static ImagePlus createCrossedCircle(int size) {
        ImageProcessor ip = new ByteProcessor(size, size);
        ip.setColor(0x00);
        ip.fill();
        ip.setColor(0xFF);
        int half = size / 2;
        int quarter = size / 4;
        ip.drawOval(quarter, quarter, half, half);
        ip.drawLine(half, quarter, half, size - quarter);
        ip.drawLine(quarter, half, size - quarter, half);
        return new ImagePlus("Crossed circle", ip);
    }

    public static void main(final String... args) throws Exception {
        final ImageJ ij = new ImageJ();

        // Run our op
        final Object image = ij.op().run("crossedCircleCreator");

        // And display the result!
        ij.ui().showUI();
        ij.ui().show(image);
    }
}
