package protoPlugins;

import ij.IJ;
import ij.ImagePlus;
import net.imagej.ImageJ;

import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/**
 * @author <a href="mailto:rdomander@rvc.ac.uk">Richard Domander</a>
 * @date 28/10/15
 */
@Plugin(type = Command.class, menuPath = "Plugins>Thickness")
public class Thickness implements Command
{
    // The following service parameters are populated automatically
    // by the service framework before the command is executed.
    @Parameter
    private LogService log;

    private ImagePlus image = null;

    private boolean setCurrentImage()
    {
        try {
            image = IJ.getImage();
        } catch (RuntimeException rte) {
            // no image currently open
            return false;
        }

        return true;
    }

    @Override
    public void run() {
        if (setCurrentImage()) {
            log.info("Opened an image successfully");
        }
    }

    public static void main(final String... args) {
        final ImageJ ij = net.imagej.Main.launch(args);
        IJ.open();
        ij.command().run(Thickness.class, true);
    }
}
