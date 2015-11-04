package org.bonej.common;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;
import ij.process.ImageStatistics;

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
     * @pre     imp != null
     * @return  true if image is binary
     */
    public static boolean isBinary(ImagePlus imp) {
        if (imp.getType() != ImagePlus.GRAY8) {
            return false;
        }

        ImageStatistics stats = imp.getStatistics();
        int blackCount = stats.histogram[Common.BINARY_BLACK];
        int whiteCount = stats.histogram[Common.BINARY_WHITE];

        return  blackCount + whiteCount == stats.pixelCount;
    }

    /**
     * Check if an image is a multi-slice image stack
     *
     * @param   imp image to test
     * @pre     imp != null
     * @return  true if the image has >= 2 slices
     */
    public static boolean isMultiSlice(ImagePlus imp) {
        return imp.getStackSize() >= 2;
    }

    /**
     * Check if the image's voxels are isotropic in all 3 dimensions (i.e. are
     * placed on a cubic grid)
     *
     * @param   imp         image to test
     * @param   tolerance   tolerated fractional deviation from equal length [0.0, 2.0]
     * @pre     imp != null
     * @return true if voxel width == height == depth (within tolerance)
     */
    public static boolean isVoxelIsotropic(ImagePlus imp, double tolerance) {
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
     * Run isVoxelIsotropic() with a default tolerance of 0%
     *
     * @param   imp   image to test
     * @return false if voxel dimensions are not equal
     */
    public static boolean isVoxelIsotropic(ImagePlus imp) {
        return isVoxelIsotropic(imp, 0.0);
    }

    /**
     * Check that the voxel thickness is correct
     *
     * @param imp
     * @return voxel thickness based on DICOM header information. Returns -1 if
     *         there is no DICOM slice position information.
     */
    public double dicomVoxelDepth(ImagePlus imp) {
        Calibration cal = imp.getCalibration();
        double vD = cal.pixelDepth;

        String position = getDicomAttribute(imp, 1, "0020,0032");
        if (position == null) {
            IJ.log("No DICOM slice position data");
            return -1;
        }
        String[] xyz = position.split("\\\\");
        double first = 0;
        if (xyz.length == 3) // we have 3 values
            first = Double.parseDouble(xyz[2]);
        else
            return -1;

        position = getDicomAttribute(imp, imp.getStackSize(), "0020,0032");
        xyz = position.split("\\\\");
        double last = 0;
        if (xyz.length == 3) // we have 3 values
            last = Double.parseDouble(xyz[2]);
        else
            return -1;

        double sliceSpacing = Math.abs((last - first)
                / (imp.getStackSize() - 1));

        String units = cal.getUnits();

        double error = Math.abs((sliceSpacing - vD) / sliceSpacing) * 100;

        if (vD != sliceSpacing) {
            IJ.log(imp.getTitle() + ":\n" + "Current voxel depth disagrees by "
                    + error + "% with DICOM header slice spacing.\n"
                    + "Current voxel depth: " + IJ.d2s(vD, 6) + " " + units
                    + "\n" + "DICOM slice spacing: " + IJ.d2s(sliceSpacing, 6)
                    + " " + units + "\n" + "Updating image properties...");
            cal.pixelDepth = sliceSpacing;
            imp.setCalibration(cal);
        } else
            IJ.log(imp.getTitle() + ": Voxel depth agrees with DICOM header.\n");
        return sliceSpacing;
    }

    /**
     * Get the value associated with a DICOM tag from an ImagePlus header
     *
     * @param imp
     * @param slice
     * @param tag
     *            , in 0000,0000 format.
     * @return the value associated with the tag
     */
    private String getDicomAttribute(ImagePlus imp, int slice, String tag) {
        ImageStack stack = imp.getImageStack();
        String header = stack.getSliceLabel(slice);
        // tag must be in format 0000,0000
        if (slice < 1 || slice > stack.getSize()) {
            return null;
        }
        if (header == null) {
            return null;
        }
        String attribute = " ";
        String value = " ";
        int idx1 = header.indexOf(tag);
        int idx2 = header.indexOf(":", idx1);
        int idx3 = header.indexOf("\n", idx2);
        if (idx1 >= 0 && idx2 >= 0 && idx3 >= 0) {
            try {
                attribute = header.substring(idx1 + 9, idx2);
                attribute = attribute.trim();
                value = header.substring(idx2 + 1, idx3);
                value = value.trim();
                // IJ.log("tag = " + tag + ", attribute = " + attribute
                // + ", value = " + value);
            } catch (Throwable e) {
                return " ";
            }
        }
        return value;
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
     * Check that IJ has enough memory to do the job
     *
     * @param memoryRequirement
     *            Estimated required memory
     * @return True if there is enough memory or if the user wants to continue.
     *         False if the user wants to continue despite a risk of
     *         insufficient memory
     */
    public static boolean checkMemory(long memoryRequirement) {
        if (memoryRequirement > IJ.maxMemory()) {
            String message = "You might not have enough memory to run this job.\n"
                    + "Do you want to continue?";
            if (IJ.showMessageWithCancel("Memory Warning", message)) {
                return true;
            } else {
                return false;
            }
        } else {
            return true;
        }
    }

    public static boolean checkMemory(ImagePlus imp, double ratio) {
        double size = ((double) imp.getWidth() * imp.getHeight() * imp
                .getStackSize());
        switch (imp.getType()) {
            case ImagePlus.GRAY8:
            case ImagePlus.COLOR_256:
                break;
            case ImagePlus.GRAY16:
                size *= 2.0;
                break;
            case ImagePlus.GRAY32:
                size *= 4.0;
                break;
            case ImagePlus.COLOR_RGB:
                size *= 4.0;
                break;
        }
        long memoryRequirement = (long) (size * ratio);
        return checkMemory(memoryRequirement);
    }

    /**
     * Guess whether an image is Hounsfield unit calibrated
     *
     * @param imp
     * @return true if the image might be HU calibrated
     */
    public static boolean huCalibrated(ImagePlus imp) {
        Calibration cal = imp.getCalibration();
        double[] coeff = cal.getCoefficients();
        if (!cal.calibrated() || cal == null
                || (cal.getCValue(0) == 0 && coeff[1] == 1)
                || (cal.getCValue(0) == Short.MIN_VALUE && coeff[1] == 1)) {
            return false;
        } else
            return true;
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