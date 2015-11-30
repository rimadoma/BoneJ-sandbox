package org.bonej.wrapperPlugins.triplePointAngles;

import org.scijava.ItemVisibility;
import org.scijava.options.OptionsPlugin;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.ChoiceWidget;
import protoOps.TriplePointAngles;

/**
 * @author <a href="mailto:rdomander@rvc.ac.uk">Richard Domander</a>
 */
@Plugin(type = OptionsPlugin.class)
public class OptionsTriplePointAngles extends OptionsPlugin {
    private static final String DEFAULT_POINT_CHOICE = "Opposite vertex";

    @Parameter(label = "Use:", style = ChoiceWidget.LIST_BOX_STYLE, choices = {"Opposite vertex", "Nth edge pixel"})
    private String pointChoice = DEFAULT_POINT_CHOICE;

    @Parameter(label = "Nth point", min = "0")
    private int nthPoint = TriplePointAngles.DEFAULT_NTH_POINT;

    @Parameter(visibility = ItemVisibility.MESSAGE)
    private final String helpUrl = "http://bonej.org/triplepointangles";

    public int getNthPoint() {
        if (pointChoice.equals("Opposite vertex")) {
            return TriplePointAngles.VERTEX_TO_VERTEX;
        }

        return nthPoint;
    }
}
