package org.bonej.wrapperPlugins;

import java.io.IOException;
import java.net.URL;

import customnode.CustomTriangleMesh;
import ij3d.Image3DUniverse;
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

import protoOps.volumeFraction.VolumeFraction;

import com.google.common.collect.ImmutableList;

import ij.ImagePlus;
import ij.plugin.frame.RoiManager;

/**
 * BoneJ UI wrapper class for the VolumeFraction Op class
 *
 * @author Richard Domander
 */
@Plugin(type = Command.class, menuPath = "Plugins>BoneJ>VolumeFraction", headless = true)
public class VolumeFractionWrapperBoneJ extends ContextCommand {
	private static final ImmutableList<String> algorithmChoiceStrings = ImmutableList.of("Voxel", "Surface");
	private final VolumeFraction volumeFraction = new VolumeFraction();
	private RoiManager roiManager = null;

	@Parameter
	private UIService uiService = null;

	@Parameter
	private PlatformService platformService = null;

	// Set required = false to disable the default error message
	@Parameter(initializer = "initializeActiveImage", required = false, persist = false)
	private ImagePlus activeImage = null;

	@Parameter(label = "Volume algorithm:", description = "The method used to calculate volume fraction",
            style = ChoiceWidget.LIST_BOX_STYLE, choices = {"Voxel", "Surface"})
	private String volumeAlgorithm = algorithmChoiceStrings.get(VolumeFraction.DEFAULT_VOLUME_ALGORITHM);

	@Parameter(label = "Surface resampling",
            description = "Voxel resampling (surface algorithm) - higher values result in simpler surfaces", min = "0")
	private int surfaceResampling = VolumeFraction.DEFAULT_SURFACE_RESAMPLING;

	// @todo Disable on init if there is no RoiManager
	@Parameter(label = "Use ROI Manager", initializer = "initRoiManager",
            description = "restrict measurements to ROIs in the ROI manager", persist = false)
	private boolean useRoiManager = false;

	// @todo check 3D libs etc. on init, disable if there are none
	@Parameter(label = "Show 3D result", description = "Show the bone and total volume surfaces in the 3D Viewer")
	private boolean show3DResult = false;

    // @todo get thresholds with @Parameters, or try to run ImageJ "Threshold" plugin?
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
		try {
			int algorithm = algorithmChoiceStrings.indexOf(volumeAlgorithm);
			volumeFraction.setVolumeAlgorithm(algorithm);
			volumeFraction.setSurfaceResampling(surfaceResampling);
			if (useRoiManager) {
				volumeFraction.setRoiManager(roiManager);
			}
		} catch (NullPointerException | IllegalArgumentException e) {
			uiService.showDialog(e.getMessage(), DialogPrompt.MessageType.ERROR_MESSAGE);
			return;
		}

		volumeFraction.run();

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
			volumeFraction.setImage(activeImage);
            minThreshold = volumeFraction.getMinThreshold();
            maxThreshold = volumeFraction.getMaxThreshold();
		} catch (IllegalArgumentException | NullPointerException e) {
            // @todo Switch uiService.showDialog to cancel
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
                volumeFraction.getForegroundVolume());
        resultsInserter.setMeasurementInFirstFreeRow(label, "Total volume (" + unit + superScriptThree + ")",
                volumeFraction.getTotalVolume());
        resultsInserter.setMeasurementInFirstFreeRow(label, "Volume ratio", volumeFraction.getVolumeRatio());
        resultsInserter.updateTable();
    }

    private void renderVolumeSurfaces() {
        CustomTriangleMesh foregroundSurface = volumeFraction.getForegroundSurface();
        CustomTriangleMesh totalSurface = volumeFraction.getTotalSurface();

        Image3DUniverse universe = new Image3DUniverse();
        universe.addCustomMesh(foregroundSurface, "Bone volume");
        universe.addCustomMesh(totalSurface, "Total volume");
        universe.show();
    }
	// endregion
}
