package org.rhq.enterprise.gui.navigation.resource;

import java.util.List;

import javax.faces.component.html.HtmlGraphicImage;
import javax.faces.component.html.HtmlOutputLink;
import javax.faces.context.FacesContext;

import org.richfaces.component.UITree;
import org.richfaces.component.html.ContextMenu;
import org.richfaces.component.html.HtmlMenuGroup;
import org.richfaces.component.html.HtmlMenuItem;
import org.richfaces.component.html.HtmlMenuSeparator;
import org.richfaces.event.NodeSelectedEvent;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.composite.ResourceFacets;
import org.rhq.core.domain.resource.composite.ResourceWithAvailability;
import org.rhq.core.gui.util.FacesComponentUtility;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.measurement.MeasurementScheduleManagerLocal;
import org.rhq.enterprise.server.operation.OperationManagerLocal;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.resource.ResourceTypeManagerLocal;
import org.rhq.enterprise.server.resource.ResourceTypeNotFoundException;
import org.rhq.enterprise.server.util.LookupUtil;

public class ResourceTreeContextMenuUIBean {

    private static final String STYLE_QUICK_LINKS_ICON = "margin: 2px;";

    private ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();
    private ResourceTypeManagerLocal resourceTypeManager = LookupUtil.getResourceTypeManager();
    private OperationManagerLocal operationManager = LookupUtil.getOperationManager();
    private MeasurementScheduleManagerLocal measurementScheduleManager = LookupUtil.getMeasurementScheduleManager();

    private ContextMenu resourceContextMenu;

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

    public void setMenu(ContextMenu menu) throws ResourceTypeNotFoundException {
        this.resourceContextMenu = menu;

        this.resourceContextMenu.getChildren().clear();

        Subject subject = EnterpriseFacesContextUtility.getSubject();

        String resourceIdString = FacesContextUtility.getOptionalRequestParameter("contextResourceId");
        String resourceTypeIdString = FacesContextUtility.getOptionalRequestParameter("contextResourceTypeId");
        if (resourceTypeIdString != null) {
            int resourceId = Integer.parseInt(resourceIdString);
            int resourceTypeId = Integer.parseInt(resourceTypeIdString);

            Resource res = resourceManager.getResourceById(subject, resourceId);

            // basic information
            addMenuItem(res.getName());
            addMenuItem(res.getResourceType().getName());

            // quick links
            ResourceFacets facets = this.resourceTypeManager.getResourceFacets(subject, resourceTypeId);
            addQuickLinks(resourceIdString, facets);

            // separator bar
            this.resourceContextMenu.getChildren().add(new HtmlMenuSeparator());

            // measurement menu
            List<MeasurementSchedule> schedules = measurementScheduleManager.getMeasurementSchedulesForResourceAndType(
                subject, resourceId, DataType.MEASUREMENT, null, true);
            addMeasurementsMenu(resourceIdString, schedules);

            // operations menu
            List<OperationDefinition> operations = operationManager.getSupportedResourceTypeOperations(subject,
                resourceTypeId);
            addOperationsMenu(resourceIdString, operations);
        }
    }

    private void addMenuItem(String value) {
        HtmlMenuItem nameItem = new HtmlMenuItem();
        nameItem.setValue(value);
        this.resourceContextMenu.getChildren().add(nameItem);
    }

    private void addQuickLinks(String resourceId, ResourceFacets facets) {
        HtmlMenuItem quickLinksItem = new HtmlMenuItem();
        quickLinksItem.setSubmitMode("none");
        quickLinksItem.setId("menu_res_" + resourceId);

        String url;
        HtmlOutputLink link;
        HtmlGraphicImage image;

        if (LookupUtil.getSystemManager().isMonitoringEnabled()) {
            url = "/rhq/resource/monitor/graphs.xhtml?id=" + resourceId;
            link = FacesComponentUtility.addOutputLink(quickLinksItem, null, url);
            image = FacesComponentUtility.addGraphicImage(link, null, "/images/icon_hub_m.gif", "Monitor");
            image.setStyle(STYLE_QUICK_LINKS_ICON);
        }

        url = "/rhq/resource/events/history.xhtml?id=" + resourceId;
        link = FacesComponentUtility.addOutputLink(quickLinksItem, null, url);
        image = FacesComponentUtility.addGraphicImage(link, null, "/images/icon_hub_e.gif", "Events");
        image.setStyle(STYLE_QUICK_LINKS_ICON);

        url = "/rhq/resource/inventory/view.xhtml?id=" + resourceId;
        link = FacesComponentUtility.addOutputLink(quickLinksItem, null, url);
        image = FacesComponentUtility.addGraphicImage(link, null, "/images/icon_hub_i.gif", "Inventory");
        image.setStyle(STYLE_QUICK_LINKS_ICON);

        if (facets.isConfiguration()) {
            url = "/rhq/resource/configuration/view.xhtml?id=" + resourceId;
            link = FacesComponentUtility.addOutputLink(quickLinksItem, null, url);
            image = FacesComponentUtility.addGraphicImage(link, null, "/images/icon_hub_c.gif", "Configuration");
            image.setStyle(STYLE_QUICK_LINKS_ICON);
        }

        if (facets.isOperation()) {
            url = "/rhq/resource/operation/resourceOperationScheduleNew.xhtml?id=" + resourceId;
            link = FacesComponentUtility.addOutputLink(quickLinksItem, null, url);
            image = FacesComponentUtility.addGraphicImage(link, null, "/images/icon_hub_o.gif", "Operations");
            image.setStyle(STYLE_QUICK_LINKS_ICON);
        }

        if (LookupUtil.getSystemManager().isMonitoringEnabled()) {
            url = "/rhq/resource/alert/listAlertDefinitions.xhtml?id=" + resourceId;
            link = FacesComponentUtility.addOutputLink(quickLinksItem, null, url);
            image = FacesComponentUtility.addGraphicImage(link, null, "/images/icon_hub_a.gif", "Alerts");
            image.setStyle(STYLE_QUICK_LINKS_ICON);
        }

        if (facets.isContent()) {
            url = "/rhq/resource/content/view.xhtml?id=" + resourceId;
            link = FacesComponentUtility.addOutputLink(quickLinksItem, null, url);
            image = FacesComponentUtility.addGraphicImage(link, null, "/images/icon_hub_p.gif", "Content");
            image.setStyle(STYLE_QUICK_LINKS_ICON);
        }

        this.resourceContextMenu.getChildren().add(quickLinksItem);
    }

    private void addMeasurementsMenu(String resourceId, List<MeasurementSchedule> schedules) {
        if (schedules != null) {
            HtmlMenuGroup measurementsMenu = new HtmlMenuGroup();
            measurementsMenu.setValue("Measurements");
            this.resourceContextMenu.getChildren().add(measurementsMenu);
            measurementsMenu.setDisabled(schedules.isEmpty());

            for (MeasurementSchedule schedule : schedules) {
                HtmlMenuItem menuItem = new HtmlMenuItem();
                String subOption = schedule.getDefinition().getDisplayName();
                menuItem.setValue(subOption);
                menuItem.setId("measurement_" + schedule.getId());

                String url = "/resource/common/monitor/Visibility.do";
                url += "?mode=chartSingleMetricSingleResource";
                url += "&m=" + schedule.getDefinition().getId();
                url += "&id=" + resourceId;

                menuItem.setSubmitMode("none");
                menuItem.setOnclick("document.location.href='" + url + "'");

                measurementsMenu.getChildren().add(menuItem);
            }
        }
    }

    private void addOperationsMenu(String resourceId, List<OperationDefinition> operations) {
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

                String url = "/rhq/resource/operation/resourceOperationScheduleNew.xhtml";
                url += "?id=" + resourceId;
                url += "&opId=" + def.getId();

                menuItem.setSubmitMode("none");
                menuItem.setOnclick("document.location.href='" + url + "'");

                operationsMenu.getChildren().add(menuItem);
            }
        }
    }
}
