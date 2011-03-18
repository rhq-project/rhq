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
package org.rhq.enterprise.gui.coregui.client.dashboard.portlets.groups;

import java.util.HashMap;
import java.util.Set;

import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Timer;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.events.DoubleClickEvent;
import com.smartgwt.client.widgets.events.DoubleClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.events.SubmitValuesEvent;
import com.smartgwt.client.widgets.form.events.SubmitValuesHandler;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.dashboard.DashboardPortlet;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.LinkManager;
import org.rhq.enterprise.gui.coregui.client.Messages;
import org.rhq.enterprise.gui.coregui.client.UserSessionManager;
import org.rhq.enterprise.gui.coregui.client.alert.AlertHistoryView;
import org.rhq.enterprise.gui.coregui.client.alert.AlertPortletConfigurationDataSource;
import org.rhq.enterprise.gui.coregui.client.components.measurement.CustomConfigMeasurementRangeEditor;
import org.rhq.enterprise.gui.coregui.client.dashboard.AutoRefreshPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.CustomSettingsPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.Portlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.PortletViewFactory;
import org.rhq.enterprise.gui.coregui.client.dashboard.PortletWindow;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.PortletConfigurationEditorComponent;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.recent.alerts.PortletAlertSelector;
import org.rhq.enterprise.gui.coregui.client.inventory.common.detail.summary.AbstractActivityView;
import org.rhq.enterprise.gui.coregui.client.util.MeasurementUtility;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;
import org.rhq.enterprise.gui.coregui.client.util.selenium.SeleniumUtility;

/**
 * @author Simeon Pinder
 */
public class GroupAlertsPortlet extends AlertHistoryView implements CustomSettingsPortlet, AutoRefreshPortlet {

    // A non-displayed, persisted identifier for the portlet
    public static final String KEY = "GroupAlerts";
    // A default displayed, persisted name for the portlet
    public static final String NAME = MSG.view_portlet_defaultName_group_alerts();

    public static final String ALERT_RANGE_RESOURCES_VALUE = "alert-range-resource-value";
    public static final String ALERT_RANGE_RESOURCE_IDS = "alert-range-resource-ids";
    public static final String RESOURCES_ALL = MSG.common_label_all_resources();
    public static final String RESOURCES_SELECTED = MSG.common_label_selected_resources();
    public static final String defaultResourceValue = RESOURCES_ALL;
    public static final String ID = "id";

    // set on initial configuration, the window for this portlet view.
    protected PortletWindow portletWindow;

    //shared private UI elements
    protected AlertResourceSelectorRegion resourceSelector;

    protected AlertPortletConfigurationDataSource dataSource;
    //instance ui widgets
    protected Canvas containerCanvas;

    protected Timer refreshTimer;
    protected DashboardPortlet storedPortlet;
    protected Configuration portletConfig;
    private int groupId;
    protected boolean portletConfigInitialized = false;

    protected static HashMap<String, String> updatedMapping = new HashMap<String, String>();
    static {
        updatedMapping.putAll(PortletConfigurationEditorComponent.CONFIG_PROPERTY_INITIALIZATION);
        //Key, default
        updatedMapping.put(ALERT_RANGE_RESOURCES_VALUE, RESOURCES_ALL);
        updatedMapping.put(ALERT_RANGE_RESOURCE_IDS, RESOURCES_ALL);
    }

    public GroupAlertsPortlet(String locatorId) {
        super(locatorId);

        //override the shared datasource
        //figure out which page we're loading
        String currentPage = History.getToken();
        String[] elements = currentPage.split("/");
        this.groupId = Integer.valueOf(elements[1]);

        setShowFilterForm(false); //disable filter form for portlet
        setOverflow(Overflow.VISIBLE);
    }

    /**Defines layout for the portlet page.
     */
    protected void initializeUi() {
        //initalize the datasource
        this.dataSource = new AlertPortletConfigurationDataSource(storedPortlet, portletConfig, this.groupId, null);
        setDataSource(this.dataSource);

        setShowHeader(false);
        setShowFooter(true);
        setShowFooterRefresh(false); //disable footer refresh

        getListGrid().setEmptyMessage(MSG.view_portlet_results_empty());
    }

    /** Responsible for initialization and lazy configuration of the portlet values
     */
    public void configure(PortletWindow portletWindow, DashboardPortlet storedPortlet) {
        //populate portlet configuration details
        if (null == this.portletWindow && null != portletWindow) {
            this.portletWindow = portletWindow;
        }

        if ((null == storedPortlet) || (null == storedPortlet.getConfiguration())) {
            return;
        }
        this.storedPortlet = storedPortlet;
        portletConfig = storedPortlet.getConfiguration();

        if (!portletConfigInitialized) {
            this.dataSource = new AlertPortletConfigurationDataSource(storedPortlet, portletConfig, this.groupId, null);
            setDataSource(this.dataSource);
            portletConfigInitialized = true;
        }

        //lazy init any elements not yet configured.
        for (String key : PortletConfigurationEditorComponent.CONFIG_PROPERTY_INITIALIZATION.keySet()) {
            if (portletConfig.getSimple(key) == null) {
                portletConfig.put(new PropertySimple(key,
                    PortletConfigurationEditorComponent.CONFIG_PROPERTY_INITIALIZATION.get(key)));
            }
        }

        //resource ids to be conditionally included in the query
        Integer[] filterResourceIds = null;
        filterResourceIds = getDataSource().extractFilterResourceIds(storedPortlet, filterResourceIds);
        //no defaults

        if (filterResourceIds != null) {
            getDataSource().setAlertFilterResourceId(filterResourceIds);
        }

        //conditionally display the selected resources ui
        if (containerCanvas != null) {
            //empty out earlier canvas
            for (Canvas c : containerCanvas.getChildren()) {
                c.destroy();
            }
            if ((resourceSelector != null) && getDataSource().getAlertResourcesToUse().equals(RESOURCES_SELECTED)) {
                containerCanvas.addChild(resourceSelector.getCanvas());
            } else {
                containerCanvas.addChild(new Canvas());
            }
        }

    }

    public Canvas getHelpCanvas() {
        return new HTMLFlow(MSG.view_portlet_help_recentAlerts());
    }

    @Override
    public DynamicForm getCustomSettingsForm() {
        LocatableDynamicForm customSettings = new LocatableDynamicForm(extendLocatorId("customSettings"));
        LocatableVLayout page = new LocatableVLayout(customSettings.extendLocatorId("page"));
        //build editor form container
        final LocatableDynamicForm form = new LocatableDynamicForm(page.extendLocatorId("alert-filter"));
        form.setMargin(5);

        //add label about what configuration affects

        //add alert priority selector
        final SelectItem alertPrioritySelector = PortletConfigurationEditorComponent
            .getAlertPriorityEditor(portletConfig);
        //add sort priority selector
        //        final SelectItem resultSortSelector = PortletConfigurationEditorComponent
        //            .getResulSortOrderEditor(portletConfig);
        //add result count selector
        final SelectItem resultCountSelector = PortletConfigurationEditorComponent.getResultCountEditor(portletConfig);

        //add range selector
        final CustomConfigMeasurementRangeEditor measurementRangeEditor = PortletConfigurationEditorComponent
            .getMeasurementRangeEditor(portletConfig);

        form.setItems(alertPrioritySelector, resultCountSelector);

        //submit handler
        customSettings.addSubmitValuesHandler(new SubmitValuesHandler() {

            @Override
            public void onSubmitValues(SubmitValuesEvent event) {
                String selectedValue;
                //alert severity
                portletConfig = AbstractActivityView.saveAlertPrioritySettings(alertPrioritySelector, portletConfig);

                //                //result sort order
                //                selectedValue = resultSortSelector.getValue().toString();
                //                if ((selectedValue.trim().isEmpty()) || (selectedValue.equalsIgnoreCase(PageOrdering.DESC.name()))) {//then desc
                //                    portletConfig.put(new PropertySimple(Constant.RESULT_SORT_ORDER, PageOrdering.DESC));
                //                } else {
                //                    portletConfig.put(new PropertySimple(Constant.RESULT_SORT_ORDER, PageOrdering.ASC));
                //                }
                //result count
                portletConfig = AbstractActivityView.saveResultCounterSettings(resultCountSelector, portletConfig);

                //time range settings
                portletConfig = AbstractActivityView.saveMeasurementRangeEditorSettings(measurementRangeEditor,
                    portletConfig);

                //persist and reload portlet
                storedPortlet.setConfiguration(portletConfig);
                configure(portletWindow, storedPortlet);
                refresh();
            }
        });
        form.markForRedraw();
        page.addMember(measurementRangeEditor);
        page.addMember(form);
        customSettings.addChild(page);
        return customSettings;
    }

    public AlertPortletConfigurationDataSource getDataSource() {
        return dataSource;
    }

    public static final class Factory implements PortletViewFactory {
        public static PortletViewFactory INSTANCE = new Factory();

        public final Portlet getInstance(String locatorId) {

            return new GroupAlertsPortlet(locatorId);
        }
    }

    @Override
    public void startRefreshCycle() {
        //current setting
        final int refreshInterval = UserSessionManager.getUserPreferences().getPageRefreshInterval();

        //cancel any existing timer
        if (refreshTimer != null) {
            refreshTimer.cancel();
        }

        if (refreshInterval >= MeasurementUtility.MINUTES) {

            refreshTimer = new Timer() {
                public void run() {

                    redraw();
                }
            };

            refreshTimer.scheduleRepeating(refreshInterval);
        }
    }

    @Override
    protected void onDestroy() {
        if (refreshTimer != null) {
            refreshTimer.cancel();
        }

        super.onDestroy();
    }

    @Override
    protected void setupTableInteractions(boolean hasWriteAccess) {
        // The portlet is a "subsystem" view. Meaning the alerts displayed can be from any accessible group for
        // the user.  This means the user can have varying permissions on the underlying groups and/or resources,
        // which makes button enablement tricky. So, for the portlet don't even show the buttons unless the user
        // is inventory manager.  Other users will just have to navigate to the alert in question in order to
        // manipulate it.

        //determine if the user is inventory manager and if so render the buttons
        Set<Permission> permissions = this.portletWindow.getGlobalPermissions();
        if ((null != permissions) && permissions.contains(Permission.MANAGE_INVENTORY)) {
            super.setupTableInteractions(true);
        }
    }

    protected CellFormatter getDetailsLinkColumnCellFormatter() {
        return new CellFormatter() {
            public String format(Object value, ListGridRecord record, int i, int i1) {
                Integer recordId = getId(record);
                Integer resourceId = record.getAttributeAsInt("resourceId");
                String detailsUrl = LinkManager.getSubsystemAlertHistoryLink(resourceId, recordId);
                return SeleniumUtility.getLocatableHref(detailsUrl, value.toString(), null);
            }
        };
    }

    @Override
    protected void configureTable() {
        super.configureTable();

        setListGridDoubleClickHandler(new DoubleClickHandler() {
            @Override
            public void onDoubleClick(DoubleClickEvent event) {
                ListGrid listGrid = (ListGrid) event.getSource();
                ListGridRecord[] selectedRows = listGrid.getSelection();
                if (selectedRows != null && selectedRows.length == 1) {
                    Integer recordId = getId(selectedRows[0]);
                    Integer resourceId = selectedRows[0].getAttributeAsInt("resourceId");
                    CoreGUI.goToView(LinkManager.getSubsystemAlertHistoryLink(resourceId, recordId));
                }
            }
        });
    }

    @Override
    protected void onInit() {
        super.onInit();
        initializeUi();
        //        getListGrid().setEmptyMessage(MSG.view_portlet_results_empty());
    }

    @Override
    protected void refreshTableInfo() {
        super.refreshTableInfo();
        if (getTableInfo() != null) {
            int count = getListGrid().getSelection().length;
            getTableInfo().setContents(
                MSG.view_table_matchingRows(String.valueOf(getListGrid().getTotalRows()), String.valueOf(count)));
        }
    }
}

/** Bundles a ResourceSelector instance with labeling in Canvas for display.
 *  Also modifies the AssignedGrid to listen for AvailbleGrid completion and act accordingly.
 */
//class AlertResourceSelectorRegion extends LocatableVLayout {
final class AlertResourceSelectorRegion extends LocatableVLayout {
    public AlertResourceSelectorRegion(String locatorId, Integer[] assigned) {
        super(locatorId);
        this.currentlyAssignedIds = assigned;
    }

    private static final Messages MSG = CoreGUI.getMessages();
    private PortletAlertSelector selector = null;

    private Integer[] currentlyAssignedIds;

    public Integer[] getCurrentlyAssignedIds() {
        return currentlyAssignedIds;
    }

    public Integer[] getListGridValues() {
        Integer[] listGridValues = new Integer[0];
        if (null != selector) {
            listGridValues = selector.getAssignedListGridValues();
        }
        return listGridValues;
    }

    public Canvas getCanvas() {
        if (selector == null) {
            selector = new PortletAlertSelector(extendLocatorId("AlertSelector"), this.currentlyAssignedIds,
                ResourceType.ANY_PLATFORM_TYPE, false);
        }
        return selector;
    }

    public void setCurrentlyAssignedIds(Integer[] currentlyAssignedIds) {
        this.currentlyAssignedIds = currentlyAssignedIds;
    }
}