package protoOps.imageCheck;

import net.imagej.Dataset;
import net.imagej.ops.AbstractNamespace;
import net.imagej.ops.Op;
import net.imagej.ops.OpMethod;

/**
 * An exercise in creating an op namespace and op methods
 *
 * @author  Richard Domander
 */

public class ImagePlusCheck extends AbstractNamespace {
    @Override
    public String getName() {
        return "imageCheck";
    }

    public interface ImagePlusCheckOp extends Op {
        String NAME = "ImagePlusCheck.ImagePlusCheckOp";
    }

    //@todo Find out how DefaultImagePlusCheck can be made into an opmethod...
    @OpMethod(op = ImagePlusCheck.ImagePlusCheckOp.class)
    public boolean imagePlusCheck(final Dataset dataset) {
        return (boolean) ops().run(ImagePlusCheck.ImagePlusCheckOp.class, dataset);
    }
}
