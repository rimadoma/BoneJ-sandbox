package org.bonej.protoPlugins;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;
import ij.macro.Interpreter;
import ij.plugin.frame.RoiManager;
import net.imagej.ImageJ;
import net.imagej.display.OverlayService;
import org.bonej.common.Common;
import org.bonej.common.ImageCheck;
import org.bonej.common.RoiUtil;
import org.bonej.common.TestDataMaker;
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
    // The following service parameters are populated automatically
    // by the SciJava service framework before this command plugin is executed.
    @Parameter
    private LogService logService;

    @Parameter
    private UIService uiService;

    // @todo Read from a file?
    private static final String HELP_URL = "http://bonej.org/thickness";

    private ImagePlus image = null;

    private GenericDialog setupDialog;

    boolean doThickness = true;
    boolean doSpacing = false;
    boolean doGraphic = true;
    boolean doRoi = false;
    boolean doMask = true;

    // @todo Add dialog titles as public static final Strings so that they can be accessed by a GUI testing robot..?

    /**
     * @todo Remove legacy code, and replace with proper ImageJ2 to show setup dialog
     */
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

        Result result = uiService.showDialog(Common.ANISOTROPY_WARNING, "Anisotropic voxels", MessageType.WARNING_MESSAGE,
                OptionType.OK_CANCEL_OPTION);

        return result != Result.CANCEL_OPTION;
    }

    //@todo write a plugin that can be used to create test images
    public static void openTestImage()
    {
        final int WIDTH = 500;
        final int HEIGHT = 500;
        final int DEPTH = 10;
        final int PADDING = 0;
        IJ.newImage("cuboid", "8-bit", WIDTH, HEIGHT, DEPTH);
        ImagePlus foo = IJ.getImage();
        foo.setImage(TestDataMaker.createCuboid(WIDTH - PADDING, HEIGHT - PADDING, DEPTH, 0xFF, PADDING));
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
            logService.info("Do thickness");

            getLocalThickness(true);
        }

        if (doSpacing) {
            logService.info("Do spacing");

            getLocalThickness(false);
        }
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
        ImagePlus result;
        RoiManager roiManager = RoiManager.getInstance();

        int color = doForeground ? Common.BINARY_BLACK : Common.BINARY_WHITE;

        String title = image.getTitle(); //@Todo stripExtension?
        String suffix = doForeground ? "_Tb.Th" : "_Tb.Sp";
        title = title + suffix;

        logService.info("Title: " + title);
        logService.info("Do foreground: " + doForeground);
        logService.info("Color: " + color);

        if (doRoi) {
            logService.info("Do crop");

            ImageStack croppedStack = RoiUtil.cropToRois(roiManager, image.getStack(), true, 0x00, 1);
            if (croppedStack == null) {
                uiService.showDialog("There are no valid ROIs in the ROI Manager for cropping", "ROI Manager empty",
                        MessageType.ERROR_MESSAGE, OptionType.DEFAULT_OPTION);
                return;
            }
            ImagePlus croppedImage = new ImagePlus("", croppedStack);
            result = processThicknessSteps(croppedImage, doForeground);
        } else {
            logService.info("Don't crop");

            result = processThicknessSteps(image, doForeground);
        }

        if (doGraphic && !Interpreter.isBatchMode()) {
            logService.info("Do graphic");
        } else {
            logService.info("Don't do graphic");
        }

        logService.info("\n");
    }

    /**
     * Process the given image through all the steps of Bob Dougherty's LocalThickness_ plugin.
     *
     * @param image         Binary (black & white) ImagePlus
     * @param doForeground  If true, then process the thickness of the foreground.
     *                      If false, then process the thickness of the background
     * @return
     */
    private ImagePlus processThicknessSteps(ImagePlus image, boolean doForeground) {
        if (doMask) {
            logService.info("Do mask");
        } else {
            logService.info("Don't do mask");
        }

        return null;
    }

    public static void main(final String... args)
    {
        final ImageJ ij = net.imagej.Main.launch(args);

        openTestImage();

        ij.command().run(Thickness.class, true);
    }
}
