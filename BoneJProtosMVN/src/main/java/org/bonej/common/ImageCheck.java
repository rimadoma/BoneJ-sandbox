package org.bonej.common;

import com.google.common.collect.ImmutableList;
import ij.IJ;
import ij.ImagePlus;
import ij.measure.Calibration;
import ij.process.ImageStatistics;

import javax.annotation.Nullable;

/**
 * Check if an image conforms to the type defined by each method.
 *
 * @author Michael Doube
 *
 */
public class ImageCheck {
    /**
     * Minimal ImageJ version required by BoneJ
     */
    public static final String requiredIJVersion = "1.49q";

    /**
     * ImageJ releases known to produce errors or bugs with BoneJ. Daily builds
     * are not included.
     */
    private static final ImmutableList<String> blacklistedIJVersions = ImmutableList.of(
            // introduced bug where ROIs added to the ROI Manager
            // lost their z-position information
            "1.48a");

    /**
     * Check if image is binary
     *
     * @param   imp image to test
     * @return  true if image is binary
     */
    public static boolean isBinary(@Nullable ImagePlus imp) {
        if (imp == null) {
            return false;
        }

        if (imp.getType() != ImagePlus.GRAY8) {
            return false;
        }

        ImageStatistics stats = imp.getStatistics();
        int blackCount = stats.histogram[Common.BINARY_BLACK];
        int whiteCount = stats.histogram[Common.BINARY_WHITE];

        return  blackCount + whiteCount == stats.pixelCount;
    }

    /**
     * Check if the image's voxels are isotropic in all 3 dimensions (i.e. are
     * placed on a cubic grid)
     *
     * @param   imp         image to test
     * @param   tolerance   tolerated fractional deviation from equal length [0.0, 1.0]
     * @return true if voxel width == height == depth (within tolerance)
     */
    public static boolean isVoxelIsotropic(@Nullable ImagePlus imp, double tolerance) {
        if (imp == null) {
            return false;
        }

        tolerance = Common.clamp(tolerance, 0.0, 1.0);

        Calibration cal = imp.getCalibration();
        final double vW = cal.pixelWidth;
        final double vH = cal.pixelHeight;
        final double tLow = 1.0 - tolerance;
        final double tHigh = 1.0 + tolerance;
        final double widthHeightRatio = vW > vH ? vW / vH : vH / vW;
        final boolean imageIs3D = (imp.getStackSize() > 1);

        if (widthHeightRatio < tLow || widthHeightRatio > tHigh) {
            return false;
        }

        if(!imageIs3D) {
            return true;
        }

        final double vD = cal.pixelDepth;
        final double widthDepthRatio =  vW > vD ? vW / vD : vD / vW;

        return (widthDepthRatio >= tLow && widthDepthRatio <= tHigh);
    }

    /**
     * Checks if BoneJ has everything it needs to run properly.
     *
     * @throws ClassNotFoundException if 3D environment is lacking
     * @throws RuntimeException if running an incompatible version of ImageJ
     * @see ImageCheck#checkBoneJEnvironment()
     * @see ImageCheck#check3DEnvironment()
     */
    public static void checkBoneJEnvironment() throws ClassNotFoundException {
        try {
            check3DEnvironment();
            checkIJVersion();
        } catch (ClassNotFoundException | RuntimeException e) {
            throw e;
        }
    }

    /**
     * Checks if BoneJ can display 3D images
     *
     * @throws ClassNotFoundException if either Jave 3D libraries or ImageJ 3D Viewer are not found
     */
    public static void check3DEnvironment() throws ClassNotFoundException {
        try {
            Class.forName("javax.media.j3d.VirtualUniverse");
        } catch (ClassNotFoundException e) {
            throw new ClassNotFoundException("Java 3D libraries are not installed.\n " +
                    "Please install and run the ImageJ 3D Viewer,\n"
                    + "which will automatically install Java's 3D libraries.");
        }
        try {
            Class.forName("ij3d.ImageJ3DViewer");
        } catch (ClassNotFoundException e) {
            throw new ClassNotFoundException("ImageJ 3D Viewer is not installed.\n"
                    + "Please install and run the ImageJ 3D Viewer.");
        }
    }

    /**
     * Show a message and return false if the version of IJ is too old for BoneJ
     * or is a known bad version
     *
     * @throws RuntimeException if the current IJ version is too old or blacklisted
     */
    public static void checkIJVersion() {
        String ijVersion = IJ.getVersion();
        if (blacklistedIJVersions.contains(ijVersion)) {
            throw new RuntimeException(
                    "The version of ImageJ you are using (v"
                            + IJ.getVersion()
                            + ") is known to run BoneJ incorrectly.\n"
                            + "Please upgrade your ImageJ using Help-Update ImageJ.");
        }

        if (requiredIJVersion.compareToIgnoreCase(ijVersion) > 0) {
            throw new RuntimeException(
                    "You are using an old version of ImageJ, v"
                            + IJ.getVersion() + ".\n"
                            + "Please update to at least ImageJ v"
                            + requiredIJVersion + " using Help-Update ImageJ.");
        }
    }
}
