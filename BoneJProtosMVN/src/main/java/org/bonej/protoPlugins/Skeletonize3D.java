package org.bonej.protoPlugins;

import Skeletonize3D_.Skeletonize3D_;
import ij.IJ;
import ij.ImagePlus;
import net.imagej.ImageJ;
import org.bonej.common.Common;
import org.bonej.common.ImageCheck;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

/**
 * A BoneJ wrapper plugin, which is used for a "bone science" flavour of the Skeletonize3D ImageJ plugin.
 *
 * @author <a href="mailto:rdomander@rvc.ac.uk">Richard Domander</a>
 */
@Plugin(type = Command.class, menuPath = "Plugins>BoneJ>Skeletonize3D")
public class Skeletonize3D implements Command
{
    private static final Skeletonize3D_ skeletonizer = new Skeletonize3D_();

    private ImagePlus inputImage = null;

    // The following service parameters are populated automatically
    // by the SciJava service framework before this command plugin is executed.
    @Parameter
    private UIService uiService;

    private boolean setInputImage()
    {
        try {
            inputImage = IJ.getImage();
        } catch (RuntimeException rte) {
            // no image currently open
            return false;
        }

        if (!ImageCheck.isBinary(inputImage))
        {
            uiService.showDialog(Common.NOT_BINARY_IMAGE_ERROR, Common.WRONG_IMAGE_TYPE_DIALOG_TITLE);
            return false;
        }

        return true;
    }

    @Override
    public void run()
    {
        if (!ImageCheck.isBoneJEnvironmentValid()) {
            return;
        }

        if (!setInputImage()) {
            return;
        }

        ImagePlus outputImage = inputImage.duplicate(); //Skeletonize3D_.run() overwrites input image
        skeletonizer.setup("", outputImage);
        skeletonizer.run(null);
        outputImage.setTitle("Skeleton of " + inputImage.getTitle());
        outputImage.show();
    }

    public static void main(final String... args)
    {
        net.imagej.Main.launch(args);
    }
}
