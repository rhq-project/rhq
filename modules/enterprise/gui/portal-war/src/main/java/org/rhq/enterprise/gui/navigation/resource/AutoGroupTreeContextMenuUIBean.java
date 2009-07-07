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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.resource.group.composite.AutoGroupComposite;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.navigation.contextmenu.MenuItemDescriptor;
import org.rhq.enterprise.gui.navigation.contextmenu.MetricMenuItemDescriptor;
import org.rhq.enterprise.gui.navigation.contextmenu.QuickLinksDescriptor;
import org.rhq.enterprise.gui.navigation.contextmenu.TreeContextMenuBase;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.measurement.MeasurementDefinitionManagerLocal;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Backing bean for the tree context menu of auto groups.
 * 
 * @author Lukas Krejci
 */
public class AutoGroupTreeContextMenuUIBean extends TreeContextMenuBase {

    private AutoGroupComposite currentAutoGroup;
    private List<MenuItemDescriptor> metricMenuItemDescriptorsForView;
    private List<MetricMenuItemDescriptor> metricMenuItemDescriptorsForGraph;

    private ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();
    private MeasurementDefinitionManagerLocal measurementDefinitionManager = LookupUtil
        .getMeasurementDefinitionManager();

    @Override
    protected void init() throws Exception {
        Subject subject = EnterpriseFacesContextUtility.getSubject();

        String parentIdString = FacesContextUtility.getOptionalRequestParameter("contextParentResourceId");
        String resourceTypeIdString = FacesContextUtility.getOptionalRequestParameter("contextAutoGroupResourceTypeId");

        if (parentIdString != null && resourceTypeIdString != null) {
            int parentId = Integer.parseInt(parentIdString);
            int resourceTypeId = Integer.parseInt(resourceTypeIdString);

            currentAutoGroup = getAutoGroupForResourceType(subject, parentId, resourceTypeId);

            List<MeasurementDefinition> definitions = measurementDefinitionManager
                .getMeasurementDefinitionsByResourceType(subject, resourceTypeId, DataType.MEASUREMENT, null);

            metricMenuItemDescriptorsForView = createViewMenuItemDescriptors(currentAutoGroup, definitions);
            metricMenuItemDescriptorsForGraph = createGraphMenuItemDescriptors(currentAutoGroup, definitions);
        } else {
            currentAutoGroup = null;
            metricMenuItemDescriptorsForView = null;
            metricMenuItemDescriptorsForGraph = null;
        }
    }

    @Override
    protected List<String> getMenuHeaders() {
        String name = currentAutoGroup.getResourceType().getName();

        return Collections.singletonList(name + " (" + currentAutoGroup.getMemberCount() + ")");
    }

    @Override
    protected QuickLinksDescriptor getMenuQuickLinks() {
        int parentId = currentAutoGroup.getParentResource().getId();
        int resourceTypeId = currentAutoGroup.getResourceType().getId();

        QuickLinksDescriptor descriptor = new QuickLinksDescriptor();
        descriptor.setMenuItemId("menu_ag_" + parentId + "_" + resourceTypeId);

        descriptor.setMonitoringUrl("/rhq/autogroup/monitor/graphs.xhtml?parent=" + parentId + "&type="
            + resourceTypeId);
        descriptor.setEventUrl("/rhq/autogroup/events/history.xhtml?parent=" + parentId + "&type=" + resourceTypeId);

        return descriptor;
    }

    @Override
    protected List<MenuItemDescriptor> getViewChartsMenuItems() {
        return metricMenuItemDescriptorsForView;
    }

    @Override
    protected List<MetricMenuItemDescriptor> getGraphToViewMenuItems() {
        return metricMenuItemDescriptorsForGraph;
    }

    @Override
    protected List<MenuItemDescriptor> getOperationsMenuItems() {
        //autogroups don't support operations
        return null;
    }

    @Override
    protected int getResourceTypeId() {
        return currentAutoGroup.getResourceType().getId();
    }

    @Override
    protected boolean shouldCreateMenu() {
        return currentAutoGroup != null;
    }

    private AutoGroupComposite getAutoGroupForResourceType(Subject subject, int parentId, int resourceTypeId) {
        List<AutoGroupComposite> autogroups = resourceManager.findChildrenAutoGroups(subject, parentId,
            new int[] { resourceTypeId });
        if (autogroups == null || autogroups.size() == 0) {
            return null;
        }

        return autogroups.get(0);
    }

    private List<MetricMenuItemDescriptor> createGraphMenuItemDescriptors(AutoGroupComposite autoGroup,
        List<MeasurementDefinition> definitions) {
        List<MetricMenuItemDescriptor> ret = new ArrayList<MetricMenuItemDescriptor>();

        int parentId = autoGroup.getParentResource().getId();
        int resourceTypeId = autoGroup.getResourceType().getId();

        for (MeasurementDefinition definition : definitions) {
            MetricMenuItemDescriptor descriptor = new MetricMenuItemDescriptor();
            fillBasicMetricMenuItemDescriptor(descriptor, parentId, resourceTypeId, "measurementGraphMenuItem_ag_",
                definition);

            descriptor.setMetricToken("ag," + parentId + "," + definition.getId() + "," + resourceTypeId);

            ret.add(descriptor);
        }

        return ret;
    }

    private List<MenuItemDescriptor> createViewMenuItemDescriptors(AutoGroupComposite autoGroup,
        List<MeasurementDefinition> definitions) {
        List<MenuItemDescriptor> ret = new ArrayList<MenuItemDescriptor>();

        int parentId = autoGroup.getParentResource().getId();
        int resourceTypeId = autoGroup.getResourceType().getId();

        for (MeasurementDefinition definition : definitions) {
            MenuItemDescriptor descriptor = new MenuItemDescriptor();
            fillBasicMetricMenuItemDescriptor(descriptor, parentId, resourceTypeId, "measurementChartMenuItem_ag_",
                definition);

            ret.add(descriptor);
        }

        return ret;
    }

    private void fillBasicMetricMenuItemDescriptor(MenuItemDescriptor descriptor, int parentId, int resourceTypeId,
        String idPrefix, MeasurementDefinition definition) {

        descriptor.setMenuItemId(idPrefix + definition.getId());
        descriptor.setName(definition.getDisplayName());

        String url = "/resource/common/monitor/Visibility.do";
        url += "?mode=chartSingleMetricMultiResource";
        url += "&m=" + definition.getId();
        url += "&type=" + resourceTypeId;
        url += "&parent=" + parentId;

        descriptor.setUrl(url);
    }
}
