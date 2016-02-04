package protoOps.datasetCheck;

import net.imagej.Dataset;
import net.imagej.ops.Op;
import net.imagej.ops.OpEnvironment;
import net.imglib2.Cursor;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.ByteType;
import net.imglib2.type.numeric.integer.Unsigned2BitType;
import net.imglib2.type.numeric.integer.Unsigned4BitType;
import net.imglib2.type.numeric.integer.UnsignedByteType;

import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import com.google.common.collect.ImmutableList;

import java.util.HashSet;
import java.util.Set;

/**
 * An Op which checks whether the given Dataset is binary, i.e. whether it contains only two distinct values.
 * One of these values is considered foreground, and one background. By default the greater value is foreground.
 *
 * @author Richard Domander
 * @todo Handle unsigned and signed types?
 * @todo Make unary?
 * @todo BitType is automatically Binary
 * @todo Does it make sense to check for bit depth?
 * @todo What's the value of white for different bit depths?
 */
@Plugin(type = Op.class, name = "datasetIsBinary")
public class DatasetIsBinary implements Op {
    private static final ImmutableList<IntegerType<?>> validTypes = ImmutableList.of(new BitType(), new ByteType(),
            new Unsigned2BitType(), new Unsigned4BitType(), new UnsignedByteType());

    @Parameter(type = ItemIO.INPUT)
    private Dataset dataset = null;

    @Parameter(type = ItemIO.OUTPUT)
    private boolean isBinary = false;

    //@todo fg and bg output @params

    @Override
    public OpEnvironment ops() {
        return null;
    }

    @Override
    public void setEnvironment(OpEnvironment ops) {

    }

    @Override
    public void run() {
        if (datasetIsEmpty() || !isTypeValid()) {
            isBinary = false;
            return;
        }

        checkElementValues();
    }

    private boolean isTypeValid() {
        final RealType<?> element = getFirstDatasetElement();
        return validTypes.stream().anyMatch(type -> type.getClass() == element.getClass());
    }

    private RealType<?> getFirstDatasetElement() {
        final Cursor<RealType<?>> cursor = dataset.cursor();
        cursor.fwd();
        return cursor.next();
    }

    private void checkElementValues() {
        Set<Double> values = new HashSet<>(4);
        final Cursor<RealType<?>> cursor = dataset.cursor();

        while (cursor.hasNext()) {
            cursor.fwd();
            RealType<?> element = cursor.next();
            double value = element.getRealDouble();
            values.add(value);
            if (values.size() > 2) {
                isBinary = false;
                return;
            }
        }

        isBinary = true;
    }

    /**
     * @todo Is is possible for any non-trivial Dataset to be empty?
     */
    private boolean datasetIsEmpty() {
        return !dataset.cursor().hasNext();
    }
}
