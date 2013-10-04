package org.rhq.coregui.client.dashboard.portlets.recent.problems;

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
import static org.rhq.coregui.client.resource.ProblemResourcesDataSource.Field.ALERTS;
import static org.rhq.coregui.client.resource.ProblemResourcesDataSource.Field.AVAILABILITY;
import static org.rhq.coregui.client.resource.ProblemResourcesDataSource.Field.RESOURCE;

import java.util.Date;
import java.util.List;

import com.google.gwt.user.client.Timer;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.events.SubmitValuesEvent;
import com.smartgwt.client.widgets.form.events.SubmitValuesHandler;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.HoverCustomizer;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.configuration.definition.PropertySimpleType;
import org.rhq.core.domain.dashboard.DashboardPortlet;
import org.rhq.coregui.client.LinkManager;
import org.rhq.coregui.client.components.table.IconField;
import org.rhq.coregui.client.components.table.Table;
import org.rhq.coregui.client.components.table.TableWidget;
import org.rhq.coregui.client.dashboard.AutoRefreshPortlet;
import org.rhq.coregui.client.dashboard.AutoRefreshUtil;
import org.rhq.coregui.client.dashboard.CustomSettingsPortlet;
import org.rhq.coregui.client.dashboard.Portlet;
import org.rhq.coregui.client.dashboard.PortletViewFactory;
import org.rhq.coregui.client.dashboard.PortletWindow;
import org.rhq.coregui.client.inventory.resource.AncestryUtil;
import org.rhq.coregui.client.resource.ProblemResourcesDataSource;
import org.rhq.coregui.client.util.MeasurementUtility;
import org.rhq.coregui.client.util.enhanced.EnhancedHLayout;

/**
 * A view that displays a paginated table of Resources with alerts,
 * and/or Resources reported unavailable.
 *
 * @author Simeon Pinder
 */
public class ProblemResourcesPortlet extends Table<ProblemResourcesDataSource> implements CustomSettingsPortlet,
    AutoRefreshPortlet {

    // A non-displayed, persisted identifier for the portlet
    public static final String KEY = "ProblemResources";
    // A default displayed, persisted name for the portlet    
    public static final String NAME = MSG.view_portlet_defaultName_problemResources();

    //keys for smart gwt elements. should be unique
    public static final String PROBLEM_RESOURCE_SHOW_HRS = "max-problems-query-span";
    public static final String PROBLEM_RESOURCE_SHOW_MAX = "max-problems-shown";

    // set on initial configuration, the window for this portlet view. 
    private PortletWindow portletWindow;

    //reference to datasource
    private ProblemResourcesDataSource dataSource;

    //constants
    public static final int unlimited = -1;
    public static final String unlimitedString = MSG.common_label_unlimited();
    public static final String defaultShowMax = "20";
    public static final String defaultShowHours = "24";

    private Timer refreshTimer;

    public ProblemResourcesPortlet() {
        super(NAME, true);

        setShowHeader(false);
        setShowFooter(true);
        //disable footer refresh
        setShowFooterRefresh(false);

        setOverflow(Overflow.VISIBLE);

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
            addExtraWidget(new TimeRange(this), false);
        }

        ListGridField resourceField = new ListGridField(RESOURCE.propertyName(), RESOURCE.title());
        resourceField.setCellFormatter(new CellFormatter() {
            public String format(Object o, ListGridRecord listGridRecord, int i, int i1) {
                String url = LinkManager.getResourceLink(listGridRecord.getAttributeAsInt("id"));
                return LinkManager.getHref(url, o.toString());
            }
        });
        resourceField.setShowHover(true);
        resourceField.setHoverCustomizer(new HoverCustomizer() {
            public String hoverHTML(Object value, ListGridRecord listGridRecord, int rowNum, int colNum) {
                return AncestryUtil.getResourceHoverHTML(listGridRecord, 0);
            }
        });

        ListGridField ancestryField = AncestryUtil.setupAncestryListGridField();

        ListGridField alertsField = new ListGridField(ALERTS.propertyName(), ALERTS.title(), 70);

        IconField availabilityField = new IconField(AVAILABILITY.propertyName(), AVAILABILITY.title(), 70);

        setListGridFields(resourceField, ancestryField, alertsField, availabilityField);
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

        if (null == this.portletWindow && null != portletWindow) {
            this.portletWindow = portletWindow;
        }

        if ((null == storedPortlet) || (null == storedPortlet.getConfiguration())) {
            return;
        }

        int configuredValue;

        //determine configuration value for ProblemResourceShowMax
        configuredValue = populateConfigurationValue(storedPortlet, PROBLEM_RESOURCE_SHOW_MAX, defaultShowMax);
        getDataSource().setMaximumProblemResourcesToDisplay(configuredValue);

        //determine configuration value for ProblemResourceShowHrs
        configuredValue = populateConfigurationValue(storedPortlet, PROBLEM_RESOURCE_SHOW_HRS, defaultShowHours);
        getDataSource().setMaximumProblemResourcesWithinHours(configuredValue);
    }

    /**Determine which configuration value to use given the property passed in.
     *
     * @param storedPortlet DashboardPortlet instance
     * @param propertyKey Widget key
     * @param defaultKeyValue default value to be used if property not yet set.
     * @return value of configuration, Ex. 1,5,10,unlimited.
     */
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

    @Override
    public Canvas getHelpCanvas() {
        return new HTMLFlow(MSG.view_portlet_help_problemResources());
    }

    /** Build custom for to dispaly the Portlet Configuration settings.
     *
     */
    public DynamicForm getCustomSettingsForm() {

        final DynamicForm form = new DynamicForm();

        final DashboardPortlet storedPortlet = portletWindow.getStoredPortlet();

        //-------------combobox for number of resource to display on the dashboard
        final SelectItem maximumProblemResourcesComboBox = new SelectItem(PROBLEM_RESOURCE_SHOW_MAX);
        maximumProblemResourcesComboBox.setTitle(MSG.common_title_display());
        maximumProblemResourcesComboBox.setHint("<nobr><b> " + MSG.view_portlet_problemResources_maxDisplaySetting()
            + "</b></nobr>");
        //spinder 9/3/10: the following is required workaround to disable editability of combobox.
        maximumProblemResourcesComboBox.setType("selection");
        //define acceptable values for display amount
        String[] acceptableDisplayValues = { "5", "10", "15", "20", "30", unlimitedString };
        maximumProblemResourcesComboBox.setValueMap(acceptableDisplayValues);
        //set width of dropdown display region
        maximumProblemResourcesComboBox.setWidth(100);

        int configuredValue = populateConfigurationValue(storedPortlet, PROBLEM_RESOURCE_SHOW_MAX, defaultShowMax);
        String selectedValue = configuredValue == unlimited ? unlimitedString : String.valueOf(configuredValue);

        //prepopulate the combobox with the previously stored selection
        maximumProblemResourcesComboBox.setDefaultValue(selectedValue);

        //------------- Build second combobox for timeframe for problem resources search.
        final SelectItem maximumTimeProblemResourcesComboBox = new SelectItem(PROBLEM_RESOURCE_SHOW_HRS);
        maximumTimeProblemResourcesComboBox.setTitle(MSG.common_title_over() + " ");
        maximumTimeProblemResourcesComboBox.setHint("<nobr><b> " + MSG.common_unit_hours() + " </b></nobr>");
        //spinder 9/3/10: the following is required workaround to disable editability of combobox.
        maximumTimeProblemResourcesComboBox.setType("selection");
        String[] acceptableTimeValues = { "1", "4", "8", "24", "48", unlimitedString };
        maximumTimeProblemResourcesComboBox.setValueMap(acceptableTimeValues);
        maximumTimeProblemResourcesComboBox.setWidth(100);

        configuredValue = populateConfigurationValue(storedPortlet, PROBLEM_RESOURCE_SHOW_HRS, defaultShowHours);
        selectedValue = configuredValue == unlimited ? unlimitedString : String.valueOf(configuredValue);

        //prepopulate the combobox with the previously stored selection
        maximumTimeProblemResourcesComboBox.setDefaultValue(selectedValue);

        //insert fields
        form.setFields(maximumProblemResourcesComboBox, maximumTimeProblemResourcesComboBox);

        //submit handler
        form.addSubmitValuesHandler(new SubmitValuesHandler() {

            @Override
            public void onSubmitValues(SubmitValuesEvent event) {

                String value = (String) form.getValue(PROBLEM_RESOURCE_SHOW_MAX);
                if (value != null) {
                    // convert display string to stored integer if necessary
                    value = unlimitedString.equals(value) ? String.valueOf(unlimited) : value;

                    storedPortlet.getConfiguration().put(new PropertySimple(PROBLEM_RESOURCE_SHOW_MAX, value));
                }

                value = (String) form.getValue(PROBLEM_RESOURCE_SHOW_HRS);
                if (value != null) {
                    // convert display string to stored integer if necessary
                    value = unlimitedString.equals(value) ? String.valueOf(unlimited) : value;

                    storedPortlet.getConfiguration().put(new PropertySimple(PROBLEM_RESOURCE_SHOW_HRS, value));
                }

                configure(portletWindow, storedPortlet);

                refresh();
            }
        });

        return form;
    }

    public static final class Factory implements PortletViewFactory {
        public static final PortletViewFactory INSTANCE = new Factory();

        public final Portlet getInstance(EntityContext context) {

            return new ProblemResourcesPortlet();
        }
    }

    public ConfigurationDefinition getConfigurationDefinition() {
        ConfigurationDefinition definition = new ConfigurationDefinition(MSG.view_portlet_configure_definitionTitle(),
            MSG.view_portlet_configure_definitionDesc());

        definition.put(new PropertyDefinitionSimple(PROBLEM_RESOURCE_SHOW_MAX, MSG
            .view_portlet_problemResources_config_display_maximum(), true, PropertySimpleType.INTEGER));
        definition.put(new PropertyDefinitionSimple(PROBLEM_RESOURCE_SHOW_HRS, MSG
            .view_portlet_problemResources_config_display_range(), true, PropertySimpleType.INTEGER));

        return definition;
    }

    public void refreshTableInfo() {
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
            //remove selected count as portlet is view only. Selection not used.
            getTableInfo().setContents(MSG.common_title_total() + ": " + getListGrid().getTotalRows());
        }
    }

    private String[] timeRange = null;

    public String[] getTimeRange() {
        return timeRange;
    }

    public void startRefreshCycle() {
        refreshTimer = AutoRefreshUtil.startRefreshCycleWithPageRefreshInterval(this, this, refreshTimer);
    }

    @Override
    protected void onDestroy() {
        AutoRefreshUtil.onDestroy(refreshTimer);

        super.onDestroy();
    }

    public boolean isRefreshing() {
        return false;
    }

    @Override
    public void refresh() {
        if (!isRefreshing()) {
            super.refresh();
        }
    }

}

/**Construct table widget Label to display timerange settings used with latest datasource query.
 *
 * @author spinder
 */
class TimeRange extends EnhancedHLayout implements TableWidget {
    private Label label = new Label();
    private ProblemResourcesPortlet portlet = null;

    public TimeRange(ProblemResourcesPortlet problemResourcesPortlet) {
        super();
        this.portlet = problemResourcesPortlet;
    }

    @Override
    public void refresh(ListGrid listGrid) {
        this.label.setWidth(400);
        this.label.setContents(MSG.view_portlet_problemResources_config_display_range2(portlet.getTimeRange()[0],
            portlet.getTimeRange()[1]));
    }

    @Override
    protected void onDraw() {
        super.onDraw();
        addMember(this.label);
    }

}
