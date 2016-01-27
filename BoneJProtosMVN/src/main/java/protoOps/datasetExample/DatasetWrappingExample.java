package protoOps.datasetExample;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.stream.IntStream;

import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imagej.ImageJ;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imagej.axis.CalibratedAxis;
import net.imagej.ops.Op;
import net.imagej.ops.OpEnvironment;
import net.imglib2.img.ImagePlusAdapter;
import net.imglib2.type.numeric.integer.UnsignedByteType;

import org.bonej.common.ImageCheck;
import org.scijava.AbstractContextual;
import org.scijava.ItemIO;
import org.scijava.convert.ConvertService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import ij.ImagePlus;
import ij.ImageStack;

/**
 * An example class to test wrapping and unwrapping of a ImageJ2 Dataset
 *
 * How to run via the Java API:
 * 1) Create an instance of the class
 * 2) Call setContext with e.g. ImageJ#getContext()
 * 3) Call setDataset
 * 4) Call run
 * 5) Call getDataset to get the result
 *
 * How to run via an opService e.g. ImageJ.op()
 * 1) Create an instance of ImageJ
 * 2) Call final Object image = ij.op().run("datasetExample", dataset)
 * 3) Object image contains the resulting dataset
 *
 * The class AbstractContextual is inherited so that you can run this class via Java API.
 * When you run it via an opService the convertService field is populated automatically.
 *
 * @author Richard Domander
 */
@Plugin(type = Op.class, name = "datasetExample", menuPath = "Plugins>Examples>Dataset")
public class DatasetWrappingExample extends AbstractContextual implements Op {
    private ImagePlus image = null;

    @Parameter
    private ConvertService convertService = null;

    @Parameter
    private DatasetService datasetService = null;

    @Parameter(type = ItemIO.BOTH)
    private Dataset dataset = null;

    public Dataset getDataset() {
        return dataset;
    }

    public void setDataset(Dataset dataset) {
        checkDataset(dataset);
        this.dataset = dataset;
        setImagePlus();
    }

    @Override
    public OpEnvironment ops() {
        return null;
    }

    @Override
    public void setEnvironment(OpEnvironment opEnvironment) {
    }

    @Override
    public void run() {
        checkNotNull(convertService, "Missing services - did you remember to call setContext?");
        checkNotNull(datasetService, "Missing services - did you remember to call setContext?");

        if (image == null) {
            // the plugin is (probably) run from an opService, check the dataset obtained as @Parameter
            setDataset(dataset);
        }

        makeNegative();

        ImgPlus<UnsignedByteType> imgPlus = ImagePlusAdapter.wrapImgPlus(image);
        dataset = datasetService.create(imgPlus);
    }

    //region -- Utility methods --
    public static void main(final String... args) throws Exception {
        int width = 320;
        int height = 200;
        int depth = 10;
        final ImageJ ij = new ImageJ();

        final Dataset inputDataset = createMonochromeDataset(ij ,width, height, depth, 0xFF);

        final DatasetWrappingExample datasetExample = new DatasetWrappingExample();
        datasetExample.setContext(ij.getContext());
        datasetExample.setDataset(inputDataset);
        datasetExample.run();

        ij.ui().showUI();
    }

    public static Dataset createMonochromeDataset(final ImageJ ijInstance, final int width, final int height,
                                                  int depth, int color) {
        final long[] dims = {width, height, depth};
        final AxisType[] axisTypes = {Axes.X, Axes.Y, Axes.Z};
        Dataset dataset = ijInstance.dataset().create(new UnsignedByteType(), dims, "Test image", axisTypes);

        IntStream.range(0, depth).forEach(i -> {
            final byte[] data = new byte[width * height];
            IntStream.range(0, data.length).forEach(j -> data[j] = (byte)color);
            dataset.setPlane(i, data);
        });

        return dataset;
    }
    //endregion

    //region -- Helper methods --
    private void checkDataset(Dataset dataset) {
        checkNotNull(dataset, "Dataset cannot be null");
        checkDatasetDimensions(dataset);
        checkArgument(convertService.supports(dataset, ImagePlus.class), "Cannot convert given dataset");
    }

    private void checkDatasetDimensions(Dataset dataset) {
        checkArgument(dataset.numDimensions() == 3, "The plugin is meant only for 3D images");
        CalibratedAxis axes[] = new CalibratedAxis[3];
        dataset.axes(axes);
        for (CalibratedAxis axis : axes) {
            checkArgument(axis.type().isSpatial(), "Unexpected dimension");
        }
    }

    private void checkImage(ImagePlus imagePlus) {
        checkArgument(imagePlus.getBitDepth() == 8, "Image must be 8-bit");
        checkArgument(ImageCheck.isBinary(imagePlus), "Image must be binary");
    }

    private void makeNegative() {
        final int depth = image.getNSlices();
        final ImageStack stack = image.getStack();

        IntStream.rangeClosed(1, depth).parallel().forEach(z -> {
            byte pixels[] = (byte[]) stack.getPixels(z);
            for (int i = 0; i < pixels.length; i++) {
                pixels[i] = (byte) (pixels[i] ^ 0xFF);
            }
        });
    }

    private void setImagePlus() {
        //@todo Find out why the call to convert pops up a weird image window
        ImagePlus imagePlus = convertService.convert(this.dataset, ImagePlus.class);
        checkImage(imagePlus);
        image = imagePlus;
    }
    //endregion
}
