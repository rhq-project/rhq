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
package org.rhq.enterprise.gui.navigation.group;

import java.io.IOException;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletResponse;

import org.richfaces.component.UITree;
import org.richfaces.component.html.HtmlTree;
import org.richfaces.component.state.TreeState;
import org.richfaces.component.state.TreeStateAdvisor;
import org.richfaces.model.TreeRowKey;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.resource.group.composite.AutoGroupComposite;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.resource.cluster.ClusterKey;
import org.rhq.enterprise.server.resource.cluster.ClusterManagerLocal;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Greg Hinkle
 */
public class ResourceGroupTreeStateAdvisor implements TreeStateAdvisor {

    private Integer selectedId;
    private ClusterKey selectedClusterKey;
    private TreeState treeState = new TreeState();

    private ResourceGroup currentGroup;

    public TreeState getTreeState() {
        return treeState;
    }

    private ResourceGroup getCurrentGroup() {
        if (currentGroup == null || currentGroup.getId() != getSelectedGroupId()) {
            this.selectedId = getSelectedGroupId();
            ResourceGroupManagerLocal groupManager = LookupUtil.getResourceGroupManager();
            currentGroup = groupManager.getResourceGroupById(EnterpriseFacesContextUtility.getSubject(),
                this.selectedId, null);
            if (!currentGroup.isVisible()) {
                this.selectedClusterKey = ClusterKey.valueOf(currentGroup.getClusterKey());
            }
        }
        return currentGroup;
    }

    private int getSelectedGroupId() {
        String groupId = FacesContextUtility.getOptionalRequestParameter("groupId");
        return Integer.parseInt(groupId);
    }

    public void changeExpandListener(org.richfaces.event.NodeExpandedEvent e) {
        HtmlTree tree = (HtmlTree) e.getComponent();
        TreeState state = (TreeState) tree.getComponentState();

        //check if we're collapsing a parent of currently selected node.
        //if we do, change the focus to the parent
        if (state.getSelectedNode() != null) {
            boolean closingParent = false;

            TreeRowKey<?> key = (TreeRowKey<?>) tree.getRowKey();
            ResourceGroupTreeNode node = (ResourceGroupTreeNode) tree.getRowData(key);
            ResourceGroupTreeNode selectedNode = (ResourceGroupTreeNode) tree.getRowData(state.getSelectedNode());

            selectedNode = selectedNode.getParent();
            while (selectedNode != null) {
                if (node.equals(selectedNode)) {
                    closingParent = true;
                    break;
                }
                selectedNode = selectedNode.getParent();
            }

            if (closingParent) {
                state.setSelected(key);
                redirectTo(node); // node state irrelevant if redirect was successful
            }
        }
    }

    public Boolean adviseNodeOpened(UITree tree) {
        TreeRowKey<?> key = (TreeRowKey<?>) tree.getRowKey();

        if (key != null) {
            ResourceGroupTreeNode node = (ResourceGroupTreeNode) tree.getRowData(key);
            if (node.getParent() == null) {
                return true;
            }

            TreeState state = (TreeState) tree.getComponentState();
            TreeRowKey<?> selectedKey = state.getSelectedNode();
            if (selectedKey == null) {
                getCurrentGroup();

                if (preopen((ResourceGroupTreeNode) tree.getRowData(key), this.selectedClusterKey)) {
                    return true;
                }
            }

            return state.isExpanded(key);
        }
        return null;
    }

    private boolean preopen(ResourceGroupTreeNode resourceTreeNode, ClusterKey selectedClusterKey) {
        ResourceGroup currentGroup = getCurrentGroup();
        if (resourceTreeNode.getData() instanceof ClusterKey) {
            if (((ClusterKey) resourceTreeNode.getData()).equals(selectedClusterKey)) {
                return true;
            }
        } else if (resourceTreeNode.getData() instanceof AutoGroupComposite) {
            ClusterKey key = resourceTreeNode.getClusterKey();
            AutoGroupComposite ag = (AutoGroupComposite) resourceTreeNode.getData();
            if (key.equals(selectedClusterKey)) {
                return true;
            }
        } else if (resourceTreeNode.getData() instanceof ResourceGroup) {
            if (currentGroup.getId() == ((ResourceGroup) resourceTreeNode.getData()).getId()) {
                return true;
            }
        }

        for (ResourceGroupTreeNode child : resourceTreeNode.getChildren()) {
            if (preopen(child, selectedClusterKey)) {
                return true;
            }
        }

        return false;
    }

    public void nodeSelectListener(org.richfaces.event.NodeSelectedEvent e) {
        HtmlTree tree = (HtmlTree) e.getComponent();
        TreeState state = (TreeState) ((HtmlTree) tree).getComponentState();

        TreeRowKey<?> key = (TreeRowKey<?>) tree.getRowKey();
        ResourceGroupTreeNode node = (ResourceGroupTreeNode) tree.getRowData(key);

        if (node != null) {
            if (node.getData() instanceof AutoGroupComposite) {
                state.setSelected(e.getOldSelection());
            }
            if (redirectTo(node) == false) { // upon redirect failure, reselect previous node
                state.setSelected(e.getOldSelection());
            }
        }
    }

    public Boolean adviseNodeSelected(UITree tree) {

        ResourceGroup currentGroup = getCurrentGroup();
        ResourceGroupTreeNode node = (ResourceGroupTreeNode) tree.getRowData(tree.getRowKey());

        if (node.getData() instanceof ResourceGroup) {
            return (this.selectedId == ((ResourceGroup) node.getData()).getId());
        } else if (node.getData() instanceof ClusterKey) {
            ClusterKey key = (ClusterKey) node.getData();

            if (currentGroup.getClusterKey() != null && currentGroup.getClusterKey().equals(key.getKey())) {
                return true;
            }
        }

        return false;
    }

    /**
     * @return false if there was an error redirecting to the target location
     */
    private boolean redirectTo(ResourceGroupTreeNode node) {
        HttpServletResponse response = (HttpServletResponse) FacesContextUtility.getFacesContext().getExternalContext()
            .getResponse();

        Subject subject = EnterpriseFacesContextUtility.getSubject();

        String path = "";
        if (node.getData() instanceof ClusterKey) {

            ClusterManagerLocal clusterManager = LookupUtil.getClusterManager();
            ResourceGroup group = clusterManager.createAutoClusterBackingGroup(subject, (ClusterKey) node.getData(),
                true);

            path = "/rhq/group/inventory/view.xhtml";

            path += ("?groupId=" + group.getId() + "&parentGroupId=" + ((ClusterKey) node.getData())
                .getClusterGroupId());

        } else if (node.getData() instanceof AutoGroupComposite) {
            FacesContext.getCurrentInstance().addMessage("leftNavGroupTreeForm:leftNavGroupTree",
                new FacesMessage(FacesMessage.SEVERITY_WARN, "No cluster autogroup views available", null));

        } else if (node.getData() instanceof ResourceGroup) {
            path = "/rhq/group/inventory/view.xhtml";
            path += ("?groupId=" + ((ResourceGroup) node.getData()).getId());
        }

        try {
            response.sendRedirect(path);
            return true; // all is well in the land
        } catch (IOException ioe) {
            FacesContext.getCurrentInstance().addMessage(
                "leftNavGroupTreeForm:leftNavGroupTree",
                new FacesMessage(FacesMessage.SEVERITY_WARN, "Unable to browse to selected group view: "
                    + ioe.getMessage(), null));
        }
        return false; // IO error from redirect
    }
}
