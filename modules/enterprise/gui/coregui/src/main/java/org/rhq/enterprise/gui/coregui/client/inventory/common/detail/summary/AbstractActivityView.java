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
package org.rhq.enterprise.gui.coregui.client.inventory.common.detail.summary;

import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.Img;
import com.smartgwt.client.widgets.form.fields.FormItemIcon;
import com.smartgwt.client.widgets.form.fields.LinkItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.resource.group.GroupCategory;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;
import org.rhq.enterprise.gui.coregui.client.RefreshableView;
import org.rhq.enterprise.gui.coregui.client.util.BrowserUtility;
import org.rhq.enterprise.gui.coregui.client.util.measurement.GwtMonitorUtils;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableCanvas;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableHLayout;
import org.rhq.enterprise.gui.coregui.client.util.selenium.SeleniumUtility;

/**
 * @author Simeon Pinder
 */
public abstract class AbstractActivityView extends LocatableHLayout implements RefreshableView {

    //Locatable ui references
    protected VLayout leftPane = new VLayout();
    protected VLayout rightPane = new VLayout();

    protected LocatableCanvas recentMeasurementsContent = new LocatableCanvas(extendLocatorId("RecentMetrics"));
    protected LocatableCanvas recentAlertsContent = new LocatableCanvas(extendLocatorId("RecentAlerts"));
    protected LocatableCanvas recentOobContent = new LocatableCanvas(extendLocatorId("RecentOobs"));
    protected LocatableCanvas recentConfigurationContent = new LocatableCanvas(extendLocatorId("RecentConfig"));
    protected LocatableCanvas recentOperationsContent = new LocatableCanvas(extendLocatorId("RecentOperations"));
    protected LocatableCanvas recentEventsContent = new LocatableCanvas(extendLocatorId("RecentEvents"));
    protected LocatableCanvas recentPkgHistoryContent = new LocatableCanvas(extendLocatorId("RecentPkgHistory"));

    //retrieve localized text
    protected String RECENT_MEASUREMENTS = MSG.common_title_recent_measurements();
    protected String RECENT_MEASUREMENTS_NONE = MSG.view_resource_inventory_activity_no_recent_metrics();
    protected String RECENT_ALERTS = MSG.common_title_recent_alerts();
    protected String RECENT_ALERTS_NONE = MSG.view_resource_inventory_activity_no_recent_alerts();
    protected String RECENT_OOB = MSG.common_title_recent_oob_metrics();
    protected String RECENT_OOB_NONE = MSG.view_resource_inventory_activity_no_recent_oob();
    protected String RECENT_CONFIGURATIONS = MSG.common_title_recent_configuration_updates();
    protected String RECENT_CONFIGURATIONS_NONE = MSG.view_resource_inventory_activity_no_recent_config_history();
    protected String RECENT_OPERATIONS = MSG.common_title_recent_operations();
    protected String RECENT_OPERATIONS_NONE = MSG.view_resource_inventory_activity_no_recent_operations();
    protected String RECENT_EVENTS = MSG.common_title_recent_event_counts();
    protected String RECENT_EVENTS_NONE = MSG.view_resource_inventory_activity_no_recent_events();
    protected String RECENT_PKG_HISTORY = MSG.common_title_recent_pkg_history();
    protected String RECENT_PKG_HISTORY_NONE = MSG.view_resource_inventory_activity_no_recent_pkg_history();

    private ResourceGroupComposite groupComposite = null;

    public AbstractActivityView(String locatorId, ResourceGroupComposite groupComposite) {
        super(locatorId);
        if (groupComposite != null) {
            this.groupComposite = groupComposite;
        }
        initializeUi();
    }

    /**Defines layout for the Activity page.
     */
    protected void initializeUi() {
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
        leftPane.setWidth("50%");
        leftPane.setPadding(5);
        leftPane.setMembersMargin(5);
        leftPane.setAutoHeight();

        ResourceGroup group = null;
        if (groupComposite != null) {
            group = groupComposite.getResourceGroup();
        }

        //recentMetrics.xhtml
        HLayout recentMetricsTitle = new TitleWithIcon("subsystems/monitor/Monitor_24.png", RECENT_MEASUREMENTS);
        if ((group == null) || ((group != null) && (group.getGroupCategory().equals(GroupCategory.COMPATIBLE)))) {//resource,CompatibleGroup
            leftPane.addMember(recentMetricsTitle);
            leftPane.addMember(recentMeasurementsContent);
            recentMeasurementsContent.setHeight(20);
            leftPane.addMember(divider1);
        }
        //recentAlerts.xhtml
        HLayout recentAlertsTitle = new TitleWithIcon("subsystems/alert/Flag_blue_24.png", RECENT_ALERTS);
        leftPane.addMember(recentAlertsTitle);
        leftPane.addMember(recentAlertsContent);
        recentAlertsContent.setHeight(20);
        //recentOOBs.xhtml
        HLayout recentOobsTitle = new TitleWithIcon("subsystems/monitor/Monitor_failed_24.png", RECENT_OOB);

        if ((group == null) || ((group != null) && (group.getGroupCategory().equals(GroupCategory.COMPATIBLE)))) {//resource,CompatibleGroup
            leftPane.addMember(divider2);
            leftPane.addMember(recentOobsTitle);
            leftPane.addMember(recentOobContent);
            recentOobContent.setHeight(20);
        }
        //rightPane
        rightPane.setWidth("50%");
        rightPane.setPadding(5);
        rightPane.setMembersMargin(5);
        rightPane.setAutoHeight();
        //recentConfigUpdates.xhtml
        HLayout recentConfigUpdatesTitle = new TitleWithIcon("subsystems/configure/Configure_24.png",
            RECENT_CONFIGURATIONS);
        if ((group == null) || ((group != null) && (group.getGroupCategory().equals(GroupCategory.COMPATIBLE)))) {//resource,CompatibleGroup
            rightPane.addMember(recentConfigUpdatesTitle);
            rightPane.addMember(recentConfigurationContent);
            recentConfigurationContent.setHeight(20);
            rightPane.addMember(divider3);
        }
        //recentOperations.xhtml
        HLayout recentOperationsTitle = new TitleWithIcon("subsystems/control/Operation_24.png", RECENT_OPERATIONS);
        if ((group == null) || ((group != null) && (group.getGroupCategory().equals(GroupCategory.COMPATIBLE)))) {//resource,CompatibleGroup
            rightPane.addMember(recentOperationsTitle);
            rightPane.addMember(recentOperationsContent);
            recentOperationsContent.setHeight(20);
            rightPane.addMember(divider4);
        }
        //recentEventCounts.xhtml
        HLayout recentEventsTitle = new TitleWithIcon("subsystems/event/Events_24.png", RECENT_EVENTS);
        rightPane.addMember(recentEventsTitle);
        rightPane.addMember(recentEventsContent);
        recentEventsContent.setHeight(20);
        //recentPackageHistory.xhtml
        HLayout recentPkgHistoryTitle = new TitleWithIcon("subsystems/content/Content_24.png", RECENT_PKG_HISTORY);
        if ((group == null) || ((group != null) && (group.getGroupCategory().equals(GroupCategory.COMPATIBLE)))) {//resource,CompatibleGroup
            rightPane.addMember(divider5);
            rightPane.addMember(recentPkgHistoryTitle);
            rightPane.addMember(recentPkgHistoryContent);
            recentPkgHistoryContent.setHeight(20);
        }

        addMember(leftPane);
        addMember(rightPane);
    }

    /** Implement to define calls to asynchronous calls out to UI display data.
     */
    protected abstract void loadData();

    @Override
    protected void onDraw() {
        super.onDraw();
        refresh();
    }

    @Override
    public void destroy() {
        // destroy members of non-locatable layouts
        SeleniumUtility.destroyMembers(leftPane);
        SeleniumUtility.destroyMembers(rightPane);

        super.destroy();
    }

    @Override
    public void refresh() {
        markForRedraw();
        //call out to 3rd party javascript lib
        BrowserUtility.graphSparkLines();
    }

    /**Creates the section top titles with icon for regions of Activity page.
     */
    public class TitleWithIcon extends HLayout {

        public TitleWithIcon(String imageUrl, String title) {
            super();
            Img titleImage = new Img(imageUrl, 24, 24);
            HTMLFlow titleElement = new HTMLFlow();
            titleElement.setWidth("*");
            titleElement.setContents(title);
            titleElement.setStyleName("HeaderLabel");
            addMember(titleImage);
            addMember(titleElement);
            setMembersMargin(10);
        }

        @Override
        public void destroy() {
            SeleniumUtility.destroyMembers(this);
            super.destroy();
        }
    }

    /** Takes last double value returned and the relevant MeasurementDefinition and formats
     *  the results for display in the UI.  'Formatting' refers to relevant rounding,
     *  number format for significant digits depending upon the measurement definition
     *  details.
     *
     * @param lastValue
     * @param md MeasurementDefinition
     * @return formatted String representation of the last value retrieved.
     */
    protected String convertLastValueForDisplay(double lastValue, MeasurementDefinition md) {
        String convertedValue = "";
        String[] convertedValues = GwtMonitorUtils.formatSimpleMetrics(new double[] { lastValue }, md);
        convertedValue = convertedValues[0];

        return convertedValue;
    }

    /** Create empty display row(LocatableDynamicForm) that is constently defined and displayed.
     *
     * @param column Locatable parent colum.
     * @param emptyMessage Contents of the empty region
     * @return
     */
    public LocatableDynamicForm createEmptyDisplayRow(String locatorId, String emptyMessage) {
        LocatableDynamicForm row = null;
        row = new LocatableDynamicForm(locatorId);

        row.setNumCols(3);
        StaticTextItem none = new StaticTextItem();
        none.setShowTitle(false);
        none.setDefaultValue(emptyMessage);
        none.setWrap(false);
        row.setItems(none);
        return row;
    }

    public StaticTextItem newTextItemIcon(String imageSrc, String mouseOver) {
        StaticTextItem iconItem = new StaticTextItem();
        FormItemIcon img = new FormItemIcon();
        img.setSrc(imageSrc);
        img.setWidth(16);
        img.setHeight(16);
        if (mouseOver != null) {
            img.setPrompt(mouseOver);
        }
        iconItem.setIcons(img);
        iconItem.setShowTitle(false);
        return iconItem;
    }

    public LinkItem newLinkItem(String title, String destination) {
        LinkItem link = new LinkItem();
        link.setLinkTitle(title);
        link.setTitle(title);
        link.setValue(destination);
        link.setTarget("_self");
        link.setShowTitle(false);
        return link;
    }

    public StaticTextItem newTextItem(String contents) {
        StaticTextItem item = new StaticTextItem();
        item.setDefaultValue(contents);
        item.setShowTitle(false);
        item.setShowPickerIcon(false);
        item.setWrap(false);
        return item;
    }

}
