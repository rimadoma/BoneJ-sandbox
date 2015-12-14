package org.bonej.wrapperPlugins;

import java.io.IOException;
import java.net.URL;

import net.imagej.Main;

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
	@Parameter(initializer = "initializeActiveImage", required = false)
	private ImagePlus activeImage = null;

	@Parameter(label = "Volume algorithm:", description = "The method used to calculate volume fraction", style = ChoiceWidget.LIST_BOX_STYLE, choices = {
			"Voxel", "Surface"})
	private String volumeAlgorithm = algorithmChoiceStrings.get(VolumeFraction.DEFAULT_VOLUME_ALGORITHM);

	@Parameter(label = "Surface resampling", description = "Voxel resampling (surface algorithm) - higher values result in simpler surfaces", min = "0")
	private int surfaceResampling = VolumeFraction.DEFAULT_SURFACE_RESAMPLING;

	// @todo Disable on init if there is no RoiManager
	@Parameter(label = "Use ROI Manager", initializer = "initRoiManager", description = "restrict measurements to ROIs in the ROI manager")
	private boolean useRoiManager = false;

	// @todo check 3D libs etc. on init, disable if there are none
	@Parameter(label = "Show 3D result", description = "Show the bone and total volume surfaces in the 3D Viewer")
	private boolean show3DResult = false;

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

	/**
	 * @todo Switch uiService.showDialog to cancel
	 */
	@SuppressWarnings("unused")
	private void initializeActiveImage() {
		try {
			volumeFraction.setImage(activeImage);
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
	// endregion
}
