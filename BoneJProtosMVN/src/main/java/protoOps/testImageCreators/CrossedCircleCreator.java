package protoOps.testImageCreators;

import net.imagej.DatasetService;
import net.imagej.ImageJ;
import net.imagej.ops.Op;
import net.imagej.ops.OpEnvironment;

import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import ij.ImagePlus;

/**
 * An Op which creates an image of a circle and a cross. Can be used, e.g. for
 * testing other Ops or Plugins.
 *
 * @author Richard Domander
 */
@Plugin(type = Op.class, name = "crossedCircleCreator", menuPath = "Plugins>Test Images>Crossed circle")
public class CrossedCircleCreator implements Op {
	@Parameter
	private DatasetService datasetService;

	@Parameter(min = "10", required = false, description = "Width and height of the resulting image (px)")
	private int imageSize = 200;

	/**
	 * @todo Find out why the user sees nothing if this ImagePlus is converted
	 *       into a Dataset (pixel values are ok).
	 */
	@Parameter(type = ItemIO.OUTPUT)
	private ImagePlus testImage = null;

	@Override
	public void run() {
		createCrossedCircle();
	}

	@Override
	public OpEnvironment ops() {
		return null;
	}

	@Override
	public void setEnvironment(OpEnvironment opEnvironment) {

	}

	/**
	 * Draw a circle with a vertical and horizontal line crossing it to
	 * this.testImage
	 */
	private void createCrossedCircle() {
		testImage = StaticTestImageHelper.createCrossedCircle(imageSize);
	}

	public static void main(final String... args) throws Exception {
		final ImageJ ij = new ImageJ();

		final Object image = ij.op().run("crossedCircleCreator");

		ij.ui().showUI();
		ij.ui().show(image);
	}
}
