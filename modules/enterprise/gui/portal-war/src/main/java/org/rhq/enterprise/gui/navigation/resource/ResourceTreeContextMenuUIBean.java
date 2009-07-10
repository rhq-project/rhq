package org.rhq.enterprise.gui.navigation.resource;

import java.util.ArrayList;
import java.util.List;

import javax.faces.context.FacesContext;

import org.richfaces.component.UITree;
import org.richfaces.event.NodeSelectedEvent;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.composite.ResourceWithAvailability;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.navigation.contextmenu.MenuItemDescriptor;
import org.rhq.enterprise.gui.navigation.contextmenu.MetricMenuItemDescriptor;
import org.rhq.enterprise.gui.navigation.contextmenu.QuickLinksDescriptor;
import org.rhq.enterprise.gui.navigation.contextmenu.TreeContextMenuBase;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.measurement.MeasurementScheduleManagerLocal;
import org.rhq.enterprise.server.operation.OperationManagerLocal;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class ResourceTreeContextMenuUIBean extends TreeContextMenuBase {

    private OperationManagerLocal operationManager = LookupUtil.getOperationManager();
    private MeasurementScheduleManagerLocal measurementScheduleManager = LookupUtil.getMeasurementScheduleManager();
    private ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();

    private Resource currentResource;
    private List<MenuItemDescriptor> menuItemDescriptorsForView;
    private List<MetricMenuItemDescriptor> metricMenuItemDescriptorsForGraph;
    private List<MenuItemDescriptor> menuItemDescriptorsForOperations;

    @Override
    protected void init() throws Exception {
        Subject subject = EnterpriseFacesContextUtility.getSubject();

        String resourceIdString = FacesContextUtility.getOptionalRequestParameter("contextResourceId");
        if (resourceIdString != null) {
            int resourceId = Integer.parseInt(resourceIdString);

            //The resource is taken from ResourceManager because of error when the AutoGroup is selected in the menu
            // the resource is not in Faces Context
            //Resource res = EnterpriseFacesContextUtility.getResource();

            currentResource = resourceManager.getResourceById(subject, resourceId);

            List<MeasurementSchedule> schedules = measurementScheduleManager.findMeasurementSchedulesForResourceAndType(
                subject, resourceId, DataType.MEASUREMENT, null, true);

            // operations menu, lazy-loaded entries because only name/id are needed for display 
            List<OperationDefinition> operations = operationManager.getSupportedResourceTypeOperations(subject,
                currentResource.getResourceType().getId(), false);

            menuItemDescriptorsForView = createViewMenuItemDescriptors(resourceId, schedules);
            metricMenuItemDescriptorsForGraph = createGraphMenuItemDescriptors(resourceId, schedules);
            menuItemDescriptorsForOperations = createOperationMenuItemDescriptors(resourceId, operations);
        } else {
            currentResource = null;
            menuItemDescriptorsForView = null;
            metricMenuItemDescriptorsForGraph = null;
            menuItemDescriptorsForOperations = null;
        }
    }

    @Override
    protected List<String> getMenuHeaders() {
        List<String> ret = new ArrayList<String>();

        ret.add(currentResource.getName());
        ret.add(currentResource.getResourceType().getName());

        return ret;
    }

    @Override
    protected QuickLinksDescriptor getMenuQuickLinks() {
        int resourceId = currentResource.getId();

        QuickLinksDescriptor descriptor = new QuickLinksDescriptor();

        descriptor.setMenuItemId("menu_res_" + resourceId);
        descriptor.setMonitoringUrl("/rhq/resource/monitor/graphs.xhtml?id=" + resourceId);
        descriptor.setInventoryUrl("/rhq/resource/inventory/view.xhtml?id=" + resourceId);
        descriptor.setAlertsUrl("/rhq/resource/alert/listAlertDefinitions.xhtml?id=" + resourceId);
        descriptor.setConfigurationUrl("/rhq/resource/configuration/view.xhtml?id=" + resourceId);
        descriptor.setOperationUrl("/rhq/resource/operation/resourceOperationScheduleNew.xhtml?id=" + resourceId);
        descriptor.setEventUrl("/rhq/resource/events/history.xhtml?id=" + resourceId);
        descriptor.setContentUrl("/rhq/resource/content/view.xhtml?id=" + resourceId);

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
        return currentResource.getResourceType().getId();
    }

    @Override
    protected boolean shouldCreateMenu() {
        return currentResource != null;
    }

    private List<MetricMenuItemDescriptor> createGraphMenuItemDescriptors(int resourceId,
        List<MeasurementSchedule> schedules) {
        List<MetricMenuItemDescriptor> ret = new ArrayList<MetricMenuItemDescriptor>();

        for (MeasurementSchedule schedule : schedules) {
            MetricMenuItemDescriptor descriptor = new MetricMenuItemDescriptor();
            fillBasicMetricMenuItemDescriptor(descriptor, resourceId, "measurementGraphMenuItem_", schedule);

            descriptor.setMetricToken(resourceId + "," + schedule.getId());

            ret.add(descriptor);
        }

        return ret;
    }

    private List<MenuItemDescriptor> createViewMenuItemDescriptors(int resourceId, List<MeasurementSchedule> schedules) {
        List<MenuItemDescriptor> ret = new ArrayList<MenuItemDescriptor>();

        for (MeasurementSchedule schedule : schedules) {
            MenuItemDescriptor descriptor = new MenuItemDescriptor();
            fillBasicMetricMenuItemDescriptor(descriptor, resourceId, "measurementChartMenuItem_", schedule);

            ret.add(descriptor);
        }

        return ret;
    }

    private List<MenuItemDescriptor> createOperationMenuItemDescriptors(int resourceId,
        List<OperationDefinition> operations) {
        List<MenuItemDescriptor> ret = new ArrayList<MenuItemDescriptor>();

        for (OperationDefinition def : operations) {
            MenuItemDescriptor descriptor = new MenuItemDescriptor();
            descriptor.setMenuItemId("operation_" + def.getId());
            descriptor.setName(def.getDisplayName());

            String url = "/rhq/resource/operation/resourceOperationScheduleNew.xhtml";
            url += "?id=" + resourceId;
            url += "&opId=" + def.getId();

            descriptor.setUrl(url);

            ret.add(descriptor);
        }

        return ret;
    }

    private void fillBasicMetricMenuItemDescriptor(MenuItemDescriptor descriptor, int resourceId, String idPrefix,
        MeasurementSchedule schedule) {

        descriptor.setMenuItemId(idPrefix + schedule.getId());
        descriptor.setName(schedule.getDefinition().getDisplayName());
        String url = "/resource/common/monitor/Visibility.do";
        url += "?mode=chartSingleMetricSingleResource";
        url += "&m=" + schedule.getDefinition().getId();
        url += "&id=" + resourceId;

        descriptor.setUrl(url);
    }

    //XXX what is this for?
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
}
