package org.bonej.protoPlugins;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;
import ij.macro.Interpreter;
import ij.plugin.frame.RoiManager;
import ij.process.StackStatistics;
import net.imagej.ImageJ;
import net.imagej.patcher.LegacyInjector;
import org.bonej.common.*;
import org.bonej.localThickness.LocalThicknessWrapper;
import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

import static org.scijava.ui.DialogPrompt.*;

/**
 * A plugin for processing the local thickness measure for bone images.
 *
 * @author <a href="mailto:rdomander@rvc.ac.uk">Richard Domander</a>
 * @todo instead of LogService open error dialogs when things go wrong
 */
@Plugin(type = Command.class, menuPath = "Plugins>Thickness")
public class Thickness implements Command
{
    // Need this because we're using ImageJ 1.x classes
    static {
        LegacyInjector.preinit();
    }

    private static final String HELP_URL = "http://bonej.org/thickness"; // @todo Read from a file?
    private static final String TRABECULAR_THICKNESS = "Tb.Th";
    private static final String TRABECULAR_SPACING = "Tb.Sp";

    private final LocalThicknessWrapper thicknessWrapper = new LocalThicknessWrapper();

    // The following service parameters are populated automatically
    // by the SciJava service framework before this command plugin is executed.
    @Parameter
    private LogService logService;

    @Parameter
    private UIService uiService;

    private ImagePlus image = null;
    private ImagePlus resultImage = null;
    private StackStatistics resultStats = null;
    private GenericDialog setupDialog;

    private boolean doThickness = true;
    private boolean doSpacing = false;
    private boolean doGraphic = true;
    private boolean doRoi = false;
    private boolean doMask = true;

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
            logService.error("8-bit binary (black and white only) image required.");
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

        if (doThickness) {
            getLocalThickness(true);
            showResultImage();
            showThicknessStats(true);
        }

        if (doSpacing) {
            getLocalThickness(false);
            showResultImage();
            showThicknessStats(false);
        }
    }

    private void showResultImage() {
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
     */
    private void getLocalThickness(boolean doForeground)
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
                return;
            }

            ImagePlus croppedImage = new ImagePlus("", croppedStack);
            croppedImage.copyScale(image);
            resultImage = processThicknessSteps(croppedImage, doForeground, suffix);
        } else {
            resultImage = processThicknessSteps(image, doForeground, suffix);
        }

        resultStats = new StackStatistics(resultImage);
    }

    /**
     * Process the given image through all the steps of LocalThickness_ plugin.
     *
     * @param image         Binary (black & white) ImagePlus
     * @param doForeground  If true, then process the thickness of the foreground.
     *                      If false, then process the thickness of the background
     * @return
     */
    private ImagePlus processThicknessSteps(ImagePlus image, boolean doForeground, String tittleSuffix)
    {
        thicknessWrapper.setSilence(true);
        thicknessWrapper.inverse = !doForeground;
        thicknessWrapper.setShowOptions(false);
        thicknessWrapper.trimThicknessMap = doMask;
        thicknessWrapper.setTitleSuffix(tittleSuffix);
        ImagePlus result = thicknessWrapper.processImage(image);
        result.copyScale(image);
        Common.pixelValuesToCalibratedValues(result);
        // Needed so that pixels with 0.0f (background) value don't affect the statistical mean
        Common.backgroundToNaN(result, 0.0f);
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
        final ImageJ ij = net.imagej.Main.launch(args);
        ij.command().run(Thickness.class, true);
    }
}
