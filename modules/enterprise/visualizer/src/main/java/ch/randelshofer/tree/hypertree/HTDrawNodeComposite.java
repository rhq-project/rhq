/*
 * HTDrawNodeComposite.java
 *
 * www.bouthier.net
 * 2001
 */

package ch.randelshofer.tree.hypertree;

import ch.randelshofer.tree.NodeInfo;
import java.awt.*;
import java.util.Iterator;
import java.util.HashMap;
import java.util.ArrayList;


/**
 * The HTDrawNodeComposite class implements the Composite design pattern
 * for HTDrawNode.
 * It represents a HTDrawNode which is not a leaf.
 */
public class HTDrawNodeComposite
        extends HTDrawNode {

    private HTModelNodeComposite node      = null; // encapsulated HTModelNode
    private ArrayList              children  = null; // children of this node
    private HashMap            geodesics = null; // geodesics linking the children


    /* --- Constructor --- */

    /**
     * Constructor.
     *
     * @param father    the father of this node
     * @param node      the encapsulated HTModelNode
     * @param model     the drawing model
     */
    HTDrawNodeComposite(HTDrawNodeComposite father,
            HTModelNodeComposite node, HTDraw model) {
        super(father, node, model);
        this.node = node;
        this.children = new ArrayList();
        this.geodesics = new HashMap();

        HTModelNode childNode = null;
        HTDrawNode child = null;
        HTDrawNode brother = null;
        boolean first = true;
        boolean second = false;
        for (Iterator i = node.children(); i.hasNext(); ) {
            childNode = (HTModelNode) i.next();
            if (childNode.isLeaf()) {
                child = new HTDrawNode(this, childNode, model);
            } else {
                child = new HTDrawNodeComposite(this, (HTModelNodeComposite) childNode, model);
            }
            addChild(child);
            if (first) {
                brother = child;
                first = false;
                second = true;
            } else if (second) {
                child.setBrother(brother);
                brother.setBrother(child);
                brother = child;
                second = false;
            } else {
                child.setBrother(brother);
                brother = child;
            }
        }
    }


    /* --- Children --- */

    /**
     * Returns the children of this node,
     * in an Enumeration.
     *
     * @return    the children of this node
     */
    Iterator children() {
        return this.children.iterator();
    }

    /**
     * Adds the HTDrawNode as a children.
     *
     * @param child    the child
     */
    void addChild(HTDrawNode child) {
        children.add(child);
        geodesics.put(child, new HTGeodesic(getCoordinates(), child.getCoordinates()));
    }


    /* --- Screen Coordinates --- */

    /**
     * Refresh the screen coordinates of this node
     * and recurse on children.
     *
     * @param sOrigin   the origin of the screen plane
     * @param sMax      the (xMax, yMax) point in the screen plane
     */
    void refreshScreenCoordinates(HTCoordS sOrigin, HTCoordS sMax) {
        super.refreshScreenCoordinates(sOrigin, sMax);
        HTDrawNode child = null;

        for (Iterator i = children(); i.hasNext(); ) {
            child = (HTDrawNode) i.next();
            child.refreshScreenCoordinates(sOrigin, sMax);
            HTGeodesic geod = (HTGeodesic) geodesics.get(child);
            if (geod != null) {
                geod.refreshScreenCoordinates(sOrigin, sMax);
            }

        }
    }


    /* --- Drawing --- */

    /**
     * Draws the branches from this node to
     * its children.
     *
     * @param g    the graphic context
     */
    void drawBranches(Graphics g, NodeInfo info) {
        HTDrawNode child = null;

        for (Iterator i = children(); i.hasNext(); ) {
            child = (HTDrawNode) i.next();

            if (child.getFatherSpace() > 1) {
                HTGeodesic geod = (HTGeodesic) geodesics.get(child);
                if (geod != null) {
                    g.setColor(info.getColor(child.getHTModelNode().getDataNodePath()).darker());
                    //((Graphics2D) g).setStroke(new BasicStroke(Math.max(1, (float) (6 * child.getWeight()))));
                    geod.draw(g);
                }
            }
            // BEGIN PATCH Performance Don't propagate in fast mode
            if (! fastMode) {
                child.drawBranches(g, info);
            }
            // END PATCH Performance Don't propagate in fast mode
        }
    }

    /**
     * Draws this node.
     *
     * @param g    the graphic context
     */
    void drawNodes(Graphics g, NodeInfo info) {
        if (fastMode == false) {
            super.drawNodes(g, info);

            HTDrawNode child = null;
            for (Iterator i = children(); i.hasNext(); ) {
                child = (HTDrawNode) i.next();
                child.drawNodes(g, info);
            }
        }
    }

    /**
     * Returns the minimal distance between this node
     * and his father, his brother, and his children.
     *
     * @return    the minimal distance
     */
    int getSpace() {
        int space = super.getSpace();

        if (! children.isEmpty()) {
            HTDrawNode child = (HTDrawNode) children.get(0);
            HTCoordS zC = child.getScreenCoordinates();
            int dC = zs.getDistance(zC);

            if (space == -1) {
                return dC;
            } else {
                return Math.min(space, dC);
            }
        } else {
            return space;
        }
    }


    /* --- Translation --- */

    /**
     * Translates this node by the given vector.
     *
     * @param t    the translation vector
     */
    void translate(HTCoordE t) {
        super.translate(t);

        // BEGIN PATCH Performance: Don't propagate when in fastMode
        if (! fastMode) {
            // END PATCH Performance: Don't propagate when in fastMode
            HTDrawNode child = null;
            for (Iterator i = children(); i.hasNext(); ) {
                child = (HTDrawNode) i.next();
                child.translate(t);
                HTGeodesic geod = (HTGeodesic) geodesics.get(child);
                if (geod != null) {
                    geod.rebuild();
                }
            }
            // BEGIN PATCH Performance: Don't propagate when in fastMode
        }
        // END PATCH Performance: Don't propagate when in fastMode

    }

    /**
     * Transform this node by the given transformation.
     *
     * @param t    the transformation
     */
    void transform(HTTransformation t) {
        super.transform(t);

        // BEGIN PATCH Performance: Don't propagate when in fastMode
        if (! fastMode) {
            // END PATCH Performance: Don't propagate when in fastMode
            HTDrawNode child = null;
            for (Iterator i = children(); i.hasNext(); ) {
                child = (HTDrawNode) i.next();
                child.transform(t);
                HTGeodesic geod = (HTGeodesic) geodesics.get(child);
                if (geod != null) {
                    geod.rebuild();
                }
            }
            // BEGIN PATCH Performance: Don't propagate when in fastMode
        }
        // END PATCH Performance: Don't propagate when in fastMode
    }

    /**
     * Ends the translation.
     */
    void endTranslation() {
        super.endTranslation();

        // BEGIN PATCH Performance: Don't propagate when in fastMode
        if (! fastMode) {
            // END PATCH Performance: Don't propagate when in fastMode
            HTDrawNode child = null;
            for (Iterator i = children(); i.hasNext(); ) {
                child = (HTDrawNode) i.next();
                child.endTranslation();
            }
            // BEGIN PATCH Performance: Don't propagate when in fastMode
        }
        // END PATCH Performance: Don't propagate when in fastMode
    }

    /**
     * Restores the hyperbolic tree to its origin.
     */
    void restore() {
        super.restore();

        HTDrawNode child = null;
        for (Iterator i = children(); i.hasNext(); ) {
            child = (HTDrawNode) i.next();
            child.restore();
            HTGeodesic geod = (HTGeodesic) geodesics.get(child);
            if (geod != null) {
                geod.rebuild();
            }
        }

    }

    /**
     * Sets the fast mode, where nodes are no more drawed.
     *
     * @param mode    setting on or off.
     */
    void fastMode(boolean mode) {
        super.fastMode(mode);
        // BEGIN PATCH Performance - We don't need to propagate 'true'
        if (! mode) {
            HTDrawNode child = null;
            for (Iterator i = children(); i.hasNext(); ) {
                child = (HTDrawNode) i.next();
                child.fastMode(mode);
            }
        }
        // BEGIN PATCH Performance - We don't need to propagate 'true'
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
        HTDrawNode result = super.findNode(zs);
        if (result != null) {
            return result;
        } else {
            HTDrawNode child = null;
            // BEGIN PATCH Performance don't propagate in fast mode
            if (! fastMode) {
                // END PATCH Performance don't propagate in fast mode
                // BEGIN PATCH Usability nodes must be found from front to back
                for (int i=children.size() - 1; i >= 0; i--) {
                    //for (Iterator i = children(); i.hasNext(); ) {
                    //     child = (HTDrawNode) i.next();
                    child = (HTDrawNode) children.get(i);
                    // END PATCH Usability nodes must be found from front to back
                    result = child.findNode(zs);
                    if (result != null) {
                        return result;
                    }
                }
                // BEGIN PATCH Performance don't propagate in fast mode
            }
            // END PATCH Performance don't propagate in fast mode
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
        String result = super.toString();
        HTDrawNode child = null;
        result += "\n\tChildren :";
        for (Iterator i = children(); i.hasNext(); ) {
            child = (HTDrawNode) i.next();
            result += "\n\t-> " + child.toString();
        }
        return result;
    }

}

