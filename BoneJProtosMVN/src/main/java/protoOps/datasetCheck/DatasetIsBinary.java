package protoOps.datasetCheck;

import net.imagej.Dataset;
import net.imagej.ops.Op;
import net.imagej.ops.OpEnvironment;
import net.imglib2.type.BooleanType;

import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/**
 * An Op which checks whether the given Dataset is binary. A binary Dataset has BooleanType.
 *
 * @author Richard Domander
 */
@Plugin(type = Op.class, name = "datasetIsBinary")
public class DatasetIsBinary implements Op {

    @Parameter(type = ItemIO.INPUT)
    private Dataset dataset = null;

    /**
     * If true, then the Dataset contains only one or two distinct values
     */
    @Parameter(type = ItemIO.OUTPUT)
    private boolean isBinary = false;

    @Override
    public OpEnvironment ops() {
        return null;
    }

    @Override
    public void setEnvironment(OpEnvironment ops) {

    }

    @Override
    public void run() {
        if (datasetIsEmpty()) {
            isBinary = false;
            return;
        }

        isBinary = isBinaryType();
    }

    //region -- Helper methods --
    private boolean isBinaryType() {
        final Class datasetTypeClass = dataset.getType().getClass();
        return BooleanType.class.isAssignableFrom(datasetTypeClass);
    }

    /**
     * @todo Is is possible for any non-trivial Dataset to be empty?
     */
    private boolean datasetIsEmpty() {
        return !dataset.cursor().hasNext();
    }
    //endregion
}