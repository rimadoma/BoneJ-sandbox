package protoOps.datasetCheck;

import java.util.TreeSet;

import net.imagej.Dataset;
import net.imagej.ops.Op;
import net.imagej.ops.OpEnvironment;
import net.imglib2.Cursor;
import net.imglib2.type.numeric.RealType;

import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/**
 * An Op which checks whether the given Dataset is binary, i.e. whether it contains only one or two distinct values.
 * One of these values is considered foreground, and one background. By default the greater value is foreground.
 *
 * @author Richard Domander
 * @todo Make unary?
 */
@Plugin(type = Op.class, name = "datasetIsBinary")
public class DatasetIsBinary implements Op {
    @Parameter(type = ItemIO.INPUT)
    private Dataset dataset = null;

    @Parameter(type = ItemIO.INPUT, required = false)
    private boolean invertedValues = false;

    @Parameter(type = ItemIO.OUTPUT)
    private boolean isBinary = false;

    @Parameter(type = ItemIO.OUTPUT)
    private long backgroundValue = 0;

    @Parameter(type = ItemIO.OUTPUT)
    private long foregroundValue = 0;

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

        checkElementValues();
    }

    //region -- Helper methods --
    private void checkElementValues() {
        TreeSet<Double> values = new TreeSet<>();
        final Cursor<RealType<?>> cursor = dataset.cursor();

        while (cursor.hasNext()) {
            cursor.fwd();
            double value = cursor.next().getRealDouble();
            values.add(value);
            if (values.size() > 2) {
                isBinary = false;
                return;
            }
        }

        if (invertedValues) {
            backgroundValue = values.last().longValue();
            foregroundValue = values.first().longValue();
        } else {
            backgroundValue = values.first().longValue();
            foregroundValue = values.last().longValue();
        }

        isBinary = true;
    }

    /**
     * @todo Is is possible for any non-trivial Dataset to be empty?
     */
    private boolean datasetIsEmpty() {
        return !dataset.cursor().hasNext();
    }
    //endregion
}
