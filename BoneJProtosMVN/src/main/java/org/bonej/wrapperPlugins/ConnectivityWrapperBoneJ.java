package org.bonej.wrapperPlugins;

import net.imagej.Main;

import org.bonej.common.ResultsInserter;
import org.scijava.command.Command;
import org.scijava.command.ContextCommand;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.DialogPrompt;
import org.scijava.ui.UIService;

import protoOps.connectivity.Connectivity;
import ij.ImagePlus;

/**
 * @todo Stop plugin with cancel if init fails
 * @author Richard Domander
 */

@Plugin(type = Command.class, menuPath = "Plugins>BoneJ>Connectivity", headless = true)
public class ConnectivityWrapperBoneJ extends ContextCommand {
	private static final Connectivity connectivity = new Connectivity();

	@Parameter
	private UIService uiService = null;

	// Set required = false to disable the default error message
	@Parameter(initializer = "initializeActiveImage", required = false, persist = false)
	private ImagePlus activeImage = null;

	@Override
	public void run() {
        try {
            connectivity.run();
            showResults();
        } catch (NullPointerException | IllegalArgumentException e) {
            uiService.showDialog(e.getMessage(), DialogPrompt.MessageType.ERROR_MESSAGE);
        }
	}

	// region -- Utility methods --
	public static void main(final String... args) {
		Main.launch(args);
	}
	// endregion

	// region -- Helper methods --
	private void showResults() {
		double eulerCharacteristic = connectivity.getEulerCharacteristic();
		double deltaChi = connectivity.getDeltaChi();
		double conn = connectivity.getConnectivity();
		double connDensity = connectivity.getConnectivityDensity();

		if (conn < 0) {
			uiService.showDialog(
					"Connectivity is negative. This usually happens if there are multiple particles "
							+ "or enclosed cavities. Try running Purify prior to Connectivity.",
					DialogPrompt.MessageType.INFORMATION_MESSAGE);
		}

		String label = activeImage.getTitle();
		String unit = activeImage.getCalibration().getUnit();
		ResultsInserter resultsInserter = new ResultsInserter();
		resultsInserter.setMeasurementInFirstFreeRow(label, "Euler characteristic", eulerCharacteristic);
		resultsInserter.setMeasurementInFirstFreeRow(label, "Δ(χ)", deltaChi);
		resultsInserter.setMeasurementInFirstFreeRow(label, "Connectivity", conn);
		resultsInserter.setMeasurementInFirstFreeRow(label, "Conn. density " + unit + "^-3", connDensity);
		resultsInserter.updateTable();
	}

	@SuppressWarnings("unused")
	private void initializeActiveImage() {
		try {
			connectivity.setInputImage(activeImage);
		} catch (IllegalArgumentException | NullPointerException e) {
			uiService.showDialog(e.getMessage(), DialogPrompt.MessageType.ERROR_MESSAGE);
		}
	}
	// endregion
}
