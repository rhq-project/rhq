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
package org.rhq.enterprise.gui.navigation.resource;

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
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.composite.ResourceFacets;
import org.rhq.core.domain.resource.flyweight.AutoGroupCompositeFlyweight;
import org.rhq.core.domain.resource.flyweight.ResourceFlyweight;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.common.tag.FunctionTagLibrary;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.resource.ResourceTypeManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Manages the tree selection and node openess for the left nav resource tree
 *
 * @author Greg Hinkle
 */
public class ResourceTreeStateAdvisor implements TreeStateAdvisor {

    UITree tree;
    TreeState treeState = new TreeState();
    private int selecteAGTypeId;

    private boolean hasMessages = false;

    private ResourceTypeManagerLocal resourceTypeManager = LookupUtil.getResourceTypeManager();
    private ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();

    public TreeState getTreeState() {
        return treeState;
    }

    public void changeExpandListener(org.richfaces.event.NodeExpandedEvent e) {
        HtmlTree tree = (HtmlTree) e.getComponent();
        TreeState state = (TreeState) tree.getComponentState();

        //check if we're collapsing a parent of currently selected node.
        //if we do, change the focus to the parent
        if (state.getSelectedNode() != null) {
            boolean closingParent = false;

            TreeRowKey<?> key = (TreeRowKey<?>) tree.getRowKey();
            ResourceTreeNode node = (ResourceTreeNode) tree.getRowData(key);
            ResourceTreeNode selectedNode = (ResourceTreeNode) tree.getRowData(state.getSelectedNode());

            ResourceTreeNode traverseCheckNode = selectedNode.getParent();
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
                } else if (!redirectTo(selectedNode)) {
                    FacesContext.getCurrentInstance().addMessage("leftNavTreeForm:leftNavTree", 
                        new FacesMessage(FacesMessage.SEVERITY_WARN, "Failed to re-expand node that shouldn't be collapsed.", null));                    
                }
            } 

        }
    }

    public void nodeSelectListener(org.richfaces.event.NodeSelectedEvent e) {
        HtmlTree tree = (HtmlTree) e.getComponent();
        TreeState state = (TreeState) ((HtmlTree) tree).getComponentState();

        ResourceTreeNode node = (ResourceTreeNode) tree.getRowData(tree.getRowKey());

        if (node != null && !redirectTo(node)) {
            state.setSelected(e.getOldSelection());
        }
    }

    public Boolean adviseNodeOpened(UITree tree) {
        TreeRowKey<?> key = (TreeRowKey<?>) tree.getRowKey();
        
        //don't bother continuing if there's nothing to check against...
        if (key == null) {
            return null;
        }

        ResourceTreeNode node = (ResourceTreeNode) tree.getRowData(key);
        
        //always expand root
        if (node.getParent() == null) {
            return true;
        }

        //make sure that the node refered to in the request parameters is visible
        int selectedId = 0;
        String typeId = FacesContextUtility.getOptionalRequestParameter("type");
        this.selecteAGTypeId = ((typeId == null || typeId.length() == 0) ? 0 : Integer.parseInt(typeId));

        if (typeId != null) {
            String id = FacesContextUtility.getOptionalRequestParameter("parent");
            if (id != null && id.length() != 0) {
                selectedId = Integer.parseInt(id);
            }

        } else {
            String id = FacesContextUtility.getOptionalRequestParameter("id");
            if (id != null && id.length() != 0) {
                selectedId = Integer.parseInt(id);
            }
        }

        if (preopen((ResourceTreeNode) tree.getRowData(key), selectedId, this.selecteAGTypeId)) {
            return true;
        }
        
        //ok, in this case return whatever the last open state was
        TreeState state = (TreeState) tree.getComponentState();
        return state.isExpanded(key);
    }

    private boolean preopen(ResourceTreeNode resourceTreeNode, int selectedResourceId, int selectedAGTypeId) {
        if (resourceTreeNode.getData() instanceof ResourceFlyweight && selectedAGTypeId == 0) {
            if (((ResourceFlyweight) resourceTreeNode.getData()).getId() == selectedResourceId) {
                return true;
            }
        } else if (resourceTreeNode.getData() instanceof AutoGroupCompositeFlyweight) {
            AutoGroupCompositeFlyweight ag = (AutoGroupCompositeFlyweight) resourceTreeNode.getData();
            if (ag.getParentResource().getId() == selectedResourceId && ag.getResourceType() != null
                && ag.getResourceType().getId() == selectedAGTypeId) {
                return true;
            }
        }

        for (ResourceTreeNode child : resourceTreeNode.getChildren()) {
            if (preopen(child, selectedResourceId, selectedAGTypeId)) {
                return true;
            }
        }

        return false;
    }

    public Boolean adviseNodeSelected(UITree tree) {
        TreeState state = (TreeState) ((HtmlTree) tree).getComponentState();
        String id = FacesContextUtility.getOptionalRequestParameter("id");
        String parent = FacesContextUtility.getOptionalRequestParameter("parent");
        String type = FacesContextUtility.getOptionalRequestParameter("type");
        ResourceTreeNode node = (ResourceTreeNode) tree.getRowData(tree.getRowKey());

        if (this.selecteAGTypeId > 0) {
            if (node.getData() instanceof AutoGroupCompositeFlyweight) {
                AutoGroupCompositeFlyweight ag = (AutoGroupCompositeFlyweight) node.getData();
                if (ag.getParentResource() != null && ag.getResourceType() != null
                    && String.valueOf(ag.getParentResource().getId()).equals(parent)
                    && String.valueOf(ag.getResourceType().getId()).equals(type)) {
                    return true;
                }
            }
        }
        if (node.getData() instanceof ResourceFlyweight) {
            if (String.valueOf(((ResourceFlyweight) node.getData()).getId()).equals(id)) {
                return Boolean.TRUE;
            }
        }

        if (tree.getRowKey().equals(state.getSelectedNode())) {
            return Boolean.TRUE;
        }
        return null;
    }

    public boolean getHasMessages() {
        hasMessages = FacesContext.getCurrentInstance().getMessages("leftNavTreeForm:leftNavTree").hasNext();
        return hasMessages;
    }

    /**
     * @return false if there was an error redirecting to the target location
     */
    private boolean redirectTo(ResourceTreeNode node) {
        HttpServletResponse response = (HttpServletResponse) FacesContextUtility.getFacesContext().getExternalContext()
            .getResponse();
        Subject subject = EnterpriseFacesContextUtility.getSubject();

        String path = "";
        if (node.getData() instanceof ResourceFlyweight) {
            ResourceFlyweight flyweight = (ResourceFlyweight) node.getData();
            
            if (flyweight.isLocked()) {
                FacesContext.getCurrentInstance().addMessage(
                    "leftNavTreeForm:leftNavTree",
                    new FacesMessage(FacesMessage.SEVERITY_WARN,
                        "You have not been granted view access to this resource", null));

                return false;
            } else {            
                path = FacesContextUtility.getRequest().getRequestURI();

                //Resource resource = this.resourceManager.getResourceById(subject, ((Resource) node.getData()).getId());
                ResourceFacets facets = this.resourceTypeManager.getResourceFacets(flyweight.getResourceType()
                    .getId());

                String fallbackPath = FunctionTagLibrary.getDefaultResourceTabURL();

                // Switching from a auto group view... default to monitor page
                if (!path.startsWith("/rhq/resource")) {
                    path = fallbackPath;
                } else {
                    if ((path.startsWith("/rhq/resource/configuration/") && !facets.isConfiguration())
                        || (path.startsWith("/rhq/resource/content/") && !facets.isContent())
                        || (path.startsWith("/rhq/resource/operation") && !facets.isOperation())
                        || (path.startsWith("/rhq/resource/events") && !facets.isEvent())) {
                        // This resource doesn't support those facets
                        path = fallbackPath;
                    } else if ((path.startsWith("/rhq/resource/configuration/view-map.xhtml")
                        || path.startsWith("/rhq/resource/configuration/edit-map.xhtml")
                        || path.startsWith("/rhq/resource/configuration/add-map.xhtml") || path
                        .startsWith("/rhq/resource/configuration/edit.xhtml")
                        && facets.isConfiguration())) {
                        path = "/rhq/resource/configuration/view.xhtml";

                    } else if (!path.startsWith("/rhq/resource/content/view.xhtml")
                        && path.startsWith("/rhq/resource/content/") && facets.isContent()) {
                        path = "/rhq/resource/content/view.xhtml";
                    } else if (path.startsWith("/rhq/resource/inventory/")
                        && !(path.startsWith("/rhq/resource/inventory/view.xhtml")
                            || (facets.isPluginConfiguration() && path
                                .startsWith("/rhq/resource/inventory/view-connection.xhtml")) || path
                            .startsWith("/rhq/resource/inventory/view-agent.xhtml"))) {
                        path = "/rhq/resource/inventory/view.xhtml";
                    } else if (path.startsWith("/rhq/resource/operation/resourceOperationHistoryDetails.xhtml")) {
                        path = "/rhq/resource/operation/resourceOperationHistory.xhtml";
                    } else if (path.startsWith("/rhq/resource/operation/resourceOperationScheduleDetails.xhtml")) {
                        path = "/rhq/resource/operation/resourceOperationSchedules.xhtml";
                    } else if (path.startsWith("/rhq/resource/monitor/response.xhtml") && !facets.isCallTime()) {
                        path = fallbackPath;
                    }
                }

                path += ("?id=" + flyweight.getId());
            }
        } else if (node.getData() instanceof AutoGroupCompositeFlyweight) {
            AutoGroupCompositeFlyweight ag = (AutoGroupCompositeFlyweight) node.getData();
            if (ag.getResourceType() == null) {
                //XXX this is a temporary measure. The subcategories will get the content page in the end.
                FacesContext.getCurrentInstance().addMessage("leftNavTreeForm:leftNavTree", 
                    new FacesMessage(FacesMessage.SEVERITY_WARN, "No subcategory page exists.", null));
                
                return false;
            } else if (ag.getMemberCount() != node.getChildren().size()) {
                // you don't have access to every autogroup resource
                FacesContext.getCurrentInstance().addMessage(
                    "leftNavTreeForm:leftNavTree",
                    new FacesMessage(FacesMessage.SEVERITY_WARN,
                        "You must have view access to all resources in an autogroup to view it", null));

                return false;
            } else {
                path = "/rhq/autogroup/monitor/graphs.xhtml?parent=" + ag.getParentResource().getId() + "&type="
                    + ag.getResourceType().getId();
            }
        }

        try {
            response.sendRedirect(path);
            return true; // all is well in the land
        } catch (IOException ioe) {
            FacesContext.getCurrentInstance().addMessage(
                "leftNavTreeForm:leftNavTree",
                new FacesMessage(FacesMessage.SEVERITY_WARN, "Unable to browse to selected resource view: "
                    + ioe.getMessage(), null));
        }
        return false; // IO errors from redirect
    }
}