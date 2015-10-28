package geometry;

public class Vectors {
    /**
     * Calculate the cross product of two vectors (x1, y1, z1) and (x2, y2, z2)
     *
     * @param x1
     * @param y1
     * @param z1
     * @param x2
     * @param y2
     * @param z2
     * @return cross product in {x, y, z} format
     */
    public static double[] crossProduct(double x1, double y1, double z1,
                                        double x2, double y2, double z2) {
        final double x = y1 * z2 - z1 * y2;
        final double y = z1 * x2 - x1 * z2;
        final double z = x1 * y2 - y1 * x2;
        double[] crossVector = { x, y, z };
        return crossVector;
    }

    /**
     * Calculate the cross product of 2 vectors, both in double[3] format
     *
     * @param a
     *            first vector
     * @param b
     *            second vector
     * @return resulting vector in double[3] format
     */
    public static double[] crossProduct(double[] a, double[] b) {
        return crossProduct(a[0], a[1], a[2], b[0], b[1], b[2]);
    }

    /**
     * Normalise a vector to have a length of 1 and the same orientation as the
     * input vector a
     *
     * @param a
     * @return Unit vector in direction of a
     */
    public static double[] norm(double[] a) {
        final double a0 = a[0];
        final double a1 = a[1];
        final double a2 = a[2];
        final double length = Math.sqrt(a0 * a0 + a1 * a1 + a2 * a2);

        double[] normed = new double[3];
        normed[0] = a0 / length;
        normed[1] = a1 / length;
        normed[2] = a2 / length;
        return normed;
    }

    /**
     * Generate an array of randomly-oriented 3D unit vectors
     *
     * @param nVectors
     *            number of vectors to generate
     * @return 2D array (nVectors x 3) containing unit vectors
     */
    public static double[][] randomVectors(int nVectors) {
        double[][] randomVectors = new double[nVectors][3];

        for (int n = 0; n < nVectors; n++)
            randomVectors[n] = randomVector();

        return randomVectors;
    }

    /**
     * Generate a single randomly-oriented vector on the unit sphere
     *
     * @return 3-element double array containing [x y z]^T
     */
    public static double[] randomVector(){
        final double z = 2 * Math.random() - 1;
        final double rho = Math.sqrt(1 - z * z);
        final double phi = Math.PI * (2 * Math.random() - 1);
        final double x = rho * Math.cos(phi);
        final double y = rho * Math.sin(phi);
        return new double[]{x, y, z};
    }

    /**
     * Generate an array of regularly-spaced 3D unit vectors. The vectors aren't
     * equally spaced in all directions, but there is no clustering around the
     * sphere's poles.
     *
     * @param nVectors
     *            number of vectors to generate
     *
     * @return 2D array (nVectors x 3) containing unit vectors
     */
    public static double[][] regularVectors(final int nVectors) {

        double[][] vectors = new double[nVectors][];
        final double inc = Math.PI * (3 - Math.sqrt(5));
        final double off = 2 / (double) nVectors;

        for (int k = 0; k < nVectors; k++) {
            final double y = k * off - 1 + (off / 2);
            final double r = Math.sqrt(1 - y * y);
            final double phi = k * inc;
            final double x = Math.cos(phi) * r;
            final double z = Math.sin(phi) * r;
            final double[] vector = { x, y, z };
            vectors[k] = vector;
        }
        return vectors;
    }
}

