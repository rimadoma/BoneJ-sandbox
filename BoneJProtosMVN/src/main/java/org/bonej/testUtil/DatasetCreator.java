package org.bonej.testUtil;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Iterator;
import java.util.Random;

import javax.annotation.Nullable;

import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.Cursor;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.*;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;

import org.scijava.AbstractContextual;
import org.scijava.plugin.Parameter;

/**
 * A utility class that can be used to automatically create different types of Datasets.
 * Handy for, e.g. testing.
 *
 * Call setContext before creating Datasets.
 *
 * @author  Richard Domander
 */
public final class DatasetCreator extends AbstractContextual {
    private static final long DEFAULT_WIDTH = 10;
    private static final long DEFAULT_HEIGHT = 10;
    private static final long DEFAULT_DEPTH = 10;
    private static final long[] DEFAULT_DIMS = {DEFAULT_WIDTH, DEFAULT_HEIGHT, DEFAULT_DEPTH};
    private static final AxisType[] DEFAULT_AXES = {Axes.X, Axes.Y, Axes.Z};

    @Parameter
    private DatasetService datasetService = null;

    /**
     * Creates a Dataset of the given type with the default dimensions (X = 10, Y = 10, Z = 10)
     * @see DatasetCreator#createDataset(DatasetType, AxisType[], long[])
     */
    public Dataset createDataset(DatasetType type) {
        return createDataset(type, DEFAULT_AXES, DEFAULT_DIMS);
    }

    /**
     * Creates a Dataset with three spatial dimensions (X, Y, Z) of the given type.
     *
     * @throws              NullPointerException if there's no DatasetService
     * @param type          The type of the Dataset - see Dataset#DatasetType
     * @param axesTypes     The types of the dimensions in the Dataset
     * @param dimensions    The sizes of the dimensions in the Dataset
     * @return A new Dataset, or null if type is not recognized
     */
    public Dataset createDataset(DatasetType type, AxisType[] axesTypes, long[] dimensions) 
            throws NullPointerException {
        checkNotNull(datasetService, "No datasetService available - did you call setContext?");

        switch (type) {
            case BIT:
                return datasetService.create(new BitType(), dimensions, "Dataset", axesTypes);
            case BYTE:
                return datasetService.create(new ByteType(), dimensions, "Dataset", axesTypes);
            case DOUBLE:
                return datasetService.create(new DoubleType(), dimensions, "Dataset", axesTypes);
            case FLOAT:
                return datasetService.create(new FloatType(), dimensions, "Dataset", axesTypes);
            case INT:
                return datasetService.create(new IntType(), dimensions, "Dataset", axesTypes);
            case LONG:
                return datasetService.create(new LongType(), dimensions, "Dataset", axesTypes);
            case SHORT:
                return datasetService.create(new ShortType(), dimensions, "Dataset", axesTypes);
            case UNSIGNED_128_BIT:
                return datasetService.create(new Unsigned128BitType(), dimensions, "Dataset", axesTypes);
            case UNSIGNED_12_BIT:
                return datasetService.create(new Unsigned12BitType(), dimensions, "Dataset", axesTypes);
            case UNSIGNED_2_BIT:
                return datasetService.create(new Unsigned2BitType(), dimensions, "Dataset", axesTypes);
            case UNSIGNED_4_BIT:
                return datasetService.create(new Unsigned4BitType(), dimensions, "Dataset", axesTypes);
            case UNSIGNED_BYTE:
                return datasetService.create(new UnsignedByteType(), dimensions, "Dataset", axesTypes);
            case UNSIGNED_SHORT:
                return datasetService.create(new UnsignedShortType(), dimensions, "Dataset", axesTypes);
            case UNSIGNED_INT:
                return datasetService.create(new UnsignedIntType(), dimensions, "Dataset", axesTypes);
            case UNSIGNED_LONG:
                return datasetService.create(new UnsignedLongType(), dimensions, "Dataset", axesTypes);
            case UNSIGNED_VARIABLE_BIT_LENGTH:
                return datasetService.create(new UnsignedVariableBitLengthType(64), dimensions, "Dataset", axesTypes);
            default:
                throw new AssertionError("Unhandled DatasetType value");
        }
    }

    /**
     * Fills the elements in the given Dataset with random whole numbers.
     *
     * @implNote        Min and max values are clamped to prevent under - and overflow.
     *                  E.g. If maxValue == 1000 and dataset type == UnsignedByteType, then maxValue = 255
     * @param minValue  Minimum value of the random numbers (inclusive)
     * @param maxValue  Maximum value of the random numbers (inclusive)
     */
    public static void fillWithRandomWholeNumbers(@Nullable final Dataset dataset, long minValue,
                                                  long maxValue) {
        if (dataset == null) {
            return;
        }

        final Cursor<RealType<?>> cursor = dataset.cursor();
        if (!cursor.hasNext()) {
            return;
        }

        cursor.fwd();
        final RealType<?> element = cursor.next();
        final long typeMin = (long) element.getMinValue();
        final long typeMax = (long) element.getMaxValue();
        minValue = clamp(minValue, typeMin, typeMax);
        maxValue = clamp(maxValue, typeMin, typeMax);
        cursor.reset();

        long exclusiveMax = maxValue + 1;

        final Iterator<Long> randomIterator =
                new Random(System.currentTimeMillis()).longs(minValue, exclusiveMax).iterator();

        cursor.forEachRemaining(c -> c.setReal(randomIterator.next()));
    }

    private static long clamp(final long value, final long min, final long max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    public enum DatasetType {
        BIT,
        BYTE,
        DOUBLE,
        FLOAT,
        INT,
        LONG,
        SHORT,
        UNSIGNED_128_BIT,
        UNSIGNED_12_BIT,
        UNSIGNED_2_BIT,
        UNSIGNED_4_BIT,
        UNSIGNED_BYTE,
        UNSIGNED_INT,
        UNSIGNED_LONG,
        UNSIGNED_SHORT,
        UNSIGNED_VARIABLE_BIT_LENGTH
    }
}
