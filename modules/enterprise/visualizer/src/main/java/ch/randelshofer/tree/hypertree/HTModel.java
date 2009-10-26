/*
 * HTModel.java
 *
 * www.bouthier.net
 * 2001
 */

package ch.randelshofer.tree.hypertree;

import ch.randelshofer.tree.TreeNode;
import ch.randelshofer.tree.NodeInfo;


/**
 * The HTModel class implements the model for the HyperTree.
 * It's a tree of HTModelNode and HTModelNodeComposite, each keeping the
 * initial layout of the tree in the Poincarre's Model.
 */
public class HTModel {

    private HTModelNode root   = null; // the root of the tree's model

    // BEGIN PATCH Increase density of diagram
   // private double      length = 0.3;  // distance between node and children
    private double      length = 0.3;  // distance between node and children
    // END PATCH Increase density of diagram
    private int         nodes  = 0;    // number of nodes

    private NodeInfo info;


  /* --- Constructor --- */

    /**
     * Constructor.
     *
     * @param root    the root of the real tree
     */
    HTModel(TreeNode root, NodeInfo info) {
        this.info = info;
        if (root.isLeaf()) {
            this.root = new HTModelNode(root, this);
        } else {
            this.root = new HTModelNodeComposite(root, this);
        }
        info.init(root);
        this.root.layoutHyperbolicTree();
    }

    public NodeInfo getInfo() {
        return info;
    }

  /* --- Accessor --- */

    /**
     * Returns the root of the tree model.
     *
     * @return    the root of the tree model
     */
    HTModelNode getRoot() {
        return root;
    }

    /**
     * Returns the distance between a node and its children
     * in the hyperbolic space.
     *
     * @return    the distance
     */
    double getLength() {
        return length;
    }


  /* --- Number of nodes --- */

    /**
     * Increments the number of nodes.
     */
    void incrementNumberOfNodes() {
        nodes++;
    }

    /**
     * Returns the number of nodes.
     *
     * @return    the number of nodes
     */
    int getNumberOfNodes() {
        return nodes;
    }

}

