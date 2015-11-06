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

    @Parameter
    private OverlayService overlayService;

    // @todo Read from a file?
    private static final String HELP_URL = "http://bonej.org/thickness";

    private ImagePlus image = null;

    private GenericDialog setupDialog;

    boolean doThickness = true;
    boolean doSpacing = false;
    boolean doGraphic = true;
    boolean doRoi = false;
    boolean doMask = true;

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

    private void openTestImage() {
        final int WIDTH = 500;
        final int HEIGHT = 500;
        final int DEPTH = 10;
        final int PADDING = 0;
        IJ.newImage("cuboid", "8-bit", WIDTH, HEIGHT, DEPTH);
        image = IJ.getImage();
        image.setImage(TestDataMaker.createCuboid(WIDTH - PADDING, HEIGHT - PADDING, DEPTH, 0xFF, PADDING));
    }

    @Override
    public void run()
    {
        if (!ImageCheck.isBoneJEnvironmentValid()) {
            return;
        }

        /*if (!setCurrentImage()) {
            return;
        }*/

        openTestImage();

        createSetupDialog();
        setupDialog.showDialog();
        if (setupDialog.wasCanceled()) {
            return;
        }
        getProcessingSettingsFromDialog();

        String title = "Test";
        boolean inverse = false;

        RoiManager roiMan = RoiManager.getInstance();

        if (doThickness) {
            logService.info("Do thickness");

            inverse = false;

            if (doRoi) {
                logService.info("Do crop");
                ImageStack stack = RoiUtil.cropToRois(roiMan, image.getStack(), true, 0x00, 1);
                if (stack == null) {
                    logService.info("Error: no valid ROIs");
                }
            } else {
                logService.info("Don't crop");
            }

            title = title + "_Tb.Th";

            if (doGraphic && !Interpreter.isBatchMode()) {
                logService.info("Do graphic");
            }
        }

        if (doSpacing) {
            logService.info("Do spacing");

            inverse = true;

            if (doRoi ) {
                logService.info("Do crop");
                ImageStack stack = RoiUtil.cropToRois(roiMan, image.getStack(), true, 0x00, 1);
                if (stack == null) {
                    logService.info("Error: no valid ROIs");
                }
            } else {
                logService.info("Don't crop");
            }

            title = title + "_Tb.Sp";

            if (doGraphic && !Interpreter.isBatchMode()) {
                logService.info("Do graphic");
            }
        }


        logService.info("Title: " + title);
        logService.info("Inverse: " + inverse);
    }

    public static void main(final String... args)
    {
        final ImageJ ij = net.imagej.Main.launch(args);
        ij.command().run(Thickness.class, true);
    }
}
