package org.bonej.wrapperPlugins;

import java.io.IOException;
import java.net.URL;

import net.imagej.Main;

import org.bonej.common.ResultsInserter;
import org.scijava.command.Command;
import org.scijava.command.ContextCommand;
import org.scijava.platform.PlatformService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.DialogPrompt;
import org.scijava.ui.UIService;
import org.scijava.widget.Button;
import org.scijava.widget.ChoiceWidget;

import protoOps.volumeFraction.VolumeFractionOp;
import protoOps.volumeFraction.VolumeFractionSurface;
import protoOps.volumeFraction.VolumeFractionVoxel;

import com.google.common.collect.ImmutableList;

import customnode.CustomTriangleMesh;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.WaitForUserDialog;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;
import ij3d.Image3DUniverse;

/**
 * A class which wraps the two VolumeFractionOp classes as a single BoneJ plugin in the UI
 *
 * @author Richard Domander
 * @todo Fix settings dialog - should not pop up when init fails
 * @todo Find a way to set thresholds in UI without a modal dialog
 */
@Plugin(type = Command.class, menuPath = "Plugins>BoneJ>VolumeFraction", headless = true)
public class VolumeFractionWrapperBoneJ extends ContextCommand {
	private static final ImmutableList<String> algorithmChoiceStrings = ImmutableList.of("Voxel", "Surface");
    private static final VolumeFractionSurface volumeFractionSurface = new VolumeFractionSurface();
    private static final VolumeFractionVoxel volumeFractionVoxel = new VolumeFractionVoxel();

    private VolumeFractionOp volumeFractionOp;
	private RoiManager roiManager = null;

	@Parameter
	private UIService uiService = null;

	@Parameter
	private PlatformService platformService = null;

	// Set required = false to disable the default error message
	@Parameter(initializer = "initializeActiveImage", required = false, persist = false)
	private ImagePlus activeImage = null;

	@Parameter(label = "Volume algorithm:", description = "The method used to calculate volume fraction",
            style = ChoiceWidget.LIST_BOX_STYLE, choices = {"Voxel", "Surface"}, persist = false)
	private String volumeAlgorithm = algorithmChoiceStrings.get(0); // Voxel is the default algorithm

	@Parameter(label = "Surface resampling",
            description = "Voxel resampling (surface algorithm) - higher values result in simpler surfaces", min = "0")
	private int surfaceResampling = VolumeFractionSurface.DEFAULT_SURFACE_RESAMPLING;

	// @todo Disable on init if there is no RoiManager
	@Parameter(label = "Use ROI Manager", initializer = "initRoiManager",
            description = "restrict measurements to ROIs in the ROI manager", persist = false)
	private boolean useRoiManager = false;

	// @todo check 3D libs etc. on init, disable if there are none
	@Parameter(label = "Show 3D result", description = "Show the bone and total volume surfaces in the 3D Viewer")
	private boolean show3DResult = false;

	@Parameter(label = "Help", persist = false, callback = "openHelpPage")
	private Button helpButton;

	@Override
	public void run() {
		try {
            if (volumeAlgorithm.equals("Surface")) {
                volumeFractionOp = volumeFractionSurface;
                ((VolumeFractionSurface)volumeFractionOp).setSurfaceResampling(surfaceResampling);
            } else {
                volumeFractionOp = volumeFractionVoxel;
            }

            volumeFractionOp.reset();

            volumeFractionOp.setImage(activeImage);

            if (volumeFractionOp.needThresholds()) {
                thresholdImage();
            }

			if (useRoiManager) {
				volumeFractionOp.setRoiManager(roiManager);
			}
		} catch (NullPointerException | IllegalArgumentException e) {
			uiService.showDialog(e.getMessage(), DialogPrompt.MessageType.ERROR_MESSAGE);
			return;
		}

		volumeFractionOp.run();

        showVolumeResults();

        if (show3DResult) {
            renderVolumeSurfaces();
        }
	}

    // region -- Utility methods --
	public static void main(final String... args) {
		Main.launch(args);
	}
	// endregion

	// region -- Helper methods --
	@SuppressWarnings("unused")
	private void initRoiManager() {
		roiManager = RoiManager.getInstance();
		useRoiManager = roiManager != null;
	}

	@SuppressWarnings("unused")
	private void initializeActiveImage() {
		try {
            VolumeFractionOp.checkImage(activeImage);
		} catch (IllegalArgumentException | NullPointerException e) {
			uiService.showDialog(e.getMessage(), DialogPrompt.MessageType.ERROR_MESSAGE);
		}
    }

	@SuppressWarnings("unused")
	private void openHelpPage() {
		try {
			URL helpUrl = new URL("http://bonej.org/volumefraction");
			platformService.open(helpUrl);
		} catch (final IOException e) {
			uiService.showDialog("An error occurred while trying to open the help page");
		}
	}

    /**
     * @todo Don't show if run from CLI
     */
    private void showVolumeResults() {
        ResultsInserter resultsInserter = new ResultsInserter();
        String unit = activeImage.getCalibration().getUnits();
        String label = activeImage.getTitle();

        String degreeDescription;
        String capitalDescription;
        char degreeSign;
        if (activeImage.getNSlices() == 1) {
            degreeDescription = "area";
            capitalDescription = "Area";
            degreeSign = '\u00B2';
        } else {
            degreeDescription = "volume";
            capitalDescription = "Volume";
            degreeSign = '\u00B3';
        }

		resultsInserter.setMeasurementInFirstFreeRow(label,
				"Bone " + degreeDescription + " (" + unit + degreeSign + ")", volumeFractionOp.getForegroundVolume());
		resultsInserter.setMeasurementInFirstFreeRow(label,
				"Total " + degreeDescription + " (" + unit + degreeSign + ")", volumeFractionOp.getTotalVolume());
		resultsInserter.setMeasurementInFirstFreeRow(label, capitalDescription + " ratio",
				volumeFractionOp.getVolumeRatio());
		resultsInserter.updateTable();
    }

    /**
     * @todo Don't show if run from CLI
     */
    private void renderVolumeSurfaces() {
        if (!(volumeFractionOp instanceof VolumeFractionSurface)) {
            // show error message
            return;
        }

        VolumeFractionSurface volumeFractionSurface = (VolumeFractionSurface)volumeFractionOp;
        CustomTriangleMesh foregroundSurface = volumeFractionSurface.getForegroundSurface();
        CustomTriangleMesh totalSurface = volumeFractionSurface.getTotalSurface();

        Image3DUniverse universe = new Image3DUniverse();
        universe.addCustomMesh(foregroundSurface, "Bone volume");
        universe.addCustomMesh(totalSurface, "Total volume");
        universe.show();
    }

    private void thresholdImage() {
        IJ.run("Threshold...");
        new WaitForUserDialog("Set the threshold, then click OK.").show();
        ImageProcessor activeProcessor = activeImage.getProcessor();
        int min = (int) Math.round(activeProcessor.getMinThreshold());
        int max = (int) Math.round(activeProcessor.getMaxThreshold());
        volumeFractionOp.setThresholds(min, max);
    }
	// endregion
}
