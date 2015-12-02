package protoOps.testImageCreators;

import ij.ImagePlus;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imagej.ImageJ;
import net.imagej.ImgPlus;
import net.imagej.ops.Op;
import net.imglib2.img.ImagePlusAdapter;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An Op which creates an image of a circle and a cross.
 * Can be used, e.g. for testing other Ops or Plugins.
 *
 * @author   Richard Domander
 */
@Plugin(type = Op.class, name = "crossedCircleCreator", menuPath = "Plugins>Test Images>Crossed circle")
public class CrossedCircleCreator implements Op
{
    @Parameter
    private DatasetService datasetService;

    @Parameter(min = "10", required = false)
    private int imageSize = 100;

    @Parameter(type = ItemIO.OUTPUT)
    private Dataset testImage = null;

    @Override
    public void run() {
        createCrossedCircle();
    }

    /**
     * Draw a circle with a vertical and horizontal line crossing it to this.testImage
     *
     * @throws  NullPointerException if this.datasetService == null
     * @return  A Dataset containing a white crossed circle
     *
     */
    private void createCrossedCircle() {
        checkNotNull(datasetService, "No dataset service found");

        ImagePlus imagePlus = StaticTestImageHelper.createCrossedCircle(imageSize);
        ImgPlus<UnsignedByteType> imgPlus = ImagePlusAdapter.wrapImgPlus(imagePlus);
        testImage = datasetService.create(imgPlus);
    }

    public static void main(final String... args) throws Exception {
        final ImageJ ij = new ImageJ();

        final Object image = ij.op().run("crossedCircleCreator");

        ij.ui().showUI();
        ij.ui().show(image);
    }
}
