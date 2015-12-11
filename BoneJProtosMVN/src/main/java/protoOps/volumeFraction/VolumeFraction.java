package protoOps.volumeFraction;

import customnode.CustomTriangleMesh;
import ij.ImagePlus;
import ij.plugin.frame.RoiManager;
import net.imagej.ops.Op;
import net.imagej.ops.OpEnvironment;
import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Michael Doube
 * @author Richard Domander
 *
 * @todo RoiManager input @Parameter
 */
@Plugin(type=Op.class, name = "volumeFraction")
public class VolumeFraction implements Op
{
    public static final int VOXEL_ALGORITHM = 0;
    public static final int SURFACE_ALGORITHM = 1;
    public static final int DEFAULT_VOLUME_ALGORITHM = VOXEL_ALGORITHM;
    public static final int DEFAULT_SURFACE_RESAMPLING = 6;

    @Parameter(type = ItemIO.INPUT)
    private ImagePlus inputImage = null;

    @Parameter(type = ItemIO.INPUT, required = false, min = "0", max = "1")
    private int volumeAlgorithm = DEFAULT_VOLUME_ALGORITHM;

    @Parameter(type = ItemIO.INPUT, required = false, min = "0")
    private int surfaceResampling = DEFAULT_SURFACE_RESAMPLING;

    @Parameter(type = ItemIO.INPUT, required = false)
    private RoiManager roiManager = null;

    @Parameter(type = ItemIO.OUTPUT)
    private double foregroundVolume = 0.0;

    @Parameter(type = ItemIO.OUTPUT)
    private double totalVolume = 0.0;

    @Parameter(type = ItemIO.OUTPUT)
    private double volumeRatio = Double.NaN;

    @Parameter(type = ItemIO.OUTPUT)
    CustomTriangleMesh foregroundSurface = null;

    @Parameter(type = ItemIO.OUTPUT)
    CustomTriangleMesh totalSurface = null;


    public void setImage(ImagePlus image) {
        checkImage(image);

        inputImage = image;
    }

    public void setSurfaceResampling(int resampling) {
        checkArgument(resampling >= 0, "Resampling value must be >= 0");

        surfaceResampling = resampling;
    }

    public void setVolumeAlgorithm(int algorithm) {
        checkArgument(algorithm == VOXEL_ALGORITHM || algorithm == SURFACE_ALGORITHM, "No such surface algorithm");

        volumeAlgorithm = algorithm;
    }

    public void setRoiManager(RoiManager roiManager) {
        checkNotNull(roiManager, "May not use a null ROI Manager");
        checkArgument(roiManager.getCount() != 0, "May not use an empty ROI Manager");

        this.roiManager = roiManager;
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
    }

    //region -- Helper methods --
    private static void checkImage(ImagePlus image) {
        checkNotNull(image, "Must have an input image");

        //@todo accept gray scale and binary images
        int bitDepth = image.getBitDepth();
        checkArgument(bitDepth == 8 || bitDepth == 16, "Input image bit depth must be 8 or 16");
    }
    //endregion
}
