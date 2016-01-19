package org.bonej.common;

import ij.ImagePlus;

/**
 * @author Richard Domander
 */
public class Common {
	public static final int BINARY_BLACK = 0x00;
	public static final int BINARY_WHITE = 0xFF;

	// @todo move to a ResourceBundle
	public static final String NOT_BINARY_IMAGE_ERROR = "8-bit binary (black and white only) image required.";

	public static final String ANISOTROPY_WARNING = "This image contains anisotropic voxels, which will\n"
			+ "result in incorrect thickness calculation.\n\n"
			+ "Consider rescaling your data so that voxels are isotropic\n" + "(Image > Scale...).\n\n"
			+ "Continue anyway?";

	public static final String WRONG_IMAGE_TYPE_DIALOG_TITLE = "Wrong kind of image";

	public static double clamp(double value, double min, double max) {
		if (Double.compare(value, min) < 0) {
			return min;
		}
		if (Double.compare(value, max) > 0) {
			return max;
		}
		return value;
	}

	public static int clamp(int value, int min, int max) {
		if (Integer.compare(value, min) < 0) {
			return min;
		}
		if (Integer.compare(value, max) > 0) {
			return max;
		}
		return value;
	}

    public static Object getEmptyPixels(final int w, final int h, final int imageType) {
        Object emptyPixels;

        switch (imageType) {
            case ImagePlus.GRAY8:
            case ImagePlus.COLOR_256:
                emptyPixels = new byte[w * h];
                break;
            case ImagePlus.GRAY16:
                emptyPixels = new short[w * h];
                break;
            case ImagePlus.COLOR_RGB:
                emptyPixels = new int[w * h];
                break;
            case ImagePlus.GRAY32:
                emptyPixels = new float[w * h];
                break;
            default:
                throw new IllegalArgumentException("Invalid image type");
        }

        return emptyPixels;
    }
}
