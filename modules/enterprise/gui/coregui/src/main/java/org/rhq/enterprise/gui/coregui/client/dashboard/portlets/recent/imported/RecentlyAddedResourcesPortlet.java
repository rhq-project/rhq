/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */

package org.rhq.enterprise.gui.coregui.client.dashboard.portlets.recent.imported;

import com.google.gwt.user.client.Timer;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.events.SubmitValuesEvent;
import com.smartgwt.client.widgets.form.events.SubmitValuesHandler;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.tree.TreeGrid;

import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.dashboard.DashboardPortlet;
import org.rhq.enterprise.gui.coregui.client.components.HeaderLabel;
import org.rhq.enterprise.gui.coregui.client.components.table.TimestampCellFormatter;
import org.rhq.enterprise.gui.coregui.client.dashboard.AutoRefreshPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.AutoRefreshPortletUtil;
import org.rhq.enterprise.gui.coregui.client.dashboard.CustomSettingsPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.Portlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.PortletViewFactory;
import org.rhq.enterprise.gui.coregui.client.dashboard.PortletWindow;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

public class RecentlyAddedResourcesPortlet extends LocatableVLayout implements CustomSettingsPortlet,
    AutoRefreshPortlet {

    // A non-displayed, persisted identifier for the portlet
    public static final String KEY = "RecentlyAddedResources";
    // A default displayed, persisted name for the portlet    
    public static final String NAME = MSG.view_portlet_defaultName_recentlyAddedResources();

    public static final int unlimited = -1;
    public static final String unlimitedString = MSG.common_label_unlimited();
    public static final String defaultValue = "-1";

    private static final String RECENTLY_ADDED_SHOW_MAX = "recently-added-show-amount";
    private static final String RECENTLY_ADDED_SHOW_HRS = "recently-added-time-range";

    // set on initial configuration, the window for this portlet view. 
    private PortletWindow portletWindow;

    private RecentlyAddedResourceDS dataSource;
    private TreeGrid treeGrid = null;
    private boolean simple = true;

    private Timer refreshTimer;

    public RecentlyAddedResourcesPortlet() {
        super();

        //insert the datasource
        this.dataSource = new RecentlyAddedResourceDS(this);
    }

    @Override
    protected void onInit() {
        super.onInit();
        treeGrid = new TreeGrid();
        treeGrid.setDataSource(getDataSource());
        treeGrid.setAutoFetchData(true);
        treeGrid.setTitle(MSG.view_portlet_defaultName_recentlyAddedResources());
        treeGrid.setResizeFieldsInRealTime(true);
        treeGrid.setTreeFieldTitle("Resource Name");

        ListGridField resourceNameField = new ListGridField("name", MSG.common_title_resource_name());

        resourceNameField.setCellFormatter(new CellFormatter() {
            public String format(Object o, ListGridRecord listGridRecord, int i, int i1) {
                return "<a href=\"#Resource/" + listGridRecord.getAttribute("id") + "\">" + String.valueOf(o) + "</a>";
            }
        });

        ListGridField timestampField = new ListGridField("timestamp", MSG.common_title_timestamp());
        TimestampCellFormatter.prepareDateField(timestampField);
        treeGrid.setFields(resourceNameField, timestampField);

        if (!simple) {
            addMember(new HeaderLabel(MSG.view_portlet_defaultName_recentlyAddedResources()));
        }

        addMember(treeGrid);

    }

    public void configure(PortletWindow portletWindow, DashboardPortlet storedPortlet) {

        if (null == this.portletWindow && null != portletWindow) {
            this.portletWindow = portletWindow;
        }

        if ((null == storedPortlet) || (null == storedPortlet.getConfiguration())) {
            return;
        }

        if (null == this.portletWindow && null != portletWindow) {
            this.portletWindow = portletWindow;
        }

        if ((null == storedPortlet) || (null == storedPortlet.getConfiguration())) {
            return;
        }

        int configuredValue;

        //determine configuration value for ProblemResourceShowMax
        configuredValue = populateConfigurationValue(storedPortlet, RECENTLY_ADDED_SHOW_MAX, defaultValue);
        getDataSource().setMaximumRecentlyAddedToDisplay(configuredValue);

        //determine configuration value for ProblemResourceShowHrs
        configuredValue = populateConfigurationValue(storedPortlet, RECENTLY_ADDED_SHOW_HRS, defaultValue);
        getDataSource().setMaximumRecentlyAddedWithinHours(configuredValue);
    }

    private int populateConfigurationValue(DashboardPortlet storedPortlet, String propertyKey, String defaultKeyValue) {
        int configuredValue = Integer.valueOf(defaultKeyValue);

        if ((storedPortlet == null) || (storedPortlet.getConfiguration() == null)) {
            return configuredValue;
        }

        if (storedPortlet.getConfiguration().getSimple(propertyKey) != null) {
            String retrieved = storedPortlet.getConfiguration().getSimple(propertyKey).getStringValue();
            // protect against legacy issue with non-numeric values
            try {
                configuredValue = Integer.parseInt(retrieved);
            } catch (NumberFormatException e) {
                configuredValue = unlimited;
            }

        } else {
            storedPortlet.getConfiguration().put(new PropertySimple(propertyKey, defaultKeyValue));
        }

        return configuredValue;
    }

    public Canvas getHelpCanvas() {
        return new HTMLFlow(MSG.view_portlet_help_recentlyAdded());
    }

    public DynamicForm getCustomSettingsForm() {
        final DynamicForm form = new DynamicForm();

        final DashboardPortlet storedPortlet = portletWindow.getStoredPortlet();

        // combobox for number of recently added resources to display on the dashboard
        final SelectItem maximumRecentlyAddedComboBox = new SelectItem(RECENTLY_ADDED_SHOW_MAX);
        maximumRecentlyAddedComboBox.setTitle(MSG.common_title_show());
        maximumRecentlyAddedComboBox.setHint("<nobr><b> " + MSG.view_portlet_recentlyAdded_setting_addedPlatforms()
            + "</b></nobr>");
        //spinder 9/3/10: the following is required workaround to disable editability of combobox.
        maximumRecentlyAddedComboBox.setType("selection");
        //define acceptable values for display amount
        String[] acceptableDisplayValues = { "5", "10", "15", "20", "30", unlimitedString };
        maximumRecentlyAddedComboBox.setValueMap(acceptableDisplayValues);
        //set width of dropdown display region
        maximumRecentlyAddedComboBox.setWidth(100);

        int configuredValue = populateConfigurationValue(storedPortlet, RECENTLY_ADDED_SHOW_MAX, defaultValue);
        String selectedValue = configuredValue == unlimited ? unlimitedString : String.valueOf(configuredValue);

        //prepopulate the combobox with the previously stored selection
        maximumRecentlyAddedComboBox.setDefaultValue(selectedValue);

        // second combobox for timeframe for problem resources search.
        final SelectItem maximumTimeRecentlyAddedComboBox = new SelectItem(RECENTLY_ADDED_SHOW_HRS);
        maximumTimeRecentlyAddedComboBox.setTitle("Over ");
        maximumTimeRecentlyAddedComboBox.setHint("<nobr><b> " + MSG.common_unit_hours() + " </b></nobr>");
        //spinder 9/3/10: the following is required workaround to disable editability of combobox.
        maximumTimeRecentlyAddedComboBox.setType("selection");
        //define acceptable values for display amount
        String[] acceptableTimeValues = { "1", "4", "8", "24", "48", unlimitedString };
        maximumTimeRecentlyAddedComboBox.setValueMap(acceptableTimeValues);
        maximumTimeRecentlyAddedComboBox.setWidth(100);

        configuredValue = populateConfigurationValue(storedPortlet, RECENTLY_ADDED_SHOW_HRS, defaultValue);
        selectedValue = configuredValue == unlimited ? unlimitedString : String.valueOf(configuredValue);

        //prepopulate the combobox with the previously stored selection
        maximumTimeRecentlyAddedComboBox.setDefaultValue(selectedValue);

        // insert fields
        form.setFields(maximumRecentlyAddedComboBox, maximumTimeRecentlyAddedComboBox);

        // submit handler
        form.addSubmitValuesHandler(new SubmitValuesHandler() {
            @Override
            public void onSubmitValues(SubmitValuesEvent event) {
                String value = (String) form.getValue(RECENTLY_ADDED_SHOW_MAX);

                if (value != null) {
                    // convert display string to stored integer if necessary
                    value = unlimitedString.equals(value) ? String.valueOf(unlimited) : value;

                    storedPortlet.getConfiguration().put(new PropertySimple(RECENTLY_ADDED_SHOW_MAX, value));
                }

                value = (String) form.getValue(RECENTLY_ADDED_SHOW_HRS);
                if (value != null) {
                    // convert display string to stored integer if necessary
                    value = unlimitedString.equals(value) ? String.valueOf(unlimited) : value;

                    storedPortlet.getConfiguration().put(new PropertySimple(RECENTLY_ADDED_SHOW_HRS, value));
                }

                configure(portletWindow, storedPortlet);

                redraw();
            }
        });

        return form;
    }

    public static final class Factory implements PortletViewFactory {
        public static PortletViewFactory INSTANCE = new Factory();

        public final Portlet getInstance(EntityContext context) {

            return new RecentlyAddedResourcesPortlet();
        }
    }

    public RecentlyAddedResourceDS getDataSource() {
        return dataSource;
    }

    public void startRefreshCycle() {
        refreshTimer = AutoRefreshPortletUtil.startRefreshCycle(this, this, refreshTimer);
    }

    @Override
    protected void onDestroy() {
        AutoRefreshPortletUtil.onDestroy(this, refreshTimer);

        super.onDestroy();
    }

    public boolean isRefreshing() {
        return false;
    }

    // Custom refresh operation as we cannot directly extend Table because it
    // contains a TreeGrid which is not a Table.
    @Override
    public void refresh() {
        if (!isRefreshing()) {
            if (null != treeGrid) {
                //now reload the table data
                treeGrid.invalidateCache();
            }
            markForRedraw();
        }
    }

}
