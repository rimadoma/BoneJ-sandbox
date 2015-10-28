package common;

/**
 * @author  Richard Domander
 * @date    27/10/15.
 */
public class Common {
    public static final int BINARY_BLACK = 0x00;
    public static final int BINARY_WHITE = 0xFF;

    public static final String ANISOTROPY_WARNING = "This image contains anisotropic voxels, which will\n"
            + "result in incorrect thickness calculation.\n\n"
            + "Consider rescaling your data so that voxels are isotropic\n"
            + "(Image > Scale...).\n\n" + "Continue anyway?";

    public static double clamp(double value, double min, double max) {
        if (Double.compare(value, min) < 0) {
            return min;
        }
        if (Double.compare(value, max) > 0) {
            return max;
        }
        return value;
    }
}
