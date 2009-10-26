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
package org.rhq.enterprise.visualizer;

import org.jdesktop.swingx.JXHyperlink;
import org.jdesktop.swingx.action.LinkAction;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * @author Greg Hinkle
 */
public class SelectionPathPanel extends JPanel {

    RandelshoferTreeNodeResource selectedNode;

    public SelectionPathPanel() {
        //setPreferredSize(new Dimension(1,30));
        this.add(new JLabel(" "));
    }

    public SelectionPathPanel(RandelshoferTreeNodeResource selectedNode) {
        this.selectedNode = selectedNode;
    }

    public RandelshoferTreeNodeResource getSelectedNode() {
        return selectedNode;
    }

    public void setSelectedNode(final RandelshoferTreeNodeResource selectedNode) {
        final RandelshoferTreeNodeResource oldNode = this.selectedNode;
        this.selectedNode = selectedNode;

        this.removeAll();

        this.setLayout(new FlowLayout(FlowLayout.LEADING));

        List<RandelshoferTreeNodeResource> path = new ArrayList<RandelshoferTreeNodeResource>();

        RandelshoferTreeNodeResource node = selectedNode;
        while (node != null) {
            if (node.id > 0)
                path.add(node);
            node = node.parent;
        }


        for (int i = path.size() - 1; i >= 0; i--) {
            node = path.get(i);
            LinkAction linkAction = new LinkAction(node) {
                  public void actionPerformed(ActionEvent e) {
                      System.out.println("Selected " + this.getName());
                      SelectionPathPanel.this.firePropertyChange("selection", oldNode, selectedNode);
                  }
            };
            JXHyperlink hyperlink = new JXHyperlink(linkAction);
            hyperlink.setFont(hyperlink.getFont().deriveFont(Font.PLAIN));

            this.add(hyperlink);

            if (i > 0) {
                this.add(new JLabel(">"));
            }

        }

        doLayout();
        repaint();

    }





}
