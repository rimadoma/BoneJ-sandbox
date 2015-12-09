package protoOps.volumeFraction;

import ij.ImagePlus;
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
    private static final int DEFAULT_SURFACE_RESAMPLING = 6;
    private static final boolean DEFAULT_SHOW_3D_RESULT = false;

    @Parameter(type = ItemIO.INPUT)
    private ImagePlus inputImage = null;

    @Parameter(type = ItemIO.INPUT, required = false, min = "0", max = "1")
    private int volumeAlgorithm = DEFAULT_VOLUME_ALGORITHM;

    @Parameter(type = ItemIO.INPUT, required = false, min = "0")
    private int surfaceResampling = DEFAULT_SURFACE_RESAMPLING;

    @Parameter(type = ItemIO.INPUT, required = false)
    private boolean show3DResult = DEFAULT_SHOW_3D_RESULT;

    @Parameter(type = ItemIO.OUTPUT)
    private double foregroundVolume;

    @Parameter(type = ItemIO.OUTPUT)
    private double backgroundVolume;

    @Parameter(type = ItemIO.OUTPUT)
    private double volumeRatio;

    public void setImage(ImagePlus image) {
        checkImage(image);

        inputImage = image;
    }

    public void setShow3DResult(boolean show3DResult) {
        this.show3DResult = show3DResult;
    }

    public void setSurfaceResampling(int resampling) {
        checkArgument(resampling >= 0, "Resampling value must be >= 0");

        surfaceResampling = resampling;
    }

    public void setVolumeAlgorithm(int algorithm) {
        checkArgument(algorithm == VOXEL_ALGORITHM || algorithm == SURFACE_ALGORITHM, "Invalid algorithm option");

        volumeAlgorithm = algorithm;
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

        int bitDepth = image.getBitDepth();
        checkArgument(bitDepth == 8 || bitDepth == 16, "Input image bit depth must be 8 or 16");
    }
    //endregion
}
