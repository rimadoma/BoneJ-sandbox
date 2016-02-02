package protoOps.volumeFraction;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.awt.*;
import java.util.ArrayList;

import net.imagej.ImageJ;

import org.junit.Test;

import protoOps.testImageCreators.StaticTestImageHelper;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Roi;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;

/**
 * Unit tests for the VolumeFractionVoxel class
 *
 * @author Richard Domander
 */
public class VolumeFractionVoxelTest {
    private static final ImageJ imageJInstance = new ImageJ();
    private static final double DELTA = 1E-12;
    private static final int WHITE = 255;
    private static final int MIN_THRESHOLD = 127;
    private static final int MAX_THRESHOLD = 255;

    private static final int WIDTH = 10;
    private static final int HEIGHT = 10;
    private static final int DEPTH = 10;
    private static final int PADDING = 1;
    private static final int TOTAL_PADDING = 2;

    private static final int FG_INDEX = 0;
    private static final int TOTAL_INDEX = 1;
    private static final int RATIO_INDEX = 2;


    /**
     * Test that volumes and volume fraction are correctly calculated
     */
    @Test
    public void testVolumeFractionOpServiceRun() throws Exception {
        // Can't make cuboid a class member due IJ1 incompatibility issues
        final ImagePlus cuboid = StaticTestImageHelper.createCuboid(WIDTH, HEIGHT, DEPTH, WHITE, PADDING);
        final int CUBOID_VOLUME = WIDTH * HEIGHT * DEPTH;
        final int TOTAL_VOLUME = (WIDTH + TOTAL_PADDING) * (HEIGHT + TOTAL_PADDING) * (DEPTH + TOTAL_PADDING);

        final ArrayList<Double> volumes = (ArrayList<Double>) imageJInstance.op().run("volumeFractionVoxel", cuboid,
                MIN_THRESHOLD, MAX_THRESHOLD);

        assertEquals("Sample foreground volume is incorrect", CUBOID_VOLUME, volumes.get(FG_INDEX), DELTA);
        assertEquals("Total sample volume is incorrect", TOTAL_VOLUME, volumes.get(TOTAL_INDEX), DELTA);
        assertEquals("Volume ratio is incorrect", CUBOID_VOLUME / (double) TOTAL_VOLUME, volumes.get(RATIO_INDEX),
                DELTA);
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
        when(mockManager.getSliceNumber(anyString())).thenCallRealMethod();

        final ArrayList<Double> volumes = (ArrayList<Double>) imageJInstance.op().run("volumeFractionVoxel", cuboid,
                MIN_THRESHOLD, MAX_THRESHOLD, mockManager);

        assertEquals("Sample foreground volume is incorrect", CUBOID_VOLUME, volumes.get(FG_INDEX), DELTA);
        assertEquals("Total sample volume is incorrect", TOTAL_VOLUME, volumes.get(TOTAL_INDEX), DELTA);
        assertEquals("Volume ratio is incorrect", CUBOID_VOLUME / (double) TOTAL_VOLUME, volumes.get(RATIO_INDEX),
                DELTA);
    }

    /**
     * Test that volumes and volume fraction are correctly calculated in the area defined by a mask
     * @todo Test fails because after setup the mask in cuboid == null
     */
    @Test
    public void testVolumeFractionWithMask() throws Exception {
        final ImagePlus cuboid = StaticTestImageHelper.createCuboid(WIDTH, HEIGHT, DEPTH, WHITE, 0);
        final double CUBOID_VOLUME = WIDTH * HEIGHT * DEPTH * 0.75;
        final double TOTAL_VOLUME = CUBOID_VOLUME;

        // Add an L-shaped mask on each slice in the stack of the cuboid
        Polygon polygon = new Polygon();
        polygon.addPoint(0, 0);
        polygon.addPoint(WIDTH, 0);
        polygon.addPoint(WIDTH, HEIGHT / 2);
        polygon.addPoint(WIDTH / 2, HEIGHT / 2);
        polygon.addPoint(WIDTH / 2, HEIGHT);
        polygon.addPoint(0, HEIGHT);
        polygon.addPoint(0, 0);

        final ImageStack stack = cuboid.getStack();
        for (int z = 1; z <= stack.getSize(); z++) {
            final ImageProcessor processor = stack.getProcessor(z);
            processor.setRoi(polygon);
        }

        // run op
        final ArrayList<Double> volumes = (ArrayList<Double>) imageJInstance.op().run("volumeFractionVoxel", cuboid,
                MIN_THRESHOLD, MAX_THRESHOLD);

        assertEquals("Sample foreground volume is incorrect", CUBOID_VOLUME, volumes.get(FG_INDEX), DELTA);
        assertEquals("Total sample volume is incorrect", TOTAL_VOLUME, volumes.get(TOTAL_INDEX), DELTA);
        assertEquals("Volume ratio is incorrect", CUBOID_VOLUME / TOTAL_VOLUME, volumes.get(RATIO_INDEX),
                DELTA);
    }
}
