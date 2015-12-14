package org.bonej.wrapperPlugins;

import java.io.IOException;
import java.net.URL;

import net.imagej.Main;

import org.bonej.common.ResultsInserter;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.command.ContextCommand;
import org.scijava.log.LogService;
import org.scijava.platform.PlatformService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.DialogPrompt;
import org.scijava.ui.UIService;
import org.scijava.widget.Button;
import org.scijava.widget.ChoiceWidget;

import protoOps.triplePointAngles.TriplePointAngles;
import ij.ImagePlus;

/**
 * A wrapper UI class for the TriplePointAngles Op. Can be thought as the "View"
 * class for the "Model" class that is TriplePointAngles.
 *
 * @author Richard Domander
 */
@Plugin(type = Command.class, menuPath = "Plugins>BoneJ>TriplePointAngles", headless = true)
public class TriplePointAnglesWrapperBoneJ extends ContextCommand {
	private static final String DEFAULT_POINT_CHOICE = "Branch end";
	private static final TriplePointAngles triplePointAngles = new TriplePointAngles();

	private double angleResults[][][] = null;

	@Parameter(label = "Angle measurement point:", style = ChoiceWidget.LIST_BOX_STYLE, description = "Measure angles from ends of the branches, or n voxels \"up\" the branch", choices = {
			"Branch end", "Edge voxel n"})
	private String pointChoice = DEFAULT_POINT_CHOICE;

	@Parameter(label = "Edge voxel #number", description = "Number of voxels the angle measurement point is from the ends of the branches", min = "0")
	private int nthPoint = TriplePointAngles.DEFAULT_NTH_POINT;

	@Parameter(label = "Help", persist = false, callback = "openHelpPage")
	private Button helpButton;

	// set required to false to disable the default, generic error message
	@Parameter(type = ItemIO.INPUT, initializer = "initializeActiveImage", required = false)
	private ImagePlus activeImage = null;

	@Parameter
	private LogService logService;

	@Parameter
	private UIService uiService;

	@Parameter
	private PlatformService platformService;

	@Override
	public void run() {
		nthPoint = nthPointFromPointChoice();

		try {
			triplePointAngles.setNthPoint(nthPoint);
			triplePointAngles.calculateTriplePointAngles();
		} catch (IllegalArgumentException e) {
			uiService.showDialog(e.getMessage(), DialogPrompt.MessageType.ERROR_MESSAGE);
			return;
		}

		angleResults = triplePointAngles.getResults();
		showResults();
	}

	// region -- Utility methods --
	public static void main(final String... args) {
		Main.launch(args);
	}
	// endregion

	// region -- Helper methods --
	private int nthPointFromPointChoice() {
		if (pointChoice.equals("Branch end")) {
			return TriplePointAngles.VERTEX_TO_VERTEX;
		}

		return nthPoint;
	}

	/**
	 * @todo Find out why cancel doesn't work
	 */
	@SuppressWarnings("unused")
	private void initializeActiveImage() {
		try {
			triplePointAngles.setInputImage(activeImage);
		} catch (IllegalArgumentException | NullPointerException e) {
			cancel(e.getMessage());
		}
	}

	@SuppressWarnings("unused")
	private void openHelpPage() {
		try {
			URL helpUrl = new URL("http://bonej.org/triplepointangles");
			platformService.open(helpUrl);
		} catch (final IOException e) {
			uiService.showDialog("An error occurred while trying to open the help page");
		}
	}

	/**
	 * Shows the angles of the triple points in the default results table
	 * 
	 * @todo Don't show results if running headless / in macro mode
	 */
	private void showResults() {
		ResultsInserter resultsInserter = new ResultsInserter();
		String label = activeImage.getTitle();

		for (int graph = 0; graph < angleResults.length; graph++) {
			for (int vertex = 0; vertex < angleResults[graph].length; vertex++) {
				resultsInserter.setMeasurementInFirstFreeRow(label, "Skeleton #", graph);
				resultsInserter.setMeasurementInFirstFreeRow(label, "Vertex #", vertex);
				resultsInserter.setMeasurementInFirstFreeRow(label, "Theta 0", angleResults[graph][vertex][0]);
				resultsInserter.setMeasurementInFirstFreeRow(label, "Theta 1", angleResults[graph][vertex][1]);
				resultsInserter.setMeasurementInFirstFreeRow(label, "Theta 2", angleResults[graph][vertex][2]);
			}
		}

		resultsInserter.updateTable();
	}
	// endregion
}
