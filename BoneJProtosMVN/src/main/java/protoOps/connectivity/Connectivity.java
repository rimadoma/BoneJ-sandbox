package protoOps.connectivity;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import ij.measure.Calibration;
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
 *
 * How to run programmatically:
 * You can call the plugin via an opService, or you can initialize an object and then:
 * 1) call setInputImage(ImagePlus image)
 * 2) call run()
 *
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
        double stackVolume = width * height * depth;

        Calibration calibration = inputImage.getCalibration();
        double pixelVolume = calibration.pixelWidth * calibration.pixelHeight * calibration.pixelDepth;

        double volume = stackVolume * pixelVolume;

        connectivityDensity = connectivity / volume;
    }

    private void calculateConnectivity() {
        connectivity = 1.0 - deltaChi;
    }

    private void calculateDeltaChi() {
        deltaChi = eulerCharacteristic - getEdgeCorrection();
    }

    private boolean isNeighborhoodForeground(int x, int y, int z, Orientation1D orientation) {
        switch (orientation) {
            case X:
                return getPixel(x, y, z) == FOREGROUND || getPixel(x, y, z) == FOREGROUND;
            case Y:
                return getPixel(x, y, z) == FOREGROUND || getPixel(x, y - 1, z) == FOREGROUND;
            case Z:
                return getPixel(x, y, z) == FOREGROUND || getPixel(x, y, z - 1) == FOREGROUND;
            default:
                return false;
        }
    }

    private boolean isNeighborhoodForeground(int x, int y, int z, Orientation2D orientation)
    {
        switch (orientation) {
            case XY:
                return getPixel(x, y, z) == FOREGROUND || getPixel(x, y - 1, z) == FOREGROUND
                        || getPixel(x - 1, y - 1, z) == FOREGROUND || getPixel(x - 1, y, z) == FOREGROUND;
            case XZ:
                return getPixel(x, y, z) == FOREGROUND || getPixel(x, y, z - 1) == FOREGROUND
                        || getPixel(x - 1, y, z - 1) == FOREGROUND || getPixel(x - 1, y, z) == FOREGROUND;
            case YZ:
                return getPixel(x, y, z) == FOREGROUND || getPixel(x, y - 1, z) == FOREGROUND
                        || getPixel(x, y - 1, z - 1) == FOREGROUND || getPixel(x, y, z - 1) == FOREGROUND;
            default:
                return false;
        }
    }

    private double getEdgeCorrection() {
        double chiZero = getStackVertices();
        double e = getStackEdges() + 3.0 * chiZero;
        double c = getStackFaces() + 2.0 * e - 3.0 * chiZero;

        double d = getEdgeVertices() + chiZero;
        double a = getFaceVertices();
        double b = getFaceEdges();

        double chiOne = d - e;
        double chiTwo = a - b + c;

        double edgeCorrection = chiTwo / 2.0 + chiOne / 4.0 + chiZero / 8.0;
        return edgeCorrection;
    }

    private double getFaceEdges() {
        long nFaceEdges = 0;
        int xInc = Math.max(1, width - 1);
        int yInc = Math.max(1, height - 1);
        int zInc = Math.max(1, depth - 1);

        // top and bottom faces (all 4 edges)
        // check 2 edges per voxel
        for (int z = 0; z < depth; z += zInc) {
            for (int y = 0; y <= height; y++) {
                for (int x = 0; x <= width; x++) {
                    // if the voxel or any of its neighbours are foreground, the
                    // vertex is counted
                    if (getPixel(x, y, z) == FOREGROUND) {
                        nFaceEdges += 2;
                        continue;
                    }

                    if (getPixel(x, y - 1, z) == FOREGROUND) {
                        nFaceEdges++;
                    }

                    if (getPixel(x - 1, y, z) == FOREGROUND) {
                        nFaceEdges++;
                    }
                }
            }
        }

        // back and front faces, horizontal edges
        for (int y = 0; y < height; y += yInc) {
            for (int z = 1; z < depth; z++) {
                for (int x = 0; x < width; x++) {
                    if (isNeighborhoodForeground(x, y, z, Orientation1D.Z)) {
                        nFaceEdges++;
                    }
                }
            }
        }

        // back and front faces, vertical edges
        for (int y = 0; y < height; y += yInc) {
            for (int z = 0; z < depth; z++) {
                for (int x = 0; x <= width; x++) {
                    if (isNeighborhoodForeground(x, y, z, Orientation1D.X)) {
                        nFaceEdges++;
                    }
                }
            }
        }

        // left and right stack faces, horizontal edges
        for (int x = 0; x < width; x += xInc) {
            for (int z = 1; z < depth; z++) {
                for (int y = 0; y < height; y++) {
                    if (isNeighborhoodForeground(x, y, z, Orientation1D.Z)) {
                        nFaceEdges++;
                    }
                }
            }
        }

        // left and right stack faces, vertical voxel edges
        for (int x = 0; x < width; x += xInc) {
            for (int z = 0; z < depth; z++) {
                for (int y = 1; y < height; y++) {
                    if (isNeighborhoodForeground(x, y, z, Orientation1D.Y)) {
                        nFaceEdges++;
                    }
                }
            }
        }
        
        return nFaceEdges;
    }

    private double getFaceVertices() {
        int xInc = Math.max(1, width - 1);
        int yInc = Math.max(1, height - 1);
        int zInc = Math.max(1, depth - 1);
        long nFaceVertices = 0;

        // top and bottom faces (all 4 edges)
        for (int z = 0; z < depth; z += zInc) {
            for (int y = 0; y <= height; y++) {
                for (int x = 0; x <= width; x++) {
                    if (isNeighborhoodForeground(x, y, z, Orientation2D.XY)) {
                        nFaceVertices++;
                    }
                }
            }
        }

        // left and right faces (2 vertical edges)
        for (int x = 0; x < width; x += xInc) {
            for (int y = 0; y <= height; y++) {
                for (int z = 1; z < depth; z++) {
                    if (isNeighborhoodForeground(x, y, z, Orientation2D.YZ)) {
                        nFaceVertices++;
                    }
                }
            }
        }

        // back and front faces (0 vertical edges)
        for (int y = 0; y < height; y += yInc) {
            for (int x = 1; x < width; x++) {
                for (int z = 1; z < depth; z++) {
                    if (isNeighborhoodForeground(x, y, z, Orientation2D.XZ)) {
                        nFaceVertices++;
                    }
                }
            }
        }

        return nFaceVertices;
    }

    private double getEdgeVertices() {
        int xInc = Math.max(1, width - 1);
        int yInc = Math.max(1, height - 1);
        int zInc = Math.max(1, depth - 1);
        long nEdgeVertices = 0;

        // vertex voxels contribute 1 edge vertex each
        // this could be taken out into a variable to avoid recalculating it
        // nEdgeVertices += getStackVertices(stack);

        // left->right edges
        for (int z = 0; z < depth; z += zInc) {
            for (int y = 0; y < height; y += yInc) {
                for (int x = 1; x < width; x++) {
                    if (isNeighborhoodForeground(x, y, z, Orientation1D.X)) {
                        nEdgeVertices++;
                    }
                }
            }
        }

        // back->front edges
        for (int z = 0; z < depth; z += zInc) {
            for (int x = 0; x < width; x += xInc) {
                for (int y = 1; y < height; y++) {
                    if (isNeighborhoodForeground(x, y, z, Orientation1D.Y)) {
                        nEdgeVertices++;
                    }
                }
            }
        }

        // top->bottom edges
        for (int y = 0; y < height; y += yInc) {
            for (int x = 0; x < width; x += xInc) {
                for (int z = 1; z < depth; z++) {
                    if (isNeighborhoodForeground(x, y, z, Orientation1D.Z)) {
                        nEdgeVertices++;
                    }
                }
            }
        }

        return nEdgeVertices;
    }

    private double getStackFaces() {
        int xInc = Math.max(1, width - 1);
        int yInc = Math.max(1, height - 1);
        int zInc = Math.max(1, depth - 1);
        long nStackFaces = 0;

        // vertex voxels contribute 3 faces
        // this could be taken out into a variable to avoid recalculating it
        // nStackFaces += getStackVertices(stack) * 3;

        // edge voxels contribute 2 faces
        // this could be taken out into a variable to avoid recalculating it
        // nStackFaces += getStackEdges(stack) * 2;

        // top and bottom faces
        for (int z = 0; z < depth; z += zInc) {
            for (int y = 1; y < height - 1; y++) {
                for (int x = 1; x < width - 1; x++) {
                    if (getPixel(x, y, z) == FOREGROUND) {
                        nStackFaces++;
                    }
                }
            }
        }

        // back and front faces
        for (int y = 0; y < height; y += yInc) {
            for (int z = 1; z < depth - 1; z++) {
                for (int x = 1; x < width - 1; x++) {
                    if (getPixel(x, y, z) == FOREGROUND) {
                        nStackFaces++;
                    }
                }
            }
        }

        // left and right faces
        for (int x = 0; x < width; x += xInc) {
            for (int y = 1; y < height - 1; y++) {
                for (int z = 1; z < depth - 1; z++) {
                    if (getPixel(x, y, z) == FOREGROUND) {
                        nStackFaces++;
                    }
                }
            }
        }
        return nStackFaces;
    }

    private double getStackEdges() {
        int xInc = Math.max(1, width - 1);
        int yInc = Math.max(1, height - 1);
        int zInc = Math.max(1, depth - 1);
        long nStackEdges = 0;

        // vertex voxels contribute 3 edges
        // this could be taken out into a variable to avoid recalculating it
        // nStackEdges += getStackVertices(stack) * 3; = f * 3;

        // left to right stack edges
        for (int z = 0; z < depth; z += zInc) {
            for (int y = 0; y < height; y += yInc) {
                for (int x = 1; x < width - 1; x++) {
                    if (getPixel(x, y, z) == FOREGROUND) {
                        nStackEdges++;
                    }
                }
            }
        }

        // back to front stack edges
        for (int z = 0; z < depth; z += zInc) {
            for (int x = 0; x < width; x += xInc) {
                for (int y = 1; y < height - 1; y++) {
                    if (getPixel(x, y, z) == FOREGROUND) {
                        nStackEdges++;
                    }
                }
            }
        }

        // top to bottom stack edges
        for (int y = 0; y < height; y += yInc) {
            for (int x = 0; x < width; x += xInc) {
                for (int z = 1; z < depth - 1; z++) {
                    if (getPixel(x, y, z) == FOREGROUND) {
                        nStackEdges++;
                    }
                }
            }
        }

        return nStackEdges;
    }

    private double getStackVertices() {
        int xInc = Math.max(1, width - 1);
        int yInc = Math.max(1, height - 1);
        int zInc = Math.max(1, depth - 1);
        long nStackVertices = 0;
        
        for (int z = 0; z < depth; z += zInc) {
            for (int y = 0; y < height; y += yInc) {
                for (int x = 0; x < width; x += xInc) {
                    if (getPixel(x, y, z) == FOREGROUND) {
                        nStackVertices++;
                    }
                }
            }
        }

        return nStackVertices;
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

    private enum Orientation1D {
        X,
        Y,
        Z
    }

    private enum Orientation2D {
        XY,
        XZ,
        YZ
    }
    //endregion
}