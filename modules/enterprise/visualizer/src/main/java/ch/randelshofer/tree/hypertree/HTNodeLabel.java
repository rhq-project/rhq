/*
 * HTNodeLabel.java
 *
 * www.bouthier.net
 * 2001
 */

package ch.randelshofer.tree.hypertree;

import ch.randelshofer.tree.NodeInfo;
import java.awt.*;


/**
 * The HTNodeLabel class implements the drawed label
 * representing a node.
 */
public class HTNodeLabel {

    private HTDrawNode node   = null;  // represented node
    private int        x      = 0;     // x up-left corner
    private int        y      = 0;     // y up-left corner
    private int        width  = 0;     // width of the label
    private int        height = 0;     // height of the label
    private boolean    active = false; // should be drawed ?


    /* ---  Constructor --- */

    /**
     * Constructor.
     *
     * @param node    the represented node
     */
    HTNodeLabel(HTDrawNode node) {
        this.node = node;
    }


    /* --- Draw --- */

    /**
     * Draw this label, if there is enough space.
     *
     * @param g    the graphic context
     */
    void draw(Graphics g, NodeInfo info) {
        FontMetrics fm = g.getFontMetrics();
        int fh = fm.getHeight();
        int space = node.getSpace();
        // BEGIN PATCH Draw composites as roundrects
        boolean isLeaf = node.getHTModelNode().isLeaf();
        // END PATCH
        if (space >= fh) {

            active = true;
            HTCoordS zs = node.getScreenCoordinates();
            String name = info.getName(node.getHTModelNode().getDataNodePath());
            Color color = info.getColor(node.getHTModelNode().getDataNodePath());
            Image icon = info.getImage(node.getHTModelNode().getDataNodePath());
            char[] nameC = (name == null) ? new char[0] : name.toCharArray();
            int nameLength = nameC.length;
            int nameWidth = fm.charsWidth(nameC, 0, nameLength);

            while((nameWidth >= space) && (nameLength > 0)) {
                nameLength--;
                // BEGIN PATCH Shortened names end with ellipsis
                nameC[nameLength - 1] = '.';
                // END PATCH Shortened names end with ellipsis
                nameWidth = fm.charsWidth(nameC, 0, nameLength);
            }
            double weight = 0;
            height = fh + (int) (10*weight);
            width = nameWidth + 10 + (int) (10 * weight);
            x = zs.x - (width / 2) - (int) (5 * weight);
            y = zs.y - (fh / 2) - (int) (5 * weight);

            g.setColor(color);
            // BEGIN PATCH Draw composites as roundrects
            if (isLeaf) {
                g.fillRect(x, y, width, height);
            } else {
                g.fillRoundRect(x, y, width, height, height, height);
            }
            // END PATCH Draw composites as roundrects
            // draw image
            if (icon != null) {
                g.drawImage(icon, x, y, null);
            }
            /*
            //g.setColor(Color.black);

            if (g instanceof Graphics2D) {
                // only in a Java2D capable environment
                ((Graphics2D) g).setStroke
                        (new BasicStroke((long) node.getBorderSize()));
            }

            // BEGIN PATCH Draw composites as roundrects
            if (isLeaf) {
                g.drawRect(x, y, width, height);
            } else {
                g.drawRoundRect(x, y, width, height, height, height);
            }*/
            // END PATCH Draw composites as roundrects

            if (g instanceof Graphics2D) {
                // only in a Java2D capable environment
                ((Graphics2D) g).setStroke
                        (new BasicStroke((long) 1.0));
            }
            int sx = zs.x - (nameWidth / 2) - (int) (5 * weight);
            int sy = y + fm.getAscent() + (fm.getLeading() / 2) +
                    (int) (5 *weight);

                    g.setColor(Color.BLACK);
                    g.drawString(new String(nameC, 0, nameLength), sx, sy);
        } else {
            active = false;

            // BEGIN PATCH Draw a small circle for inactive nodes
            HTCoordS zs = node.getScreenCoordinates();
            Color color = info.getColor(node.getHTModelNode().getDataNodePath());
            double weight = 0;
           height = width = Math.max(4, (int) (10 * weight));
            width = height;
            x = zs.x - (width / 2);
            y = zs.y - (height / 2);
            g.setColor(color);
            if (isLeaf) {
                g.fillRect(x, y, width, height);
            } else {
                g.fillOval(x, y, width, height);
            }
            //g.setColor(Color.black);
            /*
            if (g instanceof Graphics2D) {
                // only in a Java2D capable environment
                ((Graphics2D) g).setStroke
                        (new BasicStroke((long) node.getBorderSize()));
            }

            if (isLeaf) {
                g.drawRect(x, y, width, height);
            } else {
                g.drawOval(x, y, width, height);
            }*/
            // END PATCH Draw a small circle for inactive nodes
        }
    }


    /* --- Zone containing --- */

    /**
     * Is the given HTCoordS within this label ?
     *
     * @return    <CODE>true</CODE> if it is,
     *            <CODE>false</CODE> otherwise
     */
    boolean contains(HTCoordS zs) {
        if (active) {
            if ((zs.x >= x) && (zs.x <= (x + width)) &&
                    (zs.y >= y) && (zs.y <= (y + height)) ) {
                return true;
            } else {
                return false;
            }
        } else {
            return node.getScreenCoordinates().contains(zs);
        }
    }


    /* --- ToString --- */

    /**
     * Returns a string representation of the object.
     *
     * @return    a String representation of the object
     */
    public String toString() {
        String result = "label of " + node.toString() +
                "\n\tx = " + x + " : y = " + y +
                "\n\tw = " + width + " : h = " + height;
        return result;
    }

}
