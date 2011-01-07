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

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.ContentsType;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.Img;
import com.smartgwt.client.widgets.form.fields.CanvasItem;
import com.smartgwt.client.widgets.form.fields.FormItemIcon;
import com.smartgwt.client.widgets.form.fields.LinkItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;

import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.configuration.ResourceConfigurationUpdate;
import org.rhq.core.domain.criteria.AlertCriteria;
import org.rhq.core.domain.criteria.EventCriteria;
import org.rhq.core.domain.criteria.InstalledPackageCriteria;
import org.rhq.core.domain.criteria.ResourceConfigurationUpdateCriteria;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementUnits;
import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;
import org.rhq.core.domain.measurement.composite.MeasurementOOBComposite;
import org.rhq.core.domain.operation.composite.ResourceOperationLastCompletedComposite;
import org.rhq.core.domain.resource.composite.DisambiguationReport;
import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.enterprise.gui.coregui.client.ImageManager;
import org.rhq.enterprise.gui.coregui.client.RefreshableView;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.resource.disambiguation.ReportDecorator;
import org.rhq.enterprise.gui.coregui.client.util.GwtRelativeDurationConverter;
import org.rhq.enterprise.gui.coregui.client.util.selenium.Locatable;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableCanvas;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableHLayout;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * The content pane of the Resource Summary>Activity tab.
 *
 * @author Simeon Pinder
 */
public class ActivityView2 extends LocatableHLayout implements RefreshableView {
    private LocatableVLayout leftPane;
    private LocatableVLayout rightPane;
    private LocatableCanvas recentMeasurementsContent = new LocatableCanvas(extendLocatorId("RecentMetricsContent"));
    private LocatableCanvas recentAlertsContent = new LocatableCanvas(extendLocatorId("RecentAlertsContent"));
    private LocatableCanvas recentOobContent = new LocatableCanvas(extendLocatorId("RecentOobsContent"));
    private LocatableCanvas recentConfigurationContent = new LocatableCanvas(extendLocatorId("RecentConfigContent"));
    private LocatableCanvas recentOperationsContent = new LocatableCanvas(extendLocatorId("RecentOperationsContent"));
    private LocatableCanvas recentEventsContent = new LocatableCanvas(extendLocatorId("RecentEventsContent"));
    private LocatableCanvas recentPkgHistoryContent = new LocatableCanvas(extendLocatorId("RecentPkgHistoryContent"));
    private String RECENT_MEASUREMENTS = MSG.common_title_recent_measurements();
    private String RECENT_MEASUREMENTS_NONE = MSG.view_resource_inventory_activity_no_recent_metrics();
    private String RECENT_ALERTS = MSG.common_title_recent_alerts();
    private String RECENT_ALERTS_NONE = MSG.view_resource_inventory_activity_no_recent_alerts();
    private String RECENT_OOB = MSG.common_title_recent_oob_metrics();
    private String RECENT_OOB_NONE = MSG.view_resource_inventory_activity_no_recent_oob();
    private String RECENT_CONFIGURATIONS = MSG.common_title_recent_configuration_updates();
    private String RECENT_CONFIGURATIONS_NONE = MSG.view_resource_inventory_activity_no_recent_config_history();
    private String RECENT_OPERATIONS = MSG.common_title_recent_operations();
    private String RECENT_OPERATIONS_NONE = MSG.view_resource_inventory_activity_no_recent_operations();
    private String RECENT_EVENTS = MSG.common_title_recent_event_counts();
    private String RECENT_EVENTS_NONE = MSG.view_resource_inventory_activity_no_recent_events();
    private String RECENT_PKG_HISTORY = MSG.common_title_recent_pkg_history();
    private String RECENT_PKG_HISTORY_NONE = MSG.view_resource_inventory_activity_no_recent_pkg_history();

    private Timer sparklineReloader = null;

    private ResourceComposite resourceComposite;

    public ActivityView2(String locatorId, ResourceComposite resourceComposite) {
        super(locatorId);
        this.resourceComposite = resourceComposite;
        setID(locatorId);
        initializeUi();
    }

    private void initializeUi() {
        setPadding(5);
        setMembersMargin(5);
        //dividers definition
        HTMLFlow divider1 = new HTMLFlow("<hr/>");
        HTMLFlow divider2 = new HTMLFlow("<hr/>");
        HTMLFlow divider3 = new HTMLFlow("<hr/>");
        HTMLFlow divider4 = new HTMLFlow("<hr/>");
        HTMLFlow divider5 = new HTMLFlow("<hr/>");
        divider1.setWidth("50%");
        divider2.setWidth("50%");
        divider3.setWidth("50%");
        divider4.setWidth("50%");
        divider5.setWidth("50%");

        //leftPane
        leftPane = new LocatableVLayout(extendLocatorId("Left"));
        leftPane.setWidth("50%");
        leftPane.setPadding(5);
        leftPane.setMembersMargin(5);
        leftPane.setAutoHeight();
        //recentMetrics.xhtml
        LocatableHLayout recentMetricsTitle = new TitleWithIcon(leftPane, "RecentMetrics",
            "subsystems/monitor/Monitor_24.png", RECENT_MEASUREMENTS);
        leftPane.addMember(recentMetricsTitle);
        leftPane.addMember(recentMeasurementsContent);
        recentMeasurementsContent.setHeight(20);
        recentMeasurementsContent.setContents(RECENT_MEASUREMENTS_NONE);
        leftPane.addMember(divider1);
        //recentAlerts.xhtml
        LocatableHLayout recentAlertsTitle = new TitleWithIcon(leftPane, "RecentAlerts",
            "subsystems/alert/Flag_blue_24.png", RECENT_ALERTS);
        leftPane.addMember(recentAlertsTitle);
        leftPane.addMember(recentAlertsContent);
        recentAlertsContent.setHeight(20);
        //        recentAlertsContent.setContents(RECENT_ALERTS_NONE);
        leftPane.addMember(divider2);
        //recentOOBs.xhtml
        LocatableHLayout recentOobsTitle = new TitleWithIcon(leftPane, "RecentOobs",
            "subsystems/monitor/Monitor_failed_24.png", RECENT_OOB);
        leftPane.addMember(recentOobsTitle);
        leftPane.addMember(recentOobContent);
        recentOobContent.setHeight(20);
        recentOobContent.setContents(RECENT_OOB_NONE);

        //rightPane
        rightPane = new LocatableVLayout(extendLocatorId("Right"));
        rightPane.setWidth("50%");
        rightPane.setPadding(5);
        rightPane.setMembersMargin(5);
        rightPane.setAutoHeight();
        //recentConfigUpdates.xhtml
        LocatableHLayout recentConfigUpdatesTitle = new TitleWithIcon(leftPane, "RecentConfigUpdates",
            "subsystems/configure/Configure_24.png", RECENT_CONFIGURATIONS);
        rightPane.addMember(recentConfigUpdatesTitle);
        rightPane.addMember(recentConfigurationContent);
        recentConfigurationContent.setHeight(20);
        //        recentConfigurationContent.setContents(RECENT_CONFIGURATIONS_NONE);
        rightPane.addMember(divider3);
        //recentOperations.xhtml
        LocatableHLayout recentOperationsTitle = new TitleWithIcon(leftPane, "RecentOperations",
            "subsystems/control/Operation_24.png", RECENT_OPERATIONS);
        rightPane.addMember(recentOperationsTitle);
        rightPane.addMember(recentOperationsContent);
        recentOperationsContent.setHeight(20);
        //        recentOperationsContent.setContents(RECENT_OPERATIONS_NONE);
        rightPane.addMember(divider4);
        //recentEventCounts.xhtml
        LocatableHLayout recentEventsTitle = new TitleWithIcon(leftPane, "RecentEvent",
            "subsystems/event/Events_24.png", RECENT_EVENTS);
        rightPane.addMember(recentEventsTitle);
        rightPane.addMember(recentEventsContent);
        recentEventsContent.setHeight(20);
        recentEventsContent.setContents(RECENT_EVENTS_NONE);
        rightPane.addMember(divider5);
        //recentPackageHistory.xhtml
        LocatableHLayout recentPkgHistoryTitle = new TitleWithIcon(leftPane, "RecentPkgHistory",
            "subsystems/content/Content_24.png", RECENT_PKG_HISTORY);
        rightPane.addMember(recentPkgHistoryTitle);
        rightPane.addMember(recentPkgHistoryContent);
        recentPkgHistoryContent.setHeight(20);
        recentPkgHistoryContent.setContents(RECENT_PKG_HISTORY_NONE);
        loadData();
    }

    private void loadData() {
        getRecentAlerts();
        getRecentOperations();
        getRecentConfigurationUpdates();
        getRecentEventUpdates();
        getRecentOobs();
        getRecentPkgHistory();
        getRecentMetrics();
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        addMember(leftPane);
        addMember(rightPane);
        refresh();
    }

    @Override
    public void refresh() {
        //        int resourceId = this.resourceComposite.getResource().getId();
        //        this.iFrame.setContentsURL("/rhq/resource/summary/overview-plain.xhtml?id=" + resourceId);
        //        Log.debug("$$$$$$$$$$$$$ ActivityView2.refresh()");
        loadData();
        markForRedraw();
    }

    class TitleWithIcon extends LocatableHLayout {
        public TitleWithIcon(String locatorId) {
            super(locatorId);
        }

        public TitleWithIcon(Locatable parentContainer, String locatorIdentifier, String imageUrl, String title) {
            super(parentContainer.extendLocatorId(locatorIdentifier));
            Img titleImage = new Img(imageUrl, 24, 24);
            HTMLFlow titleElement = new HTMLFlow();
            titleElement.setWidth("*");
            titleElement.setContents(title);
            titleElement.setStyleName("HeaderLabel");
            addMember(titleImage);
            addMember(titleElement);
            setMembersMargin(10);
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
                LocatableVLayout column = new LocatableVLayout(recentAlertsContent.extendLocatorId("Content"));
                column.setHeight(10);
                if (!result.isEmpty()) {
                    for (Alert alert : result) {
                        LocatableDynamicForm row = new LocatableDynamicForm(recentAlertsContent
                            .extendLocatorId("ContentForm"));
                        row.setNumCols(3);

                        StaticTextItem iconItem = new StaticTextItem();
                        FormItemIcon img = new FormItemIcon();
                        img.setSrc(ImageManager.getAlertIcon(alert.getAlertDefinition().getPriority()));
                        img.setWidth(16);
                        img.setHeight(16);
                        img.setPrompt(alert.getAlertDefinition().getPriority().getDisplayName());
                        iconItem.setIcons(img);
                        iconItem.setShowTitle(false);

                        LinkItem link = new LinkItem();
                        link.setLinkTitle(alert.getAlertDefinition().getName() + ": ");
                        link.setTitle(alert.getAlertDefinition().getName());
                        link.setValue(ReportDecorator.GWT_RESOURCE_URL + resourceId + "/Alerts/History/"
                            + alert.getId());
                        link.setTarget("_self");
                        link.setShowTitle(false);

                        StaticTextItem time = new StaticTextItem();
                        time.setDefaultValue(GwtRelativeDurationConverter.format(alert.getCtime()));
                        time.setShowTitle(false);
                        time.setShowPickerIcon(false);
                        time.setWrap(false);
                        row.setItems(iconItem, link, time);

                        column.addMember(row);
                    }
                } else {
                    column.setContents(RECENT_ALERTS_NONE);
                }
                recentAlertsContent.setContents("");
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
        GWTServiceLookup.getOperationService().findRecentCompletedOperations(pageControl,
            new AsyncCallback<List<DisambiguationReport<ResourceOperationLastCompletedComposite>>>() {

                @Override
                public void onFailure(Throwable caught) {
                    Log.debug("Error retrieving recent operations for resource [" + resourceId + "]:"
                        + caught.getMessage());
                }

                @Override
                public void onSuccess(List<DisambiguationReport<ResourceOperationLastCompletedComposite>> result) {
                    LocatableVLayout column = new LocatableVLayout(recentOperationsContent.extendLocatorId("Content"));
                    column.setHeight(10);
                    if (!result.isEmpty()) {
                        for (DisambiguationReport<ResourceOperationLastCompletedComposite> report : result) {
                            LocatableDynamicForm row = new LocatableDynamicForm(recentOperationsContent
                                .extendLocatorId("ContentForm"));
                            row.setNumCols(3);

                            StaticTextItem iconItem = new StaticTextItem();
                            FormItemIcon img = new FormItemIcon();
                            img.setSrc(ImageManager.getOperationResultsIcon(report.getOriginal().getOperationStatus()));
                            img.setWidth(16);
                            img.setHeight(16);
                            img.setPrompt(report.getOriginal().getOperationStatus().getDisplayName());
                            iconItem.setIcons(img);
                            iconItem.setShowTitle(false);

                            LinkItem link = new LinkItem();
                            link.setLinkTitle(report.getOriginal().getOperationName() + ": ");
                            link.setTitle(report.getOriginal().getOperationName());
                            link.setValue(ReportDecorator.GWT_RESOURCE_URL + resourceId + "/Operations/History/"
                                + report.getOriginal().getOperationHistoryId());
                            link.setTarget("_self");
                            link.setShowTitle(false);

                            StaticTextItem time = new StaticTextItem();
                            time.setDefaultValue(GwtRelativeDurationConverter.format(report.getOriginal()
                                .getOperationStartTime()));
                            time.setShowTitle(false);
                            time.setShowPickerIcon(false);
                            time.setWrap(false);
                            row.setItems(iconItem, link, time);

                            column.addMember(row);
                        }
                    } else {
                        column.setContents(RECENT_OPERATIONS_NONE);
                    }
                    for (Canvas child : recentOperationsContent.getChildren()) {
                        child.destroy();
                    }
                    recentOperationsContent.addChild(column);
                    recentOperationsContent.markForRedraw();
                }
            });
    }

    private void getRecentConfigurationUpdates() {
        final int resourceId = this.resourceComposite.getResource().getId();
        ResourceConfigurationUpdateCriteria criteria = new ResourceConfigurationUpdateCriteria();
        criteria.addFilterResourceIds(resourceId);
        PageControl pageControl = new PageControl(0, 5);
        criteria.setPageControl(pageControl);
        GWTServiceLookup.getConfigurationService().findResourceConfigurationUpdatesByCriteria(criteria,
            new AsyncCallback<PageList<ResourceConfigurationUpdate>>() {

                @Override
                public void onFailure(Throwable caught) {
                    Log.debug("Error retrieving recent configuration updates for resource [" + resourceId + "]:"
                        + caught.getMessage());
                }

                @Override
                public void onSuccess(PageList<ResourceConfigurationUpdate> result) {
                    LocatableVLayout column = new LocatableVLayout(recentOperationsContent.extendLocatorId("Content"));
                    column.setHeight(10);
                    if (!result.isEmpty()) {
                        for (ResourceConfigurationUpdate update : result) {
                            LocatableDynamicForm row = new LocatableDynamicForm(recentConfigurationContent
                                .extendLocatorId("ContentForm"));
                            row.setNumCols(3);

                            StaticTextItem iconItem = new StaticTextItem();
                            FormItemIcon img = new FormItemIcon();
                            img.setSrc(ImageManager.getResourceConfigurationIcon(update.getStatus()));
                            img.setWidth(16);
                            img.setHeight(16);
                            iconItem.setIcons(img);
                            iconItem.setShowTitle(false);

                            LinkItem link = new LinkItem();
                            link.setLinkTitle(MSG.view_resource_inventory_activity_changed_by()
                                + update.getSubjectName());
                            link.setTitle(MSG.view_resource_inventory_activity_changed_by() + update.getSubjectName());
                            link.setValue(ReportDecorator.GWT_RESOURCE_URL + resourceId + "/Configuration/History/"
                                + update.getId());
                            link.setTarget("_self");
                            link.setShowTitle(false);

                            StaticTextItem time = new StaticTextItem();
                            time.setDefaultValue(new Date(update.getCreatedTime()).toString());
                            time.setShowTitle(false);
                            time.setShowPickerIcon(false);
                            time.setWrap(false);
                            row.setItems(iconItem, link, time);

                            column.addMember(row);
                        }
                    } else {
                        LocatableDynamicForm row = new LocatableDynamicForm(recentConfigurationContent
                            .extendLocatorId("ContentForm"));
                        row.setNumCols(3);
                        StaticTextItem none = new StaticTextItem();
                        none.setShowTitle(false);
                        none.setDefaultValue(RECENT_CONFIGURATIONS_NONE);
                        none.setWrap(false);
                        row.setItems(none);
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

    private void getRecentEventUpdates() {
        final int resourceId = this.resourceComposite.getResource().getId();
        EventCriteria criteria = new EventCriteria();
        //        criteria.addFilterResourceIds(resourceId);
        long now = System.currentTimeMillis();
        long nowMinus24Hours = now - (24 * 60 * 60 * 1000);

        criteria.addFilterResourceId(resourceId);
        PageControl pageControl = PageControl.getUnlimitedInstance();
        criteria.setPageControl(pageControl);
        //Error retrieving recent event counts for resource [" + resourceId + "]:"
        //        GWTServiceLookup.getEventService().getEventCountsBySeverity(resourceId, nowMinus24Hours, now,
        //            new AsyncCallback<Map<EventSeverity, Integer>>() {
        //                @Override
        //                public void onFailure(Throwable caught) {
        //                    Log.debug("Error retrieving recent event counts for resource [" + resourceId + "]:"
        //                        + caught.getMessage());
        //                }
        //
        //                @Override
        //                public void onSuccess(Map<EventSeverity, Integer> result) {
        //                    LocatableVLayout column = new LocatableVLayout(recentOperationsContent.extendLocatorId("Content"));
        //                    column.setHeight(10);
        //                    if (!result.isEmpty()) {
        //                        for (Entry<EventSeverity, Integer> entry : result.entrySet()) {
        //                            EventSeverity severity = entry.getKey();
        //                            LocatableDynamicForm row = new LocatableDynamicForm(recentOperationsContent
        //                                .extendLocatorId("ContentForm"));
        //                            row.setNumCols(2);
        //
        //                            StaticTextItem iconItem = new StaticTextItem();
        //                            FormItemIcon img = new FormItemIcon();
        //                            img.setSrc(ImageManager.getEventSeverityIcon(severity));
        //                            //                        img.set
        //                            img.setWidth(16);
        //                            img.setHeight(16);
        //                            iconItem.setIcons(img);
        //                            iconItem.setShowTitle(false);
        //
        //                            StaticTextItem time = new StaticTextItem();
        //                            time.setDefaultValue(entry.getValue());
        //                            time.setShowTitle(false);
        //                            time.setShowPickerIcon(false);
        //                            time.setWrap(false);
        //                            row.setItems(iconItem, time);
        //
        //                            column.addMember(row);
        //                        }
        //                    } else {
        //                        LocatableDynamicForm row = new LocatableDynamicForm(recentEventsContent
        //                            .extendLocatorId("ContentForm"));
        //                        row.setNumCols(3);
        //                        StaticTextItem none = new StaticTextItem();
        //                        none.setShowTitle(false);
        //                        none.setDefaultValue(RECENT_EVENTS_NONE);
        //                        none.setWrap(false);
        //                        row.setItems(none);
        //                        column.addMember(row);
        //                    }
        //                    //cleanup
        //                    for (Canvas child : recentEventsContent.getChildren()) {
        //                        child.destroy();
        //                    }
        //                    recentEventsContent.addChild(column);
        //                    recentEventsContent.markForRedraw();
        //                }
        //            });
    }

    private void getRecentOobs() {
        final int resourceId = this.resourceComposite.getResource().getId();
        AlertCriteria criteria = new AlertCriteria();
        criteria.addFilterResourceIds(resourceId);
        PageControl pageControl = new PageControl(0, 5);
        criteria.setPageControl(pageControl);
        GWTServiceLookup.getMeasurementDataService().getHighestNOOBsForResource(resourceId, 5,
            new AsyncCallback<PageList<MeasurementOOBComposite>>() {
                @Override
                public void onFailure(Throwable caught) {
                    Log.debug("Error retrieving recent out of bound metrics for resource [" + resourceId + "]:"
                        + caught.getMessage());
                }

                @Override
                public void onSuccess(PageList<MeasurementOOBComposite> result) {
                    LocatableVLayout column = new LocatableVLayout(recentAlertsContent.extendLocatorId("Content"));
                    column.setHeight(10);
                    if (!result.isEmpty()) {
                        for (MeasurementOOBComposite oob : result) {
                            LocatableDynamicForm row = new LocatableDynamicForm(recentOobContent
                                .extendLocatorId("ContentForm"));
                            row.setNumCols(3);

                            //                            StaticTextItem iconItem = new StaticTextItem();
                            //                            FormItemIcon img = new FormItemIcon();
                            //                            img.setSrc(ImageManager.getAlertIcon(alert.getAlertDefinition().getPriority()));
                            //                            img.setWidth(16);
                            //                            img.setHeight(16);
                            //                            iconItem.setIcons(img);
                            //                            iconItem.setShowTitle(false);
                            //
                            //                            LinkItem link = new LinkItem();
                            //                            link.setLinkTitle(alert.getAlertDefinition().getName());
                            //                            link.setTitle(alert.getAlertDefinition().getName());
                            //                            link.setValue(ReportDecorator.GWT_RESOURCE_URL + resourceId + "/Alerts/Definitions/"
                            //                                + alert.getAlertDefinition().getId());
                            //                            link.setTarget("_self");
                            //                            link.setShowTitle(false);
                            //
                            //                            StaticTextItem time = new StaticTextItem();
                            //                            time.setDefaultValue(new Date(alert.getAlertDefinition().getCtime()).toString());
                            //                            time.setShowTitle(false);
                            //                            time.setShowPickerIcon(false);
                            //                            time.setWrap(false);
                            //                            row.setItems(iconItem, link, time);
                            //
                            //                            column.addMember(row);
                        }
                    } else {
                        column.setContents(RECENT_OOB_NONE);
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

    private void getRecentPkgHistory() {
        final int resourceId = this.resourceComposite.getResource().getId();
        InstalledPackageCriteria criteria = new InstalledPackageCriteria();
        criteria.addFilterResourceId(resourceId);
        //        criteria.
        PageControl pageControl = new PageControl(0, 5);
        criteria.setPageControl(pageControl);

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
        //                    LocatableVLayout column = new LocatableVLayout(recentPkgHistoryContent.extendLocatorId("Content"));
        //                    column.setHeight(10);
        //                    if (!result.isEmpty()) {
        //                        for (InstalledPackageHistory history : result) {
        //                            LocatableDynamicForm row = new LocatableDynamicForm(recentPkgHistoryContent
        //                                .extendLocatorId("ContentForm"));
        //                            row.setNumCols(3);
        //
        //                            StaticTextItem iconItem = new StaticTextItem();
        //                            FormItemIcon img = new FormItemIcon();
        //                            img.setSrc("subsystems/content/Content_16.png");
        //                            img.setWidth(16);
        //                            img.setHeight(16);
        //                            iconItem.setIcons(img);
        //                            iconItem.setShowTitle(false);
        //
        //                            LinkItem link = new LinkItem();
        //                            link.setLinkTitle(history.getPackageVersion().getDisplayName());
        //                            link.setTitle(history.getPackageVersion().getDisplayName());
        //                            //                        link.setValue(ReportDecorator.GWT_RESOURCE_URL + resourceId + "/Configuration/History/"
        //                            //                            + update.getId());
        //                            //link.setValue(rhq/resource/content/audit-trail-item.xhtml?id=10005&selectedHistoryId=10002
        //                            link.setValue("rhq/resource/content/audit-trail-item.xhtml?id=" + resourceId
        //                                + "&selectedHistoryId=" + history.getId());
        //                            link.setTarget("_self");
        //                            link.setShowTitle(false);
        //
        //                            StaticTextItem time = new StaticTextItem();
        //                            time.setDefaultValue(new Date(history.getTimestamp()).toString());
        //                            time.setShowTitle(false);
        //                            time.setShowPickerIcon(false);
        //                            time.setWrap(false);
        //                            row.setItems(iconItem, link, time);
        //
        //                            column.addMember(row);
        //                        }
        //                    } else {
        //                        LocatableDynamicForm row = new LocatableDynamicForm(recentPkgHistoryContent
        //                            .extendLocatorId("ContentForm"));
        //                        row.setNumCols(3);
        //                        StaticTextItem none = new StaticTextItem();
        //                        none.setShowTitle(false);
        //                        none.setDefaultValue(RECENT_PKG_HISTORY_NONE);
        //                        none.setWrap(false);
        //                        row.setItems(none);
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
    }

    private void getRecentMetrics() {
        final LocatableVLayout column = new LocatableVLayout(recentMeasurementsContent.extendLocatorId("Content"));
        column.setHeight(10);
        final int resourceId = this.resourceComposite.getResource().getId();
        Set<MeasurementDefinition> definitions = this.resourceComposite.getResource().getResourceType()
            .getMetricDefinitions();

        final HashMap<String, MeasurementDefinition> map = new HashMap<String, MeasurementDefinition>();
        for (MeasurementDefinition md : definitions) {
            map.put(md.getDisplayName(), md);
        }
        int[] definitionArrayIds = new int[definitions.size()];
        final String[] displayOrder = new String[definitions.size()];
        map.keySet().toArray(displayOrder);
        Arrays.sort(displayOrder);
        int l = 0;
        for (String definitionToDisplay : displayOrder) {
            definitionArrayIds[l++] = map.get(definitionToDisplay).getId();
        }
        GWTServiceLookup.getMeasurementDataService().findDataForResource(resourceId, definitionArrayIds,
            System.currentTimeMillis() - (1000L * 60 * 60 * 8), System.currentTimeMillis(), 60,
            new AsyncCallback<List<List<MeasurementDataNumericHighLowComposite>>>() {
                @Override
                public void onFailure(Throwable caught) {
                    Log.debug("Error retrieving metrics charting data for resource [" + resourceId + "]:"
                        + caught.getMessage());
                }

                @Override
                public void onSuccess(List<List<MeasurementDataNumericHighLowComposite>> results) {
                    for (int h = 0; h < displayOrder.length; h++) {
                        MeasurementDefinition md = map.get(displayOrder[h]);
                        List<MeasurementDataNumericHighLowComposite> data = results.get(h);
                        //locate last and minimum values.
                        double lastValue = 0;
                        double minValue = Double.MAX_VALUE;
                        for (MeasurementDataNumericHighLowComposite d : data) {
                            if ((d.getValue() + "").indexOf("NaN") == -1) {
                                if (d.getValue() < minValue) {
                                    minValue = d.getValue();
                                }
                            }
                        }

                        //collapse the data into comma delimited list
                        String commaDelimitedList = "";

                        for (MeasurementDataNumericHighLowComposite d : data) {
                            if ((d.getValue() + "").indexOf("NaN") == -1) {
                                commaDelimitedList += (d.getValue() - minValue) + ",";
                                lastValue = d.getValue();
                                if (d.getValue() < minValue) {
                                    minValue = d.getValue();
                                }
                            }
                        }
                        LocatableDynamicForm row = new LocatableDynamicForm(column.extendLocatorId("ContentForm"));
                        row.setNumCols(3);
                        //                        row.setWidth(300);
                        HTMLFlow graph = new HTMLFlow();
                        //move all sparkline data up to the minimum value
                        String contents = "<span id='sparkline_" + 1 + "' class='dynamicsparkline' width='0'>"
                            + commaDelimitedList + "</span>";
                        graph.setContents(contents);
                        graph.setContentsType(ContentsType.PAGE);
                        graph.setScrollbarSize(0);
                        CanvasItem ci2 = new CanvasItem();
                        ci2.setShowTitle(false);
                        ci2.setHeight(16);
                        ci2.setWidth(60);
                        ci2.setCanvas(graph);

                        //Link/title element
                        LinkItem link = new LinkItem();
                        link.setLinkTitle(md.getDisplayName());
                        link.setTitle(md.getName());
                        link.setTarget("_self");
                        link.setShowTitle(false);
                        //Value
                        StaticTextItem value = new StaticTextItem();
                        String convertedValue = lastValue + " " + md.getUnits();
                        long KBYTES = 1024;
                        long MBYTES = KBYTES * KBYTES;
                        long GBYTES = MBYTES * KBYTES;
                        NumberFormat fmt = NumberFormat.getDecimalFormat();
                        fmt = NumberFormat.getFormat("###.####");
                        if (md.getUnits() == MeasurementUnits.BYTES) {
                            if ((lastValue / GBYTES) > 1) {
                                convertedValue = fmt.format(lastValue / GBYTES) + "GB";
                            } else if ((lastValue / MBYTES) > 1) {
                                convertedValue = fmt.format(lastValue / MBYTES) + "MB";
                            } else {
                                convertedValue = fmt.format(lastValue / KBYTES) + "KB";
                            }
                        } else {
                            convertedValue = fmt.format(lastValue / 100) + md.getUnits();
                        }
                        value.setDefaultValue(convertedValue);
                        value.setShowTitle(false);
                        value.setShowPickerIcon(false);
                        value.setWrap(false);

                        row.setItems(ci2, link, value);
                        if (commaDelimitedList.trim().length() > 100) {
                            column.addMember(row);
                        }
                    }
                    //call out to 3rd party javascript lib
                    graphSparkLines();
                }
            });

        for (Canvas child : recentMeasurementsContent.getChildren()) {
            child.destroy();
        }
        recentMeasurementsContent.setContents("");
        recentMeasurementsContent.addChild(column);
        //        graphSparkLines();
        recentMeasurementsContent.markForRedraw();
        if (sparklineReloader == null) {
            sparklineReloader = new Timer() {
                public void run() {
                    refresh();
                }
            };
            sparklineReloader.schedule(750);
        }
    }

    public static native void graphSparkLines()
    /*-{
     //find all elements where attribute class contains 'dynamicsparkline' and graph their contents
     $wnd.jQuery('.dynamicsparkline').sparkline();
    }-*/;
}
