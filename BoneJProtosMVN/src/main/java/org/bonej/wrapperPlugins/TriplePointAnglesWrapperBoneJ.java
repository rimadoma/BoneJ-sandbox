package org.bonej.wrapperPlugins;

import ij.ImagePlus;
import net.imagej.Main;
import net.imagej.ops.OpService;
import org.bonej.common.Common;
import org.bonej.common.ImageCheck;
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
import protoOps.TriplePointAngles;

import java.io.IOException;
import java.net.URL;

/**
 * @author Richard Domander
 */
@Plugin(type = Command.class, menuPath = "Plugins>BoneJ>TriplePointAngles", headless = true)
public class TriplePointAnglesWrapperBoneJ extends ContextCommand
{
    private static final String DEFAULT_POINT_CHOICE = "Opposite vertex";

    private double angleResults [][][] = null;

    @Parameter(label = "Calculate angles from:", style = ChoiceWidget.LIST_BOX_STYLE,
            choices = {"Opposite vertex", "Edge voxel n"})
    private String pointChoice = DEFAULT_POINT_CHOICE;

    @Parameter(label = "Edge voxel n:o", min = "0")
    private int nthPoint = TriplePointAngles.DEFAULT_NTH_POINT;

    //@todo check if callback method can be in another class / package
    //@todo check if callback method can be given params
    @Parameter(label = "Help", persist = false, callback = "openHelpPage")
    private Button helpButton;

    @Parameter(type = ItemIO.INPUT, initializer = "checkActiveImage")
    private ImagePlus activeImage = null;

    @Parameter
    private LogService logService;

    @Parameter
    private UIService uiService;

    @Parameter
    private OpService opService;

    @Parameter
    private PlatformService platformService;

    public int nthPointFromPointChoice() {
        if (pointChoice.equals("Opposite vertex")) {
            return TriplePointAngles.VERTEX_TO_VERTEX;
        }

        return nthPoint;
    }

    @Override
    public void run() {
        nthPoint = nthPointFromPointChoice();
        angleResults = (double[][][]) opService.run(TriplePointAngles.class, activeImage, nthPoint);

        if (angleResults == null) {
            uiService.showDialog("Image cannot be converted into skeletons", DialogPrompt.MessageType.ERROR_MESSAGE);
            return;
        }

        showResults();
    }

    //region -- Utility methods --
    public static void main(final String... args)
    {
       Main.launch(args);
    }
    //endregion

    //region -- Helper methods --
    @SuppressWarnings("unused")
    private void checkActiveImage() {
        if (activeImage == null) {
            return;
        }

        if (!ImageCheck.isBinary(activeImage))
        {
            System.out.println("Not binary");
            cancel(Common.NOT_BINARY_IMAGE_ERROR);
        }
    }

    @SuppressWarnings("unused")
    private void openHelpPage() {
        try {
            URL helpUrl = new URL("http://bonej.org/triplepointangles");
            platformService.open(helpUrl);
        } catch (final IOException e) {
            logService.error(e);
        }
    }

    /**
     * Shows the angles of the triple points in the default results table
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
    //endregion
}
