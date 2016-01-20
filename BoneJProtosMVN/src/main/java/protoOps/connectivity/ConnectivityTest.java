package protoOps.connectivity;

import static org.junit.Assert.*;

import ij.ImagePlus;
import org.junit.Test;
import protoOps.testImageCreators.StaticTestImageHelper;

/**
 * @author Richard Domander
 */
public class ConnectivityTest {
    private static final Connectivity connectivity = new Connectivity();
    private static final double DELTA = 1e-12;

    @Test
    public void testGetEulerCharacteristicBoxFrame() throws Exception {
        ImagePlus imagePlus = StaticTestImageHelper.createWireFrameCuboid(16, 16, 16, 1);
        connectivity.setInputImage(imagePlus);
        connectivity.run();
        double eulerCharacteristic = connectivity.getEulerCharacteristic();
        assertEquals(-4, eulerCharacteristic, DELTA);
    }

    @Test
    public void testGetEulerCharacteristicCrossedCircle() throws Exception {
        ImagePlus imagePlus = StaticTestImageHelper.createCrossedCircle(16);
        connectivity.setInputImage(imagePlus);
        connectivity.run();
        double eulerCharacteristic = connectivity.getEulerCharacteristic();
        assertEquals(-3, eulerCharacteristic, DELTA);
    }
}
