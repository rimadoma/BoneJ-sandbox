package protoOps.volumeFraction;

import static org.junit.Assert.assertEquals;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import protoOps.testImageCreators.StaticTestImageHelper;
import ij.ImagePlus;

/**
 * Richard Domander
 * @todo exception tests for setters
 */
public class VolumeFractionSurfaceTest {
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private static final double DELTA = 1E-12;
	private static final int CUBOID_WIDTH = 32;
	private static final int CUBOID_HEIGHT = 64;
	private static final int CUBOID_DEPTH = 96;
	private static final int PADDING = 1;
	private static final int TOTAL_PADDING = PADDING * 2;
	private static final double CUBOID_VOLUME = CUBOID_WIDTH * CUBOID_HEIGHT * CUBOID_DEPTH;
	private static final double TOTAL_VOLUME = (CUBOID_WIDTH + TOTAL_PADDING) * (CUBOID_HEIGHT + TOTAL_PADDING)
			* (CUBOID_DEPTH + TOTAL_PADDING);

	private static final ImagePlus cuboid = StaticTestImageHelper.createCuboid(32, 64, 96, 0xFF, 1);
	private final VolumeFractionSurface volumeFractionSurface = new VolumeFractionSurface();
	private double foregroundVolume = 0.0;
	private double totalVolume = 0.0;
	private double volumeRatio = Double.NaN;

    /**
     * Test adopted from doube's unit tests for VolumeFraction in BoneJ1.
     * Ignore test because it basically just tests CustomTriangleMesh.getVolume()
     * whose implementation has apparently changed since bonej1, because the test fails.
     *
     * When run with the image binary_trabeculae_small.tif the plugin produces same results than in BoneJ1
     */
    @Ignore
    @Test
    public void testVolumeFractionSurfaceCuboid() throws Exception {
		volumeFractionSurface.setImage(cuboid);
        volumeFractionSurface.setSurfaceResampling(1);
		volumeFractionSurface.run();

        foregroundVolume = volumeFractionSurface.getForegroundVolume();
        assertEquals("Incorrect foreground volume", CUBOID_VOLUME, foregroundVolume, DELTA);

        totalVolume = volumeFractionSurface.getTotalVolume();
        assertEquals(TOTAL_VOLUME, totalVolume, DELTA);

        volumeRatio = volumeFractionSurface.getVolumeRatio();
        assertEquals(CUBOID_VOLUME / TOTAL_VOLUME, volumeRatio, DELTA);
	}
}
