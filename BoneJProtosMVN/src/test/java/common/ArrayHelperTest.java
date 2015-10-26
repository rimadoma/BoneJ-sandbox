package common;

import Jama.Matrix;
import geometry.Ellipsoid;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * @author Richard Domander
 * @date 26/10/15.
 */
public class ArrayHelperTest {

    @Test
    public void testRemoveEllipsoidNulls() {
        final int DIMENSIONS = 3;
        Matrix noRotation = Matrix.identity(DIMENSIONS, DIMENSIONS);
        Ellipsoid unitSphere = new Ellipsoid(1.0, 1.0, 1.0, 0.0, 0.0, 0.0, noRotation.getArray());
        Ellipsoid allValid[] = {unitSphere, unitSphere, unitSphere};
        Ellipsoid allBad[] = {null, null, null};
        Ellipsoid twoValid[] = {unitSphere, null, unitSphere};

        allValid = ArrayHelper.removeNulls(allValid);
        assertEquals(3, Arrays.stream(allValid).filter(e -> e != null).count());

        allBad = ArrayHelper.removeNulls(allBad);
        assertEquals(0, allBad.length);

        twoValid = ArrayHelper.removeNulls(twoValid);
        assertEquals(2, Arrays.stream(twoValid).filter(e -> e != null).count());
    }
}