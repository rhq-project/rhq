/*
 * HTModelNodeComposite.java
 *
 * www.bouthier.net
 * 2001
 */

package ch.randelshofer.tree.hypertree;

import ch.randelshofer.tree.TreeNode;
import java.util.Iterator;
import java.util.ArrayList;


/**
 * The HTModelNodeComposite class implements the Composite design pattern
 * for HTModelNode.
 * It represents a HTModelNode which is not a leaf.
 */
public class HTModelNodeComposite
    extends HTModelNode {

    private ArrayList children     = null; // children of this node

    private double globalWeight = 0.0;  // sum of children weight


  /* --- Constructor --- */

    /**
     * Constructor for root node.
     *
     * @param node     the encapsulated TreeNode
     * @param model    the tree model using this HTModelNode
     */
    HTModelNodeComposite(TreeNode node, HTModel model) {
        this(node, null, model);
    }

    /**
     * Constructor.
     *
     * @param node      the encapsulated TreeNode
     * @param parent    the parent node
     * @param model     the tree model using this HTModelNode
     */
    HTModelNodeComposite(TreeNode node, HTModelNodeComposite parent, HTModel model) {
        super(node, parent, model);
        this.children = new ArrayList();

        for (TreeNode childNode : node.children()) {
        HTModelNode child;
            if (childNode.isLeaf()) {
                child = new HTModelNode(childNode, this, model);
            } else {
                child = new HTModelNodeComposite(childNode, this, model);
            }
            addChild(child);
        }

        // here the down of the tree is built, so we can compute the weight
        computeWeight();
    }


  /* --- Weight Managment --- */

    /**
     * Compute the Weight of this node.
     * As the weight is computed with the log
     * of the sum of child's weight, we must have all children
     * built before starting the computing.
     */
    private void computeWeight() {
        HTModelNode child = null;

        for (Iterator i = children(); i.hasNext(); ) {
            child = (HTModelNode) i.next();
            globalWeight += child.getWeight();
        }
        if (globalWeight != 0.0) {
            weight += Math.log(globalWeight);
        }
    }


  /* --- Tree management --- */

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
     * Adds the HTModelNode as a children.
     *
     * @param child    the child
     */
    void addChild(HTModelNode child) {
        children.add(child);
    }

    /**
     * Returns <CODE>false</CODE> as this node
     * is an instance of HTModelNodeComposite.
     *
     * @return    <CODE>false</CODE>
     */
    boolean isLeaf() {
        return false;
    }


  /* --- Hyperbolic layout --- */

    /**
     * Layout this node and its children in the hyperbolic space.
     * Mainly, divide the width angle between children and
     * put the children at the right angle.
     * Compute also an optimized length to the children.
     *
     * @param angle     the angle from the x axis (bold as love)
     * @param width     the angular width to divide, / 2
     * @param length    the parent-child length
     */
   void layout(double angle, double width, double length) {
        super.layout(angle, width, length);

        if (parent != null) {
            // Compute the new starting angle
            // e(i a) = T(z)oT(zp) (e(i angle))
            HTCoordE a = new HTCoordE(Math.cos(angle), Math.sin(angle));
            HTCoordE nz = new HTCoordE(- z.x, - z.y);
            a.translate(parent.getCoordinates());
            a.translate(nz);
            angle = a.arg();

            // Compute the new width
            // e(i w) = T(-length) (e(i width))
            // decomposed to do it faster :-)
            double c = Math.cos(width);
            double A = 1 + length * length;
            double B = 2 * length;
            width = Math.acos((A * c - B) / (A - B * c));
        }

        HTModelNode child = null;
        HTCoordE dump = new HTCoordE();

        int nbrChild = children.size();
        double l1 = (0.95 - model.getLength());
        double l2 = Math.cos((20.0 * Math.PI) / (2.0 * nbrChild + 38.0));
        length = model.getLength() + (l1 * l2);

        double startAngle = angle - width;

        // It may be interesting to sort children by weight instead
        for (Iterator i = children(); i.hasNext(); ) {
            child = (HTModelNode) i.next();

            double percent = child.getWeight() / globalWeight;
            double childWidth = width * percent;
            double childAngle = startAngle + childWidth;
            child.layout(childAngle, childWidth, length);
            startAngle += 2.0 * childWidth;
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
        HTModelNode child = null;
        result += "\n\tChildren :";
        for (Iterator i = children(); i.hasNext();) {
            child = (HTModelNode) i.next();
            result += "\n\t-> " + child.getName();
        }
        return result;
    }

}

