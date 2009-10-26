/*
 * HTDrawNode.java
 *
 * www.bouthier.net
 * 2001
 */

package ch.randelshofer.tree.hypertree;

import ch.randelshofer.tree.NodeInfo;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.FontMetrics;


/**
 * The HTDrawNode class contains the drawing coordinates of a HTModelNode
 * for the HTView.
 * It implements the Composite design pattern.
 */
class HTDrawNode {

    private   HTDraw              model    = null;  // drawing model
    private   HTModelNode         node     = null;  // encapsulated HTModelNode

    private   HTCoordE            ze       = null;  // current euclidian coordinates
    private   HTCoordE            oldZe    = null;  // old euclidian coordinates
    protected HTCoordS            zs       = null;  // current screen coordinates

    private   HTDrawNodeComposite father   = null;  // father of this node
    private   HTDrawNode          brother  = null;  // brother of this node

    private   HTNodeLabel         label    = null;  // label of the node

    protected boolean             fastMode = false; // fast mode


  /* --- Constructor --- */

    /**
     * Constructor.
     *
     * @param father    the father of this node
     * @param node      the encapsulated HTModelNode
     * @param model     the drawing model
     */
    HTDrawNode(HTDrawNodeComposite father, HTModelNode node, HTDraw model) {
        this.father = father;
        this.node = node;
        this.model = model;

        label = new HTNodeLabel(this);

        ze = new HTCoordE(node.getCoordinates());
        oldZe = new HTCoordE(ze);
        zs = new HTCoordS();

	// store this object in HTNode -> HTDrawNode mapping
	model.mapNode(node.getNode(), this);

	return;
    }


  /* --- Brother --- */

    /**
     * Sets the brother of this node.
     *
     * @param brother    the borther of this node
     */
    void setBrother(HTDrawNode brother) {
        this.brother = brother;
    }


    /**
     * Returns the encapsulated HTModelNode.
     *
     * @return    the encapsulated HTModelNode
     */
    HTModelNode getHTModelNode() {
        return node;
    }


  /* --- Coordinates --- */

    /**
     * Returns the current coordinates of this node.
     * WARNING : this is NOT a copy but the true object
     * (for performance).
     *
     * @return     the current coordinates
     */
    HTCoordE getCoordinates() {
        return ze;
    }

    HTCoordE getOldCoordinates() {
        return oldZe;
    }

    HTCoordS getScreenCoordinates() {
        return zs;
    }

    /**
     * Refresh the screen coordinates of this node.
     *
     * @param sOrigin   the origin of the screen plane
     * @param sMax      the (xMax, yMax) point in the screen plane
     */
    void refreshScreenCoordinates(HTCoordS sOrigin, HTCoordS sMax) {
        zs.projectionEtoS(ze, sOrigin, sMax);
    }


  /* --- Drawing --- */

    /**
     * Draws the branches from this node to
     * its children.
     * Overidden by HTDrawNodeComposite
     *
     * @param g    the graphic context
     */
    void drawBranches(Graphics g, NodeInfo info) {}

    /**
     * Draws this node.
     *
     * @param g    the graphic context
     */
    void drawNodes(Graphics g, NodeInfo info) {
        if (getSpace() > 1) {
        if (fastMode == false) {
            label.draw(g, info);
        }
        }
    }

    /**
     * Returns the minimal distance between this node
     * and his father and his brother.
     *
     * @return    the minimal distance
     */
    int getSpace() {
        int dF = -1;
        int dB = -1;

        if (father != null) {
            HTCoordS zF = father.getScreenCoordinates();
            dF = zs.getDistance(zF);
        }
        if (brother != null) {
		HTCoordS zB = brother.getScreenCoordinates();
		dB = zs.getDistance(zB);
        }

	// this means that the node is a standalone node
        if ((dF == -1) && (dB == -1)) {
            return Integer.MAX_VALUE;
        } else if (dF == -1) {
            return dB;
        } else if (dB == -1) {
            return dF;
        } else {
            return Math.min(dF, dB);
        }
    }
    // BEGIN PATCH Performance determine distance to father
    /**
     * Returns the distance between this node
     * and his father.
     *
     * @return    the minimal distance
     */
    int getFatherSpace() {
        int dF = -1;

        if (father != null) {
            HTCoordS zF = father.getScreenCoordinates();
            dF = zs.getDistance(zF);
        }

	// this means that the node is a standalone node
        if ((dF == -1)) {
            return Integer.MAX_VALUE;
            } else {
            return dF;
        }
    }
// END PATCH Performance determine distance to father

  /* --- Translation --- */

    /**
     * Translates this node by the given vector.
     *
     * @param t    the translation vector
     */
    void translate(HTCoordE t) {
        ze.translate(oldZe, t);
    }

    /**
     * Transform this node by the given transformation.
     *
     * @param t    the transformation
     */
    void transform(HTTransformation t) {
        ze.copy(oldZe);
        ze.transform(t);
    }

    /**
     * Ends the translation.
     */
    void endTranslation() {
        oldZe.copy(ze);
    }

    /**
     * Restores the hyperbolic tree to its origin.
     */
    void restore() {
        HTCoordE orig = node.getCoordinates();
        ze.x = orig.x;
        ze.y = orig.y;
        oldZe.copy(ze);
    }

    /**
     * Sets the fast mode, where nodes are no more drawed.
     *
     * @param mode    setting on or off.
     */
    void fastMode(boolean mode) {
        if (mode != fastMode) {
            fastMode = mode;
        }
    }


  /* --- Node searching --- */

    /**
     * Returns the node (if any) whose screen coordinates' zone
     * contains thoses given in parameters.
     *
     * @param zs    the given screen coordinate
     * @return      the searched HTDrawNode if found;
     *              <CODE>null</CODE> otherwise
     */
    HTDrawNode findNode(HTCoordS zs) {
        // BEGIN PATCH Performance: quickly return null in fast mode
        if (! fastMode && label.contains(zs)) {
        // END PATCH Performance: quickly return null in fast mode
            return this;
        } else {
            return null;
        }
    }


  /* --- ToString --- */

    /**
     * Returns a string representation of the object.
     *
     * @return    a String representation of the object
     */
    public String toString() {
        String result = node.getName() +
                        "\n\t" + ze +
                        "\n\t" + zs;
        return result;
    }
}

