package protoOps;

import ij.ImagePlus;
import net.imagej.ops.Op;
import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * @author <a href="mailto:rdomander@rvc.ac.uk">Richard Domander</a>
 * @date 30/11/15
 */
@Plugin(type=Op.class, name = "triplePointAngles")
public class TriplePointAngles implements Op {
    public static final int DEFAULT_NTH_POINT = 0;
    public static final int VERTEX_TO_VERTEX = -1;

    @Parameter
    private ImagePlus inputImage = null;

    @Parameter
    private int nthPixel = DEFAULT_NTH_POINT;

    @Parameter(type = ItemIO.OUTPUT)
    double results[][][];

    @Override
    public void run() {
        checkArgument(inputImage.getBitDepth() == 8, "The bit depth of the input image must be 8");

        calculateTriplePointAngles();
    }

    private void calculateTriplePointAngles() {
        System.out.println("NthPixel " + nthPixel);
    }
}
