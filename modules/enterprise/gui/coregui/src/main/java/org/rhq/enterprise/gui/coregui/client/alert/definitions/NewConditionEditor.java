/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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

package org.rhq.enterprise.gui.coregui.client.alert.definitions;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;

import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.FormItemIfFunction;
import com.smartgwt.client.widgets.form.fields.ButtonItem;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.SpacerItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.fields.events.ClickEvent;
import com.smartgwt.client.widgets.form.fields.events.ClickHandler;

import org.rhq.core.domain.alert.AlertCondition;
import org.rhq.core.domain.alert.AlertConditionCategory;
import org.rhq.core.domain.event.EventSeverity;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.operation.OperationRequestStatus;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;

/**
 * @author John Mazzitelli
 */
public class NewConditionEditor extends LocatableDynamicForm {

    private HashSet<AlertCondition> conditions; // the new condition we create goes into this set
    private SelectItem conditionTypeSelectItem;

    public NewConditionEditor(String locatorId, HashSet<AlertCondition> conditions) {
        super(locatorId);
        this.conditions = conditions;
    }

    @Override
    protected void onInit() {
        super.onInit();

        setMargin(20);

        conditionTypeSelectItem = new SelectItem("conditionType", "Condition Type");
        LinkedHashMap<String, String> condTypes = new LinkedHashMap<String, String>(7);
        condTypes.put(AlertConditionCategory.THRESHOLD.name(), "Measurement Absolute Value Threshold");
        condTypes.put(AlertConditionCategory.BASELINE.name(), "Measurement Baseline Threshold");
        condTypes.put(AlertConditionCategory.CHANGE.name(), "Measurement Value Change");
        condTypes.put(AlertConditionCategory.TRAIT.name(), "Trait Value Change");
        condTypes.put(AlertConditionCategory.AVAILABILITY.name(), "Availability Change");
        condTypes.put(AlertConditionCategory.CONTROL.name(), "Operation Execution");
        condTypes.put(AlertConditionCategory.EVENT.name(), "Event Detection");
        conditionTypeSelectItem.setValueMap(condTypes);
        conditionTypeSelectItem.setDefaultValue(AlertConditionCategory.THRESHOLD.name());
        conditionTypeSelectItem.setWrapTitle(false);
        conditionTypeSelectItem.setRedrawOnChange(true);
        conditionTypeSelectItem.setWidth("*");

        SpacerItem spacer = new SpacerItem();
        spacer.setColSpan(2);
        spacer.setHeight(5);

        ButtonItem ok = new ButtonItem("buttonItem", "OK");
        ok.setColSpan(2);
        ok.setAlign(Alignment.CENTER);
        ok.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                // TODO
                SC.say("Not yet implemented");
            }
        });

        ArrayList<FormItem> formItems = new ArrayList<FormItem>();
        formItems.add(conditionTypeSelectItem);
        formItems.add(spacer);
        formItems.addAll(buildMetricThresholdFormItems());
        formItems.addAll(buildMetricBaselineFormItems());
        formItems.addAll(buildMetricChangeFormItems());
        formItems.addAll(buildTraitChangeFormItems());
        formItems.addAll(buildAvailabilityChangeFormItems());
        formItems.addAll(buildOperationFormItems());
        formItems.addAll(buildEventFormItems());
        formItems.add(ok);

        setFields(formItems.toArray(new FormItem[formItems.size()]));
    };

    private ArrayList<FormItem> buildMetricThresholdFormItems() {
        ArrayList<FormItem> formItems = new ArrayList<FormItem>();

        ShowIfCategoryFunction ifFunc = new ShowIfCategoryFunction(AlertConditionCategory.THRESHOLD);

        String helpStr = "Specify the threshold value that, when violated, triggers the condition. The value you specify is an absolute value with an optional units specifier.";
        StaticTextItem helpItem = buildHelpTextItem("thresholdHelp", helpStr, ifFunc);
        formItems.add(helpItem);

        formItems.add(buildMetricDropDownMenu("thresholdMetric", ifFunc));
        formItems.add(buildComparatorDropDownMenu("thresholdComparator", ifFunc));

        TextItem absoluteValue = new TextItem("metricAbsoluteValue", "Metric Value");
        absoluteValue.setWrapTitle(false);
        absoluteValue.setRequired(true);

        absoluteValue.setShowIfCondition(ifFunc);
        formItems.add(absoluteValue);

        return formItems;
    }

    private ArrayList<FormItem> buildMetricBaselineFormItems() {
        ArrayList<FormItem> formItems = new ArrayList<FormItem>();

        ShowIfCategoryFunction ifFunc = new ShowIfCategoryFunction(AlertConditionCategory.BASELINE);

        String helpStr = "Specify the baseline value that must be violated to trigger the condition. The value you specify is a percentage of the given baseline value.";
        StaticTextItem helpItem = buildHelpTextItem("baselineHelp", helpStr, ifFunc);
        formItems.add(helpItem);

        formItems.add(buildMetricDropDownMenu("baselineMetric", ifFunc));
        formItems.add(buildComparatorDropDownMenu("baselineComparator", ifFunc));

        TextItem baselinePercentage = new TextItem("baselinePercentage", "Baseline Percentage");
        baselinePercentage.setWrapTitle(false);
        baselinePercentage.setRequired(true);
        baselinePercentage.setShowIfCondition(ifFunc);
        formItems.add(baselinePercentage);

        SelectItem baselineSelection = new SelectItem("baselineSelection", "Baseline");
        LinkedHashMap<String, String> baselines = new LinkedHashMap<String, String>(3);
        baselines.put("min", "Minimum"); // title should have the current value of the min baseline
        baselines.put("avg", "Baseline"); // title should have the current value of the avg baseline
        baselines.put("max", "Maximum"); // title should have the current value of the max baseline
        baselineSelection.setValueMap(baselines);
        baselineSelection.setDefaultValue("avg");
        baselineSelection.setWidth("*");
        baselineSelection.setWrapTitle(false);
        baselineSelection.setShowIfCondition(ifFunc);
        formItems.add(baselineSelection);

        return formItems;
    }

    private ArrayList<FormItem> buildMetricChangeFormItems() {
        ArrayList<FormItem> formItems = new ArrayList<FormItem>();

        ShowIfCategoryFunction ifFunc = new ShowIfCategoryFunction(AlertConditionCategory.CHANGE);

        String helpStr = "Specify the metric whose value must change to trigger the condition.";
        StaticTextItem helpItem = buildHelpTextItem("changeMetricHelp", helpStr, ifFunc);
        formItems.add(helpItem);

        formItems.add(buildMetricDropDownMenu("changeMetric", ifFunc));

        return formItems;
    }

    private ArrayList<FormItem> buildTraitChangeFormItems() {
        ArrayList<FormItem> formItems = new ArrayList<FormItem>();

        ShowIfCategoryFunction ifFunc = new ShowIfCategoryFunction(AlertConditionCategory.TRAIT);

        String helpStr = "Specify the trait whose value must change to trigger the condition.";
        StaticTextItem helpItem = buildHelpTextItem("traitHelp", helpStr, ifFunc);
        formItems.add(helpItem);

        LinkedHashMap<String, String> traits = new LinkedHashMap<String, String>();
        // TODO
        traits.put("dummy trait", "Dummy Trait Name");

        SelectItem traitSelection = new SelectItem("trait", "Trait");
        traitSelection.setValueMap(traits);
        traitSelection.setDefaultValue(traits.keySet().iterator().next()); // just use the first one
        traitSelection.setWidth("*");
        traitSelection.setShowIfCondition(ifFunc);
        formItems.add(traitSelection);

        return formItems;
    }

    private ArrayList<FormItem> buildAvailabilityChangeFormItems() {
        ArrayList<FormItem> formItems = new ArrayList<FormItem>();

        ShowIfCategoryFunction ifFunc = new ShowIfCategoryFunction(AlertConditionCategory.AVAILABILITY);

        String helpStr = "Specify the availability state change that will trigger the condition.";
        StaticTextItem helpItem = buildHelpTextItem("availabilityHelp", helpStr, ifFunc);
        formItems.add(helpItem);

        SelectItem selection = new SelectItem("availability", "Availability");
        LinkedHashMap<String, String> avails = new LinkedHashMap<String, String>(2);
        avails.put(AvailabilityType.UP.name(), "Goes UP");
        avails.put(AvailabilityType.DOWN.name(), "Goes DOWN");
        selection.setValueMap(avails);
        selection.setDefaultValue(AvailabilityType.DOWN.name());
        selection.setShowIfCondition(ifFunc);

        formItems.add(selection);
        return formItems;
    }

    private ArrayList<FormItem> buildOperationFormItems() {
        ArrayList<FormItem> formItems = new ArrayList<FormItem>();

        ShowIfCategoryFunction ifFunc = new ShowIfCategoryFunction(AlertConditionCategory.CONTROL);

        String helpStr = "Specify the result that must occur when the selected operation is executed in order to trigger the condition.";
        StaticTextItem helpItem = buildHelpTextItem("operationHelp", helpStr, ifFunc);
        formItems.add(helpItem);

        LinkedHashMap<String, String> ops = new LinkedHashMap<String, String>();
        // TODO
        ops.put("dummy op", "Dummy Op Name");

        SelectItem opSelection = new SelectItem("operation", "Operation");
        opSelection.setValueMap(ops);
        opSelection.setDefaultValue(ops.keySet().iterator().next()); // just use the first one
        opSelection.setShowIfCondition(ifFunc);
        formItems.add(opSelection);

        SelectItem opResultsSelection = new SelectItem("operationResults", "Operation Status");
        LinkedHashMap<String, String> operationStatuses = new LinkedHashMap<String, String>(4);
        operationStatuses.put(OperationRequestStatus.INPROGRESS.name(), OperationRequestStatus.INPROGRESS.name());
        operationStatuses.put(OperationRequestStatus.SUCCESS.name(), OperationRequestStatus.SUCCESS.name());
        operationStatuses.put(OperationRequestStatus.FAILURE.name(), OperationRequestStatus.FAILURE.name());
        operationStatuses.put(OperationRequestStatus.CANCELED.name(), OperationRequestStatus.CANCELED.name());
        opResultsSelection.setValueMap(operationStatuses);
        opResultsSelection.setDefaultValue(OperationRequestStatus.FAILURE.name());
        opResultsSelection.setWidth("*");
        opResultsSelection.setWrapTitle(false);
        opResultsSelection.setShowIfCondition(ifFunc);
        formItems.add(opResultsSelection);

        return formItems;
    }

    private ArrayList<FormItem> buildEventFormItems() {
        ArrayList<FormItem> formItems = new ArrayList<FormItem>();

        ShowIfCategoryFunction ifFunc = new ShowIfCategoryFunction(AlertConditionCategory.EVENT);

        String helpStr = "Specify the event severity that an event message must be reported with in order to trigger this condition. If you specify an optional regular expression, the event message must also match that regular expression in order for the condition to trigger.";
        StaticTextItem helpItem = buildHelpTextItem("eventHelp", helpStr, ifFunc);
        formItems.add(helpItem);

        SelectItem eventSeveritySelection = new SelectItem("eventSeverity", "Event Severity");
        LinkedHashMap<String, String> severities = new LinkedHashMap<String, String>(5);
        severities.put(EventSeverity.DEBUG.name(), EventSeverity.DEBUG.name());
        severities.put(EventSeverity.INFO.name(), EventSeverity.INFO.name());
        severities.put(EventSeverity.WARN.name(), EventSeverity.WARN.name());
        severities.put(EventSeverity.ERROR.name(), EventSeverity.ERROR.name());
        severities.put(EventSeverity.FATAL.name(), EventSeverity.FATAL.name());
        eventSeveritySelection.setValueMap(severities);
        eventSeveritySelection.setDefaultValue(EventSeverity.ERROR.name());
        eventSeveritySelection.setWrapTitle(false);
        eventSeveritySelection.setShowIfCondition(ifFunc);
        formItems.add(eventSeveritySelection);

        TextItem eventRegex = new TextItem("eventRegex", "Regular Expression");
        eventRegex.setRequired(false);
        eventRegex.setWrapTitle(false);
        eventRegex.setShowIfCondition(ifFunc);
        formItems.add(eventRegex);

        return formItems;
    }

    private SelectItem buildMetricDropDownMenu(String itemName, FormItemIfFunction ifFunc) {

        LinkedHashMap<String, String> metrics = new LinkedHashMap<String, String>();
        metrics.put("dummy metric", "Dummy Metric Name");

        SelectItem metricSelection = new SelectItem(itemName, "Metric");
        metricSelection.setValueMap(metrics);
        metricSelection.setDefaultValue(metrics.keySet().iterator().next()); // just use the first one
        metricSelection.setWidth("*");
        metricSelection.setShowIfCondition(ifFunc);
        return metricSelection;
    }

    private SelectItem buildComparatorDropDownMenu(String itemName, FormItemIfFunction ifFunc) {

        LinkedHashMap<String, String> comparators = new LinkedHashMap<String, String>();
        comparators.put("<", "< (Less than)");
        comparators.put("=", "= (Equal to)");
        comparators.put(">", "> (Greater than)");

        SelectItem comparatorSelection = new SelectItem(itemName, "Comparator");
        comparatorSelection.setValueMap(comparators);
        comparatorSelection.setDefaultValue("<");
        comparatorSelection.setShowIfCondition(ifFunc);
        return comparatorSelection;
    }

    private StaticTextItem buildHelpTextItem(String itemName, String helpText, FormItemIfFunction ifFunc) {
        StaticTextItem help = new StaticTextItem(itemName);
        help.setShowTitle(false);
        help.setColSpan(2);
        help.setRowSpan(2);
        help.setWrap(true);
        help.setValue(helpText);
        help.setShowIfCondition(ifFunc);
        return help;
    }

    private class ShowIfCategoryFunction implements FormItemIfFunction {
        private final AlertConditionCategory category;

        public ShowIfCategoryFunction(AlertConditionCategory category) {
            this.category = category;
        }

        public boolean execute(FormItem item, Object value, DynamicForm form) {
            return category.name().equals(form.getValue("conditionType").toString());
        }
    }
}
