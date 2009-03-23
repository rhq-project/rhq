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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.application.Application;
import javax.faces.component.html.HtmlGraphicImage;
import javax.faces.component.html.HtmlOutputLink;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.el.MethodBinding;
import javax.servlet.http.HttpServletRequest;

import org.richfaces.component.html.ContextMenu;
import org.richfaces.component.html.HtmlMenuGroup;
import org.richfaces.component.html.HtmlMenuItem;
import org.richfaces.component.html.HtmlMenuSeparator;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.composite.ResourceFacets;
import org.rhq.core.domain.resource.composite.ResourceWithAvailability;
import org.rhq.core.domain.resource.group.GroupCategory;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.resource.group.composite.AutoGroupComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.gui.util.FacesComponentUtility;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.navigation.resource.ResourceTreeModelUIBean;
import org.rhq.enterprise.gui.navigation.resource.ResourceTreeNode;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.measurement.MeasurementDefinitionManagerLocal;
import org.rhq.enterprise.server.operation.OperationManagerLocal;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.resource.ResourceTypeManagerLocal;
import org.rhq.enterprise.server.resource.ResourceTypeNotFoundException;
import org.rhq.enterprise.server.resource.cluster.ClusterKey;
import org.rhq.enterprise.server.resource.cluster.ClusterManagerLocal;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Greg Hinkle
 */
public class ResourceGroupTreeModelUIBean {

    private List<ResourceGroupTreeNode> roots = new ArrayList<ResourceGroupTreeNode>();
    private ResourceGroupTreeNode rootNode = null;
    private List<ResourceGroupTreeNode> children = new ArrayList<ResourceGroupTreeNode>();

    private ResourceGroupManagerLocal groupManager = LookupUtil.getResourceGroupManager();
    private ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();
    private ResourceTypeManagerLocal resourceTypeManager = LookupUtil.getResourceTypeManager();
    private OperationManagerLocal operationManager = LookupUtil.getOperationManager();
    private MeasurementDefinitionManagerLocal measurementDefinitionManager = LookupUtil
        .getMeasurementDefinitionManager();
    private ClusterManagerLocal clusterManager = LookupUtil.getClusterManager();

    private String nodeTitle;
    private static final String DATA_PATH = "/richfaces/tree/examples/simple-tree-data.properties";

    private static final String STYLE_QUICK_LINKS_ICON = "margin: 2px;";

    private ContextMenu resourceContextMenu;

    private void loadTree() {

        long start = System.currentTimeMillis();

        FacesContext facesContext = FacesContext.getCurrentInstance();
        ExternalContext externalContext = facesContext.getExternalContext();

        Integer parentGroupId = FacesContextUtility.getOptionalRequestParameter("parentGroupId", Integer.class);
        ResourceGroup parentGroup;
        if (parentGroupId != null) {
            parentGroup = groupManager.getResourceGroupById(EnterpriseFacesContextUtility.getSubject(), parentGroupId,
                GroupCategory.COMPATIBLE);
        } else {
            parentGroup = EnterpriseFacesContextUtility.getResourceGroup();
            if (parentGroup.getClusterResourceGroup() != null) {
                parentGroup = parentGroup.getClusterResourceGroup();
            }
        }

        rootNode = new ResourceGroupTreeNode(parentGroup);

        List<Resource> resources = resourceManager.getResourcesByCompatibleGroup(EnterpriseFacesContextUtility
            .getSubject(), parentGroup.getId(), PageControl.getUnlimitedInstance());

        List<Resource> members = groupManager.getResourcesForResourceGroup(EnterpriseFacesContextUtility.getSubject(),
            parentGroup.getId(), GroupCategory.COMPATIBLE);

        rootNode = load(parentGroup, resources, members);
        //        System.out.println("Loaded group tree in: " + (System.currentTimeMillis() - start));
    }

    private ResourceGroupTreeNode load(ResourceGroup group, List<Resource> resources, List<Resource> members) {

        Set<ResourceTreeNode> memberNodes = new HashSet<ResourceTreeNode>();

        for (Resource member : members) {
            memberNodes.add(ResourceTreeModelUIBean.load(member.getId(), resources, true));
        }

        ResourceGroupTreeNode root = new ResourceGroupTreeNode(group);
        root.setClusterKey(new ClusterKey(group.getId()));
        root.addMembers(memberNodes);
        load(root, memberNodes);
        return root;
    }

    private void load(ResourceGroupTreeNode parentNode, Set<ResourceTreeNode> resources) {

        Map<Object, ResourceGroupTreeNode> children = new HashMap<Object, ResourceGroupTreeNode>();

        for (ResourceTreeNode childNode : parentNode.getMembers()) {
            for (ResourceTreeNode node : childNode.getChildren()) {
                Object level = node.getData();

                if (level instanceof AutoGroupComposite) {
                    AutoGroupComposite agc = (AutoGroupComposite) level;
                    Object key = agc.getResourceType() != null ? agc.getResourceType() : agc.getSubcategory();

                    ResourceGroupTreeNode childGroupNode = children.get(key);
                    if (childGroupNode == null) {
                        childGroupNode = new ResourceGroupTreeNode(level);
                        children.put(key, childGroupNode);
                    }
                    childGroupNode.addMember(node);
                    childGroupNode.setClusterKey(parentNode.getClusterKey());

                } else if (level instanceof ResourceWithAvailability) {
                } else if (level instanceof Resource) {
                    Resource res = (Resource) level;
                    ClusterKey parentKey = parentNode.getClusterKey();
                    ClusterKey key = null;
                    if (parentKey == null) {
                        key = new ClusterKey(((ResourceGroup) parentNode.getData()).getId(), res.getResourceType()
                            .getId(), res.getResourceKey());
                    } else {
                        key = new ClusterKey(parentKey, res.getResourceType().getId(), res.getResourceKey());
                    }
                    ResourceGroupTreeNode childGroupNode = children.get(key);

                    if (childGroupNode == null) {
                        childGroupNode = new ResourceGroupTreeNode(key);
                        childGroupNode.setClusterKey(key);
                        children.put(key, childGroupNode);
                    }

                    childGroupNode.addMember(node);

                }
            }
        }

        parentNode.addChildren(children.values());

        for (ResourceGroupTreeNode child : children.values()) {
            Set<ResourceTreeNode> childChildren = new HashSet<ResourceTreeNode>();
            for (ResourceTreeNode childChild : child.getMembers()) {
                childChildren.addAll(childChild.getChildren());
            }
            if (childChildren.size() > 0)
                load(child, childChildren);
        }
    }

    public List<ResourceGroupTreeNode> getRoots() {
        if (rootNode == null) {
            loadTree();
        }
        List<ResourceGroupTreeNode> roots = new ArrayList<ResourceGroupTreeNode>();
        roots.add(this.rootNode);
        return roots;
    }

    public List<ResourceGroupTreeNode> getChildren() {
        return children;
    }

    public String getNodeTitle() {
        return nodeTitle;
    }

    public void setNodeTitle(String nodeTitle) {
        this.nodeTitle = nodeTitle;
    }

    public ContextMenu getMenu() {
        return resourceContextMenu;
    }

    public void setMenu(ContextMenu menu) throws ResourceTypeNotFoundException {
        this.resourceContextMenu = menu;

        this.resourceContextMenu.getChildren().clear();

        Subject subject = EnterpriseFacesContextUtility.getSubject();

        String clusterKeyString = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get(
            "contextClusterKey");

        String groupIdString = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get(
            "contextGroupId");
        String parentGroupIdString = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap()
            .get("contextParentGroupId");

        ResourceGroup group = null;

        if (clusterKeyString != null) {
            ClusterKey key = ClusterKey.valueOf(clusterKeyString);

            group = clusterManager.createAutoClusterBackingGroup(subject, key, false);

        } else if (groupIdString != null) {

            int groupId = Integer.parseInt(groupIdString);
            group = groupManager.getResourceGroupById(subject, groupId, null);

        }

        if (group != null) {

            Application app = FacesContext.getCurrentInstance().getApplication();

            if (group.getResourceType() != null) {
                ResourceType type = group.getResourceType();

                type = resourceTypeManager
                    .getResourceTypeById(EnterpriseFacesContextUtility.getSubject(), type.getId());

                HtmlMenuItem nameItem = new HtmlMenuItem();
                nameItem.setValue(group.getName());
                nameItem.setId("menu_group_" + group.getId());
                nameItem.setDisabled(true);
                nameItem.setStyle("color: black;");
                MethodBinding mb = app.createMethodBinding("#{otherBean.action}", null);
                nameItem.setAction(mb);

                this.resourceContextMenu.getChildren().add(nameItem);

                ResourceFacets facets = this.resourceTypeManager.getResourceFacets(subject, type.getId());

                addQuickLinks(String.valueOf(group.getId()), parentGroupIdString, facets);

                addMembers(group);

                this.resourceContextMenu.getChildren().add(new HtmlMenuSeparator());

                // *** Measurements menu
                List<MeasurementDefinition> definitions = measurementDefinitionManager
                    .getMeasurementDefinitionsByResourceType(subject, type.getId(), null, null);
                addMeasurementGraphToViewsMenu(String.valueOf(group.getId()), definitions);

                // **** Operations menugroup
                List<OperationDefinition> operations = operationManager.getSupportedResourceTypeOperations(subject,
                    type.getId());
                addOperationsMenu(String.valueOf(group.getId()), parentGroupIdString, operations);
            }
        }
    }

    private void addMembers(ResourceGroup group) {
        List<Resource> resources = null;
        if (group.getClusterKey() != null) {
            resources = clusterManager.getAutoClusterResources(EnterpriseFacesContextUtility.getSubject(), ClusterKey
                .valueOf(group.getClusterKey()));
        } else {
            resources = groupManager.getResourcesForResourceGroup(EnterpriseFacesContextUtility.getSubject(), group
                .getId(), null);
        }

        HtmlMenuGroup membersMenuItem = new HtmlMenuGroup();
        membersMenuItem.setValue("Members");
        membersMenuItem.setId("menu_groupMembers_" + group.getId());
        membersMenuItem.setDisabled(true);
        membersMenuItem.setStyle("color: black;");

        for (Resource res : resources) {
            HtmlMenuItem menuItem = new HtmlMenuItem();
            menuItem.setValue(res.getName());
            menuItem.setId("groupMember_" + res.getId());

            String url = "/rhq/resource/summary/overview.xhtml?id=" + res.getId();

            menuItem.setSubmitMode("none");
            menuItem.setOnclick("document.location.href='" + url + "'");

            membersMenuItem.getChildren().add(menuItem);
        }

        this.resourceContextMenu.getChildren().add(membersMenuItem);
    }

    private void addQuickLinks(String groupId, String parentGroupId, ResourceFacets facets) {
        HtmlMenuItem quickLinksItem = new HtmlMenuItem();
        quickLinksItem.setSubmitMode("none");
        quickLinksItem.setId("menu_groupQuickLinks_" + groupId);

        String url;
        HtmlOutputLink link;
        HtmlGraphicImage image;

        String attributes = "groupId=" + groupId;
        if (parentGroupId != null) {
            attributes += "&parentGroupId=" + parentGroupId;
        }

        if (LookupUtil.getSystemManager().isMonitoringEnabled()) {
            url = "/rhq/group/monitor/graphs.xhtml?" + attributes;
            link = FacesComponentUtility.addOutputLink(quickLinksItem, null, url);
            image = FacesComponentUtility.addGraphicImage(link, null, "/images/icons/Monitor_grey_16.png", "Monitor");
            image.setStyle(STYLE_QUICK_LINKS_ICON);
        }

        url = "/rhq/group/inventory/view.xhtml?" + attributes;
        link = FacesComponentUtility.addOutputLink(quickLinksItem, null, url);
        image = FacesComponentUtility.addGraphicImage(link, null, "/images/icons/Inventory_grey_16.png", "Inventory");
        image.setStyle(STYLE_QUICK_LINKS_ICON);

        if (facets.isConfiguration()) {
            url = "/rhq/group/configuration/viewCurrent.xhtml?" + attributes;
            link = FacesComponentUtility.addOutputLink(quickLinksItem, null, url);
            image = FacesComponentUtility.addGraphicImage(link, null, "/images/icons/Configure_grey_16.png", "Configuration");
            image.setStyle(STYLE_QUICK_LINKS_ICON);
        }

        if (facets.isOperation()) {
            url = "/rhq/group/operation/groupOperationScheduleNew.xhtml?" + attributes;
            link = FacesComponentUtility.addOutputLink(quickLinksItem, null, url);
            image = FacesComponentUtility.addGraphicImage(link, null, "/images/icons/Operation_grey_16.png", "Operations");
            image.setStyle(STYLE_QUICK_LINKS_ICON);
        }

        if (facets.isEvent()) {
            url = "/rhq/group/events/history.xhtml?" + attributes;
            link = FacesComponentUtility.addOutputLink(quickLinksItem, null, url);
            image = FacesComponentUtility.addGraphicImage(link, null, "/images/icons/Events_grey_16.png", "Events");
            image.setStyle(STYLE_QUICK_LINKS_ICON);
        }

        this.resourceContextMenu.getChildren().add(quickLinksItem);
    }

    private void addMeasurementGraphToViewsMenu(String groupId, List<MeasurementDefinition> definitions) {
        HttpServletRequest request = FacesContextUtility.getRequest();
        String requestURL = request.getRequestURL().toString().toLowerCase();
        boolean onMonitorGraphsSubtab = (requestURL.indexOf("/monitor/graphs.xhtml") != -1);

        // addChartToGraph menu only if you're looking at the graphs
        if (onMonitorGraphsSubtab && definitions != null) {
            HtmlMenuGroup measurementMenu = new HtmlMenuGroup();
            measurementMenu.setValue("Add Graph to View");
            this.resourceContextMenu.getChildren().add(measurementMenu);
            measurementMenu.setDisabled(definitions.isEmpty());

            for (MeasurementDefinition definition : definitions) {
                HtmlMenuItem menuItem = new HtmlMenuItem();
                String subOption = definition.getDisplayName();
                menuItem.setValue(subOption);
                menuItem.setId("measurementGraphMenuItem_" + definition.getId());

                /**
                 * resource    '<resourceId>,<scheduleId>'
                 * compatgroup 'cg,<groupId>,<definitionId>'
                 * autogroup   'ag,<parentId>,<definitionId>,<typeId>'
                 */
                String onClickAddMeasurements = "addMetric('cg," + groupId + "," + definition.getId() + "');";
                String onClickRefreshPage = "setTimeout(window.location.reload(), 5000);"; // refresh after 5 secs

                menuItem.setSubmitMode("none");
                menuItem.setOnclick(onClickAddMeasurements + onClickRefreshPage);

                measurementMenu.getChildren().add(menuItem);
            }
        }
    }

    private void addOperationsMenu(String groupId, String parentGroupId, List<OperationDefinition> operations) {

        if (operations != null) {
            HtmlMenuGroup operationsMenu = new HtmlMenuGroup();
            operationsMenu.setValue("Operations");
            this.resourceContextMenu.getChildren().add(operationsMenu);
            operationsMenu.setDisabled(operations.isEmpty());

            for (OperationDefinition def : operations) {
                HtmlMenuItem menuItem = new HtmlMenuItem();
                String subOption = def.getDisplayName();
                menuItem.setValue(subOption);
                menuItem.setId("operation_" + def.getId());

                String url = "/rhq/group/operation/groupOperationScheduleNew.xhtml";
                url += "?opId=" + def.getId();
                url += "&groupId=" + groupId;
                if (parentGroupId != null) {
                    url += "&parentGroupId=" + parentGroupId;
                }

                menuItem.setSubmitMode("none");
                menuItem.setOnclick("document.location.href='" + url + "'");

                operationsMenu.getChildren().add(menuItem);
            }
        }
    }
}