package protoOps.datasetCheck;

import net.imagej.Dataset;
import net.imagej.axis.CalibratedAxis;
import net.imagej.ops.Op;
import net.imagej.ops.OpEnvironment;
import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import protoOps.imageCheck.ImagePlusCheck;

import java.util.Arrays;

/**
 * An OP which checks whether the given Dataset is three dimensional, i.e. has three spatial dimensions
 *
 * @author Richard Domander
 */
@Plugin(type = ImagePlusCheck.ImagePlusCheckOp.class, name = "datasetIs3D")
public class DatasetIs3D implements Op {
    private static final int DIMENSIONS = 3;

    @Parameter(type = ItemIO.INPUT)
    private Dataset dataset;

    /**
     * If true, then the Dataset must have only three spatial dimensions, and no other dimensions.
     * If false, the Dataset is allowed to have additional non-spatial dimensions like channel
     * to be considered 3D.
     */
    @Parameter(type = ItemIO.INPUT, required = false)
    private boolean onlySpatial = false;

    @Parameter(type = ItemIO.OUTPUT)
    private boolean is3D = false;

    @Override
    public OpEnvironment ops() {
        return null;
    }

    @Override
    public void setEnvironment(OpEnvironment ops) {

    }

    @Override
    public void run() {
        int dimensions = dataset.numDimensions();

        if (dimensions < DIMENSIONS) {
            is3D = false;
            return;
        }

        CalibratedAxis axes[] = new CalibratedAxis[dimensions];
        dataset.axes(axes);

        long spatialAxes = Arrays.stream(axes).filter(axis -> axis.type().isSpatial()).count();

        is3D = spatialAxes == DIMENSIONS && (dimensions == DIMENSIONS || !onlySpatial);
    }
}
