package org.bonej.geometry;

import javax.vecmath.Vector3d;

import static com.google.common.base.Preconditions.checkNotNull;

public class Vectors {
    /**
     * Calculate the angle between two vectors u,v joined at their tails at the
     * point (tailX, tailY, tailZ)
     *
     * @param ux
     *            x-coordinate of the head of vector u
     * @param uy
     *            y-coordinate of the head of vector u
     * @param uz
     *            z-coordinate of the head of vector u
     * @param vx
     *            x-coordinate of the head of vector v
     * @param vy
     *            y-coordinate of the head of vector v
     * @param vz
     *            z-coordinate of the head of vector v
     * @param tailX
     *            x-coordinate of the mutual tail point
     * @param tailY
     *            y-coordinate of the mutual tail point
     * @param tailZ
     *            z-coordinate of the mutual tail point
     * @return angle formed by u-tail-v
     */
    public static double joinedVectorAngle(final double ux, final double uy, final double uz, final double vx,
                                           final double vy, final double vz, final double tailX, final double tailY,
                                           final double tailZ) {
        Vector3d u = new Vector3d(ux, uy, uz);
        Vector3d v = new Vector3d(vx, vy, vz);
        Vector3d tail = new Vector3d(tailX, tailY, tailZ);
        u.sub(tail);
        v.sub(tail);
        return u.angle(v);
    }

    /**
     * Calculate the angle between two joined vectors u and v
     *
     * @implNote    This method does not change the state of the vectors u, v and tail.
     * @throws NullPointerException if any of the parameters is null
     * @param u     A three dimensional vector
     * @param v     A three dimensional vector
     * @param tail  The point where the tails of the vectors u & v meet
     * @return Angle formed by u-tail-v
     */
    public static double joinedVectorAngle(final Vector3d u, final Vector3d v, final Vector3d tail)
            throws NullPointerException {
        checkNotNull("Vector u is null", u);
        checkNotNull("Vector v is null", v);
        checkNotNull("Vector tail is null", tail);

        return joinedVectorAngle(u.x, u.y, u.z, v.x, v.y, v.z, tail.x, tail.y, tail.z);
    }
}
