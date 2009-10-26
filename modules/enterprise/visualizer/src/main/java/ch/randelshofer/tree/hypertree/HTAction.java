/*
 * HTAction.java
 *
 * www.bouthier.net
 * 2001
 */

package ch.randelshofer.tree.hypertree;

import java.awt.*;
import java.awt.event.*;


/**
 * The HTAction class manage the action on the hypertree :
 * drag of a node...
 */
public class HTAction
    extends MouseAdapter
    implements MouseMotionListener {

    private HTDraw   model      = null; // the drawing model

    private HTCoordE startPoint = null; // starting point of dragging
    private HTCoordE endPoint   = null; // ending point of dragging
    private HTCoordS clickPoint = null; // clicked point

  /* --- Constructor --- */

    /**
     * Constructor.
     *
     * @param model    the drawing model
     */
    HTAction(HTDraw model) {
        this.model = model;
        startPoint = new HTCoordE();
        endPoint = new HTCoordE();
        clickPoint = new HTCoordS();
    }


  /* --- MouseAdapter --- */

    /**
     * Called when a user pressed the mouse button
     * on the hyperbolic tree.
     * Used to get the starting point of the drag.
     *
     * @param e    the MouseEvent generated when clicking
     */
    public void mousePressed(MouseEvent e) {
        if (e.isShiftDown()) {
            model.fastMode(true);
        }
        startPoint.projectionStoE(e.getX(), e.getY(),
                                  model.getSOrigin(),
                                  model.getSMax());
    }

    /**
     * Called when a user release the mouse button
     * on the hyperbolic tree.
     * Used to signal the end of the translation.
     *
     * @param e    not used here
     */
    public void mouseReleased(MouseEvent e) {
        if (model.isAdjusting()) {
        model.setAdjusting(false);
        model.repaint();
        }

        model.fastMode(false);
        model.endTranslation();
    }



    /**
     * Called when a user clicked on the hyperbolic tree.
     * Used to put the corresponding node (if any) at the
     * center of the hyperbolic tree.
     *
     * @param e    the MouseEvent generated when clicking
     */
    public void mouseClicked(MouseEvent e) {
        if (e.isShiftDown()) {
            model.restore();
        } else {
            clickPoint.x = e.getX();
            clickPoint.y = e.getY();

            HTDrawNode node = model.findNode(clickPoint);
            if (node != null) {
                model.translateToOrigin(node);
            }
        }
    }


  /* --- MouseMotionListener --- */

    /**
     * Called when a used drag the mouse on the hyperbolic tree.
     * Used to translate the hypertree, thus moving the focus.
     *
     * @param e    the MouseEvent generated when draging
     */
    public void mouseDragged(MouseEvent e) {
        // BEGIN PATCH Performance
        model.setAdjusting(true);
        // END PATCH Performance

        if (startPoint.isValid()) {
            endPoint.projectionStoE(e.getX(), e.getY(),
                                    model.getSOrigin(),
                                    model.getSMax());
            if (endPoint.isValid()) {
                model.translate(startPoint, endPoint);
            }
        }
    }

    /**
     * Called when the mouse mouve into the hyperbolic tree.
     * Not used here.
     *
     * @param e    the MouseEvent generated when mouving
     */
    public void mouseMoved(MouseEvent e) {}

}

