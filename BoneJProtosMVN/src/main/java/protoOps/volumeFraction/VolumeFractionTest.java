package protoOps.volumeFraction;

import ij.ImagePlus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Richard Domander
 */
public class VolumeFractionTest {
    private static VolumeFraction volumeFraction;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setup() {
        volumeFraction = new VolumeFraction();
    }

    @Test
    public void testSetImageThrowsNullPointerExceptionIfImageIsNull() throws Exception {
        expectedException.expect(NullPointerException.class);
        expectedException.expectMessage("Must have an input image");

        volumeFraction.setImage(null);
    }

    @Test
    public void testSetImageThrowsIllegalArgumentExceptionIfBitDepthIsWrong() throws Exception {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("Input image bit depth must be 8 or 16");
        ImagePlus mockImage = mock(ImagePlus.class);
        when(mockImage.getBitDepth()).thenReturn(24);

        volumeFraction.setImage(mockImage);
    }

    @Test
    public void testSetVolumeAlgorithmThrowsIllegalArgumentExceptionIfAlgorithmIsInvalid() throws Exception {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("Invalid algorithm option");

        volumeFraction.setVolumeAlgorithm(-1);
    }

    @Test
    public void testSetSurfaceResamplingThrowsIllegalArgumentExceptionIfValueIsNegative() throws Exception {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("Resampling value must be >= 0");

        volumeFraction.setSurfaceResampling(-1);
    }
}