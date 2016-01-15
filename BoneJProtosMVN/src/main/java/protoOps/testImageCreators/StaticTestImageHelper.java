package protoOps.testImageCreators;

import static com.google.common.base.Preconditions.checkArgument;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

/**
 * Static helper methods for the package Ops to get around ImageJ1 / ImageJ2
 * incompatibility issues.
 *
 * Needed because IJ2 Ops / Plugins can't return ImageJ1 classes from their
 * methods.
 *
 * @author Michael Doube
 * @author Richard Domander
 */
public class StaticTestImageHelper {
	/**
	 * Creates an ImagePlus with a cuboid in it
	 *
	 * @param width
	 *            The width of the cuboid
	 * @param height
	 *            The height of the cuboid
	 * @param depth
	 *            The depth of the cuboid (slices)
	 * @param color
	 *            The color of the cuboid
	 * @param padding
	 *            The amount of background (voxels) on each side of the cuboid
	 * @throws IllegalArgumentException
	 *             if width <= 0
	 * @throws IllegalArgumentException
	 *             if height <= 0
	 * @throws IllegalArgumentException
	 *             if depth <= 0
	 * @throws IllegalArgumentException
	 *             if padding < 0
	 * @return An ImagePlus object with a white cuboid on a black background
	 */
	public static ImagePlus createCuboid(int width, int height, int depth, int color, int padding) {
		checkArgument(width > 0, "Width must be positive");
		checkArgument(height > 0, "Height must be positive");
		checkArgument(depth > 0, "Depth must be positive");
		checkArgument(padding >= 0, "Padding must be >= 0");

		int totalPadding = 2 * padding;
		int paddedWidth = width + totalPadding;
		int paddedHeight = height + totalPadding;
		int paddedDepth = depth + totalPadding;

		ImageStack cuboidStack = createEmptyStack(paddedWidth, paddedHeight, paddedDepth);

		final int firstCuboidSlice = padding + 1;
		final int lastCuboidSlice = padding + depth;
		ImageProcessor cuboidProcessor;
		for (int i = firstCuboidSlice; i <= lastCuboidSlice; i++) {
			cuboidProcessor = cuboidStack.getProcessor(i);
			cuboidProcessor.setColor(color);
			cuboidProcessor.setRoi(padding, padding, width, height);
			cuboidProcessor.fill();
		}

		return new ImagePlus("Test cuboid", cuboidStack);
	}

	/**
	 * Draw the wire-frame model i.e. the edges of the given cuboid
	 *
	 * @param width
	 *            Width of the cuboid frame in pixels
	 * @param height
	 *            Height of the cuboid frame in pixels
	 * @param depth
	 *            Depth of the cuboid frame in pixels
	 * @param padding
	 *            Number of pixels added to each side of the cuboid
	 * @throws IllegalArgumentException
	 *             if width <= 0
	 * @throws IllegalArgumentException
	 *             if height <= 0
	 * @throws IllegalArgumentException
	 *             if depth <= 0
	 * @throws IllegalArgumentException
	 *             if padding < 0
	 * @return Image containing a 1-pixel wide outline of a 3D box
	 */
	public static ImagePlus createWireFrameCuboid(int width, int height, int depth, int padding) {
		checkArgument(width > 0, "Width must be positive");
		checkArgument(height > 0, "Height must be positive");
		checkArgument(depth > 0, "Depth must be positive");
		checkArgument(padding >= 0, "Padding must be >= 0");

		final int totalPadding = 2 * padding;
		final int paddedWidth = width + totalPadding;
		final int paddedHeight = height + totalPadding;
		final int paddedDepth = depth + totalPadding;
		final int boxColor = 0xFF;

		ImageStack stack = createEmptyStack(paddedWidth, paddedDepth, paddedHeight);

		// Draw edges in the xy-plane
		ImageProcessor ip = new ByteProcessor(paddedWidth, paddedHeight);
		ip.setColor(boxColor);
		ip.drawRect(padding, padding, width, height);
		stack.setProcessor(ip.duplicate(), padding);
		stack.setProcessor(ip.duplicate(), padding + depth);

		// Draw edges in the xz-plane
		final int firstCuboidSlice = padding + 1;
		final int lastCuboidSlice = padding + depth;
		for (int s = firstCuboidSlice; s <= lastCuboidSlice; s++) {
			ip = stack.getProcessor(s);
			ip.setColor(boxColor);
			ip.drawPixel(padding, padding);
			ip.drawPixel(padding, padding + height - 1);
			ip.drawPixel(padding + width - 1, padding);
			ip.drawPixel(padding + width - 1, padding + height - 1);
		}

		return new ImagePlus("Wire-frame cuboid", stack);
	}

	/**
	 * Draw a circle with a vertical and horizontal line crossing it.
	 *
	 * @param size
	 *            Width and height of the image, circle diameter is size/2
	 * @throws IllegalArgumentException
	 *             if size <= 0
	 * @return A 2D image containing a white (255) circle on black (0)
	 *         background
	 *
	 */
	public static ImagePlus createCrossedCircle(int size) {
		checkArgument(size > 0, "Image size must be positive");

		ImageProcessor ip = new ByteProcessor(size, size);
		ip.setColor(0x00);
		ip.fill();
		ip.setColor(0xFF);
		int half = size / 2;
		int quarter = size / 4;
		ip.drawOval(quarter, quarter, half, half);
		ip.drawLine(half, quarter, half, size - quarter);
		ip.drawLine(quarter, half, size - quarter, half);
		return new ImagePlus("Crossed circle", ip);
	}

	private static ImageStack createEmptyStack(int width, int height, int depth) {
		ImageStack stack = new ImageStack(width, height);
		for (int s = 0; s < depth; s++) {
			ImageProcessor processor = new ByteProcessor(width, height);
			processor.setColor(0x00);
			processor.fill();
			stack.addSlice(processor);
		}
		return stack;
	}
}
