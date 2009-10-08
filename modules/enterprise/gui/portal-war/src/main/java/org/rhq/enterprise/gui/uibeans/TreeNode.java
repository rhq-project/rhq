/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.enterprise.gui.uibeans;

import java.awt.Rectangle;
import java.io.Serializable;
import java.util.ArrayList;
import org.rhq.enterprise.gui.image.data.ITreeNode;

/**
 * Implementation of the tree node interface for rendering the navigation map.
 */
public class TreeNode implements ITreeNode, Serializable {
    /**
     * Constant representing no ctype.
     */
    public static final int NO_CTYPE = -1;

    private String desc;
    private String name;
    private ArrayList rectangles = new ArrayList(2); // we'll rarely, if ever, have more than 2
    private boolean selected;

    /**
     * Children that are one level up from this node.
     */
    protected ArrayList upChildren = new ArrayList();

    /**
     * Children that are one level down from this node.
     */
    protected ArrayList downChildren = new ArrayList();

    /**
     * Creates a new <code>TreeNode</code> instance.
     *
     * @param name the name
     * @param desc the description
     */
    public TreeNode(String name, String desc) {
        this.name = name;
        this.desc = desc;
    }

    /**
     * Get the name.
     *
     * @return the name of the node
     */
    public String getName() {
        return name;
    }

    /**
     * Set the name.
     *
     * @param name the name of the node
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get the description.
     *
     * @return the description of the node
     */
    public String getDescription() {
        return desc;
    }

    /**
     * Set the description.
     *
     * @param desc the description of the node
     */
    public void setDescription(String desc) {
        this.desc = desc;
    }

    /**
     * Get the rectangles of coordinates for the image map.
     *
     * @return the coordinate rectangles
     */
    public Rectangle[] getRectangles() {
        return (Rectangle[]) rectangles.toArray(new Rectangle[0]);
    }

    /**
     * Set the rectangle of coordinates for the image map.
     *
     * @param x  top left corner horizontal position
     * @param y  top left corner horizontal position
     * @param cx width
     * @param cy height
     */
    public void addRectangle(int x, int y, int cx, int cy) {
        rectangles.add(new Rectangle(x, y, cx, cy));
    }

    /**
     * Returns true if the node is selected, false otherwise.
     *
     * @return true or false
     */
    public boolean isSelected() {
        return selected;
    }

    /**
     * Set whether or not this node is selected.
     *
     * @param selected true or false
     */
    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    /**
     * Add a child one level above this node.
     *
     * @param child the child to add
     */
    public void addUpChild(ITreeNode child) {
        upChildren.add(child);
    }

    /**
     * Add several children one level above this node.
     *
     * @param children the children to add
     */
    public void addUpChildren(ITreeNode[] children) {
        for (int i = 0; i < children.length; ++i) {
            upChildren.add(children[i]);
        }
    }

    /**
     * Get the children one level up from this node.
     *
     * @return the children above this node
     */
    public ITreeNode[] getUpChildren() {
        return (ITreeNode[]) upChildren.toArray(new ITreeNode[0]);
    }

    /**
     * Return the number of children one level above this node.
     *
     * @return the number of children
     */
    public int getUpChildrenCount() {
        return upChildren.size();
    }

    /**
     * Returns true if this node has children one level above.
     *
     * @return true or false
     */
    public boolean hasUpChildren() {
        return (this.getUpChildrenCount() > 0);
    }

    /**
     * Add a child one level below this node.
     *
     * @param child the child to add
     */
    public void addDownChild(ITreeNode child) {
        downChildren.add(child);
    }

    /**
     * Add several children one level below this node.
     *
     * @param children the children to add
     */
    public void addDownChildren(ITreeNode[] children) {
        for (int i = 0; i < children.length; ++i) {
            downChildren.add(children[i]);
        }
    }

    /**
     * Replace down children
     *
     * @param children the children to replace
     */
    public void replaceDownChildren(ITreeNode[] children) {
        downChildren.clear();
        addDownChildren(children);
    }

    /**
     * Replace up children
     *
     * @param children the children to replace
     */
    public void replaceUpChildren(ITreeNode[] children) {
        upChildren.clear();
        addUpChildren(children);
    }

    /**
     * Get the children one level down from this node.
     *
     * @return the children above this node
     */
    public ITreeNode[] getDownChildren() {
        return (ITreeNode[]) downChildren.toArray(new ITreeNode[0]);
    }

    /**
     * Return the number of children one level below this node.
     *
     * @return the number of children
     */
    public int getDownChildrenCount() {
        return downChildren.size();
    }

    /**
     * Returns true if this node has children one level below.
     *
     * @return true or false
     */
    public boolean hasDownChildren() {
        return (this.getDownChildrenCount() > 0);
    }

    /**
     * Describe <code>clear</code> method here.
     */
    public void clear() {
        upChildren.clear();
        downChildren.clear();
    }

    /**
     * Clears the internal state of the node. The list of rectangles must be cleared at a minumum. This method is called
     * by the ResourceTree.reset() method.
     *
     * @see net.covalent.image.widget.ResourceTree#reset()
     */
    public void reset() {
        rectangles.clear();
    }

    /**
     * Determines whether two objects are equal.
     *
     * @return true or false
     */
    public boolean equals(Object o) {
        if (!(o instanceof TreeNode)) {
            return false;
        }

        TreeNode other = (TreeNode) o;
        if (other == null) // I'm not null but you are, good-bye
        {
            return false;
        }

        boolean otherHasDescription = other.getDescription() != null;
        boolean thisHasDescription = this.getDescription() != null;

        // both are null (or both are not)
        if (otherHasDescription != thisHasDescription) {
            return false;
        }

        if (otherHasDescription && thisHasDescription && !other.getDescription().equals(this.getDescription())) {
            return false;
        }

        if (other.getName().equals(this.getName())

        &&
        /* we already checked if the descriptions are equal */
        (other.isSelected() == this.isSelected()) && (other.hasDownChildren() == this.hasDownChildren())
            && (other.hasUpChildren() == this.hasUpChildren())) {
            // XXX compare children
            return true;
        }

        return false;
    }

    /**
     * Returns the hashcode of this object.
     *
     * @return hash code.
     */
    public int hashCode() {
        int result = 19;
        int pri = 13;
        result = (pri * result) + ((this.getName() != null) ? this.getName().length() : 0);
        result = (pri * result) + ((this.getDescription() != null) ? this.getDescription().length() : 0);
        Rectangle[] rects = this.getRectangles();
        if (rects != null) {
            for (int i = 0; i < rects.length; ++i) {
                result = (pri * result) + (int) rects[i].getX();
                result = (pri * result) + (int) rects[i].getY();
                result = (pri * result) + (int) rects[i].getWidth();
                result = (pri * result) + (int) rects[i].getLocation().getX();
                result = (pri * result) + (int) rects[i].getLocation().getY();
            }
        }

        return result;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(" name:").append(getName()).append(" description:").append(getDescription()).append(" isSel:")
            .append(isSelected());
        Rectangle[] rects = this.getRectangles();
        if (rects != null) {
            for (int i = 0; i < rects.length; ++i) {
                sb.append(" rect[").append(i).append("]:").append(rects[i]);
            }
        }

        return sb.toString();
    }
}

// EOF
