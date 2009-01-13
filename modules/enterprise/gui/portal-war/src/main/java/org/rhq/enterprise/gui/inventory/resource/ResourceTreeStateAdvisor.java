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
package org.rhq.enterprise.gui.inventory.resource;


import java.io.IOException;
import java.util.Map;
import java.util.HashMap;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;
import javax.servlet.ServletResponse;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.richfaces.component.UITree;
import org.richfaces.component.html.HtmlTree;
import org.richfaces.component.state.TreeStateAdvisor;
import org.richfaces.component.state.TreeState;
import org.richfaces.model.TreeRowKey;
import org.ajax4jsf.model.DataVisitor;

import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.group.composite.AutoGroupComposite;
import org.rhq.core.gui.util.FacesContextUtility;


/**
 * Manages the tree selection and node openess for the left nav resource tree
 *
 * @author Greg Hinkle
 */
public class ResourceTreeStateAdvisor implements TreeStateAdvisor {

    private boolean altered = false;
    UITree tree;
    private TreeRowKey selectedKey;
    private int selectedId;

    public void changeExpandListener(org.richfaces.event.NodeExpandedEvent e) {
        altered = true;
        Object source = e.getSource();

        HtmlTree c = (HtmlTree) e.getComponent();

        TreeState state = (TreeState) ((HtmlTree) c).getComponentState();
        TreeRowKey key = (TreeRowKey) c.getRowKey();
    }

    public void nodeSelectListener(org.richfaces.event.NodeSelectedEvent e) {
        HtmlTree tree = (HtmlTree) e.getComponent();
        TreeState state = (TreeState) ((HtmlTree) tree).getComponentState();

        try {
            tree.queueNodeExpand((TreeRowKey) tree.getRowKey());
            ResourceTreeNode node = (ResourceTreeNode) tree.getRowData(tree.getRowKey());

            if (node != null && node.getData() instanceof Resource) {

            ServletContext context = (ServletContext) FacesContextUtility.getFacesContext().getExternalContext().getContext();
            String path = FacesContextUtility.getRequest().getRequestURI();
            HttpServletResponse response = (HttpServletResponse) FacesContextUtility.getFacesContext().getExternalContext().getResponse();
            response.sendRedirect(path + "?id=" + ((Resource)node.getData()).getId());
//            context.getRequestDispatcher(path + "?id=" + FacesContextUtility.getRequiredRequestParameter("id")).forward(
//                    FacesContextUtility.getRequest(),
//                    );
            }
        } catch (IOException e1) {
            e1.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }



    public Boolean adviseNodeOpened(UITree tree) {
        TreeRowKey key = (TreeRowKey) tree.getRowKey();

        if (key != null) {
            ResourceTreeNode treeNode = ((ResourceTreeNode) tree.getRowData(key));

            TreeState state = (TreeState) tree.getComponentState();


            TreeRowKey selectedKey = state.getSelectedNode();
            if (selectedKey == null) {
                selectedKey = this.selectedKey;
            }

            if (selectedKey == null) {
                String id = FacesContextUtility.getOptionalRequestParameter("id");
                this.selectedId = Integer.parseInt(id);

                 if (preopen((ResourceTreeNode) tree.getRowData(key), this.selectedId)) {
                    return true;
                 }
            }


            if (selectedKey != null && (selectedKey.isSubKey(key)))
                return Boolean.TRUE;


            if (altered) {
                if (state.isExpanded(key))
                    return Boolean.TRUE;
            } else {
                Object data = ((ResourceTreeNode) tree.getRowData(key)).getData();
                if (data instanceof Resource) {
                    if (((Resource) data).getResourceType().getCategory() != ResourceCategory.SERVICE)
                        return Boolean.TRUE;
                }
            }
        }
        return null;
    }

    private boolean preopen(ResourceTreeNode resourceTreeNode, int selectedId) {
        if (resourceTreeNode.getData() instanceof Resource) {
            if (((Resource)resourceTreeNode.getData()).getId() == selectedId) {
                return true;
            }
        }

        for (ResourceTreeNode child: resourceTreeNode.getChildren()) {
            if (preopen(child, selectedId)) {
                return true;
            }
        }

        return false;
    }

    public Boolean adviseNodeSelected(UITree tree) {
        TreeState state = (TreeState) ((HtmlTree)tree).getComponentState();
        String id = FacesContextUtility.getOptionalRequestParameter("id");
        ResourceTreeNode node = (ResourceTreeNode) tree.getRowData(tree.getRowKey());

        if (node.getData() instanceof Resource) {
            if (String.valueOf(((Resource)node.getData()).getId()).equals(id)) {
                return Boolean.TRUE;
            }
        }

        if (tree.getRowKey().equals(state.getSelectedNode())) {
            return Boolean.TRUE;
        }
        return null;
    }

}