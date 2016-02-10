package protoOps.datasetCheck;

import static org.junit.Assert.assertEquals;
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

import java.util.ArrayList;

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

        boolean result = (boolean) ((ArrayList<Object>)ij.op().run(DatasetIsBinary.class, dataset)).get(0);
        assertFalse("Empty dataset is not binary", result);
    }

    @Test
    public void testDatasetWithOneValue() throws AssertionError {
        final int minValue = 1;
        final int maxValue = 1;
        dataset = datasetCreator.createDataset(DatasetType.INT);
        DatasetCreator.fillWithRandomWholeNumbers(dataset, minValue, maxValue);

        ArrayList<Object> results = (ArrayList<Object>) ij.op().run(DatasetIsBinary.class, dataset);
        final boolean isBinary = (boolean) results.get(0);
        final long background = (long) results.get(1);
        final long foreground = (long) results.get(2);

        assertTrue("A Dataset with one distinct value is binary", isBinary);
        assertEquals("Background value is incorrect", minValue, background);
        assertEquals("Foreground value is incorrect", maxValue, foreground);
        assertEquals("Background should equal foreground", foreground, background);
    }

    @Test
    public void testDatasetWithTwoValues() throws AssertionError {
        final int minValue = 1;
        final int maxValue = 2;
        dataset = datasetCreator.createDataset(DatasetType.INT);
        DatasetCreator.fillWithRandomWholeNumbers(dataset, minValue, maxValue);

        ArrayList<Object> results = (ArrayList<Object>) ij.op().run(DatasetIsBinary.class, dataset);
        final boolean isBinary = (boolean) results.get(0);
        final long background = (long) results.get(1);
        final long foreground = (long) results.get(2);

        assertTrue("A Dataset with two distinct values is binary", isBinary);
        assertEquals("Background value is incorrect", minValue, background);
        assertEquals("Foreground value is incorrect", maxValue, foreground);
    }

    @Test
    public void testDatasetWithMoreThanTwoValuesFails() throws AssertionError {
        dataset = datasetCreator.createDataset(DatasetType.INT);
        DatasetCreator.fillWithRandomWholeNumbers(dataset, 0, 2);

        boolean result = (boolean) ((ArrayList<Object>)ij.op().run(DatasetIsBinary.class, dataset)).get(0);
        assertFalse("A Dataset containing more than two distinct values is not binary", result);
    }

    @Test
    public void testValuesAreInverted() throws AssertionError {
        final int minValue = -1;
        final int maxValue = 0;
        dataset = datasetCreator.createDataset(DatasetType.INT);
        DatasetCreator.fillWithRandomWholeNumbers(dataset, minValue, maxValue);

        ArrayList<Object> results = (ArrayList<Object>) ij.op().run(DatasetIsBinary.class, dataset, true);
        final long background = (long) results.get(1);
        final long foreground = (long) results.get(2);

        assertEquals("Background value is incorrect", maxValue, background);
        assertEquals("Foreground value is incorrect", minValue, foreground);
        assertTrue("Foreground value should be smaller when using the inverted option", foreground <= background);
    }
}
