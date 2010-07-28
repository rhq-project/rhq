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
import java.util.HashSet;
import java.util.Set;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletResponse;

import org.richfaces.component.UITree;
import org.richfaces.component.html.HtmlTree;
import org.richfaces.component.state.TreeState;
import org.richfaces.component.state.TreeStateAdvisor;
import org.richfaces.model.TreeRowKey;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.resource.flyweight.AutoGroupCompositeFlyweight;
import org.rhq.core.domain.resource.group.ClusterKey;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.resource.cluster.ClusterManagerLocal;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Greg Hinkle
 */
public class ResourceGroupTreeStateAdvisor implements TreeStateAdvisor {

    private static class CurrentSelection {
        public ResourceGroup resourceGroup;
        public ClusterKey clusterKey;
    }
    
    private CurrentSelection currentSelection;

    private Set<ResourceGroupTreeNode> openNodes = new HashSet<ResourceGroupTreeNode>();
    
    private CurrentSelection getCurrentSelection() {
        int selectedGroupId = getSelectedGroupId();
        if (currentSelection == null || currentSelection.resourceGroup.getId() != selectedGroupId) {
            ResourceGroupManagerLocal groupManager = LookupUtil.getResourceGroupManager();
            currentSelection = new CurrentSelection();
            currentSelection.resourceGroup = groupManager.getResourceGroupById(EnterpriseFacesContextUtility.getSubject(),
                selectedGroupId, null);
            if (!currentSelection.resourceGroup.isVisible()) {
                currentSelection.clusterKey = ClusterKey.valueOf(currentSelection.resourceGroup.getClusterKey());
            }
        }
        
        return currentSelection;
    }

    private static int getSelectedGroupId() {
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

            ResourceGroupTreeNode traverseCheckNode = selectedNode.getParent();
            while (traverseCheckNode != null) {
                if (node.equals(traverseCheckNode)) {
                    closingParent = true;
                    break;
                }
                traverseCheckNode = traverseCheckNode.getParent();
            }

            if (closingParent) {
                if (redirectTo(node)) {
                    state.setSelected(key);
                    openNodes.remove(node);
                    
                    //this is nasty hack. We need some kind of flag that would persist only for the remainder
                    //of this request to advertise that no more open/closed states should be made in this request. 
                    //The tree is request scoped, so setting this flag will not persist to the next request. 
                    //In the case of the tree, setting this to true in this listener has no side-effects.
                    tree.setBypassUpdates(true);
                } else if (!redirectTo(selectedNode)) {
                    FacesContext.getCurrentInstance().addMessage("leftNavTreeForm:leftNavTree", 
                        new FacesMessage(FacesMessage.SEVERITY_WARN, "Failed to re-expand node that shouldn't be collapsed.", null));                    
                }
            } else {
                if (openNodes.contains(node)) {
                    openNodes.remove(node);
                } else {
                    openNodes.add(node);
                }
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

            CurrentSelection currentSelection = getCurrentSelection();
            
            //only update the state of open nodes in the preopen check
            //if we're not finishing the request in which a parent
            //of currently selected node was requested to close.
            //If we did update the open node states in the "remainder"
            //of such request the redirect that results from it would
            //get wrong information and the parent wouldn't appear closed
            //(because it'd had been re-opened in the below preopen call).
            //@see changeExpandListener for more nasty details.
            boolean setOpenStates = !tree.isBypassUpdates();
            
            if (preopen(node, currentSelection, setOpenStates)) {
                return true;
            }

            return openNodes.contains(node);
        }
        return null;
    }

    private boolean preopen(ResourceGroupTreeNode resourceTreeNode, CurrentSelection currentSelection, boolean setOpenState) {
        ResourceGroup currentGroup = currentSelection.resourceGroup;
        ClusterKey selectedClusterKey = currentSelection.clusterKey;
        
        boolean ret = false;
        for (ResourceGroupTreeNode child : resourceTreeNode.getChildren()) {
            if (child.getData() instanceof ClusterKey) {
                if (((ClusterKey) child.getData()).equals(selectedClusterKey)) {
                    ret = true;
                    break;
                }
            } else if (child.getData() instanceof ResourceGroup) {
                if (currentGroup.getId() == ((ResourceGroup) child.getData()).getId()) {
                    ret = true;
                    break;
                }
            }
            
            if (preopen(child, currentSelection, setOpenState)) {
                ret = true;
                break;
            }
        }

        if (setOpenState && ret) {
            openNodes.add(resourceTreeNode);
        }
        
        return ret;
    }

    public void nodeSelectListener(org.richfaces.event.NodeSelectedEvent e) {
        HtmlTree tree = (HtmlTree) e.getComponent();
        TreeState state = (TreeState) ((HtmlTree) tree).getComponentState();

        TreeRowKey<?> key = (TreeRowKey<?>) tree.getRowKey();
        ResourceGroupTreeNode node = (ResourceGroupTreeNode) tree.getRowData(key);

        if (node != null && !redirectTo(node)) {
            state.setSelected(e.getOldSelection());
        }
    }

    public Boolean adviseNodeSelected(UITree tree) {

        CurrentSelection currentSelection = getCurrentSelection();
        ResourceGroupTreeNode node = (ResourceGroupTreeNode) tree.getRowData(tree.getRowKey());

        if (node.getData() instanceof ResourceGroup) {
            return (currentSelection.resourceGroup.getId() == ((ResourceGroup) node.getData()).getId());
        } else if (node.getData() instanceof ClusterKey) {
            ClusterKey key = (ClusterKey) node.getData();

            if (currentSelection.clusterKey != null && currentSelection.clusterKey.equals(key)) {
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

        } else if (node.getData() instanceof AutoGroupCompositeFlyweight) {
            FacesContext.getCurrentInstance().addMessage("leftNavGroupTreeForm:leftNavGroupTree",
                new FacesMessage(FacesMessage.SEVERITY_WARN, "No cluster autogroup views available", null));
            return false;
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
