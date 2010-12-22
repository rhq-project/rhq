package org.rhq.enterprise.gui.coregui.client.dashboard.portlets.recent.problems;

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

//import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import com.google.gwt.user.client.Timer;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.events.SubmitValuesEvent;
import com.smartgwt.client.widgets.form.events.SubmitValuesHandler;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.grid.ListGrid;

import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.configuration.definition.PropertySimpleType;
import org.rhq.core.domain.dashboard.DashboardPortlet;
import org.rhq.enterprise.gui.coregui.client.UserSessionManager;
import org.rhq.enterprise.gui.coregui.client.components.table.Table;
import org.rhq.enterprise.gui.coregui.client.components.table.TableWidget;
import org.rhq.enterprise.gui.coregui.client.dashboard.AutoRefreshPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.CustomSettingsPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.Portlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.PortletViewFactory;
import org.rhq.enterprise.gui.coregui.client.dashboard.PortletWindow;
import org.rhq.enterprise.gui.coregui.client.resource.ProblemResourcesDataSource;
import org.rhq.enterprise.gui.coregui.client.util.MeasurementUtility;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableHLayout;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableLabel;

/**
 * A view that displays a paginated table of Resources with alerts,
 * and/or Resources reported unavailable.
 *
 * @author Simeon Pinder
 */
public class ProblemResourcesPortlet extends Table implements CustomSettingsPortlet, AutoRefreshPortlet {

    //keys for smart gwt elements. should be unique
    public static final String PROBLEM_RESOURCE_SHOW_HRS = "max-problems-query-span";
    public static final String PROBLEM_RESOURCE_SHOW_MAX = "max-problems-shown";
    public static final String KEY = MSG.view_portlet_problem_resources_title();
    private static final String TITLE = KEY;
    private DashboardPortlet storedPortlet;
    //reference to datasource
    private ProblemResourcesDataSource dataSource;

    //constants
    public static final String unlimited = MSG.common_label_unlimited();
    public static final String defaultValue = unlimited;
    private Timer defaultReloader;

    public ProblemResourcesPortlet(String locatorId) {
        super(locatorId, TITLE, true);

        setShowHeader(false);
        setShowFooter(true);
        //disable footer refresh
        setShowFooterRefresh(false);

        setOverflow(Overflow.HIDDEN);

        //insert the datasource
        this.dataSource = new ProblemResourcesDataSource(this);

        setDataSource(this.dataSource);
    }

    /** Gets access to the ListGrid from super class for editing.
     *
     */
    @Override
    protected void configureTable() {
        ListGrid listGrid = getListGrid();
        if (listGrid != null) {
            //extend height for displaying disambiguated resources
            listGrid.setCellHeight(50);
            //wrap to display disambiguation
            listGrid.setWrapCells(true);
            addExtraWidget(new TimeRange(this.getLocatorId(), this));
        }

    }

    @Override
    public ProblemResourcesDataSource getDataSource() {
        return (ProblemResourcesDataSource) super.getDataSource();
    }

    /** Implement configure action.
     *
     */
    @Override
    public void configure(PortletWindow portletWindow, DashboardPortlet storedPortlet) {

        this.storedPortlet = storedPortlet;
        int configuredValue = -1;

        //determine configuration value for ProblemResourceShowMax
        configuredValue = populateConfigurationValue(storedPortlet, PROBLEM_RESOURCE_SHOW_MAX, defaultValue);
        getDataSource().setMaximumProblemResourcesToDisplay(configuredValue);

        //determine configuration value for ProblemResourceShowHrs
        configuredValue = populateConfigurationValue(storedPortlet, PROBLEM_RESOURCE_SHOW_HRS, defaultValue);
        getDataSource().setMaximumProblemResourcesWithinHours(configuredValue);
    }

    /**Determine which configuration value to use given the property passed in.
     *
     * @param storedPortlet DashboardPortlet instance
     * @param propertyKey Widget key
     * @param defaultKeyValue default value to be used if property not yet set.
     * @return int value of configuration, Ex. 1,5,10,unlimited where unlimited==-1.
     */
    private int populateConfigurationValue(DashboardPortlet storedPortlet, String propertyKey, String defaultKeyValue) {
        int configuredValue;
        if ((storedPortlet != null) && (storedPortlet.getConfiguration().getSimple(propertyKey) != null)) {
            //retrieve and translate to int
            String retrieved = storedPortlet.getConfiguration().getSimple(propertyKey).getStringValue();
            if (retrieved.equals(unlimited)) {
                configuredValue = -1;
            } else {
                configuredValue = Integer.parseInt(retrieved);
            }
        } else {//create setting if not already there.
            storedPortlet.getConfiguration().put(new PropertySimple(propertyKey, defaultKeyValue));
            configuredValue = -1;
        }
        return configuredValue;
    }

    @Override
    public Canvas getHelpCanvas() {
        return new HTMLFlow(MSG.view_portlet_problem_resources_help());
    }

    /** Build custom for to dispaly the Portlet Configuration settings.
     *
     */
    public DynamicForm getCustomSettingsForm() {

        final LocatableDynamicForm form = new LocatableDynamicForm(extendLocatorId("custom-settings"));

        //-------------combobox for number of resource to display on the dashboard
        final SelectItem maximumProblemResourcesComboBox = new SelectItem(PROBLEM_RESOURCE_SHOW_MAX);
        maximumProblemResourcesComboBox.setTitle(MSG.common_title_display());
        maximumProblemResourcesComboBox.setHint("<nobr><b> "
            + MSG.view_portlet_problem_resources_config_problem_label() + "</b></nobr>");
        //spinder 9/3/10: the following is required workaround to disable editability of combobox.
        maximumProblemResourcesComboBox.setType("selection");
        //define acceptable values for display amount
        String[] acceptableDisplayValues = { "5", "10", "15", "20", "30", unlimited };
        maximumProblemResourcesComboBox.setValueMap(acceptableDisplayValues);
        //set width of dropdown display region
        maximumProblemResourcesComboBox.setWidth(100);

        //default selected value to 'unlimited'(live lists) and check both combobox settings here.
        String selectedValue = defaultValue;
        if (storedPortlet != null) {
            //if property exists retrieve it
            if (storedPortlet.getConfiguration().getSimple(PROBLEM_RESOURCE_SHOW_MAX) != null) {
                selectedValue = storedPortlet.getConfiguration().getSimple(PROBLEM_RESOURCE_SHOW_MAX).getStringValue();
            } else {//insert default value
                storedPortlet.getConfiguration().put(new PropertySimple(PROBLEM_RESOURCE_SHOW_MAX, defaultValue));
            }
        }
        //prepopulate the combobox with the previously stored selection
        maximumProblemResourcesComboBox.setDefaultValue(selectedValue);

        //------------- Build second combobox for timeframe for problem resources search.
        final SelectItem maximumTimeProblemResourcesComboBox = new SelectItem(PROBLEM_RESOURCE_SHOW_HRS);
        maximumTimeProblemResourcesComboBox.setTitle(MSG.common_title_over() + " ");
        maximumTimeProblemResourcesComboBox.setHint("<nobr><b> " + MSG.common_label_hours() + " </b></nobr>");
        //spinder 9/3/10: the following is required workaround to disable editability of combobox.
        maximumTimeProblemResourcesComboBox.setType("selection");
        //define acceptable values for display amount
        String[] acceptableTimeValues = { "1", "4", "8", "24", "48", unlimited };
        maximumTimeProblemResourcesComboBox.setValueMap(acceptableTimeValues);
        maximumTimeProblemResourcesComboBox.setWidth(100);

        //set to default
        selectedValue = defaultValue;
        if (storedPortlet != null) {
            //if property exists retrieve it
            if (storedPortlet.getConfiguration().getSimple(PROBLEM_RESOURCE_SHOW_HRS) != null) {
                selectedValue = storedPortlet.getConfiguration().getSimple(PROBLEM_RESOURCE_SHOW_HRS).getStringValue();
            } else {//insert default value
                storedPortlet.getConfiguration().put(new PropertySimple(PROBLEM_RESOURCE_SHOW_HRS, defaultValue));
            }
        }
        //prepopulate the combobox with the previously stored selection
        maximumTimeProblemResourcesComboBox.setDefaultValue(selectedValue);

        //insert fields
        form.setFields(maximumProblemResourcesComboBox, maximumTimeProblemResourcesComboBox);

        //submit handler
        form.addSubmitValuesHandler(new SubmitValuesHandler() {
            @Override
            public void onSubmitValues(SubmitValuesEvent event) {
                if (form.getValue(PROBLEM_RESOURCE_SHOW_MAX) != null) {
                    storedPortlet.getConfiguration().put(
                        new PropertySimple(PROBLEM_RESOURCE_SHOW_MAX, form.getValue(PROBLEM_RESOURCE_SHOW_MAX)));
                }
                if (form.getValue(PROBLEM_RESOURCE_SHOW_HRS) != null) {
                    storedPortlet.getConfiguration().put(
                        new PropertySimple(PROBLEM_RESOURCE_SHOW_HRS, form.getValue(PROBLEM_RESOURCE_SHOW_HRS)));
                }
                refresh();
            }
        });

        return form;
    }

    public static final class Factory implements PortletViewFactory {
        public static PortletViewFactory INSTANCE = new Factory();

        public final Portlet getInstance(String locatorId) {
            return new ProblemResourcesPortlet(locatorId);
        }
    }

    public ConfigurationDefinition getConfigurationDefinition() {
        ConfigurationDefinition definition = new ConfigurationDefinition(MSG
            .view_portlet_problem_resources_config_title(), MSG.view_portlet_problem_resources_config_title_desc());

        definition.put(new PropertyDefinitionSimple(PROBLEM_RESOURCE_SHOW_MAX, MSG
            .view_portlet_problem_resources_config_display_maximum(), true, PropertySimpleType.INTEGER));
        definition.put(new PropertyDefinitionSimple(PROBLEM_RESOURCE_SHOW_HRS, MSG
            .view_portlet_problem_resources_config_display_range(), true, PropertySimpleType.INTEGER));

        return definition;
    }

    protected void refreshTableInfo() {
        if (isShowFooter()) {
            long begin = 0;
            List<Long> bounds = MeasurementUtility.calculateTimeFrame(getDataSource()
                .getMaximumProblemResourcesWithinHours(), MeasurementUtility.UNIT_HOURS);
            begin = bounds.get(0);
            long end = bounds.get(1);

            //if range spans greater than year then change formatter.
            if ((end - begin) > MeasurementUtility.ONE_YEAR) {
                timeRange = new String[] { MeasurementUtility.getDateTimeYearFormatter().format(new Date(begin)),
                    MeasurementUtility.getDateTimeYearFormatter().format(new Date(end)) };
            } else {
                timeRange = new String[] { MeasurementUtility.getDateTimeFormatter().format(new Date(begin)),
                    MeasurementUtility.getDateTimeFormatter().format(new Date(end)) };
            }
            for (Object extraWidget : extraWidgets) {
                if (extraWidget instanceof TableWidget) {
                    ((TableWidget) extraWidget).refresh(getListGrid());
                }
            }
            //remove selected count as portlet is view only. Selection not used.
            getTableInfo().setContents(MSG.common_title_total() + ": " + getListGrid().getTotalRows());
        }
    }

    private String[] timeRange = null;
    private Timer reloader;

    public String[] getTimeRange() {
        return timeRange;
    }

    @Override
    public void startRefreshCycle() {
        //current setting
        final int retrievedRefreshInterval = UserSessionManager.getUserPreferences().getPageRefreshInterval();
        //cancel previous operation
        if (defaultReloader != null) {
            defaultReloader.cancel();
        }
        if (retrievedRefreshInterval >= MeasurementUtility.MINUTES) {
            defaultReloader = new Timer() {
                public void run() {
                    refresh();
                    //launch again until portlet reference and child references GC.
                    defaultReloader.schedule(retrievedRefreshInterval);
                }
            };
            defaultReloader.schedule(retrievedRefreshInterval);
        }
    }
}

/**Construct table widget Label to display timerange settings used with latest datasource query.
 *
 * @author spinder
 */
class TimeRange extends LocatableHLayout implements TableWidget {
    private LocatableLabel label = new LocatableLabel(extendLocatorId("time-range-label"));
    private ProblemResourcesPortlet portlet = null;

    public TimeRange(String locatorId, ProblemResourcesPortlet problemResourcesPortlet) {
        super(locatorId);
        this.portlet = problemResourcesPortlet;
    }

    @Override
    public void refresh(ListGrid listGrid) {
        this.label.setWidth(400);
        //        this.label.setContents("From " + portlet.getTimeRange()[0] + " to " + portlet.getTimeRange()[1]);
        this.label.setContents(MSG.view_portlet_problem_resources_config_display_range2(portlet.getTimeRange()[0],
            portlet.getTimeRange()[1]));
    }

    @Override
    protected void onDraw() {
        super.onDraw();
        addMember(this.label);
    }
}
