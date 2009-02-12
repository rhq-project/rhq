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

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;

import org.richfaces.component.UITree;
import org.richfaces.component.html.HtmlTree;
import org.richfaces.component.state.TreeState;
import org.richfaces.component.state.TreeStateAdvisor;
import org.richfaces.model.TreeRowKey;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.composite.ResourceFacets;
import org.rhq.core.domain.resource.group.composite.AutoGroupComposite;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.inventory.resource.ResourceUIBean;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.resource.ResourceTypeManagerLocal;
import org.rhq.enterprise.server.resource.ResourceTypeNotFoundException;
import org.rhq.enterprise.server.util.LookupUtil;

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
    private int selecteAGTypeId;

    private ResourceTypeManagerLocal resourceTypeManager = LookupUtil.getResourceTypeManager();
    private ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();

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

            if (node != null) {
                ServletContext context = (ServletContext) FacesContextUtility.getFacesContext().getExternalContext()
                    .getContext();
                HttpServletResponse response = (HttpServletResponse) FacesContextUtility.getFacesContext()
                    .getExternalContext().getResponse();

                Subject subject = EnterpriseFacesContextUtility.getSubject();

                if (node.getData() instanceof Resource) {
                    String path = FacesContextUtility.getRequest().getRequestURI();

                    Resource resource = this.resourceManager.getResourceById(subject, ((Resource) node.getData())
                        .getId());
                    ResourceFacets facets = this.resourceTypeManager.getResourceFacets(subject, resource
                        .getResourceType().getId());

                    String fallbackPath = LookupUtil.getSystemManager().isMonitoringEnabled() ? "/rhq/resource/monitor/graphs.xhtml"
                        : "/rhq/resource/inventory/view.xhtml";

                    // Switching from a auto group view... default to monitor page
                    if (!path.startsWith("/rhq/resource")) {
                        path = fallbackPath;
                    } else {
                        if ((path.startsWith("/rhq/resource/configuration/") && !facets.isConfiguration())
                            || (path.startsWith("/rhq/resource/content/") && !facets.isContent())
                            || (path.startsWith("/rhq/resource/operation") && !facets.isOperation())) {
                            // This resource doesn't support those facets
                            path = fallbackPath;
                        } else if (path.startsWith("/rhq/resource/configuration/edit.xhtml")
                            && facets.isConfiguration()) {
                            path = "/rhq/resource/configuration/view.xhtml";
                        } else if (!path.startsWith("/rhq/resource/content/view.xhtml")
                            && path.startsWith("/rhq/resource/content/") && facets.isContent()) {
                            path = "/rhq/resource/content/view.xhtml";
                        } else if (!path.startsWith("/rhq/resource/inventory/view.xhtml")
                            && path.startsWith("/rhq/resource/inventory/")) {
                            path = "/rhq/resource/inventory/view.xhtml";
                        }
                    }

                    response.sendRedirect(path + "?id=" + ((Resource) node.getData()).getId());
                } else if (node.getData() instanceof AutoGroupComposite) {
                    AutoGroupComposite ag = (AutoGroupComposite) node.getData();
                    if (ag.getSubcategory() != null) {
                        // this is a subcategory or subsubcategory, no page to display right now
                        FacesContextUtility.getManagedBean(ResourceUIBean.class).setMessage(
                            "No pages exist for subcategory types");
                        return;
                    } else {
                        String path = "/rhq/autogroup/monitor/graphs.xhtml?parent=" + ag.getParentResource().getId()
                            + "&type=" + ag.getResourceType().getId();
                        response.sendRedirect(path);
                    }
                }
            }
        } catch (IOException e1) {
            e1.printStackTrace(); //To change body of catch statement use File | Settings | File Templates.
        } catch (ResourceTypeNotFoundException e1) {
            e1.printStackTrace(); //To change body of catch statement use File | Settings | File Templates.
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
                String typeId = FacesContextUtility.getOptionalRequestParameter("type");
                this.selecteAGTypeId = typeId != null ? Integer.parseInt(typeId) : 0;

                if (typeId != null) {
                    String id = FacesContextUtility.getOptionalRequestParameter("parent");
                    if (id != null) {
                        this.selectedId = Integer.parseInt(id);
                    }

                } else {
                    String id = FacesContextUtility.getOptionalRequestParameter("id");
                    if (id != null) {
                        this.selectedId = Integer.parseInt(id);
                    }
                }

                if (preopen((ResourceTreeNode) tree.getRowData(key), this.selectedId, this.selecteAGTypeId)) {
                    return true;
                }
            }

            if (selectedKey != null && (key.isSubKey(selectedKey)))
                return Boolean.TRUE;

            if (altered) {
                if (state.isExpanded(key))
                    return Boolean.TRUE;
            } else {
                Object data = ((ResourceTreeNode) tree.getRowData(key)).getData();
                if (data instanceof Resource) {
                    if (((Resource) data).getResourceType().getCategory() == ResourceCategory.PLATFORM)
                        return Boolean.TRUE;
                }
            }
        }
        return null;
    }

    private boolean preopen(ResourceTreeNode resourceTreeNode, int selectedResourceId, int selectedAGTypeId) {
        if (resourceTreeNode.getData() instanceof Resource && selectedAGTypeId == 0) {
            if (((Resource) resourceTreeNode.getData()).getId() == selectedResourceId) {
                return true;
            }
        } else if (resourceTreeNode.getData() instanceof AutoGroupComposite) {
            AutoGroupComposite ag = (AutoGroupComposite) resourceTreeNode.getData();
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
            if (node.getData() instanceof AutoGroupComposite) {
                AutoGroupComposite ag = (AutoGroupComposite) node.getData();
                if (ag.getParentResource() != null && ag.getResourceType() != null
                    && String.valueOf(ag.getParentResource().getId()).equals(parent)
                    && String.valueOf(ag.getResourceType().getId()).equals(type)) {
                    return true;
                }
            }
        }
        if (node.getData() instanceof Resource) {
            if (String.valueOf(((Resource) node.getData()).getId()).equals(id)) {
                return Boolean.TRUE;
            }
        }

        if (tree.getRowKey().equals(state.getSelectedNode())) {
            return Boolean.TRUE;
        }
        return null;
    }

}