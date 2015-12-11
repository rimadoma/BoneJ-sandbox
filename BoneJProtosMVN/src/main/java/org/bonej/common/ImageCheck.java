package org.bonej.common;

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
    public static final String[] blacklistedIJVersions = {
            // introduced bug where ROIs added to the ROI Manager
            // lost their z-position information
            "1.48a" };

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
     * @param   tolerance   tolerated fractional deviation from equal length [0.0, 2.0]
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
        final double vD = cal.pixelDepth;
        final double tLow = 1.0 - tolerance;
        final double tHigh = 1.0 + tolerance;
        final boolean isStack = (imp.getStackSize() > 1);

        if (vW < vH * tLow || vW > vH * tHigh)
            return false;
        if ((vW < vD * tLow || vW > vD * tHigh) && isStack)
            return false;
        if ((vH < vD * tLow || vH > vD * tHigh) && isStack)
            return false;

        return true;
    }

    /**
     * Show a message and return false if the version of IJ is too old for BoneJ
     * or is a known bad version
     *
     * @return false if the IJ version is too old or blacklisted
     */
    private static boolean isIJVersionValid() {
        if (isIJVersionBlacklisted()) {
            IJ.error(
                    "Bad ImageJ version",
                    "The version of ImageJ you are using (v"
                            + IJ.getVersion()
                            + ") is known to run BoneJ incorrectly.\n"
                            + "Please up- or downgrade your ImageJ using Help-Update ImageJ.");
            return false;
        }

        if (requiredIJVersion.compareTo(IJ.getVersion()) > 0) {
            IJ.error(
                    "Update ImageJ",
                    "You are using an old version of ImageJ, v"
                            + IJ.getVersion() + ".\n"
                            + "Please update to at least ImageJ v"
                            + requiredIJVersion + " using Help-Update ImageJ.");
            return false;
        }
        return true;
    }

    /**
     * Checks if BoneJ has everything it needs to run properly.
     * This includes a compatible version of ImageJ, and the required libraries.
     */
    public static boolean isBoneJEnvironmentValid() {
        try {
            Class.forName("javax.media.j3d.VirtualUniverse");
        } catch (ClassNotFoundException e) {
            IJ.showMessage("Java 3D libraries are not installed.\n " +
                    "Please install and run the ImageJ 3D Viewer,\n"
                    + "which will automatically install Java's 3D libraries.");
            return false;
        }
        try {
            Class.forName("ij3d.ImageJ3DViewer");
        } catch (ClassNotFoundException e) {
            IJ.showMessage("ImageJ 3D Viewer is not installed.\n"
                    + "Please install and run the ImageJ 3D Viewer.");
            return false;
        }

        return isIJVersionValid();
    }

    /**
     * Check if the version of IJ has been blacklisted as a known broken release
     *
     * @return true if the IJ version is blacklisted, false otherwise
     */
    public static boolean isIJVersionBlacklisted() {


        for (String version : blacklistedIJVersions) {
            if (version.equals(IJ.getVersion()))
                return true;
        }
        return false;
    }
}
