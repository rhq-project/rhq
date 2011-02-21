/*
 * RHQ Management Platform
 * Copyright (C) 2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.summary;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.form.fields.LinkItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.configuration.group.GroupResourceConfigurationUpdate;
import org.rhq.core.domain.criteria.AlertCriteria;
import org.rhq.core.domain.criteria.GroupOperationHistoryCriteria;
import org.rhq.core.domain.criteria.GroupResourceConfigurationUpdateCriteria;
import org.rhq.core.domain.event.EventSeverity;
import org.rhq.core.domain.operation.GroupOperationHistory;
import org.rhq.core.domain.resource.composite.DisambiguationReport;
import org.rhq.core.domain.resource.group.GroupCategory;
import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.enterprise.gui.coregui.client.ImageManager;
import org.rhq.enterprise.gui.coregui.client.LinkManager;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.common.detail.summary.AbstractActivityView;
import org.rhq.enterprise.gui.coregui.client.resource.disambiguation.ReportDecorator;
import org.rhq.enterprise.gui.coregui.client.util.GwtRelativeDurationConverter;
import org.rhq.enterprise.gui.coregui.client.util.GwtTuple;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;

/**
 * The content pane for the group Summary>Activity subtab.
 *
 * @author Simeon Pinder
 */
public class ActivityView2 extends AbstractActivityView {

    private ResourceGroupComposite groupComposite;

    public ActivityView2(String locatorId, ResourceGroupComposite groupComposite) {
        super(locatorId, groupComposite);
        this.groupComposite = groupComposite;
        loadData();
    }

    /**Initiates data request.
     */
    protected void loadData() {
        getRecentAlerts();
        getRecentEventUpdates();
        if ((groupComposite != null)
            && (groupComposite.getResourceGroup().getGroupCategory().equals(GroupCategory.COMPATIBLE))) {//CompatibleGroup
            //TODO: spinder need to drive these calls off off facet availability
            getRecentOperations();
            getRecentConfigurationUpdates();
            //        getRecentOobs();
            //        getRecentPkgHistory();
            //        getRecentMetrics();
        }
    }

    /** Fetches alerts and updates the DynamicForm instance with the latest
     *  alert information.
     */
    private void getRecentAlerts() {
        final int groupId = this.groupComposite.getResourceGroup().getId();
        Integer[] filterGroupAlertDefinitionIds;
        Set<AlertDefinition> alertDefinitions = this.groupComposite.getResourceGroup().getAlertDefinitions();
        filterGroupAlertDefinitionIds = new Integer[alertDefinitions.size()];
        int i = 0;
        for (AlertDefinition def : alertDefinitions) {
            filterGroupAlertDefinitionIds[i++] = def.getId();
        }
        //fetches last five alerts for this resource
        AlertCriteria criteria = new AlertCriteria();
        PageControl pageControl = new PageControl(0, 5);
        pageControl.initDefaultOrderingField("ctime", PageOrdering.DESC);
        criteria.setPageControl(pageControl);
        criteria.addFilterGroupAlertDefinitionIds(filterGroupAlertDefinitionIds);
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
                        LinkItem link = newLinkItem(alert.getAlertDefinition().getName() + ": ", LinkManager
                            .getResourceGroupLink(groupId)
                            + "/Alerts/History/" + alert.getId());
                        StaticTextItem time = newTextItem(GwtRelativeDurationConverter.format(alert.getCtime()));
                        row.setItems(iconItem, link, time);

                        column.addMember(row);
                    }
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
                Log.debug("Error retrieving recent alerts for group [" + groupId + "]:" + caught.getMessage());
            }
        });
    }

    /** Fetches operations and updates the DynamicForm instance with the latest
     *  operation information.
     */
    private void getRecentOperations() {
        final int groupId = this.groupComposite.getResourceGroup().getId();
        //fetches five most recent operations.
        PageControl pageControl = new PageControl(0, 5);

        GroupOperationHistoryCriteria criteria = new GroupOperationHistoryCriteria();
        List<Integer> filterResourceGroupIds = new ArrayList<Integer>();
        filterResourceGroupIds.add(groupId);
        criteria.addFilterResourceGroupIds(filterResourceGroupIds);
        criteria.setPageControl(pageControl);
        criteria.addSortStatus(PageOrdering.DESC);

        GWTServiceLookup.getOperationService().findGroupOperationHistoriesByCriteriaDisambiguated(criteria,
            new AsyncCallback<List<DisambiguationReport<GroupOperationHistory>>>() {

                @Override
                public void onFailure(Throwable caught) {
                    Log.debug("Error retrieving recent operations for group [" + groupId + "]:" + caught.getMessage());
                }

                @Override
                public void onSuccess(List<DisambiguationReport<GroupOperationHistory>> result) {
                    VLayout column = new VLayout();
                    column.setHeight(10);
                    if (!result.isEmpty()) {
                        int rowNum = 0;
                        for (DisambiguationReport<GroupOperationHistory> report : result) {
                            // operation history records do not have a usable locatorId, we'll use rownum, which is unique and
                            // may be repeatable.
                            LocatableDynamicForm row = new LocatableDynamicForm(recentOperationsContent
                                .extendLocatorId(String.valueOf(rowNum)));
                            row.setNumCols(3);

                            StaticTextItem iconItem = newTextItemIcon(ImageManager.getOperationResultsIcon(report
                                .getOriginal().getStatus()), report.getOriginal().getStatus().getDisplayName());
                            LinkItem link = newLinkItem(report.getOriginal().getOperationDefinition().getDisplayName()
                                + ": ", ReportDecorator.GWT_GROUP_URL + groupId + "/Operations/History/"
                                + report.getOriginal().getId());
                            StaticTextItem time = newTextItem(GwtRelativeDurationConverter.format(report.getOriginal()
                                .getStartedTime()));
                            row.setItems(iconItem, link, time);

                            column.addMember(row);
                        }
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
        final int groupId = this.groupComposite.getResourceGroup().getId();

        PageControl lastFive = new PageControl(0, 5);
        GroupResourceConfigurationUpdateCriteria criteria = new GroupResourceConfigurationUpdateCriteria();
        criteria.setPageControl(lastFive);
        criteria.addSortStatus(PageOrdering.DESC);
        List<Integer> filterResourceGroupIds = new ArrayList<Integer>();
        filterResourceGroupIds.add(groupId);
        criteria.addFilterResourceGroupIds(filterResourceGroupIds);

        GWTServiceLookup.getConfigurationService().findGroupResourceConfigurationUpdatesByCriteria(criteria,
            new AsyncCallback<PageList<GroupResourceConfigurationUpdate>>() {

                @Override
                public void onFailure(Throwable caught) {
                    Log.debug("Error retrieving recent configuration updates for group [" + groupId + "]:"
                        + caught.getMessage());
                }

                @Override
                public void onSuccess(PageList<GroupResourceConfigurationUpdate> result) {
                    VLayout column = new VLayout();
                    column.setHeight(10);
                    if (!result.isEmpty()) {
                        int rowNum = 0;
                        for (GroupResourceConfigurationUpdate update : result) {
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
                            LinkItem link = newLinkItem(linkTitle, ReportDecorator.GWT_GROUP_URL + groupId
                                + "/Configuration/History/" + update.getId());
                            StaticTextItem time = newTextItem(GwtRelativeDurationConverter.format(update
                                .getCreatedTime()));

                            row.setItems(iconItem, link, time);
                            column.addMember(row);
                        }
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
        final int groupId = this.groupComposite.getResourceGroup().getId();
        long now = System.currentTimeMillis();
        long nowMinus24Hours = now - (24 * 60 * 60 * 1000);
        GWTServiceLookup.getEventService().getEventCountsBySeverityForGroup(groupId, nowMinus24Hours, now,
            new AsyncCallback<Map<EventSeverity, Integer>>() {

                @Override
                public void onFailure(Throwable caught) {
                    Log
                        .debug("Error retrieving recent event counts for group [" + groupId + "]:"
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

    //    /** Fetches OOB measurements and updates the DynamicForm instance with the latest 5
    //     *  oob change details.
    //     */
    //    private void getRecentOobs() {
    //        final int resourceId = this.resourceComposite.getResource().getId();
    //        GWTServiceLookup.getMeasurementDataService().getHighestNOOBsForResource(resourceId, 5,
    //            new AsyncCallback<PageList<MeasurementOOBComposite>>() {
    //                @Override
    //                public void onFailure(Throwable caught) {
    //                    Log.debug("Error retrieving recent out of bound metrics for resource [" + resourceId + "]:"
    //                        + caught.getMessage());
    //                }
    //
    //                @Override
    //                public void onSuccess(PageList<MeasurementOOBComposite> result) {
    //                    VLayout column = new VLayout();
    //                    column.setHeight(10);
    //                    if (!result.isEmpty()) {
    //                        for (MeasurementOOBComposite oob : result) {
    //                            LocatableDynamicForm row = new LocatableDynamicForm(recentOobContent.extendLocatorId(oob
    //                                .getScheduleName()));
    //                            row.setNumCols(2);
    //
    //                            String title = oob.getScheduleName() + ":";
    //                            String destination = "/resource/common/monitor/Visibility.do?m=" + oob.getDefinitionId()
    //                                + "&id=" + resourceId + "&mode=chartSingleMetricSingleResource";
    //                            LinkItem link = newLinkItem(title, destination);
    //                            StaticTextItem time = newTextItem(GwtRelativeDurationConverter.format(oob.getTimestamp()));
    //
    //                            row.setItems(link, time);
    //                            column.addMember(row);
    //                        }
    //                    } else {
    //                        LocatableDynamicForm row = createEmptyDisplayRow(recentOobContent.extendLocatorId("None"),
    //                            RECENT_OOB_NONE);
    //                        column.addMember(row);
    //                    }
    //                    recentOobContent.setContents("");
    //                    for (Canvas child : recentOobContent.getChildren()) {
    //                        child.destroy();
    //                    }
    //                    recentOobContent.addChild(column);
    //                    recentOobContent.markForRedraw();
    //                }
    //            });
    //    }
    //
    //    /** Fetches recent package history information and updates the DynamicForm instance with details.
    //     */
    //    private void getRecentPkgHistory() {
    ////        final int resourceId = this.resourceComposite.getResource().getId();
    //        final int groupId = this.groupComposite.getResourceGroup().getId();
    //        InstalledPackageCriteria criteria = new InstalledPackageCriteria();
    ////        criteria.addFilterResourceId(resourceId);
    //        criteria.addFilterResourceId(groupId);
    //        PageControl pageControl = new PageControl(0, 5);
    //        criteria.setPageControl(pageControl);
    //
    ////        GWTServiceLookup.getContentService().getInstalledPackageHistoryForResource(resourceId, 5,
    //        GWTServiceLookup.getContentService().getInstalledPackageHistoryForResource(resourceId, 5,
    //            new AsyncCallback<PageList<InstalledPackageHistory>>() {
    //                @Override
    //                public void onFailure(Throwable caught) {
    //                    Log.debug("Error retrieving installed package history for resource [" + resourceId + "]:"
    //                        + caught.getMessage());
    //                }
    //
    //                @Override
    //                public void onSuccess(PageList<InstalledPackageHistory> result) {
    //                    VLayout column = new VLayout();
    //                    column.setHeight(10);
    //                    if (!result.isEmpty()) {
    //                        for (InstalledPackageHistory history : result) {
    //                            LocatableDynamicForm row = new LocatableDynamicForm(recentPkgHistoryContent
    //                                .extendLocatorId(history.getPackageVersion().getFileName()
    //                                    + history.getPackageVersion().getVersion()));
    //                            row.setNumCols(3);
    //
    //                            StaticTextItem iconItem = newTextItemIcon("subsystems/content/Content_16.png", null);
    //                            String title = history.getPackageVersion().getFileName() + ":";
    //                            String destination = "/rhq/resource/content/audit-trail-item.xhtml?id=" + resourceId
    //                                + "&selectedHistoryId=" + history.getId();
    //                            LinkItem link = newLinkItem(title, destination);
    //                            StaticTextItem time = newTextItem(GwtRelativeDurationConverter.format(history
    //                                .getTimestamp()));
    //
    //                            row.setItems(iconItem, link, time);
    //                            column.addMember(row);
    //                        }
    //                    } else {
    //                        LocatableDynamicForm row = createEmptyDisplayRow(recentPkgHistoryContent
    //                            .extendLocatorId("None"), RECENT_PKG_HISTORY_NONE);
    //                        column.addMember(row);
    //                    }
    //                    //cleanup
    //                    for (Canvas child : recentPkgHistoryContent.getChildren()) {
    //                        child.destroy();
    //                    }
    //                    recentPkgHistoryContent.addChild(column);
    //                    recentPkgHistoryContent.markForRedraw();
    //                }
    //            });
    //    }

    //    /** Fetches recent metric information and updates the DynamicForm instance with i)sparkline information,
    //     * ii) link to recent metric graph for more details and iii) last metric value formatted to show significant
    //     * digits.
    //     */
    //    private void getRecentMetrics() {
    //        //display container
    //        final VLayout column = new VLayout();
    //        column.setHeight(10);//pack
    //        //        final int resourceId = this.resourceComposite.getResource().getId();
    //        final int groupId = this.groupComposite.getResourceGroup().getId();
    //
    //        //        MeasurementDefinitionCriteria mdc = new MeasurementDefinitionCriteria();
    //        //        mdc.
    //
    //        //retrieve all relevant measurement definition ids.
    //        //        Set<MeasurementDefinition> definitions = this.resourceComposite.getResource().getResourceType()
    //        Set<MeasurementDefinition> definitions = this.groupComposite.getResourceGroup().getResourceType()
    //            .getMetricDefinitions();
    //
    //        //build id mapping for measurementDefinition instances Ex. Free Memory -> MeasurementDefinition[100071]
    //        final HashMap<String, MeasurementDefinition> measurementDefMap = new HashMap<String, MeasurementDefinition>();
    //        for (MeasurementDefinition definition : definitions) {
    //            measurementDefMap.put(definition.getDisplayName(), definition);
    //        }
    //
    //        //bundle definition ids for asynch call.
    //        int[] definitionArrayIds = new int[definitions.size()];
    //        final String[] displayOrder = new String[definitions.size()];
    //        measurementDefMap.keySet().toArray(displayOrder);
    //        //sort the charting data ex. Free Memory, Free Swap Space,..System Load
    //        Arrays.sort(displayOrder);
    //
    //        //organize definitionArrayIds for ordered request on server.
    //        int index = 0;
    //        for (String definitionToDisplay : displayOrder) {
    //            definitionArrayIds[index++] = measurementDefMap.get(definitionToDisplay).getId();
    //        }
    //        //make the asynchronous call for all the measurement data
    //        //        GWTServiceLookup.getMeasurementDataService().findDataForResource(resourceId, definitionArrayIds,
    //        GWTServiceLookup.getMeasurementDataService().findDataForCompatibleGroup(groupId, definitionArrayIds[0],
    //            System.currentTimeMillis() - (1000L * 60 * 60 * 8), System.currentTimeMillis(), 60,
    //            new AsyncCallback<List<List<MeasurementDataNumericHighLowComposite>>>() {
    //                @Override
    //                public void onFailure(Throwable caught) {
    //                    //                    Log.debug("Error retrieving recent metrics charting data for group [" + resourceId + "]:"
    //                    Log.debug("Error retrieving recent metrics charting data for group [" + groupId + "]:"
    //                        + caught.getMessage());
    //                }
    //
    //                @Override
    //                public void onSuccess(List<List<MeasurementDataNumericHighLowComposite>> results) {
    //                    if (!results.isEmpty()) {
    //                        boolean someChartedData = false;
    //                        //iterate over the retrieved charting data
    //                        for (int index = 0; index < displayOrder.length; index++) {
    //
    //                            //retrieve the correct measurement definition
    //                            MeasurementDefinition md = measurementDefMap.get(displayOrder[index]);
    //
    //                            //load the data results for the given metric definition
    //                            List<MeasurementDataNumericHighLowComposite> data = results.get(index);
    //
    //                            //locate last and minimum values.
    //                            double lastValue = -1;
    //                            double minValue = Double.MAX_VALUE;//
    //                            for (MeasurementDataNumericHighLowComposite d : data) {
    //                                if ((!Double.isNaN(d.getValue()))
    //                                    && (String.valueOf(d.getValue()).indexOf("NaN") == -1)) {
    //                                    if (d.getValue() < minValue) {
    //                                        minValue = d.getValue();
    //                                    }
    //                                    lastValue = d.getValue();
    //                                }
    //                            }
    //
    //                            //collapse the data into comma delimited list for consumption by third party javascript library(jquery.sparkline)
    //                            String commaDelimitedList = "";
    //
    //                            for (MeasurementDataNumericHighLowComposite d : data) {
    //                                if ((!Double.isNaN(d.getValue()))
    //                                    && (String.valueOf(d.getValue()).indexOf("NaN") == -1)) {
    //                                    commaDelimitedList += d.getValue() + ",";
    //                                }
    //                            }
    //                            LocatableDynamicForm row = new LocatableDynamicForm(recentMeasurementsContent
    //                                .extendLocatorId(md.getName()));
    //                            row.setNumCols(3);
    //                            HTMLFlow graph = new HTMLFlow();
    //                            //                        String contents = "<span id='sparkline_" + index + "' class='dynamicsparkline' width='0'>"
    //                            //                            + commaDelimitedList + "</span>";
    //                            String contents = "<span id='sparkline_" + index + "' class='dynamicsparkline' width='0' "
    //                                + "values='" + commaDelimitedList + "'>...</span>";
    //                            graph.setContents(contents);
    //                            graph.setContentsType(ContentsType.PAGE);
    //                            //diable scrollbars on span
    //                            graph.setScrollbarSize(0);
    //
    //                            CanvasItem graphContainer = new CanvasItem();
    //                            graphContainer.setShowTitle(false);
    //                            graphContainer.setHeight(16);
    //                            graphContainer.setWidth(60);
    //                            graphContainer.setCanvas(graph);
    //
    //                            //Link/title element
    //                            //TODO: spinder, change link whenever portal.war/graphing is removed.
    //                            String title = md.getDisplayName() + ":";
    //                            //                            String destination = "/resource/common/monitor/Visibility.do?mode=chartSingleMetricSingleResource&id="
    //                            //                                + resourceId + "&m=" + md.getId();
    //                            String destination = "/resource/common/monitor/Visibility.do?mode=chartSingleMetricMultiResource&groupId="
    //                                + groupId + "&m=" + md.getId();
    //                            LinkItem link = newLinkItem(title, destination);
    //
    //                            //Value
    //                            String convertedValue = lastValue + " " + md.getUnits();
    //                            convertedValue = convertLastValueForDisplay(lastValue, md);
    //                            StaticTextItem value = newTextItem(convertedValue);
    //
    //                            row.setItems(graphContainer, link, value);
    //                            //if graph content returned
    //                            if ((md.getName().trim().indexOf("Trait.") == -1) && (lastValue != -1)) {
    //                                column.addMember(row);
    //                                someChartedData = true;
    //                            }
    //                        }
    //                        if (!someChartedData) {// when there are results but no chartable entries.
    //                            LocatableDynamicForm row = createEmptyDisplayRow(recentMeasurementsContent
    //                                .extendLocatorId("None"), RECENT_MEASUREMENTS_NONE);
    //                            column.addMember(row);
    //                        }
    //                        //call out to 3rd party javascript lib
    //                        BrowserUtility.graphSparkLines();
    //                    } else {
    //                        LocatableDynamicForm row = createEmptyDisplayRow(recentMeasurementsContent
    //                            .extendLocatorId("None"), RECENT_MEASUREMENTS_NONE);
    //                        column.addMember(row);
    //                    }
    //                }
    //            });
    //
    //        //cleanup
    //        for (Canvas child : recentMeasurementsContent.getChildren()) {
    //            child.destroy();
    //        }
    //        recentMeasurementsContent.addChild(column);
    //        recentMeasurementsContent.markForRedraw();
    //    }

}
