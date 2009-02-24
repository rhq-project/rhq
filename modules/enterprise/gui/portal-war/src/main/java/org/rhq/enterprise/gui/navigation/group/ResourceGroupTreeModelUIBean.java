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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;

import javax.faces.FacesException;
import javax.faces.el.MethodBinding;
import javax.faces.application.Application;
import javax.faces.component.UIComponent;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.el.MethodExpression;
import javax.el.ExpressionFactory;

import org.richfaces.component.UITree;
import org.richfaces.component.html.*;
import org.richfaces.event.NodeSelectedEvent;
import org.richfaces.model.TreeNode;
import org.richfaces.model.TreeNodeImpl;

import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.resource.ResourceTypeManagerLocal;
import org.rhq.enterprise.server.resource.ResourceTypeNotFoundException;
import org.rhq.enterprise.server.resource.cluster.ClusterKey;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.enterprise.server.operation.OperationManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementScheduleManagerLocal;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.gui.navigation.resource.ResourceTreeNode;
import org.rhq.enterprise.gui.navigation.resource.ResourceTreeModelUIBean;
import org.rhq.enterprise.gui.uibeans.AutoGroupCompositeDisplaySummary;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.composite.ResourceWithAvailability;
import org.rhq.core.domain.resource.group.composite.AutoGroupComposite;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.resource.group.GroupCategory;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.gui.util.FacesContextUtility;

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
    private MeasurementScheduleManagerLocal measurementScheduleManager = LookupUtil.getMeasurementScheduleManager();

    private String nodeTitle;
    private static final String DATA_PATH = "/richfaces/tree/examples/simple-tree-data.properties";

    private ContextMenu resourceContextMenu;

    private void loadTree() {

        long start = System.currentTimeMillis();
        
        FacesContext facesContext = FacesContext.getCurrentInstance();
        ExternalContext externalContext = facesContext.getExternalContext();


        Integer parentGroupId = FacesContextUtility.getOptionalRequestParameter("parentGroupId", Integer.class);
        ResourceGroup parentGroup;
        if (parentGroupId != null) {
            parentGroup = groupManager.getResourceGroupById(EnterpriseFacesContextUtility.getSubject(), parentGroupId, GroupCategory.COMPATIBLE);
        } else {
            parentGroup = EnterpriseFacesContextUtility.getResourceGroup();
        }

        rootNode = new ResourceGroupTreeNode(parentGroup);

        List<Resource> resources =
                resourceManager.getResourcesByCompatibleGroup(EnterpriseFacesContextUtility.getSubject(), parentGroup.getId(), PageControl.getUnlimitedInstance());

        List<Resource> members = groupManager.getResourcesForResourceGroup(EnterpriseFacesContextUtility.getSubject(), parentGroup.getId(), GroupCategory.COMPATIBLE);

        rootNode = load(parentGroup, resources, members);
        System.out.println("Loaded group tree in: " + (System.currentTimeMillis() - start));
    }

    private ResourceGroupTreeNode load(ResourceGroup group, List<Resource> resources, List<Resource> members) {

        Set<ResourceTreeNode> memberNodes = new HashSet<ResourceTreeNode>();

        for (Resource member : members) {
            memberNodes.add(ResourceTreeModelUIBean.load(member.getId(), resources));
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
                        key = new ClusterKey(((ResourceGroup) parentNode.getData()).getId(), res.getResourceType().getId(), res.getResourceKey());
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


    public void setMenu(ContextMenu menu) {
        System.out.println("*************************************************");
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
            nameItem.setId("menu_res_" + res.getId());
            MethodBinding mb = app.createMethodBinding("#{otherBean.action}", null);
            nameItem.setAction(mb);


            this.resourceContextMenu.getChildren().add(nameItem);


            this.resourceContextMenu.getChildren().add(new HtmlMenuSeparator());


            // *** Measurements menu
            List<MeasurementSchedule> scheds = measurementScheduleManager.getMeasurementSchedulesForResourceAndType(
                    subject, resourceId, DataType.MEASUREMENT, null, true);

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

                    // MethodExpression me = ExpressionFactory.newInstance().createMethodExpression();
                    MethodBinding binding = app.createMethodBinding("#{otherBean.action}", null);
                    menuItem.setAction(binding);

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

                    // MethodExpression me = ExpressionFactory.newInstance().createMethodExpression();
                    MethodBinding binding = app.createMethodBinding("#{otherBean.action}", null);
                    menuItem.setAction(binding);

                    operationsMenu.getChildren().add(menuItem);
                }
            }
        }
    }

    /*public void setMenu(ContextMenu menu) {
        this.operationsMenu = menu;

         operationsMenu.getChildren().clear();

        String resourceTypeIdString = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("contextResourceTypeId");
        if (resourceTypeIdString != null) {
            int resourceTypeId = Integer.parseInt(resourceTypeIdString);

            ResourceType type = null;

            // type = resourceTypeManager.getResourceTypeById(EnterpriseFacesContextUtility.getSubject(), resourceTypeId);
            List<OperationDefinition> operations = operationManager.getSupportedResourceTypeOperations(EnterpriseFacesContextUtility.getSubject(), resourceTypeId);

            if (operations != null) {
                for (OperationDefinition def : operations) {
                    HtmlMenuItem menuItem = new HtmlMenuItem();
                    String subOption = def.getDisplayName();
                    menuItem.setValue(subOption);
                    menuItem.setId("operation_" + def.getId());

                    Application app = FacesContext.getCurrentInstance().getApplication();
                    // MethodExpression me = ExpressionFactory.newInstance().createMethodExpression();
                    MethodBinding mb = app.createMethodBinding("#{otherBean.action}", null);
                    menuItem.setAction(mb);

                    operationsMenu.getChildren().add(menuItem);
                }
            }
        }
    }*/


}