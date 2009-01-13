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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.application.Application;
import javax.faces.component.html.HtmlOutputLink;
import javax.faces.context.FacesContext;

import org.richfaces.component.UITree;
import org.richfaces.component.html.ContextMenu;
import org.richfaces.component.html.HtmlMenuGroup;
import org.richfaces.component.html.HtmlMenuItem;
import org.richfaces.component.html.HtmlMenuSeparator;
import org.richfaces.event.NodeSelectedEvent;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceSubCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.composite.ResourceWithAvailability;
import org.rhq.core.domain.resource.group.composite.AutoGroupComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.gui.util.FacesComponentUtility;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.core.AgentManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementScheduleManagerLocal;
import org.rhq.enterprise.server.operation.OperationManagerLocal;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Backing bean for the left navigation tree for resources
 *
 * @author Greg Hinkle
 */
public class ResourceTreeModelUIBean {

    private List<ResourceTreeNode> roots = new ArrayList<ResourceTreeNode>();
    private ResourceTreeNode rootNode = null;

    private ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();
    private AgentManagerLocal agentManager = LookupUtil.getAgentManager();
    private OperationManagerLocal operationManager = LookupUtil.getOperationManager();
    private MeasurementScheduleManagerLocal measurementScheduleManager = LookupUtil.getMeasurementScheduleManager();

    private String nodeTitle;

    private ContextMenu resourceContextMenu;

    private void loadTree() {

        Resource currentResource = EnterpriseFacesContextUtility.getResourceIfExists();
        Subject user = EnterpriseFacesContextUtility.getSubject();

        Resource rootResource = resourceManager.getRootResourceForResource(currentResource.getId());
        Agent agent = agentManager.getAgentByResourceId(rootResource.getId());

        List<Resource> resources = resourceManager.getResourcesByAgent(user, agent.getId(), PageControl.getUnlimitedInstance());

        rootNode = load(rootResource.getId(), resources);

    }

    private ResourceTreeNode load(int rootId, List<Resource> resources) {
        Resource found = null;
        for (Resource res : resources) {
            if (res.getId() == rootId) {
                found = res;
            }
        }
        ResourceTreeNode root = new ResourceTreeNode(found);
        load(root, resources);
        return root;
    }

    private void load(ResourceTreeNode parentNode, List<Resource> resources) {

        if (parentNode.getData() instanceof Resource) {
            Resource parentResource = (Resource) parentNode.getData();

            Map<Object, List<Resource>> children = new HashMap<Object, List<Resource>>();
            for (Resource res : resources) {
                if (res.getParentResource() != null && res.getParentResource().getId() == parentResource.getId()) {
                    if (res.getResourceType().getSubCategory() != null) {
                        // These are children that have subcategories
                        // Split them by if they are a sub-sub category or just a category
                        if (res.getResourceType().getSubCategory().getParentSubCategory() == null) {
                            if (children.containsKey(res.getResourceType().getSubCategory())) {
                                children.get(res.getResourceType().getSubCategory()).add(res);
                            } else {
                                ArrayList<Resource> list = new ArrayList<Resource>();
                                list.add(res);
                                children.put(res.getResourceType().getSubCategory(), list);
                            }
                        } else if (res.getResourceType().getSubCategory().getParentSubCategory() != null) {
                            if (children.containsKey(res.getResourceType().getSubCategory().getParentSubCategory())) {
                                children.get(res.getResourceType().getSubCategory().getParentSubCategory()).add(res);
                            } else {
                                ArrayList<Resource> list = new ArrayList<Resource>();
                                list.add(res);
                                children.put(res.getResourceType().getSubCategory().getParentSubCategory(), list);
                            }
                        }
                    } else {
                        // These are children without categories of the parent resource
                        // - Add them into groupings by their resource type
                        if (children.containsKey(res.getResourceType())) {
                            children.get(res.getResourceType()).add(res);
                        } else {
                            ArrayList<Resource> list = new ArrayList<Resource>();
                            list.add(res);
                            children.put(res.getResourceType(), list);
                        }
                    }
                }
            }

            for (Object rsc : children.keySet()) {
                if (rsc != null && (rsc instanceof ResourceSubCategory || children.get(rsc).size() > 1)) {
                    double avail = 0;
                    List<Resource> entries = children.get(rsc);
                    for (Resource res : entries) {
                        avail += res.getCurrentAvailability().getAvailabilityType() == AvailabilityType.UP ? 1 : 0;
                    }
                    avail = avail / entries.size();

                    AutoGroupComposite agc = null;
                    if (rsc instanceof ResourceSubCategory) {
                        agc = new AutoGroupComposite(avail, parentResource, (ResourceSubCategory) rsc, entries.size());
                    } else if (rsc instanceof ResourceType) {
                        agc = new AutoGroupComposite(avail, parentResource, (ResourceType) rsc, entries.size());
                    }
                    ResourceTreeNode node = new ResourceTreeNode(agc);
                    parentNode.getChildren().add(node);
                    load(node, resources);
                } else {
                    List<Resource> entries = children.get(rsc);
                    for (Resource res : entries) {
                        ResourceTreeNode node = new ResourceTreeNode(res);
                        parentNode.getChildren().add(node);
                        load(node, resources);
                    }
                }

            }
        } else {
            // #####################################################################################

            AutoGroupComposite compositeParent = (AutoGroupComposite) parentNode.getData();

            Map<Object, List<Resource>> children = new HashMap<Object, List<Resource>>();
            for (Resource res : resources) {
                if (compositeParent.getSubcategory() != null) {
                    // parent is a sub category
                    if (res.getResourceType().getSubCategory() != null && compositeParent.getSubcategory().equals(res.getResourceType().getSubCategory().getParentSubCategory())) {
                        // A subSubCategory in a subcategory
                        if (children.containsKey(res.getResourceType().getSubCategory())) {
                            children.get(res.getResourceType().getSubCategory()).add(res);
                        } else {
                            ArrayList<Resource> list = new ArrayList<Resource>();
                            list.add(res);
                            children.put(res.getResourceType().getSubCategory(), list);
                        }
                    } else if (compositeParent.getSubcategory().equals(res.getResourceType().getSubCategory())) {
                        // Direct entries in a subcategory... now group them by autogroup (type)
                        if (children.containsKey(res.getResourceType())) {
                            children.get(res.getResourceType()).add(res);
                        } else {
                            ArrayList<Resource> list = new ArrayList<Resource>();
                            list.add(res);
                            children.put(res.getResourceType(), list);
                        }
                    }
                } else if (compositeParent.getResourceType() != null) {
                    if (compositeParent.getResourceType().equals(res.getResourceType())) {
                        if (children.containsKey(res.getResourceType())) {
                            children.get(res.getResourceType()).add(res);
                        } else {
                            ArrayList<Resource> list = new ArrayList<Resource>();
                            list.add(res);
                            children.put(res.getResourceType(), list);
                        }
                    }
                }
            }

            for (Object rsc : children.keySet()) {
                if (rsc != null && (rsc instanceof ResourceSubCategory || (children.get(rsc).size() > 1 && ((AutoGroupComposite) parentNode.getData()).getSubcategory() != null))) {
                    double avail = 0;
                    List<Resource> entries = children.get(rsc);
                    for (Resource res : entries) {
                        avail += res.getCurrentAvailability().getAvailabilityType() == AvailabilityType.UP ? 1 : 0;
                    }
                    avail = avail / entries.size();

                    AutoGroupComposite agc = null;
                    if (rsc instanceof ResourceSubCategory) {
                        agc = new AutoGroupComposite(avail, compositeParent.getParentResource(), (ResourceSubCategory) rsc, entries.size());
                    } else if (rsc instanceof ResourceType) {
                        agc = new AutoGroupComposite(avail, compositeParent.getParentResource(), (ResourceType) rsc, entries.size());
                    }
                    ResourceTreeNode node = new ResourceTreeNode(agc);
                    parentNode.getChildren().add(node);
                    load(node, resources);
                } else {
                    List<Resource> entries = children.get(rsc);
                    for (Resource res : entries) {
                        ResourceTreeNode node = new ResourceTreeNode(res);
                        parentNode.getChildren().add(node);
                        load(node, resources);
                    }
                }
            }
        }

    }

    public ResourceTreeNode getTreeNode() {
        if (rootNode == null) {
            loadTree();
        }

        return rootNode;
    }

    public List<ResourceTreeNode> getRoots() {
        if (roots.isEmpty()) {
            roots.add(getTreeNode());
        }
        return roots;
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

    public void processSelection(NodeSelectedEvent event) {
        UITree tree = (UITree) event.getComponent();

        try {

            Object node = tree.getRowData();
            ResourceTreeNode selectedNode = (ResourceTreeNode) node;

            Object data = selectedNode.getData();
            if (data instanceof ResourceWithAvailability) {
                FacesContext.getCurrentInstance();

            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setMenu(ContextMenu menu) {
        this.resourceContextMenu = menu;

        this.resourceContextMenu.getChildren().clear();

        Subject subject = EnterpriseFacesContextUtility.getSubject();

        String resourceIdString = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("contextResourceId");
        String resourceTypeIdString = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("contextResourceTypeId");
        if (resourceTypeIdString != null) {
            int resourceId = Integer.parseInt(resourceIdString);
            int resourceTypeId = Integer.parseInt(resourceTypeIdString);

            Resource res = resourceManager.getResourceById(EnterpriseFacesContextUtility.getSubject(), resourceId);
            Application app = FacesContext.getCurrentInstance().getApplication();
            // type = resourceTypeManager.getResourceTypeById(EnterpriseFacesContextUtility.getSubject(), resourceTypeId);

            HtmlMenuItem nameItem = new HtmlMenuItem();
            nameItem.setValue(res.getName());
            this.resourceContextMenu.getChildren().add(nameItem);

            HtmlMenuItem typeItem = new HtmlMenuItem();
            typeItem.setValue(res.getResourceType().getName());
            this.resourceContextMenu.getChildren().add(typeItem);

            HtmlMenuItem quickLinksItem = new HtmlMenuItem();
            quickLinksItem.setSubmitMode("none");
            quickLinksItem.setId("menu_res_" + res.getId());

            HtmlOutputLink monitorLink = FacesComponentUtility.addOutputLink(quickLinksItem, null, "/rhq/resource/monitor/graphs.xhtml?id=" + resourceIdString);
            FacesComponentUtility.addGraphicImage(monitorLink, null, "/images/icon_hub_m.gif", "Monitor").setStyle("margin: 2px;");
            HtmlOutputLink eventLink = FacesComponentUtility.addOutputLink(quickLinksItem, null, "/rhq/resource/events/history.xhtml?id=" + resourceIdString);
            FacesComponentUtility.addGraphicImage(eventLink, null, "/images/icon_hub_e.gif", "Events").setStyle("margin: 2px;");
            HtmlOutputLink inventoryLink = FacesComponentUtility.addOutputLink(quickLinksItem, null, "/rhq/resource/inventory/view.xhtml?id=" + resourceIdString);
            FacesComponentUtility.addGraphicImage(inventoryLink, null, "/images/icon_hub_i.gif", "Inventory").setStyle("margin: 2px;");
            HtmlOutputLink configurationLink = FacesComponentUtility.addOutputLink(quickLinksItem, null, "/rhq/resource/configuration/view.xhtml?id=" + resourceIdString);
            FacesComponentUtility.addGraphicImage(configurationLink, null, "/images/icon_hub_c.gif", "Configuration").setStyle("margin: 2px;");
            HtmlOutputLink operationsLink = FacesComponentUtility.addOutputLink(quickLinksItem, null, "/rhq/resource/operation/resourceOperationScheduleNew.xhtml?id=" + resourceIdString);
            FacesComponentUtility.addGraphicImage(operationsLink, null, "/images/icon_hub_o.gif", "Operations").setStyle("margin: 2px;");
            HtmlOutputLink alertsLink = FacesComponentUtility.addOutputLink(quickLinksItem, null, "/rhq/resource/alert/listAlertDefinitions.xhtml?id=" + resourceIdString);
            FacesComponentUtility.addGraphicImage(alertsLink, null, "/images/icon_hub_a.gif", "Alerts");

            this.resourceContextMenu.getChildren().add(quickLinksItem);

            /*
                    FacesComponentUtility.addOutputLink(nameItem, null, "/rhq/resource/monitor/graphs.xhtml?id=" + resourceIdString));

            HtmlMenuItem foo = new HtmlMenuItem();
            foo.setValue("<a href='foo'>bar</a>");


            SubviewTag f 
            NamingContainer c = new NamingContainer() {
            };

            this.resourceContextMenu.getChildren().add(foo );

            */

            this.resourceContextMenu.getChildren().add(new HtmlMenuSeparator());

            // *** Measurements menu
            List<MeasurementSchedule> scheds = measurementScheduleManager.getMeasurementSchedulesForResourceAndType(subject, resourceId, DataType.MEASUREMENT, null, true);

            if (scheds != null) {
                HtmlMenuGroup measurementsMenu = new HtmlMenuGroup();
                measurementsMenu.setValue("Measurements");
                this.resourceContextMenu.getChildren().add(measurementsMenu);
                measurementsMenu.setDisabled(scheds.isEmpty());

                for (MeasurementSchedule sched : scheds) {
                    HtmlMenuItem menuItem = new HtmlMenuItem();
                    String subOption = sched.getDefinition().getDisplayName();
                    menuItem.setValue(subOption);
                    menuItem.setId("measurement_" + sched.getId());

                    String url = "/resource/common/monitor/Visibility.do?mode=chartSingleMetricSingleResource" + "&m=" + sched.getId() + "&id=" + res.getId();

                    menuItem.setSubmitMode("none");
                    menuItem.setOnclick("document.location.href='" + url + "'");

                    measurementsMenu.getChildren().add(menuItem);

                }
            }

            // **** Operations menugroup

            List<OperationDefinition> operations = operationManager.getSupportedResourceTypeOperations(subject, resourceTypeId);

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

                    String url = "/rhq/resource/operation/resourceOperationScheduleNew.xhtml?id=" + res.getId() + "&opId=" + def.getId();

                    menuItem.setSubmitMode("none");
                    menuItem.setOnclick("document.location.href='" + url + "'");

                    operationsMenu.getChildren().add(menuItem);
                }
            }
        }
    }

}
