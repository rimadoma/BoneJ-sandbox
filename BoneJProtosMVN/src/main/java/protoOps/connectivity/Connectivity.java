package protoOps.connectivity;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import net.imagej.ops.Op;
import net.imagej.ops.OpEnvironment;

import org.bonej.common.ImageCheck;
import org.bonej.common.MultiThreader;
import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;

/**
 * @author Michael Doube
 * @author Richard Domander
 */
@Plugin(type = Op.class, name = "eulerConnectivity")
public class Connectivity implements Op {
    private static final int EULER_LUT[] = new int[256];
    private static final int FOREGROUND = -1;

    static {
        EULER_LUT[1] = 1;
        EULER_LUT[7] = -1;
        EULER_LUT[9] = -2;
        EULER_LUT[11] = -1;
        EULER_LUT[13] = -1;

        EULER_LUT[19] = -1;
        EULER_LUT[21] = -1;
        EULER_LUT[23] = -2;
        EULER_LUT[25] = -3;
        EULER_LUT[27] = -2;

        EULER_LUT[29] = -2;
        EULER_LUT[31] = -1;
        EULER_LUT[33] = -2;
        EULER_LUT[35] = -1;
        EULER_LUT[37] = -3;

        EULER_LUT[39] = -2;
        EULER_LUT[41] = -1;
        EULER_LUT[43] = -2;
        EULER_LUT[47] = -1;
        EULER_LUT[49] = -1;

        EULER_LUT[53] = -2;
        EULER_LUT[55] = -1;
        EULER_LUT[59] = -1;
        EULER_LUT[61] = 1;
        EULER_LUT[65] = -2;

        EULER_LUT[67] = -3;
        EULER_LUT[69] = -1;
        EULER_LUT[71] = -2;
        EULER_LUT[73] = -1;
        EULER_LUT[77] = -2;

        EULER_LUT[79] = -1;
        EULER_LUT[81] = -1;
        EULER_LUT[83] = -2;
        EULER_LUT[87] = -1;
        EULER_LUT[91] = 1;

        EULER_LUT[93] = -1;
        EULER_LUT[97] = -1;
        EULER_LUT[103] = 1;
        EULER_LUT[105] = 4;
        EULER_LUT[107] = 3;

        EULER_LUT[109] = 3;
        EULER_LUT[111] = 2;
        EULER_LUT[113] = -2;
        EULER_LUT[115] = -1;
        EULER_LUT[117] = -1;
        EULER_LUT[121] = 3;

        EULER_LUT[123] = 2;
        EULER_LUT[125] = 2;
        EULER_LUT[127] = 1;
        EULER_LUT[129] = -6;
        EULER_LUT[131] = -3;

        EULER_LUT[133] = -3;
        EULER_LUT[137] = -3;
        EULER_LUT[139] = -2;
        EULER_LUT[141] = -2;
        EULER_LUT[143] = -1;

        EULER_LUT[145] = -3;
        EULER_LUT[151] = 3;
        EULER_LUT[155] = 1;
        EULER_LUT[157] = 1;
        EULER_LUT[159] = 2;

        EULER_LUT[161] = -3;
        EULER_LUT[163] = -2;
        EULER_LUT[167] = 1;
        EULER_LUT[171] = -1;
        EULER_LUT[173] = 1;

        EULER_LUT[177] = -2;
        EULER_LUT[179] = -1;
        EULER_LUT[181] = 1;
        EULER_LUT[183] = 2;
        EULER_LUT[185] = 1;

        EULER_LUT[189] = 2;
        EULER_LUT[191] = 1;
        EULER_LUT[193] = -3;
        EULER_LUT[197] = -2;
        EULER_LUT[199] = 1;

        EULER_LUT[203] = 1;
        EULER_LUT[205] = -1;
        EULER_LUT[209] = -2;
        EULER_LUT[211] = 1;
        EULER_LUT[213] = -1;

        EULER_LUT[215] = 2;
        EULER_LUT[217] = 1;
        EULER_LUT[219] = 2;
        EULER_LUT[223] = 1;
        EULER_LUT[227] = 1;

        EULER_LUT[229] = 1;
        EULER_LUT[231] = 2;
        EULER_LUT[233] = 3;
        EULER_LUT[235] = 2;
        EULER_LUT[237] = 2;

        EULER_LUT[239] = 1;
        EULER_LUT[241] = -1;
        EULER_LUT[247] = 1;
        EULER_LUT[249] = 2;
        EULER_LUT[251] = 1;

        EULER_LUT[253] = 1;
    }

    private int width = 0;
    private int height = 0;
    private int depth = 0;
    private ImageStack inputStack = null;

    @Parameter(type = ItemIO.INPUT)
    private ImagePlus inputImage = null;

    @Parameter(type = ItemIO.OUTPUT)
    private double eulerCharacteristic = 0.0;

    @Parameter(type = ItemIO.OUTPUT)
    private double deltaChi = 0.0;

    @Parameter(type = ItemIO.OUTPUT)
    private double connectivity = 0.0;

    @Parameter(type = ItemIO.OUTPUT)
    private double connectivityDensity = 0.0;

    public double getEulerCharacteristic() {
        return eulerCharacteristic;
    }

    public double getDeltaChi() {
        return deltaChi;
    }

    public double getConnectivity() {
        return connectivity;
    }

    public double getConnectivityDensity() {
        return connectivityDensity;
    }

    @Override
    public OpEnvironment ops() {
        return null;
    }

    @Override
    public void setEnvironment(OpEnvironment opEnvironment) {

    }

    @Override
    public void run() {
        checkImage(inputImage);

        calculateEulerCharacteristic();
        calculateDeltaChi();
        calculateConnectivity();
        calculateConnectivityDensity();
    }

    /**
     * Sets the input image for processing
     *
     * @throws NullPointerException
     *             if image == null
     * @throws IllegalArgumentException
     *             if image is not binary
     */
    public void setInputImage(ImagePlus image) {
        checkImage(image);

        inputImage = image;
        inputStack = image.getStack();
        width = image.getWidth();
        height = image.getHeight();
        depth = image.getNSlices();
    }

    //region -- Helper methods --
    /**
     * @todo Check that there's only one object with 3D_Objects_Counter?
     */
    private static void checkImage(ImagePlus imagePlus) {
        checkNotNull(imagePlus, "Must have an input image");
        checkArgument(ImageCheck.isBinary(imagePlus), "Input image must be binary");
    }

    private void calculateConnectivityDensity() {

    }

    private void calculateConnectivity() {

    }

    private void calculateDeltaChi() {
    }

    private void calculateEulerCharacteristic() {
		eulerCharacteristic = 0;
		final AtomicInteger atomicCounter = new AtomicInteger(0);

        // Needed for concurrency. Just summing to eulerCharacteristic won't work if it isn't atomic.
        // Making it atomic would diminish the benefits of parallelization.
		final int[] sumEulerInt = new int[depth + 1];

		MultiThreader.startTask(() -> {
			for (int z = atomicCounter.getAndIncrement(); z <= depth; z = atomicCounter.getAndIncrement()) {
				for (int y = 0; y <= height; y++) {
					for (int x = 0; x <= width; x++) {
						Octant octant = getOctant(x, y, z);
						sumEulerInt[z] += deltaEuler(octant);
					}
				}
			}
		});

		eulerCharacteristic = Arrays.stream(sumEulerInt).sum();
		eulerCharacteristic /= 8.0;
    }

    private int deltaEuler(Octant octant) {
        if (octant.isEmpty()) {
            return 0;
        }

        int index;

        if (octant.neighbors[8] == FOREGROUND) {
            index = 1;
            if (octant.neighbors[1] == FOREGROUND)
                index |= 128;
            if (octant.neighbors[2] == FOREGROUND)
                index |= 64;
            if (octant.neighbors[3] == FOREGROUND)
                index |= 32;
            if (octant.neighbors[4] == FOREGROUND)
                index |= 16;
            if (octant.neighbors[5] == FOREGROUND)
                index |= 8;
            if (octant.neighbors[6] == FOREGROUND)
                index |= 4;
            if (octant.neighbors[7] == FOREGROUND)
                index |= 2;
        } else if (octant.neighbors[7] == FOREGROUND) {
            index = 1;
            if (octant.neighbors[2] == FOREGROUND)
                index |= 128;
            if (octant.neighbors[4] == FOREGROUND)
                index |= 64;
            if (octant.neighbors[1] == FOREGROUND)
                index |= 32;
            if (octant.neighbors[3] == FOREGROUND)
                index |= 16;
            if (octant.neighbors[6] == FOREGROUND)
                index |= 8;
            if (octant.neighbors[5] == FOREGROUND)
                index |= 2;
        } else if (octant.neighbors[6] == FOREGROUND) {
            index = 1;
            if (octant.neighbors[3] == FOREGROUND)
                index |= 128;
            if (octant.neighbors[1] == FOREGROUND)
                index |= 64;
            if (octant.neighbors[4] == FOREGROUND)
                index |= 32;
            if (octant.neighbors[2] == FOREGROUND)
                index |= 16;
            if (octant.neighbors[5] == FOREGROUND)
                index |= 4;
        } else if (octant.neighbors[5] == FOREGROUND) {
            index = 1;
            if (octant.neighbors[4] == FOREGROUND)
                index |= 128;
            if (octant.neighbors[3] == FOREGROUND)
                index |= 64;
            if (octant.neighbors[2] == FOREGROUND)
                index |= 32;
            if (octant.neighbors[1] == FOREGROUND)
                index |= 16;
        } else if (octant.neighbors[4] == FOREGROUND) {
            index = 1;
            if (octant.neighbors[1] == FOREGROUND)
                index |= 8;
            if (octant.neighbors[3] == FOREGROUND)
                index |= 4;
            if (octant.neighbors[2] == FOREGROUND)
                index |= 2;
        } else if (octant.neighbors[3] == FOREGROUND) {
            index = 1;
            if (octant.neighbors[2] == FOREGROUND)
                index |= 8;
            if (octant.neighbors[1] == FOREGROUND)
                index |= 4;
        } else if (octant.neighbors[2] == FOREGROUND) {
            index = 1;
            if (octant.neighbors[1] == FOREGROUND)
                index |= 2;
        } else {
            // if we have got here, all the other voxels are background
            index = 1;
        }

        return EULER_LUT[index];
    }

    private Octant getOctant(final int x, final int y, final int z) {
        Octant octant = new Octant();

        octant.neighbors[1] = getPixel(x - 1, y - 1, z - 1);
        octant.neighbors[2] = getPixel(x - 1, y, z - 1);
        octant.neighbors[3] = getPixel(x, y - 1, z - 1);
        octant.neighbors[4] = getPixel(x, y, z - 1);
        octant.neighbors[5] = getPixel(x - 1, y - 1, z);
        octant.neighbors[6] = getPixel(x - 1, y, z);
        octant.neighbors[7] = getPixel(x, y - 1, z);
        octant.neighbors[8] = getPixel(x, y, z);

        octant.countNeighbors();

        return octant;
    }

    private byte getPixel(final int x, final int y, final int z) {
        if (z < 0 || z >= depth) {
            return 0;
        }

        ImageProcessor slice = inputStack.getProcessor(z + 1);
        return (byte) slice.getPixel(x, y);
    }
    //endregion

    //region -- Helper classes --
    private final class Octant {
        public int neighborCount = 0;
        public final byte neighbors[] = new byte[9];

        public boolean isEmpty() { return neighborCount == 0; }

        public void countNeighbors() {
            neighborCount = 0;

            for (int n : neighbors) {
                neighborCount -= n;
            }
        }
    }
    //endregion
}
