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
package org.rhq.enterprise.visualizer.gridview;

import ch.randelshofer.tree.TreeView;
import ch.randelshofer.tree.TreeNode;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.ContainerAdapter;
import java.awt.event.ContainerEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.util.concurrent.*;

import org.rhq.enterprise.visualizer.RandelshoferTreeNodeResource;
import org.rhq.enterprise.visualizer.Visualizer;

/**
 * @author Greg Hinkle
 */
public class GridView extends JPanel implements TreeView {

    RandelshoferTreeNodeResource rootNode;
    RandelshoferTreeNodeResource selectedNode;
    Runnable updateJob;


    public GridView(RandelshoferTreeNodeResource rootNode, GridView parent) {
        this.rootNode = rootNode;

        update();

        addMouseListener(new MouseHandler());

        if (this.rootNode.getId() == -1) {
            updateJob = new Runnable() {
                public void run() {
                    refresh();
                }
            };

            Visualizer.getExecutor().scheduleWithFixedDelay(updateJob,1,2, TimeUnit.SECONDS);

        }
    }

    @Override
    public void removeNotify() {
        if (updateJob != null) {
            Visualizer.getExecutor().remove(updateJob);
        }
        super.removeNotify();
    }

    public void update() {
        setBackground(Color.white);
        removeAll();
        if (selectedNode == null) {
            setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(5, 5, 2, 2), BorderFactory.createEtchedBorder()));

            int childCount = rootNode.getChildren().size();

            setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));

            for (RandelshoferTreeNodeResource child : rootNode.getChildren()) {
                ResourceTableTreeComponent c = new ResourceTableTreeComponent(child, this);
                add(c);
            }
        } else {
            setLayout(new BorderLayout());
            ResourceTableTreeComponent c = new ResourceTableTreeComponent(this.selectedNode, this);

            add(c, BorderLayout.NORTH);

            GridView childTableView = new GridView(this.selectedNode, this);
            add(childTableView, BorderLayout.CENTER);

            childTableView.addPropertyChangeListener("selection", new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                System.out.println("Selection is now: " + evt.getNewValue());
                RandelshoferTreeNodeResource node = (RandelshoferTreeNodeResource) evt.getNewValue();
                firePropertyChange("selection", evt.getOldValue(), evt.getNewValue());
            }
        });
        }
        revalidate();
        repaint();
    }

    public void setSelection(RandelshoferTreeNodeResource selected) {
        RandelshoferTreeNodeResource old = this.selectedNode;
        if (this.selectedNode == selected) {
            selected = null;
        }
        this.selectedNode = selected;
        update();
        firePropertyChange("selection",
                old, this.selectedNode);
    }


    public void refresh() {

        for (int i = 0; i < getComponentCount(); i++) {
            final Component c = getComponent(i);
            if (c instanceof ResourceTableTreeComponent) {


                Runnable updater = new Runnable() {
                    public void run() {
                        ((ResourceTableTreeComponent) c).refresh();
                    }
                };
                Future f = Visualizer.getExecutor().submit(updater);
                try {
                    f.get(2, TimeUnit.SECONDS);

                } catch (ExecutionException e) {
                } catch (TimeoutException e) {
                    f.cancel(true);
                    Visualizer.getExecutor().remove(updater);
                } catch (InterruptedException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }

            } if (c instanceof GridView) {
                ((GridView)c).refresh();
            }
        }
    }


    public void setSelection(TreeNode node) {
        this.selectedNode = (RandelshoferTreeNodeResource) node;
    }

    public Component getComponent() {
        return this;
    }


    public class MouseHandler extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent e) {
            Component c = getComponentAt(e.getPoint());
            RandelshoferTreeNodeResource clickedNode = null;
            if (c != null && c instanceof ResourceTableTreeComponent) {
                clickedNode = ((ResourceTableTreeComponent) c).getNode();

                if (e.isPopupTrigger()) {
                    if (clickedNode != null) {
                        Visualizer.getPopupMenuForResource(clickedNode).show(GridView.this, e.getX(), e.getY());
                    }

                } else {
                    if (clickedNode != selectedNode) {
                        selectedNode = clickedNode;
                        update();
                        repaint();
                    }
                }
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            Component c = getComponentAt(e.getPoint());
            RandelshoferTreeNodeResource clickedNode = null;
            if (c != null && c instanceof ResourceTableTreeComponent) {
                clickedNode = ((ResourceTableTreeComponent) c).getNode();

                if (e.isPopupTrigger()) {
                    if (clickedNode != null) {
                        Visualizer.getPopupMenuForResource(clickedNode).show(GridView.this, e.getX(), e.getY());
                    }

                }
            }
        }
    }

}
