/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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
package org.rhq.coregui.client.dashboard.portlets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.smartgwt.client.types.MultipleAppearance;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.form.fields.PickerIcon;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.fields.events.FormItemClickHandler;
import com.smartgwt.client.widgets.form.fields.events.FormItemIconClickEvent;

import org.rhq.core.domain.alert.AlertFilter;
import org.rhq.core.domain.alert.AlertPriority;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.event.EventSeverity;
import org.rhq.core.domain.operation.OperationRequestStatus;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.ImageManager;
import org.rhq.coregui.client.Messages;
import org.rhq.coregui.client.components.measurement.CustomConfigMeasurementRangeEditor;
import org.rhq.coregui.client.util.MeasurementUtility;

/** Shared portlet configuration component where initial configuration settings
 *  and widgets shared across portlet editors is defined.
 *
 * @author Simeon Pinder
 */
public class PortletConfigurationEditorComponent {

    static Messages MSG = CoreGUI.getMessages();

    public interface Constant {
        String ALERT_PRIORITY = "ALERT_PRIORITY";
        String ALERT_PRIORITY_DEFAULT = ""; // no filtering
        String ALERT_FILTER = "ALERT_FILTER";
        String ALERT_FILTER_DEFAULT = ""; // no filtering
        String ALERT_NAME = "ALERT_NAME";
        String EVENT_SEVERITY = "EVENT_SEVERITY";
        String EVENT_SEVERITY_DEFAULT = ""; // no filtering
        String EVENT_SOURCE = "EVENT_SOURCE";
        String EVENT_RESOURCE = "EVENT_RESOURCE";
        String METRIC_RANGE_ENABLE = "METRIC_RANGE_ENABLE";
        String METRIC_RANGE_ENABLE_DEFAULT = String.valueOf(false); //disabled
        String METRIC_RANGE_BEGIN_END_FLAG = "METRIC_RANGE_BEGIN_END_FLAG";
        String METRIC_RANGE_BEGIN_END_FLAG_DEFAULT = String.valueOf(false);//disabled
        String METRIC_RANGE = "METRIC_RANGE";
        String METRIC_RANGE_DEFAULT = ""; //no previous range.
        String METRIC_RANGE_LASTN = "METRIC_RANGE_LASTN";
        String METRIC_RANGE_LASTN_DEFAULT = String.valueOf(8);
        String METRIC_RANGE_UNIT = "METRIC_RANGE_UNIT";
        String METRIC_RANGE_UNIT_DEFAULT = String.valueOf(MeasurementUtility.UNIT_HOURS);
        String RESULT_SORT_ORDER = "RESULT_SORT_ORDER";
        String RESULT_SORT_ORDER_DEFAULT = PageOrdering.DESC.name();//descending
        String RESULT_SORT_PRIORITY = "sort.priority";
        String RESULT_COUNT = "RESULT_COUNT";
        String RESULT_COUNT_DEFAULT = "5";
        String CUSTOM_REFRESH = "CUSTOM_REFRESH";
        String OPERATION_STATUS = "OPERATION_STATUS";
        String OPERATION_STATUS_DEFAULT = ""; // no filtering
        String CONFIG_UPDATE_STATUS = "CONFIG_UPDATE_STATUS";
        String CONFIG_UPDATE_STATUS_DEFAULT = ""; // no filtering
    }

    //configuration map initialization
    public static Map<String, String> CONFIG_PROPERTY_INITIALIZATION = new HashMap<String, String>();
    static {// Key, Default value
        CONFIG_PROPERTY_INITIALIZATION.put(Constant.ALERT_PRIORITY, Constant.ALERT_PRIORITY_DEFAULT);
        // Do not filter anything as default
        CONFIG_PROPERTY_INITIALIZATION.put(Constant.ALERT_FILTER, Constant.ALERT_FILTER_DEFAULT);
        CONFIG_PROPERTY_INITIALIZATION.put(Constant.ALERT_NAME, "");
        
        CONFIG_PROPERTY_INITIALIZATION.put(Constant.EVENT_SEVERITY, Constant.EVENT_SEVERITY_DEFAULT);
        CONFIG_PROPERTY_INITIALIZATION.put(Constant.EVENT_SOURCE, "");
        
        CONFIG_PROPERTY_INITIALIZATION.put(Constant.EVENT_RESOURCE, "");

        //result sort order, if empty initialize to "DESC"
        CONFIG_PROPERTY_INITIALIZATION.put(Constant.RESULT_SORT_ORDER, Constant.RESULT_SORT_ORDER_DEFAULT);
        //result count, if empty initialize to 5
        CONFIG_PROPERTY_INITIALIZATION.put(Constant.RESULT_COUNT, Constant.RESULT_COUNT_DEFAULT);
        //whether to specify time range for alerts. Defaults to false
        CONFIG_PROPERTY_INITIALIZATION.put(Constant.METRIC_RANGE_ENABLE, Constant.METRIC_RANGE_ENABLE_DEFAULT);
        //whether Begin and End values set for time. Aka. Advanced/full range setting Defaults to false
        CONFIG_PROPERTY_INITIALIZATION.put(Constant.METRIC_RANGE_BEGIN_END_FLAG,
            Constant.METRIC_RANGE_BEGIN_END_FLAG_DEFAULT);
        //whether in simple mode. Ex. 8 hrs. Defaults to 8
        CONFIG_PROPERTY_INITIALIZATION.put(Constant.METRIC_RANGE_LASTN, Constant.METRIC_RANGE_LASTN_DEFAULT);
        //whether in simple mode. Ex. hrs. Defaults to hours
        CONFIG_PROPERTY_INITIALIZATION.put(Constant.METRIC_RANGE_UNIT, Constant.METRIC_RANGE_UNIT_DEFAULT);
        //operation status, if empty initialize to "" i.e. all stati
        CONFIG_PROPERTY_INITIALIZATION.put(Constant.OPERATION_STATUS, Constant.OPERATION_STATUS_DEFAULT);
        //config update status, if empty initialize to "" i.e. all stati
        CONFIG_PROPERTY_INITIALIZATION.put(Constant.CONFIG_UPDATE_STATUS, Constant.CONFIG_UPDATE_STATUS_DEFAULT);
    }

    /* Single select combobox for number of items to display on the dashboard
     *
     * @return Populated selectItem instance.
     */
    public static SelectItem getResultCountEditor(Configuration portletConfig) {

        final SelectItem maximumResultsComboBox = new SelectItem(Constant.RESULT_COUNT);
        maximumResultsComboBox.setTitle(MSG.common_title_results_count());
        maximumResultsComboBox.setWrapTitle(false);
        maximumResultsComboBox.setTooltip("<nobr><b> " + MSG.common_title_results_count_tooltip() + "</b></nobr>");
        //spinder 9/3/10: the following is required workaround to disable editability of combobox.
        maximumResultsComboBox.setType("selection");
        //set width of dropdown display region
        maximumResultsComboBox.setWidth(100);

        //TODO: spinder 3/4/11 this is arbitrary. Get UXD input for better acceptable defaults
        int[] selectionValues = { 5, 10, 30, 100 };

        //define acceptable values for display amount
        String[] displayValues = new String[selectionValues.length];
        int i = 0;
        for (int selection : selectionValues) {
            displayValues[i++] = String.valueOf(selection);
        }
        maximumResultsComboBox.setValueMap(displayValues);
        //reload current settings if they exist, otherwise enable all.
        String currentValue = portletConfig.getSimpleValue(Constant.RESULT_COUNT, Constant.RESULT_COUNT_DEFAULT);
        if (currentValue.isEmpty()) {
            maximumResultsComboBox.setValue(Constant.RESULT_COUNT_DEFAULT);
        } else {
            maximumResultsComboBox.setValue(currentValue);
        }
        return maximumResultsComboBox;
    }

    public static TextItem getAlertNameEditor(Configuration portletConfig) {
        final TextItem alertNameEditor = new TextItem(Constant.ALERT_NAME);
        alertNameEditor.setTitle(MSG.common_title_name());
        alertNameEditor.setWrapTitle(false);
        alertNameEditor.setWidth(100);
        String currentValue = portletConfig.getSimpleValue(Constant.ALERT_NAME, "");
        alertNameEditor.setValue(currentValue);
        PickerIcon refreshFilter = new PickerIcon(PickerIcon.CLEAR, new FormItemClickHandler() {  
            public void onFormItemClick(FormItemIconClickEvent event) {  
                alertNameEditor.clearValue();
            }  
        });
        alertNameEditor.setIcons(refreshFilter);
        alertNameEditor.setIconPrompt("Resets the alert name filter.");
        return alertNameEditor;
    }
    
    public static TextItem getEventSourceEditor(Configuration portletConfig) {
        final TextItem eventSourceEditor = new TextItem(Constant.EVENT_SOURCE);
        eventSourceEditor.setTitle(MSG.view_alert_common_tab_conditions_type_event_matching());
        eventSourceEditor.setWrapTitle(false);
        eventSourceEditor.setWidth(100);
        String currentValue = portletConfig.getSimpleValue(Constant.EVENT_SOURCE, "");
        eventSourceEditor.setValue(currentValue);
        PickerIcon refreshFilter = new PickerIcon(PickerIcon.CLEAR, new FormItemClickHandler() {  
            public void onFormItemClick(FormItemIconClickEvent event) {  
                eventSourceEditor.clearValue();
            }  
        });
        eventSourceEditor.setIcons(refreshFilter);
        eventSourceEditor.setIconPrompt("Resets the event source filter.");
        return eventSourceEditor;
    }
    
    public static TextItem getEventResourceEditor(Configuration portletConfig) {
        final TextItem eventResourceEditor = new TextItem(Constant.EVENT_RESOURCE);
        eventResourceEditor.setTitle(MSG.common_title_resource());
        eventResourceEditor.setWrapTitle(false);
        eventResourceEditor.setWidth(100);
        String currentValue = portletConfig.getSimpleValue(Constant.EVENT_RESOURCE, "");
        eventResourceEditor.setValue(currentValue);
        PickerIcon refreshFilter = new PickerIcon(PickerIcon.CLEAR, new FormItemClickHandler() {  
            public void onFormItemClick(FormItemIconClickEvent event) {  
                eventResourceEditor.clearValue();
            }  
        });
        eventResourceEditor.setIcons(refreshFilter);
        eventResourceEditor.setIconPrompt("Resets the resource filter.");
        return eventResourceEditor;
    }
    
    /* Multiple select combobox for alert priorities to display on dashboard
     *
     * @return Populated selectItem instance.
     */
    public static SelectItem getAlertPriorityEditor(Configuration portletConfig) {
        SelectItem priorityFilter = new SelectItem(Constant.ALERT_PRIORITY, MSG.view_alerts_table_filter_priority());
        priorityFilter.setWrapTitle(false);
        priorityFilter.setWidth(200);
        priorityFilter.setMultiple(true);
        priorityFilter.setMultipleAppearance(MultipleAppearance.PICKLIST);

        LinkedHashMap<String, String> priorities = new LinkedHashMap<String, String>(3);
        priorities.put(AlertPriority.HIGH.name(), MSG.common_alert_high());
        priorities.put(AlertPriority.MEDIUM.name(), MSG.common_alert_medium());
        priorities.put(AlertPriority.LOW.name(), MSG.common_alert_low());
        LinkedHashMap<String, String> priorityIcons = new LinkedHashMap<String, String>(3);
        priorityIcons.put(AlertPriority.HIGH.name(), ImageManager.getAlertIcon(AlertPriority.HIGH));
        priorityIcons.put(AlertPriority.MEDIUM.name(), ImageManager.getAlertIcon(AlertPriority.MEDIUM));
        priorityIcons.put(AlertPriority.LOW.name(), ImageManager.getAlertIcon(AlertPriority.LOW));
        priorityFilter.setValueMap(priorities);
        priorityFilter.setValueIcons(priorityIcons);
        //reload current settings if they exist, otherwise enable all.
        String currentValue = portletConfig.getSimpleValue(Constant.ALERT_PRIORITY, Constant.ALERT_PRIORITY_DEFAULT);
        if (currentValue.trim().isEmpty() || currentValue.split(",").length == AlertPriority.values().length) {
            priorityFilter.setValues(AlertPriority.HIGH.name(), AlertPriority.MEDIUM.name(), AlertPriority.LOW.name());

        } else {
            //spinder:3/4/11 doing this nonsense due to some weird smartgwt issue with SelectItem in VLayout.
            if (currentValue.equalsIgnoreCase("HIGH")) {
                priorityFilter.setValues(AlertPriority.HIGH.name());
            } else if (currentValue.equalsIgnoreCase("HIGH,MEDIUM")) {
                priorityFilter.setValues(AlertPriority.HIGH.name(), AlertPriority.MEDIUM.name());
            } else if (currentValue.equalsIgnoreCase("HIGH,LOW")) {
                priorityFilter.setValues(AlertPriority.HIGH.name(), AlertPriority.LOW.name());
            } else if (currentValue.equalsIgnoreCase("MEDIUM")) {
                priorityFilter.setValues(AlertPriority.MEDIUM.name());
            } else if (currentValue.equalsIgnoreCase("MEDIUM,LOW")) {
                priorityFilter.setValues(AlertPriority.MEDIUM.name(), AlertPriority.LOW.name());
            } else {
                priorityFilter.setValues(AlertPriority.LOW.name());
            }
        }

        return priorityFilter;
    }

    /**
     * Multiple filter options, acknowledged, recovery alert, recovered

     * @return Populated selectItem instance
     */
    public static SelectItem getAlertFilterEditor(Configuration portletConfig) {
        SelectItem alertFilter = new SelectItem(Constant.ALERT_FILTER, MSG.view_alerts_table_filter_options());
        alertFilter.setWidth(325);
        alertFilter.setWrapTitle(false);
        alertFilter.setMultiple(true);
        alertFilter.setMultipleAppearance(MultipleAppearance.PICKLIST);

        LinkedHashMap<String, String> filters = new LinkedHashMap<String, String>(3);
        filters.put(AlertFilter.ACKNOWLEDGED_STATUS.name(), MSG.common_alert_filter_acknowledged_status());
        filters.put(AlertFilter.RECOVERED_STATUS.name(), MSG.common_alert_filter_recovered_status());
        filters.put(AlertFilter.RECOVERY_TYPE.name(), MSG.common_alert_filter_recovery_type());
        alertFilter.setValueMap(filters);

        // Populate
        String currentValue = portletConfig.getSimpleValue(Constant.ALERT_FILTER, Constant.ALERT_FILTER_DEFAULT);
        alertFilter.setValues(currentValue);

        return alertFilter;
    }
    
    /* Multiple select combobox for event severities to display on dashboard
    *
    * @return Populated selectItem instance.
    */
   public static SelectItem getEventSeverityEditor(Configuration portletConfig) {
       SelectItem severityFilter = new SelectItem(Constant.EVENT_SEVERITY, MSG.view_inventory_eventHistory_severityFilter());
       severityFilter.setWrapTitle(false);
       severityFilter.setWidth(200);
       severityFilter.setMultiple(true);
       severityFilter.setMultipleAppearance(MultipleAppearance.PICKLIST);

       LinkedHashMap<String, String> severities = new LinkedHashMap<String, String>(5);
       severities.put(EventSeverity.DEBUG.name(), MSG.common_severity_debug());
       severities.put(EventSeverity.INFO.name(), MSG.common_severity_info());
       severities.put(EventSeverity.WARN.name(), MSG.common_severity_warn());
       severities.put(EventSeverity.ERROR.name(), MSG.common_severity_error());
       severities.put(EventSeverity.FATAL.name(), MSG.common_severity_fatal());
       LinkedHashMap<String, String> severityIcons = new LinkedHashMap<String, String>(5);
       severityIcons.put(EventSeverity.DEBUG.name(), ImageManager.getEventSeverityIcon(EventSeverity.DEBUG));
       severityIcons.put(EventSeverity.INFO.name(), ImageManager.getEventSeverityIcon(EventSeverity.INFO));
       severityIcons.put(EventSeverity.WARN.name(), ImageManager.getEventSeverityIcon(EventSeverity.WARN));
       severityIcons.put(EventSeverity.ERROR.name(), ImageManager.getEventSeverityIcon(EventSeverity.ERROR));
       severityIcons.put(EventSeverity.FATAL.name(), ImageManager.getEventSeverityIcon(EventSeverity.FATAL));
       severityFilter.setValueMap(severities);
       severityFilter.setValueIcons(severityIcons);
       //reload current settings if they exist, otherwise enable all.
       String currentValue = portletConfig.getSimpleValue(Constant.EVENT_SEVERITY, Constant.EVENT_SEVERITY_DEFAULT);
       if (currentValue.trim().isEmpty() || currentValue.split(",").length == EventSeverity.values().length) {
            severityFilter.setValues(EventSeverity.DEBUG.name(), EventSeverity.INFO.name(), EventSeverity.WARN.name(),
                EventSeverity.ERROR.name(), EventSeverity.FATAL.name());

       } else {
           List<String> values = new ArrayList<String>(5);
           if (currentValue.toUpperCase().contains(EventSeverity.FATAL.name())) {
               values.add(EventSeverity.FATAL.name());
           } 
           if (currentValue.toUpperCase().contains(EventSeverity.ERROR.name())) {
               values.add(EventSeverity.ERROR.name());
           }
           if (currentValue.toUpperCase().contains(EventSeverity.WARN.name())) {
               values.add(EventSeverity.WARN.name());
           }
           if (currentValue.toUpperCase().contains(EventSeverity.INFO.name())) {
               values.add(EventSeverity.INFO.name());
           }
           if (currentValue.toUpperCase().contains(EventSeverity.DEBUG.name())) {
               values.add(EventSeverity.DEBUG.name());
           }
           severityFilter.setValues(values.toArray(new String[values.size()]));
       }

       return severityFilter;
   }
   
    /* Single select combobox for sort order of items to display on dashboard
     *
     * @return Populated selectItem instance.
     */
    public static SelectItem getResulSortOrderEditor(Configuration portletConfig) {
        SelectItem sortPrioritySelection = new SelectItem(Constant.RESULT_SORT_PRIORITY, MSG.common_title_sort_order());
        sortPrioritySelection.setWrapTitle(false);
        sortPrioritySelection.setTooltip(MSG.common_title_sort_order_tooltip());
        LinkedHashMap<String, String> priorities = new LinkedHashMap<String, String>(2);
        priorities.put(PageOrdering.ASC.name(), "Ascending");
        priorities.put(PageOrdering.DESC.name(), "Descending");
        LinkedHashMap<String, String> priorityIcons = new LinkedHashMap<String, String>(2);
        priorityIcons.put(PageOrdering.ASC.name(), "ascending");
        priorityIcons.put(PageOrdering.DESC.name(), "descending");

        sortPrioritySelection.setValueMap(priorities);
        sortPrioritySelection.setValueIcons(priorityIcons);
        //TODO: spinder 3/4/11 not sure why this is necessary. [SKIN] not being interpreted.
        String skinDir = "../org.rhq.coregui.CoreGUI/sc/skins/Enterprise/images";
        sortPrioritySelection.setImageURLPrefix(skinDir + "/actions/sort_");
        sortPrioritySelection.setImageURLSuffix(".png");

        //reload current settings if they exist, otherwise enable all.
        String currentValue = portletConfig.getSimpleValue(Constant.RESULT_SORT_ORDER,
            Constant.RESULT_SORT_ORDER_DEFAULT);
        if (currentValue.isEmpty()) {
            sortPrioritySelection.setDefaultValue(Constant.RESULT_SORT_ORDER_DEFAULT);
        } else {
            sortPrioritySelection.setDefaultValue(currentValue);
        }
        return sortPrioritySelection;
    }

    /** Convenience method to construct CustomConfigMeasurementRangeEditor instances.
     *
     * @param portletConfig
     * @return
     */
    public static CustomConfigMeasurementRangeEditor getMeasurementRangeEditor(Configuration portletConfig) {
        return new CustomConfigMeasurementRangeEditor(portletConfig);
    }

    public static SelectItem getOperationStatusEditor(Configuration portletConfig) {
        SelectItem priorityFilter = new SelectItem(Constant.OPERATION_STATUS, MSG.common_title_operation_status());
        priorityFilter.setWrapTitle(false);
        priorityFilter.setWidth(325);
        priorityFilter.setMultiple(true);
        priorityFilter.setMultipleAppearance(MultipleAppearance.PICKLIST);

        LinkedHashMap<String, String> stati = new LinkedHashMap<String, String>(4);
        stati.put(OperationRequestStatus.SUCCESS.name(), MSG.common_status_success());
        stati.put(OperationRequestStatus.INPROGRESS.name(), MSG.common_status_inprogress());
        stati.put(OperationRequestStatus.CANCELED.name(), MSG.common_status_canceled());
        stati.put(OperationRequestStatus.FAILURE.name(), MSG.common_status_failed());

        LinkedHashMap<String, String> statusIcons = new LinkedHashMap<String, String>(3);
        statusIcons.put(OperationRequestStatus.SUCCESS.name(),
            ImageManager.getOperationResultsIcon(OperationRequestStatus.SUCCESS));
        statusIcons.put(OperationRequestStatus.INPROGRESS.name(),
            ImageManager.getOperationResultsIcon(OperationRequestStatus.INPROGRESS));
        statusIcons.put(OperationRequestStatus.CANCELED.name(),
            ImageManager.getOperationResultsIcon(OperationRequestStatus.CANCELED));
        statusIcons.put(OperationRequestStatus.FAILURE.name(),
            ImageManager.getOperationResultsIcon(OperationRequestStatus.FAILURE));
        priorityFilter.setValueMap(stati);
        priorityFilter.setValueIcons(statusIcons);
        //reload current settings if they exist, otherwise enable all.
        String currentValue = portletConfig
            .getSimpleValue(Constant.OPERATION_STATUS, Constant.OPERATION_STATUS_DEFAULT);
        if (currentValue.isEmpty() || currentValue.split(",").length == OperationRequestStatus.values().length) {
            priorityFilter.setValues(OperationRequestStatus.SUCCESS.name(), OperationRequestStatus.INPROGRESS.name(),
                OperationRequestStatus.CANCELED.name(), OperationRequestStatus.FAILURE.name());
        } else {
            //spinder:3/4/11 doing this nonsense due to some weird smartgwt issue with SelectItem in VLayout.
            if (currentValue.equalsIgnoreCase(OperationRequestStatus.SUCCESS.name())) {
                priorityFilter.setValues(OperationRequestStatus.SUCCESS.name());
            } else if (currentValue.equalsIgnoreCase("SUCCESS,INPROGRESS,CANCELED,FAILURE")) {
                priorityFilter.setValues(OperationRequestStatus.SUCCESS.name(),
                    OperationRequestStatus.INPROGRESS.name(), OperationRequestStatus.CANCELED.name(),
                    OperationRequestStatus.FAILURE.name());
            } else if (currentValue.equalsIgnoreCase("SUCCESS,INPROGRESS,CANCELED")) {
                priorityFilter.setValues(OperationRequestStatus.SUCCESS.name(),
                    OperationRequestStatus.INPROGRESS.name(), OperationRequestStatus.CANCELED.name());
            } else if (currentValue.equalsIgnoreCase("SUCCESS,INPROGRESS,FAILURE")) {
                priorityFilter.setValues(OperationRequestStatus.SUCCESS.name(),
                    OperationRequestStatus.INPROGRESS.name(), OperationRequestStatus.FAILURE.name());
            } else if (currentValue.equalsIgnoreCase("SUCCESS,INPROGRESS")) {
                priorityFilter.setValues(OperationRequestStatus.SUCCESS.name(),
                    OperationRequestStatus.INPROGRESS.name());
            } else if (currentValue.equalsIgnoreCase("SUCCESS,CANCELED,FAILURE")) {
                priorityFilter.setValues(OperationRequestStatus.SUCCESS.name(), OperationRequestStatus.CANCELED.name(),
                    OperationRequestStatus.FAILURE.name());
            } else if (currentValue.equalsIgnoreCase("SUCCESS,CANCELED")) {
                priorityFilter.setValues(OperationRequestStatus.SUCCESS.name(), OperationRequestStatus.CANCELED.name());
            } else if (currentValue.equalsIgnoreCase("SUCCESS,FAILURE")) {
                priorityFilter.setValues(OperationRequestStatus.SUCCESS.name(), OperationRequestStatus.FAILURE.name());
            } else if (currentValue.equalsIgnoreCase("INPROGRESS")) {
                priorityFilter.setValues(OperationRequestStatus.INPROGRESS.name());
            } else if (currentValue.equalsIgnoreCase("INPROGRESS,CANCELED,FAILURE")) {
                priorityFilter.setValues(OperationRequestStatus.INPROGRESS.name(),
                    OperationRequestStatus.CANCELED.name(), OperationRequestStatus.FAILURE.name());
            } else if (currentValue.equalsIgnoreCase("INPROGRESS,CANCELED")) {
                priorityFilter.setValues(OperationRequestStatus.INPROGRESS.name(),
                    OperationRequestStatus.CANCELED.name());
            } else if (currentValue.equalsIgnoreCase("INPROGRESS,FAILURE")) {
                priorityFilter.setValues(OperationRequestStatus.INPROGRESS.name(),
                    OperationRequestStatus.FAILURE.name());
            } else if (currentValue.equalsIgnoreCase("CANCELED")) {
                priorityFilter.setValues(OperationRequestStatus.CANCELED.name());
            } else if (currentValue.equalsIgnoreCase("CANCELED,FAILURE")) {
                priorityFilter.setValues(OperationRequestStatus.CANCELED.name(), OperationRequestStatus.FAILURE.name());
            } else if (currentValue.equalsIgnoreCase("FAILURE")) {
                priorityFilter.setValues(OperationRequestStatus.FAILURE.name());
            }
        }
        return priorityFilter;
    }

    public static SelectItem getConfigurationUpdateStatusEditor(Configuration portletConfig) {
        SelectItem priorityFilter = new SelectItem(Constant.CONFIG_UPDATE_STATUS, MSG.common_title_updateStatus());
        priorityFilter.setWrapTitle(false);
        priorityFilter.setWidth(335);
        priorityFilter.setMultiple(true);
        priorityFilter.setMultipleAppearance(MultipleAppearance.PICKLIST);

        LinkedHashMap<String, String> stati = new LinkedHashMap<String, String>(4);
        stati.put(ConfigurationUpdateStatus.SUCCESS.name(), MSG.common_status_success());
        stati.put(ConfigurationUpdateStatus.INPROGRESS.name(), MSG.common_status_inprogress());
        stati.put(ConfigurationUpdateStatus.NOCHANGE.name(), MSG.common_status_nochange());
        stati.put(ConfigurationUpdateStatus.FAILURE.name(), MSG.common_status_failed());

        LinkedHashMap<String, String> statusIcons = new LinkedHashMap<String, String>(3);
        statusIcons.put(ConfigurationUpdateStatus.SUCCESS.name(),
            ImageManager.getResourceConfigurationIcon(ConfigurationUpdateStatus.SUCCESS));
        statusIcons.put(ConfigurationUpdateStatus.INPROGRESS.name(),
            ImageManager.getResourceConfigurationIcon(ConfigurationUpdateStatus.INPROGRESS));
        statusIcons.put(ConfigurationUpdateStatus.NOCHANGE.name(),
            ImageManager.getResourceConfigurationIcon(ConfigurationUpdateStatus.NOCHANGE));
        statusIcons.put(ConfigurationUpdateStatus.FAILURE.name(),
            ImageManager.getResourceConfigurationIcon(ConfigurationUpdateStatus.FAILURE));
        priorityFilter.setValueMap(stati);
        priorityFilter.setValueIcons(statusIcons);
        //reload current settings if they exist, otherwise enable all.
        String currentValue = portletConfig.getSimpleValue(Constant.CONFIG_UPDATE_STATUS,
            Constant.CONFIG_UPDATE_STATUS_DEFAULT);
        if (currentValue.isEmpty() || currentValue.split(",").length == ConfigurationUpdateStatus.values().length) {
            priorityFilter.setValues(ConfigurationUpdateStatus.SUCCESS.name(),
                ConfigurationUpdateStatus.INPROGRESS.name(), ConfigurationUpdateStatus.NOCHANGE.name(),
                ConfigurationUpdateStatus.FAILURE.name());
        } else {
            //spinder:3/4/11 doing this nonsense due to some weird smartgwt issue with SelectItem in VLayout.
            if (currentValue.equalsIgnoreCase(ConfigurationUpdateStatus.SUCCESS.name())) {
                priorityFilter.setValues(ConfigurationUpdateStatus.SUCCESS.name());
            } else if (currentValue.equalsIgnoreCase("SUCCESS,INPROGRESS,NOCHANGE,FAILURE")) {
                priorityFilter.setValues(ConfigurationUpdateStatus.SUCCESS.name(),
                    ConfigurationUpdateStatus.INPROGRESS.name(), ConfigurationUpdateStatus.NOCHANGE.name(),
                    ConfigurationUpdateStatus.FAILURE.name());
            } else if (currentValue.equalsIgnoreCase("SUCCESS,INPROGRESS,NOCHANGE")) {
                priorityFilter.setValues(ConfigurationUpdateStatus.SUCCESS.name(),
                    ConfigurationUpdateStatus.INPROGRESS.name(), ConfigurationUpdateStatus.NOCHANGE.name());
            } else if (currentValue.equalsIgnoreCase("SUCCESS,INPROGRESS,FAILURE")) {
                priorityFilter.setValues(ConfigurationUpdateStatus.SUCCESS.name(),
                    ConfigurationUpdateStatus.INPROGRESS.name(), ConfigurationUpdateStatus.FAILURE.name());
            } else if (currentValue.equalsIgnoreCase("SUCCESS,INPROGRESS")) {
                priorityFilter.setValues(ConfigurationUpdateStatus.SUCCESS.name(),
                    ConfigurationUpdateStatus.INPROGRESS.name());
            } else if (currentValue.equalsIgnoreCase("SUCCESS,NOCHANGE,FAILURE")) {
                priorityFilter.setValues(ConfigurationUpdateStatus.SUCCESS.name(),
                    ConfigurationUpdateStatus.NOCHANGE.name(), ConfigurationUpdateStatus.FAILURE.name());
            } else if (currentValue.equalsIgnoreCase("SUCCESS,NOCHANGE")) {
                priorityFilter.setValues(ConfigurationUpdateStatus.SUCCESS.name(),
                    ConfigurationUpdateStatus.NOCHANGE.name());
            } else if (currentValue.equalsIgnoreCase("SUCCESS,FAILURE")) {
                priorityFilter.setValues(ConfigurationUpdateStatus.SUCCESS.name(),
                    ConfigurationUpdateStatus.FAILURE.name());
            } else if (currentValue.equalsIgnoreCase("INPROGRESS")) {
                priorityFilter.setValues(ConfigurationUpdateStatus.INPROGRESS.name());
            } else if (currentValue.equalsIgnoreCase("INPROGRESS,NOCHANGE,FAILURE")) {
                priorityFilter.setValues(ConfigurationUpdateStatus.INPROGRESS.name(),
                    ConfigurationUpdateStatus.NOCHANGE.name(), ConfigurationUpdateStatus.FAILURE.name());
            } else if (currentValue.equalsIgnoreCase("INPROGRESS,NOCHANGE")) {
                priorityFilter.setValues(ConfigurationUpdateStatus.INPROGRESS.name(),
                    ConfigurationUpdateStatus.NOCHANGE.name());
            } else if (currentValue.equalsIgnoreCase("INPROGRESS,FAILURE")) {
                priorityFilter.setValues(ConfigurationUpdateStatus.INPROGRESS.name(),
                    ConfigurationUpdateStatus.FAILURE.name());
            } else if (currentValue.equalsIgnoreCase("NOCHANGE")) {
                priorityFilter.setValues(ConfigurationUpdateStatus.NOCHANGE.name());
            } else if (currentValue.equalsIgnoreCase("NOCHANGE,FAILURE")) {
                priorityFilter.setValues(ConfigurationUpdateStatus.NOCHANGE.name(),
                    ConfigurationUpdateStatus.FAILURE.name());
            } else if (currentValue.equalsIgnoreCase("FAILURE")) {
                priorityFilter.setValues(ConfigurationUpdateStatus.FAILURE.name());
            }
        }
        return priorityFilter;
    }

}
