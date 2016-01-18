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

import ij.ImagePlus;
import ij.plugin.frame.RoiManager;

/**
 * A class which wraps the two VolumeFractionOp classes as a single BoneJ plugin in the UI
 *
 * @author Richard Domander
 * @todo Fix settings dialog - should not pop up when init fails
 * @todo Fix render volume surface
 * @todo Threshold dialog (run threshold?)
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

    // @todo add callbacks to keep values sensible (min <= max etc.)
    @Parameter(label = "Minimum threshold", min = "0", stepSize = "5",
            description = "The minimum value for pixels included in the volume calculation", persist = false)
    private int minThreshold;

    @Parameter(label = "Maximum threshold", min = "0", stepSize = "5",
            description = "The maximum value for pixels included in the volume calculation", persist = false)
    private int maxThreshold;

	@Parameter(label = "Help", persist = false, callback = "openHelpPage")
	private Button helpButton;

	@Override
	public void run() {
        volumeFractionSurface.reset();
        volumeFractionVoxel.reset();

		try {
            if (volumeAlgorithm.equals("Surface")) {
                volumeFractionOp = volumeFractionSurface;
                ((VolumeFractionSurface)volumeFractionOp).setSurfaceResampling(surfaceResampling);
            } else {
                volumeFractionOp = volumeFractionVoxel;
            }

            volumeFractionOp.setImage(activeImage);

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
        volumeFractionOp = volumeFractionVoxel;

		try {
            volumeFractionOp.setImage(activeImage);
            minThreshold = volumeFractionOp.getMinThreshold();
            maxThreshold = volumeFractionOp.getMaxThreshold();
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
        char superScriptThree = '\u00B3';
        resultsInserter.setMeasurementInFirstFreeRow(label, "Bone volume (" + unit + superScriptThree + ")",
                volumeFractionOp.getForegroundVolume());
        resultsInserter.setMeasurementInFirstFreeRow(label, "Total volume (" + unit + superScriptThree + ")",
                volumeFractionOp.getTotalVolume());
        resultsInserter.setMeasurementInFirstFreeRow(label, "Volume ratio", volumeFractionOp.getVolumeRatio());
        resultsInserter.updateTable();
    }

    /**
     * @todo Don't show if run from CLI
     */
    private void renderVolumeSurfaces() {
        if (!(volumeFractionOp instanceof VolumeFractionSurface)) {
            return;
        }

        uiService.showDialog("Volume surface rendering not functional due Java3D issues with Java 8");

        /*VolumeFractionSurface volumeFractionSurface = (VolumeFractionSurface)volumeFractionOp;
        CustomTriangleMesh foregroundSurface = volumeFractionSurface.getForegroundSurface();
        CustomTriangleMesh totalSurface = volumeFractionSurface.getTotalSurface();

        Image3DUniverse universe = new Image3DUniverse();
        universe.addCustomMesh(foregroundSurface, "Bone volume");
        universe.addCustomMesh(totalSurface, "Total volume");
        universe.show();*/
    }
	// endregion
}
