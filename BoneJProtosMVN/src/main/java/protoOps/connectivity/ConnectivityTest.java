package protoOps.connectivity;

import static org.junit.Assert.assertEquals;

import ij.measure.Calibration;
import org.junit.Test;

import protoOps.testImageCreators.StaticTestImageHelper;
import ij.ImagePlus;

/**
 * @author Richard Domander
 */
public class ConnectivityTest {
    private static final Connectivity connectivity = new Connectivity();
    private static final double DELTA = 1e-12;

    @Test
    public void testConnectionDensity() throws Exception {
		final double PIXEL_VOLUME = 0.2 * 0.2 * 0.2;
		final int CUBOID_WIDTH = 32;
		final int CUBOID_HEIGHT = 64;
		final int CUBOID_DEPTH = 128;
		final int PADDING = 32;
		final int TOTAL_PADDING = 2 * PADDING;
		final int CUBOID_VOLUME = (CUBOID_WIDTH + TOTAL_PADDING) * (CUBOID_HEIGHT + TOTAL_PADDING)
				* (CUBOID_DEPTH + TOTAL_PADDING);
		final double STACK_VOLUME = CUBOID_VOLUME * PIXEL_VOLUME;
		final double EXPECTED_CONNECTIVITY = 5.0;
		final double EXPECTED_DENSITY = EXPECTED_CONNECTIVITY / STACK_VOLUME;

        Calibration calibration = new Calibration();
        calibration.pixelWidth = 0.2;
        calibration.pixelHeight = 0.2;
        calibration.pixelDepth = 0.2;
        ImagePlus imagePlus = StaticTestImageHelper.createWireFrameCuboid(CUBOID_WIDTH, CUBOID_HEIGHT, CUBOID_DEPTH,
                PADDING);
        imagePlus.setCalibration(calibration);
        connectivity.setInputImage(imagePlus);
        connectivity.run();

        // The expected values are just magic numbers from the corresponding test in BoneJ1.
        // Assert to make sure that the functions return the same values.
        double eulerCharacteristic = connectivity.getEulerCharacteristic();
        assertEquals(-4.0, eulerCharacteristic, DELTA);

        double deltaChi = connectivity.getDeltaChi();
        assertEquals(-4.0, deltaChi, DELTA);

        double conn = connectivity.getConnectivity();
        assertEquals(EXPECTED_CONNECTIVITY, conn, DELTA);

        double connDensity = connectivity.getConnectivityDensity();
        assertEquals(EXPECTED_DENSITY, connDensity, DELTA);
    }

    /**
     * @todo Test fails if any cuboid dimension < 3
     */
    @Test
    public void testGetEulerCharacteristicBoxFrame() throws Exception {
        ImagePlus imagePlus = StaticTestImageHelper.createWireFrameCuboid(3, 16, 3,1);
        connectivity.setInputImage(imagePlus);
        connectivity.run();

        double eulerCharacteristic = connectivity.getEulerCharacteristic();
        assertEquals(-4.0, eulerCharacteristic, DELTA);
    }

    /**
     * @todo Test fails if crossed circle size < 12
     */
    @Test
    public void testGetEulerCharacteristicCrossedCircle() throws Exception {
        ImagePlus imagePlus = StaticTestImageHelper.createCrossedCircle(12);
        connectivity.setInputImage(imagePlus);
        connectivity.run();

        double eulerCharacteristic = connectivity.getEulerCharacteristic();
        assertEquals(-3.0, eulerCharacteristic, DELTA);
    }
}
