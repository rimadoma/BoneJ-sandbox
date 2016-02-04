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
 * @todo Find out if test are exhaustive for possible Dataset types
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
    public void testInvalidElementTypesFail() throws AssertionError {
        DatasetType[] invalidTypes = {DatasetType.DOUBLE, DatasetType.FLOAT, DatasetType.INT, DatasetType.LONG,
                DatasetType.SHORT, DatasetType.UNSIGNED_12_BIT, DatasetType.UNSIGNED_128_BIT, DatasetType.UNSIGNED_INT,
                DatasetType.UNSIGNED_LONG, DatasetType.UNSIGNED_SHORT};

        for (DatasetType invalidType : invalidTypes) {
            dataset = datasetCreator.createDataset(invalidType);
            boolean result = (boolean) ij.op().run(DatasetIsBinary.class, dataset);
            assertFalse("Should not be binary", result);
        }
    }

    @Test
    public void testValidElementTypesPass() throws AssertionError {
        DatasetType[] validTypes = {DatasetType.BIT, DatasetType.BYTE, DatasetType.UNSIGNED_2_BIT,
                DatasetType.UNSIGNED_4_BIT, DatasetType.UNSIGNED_BYTE};

        for (DatasetType validType : validTypes) {
            dataset = datasetCreator.createDataset(validType);
            boolean result = (boolean) ij.op().run(DatasetIsBinary.class, dataset);
            assertTrue(dataset.getType().getClass().getName() + " should be binary", result);
        }
    }
}
