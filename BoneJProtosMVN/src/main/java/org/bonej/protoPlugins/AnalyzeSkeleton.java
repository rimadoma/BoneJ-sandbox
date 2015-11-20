package org.bonej.protoPlugins;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import net.imagej.ImageJ;
import org.bonej.common.Common;
import org.bonej.common.ImageCheck;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.prefs.PrefService;
import org.scijava.ui.UIService;
import skeleton_analysis.AnalyzeSkeleton_;

/**
 * A BoneJ wrapper plugin, which is used for a "bone science" flavour of the AnalyzeSkeleton ImageJ plugin.
 *
 * @author <a href="mailto:rdomander@rvc.ac.uk">Richard Domander</a>
 */

@Plugin(type = Command.class, menuPath = "Plugins>BoneJ>AnalyzeSkeleton")
public class AnalyzeSkeleton implements Command
{
    private static final AnalyzeSkeleton_ skeletonAnalyzer = new AnalyzeSkeleton_();

    private static final int DEFAULT_PRUNE_INDEX = AnalyzeSkeleton_.NONE;
    private static final boolean DEFAULT_PRUNE_ENDS = false;
    private static final boolean DEFAULT_SHORTEST_PATH = false;
    private static final boolean DEFAULT_SHOW_DETAILED = false;

    private static final String HELP_URL = "http://fiji.sc/wiki/index.php/AnalyzeSkeleton";

    private ImagePlus inputImage = null;

    // The following service parameters are populated automatically
    // by the SciJava service framework before this command plugin is executed.
    @Parameter
    private UIService uiService;

    @Parameter
    private PrefService prefService;

    private GenericDialog settingsDialog;

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

        loadSettingsFromPreferences();
        settingsDialog = createSettingsDialog();
        settingsDialog.showDialog();
        getSettingsFromDialog();
        if (settingsDialog.wasCanceled()) {
            return;
        }
        saveSettingsToDialog();
    }

    private void saveSettingsToDialog()
    {

    }

    private void loadSettingsFromPreferences()
    {

    }

    private void getSettingsFromDialog()
    {

    }

    private GenericDialog createSettingsDialog()
    {
        GenericDialog dialog = new GenericDialog("Analyse Skeleton");
        dialog.addChoice("Prune cycle method: ", AnalyzeSkeleton_.pruneCyclesModes,
                AnalyzeSkeleton_.pruneCyclesModes[DEFAULT_PRUNE_INDEX]);
        dialog.addCheckbox("Prune ends", DEFAULT_PRUNE_ENDS);
        dialog.addCheckbox("Calculate largest shortest path", DEFAULT_SHORTEST_PATH);
        dialog.addCheckbox("Show detailed info", DEFAULT_SHOW_DETAILED);
        dialog.addHelp(HELP_URL);
        return dialog;
    }

    public static void main(final String... args)
    {
        final ImageJ ij = net.imagej.Main.launch(args);
    }
}
