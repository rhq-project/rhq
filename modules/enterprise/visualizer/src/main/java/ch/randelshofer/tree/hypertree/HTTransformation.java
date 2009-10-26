/*
 * HTTransformation.java
 *
 * www.bouthier.net
 * 2001
 */

package ch.randelshofer.tree.hypertree;


/**
 * The HTTransformation class implements a isometrie transformation
 * in the hyperbolic space.
 */
public class HTTransformation {

    HTCoordE P = null; // translation vector
    HTCoordE O = null; // rotation vector


  /* --- Constructor --- */

    /**
     * Constructor.
     */
    HTTransformation() {
        P = new HTCoordE();
        O = new HTCoordE();
    }


  /* --- Composition of 2 translation --- */

    /**
     * Compose the 2 given vectors translations
     * into one given transformation.
     */
    void composition(HTCoordE first, HTCoordE second) {
        P.x = first.x + second.x;
        P.y = first.y + second.y;

        HTCoordE d = new HTCoordE(second);
        d.y = - d.y;
        d.multiply(first);
        d.x += 1;
        P.divide(d);

        O.x = first.x;
        O.y = - first.y;
        O.multiply(second);
        O.x += 1;
        O.divide(d);
    }


  /* --- ToString --- */

    /**
     * Returns a string representation of the object.
     *
     * @return    a String representation of the object
     */
    public String toString() {
        String result = "Transformation : " +
                        "\n\tP = " + P +
                        "\n\tO = " + O;
        return result;
    }

}

