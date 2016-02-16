package protoOps.volumeFraction;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.awt.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import protoOps.testImageCreators.StaticTestImageHelper;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Roi;
import ij.measure.Calibration;
import ij.plugin.frame.RoiManager;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;

/**
 * Unit tests for the VolumeFractionVoxel class
 *
 * @author Richard Domander
 */
public class VolumeFractionVoxelTest {
    private static final double DELTA = 1E-12;
    private static final int WHITE = 255;
    private static final int MIN_THRESHOLD = 127;
    private static final int MAX_THRESHOLD = 255;

    private static final int WIDTH = 10;
    private static final int HEIGHT = 10;
    private static final int DEPTH = 10;
    private static final int PADDING = 1;
    private static final int TOTAL_PADDING = 2;

    private VolumeFractionVoxel volumeFractionVoxel;

    @Before
    public void setUp() {
        volumeFractionVoxel = new VolumeFractionVoxel();
    }

    @After
    public void tearDown() {
        volumeFractionVoxel = null;
    }

    /**
     * Test that volumes and volume fraction are correctly calculated
     */
    @Test
    public void testVolumeFractionVoxel() throws Exception {
        // Can't make cuboid a class member due IJ1 incompatibility issues
        final ImagePlus cuboid = StaticTestImageHelper.createCuboid(WIDTH, HEIGHT, DEPTH, WHITE, PADDING);
        final int CUBOID_VOLUME = WIDTH * HEIGHT * DEPTH;
        final int TOTAL_VOLUME = (WIDTH + TOTAL_PADDING) * (HEIGHT + TOTAL_PADDING) * (DEPTH + TOTAL_PADDING);

        volumeFractionVoxel.setImage(cuboid);
        volumeFractionVoxel.setThresholds(MIN_THRESHOLD, MAX_THRESHOLD);
        volumeFractionVoxel.run();

        assertEquals("Sample foreground volume is incorrect", CUBOID_VOLUME, volumeFractionVoxel.getForegroundVolume(),
                DELTA);
        assertEquals("Total sample volume is incorrect", TOTAL_VOLUME, volumeFractionVoxel.getTotalVolume(), DELTA);
        assertEquals("Volume ratio is incorrect", CUBOID_VOLUME / (double) TOTAL_VOLUME,
                volumeFractionVoxel.getVolumeRatio(), DELTA);
    }

    /**
     * Test that volumes and volume fraction are correctly calculated in the area defined by the ROIs
     * in the given ROIManager
     */
    @Test
    public void testVolumeFractionWithRoiManager() throws Exception {
        final ImagePlus cuboid = StaticTestImageHelper.createCuboid(WIDTH, HEIGHT, DEPTH, WHITE, PADDING);
        final int CUBOID_VOLUME = WIDTH * HEIGHT * 1;
        final int TOTAL_VOLUME = (WIDTH + TOTAL_PADDING) * (HEIGHT + TOTAL_PADDING) * 1;

        //Mock a RoiManager
        Roi roi = new Roi(0, 0, cuboid.getWidth(), cuboid.getHeight());
        roi.setName("0002-0000-0001");
        Roi rois[] = {roi};

        RoiManager mockManager = mock(RoiManager.class);
        when(mockManager.getRoisAsArray()).thenReturn(rois);
        when(mockManager.getCount()).thenReturn(rois.length);
        when(mockManager.getSliceNumber(anyString())).thenCallRealMethod();

        volumeFractionVoxel.setImage(cuboid);
        volumeFractionVoxel.setThresholds(MIN_THRESHOLD, MAX_THRESHOLD);
        volumeFractionVoxel.setRoiManager(mockManager);
        volumeFractionVoxel.run();

        assertEquals("Sample foreground volume is incorrect", CUBOID_VOLUME, volumeFractionVoxel.getForegroundVolume(),
                DELTA);
        assertEquals("Total sample volume is incorrect", TOTAL_VOLUME, volumeFractionVoxel.getTotalVolume(), DELTA);
        assertEquals("Volume ratio is incorrect", CUBOID_VOLUME / (double) TOTAL_VOLUME,
                volumeFractionVoxel.getVolumeRatio(), DELTA);
    }

    /**
     * Test that volumes and volume fraction are correctly calculated in the area defined by a mask
     */
    @Test
    public void testVolumeFractionWithMask() throws Exception {
        final double FOREGROUND_VOLUME = WIDTH * HEIGHT * DEPTH * 0.75;
        final double TOTAL_VOLUME = FOREGROUND_VOLUME;
        int white = 0xFF;

        ImageProcessor mask = createLMask(WIDTH, HEIGHT);

        // Mock Image* classes to make getMask() work
        // In a normal instance of ImageProcessor getMask() returns null,
        // even right after setMask().
        ImageProcessor mockProcessor = mock(ImageProcessor.class);
        when(mockProcessor.get(anyInt(), anyInt())).thenReturn(white);
        when(mockProcessor.getRoi()).thenReturn(new Rectangle(0, 0, WIDTH, HEIGHT));
        when(mockProcessor.getMask()).thenReturn(mask);

        ImageStack mockStack = mock(ImageStack.class);
        when(mockStack.getWidth()).thenReturn(WIDTH);
        when(mockStack.getHeight()).thenReturn(HEIGHT);
        when(mockStack.getSize()).thenReturn(DEPTH);
        when(mockStack.getProcessor(anyInt())).thenReturn(mockProcessor);

        ImageStatistics statistics = new ImageStatistics();
        statistics.histogram = new int[256];

        ImagePlus mockCuboid = mock(ImagePlus.class);
        when(mockCuboid.getStack()).thenReturn(mockStack);
        when(mockCuboid.getBitDepth()).thenReturn(8);
        when(mockCuboid.getStatistics()).thenReturn(statistics);
        when(mockCuboid.getType()).thenReturn(ImagePlus.GRAY8);
        when(mockCuboid.getCalibration()).thenReturn(new Calibration());

        volumeFractionVoxel.setImage(mockCuboid);
        volumeFractionVoxel.setThresholds(MIN_THRESHOLD, MAX_THRESHOLD);
        volumeFractionVoxel.run();

        assertEquals("Sample foreground volume is incorrect", FOREGROUND_VOLUME,
                volumeFractionVoxel.getForegroundVolume(), DELTA);
        assertEquals("Total sample volume is incorrect", TOTAL_VOLUME, volumeFractionVoxel.getTotalVolume(), DELTA);
        assertEquals("Volume ratio is incorrect", FOREGROUND_VOLUME / TOTAL_VOLUME,
                volumeFractionVoxel.getVolumeRatio(), DELTA);
    }

    /**
     * Creates an L-shaped mask that blocks the lower right-hand corner of an image
     *
     * NB width & height need to be the same than the dimensions of the image this mask is used on.
     *
     * @param width     Width of the mask
     * @param height    Height of the mask
     * @return An ImageProcessor that can be passed to ImageProcessor#setMask
     */
    private ImageProcessor createLMask(final int width, final int height) {
        ImageProcessor mask = new ByteProcessor(width, height);
        ImageProcessor tmp = new ByteProcessor(width, height);

        Polygon polygon = new Polygon();
        polygon.addPoint(0, 0);
        polygon.addPoint(width, 0);
        polygon.addPoint(width, height / 2);
        polygon.addPoint(width / 2, height / 2);
        polygon.addPoint(width / 2, height);
        polygon.addPoint(0, height);
        polygon.addPoint(0, 0);
        tmp.setRoi(polygon);

        mask.setPixels(tmp.getMask().getPixels());
        return mask;
    }
}
