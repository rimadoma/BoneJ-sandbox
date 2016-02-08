package protoOps.datasetCheck;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.type.numeric.integer.UnsignedByteType;

import org.junit.Test;

/**
 * Unit tests for the TestDatasetIs3D Op
 *
 * @author Richard Domander
 */
public class TestDatasetIs3D {
    private static final ImageJ imageJInstance = new ImageJ();

    @Test
    public void Test2DImageFails() {
        final long[] dims = {100, 100, 10};
        final AxisType[] axisTypes = {Axes.X, Axes.Y, Axes.TIME};
        Dataset dataset = imageJInstance.dataset().create(new UnsignedByteType(), dims, "Test image", axisTypes);

        boolean result = (boolean) imageJInstance.op().run("datasetIs3D", dataset, false);
        assertFalse("An image with two spatial dimensions is not 3D", result);
    }

    @Test
    public void Test3DImagePasses() {
        final long[] dims = {100, 100, 100, 3};
        final AxisType[] axisTypes = {Axes.X, Axes.Y, Axes.Z, Axes.CHANNEL};
        Dataset dataset = imageJInstance.dataset().create(new UnsignedByteType(), dims, "Test image", axisTypes);

        boolean result = (boolean) imageJInstance.op().run("datasetIs3D", dataset, false);
        assertTrue("An image with three spatial dimensions is 3D", result);
    }

    @Test
    public void Test4DImageFails() {
        final AxisType W = Axes.get("W", true);
        final long[] dims = {100, 100, 100, 100};
        final AxisType[] axisTypes = {Axes.X, Axes.Y, Axes.Z, W};
        Dataset dataset = imageJInstance.dataset().create(new UnsignedByteType(), dims, "Test image", axisTypes);

        boolean result = (boolean) imageJInstance.op().run("datasetIs3D", dataset, false);
        assertFalse("An image with four spatial dimensions is not 3D", result);
    }

    @Test
    public void TestOnlySpatial() {
        final long[] dims = {100, 100, 100};
        final AxisType[] axisTypes = {Axes.X, Axes.Y, Axes.Z};
        Dataset dataset = imageJInstance.dataset().create(new UnsignedByteType(), dims, "Test image", axisTypes);

        boolean result = (boolean) imageJInstance.op().run("datasetIs3D", dataset, true);
        assertTrue("An image with exactly three spatial dimensions is 3D", result);

        final long[] extraDims = {100, 100, 100, 3};
        final AxisType[] extraAxisTypes = {Axes.X, Axes.Y, Axes.Z, Axes.CHANNEL};
        Dataset extraDataset = imageJInstance.dataset().create(new UnsignedByteType(), extraDims, "Test image",
                extraAxisTypes);

        result = (boolean) imageJInstance.op().run("datasetIs3D", extraDataset, true);
        assertFalse("An image with non-spatial images is not strictly 3D", result);
    }
}
