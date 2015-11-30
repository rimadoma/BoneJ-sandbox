package org.bonej.wrapperPlugins.triplePointAngles;

import ij.ImagePlus;
import net.imagej.Main;
import net.imagej.ops.OpService;
import org.bonej.common.Common;
import org.bonej.common.ImageCheck;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.command.CommandModule;
import org.scijava.command.CommandService;
import org.scijava.module.Module;
import org.scijava.module.ModuleService;
import org.scijava.options.OptionsService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;
import protoOps.TriplePointAngles;

import java.util.concurrent.Future;

/**
 * @author <a href="mailto:rdomander@rvc.ac.uk">Richard Domander</a>
 */
@Plugin(type = Command.class, menuPath = "Plugins>BoneJ>TriplePointAngles", headless = true)
public class TriplePointAnglesWrapperBoneJ implements Command
{
    @Parameter(type = ItemIO.INPUT)
    private ImagePlus activeImage = null;

    @Parameter
    private UIService uiService;

    @Parameter
    private OptionsService optionsService;

    @Parameter
    private CommandService commandService;

    @Parameter
    private ModuleService moduleService;

    @Parameter
    private OpService opService;

    private boolean isImageCompatible() {
        if (!ImageCheck.isBinary(activeImage))
        {
            uiService.showDialog(Common.NOT_BINARY_IMAGE_ERROR, Common.WRONG_IMAGE_TYPE_DIALOG_TITLE);
            activeImage = null;
            return false;
        }

        return true;
    }

    @Override
    public void run() {
        if (activeImage == null) {
            return;
        }

        if (!isImageCompatible()) {
            return;
        }

        //@todo: what happens if the wrapper plugin is run as macro?
        final Future<CommandModule> future = commandService.run(OptionsTriplePointAngles.class, true);
        moduleService.waitFor(future);
        OptionsTriplePointAngles options = optionsService.getOptions(OptionsTriplePointAngles.class);
        int nthPoint = options.getNthPoint();
        final Object output = opService.run(TriplePointAngles.class, activeImage, nthPoint);
    }

    public static void main(final String... args)
    {
        Main.launch(args);
    }
}
