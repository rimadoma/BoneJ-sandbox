package org.bonej.wrapperPlugins.triplePointAngles;

import org.scijava.log.LogService;
import org.scijava.options.OptionsPlugin;
import org.scijava.platform.PlatformService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.Button;
import org.scijava.widget.ChoiceWidget;
import protoOps.TriplePointAngles;

import java.io.IOException;
import java.net.URL;

/**
 * @author Richard Domander
 */
@Plugin(type = OptionsPlugin.class)
public class OptionsTriplePointAngles extends OptionsPlugin {
    private static final String HELP_URL = "http://bonej.org/triplepointangles";
    private static final String DEFAULT_POINT_CHOICE = "Opposite vertex";

    @Parameter
    private PlatformService platformService;

    @Parameter
    private LogService logService;

    @Parameter(label = "Use:", style = ChoiceWidget.LIST_BOX_STYLE, choices = {"Opposite vertex", "Nth edge pixel"})
    private String pointChoice = DEFAULT_POINT_CHOICE;

    @Parameter(label = "Nth point", min = "0")
    private int nthPoint = TriplePointAngles.DEFAULT_NTH_POINT;

    @Parameter(label = "Help", persist = false, callback = "openHelpPage")
    private Button helpButton;

    public int getNthPoint() {
        if (pointChoice.equals("Opposite vertex")) {
            return TriplePointAngles.VERTEX_TO_VERTEX;
        }

        return nthPoint;
    }

    @SuppressWarnings("unused")
    private void openHelpPage() {
        try {
            URL helpUrl = new URL(HELP_URL);
            platformService.open(helpUrl);
        } catch (final IOException e) {
            logService.error(e);
        }
    }
}
