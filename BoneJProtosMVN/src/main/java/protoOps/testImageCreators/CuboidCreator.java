package protoOps.testImageCreators;

import ij.ImagePlus;
import net.imagej.*;
import net.imagej.ops.Op;
import net.imagej.ops.OpEnvironment;
import net.imglib2.img.ImagePlusAdapter;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An Op which creates an image of a solid cuboid.
 * Can be used, e.g. for testing other Ops or Plugins.
 *
 * @author  Richard Domander
 */
@Plugin(type = Op.class, name = "cuboidCreator", menuPath = "Plugins>Test Images>Cuboid")
public class CuboidCreator implements Op
{
    @Parameter
    private DatasetService datasetService;

    @Parameter(min = "1", required = false)
    private int cuboidWidth = 100;

    @Parameter(min = "1", required = false)
    private int cuboidHeight = 100;

    @Parameter(min = "1", required = false)
    private int cuboidDepth = 100;

    @Parameter(min = "0", required = false)
    private int cuboidPadding = 10;

    @Parameter(min = "0x00", max = "0xFF", required = false)
    private int cuboidColor = 0xFF;

    @Parameter(type = ItemIO.OUTPUT)
    private Dataset testImage = null;

    @Override
    public void run() {
        createCuboid();
    }

    @Override
    public OpEnvironment ops() {
        return null;
    }

    @Override
    public void setEnvironment(OpEnvironment opEnvironment) {

    }

    /**
     * Draw a cuboid to this.testImage
     *
     * @throws  NullPointerException if this.datasetService == null
     */
    private void createCuboid() {
        checkNotNull(datasetService, "No dataset service found");

        ImagePlus imagePlus = StaticTestImageHelper.createCuboid(cuboidWidth, cuboidHeight, cuboidDepth, cuboidColor,
                cuboidPadding);
        final ImgPlus<UnsignedByteType> image = ImagePlusAdapter.wrapImgPlus(imagePlus);
        testImage = datasetService.create(image);
    }

    public static void main(final String... args) throws Exception {
        final ImageJ ij = new ImageJ();

        final Object image = ij.op().run("cuboidCreator");

        ij.ui().showUI();
        ij.ui().show(image);
    }
}