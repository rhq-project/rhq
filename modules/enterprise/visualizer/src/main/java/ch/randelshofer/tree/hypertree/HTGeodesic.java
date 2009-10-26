/*
 * HTGeodesic.java
 *
 * www.bouthier.net
 * 2001
 */

package ch.randelshofer.tree.hypertree;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.QuadCurve2D;

/**
 * The HTGeodesic class implements a geodesic
 * linking to points in the Poincarre model.
 */
class HTGeodesic {

    private static final double EPSILON = 1.0E-10; // epsilon

    private static final int    LINE    = 0;       // draw a line
    private static final int    ARC     = 1;       // draw an arc

    private int      type = LINE; // type of the geodesic

    private HTCoordE za   = null; // first point (Euclidian)
    private HTCoordE zb   = null; // second point (Euclidian)
    private HTCoordE zc   = null; // control point (Euclidian)
    private HTCoordE zo   = null; // center of the geodesic;

    private HTCoordS a    = null; // first point (on the screen)
    private HTCoordS b    = null; // second point (on the screen)
    private HTCoordS c    = null; // control point (on the screen)


  /* --- Constructor --- */

    /**
     * Constructor.
     *
     * @param za       the first point
     * @param zb       the second point
     */
    HTGeodesic(HTCoordE za, HTCoordE zb) {
        this.za    = za;
        this.zb    = zb;

        zc = new HTCoordE();
        zo = new HTCoordE();

        a = new HTCoordS();
        b = new HTCoordS();
        c = new HTCoordS();

        rebuild();
    }


  /* --- Refresh --- */

    /**
     * Refresh the screen coordinates of this node.
     *
     * @param sOrigin   the origin of the screen plane
     * @param sMax      the (xMax, yMax) point in the screen plane
     */
    void refreshScreenCoordinates(HTCoordS sOrigin, HTCoordS sMax) {
        a.projectionEtoS(za, sOrigin, sMax);
        b.projectionEtoS(zb, sOrigin, sMax);
        c.projectionEtoS(zc, sOrigin, sMax);
    }


  /* --- Rebuild --- */

    /**
     * Builds the geodesic.
     */
    void rebuild() {
        if ( (Math.abs(za.d()) < EPSILON) ||                       // za == origin
             (Math.abs(zb.d()) < EPSILON) ||                       // zb == origin
             (Math.abs((za.x / zb.x) - (za.y / zb.y)) < EPSILON) ) // za = lambda.zb
        {
            type = LINE;
        } else {
            type = ARC;

            double da = 1 + za.x * za.x + za.y * za.y;
            double db = 1 + zb.x * zb.x + zb.y * zb.y;
            double dd = 2 * (za.x * zb.y - zb.x * za.y);

            zo.x = (zb.y * da - za.y * db) / dd;
            zo.y = (za.x * db - zb.x * da) / dd;

            double det = (zb.x - zo.x) * (za.y - zo.y) - (za.x - zo.x) * (zb.y - zo.y);
            double fa  = za.y * (za.y - zo.y) - za.x * (zo.x - za.x);
            double fb  = zb.y * (zb.y - zo.y) - zb.x * (zo.x - zb.x);

            zc.x = ((za.y - zo.y) * fb - (zb.y - zo.y) * fa) / det;
            zc.y = ((zo.x - za.x) * fb - (zo.x - zb.x) * fa) / det;
        }
    }


  /* --- Draw --- */

    /**
     * Draws this geodesic.
     *
     * @param g    the graphic context
     */
    void draw(Graphics g) {
        if (g instanceof Graphics2D) {
            Graphics2D g2 = (Graphics2D) g;
            //g2.setColor(Color.black);
            switch(type) {
            case LINE:
                g2.drawLine(a.x, a.y, b.x, b.y);
                break;
            case ARC:
                // BEGIN PATCH drawLine performs better for large trees
                //g2.drawLine(a.x, a.y, b.x, b.y);
                g2.draw(new QuadCurve2D.Double(a.x, a.y, c.x, c.y, b.x, b.y));
                // END PATCH drawLine performs better for large trees
                break;
            default:
                break;
            }
         } else {
	     System.err.println("only Graphics available");
	     //g.setColor(Color.black);
	     //switch(type) {
	     //case LINE:
                g.drawLine(a.x, a.y, b.x, b.y);
		//    break;

	    //
	    //case ARC:
            //    g.drawArc(a.x, a.y, c.x, c.y, b.x, b.y);
            //    break;
	    //default:
            //    break;
	    //}
         }
    }


 /* --- ToString --- */

    /**
     * Returns a string representation of the object.
     *
     * @return    a String representation of the object
     */
    public String toString() {
        String result = "Geodesic betweens : " +
                        "\n\t A: " + za +
                        "\n\t B: " + zb +
                        "\n\t is ";
        switch(type) {
        case LINE:
            result += "a line.";
            break;
        case ARC:
            result += "an arc.";
            break;
        default:
            result += "nothing ?";
            break;
        }
        return result;
    }

}

