package org.bonej.protoPlugins;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import net.imagej.ImageJ;
import org.bonej.analyseSkeleton.AnalyzeSkeleton_;
import org.bonej.common.Common;
import org.bonej.common.ImageCheck;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.prefs.PrefService;
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

    private static final int DEFAULT_PRUNE_MODE_INDEX = AnalyzeSkeleton_.NONE;
    private static final boolean DEFAULT_PRUNE_ENDS = false;
    private static final boolean DEFAULT_SHORTEST_PATH = false;
    private static final boolean DEFAULT_SHOW_DETAILED = false;
    private static final boolean DEFAULT_EXCLUDE_ROI = false;
    private static final boolean DEFAULT_DISPLAY_LABELED = false;
    private static final String HELP_URL = "http://fiji.sc/wiki/index.php/AnalyzeSkeleton";

    private static final String PRUNE_MODE_INDEX_KEY = "bonej.analyzeSkeleton.pruneModeIndex";
    private static final String PRUNE_ENDS_KEY = "bonej.analyzeSkeleton.pruneEnds";
    private static final String CALCULATE_PATH_KEY = "bonej.analyzeSkeleton.shortestPath";
    private static final String SHOW_DETAILED_KEY = "bonej.analyzeSkeleton.showDetailedInfo";
    private static final String EXCLUDE_ROI_KEY = "bonej.analyzeSkeleton.excludeRoi";
    private static final String DISPLAY_LABELED_KEY = "bonej.analyzeSkeleton.displayLabeledSkeletons";

    private ImagePlus inputImage = null;


    // The following service parameters are populated automatically
    // by the SciJava service framework before this command plugin is executed.
    @Parameter
    private UIService uiService;

    @Parameter
    private PrefService prefService;

    private GenericDialog settingsDialog;
    private int pruneModeIndex = DEFAULT_PRUNE_MODE_INDEX;
    private boolean pruneEnds = DEFAULT_PRUNE_ENDS;
    private boolean calculatePath = DEFAULT_SHORTEST_PATH;
    private boolean showDetailedInfo = DEFAULT_SHOW_DETAILED;
    private boolean excludeRoi = DEFAULT_EXCLUDE_ROI;
    private boolean displayLabeledSkeletons = DEFAULT_DISPLAY_LABELED;

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
        saveSettingsToPreferences();

        skeletonAnalyzer.setup("", inputImage);
        skeletonAnalyzer.run(pruneModeIndex, pruneEnds, calculatePath, null, false, false, null);
    }

    private void saveSettingsToPreferences()
    {
        prefService.put(PRUNE_MODE_INDEX_KEY, pruneModeIndex);
        prefService.put(PRUNE_ENDS_KEY, pruneEnds);
        prefService.put(CALCULATE_PATH_KEY, calculatePath);
        prefService.put(SHOW_DETAILED_KEY, showDetailedInfo);
        prefService.put(EXCLUDE_ROI_KEY, excludeRoi);
        prefService.put(DISPLAY_LABELED_KEY, displayLabeledSkeletons);
    }

    private void loadSettingsFromPreferences()
    {
        pruneModeIndex = prefService.getInt(PRUNE_MODE_INDEX_KEY, DEFAULT_PRUNE_MODE_INDEX);
        pruneEnds = prefService.getBoolean(PRUNE_ENDS_KEY, DEFAULT_PRUNE_ENDS);
        calculatePath = prefService.getBoolean(CALCULATE_PATH_KEY, DEFAULT_SHORTEST_PATH);
        showDetailedInfo = prefService.getBoolean(SHOW_DETAILED_KEY, DEFAULT_SHOW_DETAILED);
        excludeRoi = prefService.getBoolean(EXCLUDE_ROI_KEY, DEFAULT_EXCLUDE_ROI);
        displayLabeledSkeletons = prefService.getBoolean(DISPLAY_LABELED_KEY, DEFAULT_SHOW_DETAILED);
    }

    private void getSettingsFromDialog()
    {
        pruneModeIndex = settingsDialog.getNextChoiceIndex();
        pruneEnds = settingsDialog.getNextBoolean();
        calculatePath = settingsDialog.getNextBoolean();
        showDetailedInfo = settingsDialog.getNextBoolean();
        excludeRoi = settingsDialog.getNextBoolean();
        displayLabeledSkeletons = settingsDialog.getNextBoolean();
    }

    private GenericDialog createSettingsDialog()
    {
        GenericDialog dialog = new GenericDialog("Analyse Skeleton");
        dialog.addChoice("Prune cycle method: ", AnalyzeSkeleton_.pruneCyclesModes,
                AnalyzeSkeleton_.pruneCyclesModes[pruneModeIndex]);
        dialog.addCheckbox("Prune ends", pruneEnds);
        dialog.addCheckbox("Calculate largest shortest path", calculatePath);
        dialog.addCheckbox("Show detailed info", showDetailedInfo);
        dialog.addCheckbox("Exclude ROI from pruning", excludeRoi);
        dialog.addCheckbox("Display labeled skeletons", displayLabeledSkeletons);
        dialog.addHelp(HELP_URL);
        return dialog;
    }

    public static void main(final String... args)
    {
        final ImageJ ij = net.imagej.Main.launch(args);
    }
}
