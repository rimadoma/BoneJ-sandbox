package org.bonej.wrapperPlugins;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;
import ij.macro.Interpreter;
import ij.plugin.frame.RoiManager;
import ij.process.StackStatistics;
import net.imagej.patcher.LegacyInjector;
import org.bonej.common.Common;
import org.bonej.common.ImageCheck;
import org.bonej.common.ResultsInserter;
import org.bonej.common.RoiUtil;
import org.bonej.localThickness.LocalThicknessWrapper;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.prefs.PrefService;
import org.scijava.ui.UIService;

import static org.scijava.ui.DialogPrompt.*;

/**
 * A BoneJ wrapper plugin, which is used for a "bone science" flavour of the LocalThickness ImageJ plugin.
 *
 * @author <a href="mailto:rdomander@rvc.ac.uk">Richard Domander</a>
 */
@Plugin(type = Command.class, menuPath = "Plugins>BoneJ>Thickness")
public class Thickness implements Command
{
    // Need this because we're using ImageJ 1.x classes
    static {
        LegacyInjector.preinit();
    }

    private static final String HELP_URL = "http://bonej.org/thickness"; // @todo Read from a file?
    private static final String TRABECULAR_THICKNESS = "Tb.Th";
    private static final String TRABECULAR_SPACING = "Tb.Sp";

    private static final String THICKNESS_PREFERENCE_KEY = "bonej.localThickness.doThickness";
    private static final String SPACING_PREFERENCE_KEY = "bonej.localThickness.doSpacing";
    private static final String GRAPHIC_PREFERENCE_KEY = "bonej.localThickness.doGraphic";
    private static final String ROI_PREFERENCE_KEY = "bonej.localThickness.doRoi";
    private static final String MASK_PREFERENCE_KEY = "bonej.localThickness.doMask";

    private static final boolean THICKNESS_DEFAULT = true;
    private static final boolean SPACING_DEFAULT = false;
    private static final boolean GRAPHIC_DEFAULT = true;
    private static final boolean ROI_DEFAULT = false;
    private static final boolean MASK_DEFAULT = true;

    private final LocalThicknessWrapper thicknessWrapper = new LocalThicknessWrapper();

    // The following service parameters are populated automatically
    // by the SciJava service framework before this command plugin is executed.
    @Parameter
    private UIService uiService;

    @Parameter
    private PrefService prefService;

    private ImagePlus image = null;
    private ImagePlus resultImage = null;
    private StackStatistics resultStats = null;
    private GenericDialog setupDialog;

    private boolean doThickness = THICKNESS_DEFAULT;
    private boolean doSpacing = SPACING_DEFAULT;
    private boolean doGraphic = GRAPHIC_DEFAULT;
    private boolean doRoi = ROI_DEFAULT;
    private boolean doMask = MASK_DEFAULT;

    private void createSetupDialog()
    {
        setupDialog = new GenericDialog("Plugin options");
        setupDialog.addCheckbox("Thickness", doThickness);
        setupDialog.addCheckbox("Spacing", doSpacing);
        setupDialog.addCheckbox("Graphic Result", doGraphic);
        setupDialog.addCheckbox("Crop using ROI Manager", doRoi);
        setupDialog.addCheckbox("Mask thickness map", doMask);
        setupDialog.addHelp(HELP_URL);
    }

    private void getProcessingSettingsFromDialog()
    {
        doThickness = setupDialog.getNextBoolean();
        doSpacing = setupDialog.getNextBoolean();
        doGraphic = setupDialog.getNextBoolean();
        doRoi = setupDialog.getNextBoolean();
        doMask = setupDialog.getNextBoolean();
    }

    /**
     * @return true if the current image open in ImageJ can be processed by this plugin
     */
    private boolean setCurrentImage()
    {
        try {
            image = IJ.getImage();
        } catch (RuntimeException rte) {
            // no image currently open
            return false;
        }

        if (!ImageCheck.isBinary(image)) {
            uiService.showDialog(Common.NOT_BINARY_IMAGE_ERROR, Common.WRONG_IMAGE_TYPE_DIALOG_TITLE);
            return false;
        }

        final double ANISOTROPY_TOLERANCE = 1E-3;
        if (ImageCheck.isVoxelIsotropic(image, ANISOTROPY_TOLERANCE)) {
            return true;
        }

        Result result = uiService.showDialog(Common.ANISOTROPY_WARNING, "Anisotropic voxels",
                MessageType.WARNING_MESSAGE, OptionType.OK_CANCEL_OPTION);

        return result != Result.CANCEL_OPTION;
    }

    @Override
    public void run()
    {
        if (!ImageCheck.isBoneJEnvironmentValid()) {
            return;
        }

        if (!setCurrentImage()) {
            return;
        }

        loadSettings();
        createSetupDialog();
        setupDialog.showDialog();
        if (setupDialog.wasCanceled()) {
            return;
        }
        getProcessingSettingsFromDialog();

        if (!doThickness && !doSpacing) {
            uiService.showDialog("Nothing to process, shutting down plugin.", "Nothing to process",
                    MessageType.INFORMATION_MESSAGE, OptionType.DEFAULT_OPTION);
            return;
        }

        saveSettings();

        if (doThickness) {
            boolean processingCompleted = getLocalThickness(true);
            if(!processingCompleted) {
                return;
            }
            showResultImage();
            showThicknessStats(true);
        }

        if (doSpacing) {
            getLocalThickness(false);
            showResultImage();
            showThicknessStats(false);
        }
    }

    private void loadSettings()
    {
        doThickness = prefService.getBoolean(THICKNESS_PREFERENCE_KEY, THICKNESS_DEFAULT);
        doSpacing = prefService.getBoolean(SPACING_PREFERENCE_KEY, SPACING_DEFAULT);
        doGraphic = prefService.getBoolean(GRAPHIC_PREFERENCE_KEY, GRAPHIC_DEFAULT);
        doRoi = prefService.getBoolean(ROI_PREFERENCE_KEY, ROI_DEFAULT);
        doMask = prefService.getBoolean(MASK_PREFERENCE_KEY, MASK_DEFAULT);
    }

    private void saveSettings()
    {
        prefService.put(THICKNESS_PREFERENCE_KEY, doThickness);
        prefService.put(SPACING_PREFERENCE_KEY, doSpacing);
        prefService.put(GRAPHIC_PREFERENCE_KEY, doGraphic);
        prefService.put(ROI_PREFERENCE_KEY, doRoi);
        prefService.put(MASK_PREFERENCE_KEY, doMask);
    }

    private void showResultImage()
    {
        if (!doGraphic || Interpreter.isBatchMode()) {
            return;
        }

        resultImage.show();
        resultImage.getProcessor().setMinAndMax(0.0, resultStats.max);
        IJ.run("Fire");
    }

    /**
     * Calculate the local thickness measure with various user options from the setup dialog
     * (foreground/background thickness, crop image, show image...).
     *
     * @param doForeground  If true, then process the thickness of the foreground (trabecular thickness).
     *                      If false, then process the thickness of the background (trabecular spacing).
     * @return Returns true if localThickness succeeded, and resultImage != null
     */
    private boolean getLocalThickness(boolean doForeground)
    {
        resultImage = null;
        resultStats = null;

        RoiManager roiManager = RoiManager.getInstance();
        String suffix = doForeground ? "_" + TRABECULAR_THICKNESS : "_" + TRABECULAR_SPACING;

        if (doRoi) {
            ImageStack croppedStack = RoiUtil.cropToRois(roiManager, image.getStack(), true, Common.BINARY_BLACK);

            if (croppedStack == null) {
                uiService.showDialog("There are no valid ROIs in the ROI Manager for cropping", "ROI Manager empty",
                        MessageType.ERROR_MESSAGE, OptionType.DEFAULT_OPTION);
                return false;
            }

            ImagePlus croppedImage = new ImagePlus("", croppedStack);
            croppedImage.copyScale(image);
            resultImage = processThicknessSteps(croppedImage, doForeground, suffix);
        } else {
            resultImage = processThicknessSteps(image, doForeground, suffix);
        }

        resultStats = new StackStatistics(resultImage);

        return true;
    }

    /**
     * Process the given image through all the steps of LocalThickness_ plugin.
     *
     * @param image         Binary (black & white) ImagePlus
     * @param doForeground  If true, then process the thickness of the foreground.
     *                      If false, then process the thickness of the background
     * @return  A new ImagePlus which contains the thickness
     */
    private ImagePlus processThicknessSteps(ImagePlus image, boolean doForeground, String tittleSuffix)
    {
        thicknessWrapper.setSilence(true);
        thicknessWrapper.inverse = !doForeground;
        thicknessWrapper.setShowOptions(false);
        thicknessWrapper.maskThicknessMask = doMask;
        thicknessWrapper.setTitleSuffix(tittleSuffix);
        thicknessWrapper.calibratePixels = true;
        ImagePlus result = thicknessWrapper.processImage(image);
        return result;
    }

    public void showThicknessStats(boolean doForeground)
    {
        String title = resultImage.getTitle();
        String units = resultImage.getCalibration().getUnits();
        String legend = doForeground ? TRABECULAR_THICKNESS : TRABECULAR_SPACING;

        ResultsInserter resultsInserter = new ResultsInserter();
        resultsInserter.setMeasurementInFirstFreeRow(title, legend + " Mean (" + units + ")", resultStats.mean);
        resultsInserter.setMeasurementInFirstFreeRow(title, legend + " Std Dev (" + units + ")", resultStats.stdDev);
        resultsInserter.setMeasurementInFirstFreeRow(title, legend + " Max (" + units + ")", resultStats.max);
        resultsInserter.updateTable();
    }

    public static void main(final String... args)
    {
        net.imagej.Main.launch(args);
    }
}
