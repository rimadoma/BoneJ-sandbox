package org.bonej.wrapperPlugins;

import ij.IJ;
import ij.ImagePlus;
import org.bonej.common.Common;
import org.bonej.common.ImageCheck;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.DialogPrompt;
import org.scijava.ui.UIService;
import sc.fiji.skeletonize3D.Skeletonize3D_;

/**
 * A BoneJ wrapper plugin, which is used for a "bone science" flavour of the Skeletonize3D ImageJ plugin.
 *
 * @author >Richard Domander
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
        try {
            ImageCheck.checkIJVersion();
        } catch (RuntimeException e) {
            uiService.showDialog(e.getMessage(), DialogPrompt.MessageType.ERROR_MESSAGE);
        }

        if (!setInputImage()) {
            return;
        }

        ImagePlus outputImage = inputImage.duplicate(); //Skeletonize3D_.run() overwrites input image
        skeletonizer.setup("", outputImage);
        skeletonizer.run(null);
        outputImage.setTitle("Skeleton of " + inputImage.getTitle());

        if (inputImage.isInvertedLut() != outputImage.isInvertedLut()) {
            //Invert the LUT of the output image to match input image
            IJ.run("Invert LUT");
        }

        outputImage.show();
    }

    public static void main(final String... args)
    {
        net.imagej.Main.launch(args);
    }
}
