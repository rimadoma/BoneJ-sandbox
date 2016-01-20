package protoOps.connectivity;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import net.imagej.ops.Op;
import net.imagej.ops.OpEnvironment;

import org.bonej.common.ImageCheck;
import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import ij.ImagePlus;

/**
 * @author Michael Doube
 * @author Richard Domander
 */
@Plugin(type = Op.class, name = "eulerConnectivity")
public class Connectivity implements Op {
    private int width = 0;
    private int height = 0;
    private int depth = 0;

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

    }
    //endregion
}
