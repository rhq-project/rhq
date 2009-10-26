/*
 * HTView.java
 * www.bouthier.net
 *
 * The MIT License :
 * -----------------
 * Copyright (c) 2001 Christophe Bouthier
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package ch.randelshofer.tree.hypertree;

import ch.randelshofer.tree.*;
import ch.randelshofer.tree.sunburst.SunburstNode;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;
import java.awt.image.*;
import javax.swing.*;

import org.rhq.enterprise.visualizer.RandelshoferTreeNodeResource;
import org.rhq.enterprise.visualizer.Visualizer;


/**
 * The SwingHTView class implements a view of the HyperTree for use in
 * a Swing based application.
 *
 * @author Christophe Bouthier [bouthier@loria.fr]
 *         Roman Kennke [roman@ontographics.com]
 * @version 1.0
 */
public class SwingHTView extends JPanel implements HTView {


    private HTModel model = null; // the tree model represented
    private HTDraw draw = null; // the drawing model
    private HTAction action = null; // action manager
    private boolean fastMode = false;
    private boolean longNameMode = false;
    private boolean circleMode = false;
    private boolean transNotCorrected = false;
    private boolean quadMode = true;

    private TreeNode selectedNode;


    private Image image = null;

    /* --- Constructor --- */

    /**
     * Constructor.
     *
     * @param model the tree model to view
     */
    public SwingHTView(HTModel model) {
        super(new BorderLayout());
        setPreferredSize(new Dimension(250, 250));

        setBackground(Color.white);

        this.model = model;
        draw = new HTDraw(model, this);
        action = new HTAction(draw);
        startMouseListening();

        addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                setSelectedNode(getNodeUnderTheMouse(e));
            }

            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    TreeNode node = getNodeUnderTheMouse(e);
                    if (node != null) {
                        Visualizer.getPopupMenuForResource((RandelshoferTreeNodeResource) node).show(SwingHTView.this, e.getX(), e.getY());
                    }
                }
            }

            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    TreeNode node = getNodeUnderTheMouse(e);
                    if (node != null) {
                        Visualizer.getPopupMenuForResource((RandelshoferTreeNodeResource) node).show(SwingHTView.this, e.getX(), e.getY());
                    }
                }
            }
        });

        // BEGIN PATCH Tooltip
        ToolTipManager.sharedInstance().registerComponent(this);
        // END PATCH Tooltip

    }

    public TreeNode getSelectedNode() {
        return selectedNode;
    }

    public void setSelection(TreeNode node) {
        setSelectedNode(node);
    }

    public Component getComponent() {
        return this;
    }

    public void setSelectedNode(TreeNode node) {
        if (selectedNode != node) {
            TreeNode oldNode = selectedNode;
            HTDrawNode drawNode = draw.findDrawNode(node);
            if (drawNode != null) {
                draw.translateToOrigin(drawNode);
            }
            selectedNode = node;
            firePropertyChange("selection", oldNode, selectedNode);
        }
    }

    /* --- DataNode finding --- */

    /**
     * Returns the node containing the mouse event.
     * <p/>
     * This will be a DataNode.
     *
     * @param event the mouse event on a node
     * @return the node containing this event;
     *         could be <CODE>null</CODE> if no node was found
     */
    public TreeNode getNodeUnderTheMouse(MouseEvent event) {
        int x = event.getX();
        int y = event.getY();

        HTDrawNode node = draw.findNode(new HTCoordS(x, y));
        if (node != null) {
            return node.getHTModelNode().getNode();
        } else {
            return null;
        }
    }

    /* --- Tooltip --- */

    /**
     * Returns the tooltip to be displayed.
     *
     * @param event the event triggering the tooltip
     * @return the String to be displayed
     */
    public String getToolTipText(MouseEvent event) {
        int x = event.getX();
        int y = event.getY();

        HTDrawNode node = draw.findNode(new HTCoordS(x, y));
        if (node != null) {
            return model.getInfo().getTooltip(node.getHTModelNode().getDataNodePath());
        } else {
            return null;
        }
    }

    /* --- Paint --- */
    private BufferedImage lazyBufferedImage;

    private boolean isThreaded = true;

    /**
     * Paint the component.
     *
     * @param g the graphic context
     */
    //PDA
    public void paintComponent(final Graphics g) {
        super.paintComponent(g);
        if (image != null) {
            g.drawImage(image, 0, 0, getWidth(), this.getHeight(), this);
        }

        if (g instanceof Graphics2D) {
            // BEGIN PATCH Switch antialiasing off during animation
            if (draw.isAdjusting() || draw.isAnimating()) {
                ((Graphics2D) g).setRenderingHint
                        (RenderingHints.KEY_ANTIALIASING,
                                RenderingHints.VALUE_ANTIALIAS_OFF);
            } else {
                ((Graphics2D) g).setRenderingHint
                        (RenderingHints.KEY_ANTIALIASING,
                                RenderingHints.VALUE_ANTIALIAS_ON);
            }
            // END PATCH Switch antialiasing off during animation
        }
        long start1 = System.currentTimeMillis();
        draw.refreshScreenCoordinates();
        long start2 = System.currentTimeMillis();
        if (!isThreaded) {
            draw.drawBranches(g);
            draw.drawNodes(g);
        } else {
            if (lazyBufferedImage == null ||
                    lazyBufferedImage.getWidth() != getWidth() ||
                    lazyBufferedImage.getHeight() != getHeight()) {
                if (lazyBufferedImage != null) {
                    lazyBufferedImage.flush();
                    lazyBufferedImage = null;
                }
                lazyBufferedImage = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
            }
            final BufferedImage i2 = lazyBufferedImage;

            Runnable r2 = new Runnable() {
                public void run() {
                    Graphics2D g2 = i2.createGraphics();
                    g2.setFont(getFont());
                    if (draw.isAdjusting() || draw.isAnimating()) {
                        g2.setRenderingHint
                                (RenderingHints.KEY_ANTIALIASING,
                                        RenderingHints.VALUE_ANTIALIAS_OFF);
                    } else {
                        g2.setRenderingHint
                                (RenderingHints.KEY_ANTIALIASING,
                                        RenderingHints.VALUE_ANTIALIAS_ON);
                    }
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC));
                    g2.setBackground(new Color(0, true));
                    g2.clearRect(0, 0, i2.getWidth(), i2.getHeight());
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
                    draw.drawNodes(g2);
                    g2.dispose();
                }
            };
            Thread t2 = new Thread(r2);
            t2.start();
            draw.drawBranches(g);
            try {
                t2.join();
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
            g.drawImage(i2, 0, 0, null);
            //i2.flush();
        }
        long end = System.currentTimeMillis();
        g.setColor(Color.black);
        //g.drawString(isThreaded+" "+(end-start1)+" nd="+HTDrawNode.drawCount,12,12);
    }

    /* --- Thread-safe locking --- */

    /**
     * Stops the listening of mouse events.
     */
    public void stopMouseListening() {
        this.removeMouseListener(action);
        this.removeMouseMotionListener(action);
    }

    /**
     * Starts the listening of mouse events.
     */
    public void startMouseListening() {
        this.addMouseListener(action);
        this.addMouseMotionListener(action);
    }


    public void translateToOrigin(TreeNode node) {
        HTDrawNode drawNode = draw.findDrawNode(node);
        draw.translateToOrigin(drawNode);
        return;
    }

    public void setImage(Image image) {
        this.image = image;
        return;
    }


}

