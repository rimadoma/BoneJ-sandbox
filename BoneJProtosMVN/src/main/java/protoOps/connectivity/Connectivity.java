package protoOps.connectivity;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Arrays;
import java.util.stream.IntStream;

import net.imagej.ops.Op;
import net.imagej.ops.OpEnvironment;

import org.bonej.common.ImageCheck;
import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;

/**
 * An Op which determines the number of connected structures in the given image by calculating the Euler characteristic.
 * The algorithm uses voxel neighbourhoods to calculate the Euler characteristic.
 * An assumption is made that there is only one continuous foreground structure in the image.
 * Foreground voxels are assumed to have the value Connectivity#FOREGROUND
 *
 * How to run programmatically:
 * You can call the plugin via an opService, or you can initialize an object and then:
 * 1) call setInputImage(ImagePlus image)
 * 2) call run()
 *
 * @todo Handle special cases where the stack only has two dimensions (XY / XZ / YZ)
 * @todo Rewrite to use Datasets either directly or by unwrapping them as ImagePlus
 * @author Michael Doube
 * @author Richard Domander
 */
@Plugin(type = Op.class, name = "eulerConnectivity")
public class Connectivity implements Op {
    private static final int EULER_LUT[] = new int[256];
    private static final int FOREGROUND = -1;

    //region LUT init
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
    //endregion

    private int width = 0;
    private int height = 0;
    private int depth = 0;
    private ImageStack inputStack = null;

    @Parameter(type = ItemIO.INPUT)
    private ImagePlus inputImage = null;

    /** Euler characteristic of the sample as though floating in space (χ). */
    @Parameter(type = ItemIO.OUTPUT)
    private double eulerCharacteristic = 0.0;

    /** Δ(χ): the sample's contribution to the Euler characteristic of the structure to which it was connected.
     *  Calculated by counting the intersections of voxels and the edges of the image stack. */
    @Parameter(type = ItemIO.OUTPUT)
    private double deltaChi = 0.0;

    /** The connectivity of the image = 1 - Δ(χ) */
    @Parameter(type = ItemIO.OUTPUT)
    private double connectivity = 0.0;

    /** The connectivity density of the image = connectivity / sample volume */
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

    /**
     * Sets the input image for processing
     *
     * @throws NullPointerException if image == null
     * @throws IllegalArgumentException if image is not binary
     */
    public void setInputImage(final ImagePlus image) throws NullPointerException, IllegalArgumentException {
        checkImage(image);

        inputImage = image;
        inputStack = image.getStack();
        width = image.getWidth();
        height = image.getHeight();
        depth = image.getNSlices();
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

    //region -- Helper methods --
    /**
     * @todo Check that there's only one object with 3D_Objects_Counter?
     */
    private static void checkImage(final ImagePlus imagePlus) {
        checkNotNull(imagePlus, "Must have an input image");
        checkArgument(ImageCheck.isBinary(imagePlus), "Input image must be binary");
    }

    private void calculateConnectivityDensity() {
        double stackVolume = width * height * depth;

        Calibration calibration = inputImage.getCalibration();
        double pixelVolume = calibration.pixelWidth * calibration.pixelHeight * calibration.pixelDepth;

        double sampleVolume = stackVolume * pixelVolume;

        connectivityDensity = connectivity / sampleVolume;
    }

    private void calculateConnectivity() {
        connectivity = 1.0 - deltaChi;
    }

    private void calculateDeltaChi() {
        deltaChi = eulerCharacteristic - getEdgeCorrection();
    }

    private void calculateEulerCharacteristic() {
        // The array sumEulerInt is needed to calculate eulerCharacteristic concurrently
        final int[] sumEulerInt = new int[depth + 1];

        IntStream.rangeClosed(0, depth).parallel().forEach(z -> {
            for (int y = 0; y <= height; y++) {
                for (int x = 0; x <= width; x++) {
                    final byte[] octant = getOctant(x, y, z);
                    sumEulerInt[z] += getDeltaEuler(octant);
                }
            }
        });

        eulerCharacteristic = Arrays.stream(sumEulerInt).parallel().sum();
        eulerCharacteristic /= 8.0;
    }

    private static boolean isOctantEmpty(final byte[] octant) {
        return octant[0] == 0;
    }
    
    /**
     * Get octant of a vertex at (0,0,0) of a voxel at (x,y,z) in the input image
     *
     * @param x The x-coordinate of the voxel
     * @param y The x-coordinate of the voxel
     * @param z The x-coordinate of the voxel
     * @return  A nine element array which contains the voxel values at indices 1 - 8,
     *          and the number of foreground voxels in the neighborhood at index 0
     */
    private byte[] getOctant(final int x, final int y, final int z) {
        // index 0 is a counter to determine if octant is empty
        // index 8 is (x,y,z)
        final byte[] octant = new byte[9];

        octant[1] = getPixel(x - 1, y - 1, z - 1);
        octant[2] = getPixel(x - 1, y, z - 1);
        octant[3] = getPixel(x, y - 1, z - 1);
        octant[4] = getPixel(x, y, z - 1);
        octant[5] = getPixel(x - 1, y - 1, z);
        octant[6] = getPixel(x - 1, y, z);
        octant[7] = getPixel(x, y - 1, z);
        octant[8] = getPixel(x, y, z);

        octant[0] = countNeighbors(octant);

        return octant;
    }

    private static byte countNeighbors(final byte[] octant) {
        byte neighbors = 0;

        for (int n = 1; n < octant.length; n++) {
            neighbors -= octant[n]; // foreground is -1
        }

        return neighbors;
    }

    /**
     * Get pixel form the 3D image stack
     *
     * @param x The x-coordinate of the pixel
     * @param y The y-coordinate of the pixel
     * @param z The z-coordinate of the pixel
     * @return  The value of the pixel at (x, y, z),
     *          or 0 if (x, y, z) is out of bounds
     *
     */
    private byte getPixel(final int x, final int y, final int z) {
        if ((x < 0) || (x >= width) || (y < 0) || (y >= height) || (z < 0) || (z >= depth)) {
            return 0;
        }

        byte pixels[] = (byte[]) inputStack.getPixels(z + 1);
        return pixels[y * width + x];
    }

    /**
     * Get delta euler value for an octant (~= vertex) from look up table
     *
     * @param octant
     *            9 element array containing nVoxels in zeroth element and 8
     *            voxel values
     * @return Delta euler value from the LUT, or 0 if octant is empty
     */
    private static int getDeltaEuler(final byte[] octant) {
        if (isOctantEmpty(octant)) {
            return 0;
        }

        char index = 1;
        // have to rotate octant voxels around vertex so that
        // octant[8] is foreground as eulerLUT assumes that voxel in position
        // 8 is always foreground. Only have to check each voxel once.
        if (octant[8] == FOREGROUND) {
            if (octant[1] == FOREGROUND)
                index |= 128;
            if (octant[2] == FOREGROUND)
                index |= 64;
            if (octant[3] == FOREGROUND)
                index |= 32;
            if (octant[4] == FOREGROUND)
                index |= 16;
            if (octant[5] == FOREGROUND)
                index |= 8;
            if (octant[6] == FOREGROUND)
                index |= 4;
            if (octant[7] == FOREGROUND)
                index |= 2;
        } else if (octant[7] == -1) {
            if (octant[2] == FOREGROUND)
                index |= 128;
            if (octant[4] == FOREGROUND)
                index |= 64;
            if (octant[1] == FOREGROUND)
                index |= 32;
            if (octant[3] == FOREGROUND)
                index |= 16;
            if (octant[6] == FOREGROUND)
                index |= 8;
            if (octant[5] == FOREGROUND)
                index |= 2;
        } else if (octant[6] == FOREGROUND) {
            if (octant[3] == FOREGROUND)
                index |= 128;
            if (octant[1] == FOREGROUND)
                index |= 64;
            if (octant[4] == FOREGROUND)
                index |= 32;
            if (octant[2] == FOREGROUND)
                index |= 16;
            if (octant[5] == FOREGROUND)
                index |= 4;
        } else if (octant[5] == FOREGROUND) {
            if (octant[4] == FOREGROUND)
                index |= 128;
            if (octant[3] == FOREGROUND)
                index |= 64;
            if (octant[2] == FOREGROUND)
                index |= 32;
            if (octant[1] == FOREGROUND)
                index |= 16;
        } else if (octant[4] == FOREGROUND) {
            if (octant[1] == FOREGROUND)
                index |= 8;
            if (octant[3] == FOREGROUND)
                index |= 4;
            if (octant[2] == FOREGROUND)
                index |= 2;
        } else if (octant[3] == FOREGROUND) {
            if (octant[2] == FOREGROUND)
                index |= 8;
            if (octant[1] == FOREGROUND)
                index |= 4;
        } else if (octant[2] == FOREGROUND && octant[1] == FOREGROUND) {
                index |= 2;
        }

        return EULER_LUT[index];
    }

    private boolean isNeighborhoodForeground(final int x, final int y, final int z, final Orientation1D orientation) {
        switch (orientation) {
            case X:
                return getPixel(x, y, z) == FOREGROUND || getPixel(x - 1, y, z) == FOREGROUND;
            case Y:
                return getPixel(x, y, z) == FOREGROUND || getPixel(x, y - 1, z) == FOREGROUND;
            case Z:
                return getPixel(x, y, z) == FOREGROUND || getPixel(x, y, z - 1) == FOREGROUND;
            default:
                return false;
        }
    }

    private boolean isNeighborhoodForeground(final int x, final int y, final int z, final Orientation2D orientation)
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

    /**
     * Check all vertices of stack and count if foreground (-1) this is &#967;
     * <sub>0</sub> from Odgaard and Gundersen (1993) and <i>f</i> in my working
     *
     * @return number of voxel vertices intersecting with stack vertices
     */
    private long getStackVertices() {
        long nStackVertices = 0;
        int xInc = Math.max(1, width - 1);
        int yInc = Math.max(1, height - 1);
        int zInc = Math.max(1, depth - 1);

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

    /**
     * Count the number of foreground voxels on edges of stack, this is part of
     * &#967;<sub>1</sub> (<i>e</i> in my working)
     *
     * @return number of voxel edges intersecting with stack edges
     */
    private long getStackEdges() {
        long nStackEdges = 0;
        int xInc = Math.max(1, width - 1);
        int yInc = Math.max(1, height - 1);
        int zInc = Math.max(1, depth - 1);

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

    /**
     * Count the number of foreground voxel faces intersecting with stack faces
     * This is part of &#967;<sub>2</sub> and is <i>c</i> in my working
     *
     * @return number of voxel faces intersecting with stack faces
     */
    private long getStackFaces() {
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

    /**
     * Count the number of voxel vertices intersecting stack faces. This
     * contributes to &#967;<sub>2</sub> (<i>a</i> in my working)
     *
     * @return Number of voxel vertices intersecting stack faces
     */
    private long getFaceVertices() {
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

    /**
     * Count the number of intersections between voxel edges and stack faces.
     * This is part of &#967;<sub>2</sub>, in my working it's called <i>b</i>
     *
     * @return number of intersections between voxel edges and stack faces
     */
    private long getFaceEdges() {
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
    
    /**
     * Count number of voxel vertices intersecting stack edges. It contributes
     * to &#967;<sub>1</sub>, and I call it <i>d</i> in my working
     *
     * @return number of voxel vertices intersecting stack edges
     */
    private long getEdgeVertices() {
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
    
    /**
     * <p>
     * Calculate a correction value to convert the Euler number of a stack to
     * the stack's contribution to the Euler number of whatever it is cut from.
     * <ol type="a">
     * <li>Number of voxel vertices on stack faces</li>
     * <li>Number of voxel edges on stack faces</li>
     * <li>Number of voxel faces on stack faces</li>
     * <li>Number of voxel vertices on stack edges</li>
     * <li>Number of voxel edges on stack edges</li>
     * <li>Number of voxel vertices on stack vertices</li>
     * </ol>
     * </p>
     * <p>
     * Subtract the returned value from the Euler number prior to calculation of
     * connectivity
     * </p>
     *
     * @return edgeCorrection for subtraction from the stack's Euler number
     */
    private double getEdgeCorrection() {
        final long chiZero = getStackVertices();
        final long e = getStackEdges() + 3 * chiZero;
        final long c = getStackFaces() + 2 * e - 3 * chiZero; 
        
        // there are already 6 * chiZero in 2 * e, so remove 3 * chiZero

        final long d = getEdgeVertices() + chiZero;
        final long a = getFaceVertices();
        final long b = getFaceEdges();
        
        final double chiOne = d - e;
        final double chiTwo = a - b + c;

        return chiTwo / 2.0 + chiOne / 4.0 + chiZero / 8.0;
    }
    //endregion

    //region -- Helper classes --
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