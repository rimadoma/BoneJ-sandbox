package org.bonej.localThickness;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;

/**
 * An additional image processing step to LocalThickness,
 * which removes pixels from the thickness map, which are background
 * in the original image. This is to avoid volume dilation in the map.
 *
 * @author <a href="mailto:rdomander@rvc.ac.uk">Richard Domander</a>
 */
public class MaskThicknessMapWithOriginal
{
    private ImagePlus resultImage = null;

    /**
     * Pixels with colors < threshold are considered background
     */
    public int threshold = EDT_S1D.DEFAULT_THRESHOLD;

    /**
     * If true, inverts the threshold condition, i.e. color >= threshold is background
     */
    public boolean inverse = EDT_S1D.DEFAULT_INVERSE;

    /**
     * Creates a copy of the thicknessMap, where "overhanging" pixels have been removed.
     *
     * Pixel p{x,y} in the map is considered overhanging if the pixel q{x=p.x,y=p.y}
     * in the original image is background.
     *
     * @param   original        The original, unprocessed 8-bit binary image
     * @param   thicknessMap    The 32-bit thickness map image produced from the original
     * @return  A 32-bit thickness map image where the overhanging pixels have been set to 0
     *          Returns null if either of the input images is null, or has wrong bit depth
     *          Returns null if the dimensions of the images don't match
     *
     * Modified from org.doube.bonej.Thickness.trimOverhang created by Michael Doube
     */
    public ImagePlus trimOverhang(ImagePlus original, ImagePlus thicknessMap) {
        if (original == null || original.getBitDepth() != 8) {
            return null;
        }

        if (thicknessMap == null || thicknessMap.getBitDepth() != 32) {
            return null;
        }

        final int w = original.getWidth();
        final int h = original.getHeight();
        final int d = original.getImageStackSize();

        if (w != thicknessMap.getWidth() || h != thicknessMap.getHeight() || d != thicknessMap.getImageStackSize()) {
            return null;
        }

        final ImageStack originalStack = original.getImageStack();

        resultImage = thicknessMap.duplicate();
        resultImage.setTitle(thicknessMap.getTitle()); // duplicate() add DUP_ to title
        final ImageStack resultStack = resultImage.getImageStack();

        ImageProcessor originalProcessor;
        ImageProcessor resultProcessor;
        for (int z = 1; z <= d; z++) {
            IJ.showStatus("Masking thickness map...");
            IJ.showProgress(z, d);
            originalProcessor = originalStack.getProcessor(z);
            resultProcessor = resultStack.getProcessor(z);
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int color = originalProcessor.get(x, y);
                    if ((color < threshold && !inverse) || (color >= threshold && inverse)) {
                        resultProcessor.set(x, y, 0);
                    }
                }
            }
        }

        return getResultImage();
    }

    /**
     * @return  The result of the last call to trimOverhang.
     *          Returns null if the method hasn't been successfully called.
     */
    public ImagePlus getResultImage()
    {
        return resultImage;
    }

    public void resetResultImage()
    {
        resultImage = null;
    }
}
