package org.bonej.localThickness;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.PlugIn;

/**
 * A class which wraps the different stages of LocalThickness processing.
 *
 * By default LocalThicknessWrapper runs the stages "silently" so that the user doesn't see the intermediate images
 * between the them. Even if the intermediate images are shown, the user won't be able to interrupt the process like in
 * Local_Thickness_Driver  (think what happens if an image before WindowManager.getCurrentImage() is called...).
 *
 * By default the class won't show the options (threshold, inverse) dialog for LocalThickness.
 * Instead it's run with default values defined in EDT_S1D.DEFAULT_INVERSE and EDT_S1D.DEFAULT_THRESHOLD respectively.
 *
 * @author <a href="mailto:rdomander@rvc.ac.uk">Richard Domander</a>
 */
public class LocalThicknessWrapper implements PlugIn
{
    private static final String DEFAULT_TITLE_SUFFIX = "_LocThk";
    private static final String DEFAULT_TITLE = "ThicknessMap";

    private static final EDT_S1D geometryToDistancePlugin = new EDT_S1D();
    private static final Distance_Ridge distanceRidgePlugin = new Distance_Ridge();
    private static final Local_Thickness_Parallel localThicknessPlugin = new Local_Thickness_Parallel();
    private static final Clean_Up_Local_Thickness thicknessCleaningPlugin = new Clean_Up_Local_Thickness();
    private static final MaskThicknessMapWithOriginal thicknessMask = new MaskThicknessMapWithOriginal();

    private ImagePlus resultImage = null;
    private boolean showOptions = false;
    private String titleSuffix = DEFAULT_TITLE_SUFFIX;

    // Fields used to set LocalThickness options programmatically
    /**
     * A pixel is considered to be a part of the background if its color < threshold
     */
    public int threshold = EDT_S1D.DEFAULT_THRESHOLD;

    /**
     * Inverts thresholding so that pixels with values >= threshold are considered background.
     */
    public boolean inverse = EDT_S1D.DEFAULT_INVERSE;

    /**
     * Controls whether the Thickness map gets masked with the original @see MaskThicknessMapWithOriginal
     */
    public boolean maskThicknessMask = true;

    /**
     * Controls whether the pixel values in the Thickness map get scaled @see LocalThicknessWrapper.calibratePixels()
     */
    public boolean calibratePixels = true;

    public LocalThicknessWrapper()
    {
        setSilence(true);
        geometryToDistancePlugin.showOptions = showOptions;
    }

    /**
     * Controls whether the options dialog (threshold, inverse) is shown to the user before processing starts.
     *
     * @param show  If true, then the dialog is shown.
     */
    public void setShowOptions(boolean show)
    {
        showOptions = show;
        geometryToDistancePlugin.showOptions = show;
    }

    /**
     * Sets whether intermediate images are shown to the user, or only the final result
     *
     * @param silent If true, then no intermediate images are shown in the GUI.
     */
    public void setSilence(boolean silent)
    {
        distanceRidgePlugin.runSilent = silent;
        localThicknessPlugin.runSilent = silent;
        thicknessCleaningPlugin.runSilent = silent;
        geometryToDistancePlugin.runSilent = silent;
    }

    public ImagePlus processImage(ImagePlus image)
    {
        resultImage = null;
        String originalTitle = Local_Thickness_Driver.stripExtension(image.getTitle());
        if (originalTitle == null || originalTitle.isEmpty()) {
            originalTitle = DEFAULT_TITLE;
        }

        geometryToDistancePlugin.setup("", image);
        if (!showOptions) {
            // set options programmatically
            geometryToDistancePlugin.inverse = inverse;
            geometryToDistancePlugin.thresh = threshold;

            geometryToDistancePlugin.run(null);
        } else {
            // get options from EDT_S1D's dialog
            geometryToDistancePlugin.run(null);

            if (geometryToDistancePlugin.gotCancelled()) {
                resultImage = null;
                return getResultImage();
            }

            inverse = geometryToDistancePlugin.inverse;
            threshold = geometryToDistancePlugin.thresh;
        }
        resultImage = geometryToDistancePlugin.getResultImage();

        distanceRidgePlugin.setup("", resultImage);
        distanceRidgePlugin.run(null);
        resultImage = distanceRidgePlugin.getResultImage();

        localThicknessPlugin.setup("", resultImage);
        localThicknessPlugin.run(null);
        resultImage = localThicknessPlugin.getResultImage();

        thicknessCleaningPlugin.setup("", resultImage);
        thicknessCleaningPlugin.run(null);
        resultImage = thicknessCleaningPlugin.getResultImage();

        if (maskThicknessMask) {
            thicknessMask.inverse = inverse;
            thicknessMask.threshold = threshold;
            resultImage = thicknessMask.trimOverhang(image, resultImage);
        }

        resultImage.setTitle(originalTitle + titleSuffix);
        resultImage.copyScale(image);

        if (calibratePixels) {
            calibratePixels();
        }

        return getResultImage();
    }

    /**
     * Scales each pixels value with pixelWidth, and sets background pixels to Float.NaN. This done so that you can
     * calculate thickness statistics from the pixel values directly, for example the mean thickness of the map.
     * Also this way the user will see the pixel values as real units, e.g. mm.
     */
    private void calibratePixels()
    {
        pixelValuesToCalibratedValues();
        backgroundToNaN(0x00);
    }

    public ImagePlus getResultImage()
    {
        return resultImage;
    }

    @Override
    public void run(String s) {
        ImagePlus image = IJ.getImage();
        processImage(image);

        if (resultImage == null) {
            return;
        }

        resultImage.show();
        IJ.run("Fire"); // changes the color palette of the output image
    }

    /**
     * @param imageTitleSuffix  The suffix that's added to the end of the title of the resulting thickness map image
     * @post this.titleSuffix != null && !this.tittleSuffix.isEmpty
     */
    public void setTitleSuffix(String imageTitleSuffix) {
        titleSuffix = imageTitleSuffix;

        if (titleSuffix == null || titleSuffix.isEmpty()) {
            titleSuffix = DEFAULT_TITLE_SUFFIX;
        }
    }

    /**
     * Sets the value of the background pixels to Float.NaN
     *
     * @param backgroundColor   The color used to identify background pixels (usually 0.0f)
     */
    private void backgroundToNaN(float backgroundColor)
    {
        final int depth = resultImage.getNSlices();
        final int pixelsPerSlice = resultImage.getWidth() * resultImage.getHeight();
        final ImageStack stack = resultImage.getStack();

        for (int z = 1; z <= depth; z++) {
            float pixels[] = (float[]) stack.getPixels(z);
            for (int i = 0; i < pixelsPerSlice; i++) {
                if (Float.compare(pixels[i], backgroundColor) == 0) {
                    pixels[i] = Float.NaN;
                }
            }
        }
    }

    /**
     * Multiplies all pixel values of the given image by image.getCalibration().pixelWidth
     */
    private void pixelValuesToCalibratedValues()
    {
        double pixelWidth = resultImage.getCalibration().pixelWidth;
        ImageStack stack = resultImage.getStack();
        final int depth = stack.getSize();

        for (int z = 1; z <= depth; z++) {
            stack.getProcessor(z).multiply(pixelWidth);
        }
    }
}