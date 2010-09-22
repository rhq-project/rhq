package org.rhq.enterprise.gui.coregui.client.dashboard.portlets.recent.operations;

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

import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.events.SubmitValuesEvent;
import com.smartgwt.client.widgets.form.events.SubmitValuesHandler;
import com.smartgwt.client.widgets.form.fields.CheckboxItem;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.events.ChangeEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangeHandler;
import com.smartgwt.client.widgets.grid.HeaderSpan;
import com.smartgwt.client.widgets.layout.VStack;

import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.configuration.definition.PropertySimpleType;
import org.rhq.core.domain.dashboard.DashboardPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.CustomSettingsPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.Portlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.PortletViewFactory;
import org.rhq.enterprise.gui.coregui.client.dashboard.PortletWindow;
import org.rhq.enterprise.gui.coregui.client.operation.RecentOperationsDataSource;
import org.rhq.enterprise.gui.coregui.client.operation.ScheduledOperationsDataSource;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableHLayout;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableListGrid;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * A view that displays a live table of completed Operations and scheduled operations. 
 *
 * @author Simeon Pinder
 */
public class OperationsPortlet extends LocatableVLayout implements CustomSettingsPortlet {

    //unique field/form identifiers
    public static final String OPERATIONS_RANGE_COMPLETED_ENABLED = "operations-completed-enabled";
    public static final String OPERATIONS_RANGE_SCHEDULED_ENABLED = "operations-scheduled-enabled";
    public static final String OPERATIONS_RANGE_COMPLETED = "operations-range-completed";
    public static final String OPERATIONS_RANGE_SCHEDULED = "operations-range-scheduled";
    //portlet key
    public static final String KEY = "Operations";
    private static final String TITLE = KEY;
    private static String recentOperations = "Recent Operations";
    private static String scheduledOperations = "Scheduled Operations";
    public static String RANGE_DISABLED_MESSAGE = "(Results currently disabled. Change settings to enable results.)";
    //TODO: change this to use the Smart GWT default value.
    public static String RANGE_DISABLED_MESSAGE_DEFAULT = "No items to show.";
    //ListGrids for operations
    private LocatableListGrid recentOperationsGrid = null;
    private LocatableListGrid scheduledOperationsGrid = null;
    private DashboardPortlet storedPortlet = null;
    private RecentOperationsDataSource dataSourceCompleted;
    private ScheduledOperationsDataSource dataSourceScheduled;
    public static String unlimited = "unlimited";
    public static String defaultValue = unlimited;
    public static boolean defaultEnabled = true;

    //default no-args constructor for serialization.
    private OperationsPortlet() {
        super("(unitialized)");
    }

    public OperationsPortlet(String locatorId) {
        super(locatorId);
        this.dataSourceCompleted = new RecentOperationsDataSource(this);
        this.dataSourceScheduled = new ScheduledOperationsDataSource(this);
    }

    @Override
    protected void onInit() {
        super.onInit();
        //set title for larger container
        setTitle(TITLE);

        this.recentOperationsGrid = new LocatableListGrid(recentOperations);
        recentOperationsGrid.setDataSource(getDataSourceCompleted());
        recentOperationsGrid.setAutoFetchData(true);
        recentOperationsGrid.setTitle(recentOperations);
        recentOperationsGrid.setWidth100();
        //defining header span
        String[] completedRows = new String[] { RecentOperationsDataSource.location,
            RecentOperationsDataSource.operation, RecentOperationsDataSource.resource,
            RecentOperationsDataSource.status, RecentOperationsDataSource.time };
        recentOperationsGrid.setHeaderSpans(new HeaderSpan(recentOperations, completedRows));
        recentOperationsGrid.setHeaderSpanHeight(new Integer(20));
        recentOperationsGrid.setHeaderHeight(40);
        recentOperationsGrid.setResizeFieldsInRealTime(true);
        recentOperationsGrid.setCellHeight(50);
        recentOperationsGrid.setWrapCells(true);
        addMember(recentOperationsGrid);

        // Add the list table as the top half of the view.
        this.scheduledOperationsGrid = new LocatableListGrid(scheduledOperations);
        scheduledOperationsGrid.setDataSource(getDataSourceScheduled());
        scheduledOperationsGrid.setAutoFetchData(true);
        scheduledOperationsGrid.setTitle(scheduledOperations);
        scheduledOperationsGrid.setWidth100();
        String[] scheduledRows = new String[] { ScheduledOperationsDataSource.location,
            ScheduledOperationsDataSource.operation, ScheduledOperationsDataSource.resource,
            ScheduledOperationsDataSource.time };
        scheduledOperationsGrid.setHeaderSpans(new HeaderSpan(scheduledOperations, scheduledRows));
        scheduledOperationsGrid.setHeaderSpanHeight(new Integer(20));
        scheduledOperationsGrid.setHeaderHeight(40);

        scheduledOperationsGrid.setTitle(scheduledOperations);
        scheduledOperationsGrid.setResizeFieldsInRealTime(true);
        scheduledOperationsGrid.setCellHeight(50);
        scheduledOperationsGrid.setWrapCells(true);

        addMember(scheduledOperationsGrid);

    }

    @Override
    public void configure(PortletWindow portletWindow, DashboardPortlet storedPortlet) {
        this.storedPortlet = storedPortlet;
        //Operation range property - retrieve existing value
        PropertySimple property = storedPortlet.getConfiguration().getSimple(OPERATIONS_RANGE_COMPLETED);
        if ((property != null) && (property.getStringValue() != null)) {
            //retrieve and translate to int
            String retrieved = property.getStringValue();
            if (retrieved.equals(unlimited)) {
                getDataSourceCompleted().setOperationsRangeCompleted(-1);
            } else {
                getDataSourceCompleted().setOperationsRangeCompleted(Integer.parseInt(retrieved));
            }
        } else {//create setting
            storedPortlet.getConfiguration().put(new PropertySimple(OPERATIONS_RANGE_COMPLETED, defaultValue));
            getDataSourceCompleted().setOperationsRangeCompleted(-1);
        }

        property = storedPortlet.getConfiguration().getSimple(OPERATIONS_RANGE_SCHEDULED);
        if ((property != null) && (property.getStringValue() != null)) {
            //retrieve and translate to int
            String retrieved = property.getStringValue();
            if (retrieved.equals(unlimited)) {
                getDataSourceScheduled().setOperationsRangeScheduled(-1);
            } else {
                getDataSourceScheduled().setOperationsRangeScheduled(Integer.parseInt(retrieved));
            }
        } else {//create setting
            storedPortlet.getConfiguration().put(new PropertySimple(OPERATIONS_RANGE_SCHEDULED, defaultValue));
            getDataSourceScheduled().setOperationsRangeScheduled(-1);
        }
        //Checkbox settings property
        property = storedPortlet.getConfiguration().getSimple(OPERATIONS_RANGE_SCHEDULED_ENABLED);
        if ((property != null) && (property.getBooleanValue() != null)) {
            getDataSourceScheduled().setOperationsRangeScheduleEnabled(property.getBooleanValue().booleanValue());
        } else {//create setting
            storedPortlet.getConfiguration()
                .put(new PropertySimple(OPERATIONS_RANGE_SCHEDULED_ENABLED, defaultEnabled));
            getDataSourceScheduled().setOperationsRangeScheduleEnabled(defaultEnabled);
        }
        property = storedPortlet.getConfiguration().getSimple(OPERATIONS_RANGE_COMPLETED_ENABLED);
        if ((property != null) && (property.getBooleanValue() != null)) {
            getDataSourceCompleted().setOperationsRangeCompleteEnabled(property.getBooleanValue().booleanValue());
        } else {//create setting
            storedPortlet.getConfiguration()
                .put(new PropertySimple(OPERATIONS_RANGE_COMPLETED_ENABLED, defaultEnabled));
            getDataSourceCompleted().setOperationsRangeCompleteEnabled(defaultEnabled);
        }
    }

    @Override
    public Canvas getHelpCanvas() {
        return new HTMLFlow("This portlet displays both operations that have occurred and are scheduled to occur.");
    }

    /** Constructs the dynamic form instance using 1 column and multiple row layouts.
     */
    public DynamicForm getCustomSettingsForm() {

        //root dynamic form instance
        final DynamicForm form = new DynamicForm();

        //vertical layout
        VStack column = new VStack();

        //label
        Label operationRange = new Label("Operation Range");
        column.addMember(operationRange);

        //horizontal layout
        LocatableHLayout row = new LocatableHLayout("enable.completed.operations");

        //checkbox indicating whether to apply completed operations grouping settings
        final CheckboxItem enableCompletedOperationsGrouping = new CheckboxItem();
        enableCompletedOperationsGrouping.setName(OPERATIONS_RANGE_COMPLETED_ENABLED);
        enableCompletedOperationsGrouping.setTitle(" show Last ");
        //add change listener
        enableCompletedOperationsGrouping.addChangeHandler(new ChangeHandler() {
            public void onChange(ChangeEvent event) {
                String selectedItem = "" + event.getValue();
                //stuff into the master form for retrieval
                form.setValue(OPERATIONS_RANGE_COMPLETED_ENABLED, selectedItem);
            }
        });
        //retrieve previous value otherwise initialize to true(live unlimited list)
        PropertySimple retrieved = storedPortlet.getConfiguration().getSimple(OPERATIONS_RANGE_COMPLETED_ENABLED);
        if (retrieved != null) {
            enableCompletedOperationsGrouping.setValue(retrieved.getBooleanValue());
        } else {//default
            enableCompletedOperationsGrouping.setValue(true);
        }

        //wrap field item in dynamicform for addition as a field item
        DynamicForm item = new DynamicForm();
        item.setFields(enableCompletedOperationsGrouping);
        row.addMember(item);

        //-------------combobox for number of completed scheduled ops to display on the dashboard
        final SelectItem maximumCompletedOperationsComboBox = new SelectItem(OPERATIONS_RANGE_COMPLETED);
        maximumCompletedOperationsComboBox.setTitle("");
        maximumCompletedOperationsComboBox.setHint("<nobr><b> completed operations.</b></nobr>");
        //spinder: required to disable editability
        maximumCompletedOperationsComboBox.setType("selection");
        //define acceptable values for display amount
        String[] acceptableDisplayValues = { "1", "5", "10", "15", unlimited };
        maximumCompletedOperationsComboBox.setValueMap(acceptableDisplayValues);
        //set width of dropdown display region
        maximumCompletedOperationsComboBox.setWidth(100);
        maximumCompletedOperationsComboBox.addChangeHandler(new ChangeHandler() {
            public void onChange(ChangeEvent event) {
                String selectedItem = "" + event.getValue();
                //stuff into the master form for retrieval
                form.setValue(OPERATIONS_RANGE_COMPLETED, selectedItem);
            }
        });

        //default selected value to 'unlimited'(live lists) and check both combobox settings here.
        String selectedValue = defaultValue;
        if (storedPortlet != null) {
            //if property exists retrieve it
            if (storedPortlet.getConfiguration().getSimple(OPERATIONS_RANGE_COMPLETED) != null) {
                selectedValue = storedPortlet.getConfiguration().getSimple(OPERATIONS_RANGE_COMPLETED).getStringValue();
            } else {//insert default value
                storedPortlet.getConfiguration().put(new PropertySimple(OPERATIONS_RANGE_COMPLETED, defaultValue));
            }
        }
        //prepopulate the combobox with the previously stored selection
        maximumCompletedOperationsComboBox.setDefaultValue(selectedValue);
        DynamicForm item2 = new DynamicForm();
        item2.setFields(maximumCompletedOperationsComboBox);
        row.addMember(item2);

        //horizontal layout
        LocatableHLayout row2 = new LocatableHLayout("enable.scheduled.operations");

        final CheckboxItem enableScheduledOperationsGrouping = new CheckboxItem();
        enableScheduledOperationsGrouping.setName(OPERATIONS_RANGE_SCHEDULED_ENABLED);
        enableScheduledOperationsGrouping.setTitle(" show Next ");
        enableScheduledOperationsGrouping.addChangeHandler(new ChangeHandler() {
            public void onChange(ChangeEvent event) {
                String selectedItem = "" + event.getValue();
                //stuff into the master form for retrieval
                form.setValue(OPERATIONS_RANGE_SCHEDULED_ENABLED, selectedItem);
            }
        });

        //wrap field item in dynamicform for addition
        DynamicForm fieldWrapper = new DynamicForm();
        fieldWrapper.setFields(enableScheduledOperationsGrouping);
        row2.addMember(fieldWrapper);

        //retrieve previous value otherwise initialize to true(live unlimited list)
        retrieved = storedPortlet.getConfiguration().getSimple(OPERATIONS_RANGE_SCHEDULED_ENABLED);
        if (retrieved != null) {
            enableScheduledOperationsGrouping.setValue(retrieved.getBooleanValue());
        } else {
            enableScheduledOperationsGrouping.setValue(true);
        }

        //------------- Build second combobox for timeframe for problem resources search.
        final SelectItem maximumScheduledOperationsComboBox = new SelectItem(OPERATIONS_RANGE_SCHEDULED);
        maximumScheduledOperationsComboBox.setTitle("");
        maximumScheduledOperationsComboBox.setHint("<nobr><b> scheduled operations.</b></nobr>");
        maximumScheduledOperationsComboBox.setType("selection");
        maximumScheduledOperationsComboBox.setValueMap(acceptableDisplayValues);
        maximumScheduledOperationsComboBox.setWidth(100);
        maximumScheduledOperationsComboBox.addChangeHandler(new ChangeHandler() {
            public void onChange(ChangeEvent event) {
                String selectedItem = "" + event.getValue();
                //stuff into the master form for retrieval
                form.setValue(OPERATIONS_RANGE_SCHEDULED, selectedItem);
            }
        });

        //set to default
        selectedValue = defaultValue;
        if (storedPortlet != null) {
            //if property exists retrieve it
            if (storedPortlet.getConfiguration().getSimple(OPERATIONS_RANGE_SCHEDULED) != null) {
                selectedValue = storedPortlet.getConfiguration().getSimple(OPERATIONS_RANGE_SCHEDULED).getStringValue();
            } else {//insert default value
                storedPortlet.getConfiguration().put(new PropertySimple(OPERATIONS_RANGE_SCHEDULED, defaultValue));
            }
        }
        //prepopulate the combobox with the previously stored selection
        maximumScheduledOperationsComboBox.setDefaultValue(selectedValue);
        DynamicForm fieldWrapper2 = new DynamicForm();
        fieldWrapper2.setFields(maximumScheduledOperationsComboBox);
        row2.addMember(fieldWrapper2);
        column.addMember(row);
        column.addMember(row2);
        form.addChild(column);

        //submit handler
        form.addSubmitValuesHandler(new SubmitValuesHandler() {
            @Override
            public void onSubmitValues(SubmitValuesEvent event) {
                //no need to insert validation here as user not allowed to enter values
                if (form.getValue(OPERATIONS_RANGE_SCHEDULED) != null) {//if new value supplied
                    storedPortlet.getConfiguration().put(
                        new PropertySimple(OPERATIONS_RANGE_SCHEDULED, form.getValue(OPERATIONS_RANGE_SCHEDULED)));
                }
                if (form.getValue(OPERATIONS_RANGE_COMPLETED) != null) {//if new value supplied
                    storedPortlet.getConfiguration().put(
                        new PropertySimple(OPERATIONS_RANGE_COMPLETED, form.getValue(OPERATIONS_RANGE_COMPLETED)));
                }
                if (form.getValue(OPERATIONS_RANGE_COMPLETED_ENABLED) != null) {//if new value supplied
                    storedPortlet.getConfiguration().put(
                        new PropertySimple(OPERATIONS_RANGE_COMPLETED_ENABLED, form
                            .getValue(OPERATIONS_RANGE_COMPLETED_ENABLED)));
                }
                if (form.getValue(OPERATIONS_RANGE_SCHEDULED_ENABLED) != null) {//if new value supplied
                    storedPortlet.getConfiguration().put(
                        new PropertySimple(OPERATIONS_RANGE_SCHEDULED_ENABLED, form
                            .getValue(OPERATIONS_RANGE_SCHEDULED_ENABLED)));
                }
            }
        });

        return form;
    }

    public static final class Factory implements PortletViewFactory {
        public static PortletViewFactory INSTANCE = new Factory();

        public final Portlet getInstance(String locatorId) {
            return new OperationsPortlet(locatorId);
        }
    }

    /** Custom refresh operation as we cannot directly extend Table because it only
     * contains one ListGrid while the OperationsPortlet displays two tables.
     */
    @Override
    public void redraw() {
        super.redraw();
        //now reload the table data
        this.recentOperationsGrid.invalidateCache();
        this.recentOperationsGrid.markForRedraw();
        this.scheduledOperationsGrid.invalidateCache();
        this.scheduledOperationsGrid.markForRedraw();
    }

    public ConfigurationDefinition getConfigurationDefinition() {
        ConfigurationDefinition definition = new ConfigurationDefinition("OperationsPortlet Configuration",
            "The configuration settings for the Operations portlet.");

        definition.put(new PropertyDefinitionSimple(OPERATIONS_RANGE_COMPLETED,
            "Maximum number of Completed operations to display.", true, PropertySimpleType.STRING));
        definition.put(new PropertyDefinitionSimple(OPERATIONS_RANGE_SCHEDULED,
            "Maximum number of Scheduled operations to display.", true, PropertySimpleType.STRING));
        definition
            .put(new PropertyDefinitionSimple(OPERATIONS_RANGE_COMPLETED_ENABLED,
                "Whether to enable completed operations results grouping for dashboard.", true,
                PropertySimpleType.BOOLEAN));
        definition
            .put(new PropertyDefinitionSimple(OPERATIONS_RANGE_SCHEDULED_ENABLED,
                "Whether to enable scheduled operations results grouping for dashboard.", true,
                PropertySimpleType.BOOLEAN));

        return definition;
    }

    public RecentOperationsDataSource getDataSourceCompleted() {
        return this.dataSourceCompleted;
    }

    public ScheduledOperationsDataSource getDataSourceScheduled() {
        return this.dataSourceScheduled;
    }

    public LocatableListGrid getCompletedOperationsGrid() {
        return this.recentOperationsGrid;
    }

    public LocatableListGrid getScheduledOperationsGrid() {
        return this.scheduledOperationsGrid;
    }
}
