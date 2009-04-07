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

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;
import javax.faces.context.FacesContext;
import javax.faces.application.FacesMessage;

import org.richfaces.component.state.TreeStateAdvisor;
import org.richfaces.component.state.TreeState;
import org.richfaces.component.UITree;
import org.richfaces.component.html.HtmlTree;
import org.richfaces.model.TreeRowKey;

import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.core.domain.resource.group.composite.AutoGroupComposite;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.auth.Subject;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.gui.inventory.resource.ResourceUIBean;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;
import org.rhq.enterprise.server.resource.cluster.ClusterKey;
import org.rhq.enterprise.server.resource.cluster.ClusterManagerLocal;

/**
 * @author Greg Hinkle
 */
public class ResourceGroupTreeStateAdvisor implements TreeStateAdvisor {

    private Integer selectedId;
    private TreeRowKey selectedKey;
    private ClusterKey selectedClusterKey;


    private ResourceGroup currentGroup;

    public ResourceGroup getCurrentGroup() {
        if (currentGroup == null || currentGroup.getId() != getSelectedGroupId()) {
            this.selectedId = getSelectedGroupId();
            ResourceGroupManagerLocal groupManager = LookupUtil.getResourceGroupManager();
            currentGroup = groupManager.getResourceGroupById(EnterpriseFacesContextUtility.getSubject(), this.selectedId, null);
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

    public Boolean adviseNodeOpened(UITree tree) {
        TreeRowKey key = (TreeRowKey) tree.getRowKey();

        if (key != null) {

            TreeState state = (TreeState) tree.getComponentState();
            TreeRowKey selectedKey = state.getSelectedNode();

            getCurrentGroup();

            if (preopen((ResourceGroupTreeNode) tree.getRowData(key), this.selectedId)) {
                return true;
            }
        }
        return null;
    }

    private boolean preopen(ResourceGroupTreeNode resourceTreeNode, int selectedGroupId) {
        ResourceGroup currentGroup = getCurrentGroup();
        if (resourceTreeNode.getData() instanceof ClusterKey) {
            if (((ClusterKey) resourceTreeNode.getData()).equals(this.selectedClusterKey)) {
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
            if (preopen(child, selectedGroupId)) {
                return true;
            }
        }

        return false;
    }

    public void nodeSelectListener(org.richfaces.event.NodeSelectedEvent e) {
        HtmlTree tree = (HtmlTree) e.getComponent();
        TreeState state = (TreeState) ((HtmlTree) tree).getComponentState();

        try {
            tree.queueNodeExpand((TreeRowKey) tree.getRowKey());
            ResourceGroupTreeNode node = (ResourceGroupTreeNode) tree.getRowData(tree.getRowKey());

            if (node != null) {
                ServletContext context = (ServletContext) FacesContextUtility.getFacesContext().getExternalContext()
                        .getContext();
                HttpServletResponse response = (HttpServletResponse) FacesContextUtility.getFacesContext()
                        .getExternalContext().getResponse();

                Subject subject = EnterpriseFacesContextUtility.getSubject();

                if (node.getData() instanceof ClusterKey) {

                    ClusterManagerLocal clusterManager = LookupUtil.getClusterManager();
                    ResourceGroup group = clusterManager.createAutoClusterBackingGroup(
                            LookupUtil.getSubjectManager().getOverlord(), 
                            (ClusterKey) node.getData(), true);

                    String path = "/rhq/group/inventory/view.xhtml";

                    response.sendRedirect(path + "?groupId=" + group.getId() + "&parentGroupId=" + ((ClusterKey) node.getData()).getClusterGroupId());

                } else if (node.getData() instanceof AutoGroupComposite) {
                    state.setSelected(e.getOldSelection());
                    FacesContext.getCurrentInstance().addMessage("leftNavGroupTreeForm:leftNavGroupTree",
                        new FacesMessage(FacesMessage.SEVERITY_WARN, "No cluster autogroup views available", null));

                } else if (node.getData() instanceof ResourceGroup) {
                    String path = "/rhq/group/inventory/view.xhtml";
                    response.sendRedirect(path + "?groupId=" + ((ResourceGroup)node.getData()).getId());
                }
            }
        } catch (Exception e1) {
            state.setSelected(e.getOldSelection());
            FacesContext.getCurrentInstance().addMessage("leftNavGroupTreeForm:leftNavGroupTree",
                new FacesMessage(FacesMessage.SEVERITY_WARN, "Unable to browse to selected group view: " + e1.getMessage(), null));
            e1.printStackTrace();
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

}
