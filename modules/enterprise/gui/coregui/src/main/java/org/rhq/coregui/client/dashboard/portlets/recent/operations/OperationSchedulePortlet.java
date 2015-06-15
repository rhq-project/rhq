package org.rhq.coregui.client.dashboard.portlets.recent.operations;

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

import com.google.gwt.user.client.Timer;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.events.SubmitValuesEvent;
import com.smartgwt.client.widgets.form.events.SubmitValuesHandler;
import com.smartgwt.client.widgets.form.fields.CheckboxItem;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.events.ChangeEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangeHandler;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.HoverCustomizer;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.layout.VStack;

import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.configuration.definition.PropertySimpleType;
import org.rhq.core.domain.dashboard.DashboardPortlet;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.LinkManager;
import org.rhq.coregui.client.components.table.TimestampCellFormatter;
import org.rhq.coregui.client.dashboard.AutoRefreshPortlet;
import org.rhq.coregui.client.dashboard.AutoRefreshUtil;
import org.rhq.coregui.client.dashboard.CustomSettingsPortlet;
import org.rhq.coregui.client.dashboard.Portlet;
import org.rhq.coregui.client.dashboard.PortletViewFactory;
import org.rhq.coregui.client.dashboard.PortletWindow;
import org.rhq.coregui.client.inventory.resource.AncestryUtil;
import org.rhq.coregui.client.operation.ScheduledOperationsDataSource;
import org.rhq.coregui.client.util.enhanced.EnhancedHLayout;
import org.rhq.coregui.client.util.enhanced.EnhancedVLayout;

/**
 * A view that displays a live table of completed Operations and scheduled operations. 
 *
 * @author Simeon Pinder
 */
public class OperationSchedulePortlet extends EnhancedVLayout implements CustomSettingsPortlet, AutoRefreshPortlet {

    // A non-displayed, persisted identifier for the portlet
    public static final String KEY = "OperationSchedule";
    // A default displayed, persisted name for the portlet    
    public static final String NAME = MSG.common_title_scheduled_operations();

    public static final String OPERATIONS_RANGE_SCHEDULED_ENABLED = "operations-scheduled-enabled";
    public static final String OPERATIONS_RANGE_SCHEDULED = "operations-range-scheduled";
    public static final String RANGE_DISABLED_MESSAGE = MSG.view_portlet_operations_disabled();
    //TODO: change this to use the Smart GWT default value.
    public static final String RANGE_DISABLED_MESSAGE_DEFAULT = MSG.common_msg_noItemsToShow();

    private static final int WIDTH_RECENT_TIME = 150;
    private static final int WIDTH_RECENT_STATUS = 50;
    private static final int WIDTH_SCHEDULED_TIME = WIDTH_RECENT_TIME + WIDTH_RECENT_STATUS;

    // set on initial configuration, the window for this portlet view. 
    private PortletWindow portletWindow;

    //ListGrids for operations
    private ListGrid scheduledOperationsGrid = null;

    private ScheduledOperationsDataSource dataSourceScheduled;
    public static final String unlimited = "-1";
    public static final String unlimitedString = MSG.common_label_unlimited();
    public static final String defaultValue = "5";
    public static final boolean defaultEnabled = true;

    private Timer refreshTimer;

    public OperationSchedulePortlet() {
        super();
        this.dataSourceScheduled = new ScheduledOperationsDataSource(this);
    }

    @Override
    protected void onInit() {
        super.onInit();

        this.scheduledOperationsGrid = new ListGrid();
        scheduledOperationsGrid.setDataSource(getDataSourceScheduled());
        scheduledOperationsGrid.setAutoFetchData(true);
        scheduledOperationsGrid.setWidth100();
        scheduledOperationsGrid.setWrapCells(true);

        addMember(scheduledOperationsGrid);
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        ListGridField timeNext = new ListGridField(ScheduledOperationsDataSource.Field.TIME.propertyName(),
            ScheduledOperationsDataSource.Field.TIME.title(), WIDTH_SCHEDULED_TIME);
        timeNext.setCellFormatter(new TimestampCellFormatter() {
            public String format(Object value, ListGridRecord record, int rowNum, int colNum) {
                if (value != null) {
                    String timestamp = super.format(value, record, rowNum, colNum);
                    Integer id = record.getAttributeAsInt(AncestryUtil.RESOURCE_ID);
                    String url = null;
                    Integer opScheduleId = record.getAttributeAsInt("id");
                    if (id == null) {
                        id = record.getAttributeAsInt(ScheduledOperationsDataSource.Field.GROUP_ID.propertyName());
                        if (record.getAttributeAsBoolean(ScheduledOperationsDataSource.Field.IS_AUTO_GROUP
                            .propertyName())) {
                            url = LinkManager.getAutoGroupOperationScheduleLink(id, opScheduleId);
                        } else {
                            url = LinkManager.getSubsystemGroupOperationScheduleLink(id, opScheduleId);
                        }

                    } else {
                        url = LinkManager.getSubsystemResourceOperationScheduleLink(id, opScheduleId);
                    }
                    return LinkManager.getHref(url, timestamp);
                } else {
                    return "<i>" + MSG.common_label_none() + "</i>";
                }
            }
        });
        timeNext.setShowHover(true);
        timeNext.setHoverCustomizer(TimestampCellFormatter.getHoverCustomizer(ScheduledOperationsDataSource.Field.TIME
            .propertyName()));

        ListGridField operationNext = new ListGridField(ScheduledOperationsDataSource.Field.OPERATION.propertyName(),
            ScheduledOperationsDataSource.Field.OPERATION.title());

        ListGridField resourceNext = new ListGridField(
            ScheduledOperationsDataSource.Field.RESOURCE_OR_GROUP.propertyName(),
            ScheduledOperationsDataSource.Field.RESOURCE_OR_GROUP.title());
        resourceNext.setCellFormatter(new CellFormatter() {
            public String format(Object o, ListGridRecord listGridRecord, int i, int i1) {
                Integer id = listGridRecord.getAttributeAsInt(AncestryUtil.RESOURCE_ID);
                String url = null;
                if (id == null) {
                    id = listGridRecord.getAttributeAsInt(ScheduledOperationsDataSource.Field.GROUP_ID.propertyName());
                    if (listGridRecord.getAttributeAsBoolean(ScheduledOperationsDataSource.Field.IS_AUTO_GROUP
                        .propertyName())) {
                        url = LinkManager.getAutoGroupLink(id);
                    } else {
                        url = LinkManager.getResourceGroupLink(id);
                    }

                } else {
                    url = LinkManager.getResourceLink(id);
                }
                return LinkManager.getHref(url, o.toString());
            }
        });
        resourceNext.setShowHover(true);
        resourceNext.setHoverCustomizer(new HoverCustomizer() {
            public String hoverHTML(Object value, ListGridRecord listGridRecord, int rowNum, int colNum) {
                Integer id = listGridRecord.getAttributeAsInt(AncestryUtil.RESOURCE_ID);
                if (id != null) {
                    return MSG.common_title_resource() + ": " + AncestryUtil.getResourceHoverHTML(listGridRecord, 0);
                } else {
                    String type = listGridRecord.getAttributeAsString(ScheduledOperationsDataSource.Field.GROUP_TYPE
                        .propertyName());
                    String name = listGridRecord
                        .getAttributeAsString(ScheduledOperationsDataSource.Field.RESOURCE_OR_GROUP.propertyName());
                    return "<p>" + MSG.common_title_group() + " (" + type + "):<br/><br/>" + name + "</p>";
                }
            }
        });

        ListGridField ancestryNext = new ListGridField(AncestryUtil.RESOURCE_ANCESTRY, CoreGUI.getMessages()
            .common_title_ancestry());
        ancestryNext.setAlign(Alignment.LEFT);
        ancestryNext.setCellAlign(Alignment.LEFT);
        AncestryUtil.setupAncestryListGridFieldCellFormatter(ancestryNext);
        ancestryNext.setShowHover(true);
        ancestryNext.setHoverCustomizer(new HoverCustomizer() {
            public String hoverHTML(Object value, ListGridRecord listGridRecord, int rowNum, int colNum) {
                Integer id = listGridRecord.getAttributeAsInt(AncestryUtil.RESOURCE_ID);
                if (id != null) {
                    return AncestryUtil.getAncestryHoverHTML(listGridRecord, 0);
                } else {
                    return MSG.common_title_group();
                }
            }
        });

        scheduledOperationsGrid.setFields(timeNext, operationNext, resourceNext, ancestryNext);
    }

    @Override
    public void configure(PortletWindow portletWindow, DashboardPortlet storedPortlet) {

        if (null == this.portletWindow && null != portletWindow) {
            this.portletWindow = portletWindow;
        }

        if ((null == storedPortlet) || (null == storedPortlet.getConfiguration())) {
            return;
        }

        String retrieved = defaultValue;
        PropertySimple property = storedPortlet.getConfiguration().getSimple(OPERATIONS_RANGE_SCHEDULED);

        if ((property != null) && (property.getStringValue() != null)) {
            retrieved = property.getStringValue();
            // protect against legacy issue with non-numeric values
            try {
                Integer.parseInt(retrieved);
            } catch (NumberFormatException e) {
                retrieved = unlimited;
            }
        } else {
            storedPortlet.getConfiguration().put(new PropertySimple(OPERATIONS_RANGE_SCHEDULED, defaultValue));
        }

        getDataSourceScheduled().setOperationsRangeScheduled(Integer.parseInt(retrieved));

        //Checkbox settings property
        property = storedPortlet.getConfiguration().getSimple(OPERATIONS_RANGE_SCHEDULED_ENABLED);
        if ((property != null) && (property.getBooleanValue() != null)) {
            getDataSourceScheduled().setOperationsRangeScheduleEnabled(property.getBooleanValue().booleanValue());

        } else {
            storedPortlet.getConfiguration()
                .put(new PropertySimple(OPERATIONS_RANGE_SCHEDULED_ENABLED, defaultEnabled));
            getDataSourceScheduled().setOperationsRangeScheduleEnabled(defaultEnabled);
        }
    }

    @Override
    public Canvas getHelpCanvas() {
        return new HTMLFlow(MSG.view_portlet_help_scheduledOperations());
    }

    /** Constructs the dynamic form instance using 1 column and multiple row layouts.
     */
    public DynamicForm getCustomSettingsForm() {

        //root dynamic form instance
        final DynamicForm form = new DynamicForm();

        final DashboardPortlet storedPortlet = portletWindow.getStoredPortlet();

        //vertical layout
        VStack column = new VStack();

        //horizontal layout
        EnhancedHLayout sheduledOperationsLayout = new EnhancedHLayout();

        final CheckboxItem enableScheduledOperationsGrouping = new CheckboxItem();
        enableScheduledOperationsGrouping.setName(OPERATIONS_RANGE_SCHEDULED_ENABLED);
        enableScheduledOperationsGrouping.setTitle(" " + MSG.view_portlet_operations_config_show_next() + " ");
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
        sheduledOperationsLayout.addMember(fieldWrapper);

        //retrieve previous value otherwise initialize to true(live unlimited list)
        PropertySimple property = storedPortlet.getConfiguration().getSimple(OPERATIONS_RANGE_SCHEDULED_ENABLED);
        if (property != null) {
            enableScheduledOperationsGrouping.setValue(property.getBooleanValue());
        } else {
            enableScheduledOperationsGrouping.setValue(true);
        }

        //------------- Build second combobox for timeframe for problem resources search.
        final SelectItem maximumScheduledOperationsComboBox = new SelectItem(OPERATIONS_RANGE_SCHEDULED);
        maximumScheduledOperationsComboBox.setTitle("");
        maximumScheduledOperationsComboBox.setHint("<nobr><b> " + MSG.common_label_scheduled_operations()
            + ".</b></nobr>");
        maximumScheduledOperationsComboBox.setType("selection");
        //define acceptable values for display amount
        String[] acceptableDisplayValues = { "1", "5", "10", "15", unlimitedString };
        maximumScheduledOperationsComboBox.setValueMap(acceptableDisplayValues);
        maximumScheduledOperationsComboBox.setWidth(100);
        maximumScheduledOperationsComboBox.addChangeHandler(new ChangeHandler() {
            public void onChange(ChangeEvent event) {
                String selectedItem = "" + event.getValue();
                //stuff into the master form for retrieval
                form.setValue(OPERATIONS_RANGE_SCHEDULED, selectedItem);
            }
        });

        String retrieved = defaultValue;

        if ((property = storedPortlet.getConfiguration().getSimple(OPERATIONS_RANGE_SCHEDULED)) != null) {
            retrieved = property.getStringValue();
            // protect against legacy issue with non-numeric values
            try {
                Integer.parseInt(retrieved);
            } catch (NumberFormatException e) {
                retrieved = unlimited;
            }
        }

        //prepopulate the combobox with the previously stored selection
        String selectedValue = retrieved.equals(unlimited) ? unlimitedString : retrieved;

        //prepopulate the combobox with the previously stored selection
        maximumScheduledOperationsComboBox.setDefaultValue(selectedValue);
        DynamicForm fieldWrapper2 = new DynamicForm();
        fieldWrapper2.setFields(maximumScheduledOperationsComboBox);
        sheduledOperationsLayout.addMember(fieldWrapper2);
        column.addMember(sheduledOperationsLayout);
        form.addChild(column);

        //submit handler
        form.addSubmitValuesHandler(new SubmitValuesHandler() {
            @Override
            public void onSubmitValues(SubmitValuesEvent event) {
                //no need to insert validation here as user not allowed to enter values
                String value = (String) form.getValue(OPERATIONS_RANGE_SCHEDULED);
                if (value != null) {
                    // convert display string to stored integer if necessary
                    value = unlimitedString.equals(value) ? unlimited : value;

                    storedPortlet.getConfiguration().put(new PropertySimple(OPERATIONS_RANGE_SCHEDULED, value));
                }

                if (form.getValue(OPERATIONS_RANGE_SCHEDULED_ENABLED) != null) {//if new value supplied
                    storedPortlet.getConfiguration().put(
                        new PropertySimple(OPERATIONS_RANGE_SCHEDULED_ENABLED, form
                            .getValue(OPERATIONS_RANGE_SCHEDULED_ENABLED)));
                }
                storedPortlet.setConfiguration(storedPortlet.getConfiguration());
                configure(portletWindow, storedPortlet);
                refresh();
            }
        });

        return form;
    }

    public static final class Factory implements PortletViewFactory {
        public static final PortletViewFactory INSTANCE = new Factory();

        public final Portlet getInstance(EntityContext context) {

            return new OperationSchedulePortlet();
        }
    }

    public ConfigurationDefinition getConfigurationDefinition() {
        ConfigurationDefinition definition = new ConfigurationDefinition(MSG.view_portlet_configure_definitionTitle(),
            MSG.view_portlet_configure_definitionDesc());

        definition.put(new PropertyDefinitionSimple(OPERATIONS_RANGE_SCHEDULED, MSG
            .view_portlet_operations_config_scheduled_maximum(), true, PropertySimpleType.STRING));
        definition.put(new PropertyDefinitionSimple(OPERATIONS_RANGE_SCHEDULED_ENABLED, MSG
            .view_portlet_operations_config_scheduled_enable(), true, PropertySimpleType.BOOLEAN));

        return definition;
    }

    public ScheduledOperationsDataSource getDataSourceScheduled() {
        return this.dataSourceScheduled;
    }

    public ListGrid getScheduledOperationsGrid() {
        return this.scheduledOperationsGrid;
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

    // Custom refresh operation as we cannot directly extend Table because it only
    // contains one ListGrid while the OperationsPortlet displays two tables.    
    @Override
    public void refresh() {
        if (!isRefreshing()) {
            if (null != this.scheduledOperationsGrid) {
                this.scheduledOperationsGrid.invalidateCache();
            }
            markForRedraw();
        }
    }

}
