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
import java.util.Collections;
import java.util.List;

import org.richfaces.component.html.ContextMenu;
import org.richfaces.component.html.HtmlMenuGroup;
import org.richfaces.component.html.HtmlMenuItem;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.navigation.contextmenu.MenuItemDescriptor;
import org.rhq.enterprise.gui.navigation.contextmenu.MetricMenuItemDescriptor;
import org.rhq.enterprise.gui.navigation.contextmenu.QuickLinksDescriptor;
import org.rhq.enterprise.gui.navigation.contextmenu.TreeContextMenuBase;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.measurement.MeasurementDefinitionManagerLocal;
import org.rhq.enterprise.server.operation.OperationManagerLocal;
import org.rhq.enterprise.server.resource.cluster.ClusterKey;
import org.rhq.enterprise.server.resource.cluster.ClusterManagerLocal;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Greg Hinkle
 * @author Lukas Krejci
 */
public class ResourceGroupTreeContextMenuUIBean extends TreeContextMenuBase {

    private ResourceGroup currentResourceGroup;
    private String currentParentResourceGroupId;
    private List<MenuItemDescriptor> menuItemDescriptorsForView;
    private List<MetricMenuItemDescriptor> metricMenuItemDescriptorsForGraph;
    private List<MenuItemDescriptor> menuItemDescriptorsForOperations;

    private ResourceGroupManagerLocal groupManager = LookupUtil.getResourceGroupManager();
    private ClusterManagerLocal clusterManager = LookupUtil.getClusterManager();
    private OperationManagerLocal operationManager = LookupUtil.getOperationManager();
    private MeasurementDefinitionManagerLocal measurementDefinitionManager = LookupUtil
        .getMeasurementDefinitionManager();

    @Override
    protected void init() throws Exception {
        Subject subject = EnterpriseFacesContextUtility.getSubject();

        String clusterKeyString = FacesContextUtility.getOptionalRequestParameter("contextClusterKey");
        String groupIdString = FacesContextUtility.getOptionalRequestParameter("contextGroupId");
        currentParentResourceGroupId = FacesContextUtility.getOptionalRequestParameter("contextParentGroupId");

        currentResourceGroup = null;

        if (clusterKeyString != null) {
            ClusterKey key = ClusterKey.valueOf(clusterKeyString);
            currentResourceGroup = clusterManager.createAutoClusterBackingGroup(subject, key, false);

        } else if (groupIdString != null) {
            int groupId = Integer.parseInt(groupIdString);
            currentResourceGroup = groupManager.getResourceGroupById(subject, groupId, null);

        }

        if (currentResourceGroup != null) {
            List<MeasurementDefinition> definitions = measurementDefinitionManager
                .getMeasurementDefinitionsByResourceType(subject, currentResourceGroup.getResourceType().getId(),
                    DataType.MEASUREMENT, null);

            // operations menugroup, lazy-loaded entries because only name/id are needed for display
            List<OperationDefinition> operations = operationManager.getSupportedResourceTypeOperations(subject,
                currentResourceGroup.getResourceType().getId(), false);

            menuItemDescriptorsForView = createViewMenuItemDescriptors(currentResourceGroup, definitions);
            metricMenuItemDescriptorsForGraph = createGraphMenuItemDescriptors(currentResourceGroup, definitions);
            menuItemDescriptorsForOperations = createOperationMenuItemDescriptors(currentResourceGroup.getId(),
                currentParentResourceGroupId, operations);
        } else {
            menuItemDescriptorsForView = null;
            metricMenuItemDescriptorsForGraph = null;
            menuItemDescriptorsForOperations = null;
        }
    }

    @Override
    protected List<String> getMenuHeaders() {
        return Collections.singletonList(currentResourceGroup.getName());
    }

    @Override
    protected QuickLinksDescriptor getMenuQuickLinks() {
        QuickLinksDescriptor descriptor = new QuickLinksDescriptor();

        descriptor.setMenuItemId("menu_groupQuickLinks_" + currentResourceGroup.getId());

        String attributes = "groupId=" + currentResourceGroup.getId();
        if (currentParentResourceGroupId != null) {
            attributes += "&parentGroupId=" + currentParentResourceGroupId;
        }

        descriptor.setMonitoringUrl("/rhq/group/monitor/graphs.xhtml?" + attributes);
        descriptor.setInventoryUrl("/rhq/group/inventory/view.xhtml?" + attributes);
        descriptor.setConfigurationUrl("/rhq/group/configuration/viewCurrent.xhtml?" + attributes);
        descriptor.setOperationUrl("/rhq/group/operation/groupOperationScheduleNew.xhtml?" + attributes);
        descriptor.setEventUrl("/rhq/group/events/history.xhtml?" + attributes);

        return descriptor;
    }

    @Override
    protected List<MenuItemDescriptor> getViewChartsMenuItems() {
        return menuItemDescriptorsForView;
    }

    @Override
    protected List<MetricMenuItemDescriptor> getGraphToViewMenuItems() {
        return metricMenuItemDescriptorsForGraph;
    }

    @Override
    protected List<MenuItemDescriptor> getOperationsMenuItems() {
        return menuItemDescriptorsForOperations;
    }

    @Override
    protected int getResourceTypeId() {
        return currentResourceGroup.getResourceType().getId();
    }

    @Override
    protected boolean shouldCreateMenu() {
        return currentResourceGroup != null && currentResourceGroup.getResourceType() != null;
    }

    @Override
    protected void addAdditionalMenuItems(ContextMenu menu) {
        List<Resource> resources = null;
        if (currentResourceGroup.getClusterKey() != null) {
            resources = clusterManager.getAutoClusterResources(EnterpriseFacesContextUtility.getSubject(), ClusterKey
                .valueOf(currentResourceGroup.getClusterKey()));
        } else {
            resources = groupManager.findResourcesForResourceGroup(EnterpriseFacesContextUtility.getSubject(),
                currentResourceGroup.getId(), null);
        }

        HtmlMenuGroup membersMenuItem = new HtmlMenuGroup();
        membersMenuItem.setValue("Members");
        membersMenuItem.setId("menu_groupMembers_" + currentResourceGroup.getId());
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

        menu.getChildren().add(membersMenuItem);
    }

    private List<MetricMenuItemDescriptor> createGraphMenuItemDescriptors(ResourceGroup group,
        List<MeasurementDefinition> definitions) {

        List<MetricMenuItemDescriptor> ret = new ArrayList<MetricMenuItemDescriptor>();

        for (MeasurementDefinition definition : definitions) {
            MetricMenuItemDescriptor descriptor = new MetricMenuItemDescriptor();

            fillBasicMetricMenuItemDescriptor(descriptor, group.getId(), "measurementGraphMenuItem_", definition);
            descriptor.setMetricToken("cg," + group.getId() + "," + definition.getId());

            ret.add(descriptor);
        }

        return ret;
    }

    private List<MenuItemDescriptor> createViewMenuItemDescriptors(ResourceGroup group,
        List<MeasurementDefinition> definitions) {

        List<MenuItemDescriptor> ret = new ArrayList<MenuItemDescriptor>();

        for (MeasurementDefinition definition : definitions) {
            MenuItemDescriptor descriptor = new MenuItemDescriptor();

            fillBasicMetricMenuItemDescriptor(descriptor, group.getId(), "measurementChartMenuItem_", definition);

            ret.add(descriptor);
        }

        return ret;
    }

    private List<MenuItemDescriptor> createOperationMenuItemDescriptors(int groupId, String parentGroupId,
        List<OperationDefinition> operations) {
        List<MenuItemDescriptor> ret = new ArrayList<MenuItemDescriptor>();

        for (OperationDefinition def : operations) {
            MenuItemDescriptor descriptor = new MenuItemDescriptor();
            descriptor.setMenuItemId("operation_" + def.getId());
            descriptor.setName(def.getDisplayName());

            String url = "/rhq/group/operation/groupOperationScheduleNew.xhtml";
            url += "?opId=" + def.getId();
            url += "&groupId=" + groupId;
            if (parentGroupId != null) {
                url += "&parentGroupId=" + parentGroupId;
            }
            descriptor.setUrl(url);

            ret.add(descriptor);
        }

        return ret;
    }

    private void fillBasicMetricMenuItemDescriptor(MenuItemDescriptor descriptor, int groupId, String idPrefix,
        MeasurementDefinition definition) {

        descriptor.setMenuItemId(idPrefix + definition.getId());
        descriptor.setName(definition.getDisplayName());
        String url = "/resource/common/monitor/Visibility.do";
        url += "?mode=chartSingleMetricSingleResource";
        url += "&m=" + definition.getId();
        url += "&groupId=" + groupId;

        descriptor.setUrl(url);
    }

}
