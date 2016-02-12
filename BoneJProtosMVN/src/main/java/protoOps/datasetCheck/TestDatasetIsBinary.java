package protoOps.datasetCheck;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.stream.Stream;

import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.type.logic.BitType;

import org.bonej.testUtil.DatasetCreator;
import org.bonej.testUtil.DatasetCreator.DatasetType;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Unit tests for the DatasetIsBinary Op
 *
 * @author Richard Domander
 */
public class TestDatasetIsBinary {
    private static final ImageJ ij = new ImageJ();
    private static Dataset dataset = null;
    private static final DatasetCreator datasetCreator = new DatasetCreator();

    @BeforeClass
    public static void oneTimeSetup() {
        datasetCreator.setContext(ij.getContext());
    }

    @After
    public void tearDown() {
        dataset = null;
    }

    @AfterClass
    public static void oneTimeTearDown() {
        ij.context().dispose();
    }

    @Test
    public void testEmptyDatasetFails() throws AssertionError {
        final long[] dims = {0, 0};
        final AxisType[] axisTypes = {Axes.X, Axes.Y};
        dataset = ij.dataset().create(new BitType(), dims, "Test set", axisTypes);

        final boolean result = (boolean) ij.op().run(DatasetIsBinary.class, dataset);
        assertFalse("Empty dataset is not binary", result);
    }

    @Test
    public void testDatasetWithOneValuePasses() throws AssertionError {
        final int minValue = 1;
        final int maxValue = 1;
        dataset = datasetCreator.createDataset(DatasetType.BIT);
        DatasetCreator.fillWithRandomWholeNumbers(dataset, minValue, maxValue);

        final boolean result = (boolean) ij.op().run(DatasetIsBinary.class, dataset);

        assertTrue("A Dataset with one distinct value is binary", result);
    }

    @Test
    public void testDatasetWithTwoValuesPasses() throws AssertionError {
        final int minValue = 0;
        final int maxValue = 1;
        dataset = datasetCreator.createDataset(DatasetType.BIT);
        DatasetCreator.fillWithRandomWholeNumbers(dataset, minValue, maxValue);

        final boolean result = (boolean) ij.op().run(DatasetIsBinary.class, dataset);

        assertTrue("A Dataset with two distinct values is binary", result);
    }

    @Test
    public void testInvalidDatasetTypesFail() throws AssertionError {
        final Stream<DatasetType> allTypes = Arrays.stream(DatasetType.values());
        allTypes.filter(t -> t != DatasetType.BIT).forEach( type -> {
            dataset = datasetCreator.createDataset(type);
            final boolean result = (boolean) ij.op().run(DatasetIsBinary.class, dataset);
            final String typeClassName = dataset.getType().getClass().getName();
            assertFalse("A Dataset of type " + typeClassName + " should not be binary", result);
        });
    }
}