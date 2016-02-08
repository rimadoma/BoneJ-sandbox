package protoOps.datasetCheck;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.type.numeric.integer.UnsignedByteType;

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
        dataset = ij.dataset().create(new UnsignedByteType(), dims, "Test set", axisTypes);

        boolean result = (boolean) ij.op().run(DatasetIsBinary.class, dataset);

        assertFalse("Empty dataset is not binary", result);
    }

    @Test
    public void testDatasetWithOneValuePasses() throws AssertionError {
        Dataset dataset = datasetCreator.createDataset(DatasetType.INT);
        DatasetCreator.fillWithRandomData(dataset, 1, 1);

        boolean result = (boolean) ij.op().run(DatasetIsBinary.class, dataset);
        assertTrue("A Dataset with one distinct value is binary", result);
    }

    @Test
    public void testDatasetWithTwoValuesPasses() throws AssertionError {
        Dataset dataset = datasetCreator.createDataset(DatasetType.INT);
        DatasetCreator.fillWithRandomData(dataset, 0, 1);

        boolean result = (boolean) ij.op().run(DatasetIsBinary.class, dataset);
        assertTrue("A Dataset with two distinct values is binary", result);
    }

    @Test
    public void testDatasetWithMoreThanTwoValuesFails() throws AssertionError {
        Dataset dataset = datasetCreator.createDataset(DatasetType.INT);
        DatasetCreator.fillWithRandomData(dataset, 0, 2);

        boolean result = (boolean) ij.op().run(DatasetIsBinary.class, dataset);
        assertFalse("A Dataset containing more than two distinct values is not binary", result);
    }
}
