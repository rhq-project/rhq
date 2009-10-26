/*
 * HTCoordE.java
 *
 * www.bouthier.net
 * 2001
 */

package ch.randelshofer.tree.hypertree;


/**
 * The HTCoordE class implements the coordinates of a point
 * in the Euclidian space.
 */
class HTCoordE {

    private static final double EPSILON = 1.0E-10; // epsilon

    double x = 0.0; // x coord
    double y = 0.0; // y coord


  /* --- Constructor --- */

    /**
     * Constructor.
     * x = 0 and y = 0.
     */
    HTCoordE() {}

    /**
     * Constructor copying the given euclidian point.
     *
     * @param z    the euclidian point to copy
     */
    HTCoordE(HTCoordE z) {
        this.copy(z);
    }

    /**
     * Constructor fixing x and y.
     *
     * @param x    the x coord
     * @param y    the y coord
     */
    HTCoordE(double x, double y) {
        this.x = x;
        this.y = y;
    }


  /* --- Copy --- */

    /**
     * Copy the given HTCoordE into this HTCoordE.
     *
     * @param z    the HTCoordE to copy
     */
    void copy(HTCoordE z) {
        this.x = z.x;
        this.y = z.y;
    }


  /* --- Projections --- */

    /**
     * Progects from Screen to Euclidian.
     *
     * @param x        the x screen coordinate
     * @param y        the y screen coordinate
     * @param sOrigin   the origin of the screen plane
     * @param sMax      the (xMax, yMax) point in the screen plane
     */
    void projectionStoE(int x, int y, HTCoordS sOrigin, HTCoordS sMax) {
        this.x = (double) (x - sOrigin.x) / (double) sMax.x;
        this.y = -((double) (y - sOrigin.y) / (double) sMax.y);
    }


  /* --- Validation --- */

    /**
     * Is this coordinate in the hyperbolic disc ?
     *
     * @return    <CODE>true</CODE> if this point is in;
     *            <CODE>false</CODE> otherwise
     */
    boolean isValid() {
        return (this.d2() < 1.0);
    }


  /* --- Transformation --- */

    /*
     * Some complex computing formula :
     *
     * arg(z)  = atan(y / x) if x > 0
     *         = atan(y / x) + Pi if x < 0
     *
     * d(z)    = Math.sqrt((z.x * z.x) + (z.y * z.y))
     *
     * conj(z) = | z.x
     *           | - z.y
     *
     * a * b   = | (a.x * b.x) - (a.y * b.y)
     *           | (a.x * b.y) + (a.y * b.x)
     *
     * a / b   = | ((a.x * b.x) + (a.y * b.y)) / d(b)
     *           | ((a.y * b.x) - (a.x * b.y)) / d(b)
     */


    /**
     * Multiply this coordinate by the given coordinate.
     *
     * @param z    the coord to multiply with
     */
    void multiply(HTCoordE z) {
        double tx = x;
        double ty = y;
        x = (tx * z.x) - (ty * z.y);
        y = (tx * z.y) + (ty * z.x);
    }

    /**
     * Divide this coordinate by the given coordinate.
     *
     * @param z    the coord to divide with
     */
    void divide(HTCoordE z) {
        double d = z.d2();
        double tx = x;
        double ty = y;
        x = ((tx * z.x) + (ty * z.y)) / d;
        y = ((ty * z.x) - (tx * z.y)) / d;
    }

    /**
     * Substracts the second coord to the first one
     * and put the result in this HTCoorE
     * (this = a - b).
     *
     * @param a    the first coord
     * @param b    the second coord
     */
    void sub(HTCoordE a, HTCoordE b) {
        x = a.x - b.x;
        y = a.y - b.y;
    }

    /**
     * Returns the angle between the x axis and the line
     * passing throught the origin O and this point.
     * The angle is given in radians.
     *
     * @return    the angle, in radians
     */
    double arg() {
        double a = Math.atan(y / x);
        if (x < 0) {
            a += Math.PI;
        } else if (y < 0) {
            a += 2 * Math.PI;
        }
        return a;
    }

    /**
     * Returns the square of the distance from the origin
     * to this point.
     *
     * @return    the square of the distance
     */
    double d2() {
        return (x * x) + (y * y);
    }

    /**
     * Returns the distance from the origin
     * to this point.
     *
     * @return    the distance
     */
    double d() {
        return Math.sqrt(d2());
    }

    /**
     * Returns the distance from this point
     * to the point given in parameter.
     *
     * @param p    the other point
     * @return     the distance between the 2 points
     */
    double d(HTCoordE p) {
        return Math.sqrt((p.x - x) * (p.x - x) + (p.y - y) * (p.y - y));
    }

    /**
     * Translate this Euclidian point
     * by the coordinates of the given Euclidian point.
     *
     * @param t    the translation coordinates
     */
    void translate(HTCoordE t) {
        // z = (z + t) / (1 + z * conj(t))

        // first the denominator
        double denX = (x * t.x) + (y * t.y) + 1;
        double denY = (y * t.x) - (x * t.y) ;
        double dd   = (denX * denX) + (denY * denY);

        // and the numerator
        double numX = x + t.x;
        double numY = y + t.y;

        // then the division (bell)
        x = ((numX * denX) + (numY * denY)) / dd;
        y = ((numY * denX) - (numX * denY)) / dd;
    }

    /**
     * Translate the given Euclidian point
     * by the coordinates of the given translation vector,
     * and put the results in this point.
     *
     * @param s    the source point
     * @param t    the translation vector
     */
    void translate(HTCoordE s, HTCoordE t) {
        this.copy(s);
        this.translate(t);
    }
    /**
     * Transform this node by the given transformation.
     *
     * @param t    the transformation
     */
    void transform(HTTransformation t) {

        HTCoordE z = new HTCoordE(this);
        multiply(t.O);
        x += t.P.x;
        y += t.P.y;

        HTCoordE d = new HTCoordE(t.P);
        d.y = - d.y;
        d.multiply(z);
        d.multiply(t.O);
        d.x += 1;

        divide(d);
    }

  /* --- ToString --- */

    /**
     * Returns a string representation of the object.
     *
     * @return    a String representation of the object
     */
    public String toString() {
        return "(" + x + " : " + y + ")E";
    }

}

