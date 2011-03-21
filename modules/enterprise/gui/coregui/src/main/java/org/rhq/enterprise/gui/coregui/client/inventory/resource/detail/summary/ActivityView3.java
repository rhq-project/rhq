/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.summary;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.ContentsType;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.form.fields.CanvasItem;
import com.smartgwt.client.widgets.form.fields.LinkItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.bundle.BundleDeployment;
import org.rhq.core.domain.configuration.ResourceConfigurationUpdate;
import org.rhq.core.domain.content.InstalledPackageHistory;
import org.rhq.core.domain.criteria.AlertCriteria;
import org.rhq.core.domain.criteria.InstalledPackageCriteria;
import org.rhq.core.domain.criteria.ResourceBundleDeploymentCriteria;
import org.rhq.core.domain.event.EventSeverity;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;
import org.rhq.core.domain.measurement.composite.MeasurementOOBComposite;
import org.rhq.core.domain.operation.composite.ResourceOperationLastCompletedComposite;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.ResourceTypeFacet;
import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.enterprise.gui.coregui.client.ImageManager;
import org.rhq.enterprise.gui.coregui.client.LinkManager;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.common.detail.summary.AbstractActivityView;
import org.rhq.enterprise.gui.coregui.client.resource.disambiguation.ReportDecorator;
import org.rhq.enterprise.gui.coregui.client.util.BrowserUtility;
import org.rhq.enterprise.gui.coregui.client.util.GwtRelativeDurationConverter;
import org.rhq.enterprise.gui.coregui.client.util.GwtTuple;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;

/**
 * The content pane of the Resource Summary>Activity tab.
 *
 * @author Simeon Pinder
 */
public class ActivityView3 extends AbstractActivityView {

    private ResourceComposite resourceComposite;

    public ActivityView3(String locatorId, ResourceComposite resourceComposite) {
        super(locatorId, null, resourceComposite);
        this.resourceComposite = resourceComposite;
    }

    @Override
    protected void onInit() {
        super.onInit();
        loadData();
    }

    /**Initiates data request.
     */
    protected void loadData() {

        ResourceType type = null;
        Resource resource = null;
        Set<ResourceTypeFacet> facets = null;

        if ((resourceComposite != null) && (resourceComposite.getResource() != null)) {
            resource = resourceComposite.getResource();
            type = this.resourceComposite.getResource().getResourceType();
            facets = this.resourceComposite.getResourceFacets().getFacets();

            //alerts
            getRecentAlerts();
            //operations
            if (facets.contains(ResourceTypeFacet.OPERATION)) {
                getRecentOperations();
            }
            //config updates
            if (facets.contains(ResourceTypeFacet.CONFIGURATION)) {
                getRecentConfigurationUpdates();
            }
            //events
            if (facets.contains(ResourceTypeFacet.EVENT)) {
                getRecentEventUpdates();
            }
            //measurements
            getRecentOobs();
            getRecentPkgHistory();
            getRecentMetrics();

            //conditionally display Bundle Deployments region.
            if (displayBundlesForResource(resource)) {
                getRecentBundleDeployments();
            }
        }
    }

    /** Fetches alerts and updates the DynamicForm instance with the latest
     *  alert information.
     */
    private void getRecentAlerts() {
        final int resourceId = this.resourceComposite.getResource().getId();
        //fetches last five alerts for this resource
        AlertCriteria criteria = new AlertCriteria();
        PageControl pageControl = new PageControl(0, 5);
        pageControl.initDefaultOrderingField("ctime", PageOrdering.DESC);
        criteria.setPageControl(pageControl);
        criteria.addFilterResourceIds(resourceId);
        GWTServiceLookup.getAlertService().findAlertsByCriteria(criteria, new AsyncCallback<PageList<Alert>>() {
            @Override
            public void onSuccess(PageList<Alert> result) {
                VLayout column = new VLayout();
                column.setHeight(10);
                if (!result.isEmpty()) {
                    int rowNum = 0;
                    for (Alert alert : result) {
                        // alert history records do not have a usable locatorId, we'll use rownum, which is unique and
                        // may be repeatable.
                        LocatableDynamicForm row = new LocatableDynamicForm(recentAlertsContent.extendLocatorId(String
                            .valueOf(rowNum++)));
                        row.setNumCols(3);
                        StaticTextItem iconItem = newTextItemIcon(ImageManager.getAlertIcon(alert.getAlertDefinition()
                            .getPriority()), alert.getAlertDefinition().getPriority().getDisplayName());
                        LinkItem link = newLinkItem(alert.getAlertDefinition().getName() + ": ",
                            ReportDecorator.GWT_RESOURCE_URL + resourceId + "/Alerts/History/" + alert.getId());
                        StaticTextItem time = newTextItem(GwtRelativeDurationConverter.format(alert.getCtime()));
                        row.setItems(iconItem, link, time);

                        column.addMember(row);
                    }
                    //link to more details
                    LocatableDynamicForm row = new LocatableDynamicForm(recentAlertsContent.extendLocatorId(String
                        .valueOf(rowNum++)));
                    addSeeMoreLink(row, ReportDecorator.GWT_RESOURCE_URL + resourceId + "/Alerts/History/", column);
                } else {
                    LocatableDynamicForm row = createEmptyDisplayRow(recentAlertsContent.extendLocatorId("None"),
                        RECENT_ALERTS_NONE);
                    column.addMember(row);
                }
                for (Canvas child : recentAlertsContent.getChildren()) {
                    child.destroy();
                }
                recentAlertsContent.addChild(column);
                recentAlertsContent.markForRedraw();
            }

            @Override
            public void onFailure(Throwable caught) {
                Log.debug("Error retrieving recent alerts for resource [" + resourceId + "]:" + caught.getMessage());
            }
        });
    }

    /** Fetches operations and updates the DynamicForm instance with the latest
     *  operation information.
     */
    private void getRecentOperations() {
        final int resourceId = this.resourceComposite.getResource().getId();
        //fetches five most recent operations.
        PageControl pageControl = new PageControl(0, 5);
        pageControl.initDefaultOrderingField("ro.createdTime", PageOrdering.DESC);
        GWTServiceLookup.getOperationService().findRecentCompletedOperations(resourceId, pageControl,
            new AsyncCallback<PageList<ResourceOperationLastCompletedComposite>>() {

                @Override
                public void onFailure(Throwable caught) {
                    Log.debug("Error retrieving recent operations for resource [" + resourceId + "]:"
                        + caught.getMessage());
                }

                @Override
                public void onSuccess(PageList<ResourceOperationLastCompletedComposite> result) {
                    VLayout column = new VLayout();
                    column.setHeight(10);
                    if (!result.isEmpty()) {
                        int rowNum = 0;
                        for (ResourceOperationLastCompletedComposite report : result) {
                            // operation history records do not have a usable locatorId, we'll use rownum, which is unique and
                            // may be repeatable.
                            LocatableDynamicForm row = new LocatableDynamicForm(recentOperationsContent
                                .extendLocatorId(String.valueOf(rowNum)));
                            row.setNumCols(3);

                            StaticTextItem iconItem = newTextItemIcon(ImageManager.getOperationResultsIcon(report
                                .getOperationStatus()), report.getOperationStatus().getDisplayName());
                            LinkItem link = newLinkItem(report.getOperationName() + ": ", LinkManager
                                .getResourceLink(resourceId)
                                + "/Operations/History/" + report.getOperationHistoryId());
                            StaticTextItem time = newTextItem(GwtRelativeDurationConverter.format(report
                                .getOperationStartTime()));
                            row.setItems(iconItem, link, time);

                            column.addMember(row);
                        }
                        //link to more details
                        LocatableDynamicForm row = new LocatableDynamicForm(recentOperationsContent
                            .extendLocatorId(String.valueOf(rowNum++)));
                        addSeeMoreLink(row, ReportDecorator.GWT_RESOURCE_URL + resourceId + "/Operations/History/",
                            column);
                    } else {
                        LocatableDynamicForm row = createEmptyDisplayRow(recentOperationsContent
                            .extendLocatorId("None"), RECENT_OPERATIONS_NONE);
                        column.addMember(row);
                    }
                    for (Canvas child : recentOperationsContent.getChildren()) {
                        child.destroy();
                    }
                    recentOperationsContent.addChild(column);
                    recentOperationsContent.markForRedraw();
                }
            });
    }

    /** Fetches configuration updates and updates the DynamicForm instance with the latest
     *  config change information.
     */
    private void getRecentConfigurationUpdates() {
        final int resourceId = this.resourceComposite.getResource().getId();

        PageControl lastFive = new PageControl(0, 5);
        lastFive.initDefaultOrderingField("cu.createdTime", PageOrdering.DESC);

        GWTServiceLookup.getConfigurationService().findResourceConfigurationUpdates(resourceId, null, null, true,
            lastFive, new AsyncCallback<PageList<ResourceConfigurationUpdate>>() {

                @Override
                public void onFailure(Throwable caught) {
                    Log.debug("Error retrieving recent configuration updates for resource [" + resourceId + "]:"
                        + caught.getMessage());
                }

                @Override
                public void onSuccess(PageList<ResourceConfigurationUpdate> result) {
                    VLayout column = new VLayout();
                    column.setHeight(10);
                    if (!result.isEmpty()) {
                        int rowNum = 0;
                        for (ResourceConfigurationUpdate update : result) {
                            // config update history records do not have a usable locatorId, we'll use rownum, which is unique and
                            // may be repeatable.
                            LocatableDynamicForm row = new LocatableDynamicForm(recentConfigurationContent
                                .extendLocatorId(String.valueOf(rowNum)));
                            row.setNumCols(3);

                            StaticTextItem iconItem = newTextItemIcon(ImageManager.getResourceConfigurationIcon(update
                                .getStatus()), null);
                            String linkTitle = MSG.view_resource_inventory_activity_changed_by() + " "
                                + update.getSubjectName() + ":";
                            if ((update.getSubjectName() == null) || (update.getSubjectName().trim().isEmpty())) {
                                linkTitle = MSG.common_msg_changeAutoDetected();
                            }
                            LinkItem link = newLinkItem(linkTitle, LinkManager.getResourceLink(resourceId)
                                + "/Configuration/History/" + update.getId());
                            StaticTextItem time = newTextItem(GwtRelativeDurationConverter.format(update
                                .getCreatedTime()));

                            row.setItems(iconItem, link, time);
                            column.addMember(row);
                        }
                        //link to more details
                        LocatableDynamicForm row = new LocatableDynamicForm(recentConfigurationContent
                            .extendLocatorId(String.valueOf(rowNum++)));
                        addSeeMoreLink(row, ReportDecorator.GWT_RESOURCE_URL + resourceId + "/Configuration/History/",
                            column);
                    } else {
                        LocatableDynamicForm row = createEmptyDisplayRow(recentConfigurationContent
                            .extendLocatorId("None"), RECENT_CONFIGURATIONS_NONE);
                        column.addMember(row);
                    }
                    //cleanup
                    for (Canvas child : recentConfigurationContent.getChildren()) {
                        child.destroy();
                    }
                    recentConfigurationContent.addChild(column);
                    recentConfigurationContent.markForRedraw();
                }
            });
    }

    /** Fetches recent events and updates the DynamicForm instance with the latest
     *  event information over last 24hrs.
     */
    private void getRecentEventUpdates() {

        final int resourceId = this.resourceComposite.getResource().getId();
        long now = System.currentTimeMillis();
        long nowMinus24Hours = now - (24 * 60 * 60 * 1000);

        GWTServiceLookup.getEventService().getEventCountsBySeverity(resourceId, nowMinus24Hours, now,
            new AsyncCallback<Map<EventSeverity, Integer>>() {

                @Override
                public void onFailure(Throwable caught) {
                    Log.debug("Error retrieving recent event counts for resource [" + resourceId + "]:"
                        + caught.getMessage());
                }

                @Override
                public void onSuccess(Map<EventSeverity, Integer> eventCounts) {
                    //Now populated Tuples
                    List<GwtTuple<EventSeverity, Integer>> results = new ArrayList<GwtTuple<EventSeverity, Integer>>();
                    for (EventSeverity severity : eventCounts.keySet()) {
                        int count = eventCounts.get(severity);
                        if (count > 0) {
                            results.add(new GwtTuple<EventSeverity, Integer>(severity, count));
                        }
                    }
                    //build display
                    VLayout column = new VLayout();
                    column.setHeight(10);

                    if (!results.isEmpty()) {
                        int rowNum = 0;
                        for (GwtTuple<EventSeverity, Integer> tuple : results) {
                            // event history records do not have a usable locatorId, we'll use rownum, which is unique and
                            // may be repeatable.
                            LocatableDynamicForm row = new LocatableDynamicForm(recentEventsContent
                                .extendLocatorId(String.valueOf(rowNum)));
                            row.setNumCols(2);
                            row.setWidth(10);//pack.

                            //icon
                            StaticTextItem iconItem = newTextItemIcon(ImageManager.getEventSeverityIcon(tuple
                                .getLefty()), tuple.getLefty().name());
                            //count
                            StaticTextItem count = newTextItem(String.valueOf(tuple.righty));
                            row.setItems(iconItem, count);

                            column.addMember(row);
                        }
                        LocatableDynamicForm row = new LocatableDynamicForm(recentEventsContent.extendLocatorId(String
                            .valueOf(rowNum++)));
                        addSeeMoreLink(row, ReportDecorator.GWT_RESOURCE_URL + resourceId + "/Events/History/", column);
                    } else {
                        LocatableDynamicForm row = createEmptyDisplayRow(recentEventsContent.extendLocatorId("None"),
                            RECENT_EVENTS_NONE);
                        column.addMember(row);
                    }
                    //cleanup
                    for (Canvas child : recentEventsContent.getChildren()) {
                        child.destroy();
                    }
                    recentEventsContent.addChild(column);
                    recentEventsContent.markForRedraw();
                }
            });
    }

    /** Fetches OOB measurements and updates the DynamicForm instance with the latest 5
     *  oob change details.
     */
    private void getRecentOobs() {
        final int resourceId = this.resourceComposite.getResource().getId();
        GWTServiceLookup.getMeasurementDataService().getHighestNOOBsForResource(resourceId, 5,
            new AsyncCallback<PageList<MeasurementOOBComposite>>() {
                @Override
                public void onFailure(Throwable caught) {
                    Log.debug("Error retrieving recent out of bound metrics for resource [" + resourceId + "]:"
                        + caught.getMessage());
                }

                @Override
                public void onSuccess(PageList<MeasurementOOBComposite> result) {
                    VLayout column = new VLayout();
                    column.setHeight(10);
                    if (!result.isEmpty()) {
                        for (MeasurementOOBComposite oob : result) {
                            LocatableDynamicForm row = new LocatableDynamicForm(recentOobContent.extendLocatorId(oob
                                .getScheduleName()));
                            row.setNumCols(2);

                            String title = oob.getScheduleName() + ":";
                            String destination = "/resource/common/monitor/Visibility.do?m=" + oob.getDefinitionId()
                                + "&id=" + resourceId + "&mode=chartSingleMetricSingleResource";
                            LinkItem link = newLinkItem(title, destination);
                            StaticTextItem time = newTextItem(GwtRelativeDurationConverter.format(oob.getTimestamp()));

                            row.setItems(link, time);
                            column.addMember(row);
                        }
                    } else {
                        LocatableDynamicForm row = createEmptyDisplayRow(recentOobContent.extendLocatorId("None"),
                            RECENT_OOB_NONE);
                        column.addMember(row);
                    }
                    recentOobContent.setContents("");
                    for (Canvas child : recentOobContent.getChildren()) {
                        child.destroy();
                    }
                    recentOobContent.addChild(column);
                    recentOobContent.markForRedraw();
                }
            });
    }

    /** Fetches recent package history information and updates the DynamicForm instance with details.
     */
    private void getRecentPkgHistory() {
        final int resourceId = this.resourceComposite.getResource().getId();
        InstalledPackageCriteria criteria = new InstalledPackageCriteria();
        criteria.addFilterResourceId(resourceId);
        PageControl pageControl = new PageControl(0, 5);
        criteria.setPageControl(pageControl);

        GWTServiceLookup.getContentService().getInstalledPackageHistoryForResource(resourceId, 5,
            new AsyncCallback<PageList<InstalledPackageHistory>>() {
                @Override
                public void onFailure(Throwable caught) {
                    Log.debug("Error retrieving installed package history for resource [" + resourceId + "]:"
                        + caught.getMessage());
                }

                @Override
                public void onSuccess(PageList<InstalledPackageHistory> result) {
                    VLayout column = new VLayout();
                    column.setHeight(10);
                    if (!result.isEmpty()) {
                        for (InstalledPackageHistory history : result) {
                            LocatableDynamicForm row = new LocatableDynamicForm(recentPkgHistoryContent
                                .extendLocatorId(history.getPackageVersion().getFileName()
                                    + history.getPackageVersion().getVersion()));
                            row.setNumCols(3);

                            StaticTextItem iconItem = newTextItemIcon("subsystems/content/Package_16.png", null);
                            String title = history.getPackageVersion().getFileName() + ":";
                            String destination = "/rhq/resource/content/audit-trail-item.xhtml?id=" + resourceId
                                + "&selectedHistoryId=" + history.getId();
                            LinkItem link = newLinkItem(title, destination);
                            StaticTextItem time = newTextItem(GwtRelativeDurationConverter.format(history
                                .getTimestamp()));

                            row.setItems(iconItem, link, time);
                            column.addMember(row);
                        }
                        //                        //insert see more link
                        //                        LocatableDynamicForm row = new LocatableDynamicForm(recentPkgHistoryContent
                        //                            .extendLocatorId("RecentPkgHistorySeeMore"));
                        //                        String destination = "/rhq/resource/content/audit-trail-item.xhtml?id=" + resourceId;
                        //                        addSeeMoreLink(row, destination, column);
                    } else {
                        LocatableDynamicForm row = createEmptyDisplayRow(recentPkgHistoryContent
                            .extendLocatorId("None"), RECENT_PKG_HISTORY_NONE);
                        column.addMember(row);
                    }
                    //cleanup
                    for (Canvas child : recentPkgHistoryContent.getChildren()) {
                        child.destroy();
                    }
                    recentPkgHistoryContent.addChild(column);
                    recentPkgHistoryContent.markForRedraw();
                }
            });
    }

    /** Fetches recent metric information and updates the DynamicForm instance with i)sparkline information,
     * ii) link to recent metric graph for more details and iii) last metric value formatted to show significant
     * digits.
     */
    private void getRecentMetrics() {
        //display container
        final VLayout column = new VLayout();
        column.setHeight(10);//pack
        final int resourceId = this.resourceComposite.getResource().getId();

        //retrieve all relevant measurement definition ids.
        Set<MeasurementDefinition> definitions = this.resourceComposite.getResource().getResourceType()
            .getMetricDefinitions();

        //build id mapping for measurementDefinition instances Ex. Free Memory -> MeasurementDefinition[100071]
        final HashMap<String, MeasurementDefinition> measurementDefMap = new HashMap<String, MeasurementDefinition>();
        for (MeasurementDefinition definition : definitions) {
            measurementDefMap.put(definition.getDisplayName(), definition);
        }

        //bundle definition ids for asynch call.
        int[] definitionArrayIds = new int[definitions.size()];
        final String[] displayOrder = new String[definitions.size()];
        measurementDefMap.keySet().toArray(displayOrder);
        //sort the charting data ex. Free Memory, Free Swap Space,..System Load
        Arrays.sort(displayOrder);

        //organize definitionArrayIds for ordered request on server.
        int index = 0;
        for (String definitionToDisplay : displayOrder) {
            definitionArrayIds[index++] = measurementDefMap.get(definitionToDisplay).getId();
        }
        //make the asynchronous call for all the measurement data
        GWTServiceLookup.getMeasurementDataService().findDataForResource(resourceId, definitionArrayIds,
            System.currentTimeMillis() - (1000L * 60 * 60 * 8), System.currentTimeMillis(), 60,
            new AsyncCallback<List<List<MeasurementDataNumericHighLowComposite>>>() {
                @Override
                public void onFailure(Throwable caught) {
                    Log.debug("Error retrieving recent metrics charting data for resource [" + resourceId + "]:"
                        + caught.getMessage());
                }

                @Override
                public void onSuccess(List<List<MeasurementDataNumericHighLowComposite>> results) {
                    if (!results.isEmpty()) {
                        boolean someChartedData = false;
                        //iterate over the retrieved charting data
                        for (int index = 0; index < displayOrder.length; index++) {

                            //retrieve the correct measurement definition
                            MeasurementDefinition md = measurementDefMap.get(displayOrder[index]);

                            //load the data results for the given metric definition
                            List<MeasurementDataNumericHighLowComposite> data = results.get(index);

                            //locate last and minimum values.
                            double lastValue = -1;
                            double minValue = Double.MAX_VALUE;//
                            for (MeasurementDataNumericHighLowComposite d : data) {
                                if ((!Double.isNaN(d.getValue()))
                                    && (String.valueOf(d.getValue()).indexOf("NaN") == -1)) {
                                    if (d.getValue() < minValue) {
                                        minValue = d.getValue();
                                    }
                                    lastValue = d.getValue();
                                }
                            }

                            //collapse the data into comma delimited list for consumption by third party javascript library(jquery.sparkline)
                            String commaDelimitedList = "";

                            for (MeasurementDataNumericHighLowComposite d : data) {
                                if ((!Double.isNaN(d.getValue()))
                                    && (String.valueOf(d.getValue()).indexOf("NaN") == -1)) {
                                    commaDelimitedList += d.getValue() + ",";
                                }
                            }
                            LocatableDynamicForm row = new LocatableDynamicForm(recentMeasurementsContent
                                .extendLocatorId(md.getName()));
                            row.setNumCols(3);
                            HTMLFlow graph = new HTMLFlow();
                            //                        String contents = "<span id='sparkline_" + index + "' class='dynamicsparkline' width='0'>"
                            //                            + commaDelimitedList + "</span>";
                            String contents = "<span id='sparkline_" + index + "' class='dynamicsparkline' width='0' "
                                + "values='" + commaDelimitedList + "'>...</span>";
                            graph.setContents(contents);
                            graph.setContentsType(ContentsType.PAGE);
                            //diable scrollbars on span
                            graph.setScrollbarSize(0);

                            CanvasItem graphContainer = new CanvasItem();
                            graphContainer.setShowTitle(false);
                            graphContainer.setHeight(16);
                            graphContainer.setWidth(60);
                            graphContainer.setCanvas(graph);

                            //Link/title element
                            //TODO: spinder, change link whenever portal.war/graphing is removed.
                            String title = md.getDisplayName() + ":";
                            String destination = "/resource/common/monitor/Visibility.do?mode=chartSingleMetricSingleResource&id="
                                + resourceId + "&m=" + md.getId();
                            LinkItem link = newLinkItem(title, destination);

                            //Value
                            String convertedValue = lastValue + " " + md.getUnits();
                            convertedValue = convertLastValueForDisplay(lastValue, md);
                            StaticTextItem value = newTextItem(convertedValue);

                            row.setItems(graphContainer, link, value);
                            //if graph content returned
                            if ((md.getName().trim().indexOf("Trait.") == -1) && (lastValue != -1)) {
                                column.addMember(row);
                                someChartedData = true;
                            }
                        }
                        if (!someChartedData) {// when there are results but no chartable entries.
                            LocatableDynamicForm row = createEmptyDisplayRow(recentMeasurementsContent
                                .extendLocatorId("None"), RECENT_MEASUREMENTS_NONE);
                            column.addMember(row);
                        } else {
                            //insert see more link
                            LocatableDynamicForm row = new LocatableDynamicForm(recentMeasurementsContent
                                .extendLocatorId("RecentMeasurementsContentSeeMore"));
                            addSeeMoreLink(row, ReportDecorator.GWT_RESOURCE_URL + resourceId + "/Monitoring/Graphs/",
                                column);
                        }
                        //call out to 3rd party javascript lib
                        BrowserUtility.graphSparkLines();
                    } else {
                        LocatableDynamicForm row = createEmptyDisplayRow(recentMeasurementsContent
                            .extendLocatorId("None"), RECENT_MEASUREMENTS_NONE);
                        column.addMember(row);
                    }
                }
            });

        //cleanup
        for (Canvas child : recentMeasurementsContent.getChildren()) {
            child.destroy();
        }
        recentMeasurementsContent.addChild(column);
        recentMeasurementsContent.markForRedraw();
    }

    /** Fetches recent bundle deployment information and updates the DynamicForm instance with details.
     */
    protected void getRecentBundleDeployments() {
        final int resourceId = this.resourceComposite.getResource().getId();
        ResourceBundleDeploymentCriteria criteria = new ResourceBundleDeploymentCriteria();
        PageControl pageControl = new PageControl(0, 5);
        criteria.setPageControl(pageControl);
        criteria.addFilterResourceIds(resourceId);
        criteria.addSortStatus(PageOrdering.DESC);
        criteria.fetchDestination(true);
        criteria.fetchBundleVersion(true);
        criteria.fetchResourceDeployments(true);

        GWTServiceLookup.getBundleService().findBundleDeploymentsByCriteria(criteria,
            new AsyncCallback<PageList<BundleDeployment>>() {
                @Override
                public void onFailure(Throwable caught) {
                    Log.debug("Error retrieving installed bundle deployments for resource [" + resourceId + "]:"
                        + caught.getMessage());
                }

                @Override
                public void onSuccess(PageList<BundleDeployment> result) {
                    VLayout column = new VLayout();
                    column.setHeight(10);
                    if (!result.isEmpty()) {
                        for (BundleDeployment deployment : result) {
                            LocatableDynamicForm row = new LocatableDynamicForm(recentBundleDeployContent
                                .extendLocatorId(deployment.getBundleVersion().getName()
                                    + deployment.getBundleVersion().getVersion()));
                            row.setNumCols(3);

                            StaticTextItem iconItem = newTextItemIcon("subsystems/content/Content_16.png", null);
                            String title = deployment.getBundleVersion().getName() + "["
                                + deployment.getBundleVersion().getVersion() + "]:";

                            String destination = ReportDecorator.GWT_BUNDLE_URL
                                + deployment.getBundleVersion().getBundle().getId() + "/destinations/"
                                + deployment.getDestination().getId();
                            LinkItem link = newLinkItem(title, destination);
                            StaticTextItem time = newTextItem(GwtRelativeDurationConverter
                                .format(deployment.getCtime()));

                            row.setItems(iconItem, link, time);
                            column.addMember(row);
                        }
                        //insert see more link
                        //TODO: spinder:2/25/11 (add this later) no current view for seeing all bundle deployments
                        //                        LocatableDynamicForm row = new LocatableDynamicForm(recentBundleDeployContent.extendLocatorId("RecentBundleContentSeeMore"));
                        //                        addSeeMoreLink(row, LinkManager.getResourceGroupLink(groupId) + "/Events/History/", column);
                    } else {
                        LocatableDynamicForm row = createEmptyDisplayRow(recentBundleDeployContent
                            .extendLocatorId("None"), RECENT_BUNDLE_DEPLOY_NONE);
                        column.addMember(row);
                    }
                    //cleanup
                    for (Canvas child : recentBundleDeployContent.getChildren()) {
                        child.destroy();
                    }
                    recentBundleDeployContent.addChild(column);
                    recentBundleDeployContent.markForRedraw();
                }
            });
    }
}
