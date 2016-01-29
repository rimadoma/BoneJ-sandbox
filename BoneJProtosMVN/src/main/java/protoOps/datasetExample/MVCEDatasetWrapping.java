package protoOps.datasetExample;

import ij.ImagePlus;
import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imagej.ops.Op;
import net.imagej.ops.OpEnvironment;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import org.scijava.convert.ConvertService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.stream.IntStream;

/**
 * A (M)inimal, (C)omplete, and (V)erifiable (E)xample of an issue I've encountered with ConvertService.
 *
 * A call to ConvertService.convert(dataset, ImagePlus.class) causes an image window to pop up,
 * when the ImageJ UI is opened (line 77).
 *
 * The image turns red if you move the slider from slide 1.
 */

@Plugin(type = Op.class, name = "datasetWrapping")
public class MVCEDatasetWrapping implements Op {
    private ImagePlus imagePlus;

    @Parameter
    private ConvertService convertService;

    @Parameter
    private Dataset dataset;

    @Override
    public OpEnvironment ops() {
        return null;
    }

    @Override
    public void setEnvironment(OpEnvironment ops) {

    }

    @Override
    public void run() {
        convertDataset();
    }

    public static void main(final String... args) throws Exception {
        int width = 320;
        int height = 200;
        int depth = 10;
        final ImageJ ij = new ImageJ();

        final Dataset inputDataset = createMonochromeDataset(ij ,width, height, depth, 0xFF);

        ij.op().run("datasetWrapping", inputDataset);

        ij.ui().showUI();
    }

    private static Dataset createMonochromeDataset(final ImageJ ijInstance, final int width, final int height,
                                                   final int depth, final int color) {
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

    private void convertDataset() {
        imagePlus = convertService.convert(dataset, ImagePlus.class);
    }
}
