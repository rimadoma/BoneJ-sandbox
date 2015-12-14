package org.bonej.geometry;

import javax.vecmath.Vector3d;

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
	public static double joinedVectorAngle(double ux, double uy, double uz, double vx, double vy, double vz,
			double tailX, double tailY, double tailZ) {
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
	 * @param u
	 *            A three dimensional vector
	 * @param v
	 *            Another three dimensional vector
	 * @param tail
	 *            The point where the tails of the vectors u & v meet
	 * @return Angle formed by u-tail-v
	 *
	 *         This method does not change the state of the vectors u, v and
	 *         tail.
	 */
	public static double joinedVectorAngle(Vector3d u, Vector3d v, Vector3d tail) {
		return joinedVectorAngle(u.x, u.y, u.z, v.x, v.y, v.z, tail.x, tail.y, tail.z);
	}
}
