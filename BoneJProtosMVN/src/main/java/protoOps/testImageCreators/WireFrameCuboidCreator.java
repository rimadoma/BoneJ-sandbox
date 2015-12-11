package protoOps.testImageCreators;

import ij.ImagePlus;
import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imagej.ImageJ;
import net.imagej.ImgPlus;
import net.imagej.ops.Op;
import net.imagej.ops.OpEnvironment;
import net.imglib2.img.ImagePlusAdapter;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An Op which creates an image of a wire-frame cuboid.
 * Can be used, e.g. for testing other Ops or Plugins.
 *
 * @author  Richard Domander
 */
@Plugin(type = Op.class, name = "wireFrameCuboidCreator", menuPath = "Plugins>Test Images>Wire-frame cuboid")
public class WireFrameCuboidCreator implements Op {
    @Parameter
    private DatasetService datasetService;

    @Parameter(min = "1", required = false, description = "Cuboid width (px)")
    private int cuboidWidth = 100;

    @Parameter(min = "1", required = false, description = "Cuboid height (px)")
    private int cuboidHeight = 100;

    @Parameter(min = "1", required = false, description = "Cuboid depth (px)")
    private int cuboidDepth = 100;

    @Parameter(min = "0", required = false, description = "Empty space around the cuboid (px)")
    private int cuboidPadding = 10;

    @Parameter(type = ItemIO.OUTPUT)
    private Dataset testImage = null;

    @Override
    public void run() {
        createWireFrameCuboid();
    }

    @Override
    public OpEnvironment ops() {
        return null;
    }

    @Override
    public void setEnvironment(OpEnvironment opEnvironment) {

    }

    /**
     * Draw a wire-frame cuboid to this.testImage
     *
     * @throws  NullPointerException if this.datasetService == null
     *
     */
    private void createWireFrameCuboid() {
        checkNotNull(datasetService, "No dataset service found");

        ImagePlus imagePlus = StaticTestImageHelper.createWireFrameCuboid(cuboidWidth, cuboidHeight, cuboidDepth,
                cuboidPadding);
        ImgPlus<UnsignedByteType> imgPlus = ImagePlusAdapter.wrapImgPlus(imagePlus);
        testImage = datasetService.create(imgPlus);
    }

    public static void main(final String... args) throws Exception {
        final ImageJ ij = new ImageJ();

        final Object image = ij.op().run("wireFrameCuboidCreator", 128, 128, 128, 32);

        ij.ui().showUI();
        ij.ui().show(image);
    }
}
