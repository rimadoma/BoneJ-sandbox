package org.bonej.wrapperPlugins;

import ij.IJ;
import ij.ImagePlus;
import net.imagej.ImageJ;
import org.bonej.analyseSkeleton.AnalyzeSkeleton_;
import org.bonej.common.Common;
import org.bonej.common.ImageCheck;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

/**
 * A BoneJ wrapper plugin, which is used for a "bone science" flavour of the AnalyzeSkeleton ImageJ plugin.
 *
 * @author <a href="mailto:rdomander@rvc.ac.uk">Richard Domander</a>
 */

@Plugin(type = Command.class, menuPath = "Plugins>BoneJ>AnalyzeSkeleton")
public class AnalyzeSkeleton implements Command
{
    private static final AnalyzeSkeleton_ skeletonAnalyzer = new AnalyzeSkeleton_();
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
        if (!setInputImage()) {
            return;
        }

        skeletonAnalyzer.setup("", inputImage);
        skeletonAnalyzer.run(null);
    }

    public static void main(final String... args)
    {
        final ImageJ ij = net.imagej.Main.launch(args);
    }
}
