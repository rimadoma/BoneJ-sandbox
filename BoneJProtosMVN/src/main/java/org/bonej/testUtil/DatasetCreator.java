package org.bonej.testUtil;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Random;
import java.util.stream.IntStream;

import javax.annotation.Nullable;

import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.type.logic.BitType;
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
 * @todo    Extend to NativeType<T>?
 * @todo    VolatileRealType?
 * @todo    UnsignedVariableBitLengthType?
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
    @Nullable
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
    @Nullable
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
            default:
                return null;
        }
    }

    /**
     * Fills the given Dataset with random numbers.
     *
     * @todo    write methods for other Dataset types (or a generic version with Cursor?)
     * @throws  IllegalArgumentException if the Dataset isn't three dimensional, or has the wrong type
     * @param dataset   A 3D GenericIntType Dataset (X,Y,Z dimensions)
     * @param minValue  Minimum value of the random numbers (inclusive)
     * @param maxValue  Maximum value of the random numbers (inclusive)
     */
    public static void fillWithRandomData(final Dataset dataset, final int minValue, final int maxValue)
            throws IllegalArgumentException {
        checkArgument(dataset.getType() instanceof GenericIntType, "Dataset has the wrong type");

        final long width = datasetWidth(dataset);
        final long height = datasetHeight(dataset);
        final long depth = datasetDepth(dataset);

        checkArgument(width >= 0, "Dataset has no x-dimension");
        checkArgument(height >= 0, "Dataset has no y-dimension");
        checkArgument(depth >= 0, "Dataset has no z-dimension");

        final long planeSize = width * height;
        final int modulo = maxValue + 1;
        final Random random = new Random(System.currentTimeMillis());

        IntStream.range(0, (int)depth).forEach(z -> {
            int[] plane = random.ints(planeSize, minValue, modulo).toArray();
            dataset.setPlane(z, plane);
        });
    }

    public static long datasetDepth(Dataset dataset) {
        return datasetNamedDimension(dataset, Axes.Z);
    }

    public static long datasetHeight(Dataset dataset) {
        return datasetNamedDimension(dataset, Axes.Y);
    }

    public static long datasetWidth(Dataset dataset) {
        return datasetNamedDimension(dataset, Axes.X);
    }

    private static long datasetNamedDimension(Dataset dataset, AxisType axisType) {
        int index = dataset.dimensionIndex(axisType);

        if (index < 0) {
            return -1;
        }

        return dataset.dimension(index);
    }

    /**
     * Instead using a hacky enum, it'd be ideal to have a method with a RealType<?> parameter,
     * which would then somehow create a Dataset based on the runtime type of that parameter.
     */
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
        UNSIGNED_SHORT,
        UNSIGNED_INT,
        UNSIGNED_LONG
    }
}
