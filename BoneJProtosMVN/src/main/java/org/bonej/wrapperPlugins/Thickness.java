package org.bonej.wrapperPlugins;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.macro.Interpreter;
import ij.plugin.frame.RoiManager;
import ij.process.StackStatistics;
import net.imagej.Main;
import org.bonej.common.Common;
import org.bonej.common.ImageCheck;
import org.bonej.common.ResultsInserter;
import org.bonej.common.RoiUtil;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.platform.PlatformService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.prefs.PrefService;
import org.scijava.ui.UIService;
import sc.fiji.localThickness.LocalThicknessWrapper;

import java.io.IOException;
import java.net.URL;

import static org.scijava.ui.DialogPrompt.*;

/**
 * A BoneJ wrapper plugin, which is used for a "bone science" flavour of the LocalThickness ImageJ plugin.
 *
 * @author Richard Domander
 */
@Plugin(type = Command.class, menuPath = "Plugins>BoneJ>Thickness")
public class Thickness implements Command
{
    private static final String TRABECULAR_THICKNESS = "Tb.Th";
    private static final String TRABECULAR_SPACING = "Tb.Sp";

    private static final boolean THICKNESS_DEFAULT = true;
    private static final boolean SPACING_DEFAULT = false;
    private static final boolean GRAPHIC_DEFAULT = true;
    private static final boolean ROI_DEFAULT = false;
    private static final boolean MASK_DEFAULT = true;

    // The following service parameters are populated automatically
    // by the SciJava service framework before this command plugin is executed.
    @Parameter
    private UIService uiService;

    @Parameter
    private PrefService prefService;

    @Parameter
    private PlatformService platformService;

    @Parameter
    private LogService logService;

    @Parameter(type = ItemIO.INPUT, persist = false, initializer = "checkPluginRequirements")
    private ImagePlus image = null;

    @Parameter(label = "Trabecular thickness", description = "Calculate the thickness of the trabeculae",
            type = ItemIO.INPUT, required = false)
    private boolean doThickness = THICKNESS_DEFAULT;

    @Parameter(label = "Trabecular spacing", description = "Calculate the thickness of the spaces between trabeculae",
            type = ItemIO.INPUT, required = false)
    private boolean doSpacing = SPACING_DEFAULT;

    @Parameter(label = "Show thickness map image", description = "Show thickness map image(s) after calculations",
            type = ItemIO.INPUT, required = false)
    private boolean doGraphic = GRAPHIC_DEFAULT;

    //@todo Find out how to disable doRoi option if roiManager == null
    @Parameter(label = "Crop using ROI Manager", description = "Limit thickness map(s) toi ROIs in the ROI Manager",
            type = ItemIO.INPUT, required = false, persist = false)
    private boolean doRoi = ROI_DEFAULT;

    @Parameter(label = "Mask thickness map", description = "Remove pixel artifacts from the thickness map(s)",
            type = ItemIO.INPUT, required = false)
    private boolean doMask = MASK_DEFAULT;

    @Parameter(label = "Help", persist = false, callback = "openHelpPage")
    private org.scijava.widget.Button helpButton;

    private ImagePlus resultImage = null;
    private boolean pluginHasRequirements = true;

    @SuppressWarnings("unused")
    private void openHelpPage() {
        try {
            URL helpUrl = new URL("http://bonej.org/thickness");
            platformService.open(helpUrl);
        } catch (final IOException e) {
            logService.error(e);
        }
    }

    @SuppressWarnings("unused")
    private void checkPluginRequirements()
    {
        try {
            ImageCheck.checkIJVersion();
        } catch (RuntimeException e) {
            uiService.showDialog(e.getMessage(), MessageType.ERROR_MESSAGE);
            pluginHasRequirements = false;
            return;
        }

        try {
            image = IJ.getImage();
        } catch (RuntimeException rte) {
            // no image currently open, IJ.getImage() shows an error dialog
            pluginHasRequirements = false;
            return;
        }

        if (!ImageCheck.isBinary(image)) {
            uiService.showDialog(Common.NOT_BINARY_IMAGE_ERROR, Common.WRONG_IMAGE_TYPE_DIALOG_TITLE);
            pluginHasRequirements = false;
            return;
        }

        final double ANISOTROPY_TOLERANCE = 1E-3;
        if (ImageCheck.isVoxelIsotropic(image, ANISOTROPY_TOLERANCE)) {
            pluginHasRequirements = true;
            return;
        }

        Result result = uiService.showDialog(Common.ANISOTROPY_WARNING, "Anisotropic voxels",
                MessageType.WARNING_MESSAGE, OptionType.OK_CANCEL_OPTION);

        pluginHasRequirements = result != Result.CANCEL_OPTION;
    }

    @Override
    public void run()
    {
        if (!pluginHasRequirements) {
            return;
        }

        if (!doThickness && !doSpacing) {
            uiService.showDialog("Nothing to process, shutting down plugin.", "Nothing to process",
                    MessageType.INFORMATION_MESSAGE, OptionType.DEFAULT_OPTION);
            // set doThickness true so that the next time the plugin is run it has something to do by default
            doThickness = true;
            return;
        }

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

    //region -- Utility methods --
    public static void main(final String... args)
    {
        Main.launch(args);
    }
    //endregion

    //region -- Helper methods --
    private void showResultImage()
    {
        if (!doGraphic || Interpreter.isBatchMode()) {
            return;
        }

        resultImage.show();
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

        String suffix = doForeground ? "_" + TRABECULAR_THICKNESS : "_" + TRABECULAR_SPACING;

        if (doRoi) {
            RoiManager roiManager = RoiManager.getInstance();
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

        return true;
    }

    /**
     * Process the given image through all the steps of LocalThickness_ plugin.
     *
     * @param image         Binary (black & white) ImagePlus
     * @param doForeground  If true, then process the thickness of the foreground.
     *                      If false, then process the thickness of the background
     * @param tittleSuffix  Suffix added to the thickness map image title
     * @return  A new ImagePlus which contains the thickness
     */
    private ImagePlus processThicknessSteps(ImagePlus image, boolean doForeground, String tittleSuffix)
    {
        LocalThicknessWrapper thicknessWrapper = new LocalThicknessWrapper();
        thicknessWrapper.setSilence(true);
        thicknessWrapper.inverse = !doForeground;
        thicknessWrapper.setShowOptions(false);
        thicknessWrapper.maskThicknessMap = doMask;
        thicknessWrapper.setTitleSuffix(tittleSuffix);
        thicknessWrapper.calibratePixels = true;
        ImagePlus result = thicknessWrapper.processImage(image);
        return result;
    }

    private void showThicknessStats(boolean doForeground)
    {
        StackStatistics resultStats = new StackStatistics(resultImage);

        String title = resultImage.getTitle();
        String units = resultImage.getCalibration().getUnits();
        String legend = doForeground ? TRABECULAR_THICKNESS : TRABECULAR_SPACING;

        ResultsInserter resultsInserter = new ResultsInserter();
        resultsInserter.setMeasurementInFirstFreeRow(title, legend + " Mean (" + units + ")", resultStats.mean);
        resultsInserter.setMeasurementInFirstFreeRow(title, legend + " Std Dev (" + units + ")", resultStats.stdDev);
        resultsInserter.setMeasurementInFirstFreeRow(title, legend + " Max (" + units + ")", resultStats.max);
        resultsInserter.updateTable();
    }
    //endregion
}
