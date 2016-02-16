package protoOps.volumeFraction;

import static org.junit.Assert.assertEquals;

import org.junit.*;
import org.junit.rules.ExpectedException;

import protoOps.testImageCreators.StaticTestImageHelper;
import ij.ImagePlus;

/**
 * Unit tests for the VolumeFractionSurface Op
 *
 * Richard Domander
 */
public class VolumeFractionSurfaceTest {
    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    private static final double DELTA = 1E-12;
	private static final int CUBOID_WIDTH = 32;
	private static final int CUBOID_HEIGHT = 64;
	private static final int CUBOID_DEPTH = 96;
	private static final int PADDING = 1;
	private static final int TOTAL_PADDING = PADDING * 2;
	private static final double CUBOID_VOLUME = CUBOID_WIDTH * CUBOID_HEIGHT * CUBOID_DEPTH;
	private static final double TOTAL_VOLUME = (CUBOID_WIDTH + TOTAL_PADDING) * (CUBOID_HEIGHT + TOTAL_PADDING)
			* (CUBOID_DEPTH + TOTAL_PADDING);

    private VolumeFractionSurface volumeFractionSurface;

    @Before
    public void setUp() {
        volumeFractionSurface = new VolumeFractionSurface();
    }

    @After
    public void tearDown() {
        volumeFractionSurface = null;
    }

    @Test
    public void testSetSurfaceResamplingThrowsIllegalArgumentExceptionIfArgumentIsNegative() throws Exception {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("Resampling value must be >= 0");

        volumeFractionSurface.setSurfaceResampling(-1);
    }

    /**
     * Copied from BoneJ1's unit tests. Fails because the implementation of
     * CustomTriangleMesh#getVolume() has changed since then..?
     * When run with the image binary_trabeculae_small.tif the plugin produces same results than in BoneJ1
     */
    @Test
    public void testVolumeFractionSurfaceCuboid() throws Exception {
        final ImagePlus cuboid = StaticTestImageHelper.createCuboid(CUBOID_WIDTH, CUBOID_HEIGHT, CUBOID_DEPTH, 0xFF,
                PADDING);

		volumeFractionSurface.setImage(cuboid);
        volumeFractionSurface.setSurfaceResampling(1);
		volumeFractionSurface.run();

        double foregroundVolume = volumeFractionSurface.getForegroundVolume();
        assertEquals("Incorrect foreground volume", CUBOID_VOLUME, foregroundVolume, DELTA);

        double totalVolume = volumeFractionSurface.getTotalVolume();
        assertEquals(TOTAL_VOLUME, totalVolume, DELTA);

        double volumeRatio = volumeFractionSurface.getVolumeRatio();
        assertEquals(CUBOID_VOLUME / TOTAL_VOLUME, volumeRatio, DELTA);
	}
}
