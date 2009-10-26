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
package org.rhq.enterprise.visualizer.tableview;

import ch.randelshofer.tree.TreeView;
import ch.randelshofer.tree.TreeNode;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.ContainerAdapter;
import java.awt.event.ContainerEvent;

import org.rhq.enterprise.visualizer.RandelshoferTreeNodeResource;
import org.rhq.enterprise.visualizer.Visualizer;
import org.rhq.enterprise.visualizer.gridview.ResourceTableTreeComponent;
import org.rhq.enterprise.visualizer.gridview.GridView;
import org.jdesktop.swingx.JXTable;

/**
 * @author Greg Hinkle
 */
public class TableView extends JPanel implements TreeView {

    RandelshoferTreeNodeResource rootNode;
    RandelshoferTreeNodeResource selectedNode;


    public TableView(RandelshoferTreeNodeResource rootNode) {
        this.rootNode = rootNode;

        update();

        addMouseListener(new MouseHandler());

        if (this.rootNode.getId() == -1) {
            final Thread t = new Thread(new Runnable() {
                public void run() {
                    while (true) {
                        try {
                            Thread.currentThread().sleep(5000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                        }
                        refresh();
                    }
                }
            });
            t.start();


            addContainerListener(new ContainerAdapter() {
                @Override
                public void componentRemoved(ContainerEvent e) {
                    t.stop();
                }
            });
        }

    }

    public void update() {
        removeAll();

        setLayout(new BorderLayout());

        JXTable table = new JXTable(new TableModel());

        add(table, BorderLayout.CENTER);

    }


    public void refresh() {
        for (int i = 0; i < getComponentCount(); i++) {
            Component c = getComponent(i);
            if (c instanceof ResourceTableTreeComponent) {
                ((ResourceTableTreeComponent) c).refresh();
            }
        }
    }


    public void setSelection(TreeNode node) {
        this.selectedNode = (RandelshoferTreeNodeResource) node;
    }

    public Component getComponent() {
        return this;
    }



    public class TableModel extends AbstractTableModel {
        public String[] columns = { "Name", "Memory", "CPU" };

        public int getRowCount() {

            return rootNode.getChildren().size();
        }

        @Override
        public String getColumnName(int column) {
            return columns[column];
        }

        public int getColumnCount() {
            return columns.length;
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            Object val = null;
            switch (columnIndex) {
                case 0:
                    val = rootNode.getChildren().get(rowIndex).getName();
                    break;

            }
            return val;

        }
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
                        Visualizer.getPopupMenuForResource(clickedNode).show(TableView.this, e.getX(), e.getY());
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
                        Visualizer.getPopupMenuForResource(clickedNode).show(TableView.this, e.getX(), e.getY());
                    }

                }
            }
        }
    }

}