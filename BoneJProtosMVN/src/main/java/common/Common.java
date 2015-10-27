package common;

/**
 * @author  Richard Domander
 * @date    27/10/15.
 */
public class Common {
    public static final int BINARY_BLACK = 0x00;
    public static final int BINARY_WHITE = 0xFF;

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
