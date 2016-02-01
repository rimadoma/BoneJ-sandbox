package protoOps.imageCheck;

import ij.ImagePlus;
import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imagej.ops.OpEnvironment;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import org.scijava.ItemIO;
import org.scijava.convert.ConvertService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/**
 * @author Richard Domander
 */
@Plugin(type = ImagePlusCheck.ImagePlusCheckOp.class, name = ImagePlusCheck.ImagePlusCheckOp.NAME)
public class DefaultImagePlusCheck implements ImagePlusCheck.ImagePlusCheckOp {
    @Parameter(type = ItemIO.INPUT)
    Dataset dataset = null;

    @Parameter
    ConvertService convertService = null;

    @Parameter(type = ItemIO.OUTPUT)
    boolean isConvertible = false;

    @Override
    public OpEnvironment ops() {
        return null;
    }

    @Override
    public void setEnvironment(OpEnvironment ops) {

    }

    @Override
    public void run() {
        if (convertService == null) {
            throw new NullPointerException("No ConvertService available");
        }

        isConvertible = convertService.supports(dataset, ImagePlus.class);
    }

    public static void main(String... args) {
        final ImageJ ij = new ImageJ();

        ij.getApp().getVersion();

        Dataset dataset = ij.dataset().create(new UnsignedByteType(), new long[]{10, 10, 10}, "",
                new AxisType[]{Axes.X, Axes.Y, Axes.Z});
        boolean checks = (boolean) ij.op().run(ImagePlusCheck.ImagePlusCheckOp.class, dataset);
        if (checks) {
            System.out.println("Dataset can be converted into an ImagePlus");
        } else {
            System.out.println("Dataset cannot be converted into an ImagePlus");
        }

        ij.context().dispose();
    }
}
