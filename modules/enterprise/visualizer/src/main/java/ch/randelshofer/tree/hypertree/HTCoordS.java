/*
 * HTCoordS.java
 *
 * www.bouthier.net
 * 2001
 */

package ch.randelshofer.tree.hypertree;


/**
 * The HTCoordS class implements the coordinates of a point
 * in the Screen space.
 * As the screen space is represented with finite pixels,
 * we just use int instead of float or double.
 */
class HTCoordS {

    private static final int ZONE_LENGTH = 4; // size of the zone

    int x = 0; // x coord
    int y = 0; // y coord


  /* --- Constructor --- */

    /**
     * Constructor.
     * x = 0 and y = 0.
     */
    HTCoordS() {}

    /**
     * Constructor copying the given screen point.
     *
     * @param z    the screen point to copy
     */
    HTCoordS(HTCoordS z) {
        this.x = z.x;
        this.y = z.y;
    }

    /**
     * Constructor fixing x and y.
     *
     * @param x    the x coord
     * @param y    the y coord
     */
    HTCoordS(int x, int y) {
        this.x = x;
        this.y = y;
    }


  /* --- Projection --- */

    /**
     * Projects the given Euclidian point on the screen plane.
     *
     * @param ze        the euclidian point
     * @param sOrigin   the origin of the screen plane
     * @param sMax      the (xMax, yMax) point in the screen plane
     */
    void projectionEtoS(HTCoordE ze, HTCoordS sOrigin, HTCoordS sMax) {
        x = (int) Math.round(ze.x * sMax.x) + sOrigin.x;
        y = - (int) Math.round(ze.y * sMax.y) + sOrigin.y;
    }


  /* --- Zone containing --- */

    /**
     * Is the given HTCoordS within the zone length
     * of this HTCoordS ?
     *
     * @return    <CODE>true</CODE> if it is,
     *            <CODE>false</CODE> otherwise
     */
    boolean contains(HTCoordS zs) {
        int length = getDistance(zs);
        if (length <= ZONE_LENGTH) {
            return true;
        } else {
            return false;
        }
    }


  /* -- Distance --- */

    /**
     * Returns the distance between this point
     * and the given point.
     *
     * @param z    the given point
     * @return     the distance
     */
    int getDistance(HTCoordS z) {
        int d2 = (z.x - x) * (z.x - x) + (z.y - y) * (z.y - y);
        return (int) Math.round(Math.sqrt(d2));
    }


  /* --- ToString --- */

    /**
     * Returns a string representation of the object.
     *
     * @return    a String representation of the object
     */
    public String toString() {
        return "(" + x + " : " + y + ")S";
    }

}

