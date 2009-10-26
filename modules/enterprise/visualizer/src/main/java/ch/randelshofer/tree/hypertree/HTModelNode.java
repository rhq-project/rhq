/*
 * HTModelNode.java
 *
 * www.bouthier.net
 * 2001
 */
package ch.randelshofer.tree.hypertree;

import ch.randelshofer.tree.*;
import java.util.LinkedList;
import java.util.List;

/**
 * The HTModelNode class implements encapsulation of a TreeNode
 * for the model.
 * It keeps the original euclidian coordinates of the data.
 * It implements the Composite design pattern.
 */
public class HTModelNode {
    protected HTModel model = null; // tree model
    protected HTModelNodeComposite parent = null; // parent data
    protected HTCoordE z = null; // Euclidian coordinates
    protected double weight = 1.0;  // part of space taken by this data
    private TreePath<TreeNode> dataNodePath;

    /* --- Constructor --- */
    /**
     * Constructor for root data.
     *
     * @param data     the encapsulated TreeNode
     * @param model    the tree model using this HTModelNode
     */
    HTModelNode(TreeNode node, HTModel model) {
        this(node, null, model);
    }

    /**
     * Constructor.
     *
     * @param data      the encapsulated TreeNode
     * @param parent    the parent data
     * @param model     the tree model using this HTModelNode
     */
    HTModelNode(TreeNode data, HTModelNodeComposite parent, HTModel model) {
        this.dataNodePath = (parent == null) ? new TreePath<TreeNode>(data) : parent.getDataNodePath().pathByAddingChild(data);
        this.parent = parent;
        this.model = model;
        model.incrementNumberOfNodes();

        z = new HTCoordE();
    }

    /**
     * Returns the encapsulated data.
     *
     * @return    the encapsulated data
     */
    TreeNode getNode() {
        return dataNodePath.getLastPathComponent();
    }

    public TreePath<TreeNode> getDataNodePath() {
        return dataNodePath;
    }

    /* --- Name --- */
    /**
     * Returns the name of this data.
     *
     * @return    the name of this data
     */
    String getName() {
        return model.getInfo().getName(getDataNodePath());
    }


    /* --- Weight Managment --- */
    /**
     * Returns the weight of this data.
     *
     * @return    the weight of this data
     */
    double getWeight() {
        return weight;
    }


    /* --- Tree management --- */
    /**
     * Returns the parent of this data.
     *
     * @return    the parent of this data
     */
    HTModelNodeComposite getParent() {
        return parent;
    }

    /**
     * Returns <CODE>true</CODE> if this data
     * is not an instance of HTModelNodeComposite.
     *
     * @return    <CODE>true</CODE>
     */
    boolean isLeaf() {
        return true;
    }


    /* --- Coordinates --- */
    /**
     * Returns the coordinates of this data.
     * Thoses are the original hyperbolic coordinates,
     * without any translations.
     * WARNING : this is NOT a copy but the true object
     * (for performance).
     *
     * @return    the original hyperbolic coordinates
     */
    HTCoordE getCoordinates() {
        return z;
    }


    /* --- Hyperbolic layout --- */
    /**
     * Layouts the nodes in the hyperbolic space.
     */
    void layoutHyperbolicTree() {
        this.layout(0.0, Math.PI, model.getLength());
    }

    /**
     * Layout this data in the hyperbolic space.
     * First set the point at the right distance,
     * then translate by father's coordinates.
     * Then, compute the right angle and the right width.
     *
     * @param angle     the angle from the x axis (bold as love)
     * @param width     the angular width to divide, / 2
     * @param length    the parent-child length
     */
    void layout(double angle, double width, double length) {
        // Nothing to do for the root data
        if (parent == null) {
            return;
        }

        HTCoordE zp = parent.getCoordinates();

        // We first start as if the parent was the origin.
        // We still are in the hyperbolic space.
        z.x = length * Math.cos(angle);
        z.y = length * Math.sin(angle);

        // Then translate by parent's coordinates
        z.translate(zp);
    }


    /* --- ToString --- */
    /**
     * Returns a string representation of the object.
     *
     * @return    a String representation of the object
     */
    public String toString() {
        String result = getName() +
                "\n\t" + z +
                "\n\tWeight = " + weight;
        return result;
    }
}

