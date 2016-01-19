package org.bonej.common;

import ij.ImagePlus;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.*;

/**
 * Richard Domander
 */
public class CommonTest {
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void testGetPixelsReturnsCorrectArrayType() throws Exception {
        Object array = Common.getEmptyPixels(1, 1, ImagePlus.GRAY8);
        assertTrue("Image type GRAY8 should return a byte[]", array instanceof byte[]);

        array = Common.getEmptyPixels(1, 1, ImagePlus.COLOR_256);
        assertTrue("Image type COLOR_256 should return a byte[]", array instanceof byte[]);

        array = Common.getEmptyPixels(1, 1, ImagePlus.GRAY16);
        assertTrue("Image type GRAY16 should return a short[]", array instanceof short[]);

        array = Common.getEmptyPixels(1, 1, ImagePlus.GRAY32);
        assertTrue("Image type GRAY32 should return a float[]", array instanceof float[]);

        array = Common.getEmptyPixels(1, 1, ImagePlus.COLOR_RGB);
        assertTrue("Image type COLOR_RGB should return a int[]", array instanceof int[]);
    }

    @Test
    public void testGetPixelsThrowsIllegalArgumentExceptionIfImageTypeIsUnrecognized() throws Exception {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("Invalid image type");

        Common.getEmptyPixels(1, 1, -1);
    }
}