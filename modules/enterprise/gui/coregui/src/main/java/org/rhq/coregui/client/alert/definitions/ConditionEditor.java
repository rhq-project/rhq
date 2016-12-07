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

package org.rhq.coregui.client.alert.definitions;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.FormItemIfFunction;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.SpacerItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.toolbar.ToolStrip;

import org.rhq.core.domain.alert.AlertCondition;
import org.rhq.core.domain.alert.AlertConditionCategory;
import org.rhq.core.domain.alert.AlertConditionOperator;
import org.rhq.core.domain.alert.BooleanExpression;
import org.rhq.core.domain.event.EventSeverity;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementUnits;
import org.rhq.core.domain.measurement.NumericType;
import org.rhq.core.domain.measurement.composite.MeasurementNumericValueAndUnits;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.operation.OperationRequestStatus;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.components.form.DurationItem;
import org.rhq.coregui.client.components.form.NumberWithUnitsValidator;
import org.rhq.coregui.client.components.form.SortedSelectItem;
import org.rhq.coregui.client.components.form.TimeUnit;
import org.rhq.coregui.client.util.MeasurementConverterClient;
import org.rhq.coregui.client.util.enhanced.EnhancedIButton;
import org.rhq.coregui.client.util.enhanced.EnhancedVLayout;
import org.rhq.coregui.client.util.enhanced.EnhancedIButton.ButtonColor;
import org.rhq.coregui.client.util.measurement.MeasurementParser;
import org.rhq.coregui.client.util.message.Message;
import org.rhq.coregui.client.util.message.Message.Severity;

/**
 * @author John Mazzitelli
 */
public class ConditionEditor extends EnhancedVLayout {

    // these aren't "real" calltime condition categories (not real AlertConditionCategory enums)
    // but we need these values for the drop down menu selections
    private static final String ALERT_CONDITION_CATEGORY_CALLTIME_CHANGE = "calltime-change";
    private static final String ALERT_CONDITION_CATEGORY_CALLTIME_THRESHOLD = "calltime-threshold";

    private static final String AVAILABILITY_ITEMNAME = "availability";
    private static final String AVAILABILITY_DURATION_ITEMNAME = "availabilityDuration";
    private static final String AVAILABILITY_DURATION_VAL_ITEMNAME = "availabilityDurationVal";
    private static final String THRESHOLD_METRIC_ITEMNAME = "thresholdMetric";
    private static final String THRESHOLD_COMPARATOR_ITEMNAME = "thresholdComparator";
    private static final String THRESHOLD_ABSVALUE_ITEMNAME = "metricAbsoluteValue";
    private static final String THRESHOLD_NO_METRICS_ITEMNAME = "thresholdNoMetrics";
    private static final String BASELINE_METRIC_ITEMNAME = "baselineMetric";
    private static final String BASELINE_COMPARATOR_ITEMNAME = "baselineComparator";
    private static final String BASELINE_PERCENTAGE_ITEMNAME = "baselinePercentage";
    private static final String BASELINE_SELECTION_ITEMNAME = "baselineSelection";
    private static final String BASELINE_NO_METRICS_ITEMNAME = "baselineNoMetrics";
    private static final String CHANGE_METRIC_ITEMNAME = "changeMetric";
    private static final String CHANGE_NO_METRICS_ITEMNAME = "changeNoMetrics";
    private static final String CALLTIME_THRESHOLD_METRIC_ITEMNAME = "calltimeThresholdMetric";
    private static final String CALLTIME_THRESHOLD_MINMAXAVG_ITEMNAME = "calltimeThresholdMinMaxAvgSelection";
    private static final String CALLTIME_THRESHOLD_COMPARATOR_ITEMNAME = "calltimeThresholdComparator";
    private static final String CALLTIME_THRESHOLD_ABSVALUE_ITEMNAME = "calltimeThresholdAbsoluteValue";
    private static final String CALLTIME_THRESHOLD_REGEX_ITEMNAME = "calltimeThresholdRegex";
    private static final String CALLTIME_CHANGE_METRIC_ITEMNAME = "calltimeChangeMetric";
    private static final String CALLTIME_CHANGE_MINMAXAVG_ITEMNAME = "calltimeChangeMinMaxAvgSelection";
    private static final String CALLTIME_CHANGE_COMPARATOR_ITEMNAME = "calltimeChangeComparator";
    private static final String CALLTIME_CHANGE_PERCENTAGE_ITEMNAME = "calltimeChangePercentageValue";
    private static final String CALLTIME_CHANGE_REGEX_ITEMNAME = "calltimeChangeRegex";
    private static final String TRAIT_METRIC_ITEMNAME = "trait";
    private static final String TRAIT_REGEX_ITEMNAME = "traitRegex";
    private static final String OPERATION_NAME_ITEMNAME = "operation";
    private static final String OPERATION_RESULTS_ITEMNAME = "operationResults";
    private static final String EVENT_SEVERITY_ITEMNAME = "eventSeverity";
    private static final String EVENT_REGEX_ITEMNAME = "eventRegex";
    private static final String EVENT_SOURCE_PATH_REGEX_ITEMNAME = "eventSourcePathRegex";
    private static final String DRIFT_DEFNAME_REGEX_ITEMNAME = "driftDefNameRegex";
    private static final String DRIFT_PATHNAME_REGEX_ITEMNAME = "driftPathNameRegex";
    private static final String RANGE_METRIC_ITEMNAME = "rangeMetric";
    private static final String RANGE_COMPARATOR_ITEMNAME = "rangeComparator";
    private static final String RANGE_LO_ABSVALUE_ITEMNAME = "rangeMetricLoValue";
    private static final String RANGE_HI_ABSVALUE_ITEMNAME = "rangeMetricHiValue";
    private static final String RANGE_NO_METRICS_ITEMNAME = "rangeNoMetrics";

    private DynamicForm form;
    private SelectItem conditionTypeSelectItem;
    // the new condition we create goes into this set
    private HashSet<AlertCondition> conditions;

    // the new conditions that already exist in db and are modified
    private Map<Integer, AlertCondition> modifiedConditions;
    private final SelectItem conditionExpression; // this is the GWT menu where the user selects ALL or ANY conjunction
    private boolean supportsMetrics = false;
    private boolean supportsCalltimeMetrics = false;
    private boolean supportsTraits = false;
    private boolean supportsOperations = false;
    private boolean supportsEvents = false;
    private boolean supportsResourceConfig = false;
    private boolean supportsDrift = false;
    private Runnable closeFunction; // this is called after a button is pressed and the editor should close
    private ResourceType resourceType;
    private boolean editMode = false;
    private AlertCondition existingCondition;

    public ConditionEditor(HashSet<AlertCondition> conditions, Map<Integer, AlertCondition> modifiedConditions,
        SelectItem conditionExpression, ResourceType rtype, AlertCondition existingCondition, Runnable closeFunc) {
        super();
        this.editMode = existingCondition != null;
        this.existingCondition = existingCondition;
        this.conditions = conditions;
        this.modifiedConditions = modifiedConditions;
        this.conditionExpression = conditionExpression;
        this.closeFunction = closeFunc;
        this.resourceType = rtype;

        this.supportsEvents = (rtype.getEventDefinitions() != null && rtype.getEventDefinitions().size() > 0);
        this.supportsResourceConfig = (rtype.getResourceConfigurationDefinition() != null);
        this.supportsDrift = (rtype.getDriftDefinitionTemplates() != null && rtype.getDriftDefinitionTemplates().size() > 0);

        Set<MeasurementDefinition> metricDefinitions = rtype.getMetricDefinitions();
        if (metricDefinitions != null && metricDefinitions.size() > 0) {
            for (MeasurementDefinition measurementDefinition : metricDefinitions) {
                switch (measurementDefinition.getDataType()) {
                case MEASUREMENT: {
                    this.supportsMetrics = true;
                    break;
                }
                case CALLTIME: {
                    this.supportsCalltimeMetrics = true;
                    break;
                }
                case TRAIT: {
                    this.supportsTraits = true;
                    break;
                }
                default: {
                    break;
                }
                }
            }
        }

        Set<OperationDefinition> operationDefinitions = rtype.getOperationDefinitions();
        if (operationDefinitions != null && operationDefinitions.size() > 0) {
            this.supportsOperations = true;
        }
    }

    @Override
    protected void onInit() {
        super.onInit();
        initForm();

        HLayout wrapper = new HLayout();
        wrapper.setLayoutMargin(20);
        wrapper.setAlign(Alignment.CENTER);
        wrapper.setDefaultLayoutAlign(Alignment.CENTER);
        wrapper.setMembersMargin(20);
        wrapper.addMember(form);
        addMember(wrapper);
        addMember(buildToolStrip());
    }

    private void initForm() {
        conditionTypeSelectItem = new SortedSelectItem("conditionType",
            MSG.view_alert_definition_condition_editor_option_label());
        LinkedHashMap<String, String> condTypes = new LinkedHashMap<String, String>(7);
        Map<String, String> allMessages = new HashMap<String, String>(13);
        allMessages.put(AlertConditionCategory.AVAILABILITY.name(),
            MSG.view_alert_definition_condition_editor_option_availability());
        allMessages.put(AlertConditionCategory.AVAIL_DURATION.name(),
            MSG.view_alert_definition_condition_editor_availabilityDuration());
        allMessages.put(AlertConditionCategory.THRESHOLD.name(),
            MSG.view_alert_definition_condition_editor_option_metric_threshold());
        allMessages.put(AlertConditionCategory.BASELINE.name(),
            MSG.view_alert_definition_condition_editor_option_metric_baseline());
        allMessages.put(AlertConditionCategory.CHANGE.name(),
            MSG.view_alert_definition_condition_editor_option_metric_change());
        allMessages.put(AlertConditionCategory.RANGE.name(),
            MSG.view_alert_definition_condition_editor_option_metric_range());
        allMessages.put(ALERT_CONDITION_CATEGORY_CALLTIME_THRESHOLD,
            MSG.view_alert_definition_condition_editor_option_metric_calltime_threshold());
        allMessages.put(ALERT_CONDITION_CATEGORY_CALLTIME_CHANGE,
            MSG.view_alert_definition_condition_editor_option_metric_calltime_change());
        allMessages.put(AlertConditionCategory.TRAIT.name(),
            MSG.view_alert_definition_condition_editor_option_metric_trait_change());
        allMessages.put(AlertConditionCategory.CONTROL.name(),
            MSG.view_alert_definition_condition_editor_option_operation());
        allMessages.put(AlertConditionCategory.RESOURCE_CONFIG.name(),
            MSG.view_alert_definition_condition_editor_option_resource_configuration());
        allMessages.put(AlertConditionCategory.EVENT.name(), MSG.view_alert_definition_condition_editor_option_event());
        allMessages.put(AlertConditionCategory.DRIFT.name(), MSG.view_alert_definition_condition_editor_option_drift());

        List<FormItem> formItems = new ArrayList<FormItem>();
        condTypes.put(AlertConditionCategory.AVAILABILITY.name(),
            allMessages.get(AlertConditionCategory.AVAILABILITY.name()));
        condTypes.put(AlertConditionCategory.AVAIL_DURATION.name(),
            allMessages.get(AlertConditionCategory.AVAIL_DURATION.name()));
        formItems.addAll(buildAvailabilityChangeFormItems(editMode
            && AlertConditionCategory.AVAILABILITY == existingCondition.getCategory()));
        formItems.addAll(buildAvailabilityDurationFormItems(editMode
            && AlertConditionCategory.AVAIL_DURATION == existingCondition.getCategory()));
        if (supportsMetrics) {
            condTypes.put(AlertConditionCategory.THRESHOLD.name(),
                allMessages.get(AlertConditionCategory.THRESHOLD.name()));
            condTypes.put(AlertConditionCategory.BASELINE.name(),
                allMessages.get(AlertConditionCategory.BASELINE.name()));
            condTypes.put(AlertConditionCategory.CHANGE.name(), allMessages.get(AlertConditionCategory.CHANGE.name()));
            condTypes.put(AlertConditionCategory.RANGE.name(), allMessages.get(AlertConditionCategory.RANGE.name()));
            formItems.addAll(buildMetricThresholdFormItems(editMode
                && AlertConditionCategory.THRESHOLD == existingCondition.getCategory()
                && existingCondition.getOption() == null));
            formItems.addAll(buildMetricBaselineFormItems(editMode
                && AlertConditionCategory.BASELINE == existingCondition.getCategory()));
            formItems.addAll(buildMetricChangeFormItems(editMode
                && AlertConditionCategory.CHANGE == existingCondition.getCategory()
                && existingCondition.getOption() == null));
            formItems.addAll(buildMetricRangeFormItems(editMode
                && AlertConditionCategory.RANGE == existingCondition.getCategory()));
        }
        if (supportsCalltimeMetrics) {
            condTypes.put(ALERT_CONDITION_CATEGORY_CALLTIME_THRESHOLD,
                allMessages.get(ALERT_CONDITION_CATEGORY_CALLTIME_THRESHOLD));
            condTypes.put(ALERT_CONDITION_CATEGORY_CALLTIME_CHANGE,
                allMessages.get(ALERT_CONDITION_CATEGORY_CALLTIME_CHANGE));
            formItems.addAll(buildCalltimeThresholdFormItems(editMode
                && AlertConditionCategory.THRESHOLD == existingCondition.getCategory()
                && existingCondition.getOption() != null));
            formItems.addAll(buildCalltimeChangeFormItems(editMode
                && AlertConditionCategory.CHANGE == existingCondition.getCategory()
                && existingCondition.getOption() != null));
        }
        if (supportsTraits) {
            condTypes.put(AlertConditionCategory.TRAIT.name(), allMessages.get(AlertConditionCategory.TRAIT.name()));
            formItems.addAll(buildTraitChangeFormItems(editMode
                && AlertConditionCategory.TRAIT == existingCondition.getCategory()));
        }
        if (supportsOperations) {
            condTypes
                .put(AlertConditionCategory.CONTROL.name(), allMessages.get(AlertConditionCategory.CONTROL.name()));
            formItems.addAll(buildOperationFormItems(editMode
                && AlertConditionCategory.CONTROL == existingCondition.getCategory()));
        }
        if (supportsResourceConfig) {
            condTypes.put(AlertConditionCategory.RESOURCE_CONFIG.name(),
                allMessages.get(AlertConditionCategory.RESOURCE_CONFIG.name()));
            formItems.addAll(buildResourceConfigChangeFormItems(editMode
                && AlertConditionCategory.RESOURCE_CONFIG == existingCondition.getCategory()));
        }
        if (supportsEvents) {
            condTypes.put(AlertConditionCategory.EVENT.name(), allMessages.get(AlertConditionCategory.EVENT.name()));
            formItems.addAll(buildEventFormItems(editMode
                && AlertConditionCategory.EVENT == existingCondition.getCategory()));
        }
        if (supportsDrift) {
            condTypes.put(AlertConditionCategory.DRIFT.name(), allMessages.get(AlertConditionCategory.DRIFT.name()));
            formItems.addAll(buildDriftFormItems(editMode
                && AlertConditionCategory.DRIFT == existingCondition.getCategory()));
        }
        conditionTypeSelectItem.setValueMap(condTypes);

        conditionTypeSelectItem.setWrapTitle(false);
        conditionTypeSelectItem.setRedrawOnChange(true);
        conditionTypeSelectItem.setWidth("*");
        if (editMode) {
            conditionTypeSelectItem.setDefaultValue(existingCondition.getCategory().name());
        } else {
            conditionTypeSelectItem.setDefaultValue(AlertConditionCategory.AVAILABILITY.name());
        }

        SpacerItem spacer1 = new SpacerItem();
        spacer1.setColSpan(2);
        spacer1.setHeight(5);

        SpacerItem spacer2 = new SpacerItem();
        spacer2.setColSpan(2);
        spacer2.setHeight(5);

        formItems.add(0, spacer1);
        formItems.add(0, conditionTypeSelectItem);
        formItems.add(spacer2);

        form = new DynamicForm();
        form.setItems(formItems.toArray(new FormItem[formItems.size()]));
    }

    private ToolStrip buildToolStrip() {
        IButton ok = new EnhancedIButton(MSG.common_button_ok(), ButtonColor.BLUE);
        ok.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                if (form.validate(false)) {
                    if (saveCondition()) {
                        closeFunction.run();
                    }
                }
            }
        });

        IButton cancel = new EnhancedIButton(MSG.common_button_cancel());
        cancel.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                closeFunction.run();
            }
        });

        ToolStrip footer = new ToolStrip();
        footer.setPadding(5);
        footer.setWidth100();
        footer.setMembersMargin(15);
        footer.addSpacer(60);
        footer.addMember(ok);
        footer.addMember(cancel);
        footer.addSpacer(60);
        footer.addFill();
        return footer;
    }

    private boolean saveCondition() {

        try {
            // Find out if this is using the ALL conjunction - if it is, we can't have more than one conditional use the same metric.
            // If we do, immediately abort and warn the user. See BZ 737565
            if ((BooleanExpression.ALL.name().equals(this.conditionExpression.getValue().toString()))
                && (supportsMetrics && this.resourceType.getMetricDefinitions() != null)) {

                Map<Integer, String> metricIdsUsed = new HashMap<Integer, String>();
                for (AlertCondition condition : this.conditions) {
                    if (condition.getMeasurementDefinition() != null) {
                        Integer id = Integer.valueOf(condition.getMeasurementDefinition().getId());
                        if (metricIdsUsed.containsKey(id)) {
                            String msg = MSG.view_alert_definition_condition_editor_metricswarning(metricIdsUsed
                                .get(id));
                            Message warning = new Message(msg, Severity.Warning, EnumSet.of(Message.Option.Transient));
                            CoreGUI.getMessageCenter().notify(warning);
                            return false; // multiple conditions used the same metric with ALL conjunction, this doesn't work - abort (BZ 737565)
                        }
                        metricIdsUsed.put(id, condition.getMeasurementDefinition().getDisplayName());
                    }
                }
            }

            final boolean calltimeCategory;
            final AlertConditionCategory category;

            String selectedCategory = conditionTypeSelectItem.getValue().toString();
            if (selectedCategory.equals(ALERT_CONDITION_CATEGORY_CALLTIME_THRESHOLD)) {
                calltimeCategory = true;
                category = AlertConditionCategory.THRESHOLD;
            } else if (selectedCategory.equals(ALERT_CONDITION_CATEGORY_CALLTIME_CHANGE)) {
                calltimeCategory = true;
                category = AlertConditionCategory.CHANGE;
            } else {
                calltimeCategory = false;
                category = AlertConditionCategory.valueOf(selectedCategory);
            }

            AlertCondition newCondition = new AlertCondition();
            newCondition.setCategory(category);

            switch (category) {
            case AVAILABILITY: {
                newCondition.setName(form.getValueAsString(AVAILABILITY_ITEMNAME));
                newCondition.setComparator(null);
                newCondition.setThreshold(null);
                newCondition.setOption(null);
                newCondition.setMeasurementDefinition(null);
                break;
            }

            case AVAIL_DURATION: {
                newCondition.setName(form.getValueAsString(AVAILABILITY_DURATION_ITEMNAME));
                newCondition.setComparator(null);
                newCondition.setThreshold(null);
                // entered in minutes, converted to seconds by DurationItem, and stored in seconds
                int duration = Integer.valueOf(form.getValueAsString(AVAILABILITY_DURATION_VAL_ITEMNAME));
                newCondition.setOption(String.valueOf(duration));
                newCondition.setMeasurementDefinition(null);
                break;
            }

            case THRESHOLD: {
                if (!calltimeCategory) {
                    MeasurementDefinition measDef = getMeasurementDefinition(form
                        .getValueAsString(THRESHOLD_METRIC_ITEMNAME));
                    newCondition.setName(measDef.getDisplayName());
                    newCondition.setThreshold(getMeasurementValue(measDef,
                        form.getValueAsString(THRESHOLD_ABSVALUE_ITEMNAME)));
                    newCondition.setComparator(form.getValueAsString(THRESHOLD_COMPARATOR_ITEMNAME));
                    newCondition.setOption(null);
                    newCondition.setMeasurementDefinition(measDef);
                } else {
                    MeasurementDefinition measDef = getMeasurementDefinition(form
                        .getValueAsString(CALLTIME_THRESHOLD_METRIC_ITEMNAME));
                    newCondition.setName(form.getValueAsString(CALLTIME_THRESHOLD_REGEX_ITEMNAME));
                    newCondition.setThreshold(getMeasurementValue(measDef,
                        form.getValueAsString(CALLTIME_THRESHOLD_ABSVALUE_ITEMNAME)));
                    newCondition.setComparator(form.getValueAsString(CALLTIME_THRESHOLD_COMPARATOR_ITEMNAME));
                    newCondition.setOption(form.getValueAsString(CALLTIME_THRESHOLD_MINMAXAVG_ITEMNAME));
                    newCondition.setMeasurementDefinition(measDef);
                }
                break;
            }

            case BASELINE: {
                MeasurementDefinition measDef = getMeasurementDefinition(form
                    .getValueAsString(BASELINE_METRIC_ITEMNAME));
                newCondition.setName(measDef.getDisplayName());
                newCondition.setThreshold(getMeasurementValueByUnits(MeasurementUnits.PERCENTAGE,
                    form.getValueAsString(BASELINE_PERCENTAGE_ITEMNAME)));
                newCondition.setComparator(form.getValueAsString(BASELINE_COMPARATOR_ITEMNAME));
                newCondition.setOption(form.getValueAsString(BASELINE_SELECTION_ITEMNAME));
                newCondition.setMeasurementDefinition(measDef);
                break;
            }

            case CHANGE: {
                if (!calltimeCategory) {
                    MeasurementDefinition measDef = getMeasurementDefinition(form
                        .getValueAsString(CHANGE_METRIC_ITEMNAME));
                    newCondition.setName(measDef.getDisplayName());
                    newCondition.setComparator(null);
                    newCondition.setThreshold(null);
                    newCondition.setOption(null);
                    newCondition.setMeasurementDefinition(measDef);
                } else {
                    MeasurementDefinition measDef = getMeasurementDefinition(form
                        .getValueAsString(CALLTIME_CHANGE_METRIC_ITEMNAME));
                    newCondition.setName(form.getValueAsString(CALLTIME_CHANGE_REGEX_ITEMNAME));
                    newCondition.setThreshold(getMeasurementValueByUnits(MeasurementUnits.PERCENTAGE,
                        form.getValueAsString(CALLTIME_CHANGE_PERCENTAGE_ITEMNAME)));
                    newCondition.setComparator(form.getValueAsString(CALLTIME_CHANGE_COMPARATOR_ITEMNAME));
                    newCondition.setOption(form.getValueAsString(CALLTIME_CHANGE_MINMAXAVG_ITEMNAME));
                    newCondition.setMeasurementDefinition(measDef);
                }
                break;
            }

            case TRAIT: {
                MeasurementDefinition measDef = getMeasurementDefinition(form.getValueAsString(TRAIT_METRIC_ITEMNAME));
                newCondition.setName(measDef.getDisplayName());
                newCondition.setComparator(null);
                newCondition.setThreshold(null);
                newCondition.setOption(form.getValueAsString(TRAIT_REGEX_ITEMNAME));
                newCondition.setMeasurementDefinition(measDef);
                break;
            }

            case CONTROL: {
                newCondition.setName(form.getValueAsString(OPERATION_NAME_ITEMNAME));
                newCondition.setComparator(null);
                newCondition.setThreshold(null);
                newCondition.setOption(form.getValueAsString(OPERATION_RESULTS_ITEMNAME));
                newCondition.setMeasurementDefinition(null);
                break;
            }

            case EVENT: {
                newCondition.setName(form.getValueAsString(EVENT_SEVERITY_ITEMNAME));
                newCondition.setComparator(null);
                newCondition.setThreshold(null);
                Object regex1 = form.getValue(EVENT_REGEX_ITEMNAME);
                Object regex2 = form.getValue(EVENT_SOURCE_PATH_REGEX_ITEMNAME);
                newCondition.setOption((regex1 == null ? "" : regex1) + AlertCondition.ADHOC_SEPARATOR
                    + (regex2 == null ? "" : regex2));
                newCondition.setMeasurementDefinition(null);
                break;
            }

            case RESOURCE_CONFIG: {
                newCondition.setName(null);
                newCondition.setComparator(null);
                newCondition.setThreshold(null);
                newCondition.setOption(null);
                newCondition.setMeasurementDefinition(null);
                break;
            }

            case DRIFT: {
                newCondition.setName(form.getValueAsString(DRIFT_DEFNAME_REGEX_ITEMNAME));
                newCondition.setComparator(null);
                newCondition.setThreshold(null);
                newCondition.setOption(form.getValueAsString(DRIFT_PATHNAME_REGEX_ITEMNAME));
                newCondition.setMeasurementDefinition(null);
                break;
            }

            case RANGE: {
                MeasurementDefinition measDef = getMeasurementDefinition(form.getValueAsString(RANGE_METRIC_ITEMNAME));
                newCondition.setName(measDef.getDisplayName());
                newCondition.setThreshold(getMeasurementValue(measDef,
                    form.getValueAsString(RANGE_LO_ABSVALUE_ITEMNAME)));
                newCondition.setComparator(form.getValueAsString(RANGE_COMPARATOR_ITEMNAME));
                newCondition.setOption(getMeasurementValue(measDef, form.getValueAsString(RANGE_HI_ABSVALUE_ITEMNAME))
                    .toString());
                newCondition.setMeasurementDefinition(measDef);
                break;
            }

            default: {
                CoreGUI.getErrorHandler().handleError(
                    MSG.view_alert_common_tab_invalid_condition_category(category.name())); // should never happen
                break;
            }
            }
            if (editMode) {
                if (existingCondition.getId() != 0) {
                    // get rid of the id, because of the equals method
                    AlertCondition conditionWithoutId = new AlertCondition(existingCondition);
                    if (!conditionWithoutId.equals(newCondition)) {
                        // there was a change
                        this.modifiedConditions.put(existingCondition.getId(), newCondition);
                        existingCondition.setMeasurementDefinition(newCondition.getMeasurementDefinition());
                        existingCondition.setName(newCondition.getName());
                        existingCondition.setComparator(newCondition.getComparator());
                        existingCondition.setThreshold(newCondition.getThreshold());
                        existingCondition.setOption(newCondition.getOption());
                        existingCondition.setTriggerId(newCondition.getTriggerId());
                        existingCondition.setCategory(newCondition.getCategory());
                    }
                } else {
                    this.conditions.remove(existingCondition);
                    this.conditions.add(newCondition);
                }
            } else {
                this.conditions.add(newCondition);
            }

            return true;
        } catch (Exception e) {
            CoreGUI.getErrorHandler().handleError("Problem creating condition", e);
            return false;
        }
    }

    private Double getMeasurementValue(MeasurementDefinition measDef, String userEnteredValue) {
        return getMeasurementValueByUnits(measDef.getUnits(), userEnteredValue);
    }

    private Double getMeasurementValueByUnits(MeasurementUnits units, String userEnteredValue) {
        return MeasurementParser.parse(userEnteredValue, units).getValue();
    }

    private ArrayList<FormItem> buildMetricThresholdFormItems(boolean editMode) {
        ArrayList<FormItem> formItems = new ArrayList<FormItem>();

        ShowIfCategoryFunction ifFunc = new ShowIfCategoryFunction(AlertConditionCategory.THRESHOLD);

        String helpStr = MSG.view_alert_definition_condition_editor_metric_threshold_tooltip();
        StaticTextItem helpItem = buildHelpTextItem("thresholdHelp", helpStr, ifFunc);
        formItems.add(helpItem);

        SelectItem metricDropDownMenu = buildMetricDropDownMenu(THRESHOLD_METRIC_ITEMNAME, false, ifFunc, editMode);
        if (metricDropDownMenu != null) {
            formItems.add(metricDropDownMenu);
            formItems.add(buildComparatorDropDownMenu(THRESHOLD_COMPARATOR_ITEMNAME, ifFunc, editMode));
            TextItem absoluteValue = new TextItem(THRESHOLD_ABSVALUE_ITEMNAME,
                MSG.view_alert_definition_condition_editor_metric_threshold_value());
            absoluteValue.setWrapTitle(false);
            absoluteValue.setRequired(true);
            absoluteValue.setTooltip(MSG.view_alert_definition_condition_editor_metric_threshold_value_tooltip());
            absoluteValue.setHoverWidth(200);
            absoluteValue.setValidateOnChange(true);
            absoluteValue.setValidators(new NumberWithUnitsValidator(this.resourceType.getMetricDefinitions(),
                metricDropDownMenu));
            if (editMode) {
                MeasurementUnits units = existingCondition.getMeasurementDefinition().getUnits();
                double doubleValue = existingCondition.getThreshold();
                MeasurementNumericValueAndUnits valueWithUnits = null;
                if (units.getFamily() == MeasurementUnits.Family.RELATIVE) {
                    valueWithUnits = new MeasurementNumericValueAndUnits(doubleValue * 100, MeasurementUnits.PERCENTAGE);
                } else {
                    valueWithUnits = MeasurementConverterClient.fit(doubleValue, units);
                }
                absoluteValue.setDefaultValue(valueWithUnits.toString());
            }
            absoluteValue.setShowIfCondition(ifFunc);
            formItems.add(absoluteValue);
            formItems.add(buildBaseUnitsItem(metricDropDownMenu, ifFunc, editMode));
        } else {
            String noMetricsStr = MSG.view_alert_definition_condition_editor_metric_nometrics();
            StaticTextItem noMetrics = buildHelpTextItem(THRESHOLD_NO_METRICS_ITEMNAME, noMetricsStr, ifFunc);
            formItems.add(noMetrics);
        }

        return formItems;
    }

    private ArrayList<FormItem> buildMetricRangeFormItems(boolean editMode) {
        ArrayList<FormItem> formItems = new ArrayList<FormItem>();

        ShowIfCategoryFunction ifFunc = new ShowIfCategoryFunction(AlertConditionCategory.RANGE);

        String helpStr = MSG.view_alert_definition_condition_editor_metric_range_tooltip();
        StaticTextItem helpItem = buildHelpTextItem("rangeHelp", helpStr, ifFunc);
        formItems.add(helpItem);

        SelectItem metricDropDownMenu = buildMetricDropDownMenu(RANGE_METRIC_ITEMNAME, false, ifFunc, editMode);
        if (metricDropDownMenu != null) {
            formItems.add(metricDropDownMenu);
            formItems.add(buildRangeComparatorDropDownMenu(RANGE_COMPARATOR_ITEMNAME, ifFunc, editMode));
            TextItem absoluteLowValue = new TextItem(RANGE_LO_ABSVALUE_ITEMNAME,
                MSG.view_alert_definition_condition_editor_metric_range_lovalue());
            absoluteLowValue.setWrapTitle(false);
            absoluteLowValue.setRequired(true);
            absoluteLowValue.setTooltip(MSG.view_alert_definition_condition_editor_metric_range_lovalue_tooltip());
            absoluteLowValue.setHoverWidth(200);
            absoluteLowValue.setValidateOnChange(true);
            absoluteLowValue.setValidators(new NumberWithUnitsValidator(this.resourceType.getMetricDefinitions(),
                metricDropDownMenu));
            absoluteLowValue.setShowIfCondition(ifFunc);

            TextItem absoluteHighValue = new TextItem(RANGE_HI_ABSVALUE_ITEMNAME,
                MSG.view_alert_definition_condition_editor_metric_range_hivalue());
            absoluteHighValue.setWrapTitle(false);
            absoluteHighValue.setRequired(true);
            absoluteHighValue.setTooltip(MSG.view_alert_definition_condition_editor_metric_range_hivalue_tooltip());
            absoluteHighValue.setHoverWidth(200);
            absoluteHighValue.setValidateOnChange(true);
            absoluteHighValue.setValidators(new NumberWithUnitsValidator(this.resourceType.getMetricDefinitions(),
                metricDropDownMenu));
            absoluteHighValue.setShowIfCondition(ifFunc);
            if (editMode) {
                absoluteLowValue.setDefaultValue(String.valueOf(existingCondition.getThreshold()));
                absoluteHighValue.setDefaultValue(String.valueOf(existingCondition.getOption()));
            }

            formItems.add(absoluteLowValue);
            formItems.add(absoluteHighValue);
            formItems.add(buildBaseUnitsItem(metricDropDownMenu, ifFunc, editMode));
        } else {
            String noMetricsStr = MSG.view_alert_definition_condition_editor_metric_nometrics();
            StaticTextItem noMetrics = buildHelpTextItem(RANGE_NO_METRICS_ITEMNAME, noMetricsStr, ifFunc);
            formItems.add(noMetrics);
        }

        return formItems;
    }

    private ArrayList<FormItem> buildMetricBaselineFormItems(boolean editMode) {
        ArrayList<FormItem> formItems = new ArrayList<FormItem>();

        ShowIfCategoryFunction ifFunc = new ShowIfCategoryFunction(AlertConditionCategory.BASELINE);

        String helpStr = MSG.view_alert_definition_condition_editor_metric_baseline_tooltip();
        StaticTextItem helpItem = buildHelpTextItem("baselineHelp", helpStr, ifFunc);
        formItems.add(helpItem);

        // if a metric is trending (up or down), it will never have baselines calculated for it so only show dynamic metrics
        SelectItem metricDropDownMenu = buildMetricDropDownMenu(BASELINE_METRIC_ITEMNAME, true, ifFunc, editMode);
        if (metricDropDownMenu != null) {
            formItems.add(metricDropDownMenu);
            formItems.add(buildComparatorDropDownMenu(BASELINE_COMPARATOR_ITEMNAME, ifFunc, editMode));

            TextItem baselinePercentage = new TextItem(BASELINE_PERCENTAGE_ITEMNAME,
                MSG.view_alert_definition_condition_editor_metric_baseline_percentage());
            baselinePercentage.setWrapTitle(false);
            baselinePercentage.setRequired(true);
            baselinePercentage.setTooltip(MSG
                .view_alert_definition_condition_editor_metric_baseline_percentage_tooltip());
            baselinePercentage.setHoverWidth(200);
            baselinePercentage.setShowIfCondition(ifFunc);
            baselinePercentage.setValidateOnChange(true);
            baselinePercentage.setValidators(new NumberWithUnitsValidator(MeasurementUnits.PERCENTAGE));
            if (editMode) {
                baselinePercentage.setDefaultValue(String.valueOf((int) (existingCondition.getThreshold() * 100)));
            }
            formItems.add(baselinePercentage);

            SelectItem baselineSelection = new SelectItem(BASELINE_SELECTION_ITEMNAME,
                MSG.view_alert_definition_condition_editor_metric_baseline_value());
            LinkedHashMap<String, String> baselines = new LinkedHashMap<String, String>(3);
            baselines.put("min", MSG.common_title_monitor_minimum()); // TODO can we have the current value of the min baseline
            baselines.put("mean", MSG.common_title_monitor_average()); // TODO can we have the current value of the avg baseline
            baselines.put("max", MSG.common_title_monitor_maximum()); // TODO can we have the current value of the max baseline
            baselineSelection.setValueMap(baselines);
            baselineSelection.setDefaultValue(editMode ? existingCondition.getOption() : "mean");
            baselineSelection.setWrapTitle(false);
            baselineSelection.setWidth("*");
            baselineSelection.setRedrawOnChange(true);
            baselineSelection.setShowIfCondition(ifFunc);
            formItems.add(baselineSelection);
        } else {
            String noMetricsStr = MSG.view_alert_definition_condition_editor_metric_nometrics();
            StaticTextItem noMetrics = buildHelpTextItem(BASELINE_NO_METRICS_ITEMNAME, noMetricsStr, ifFunc);
            formItems.add(noMetrics);
        }

        return formItems;
    }

    private ArrayList<FormItem> buildMetricChangeFormItems(boolean editMode) {
        ArrayList<FormItem> formItems = new ArrayList<FormItem>();

        ShowIfCategoryFunction ifFunc = new ShowIfCategoryFunction(AlertConditionCategory.CHANGE);

        String helpStr = MSG.view_alert_definition_condition_editor_metric_change_tooltip();
        StaticTextItem helpItem = buildHelpTextItem("changeMetricHelp", helpStr, ifFunc);
        formItems.add(helpItem);

        SelectItem metricDropDownMenu = buildMetricDropDownMenu(CHANGE_METRIC_ITEMNAME, false, ifFunc, editMode);
        if (metricDropDownMenu != null) {
            formItems.add(metricDropDownMenu);
        } else {
            String noMetricsStr = MSG.view_alert_definition_condition_editor_metric_nometrics();
            StaticTextItem noMetrics = buildHelpTextItem(CHANGE_NO_METRICS_ITEMNAME, noMetricsStr, ifFunc);
            formItems.add(noMetrics);
        }

        return formItems;
    }

    private ArrayList<FormItem> buildCalltimeThresholdFormItems(boolean editMode) {
        ArrayList<FormItem> formItems = new ArrayList<FormItem>();

        ShowIfCategoryFunction ifFunc = new ShowIfCategoryFunction(ALERT_CONDITION_CATEGORY_CALLTIME_THRESHOLD);

        String helpStr = MSG.view_alert_definition_condition_editor_metric_calltime_threshold_tooltip();
        StaticTextItem helpItem = buildHelpTextItem("calltimeThresholdHelp", helpStr, ifFunc);
        formItems.add(helpItem);

        SelectItem metricDropDownMenu = buildCalltimeMetricDropDownMenu(CALLTIME_THRESHOLD_METRIC_ITEMNAME, ifFunc,
            editMode);
        formItems.add(metricDropDownMenu);

        SelectItem minMaxAvgSelection = new SelectItem(CALLTIME_THRESHOLD_MINMAXAVG_ITEMNAME,
            MSG.view_alert_definition_condition_editor_metric_calltime_common_limit());
        LinkedHashMap<String, String> limits = new LinkedHashMap<String, String>(3);
        limits.put("MIN", MSG.common_title_monitor_minimum());
        limits.put("AVG", MSG.common_title_monitor_average());
        limits.put("MAX", MSG.common_title_monitor_maximum());
        minMaxAvgSelection
            .setTooltip(MSG.view_alert_definition_condition_editor_metric_calltime_common_limit_tooltip());
        minMaxAvgSelection.setHoverWidth(200);
        minMaxAvgSelection.setValueMap(limits);
        minMaxAvgSelection.setDefaultValue(editMode ? existingCondition.getOption() : "AVG");
        minMaxAvgSelection.setWrapTitle(false);
        minMaxAvgSelection.setWidth("*");
        minMaxAvgSelection.setRedrawOnChange(true);
        minMaxAvgSelection.setShowIfCondition(ifFunc);
        formItems.add(minMaxAvgSelection);

        formItems.add(buildComparatorDropDownMenu(CALLTIME_THRESHOLD_COMPARATOR_ITEMNAME, ifFunc, editMode));
        TextItem absoluteValue = new TextItem(CALLTIME_THRESHOLD_ABSVALUE_ITEMNAME,
            MSG.view_alert_definition_condition_editor_metric_calltime_threshold_value());
        absoluteValue.setWrapTitle(false);
        absoluteValue.setRequired(true);
        absoluteValue.setTooltip(MSG.view_alert_definition_condition_editor_metric_calltime_threshold_value_tooltip());
        absoluteValue.setHoverWidth(200);
        absoluteValue.setShowIfCondition(ifFunc);
        absoluteValue.setValidateOnChange(true);
        absoluteValue.setValidators(new NumberWithUnitsValidator(this.resourceType.getMetricDefinitions(),
            metricDropDownMenu));

        TextItem regex = new TextItem(CALLTIME_THRESHOLD_REGEX_ITEMNAME,
            MSG.view_alert_definition_condition_editor_common_regex());
        regex.setRequired(false);
        regex.setTooltip(MSG.view_alert_definition_condition_editor_metric_calltime_regexTooltip());
        regex.setHoverWidth(200);
        regex.setWrapTitle(false);
        regex.setShowIfCondition(ifFunc);
        if (editMode) {
            absoluteValue.setDefaultValue(String.valueOf(existingCondition.getThreshold()));
            regex.setDefaultValue(existingCondition.getName());
        }

        formItems.add(absoluteValue);
        formItems.add(regex);
        return formItems;
    }

    private ArrayList<FormItem> buildCalltimeChangeFormItems(boolean editMode) {
        ArrayList<FormItem> formItems = new ArrayList<FormItem>();

        ShowIfCategoryFunction ifFunc = new ShowIfCategoryFunction(ALERT_CONDITION_CATEGORY_CALLTIME_CHANGE);

        String helpStr = MSG.view_alert_definition_condition_editor_metric_calltime_change_tooltip();
        StaticTextItem helpItem = buildHelpTextItem("calltimeChangeHelp", helpStr, ifFunc);
        formItems.add(helpItem);

        formItems.add(buildCalltimeMetricDropDownMenu(CALLTIME_CHANGE_METRIC_ITEMNAME, ifFunc, editMode));

        SelectItem minMaxAvgSelection = new SelectItem(CALLTIME_CHANGE_MINMAXAVG_ITEMNAME,
            MSG.view_alert_definition_condition_editor_metric_calltime_common_limit());
        LinkedHashMap<String, String> limits = new LinkedHashMap<String, String>(3);
        limits.put("MIN", MSG.common_title_monitor_minimum());
        limits.put("AVG", MSG.common_title_monitor_average());
        limits.put("MAX", MSG.common_title_monitor_maximum());
        minMaxAvgSelection
            .setTooltip(MSG.view_alert_definition_condition_editor_metric_calltime_common_limit_tooltip());
        minMaxAvgSelection.setHoverWidth(200);
        minMaxAvgSelection.setValueMap(limits);
        minMaxAvgSelection.setDefaultValue(editMode ? existingCondition.getOption() : "AVG");
        minMaxAvgSelection.setWrapTitle(false);
        minMaxAvgSelection.setWidth("*");
        minMaxAvgSelection.setRedrawOnChange(true);
        minMaxAvgSelection.setShowIfCondition(ifFunc);
        formItems.add(minMaxAvgSelection);

        formItems.add(buildCalltimeComparatorDropDownMenu(CALLTIME_CHANGE_COMPARATOR_ITEMNAME, ifFunc, editMode));

        TextItem percentage = new TextItem(CALLTIME_CHANGE_PERCENTAGE_ITEMNAME,
            MSG.view_alert_definition_condition_editor_metric_calltime_change_percentage());
        percentage.setWrapTitle(false);
        percentage.setRequired(true);
        percentage.setTooltip(MSG.view_alert_definition_condition_editor_metric_calltime_change_percentage_tooltip());
        percentage.setHoverWidth(200);
        percentage.setShowIfCondition(ifFunc);
        percentage.setValidateOnChange(true);
        percentage.setValidators(new NumberWithUnitsValidator(MeasurementUnits.PERCENTAGE));

        TextItem regex = new TextItem(CALLTIME_CHANGE_REGEX_ITEMNAME,
            MSG.view_alert_definition_condition_editor_common_regex());
        regex.setRequired(false);
        regex.setTooltip(MSG.view_alert_definition_condition_editor_metric_calltime_regexTooltip());
        regex.setHoverWidth(200);
        regex.setWrapTitle(false);
        regex.setShowIfCondition(ifFunc);
        if (editMode) {
            percentage.setDefaultValue(String.valueOf((int) (existingCondition.getThreshold() * 100)));
            regex.setDefaultValue(existingCondition.getName());
        }

        formItems.add(percentage);
        formItems.add(regex);
        return formItems;
    }

    private ArrayList<FormItem> buildTraitChangeFormItems(boolean editMode) {
        ArrayList<FormItem> formItems = new ArrayList<FormItem>();

        ShowIfCategoryFunction ifFunc = new ShowIfCategoryFunction(AlertConditionCategory.TRAIT);

        String helpStr = MSG.view_alert_definition_condition_editor_metric_trait_change_tooltip();
        StaticTextItem helpItem = buildHelpTextItem("traitHelp", helpStr, ifFunc);
        formItems.add(helpItem);

        LinkedHashMap<String, String> traitsMap = new LinkedHashMap<String, String>();
        for (MeasurementDefinition def : this.resourceType.getMetricDefinitions()) {
            if (def.getDataType() == DataType.TRAIT) {
                traitsMap.put(String.valueOf(def.getId()), def.getDisplayName());
            }
        }

        SelectItem traitSelection = new SortedSelectItem(TRAIT_METRIC_ITEMNAME,
            MSG.view_alert_definition_condition_editor_metric_trait_change_value());
        traitSelection.setValueMap(traitsMap);
        if (editMode) {
            traitSelection.setDefaultValue(String.valueOf(existingCondition.getMeasurementDefinition().getId()));
        } else {
            traitSelection.setDefaultToFirstOption(true);
        }
        traitSelection.setWidth("*");
        traitSelection.setRedrawOnChange(true);
        traitSelection.setShowIfCondition(ifFunc);
        formItems.add(traitSelection);

        TextItem eventRegex = new TextItem(TRAIT_REGEX_ITEMNAME,
            MSG.view_alert_definition_condition_editor_common_regex());
        eventRegex.setRequired(false);
        eventRegex.setTooltip(MSG.view_alert_definition_condition_editor_metric_trait_regexTooltip());
        eventRegex.setHoverWidth(200);
        eventRegex.setWrapTitle(false);
        eventRegex.setShowIfCondition(ifFunc);
        if (editMode) {
            eventRegex.setDefaultValue(existingCondition.getOption());
        }
        formItems.add(eventRegex);

        return formItems;
    }

    private ArrayList<FormItem> buildAvailabilityChangeFormItems(boolean editMode) {
        ArrayList<FormItem> formItems = new ArrayList<FormItem>();

        ShowIfCategoryFunction ifFunc = new ShowIfCategoryFunction(AlertConditionCategory.AVAILABILITY);

        String helpStr = MSG.view_alert_definition_condition_editor_availability_tooltip();
        StaticTextItem helpItem = buildHelpTextItem("availabilityHelp", helpStr, ifFunc);
        formItems.add(helpItem);

        SelectItem selection = new SortedSelectItem(AVAILABILITY_ITEMNAME, MSG.common_title_availability());
        LinkedHashMap<String, String> avails = new LinkedHashMap<String, String>(2);
        avails.put(AlertConditionOperator.AVAIL_GOES_DOWN.name(),
            MSG.view_alert_definition_condition_editor_operator_availability_goesDown());
        // do not add 'Goes Disabled' and 'Goes Unknown' for platform
        if (resourceType.getCategory() != ResourceCategory.PLATFORM) {
            avails.put(AlertConditionOperator.AVAIL_GOES_DISABLED.name(),
                MSG.view_alert_definition_condition_editor_operator_availability_goesDisabled());
            avails.put(AlertConditionOperator.AVAIL_GOES_UNKNOWN.name(),
                MSG.view_alert_definition_condition_editor_operator_availability_goesUnknown());
        }
        avails.put(AlertConditionOperator.AVAIL_GOES_NOT_UP.name(),
            MSG.view_alert_definition_condition_editor_operator_availability_goesNotUp());
        avails.put(AlertConditionOperator.AVAIL_GOES_UP.name(),
            MSG.view_alert_definition_condition_editor_operator_availability_goesUp());
        selection.setValueMap(avails);
        String defaultValue = AlertConditionOperator.AVAIL_GOES_DOWN.name();
        selection.setDefaultValue(editMode ? existingCondition.getName() : defaultValue);
        selection.setShowIfCondition(ifFunc);

        formItems.add(selection);
        return formItems;
    }

    private ArrayList<FormItem> buildAvailabilityDurationFormItems(boolean editMode) {
        ArrayList<FormItem> formItems = new ArrayList<FormItem>();

        ShowIfCategoryFunction ifFunc = new ShowIfCategoryFunction(AlertConditionCategory.AVAIL_DURATION);

        String helpStr = MSG.view_alert_definition_condition_editor_availabilityDuration_tooltip();
        StaticTextItem helpItem = buildHelpTextItem("availabilityDurationHelp", helpStr, ifFunc);
        formItems.add(helpItem);

        SelectItem selection = new SortedSelectItem(AVAILABILITY_DURATION_ITEMNAME,
            MSG.view_alert_definition_condition_editor_availabilityDuration_state());
        LinkedHashMap<String, String> avails = new LinkedHashMap<String, String>(2);
        avails.put(AlertConditionOperator.AVAIL_DURATION_DOWN.name(),
            MSG.view_alert_definition_condition_editor_operator_availability_durationDown());
        avails.put(AlertConditionOperator.AVAIL_DURATION_NOT_UP.name(),
            MSG.view_alert_definition_condition_editor_operator_availability_durationNotUp());
        selection.setValueMap(avails);
        selection.setDefaultValue(editMode ? existingCondition.getName() : AlertConditionOperator.AVAIL_DURATION_DOWN
            .name());
        selection.setShowIfCondition(ifFunc);
        formItems.add(selection);

        TreeSet<TimeUnit> supportedTimeUnits = new TreeSet<TimeUnit>();
        supportedTimeUnits.add(TimeUnit.MINUTES);
        supportedTimeUnits.add(TimeUnit.HOURS);
        DurationItem durationValue = new DurationItem(AVAILABILITY_DURATION_VAL_ITEMNAME, MSG.common_title_duration(),
            TimeUnit.SECONDS, supportedTimeUnits, false, false);
        durationValue.setWrapTitle(false);
        durationValue.setRequired(true);
        durationValue.setTooltip(MSG.view_alert_definition_condition_editor_availabilityDuration_tooltip_duration());
        durationValue.setHoverWidth(200);
        if (editMode) {
            durationValue.setAndFormatValue(Integer.parseInt(existingCondition.getOption()) * 1000L);
        }
        durationValue.setShowIfCondition(ifFunc);
        formItems.add(durationValue);

        return formItems;
    }

    private ArrayList<FormItem> buildOperationFormItems(boolean editMode) {
        ArrayList<FormItem> formItems = new ArrayList<FormItem>();

        ShowIfCategoryFunction ifFunc = new ShowIfCategoryFunction(AlertConditionCategory.CONTROL);

        String helpStr = MSG.view_alert_definition_condition_editor_operation_tooltip();
        StaticTextItem helpItem = buildHelpTextItem("operationHelp", helpStr, ifFunc);
        formItems.add(helpItem);

        LinkedHashMap<String, String> ops = new LinkedHashMap<String, String>();
        for (OperationDefinition opDef : this.resourceType.getOperationDefinitions()) {
            ops.put(opDef.getName(), opDef.getDisplayName());
        }

        SelectItem opSelection = new SortedSelectItem(OPERATION_NAME_ITEMNAME, MSG.common_title_value());
        opSelection.setValueMap(ops);
        if (editMode) {
            opSelection.setDefaultValue(existingCondition.getName());
        } else {
            opSelection.setDefaultToFirstOption(true);
        }

        opSelection.setWidth("*");
        opSelection.setRedrawOnChange(true);
        opSelection.setShowIfCondition(ifFunc);
        formItems.add(opSelection);

        SelectItem opResultsSelection = new SortedSelectItem(OPERATION_RESULTS_ITEMNAME, MSG.common_title_status());
        LinkedHashMap<String, String> operationStatuses = new LinkedHashMap<String, String>(4);
        operationStatuses.put(OperationRequestStatus.INPROGRESS.name(), MSG.common_status_inprogress());
        operationStatuses.put(OperationRequestStatus.SUCCESS.name(), MSG.common_status_success());
        operationStatuses.put(OperationRequestStatus.FAILURE.name(), MSG.common_status_failed());
        operationStatuses.put(OperationRequestStatus.CANCELED.name(), MSG.common_status_canceled());
        opResultsSelection.setValueMap(operationStatuses);
        opResultsSelection.setDefaultValue(editMode ? existingCondition.getOption() : OperationRequestStatus.FAILURE
            .name());
        opResultsSelection.setWrapTitle(false);
        opResultsSelection.setShowIfCondition(ifFunc);
        formItems.add(opResultsSelection);

        return formItems;
    }

    private ArrayList<FormItem> buildEventFormItems(boolean editMode) {
        ArrayList<FormItem> formItems = new ArrayList<FormItem>();

        ShowIfCategoryFunction ifFunc = new ShowIfCategoryFunction(AlertConditionCategory.EVENT);

        String helpStr = MSG.view_alert_definition_condition_editor_event_tooltip();
        StaticTextItem helpItem = buildHelpTextItem("eventHelp", helpStr, ifFunc);
        formItems.add(helpItem);

        SelectItem eventSeveritySelection = new SelectItem(EVENT_SEVERITY_ITEMNAME,
            MSG.view_alert_definition_condition_editor_event_severity());
        LinkedHashMap<String, String> severities = new LinkedHashMap<String, String>(5);
        severities.put(EventSeverity.DEBUG.name(), MSG.common_severity_debug());
        severities.put(EventSeverity.INFO.name(), MSG.common_severity_info());
        severities.put(EventSeverity.WARN.name(), MSG.common_severity_warn());
        severities.put(EventSeverity.ERROR.name(), MSG.common_severity_error());
        severities.put(EventSeverity.FATAL.name(), MSG.common_severity_fatal());
        eventSeveritySelection.setValueMap(severities);
        eventSeveritySelection.setDefaultValue(editMode ? existingCondition.getName() : EventSeverity.ERROR.name());
        eventSeveritySelection.setWrapTitle(false);
        eventSeveritySelection.setShowIfCondition(ifFunc);
        formItems.add(eventSeveritySelection);
        
        String eventRegexValue = "", eventSourcePathRegexValue = "";
        if (editMode) {
            if (existingCondition.getOption().contains(AlertCondition.ADHOC_SEPARATOR)) {
                String[] regexes = existingCondition.getOption().split(AlertCondition.ADHOC_SEPARATOR);
                if (regexes.length > 0) {
                    eventRegexValue = regexes[0];
                    if (regexes.length > 1) {
                        eventSourcePathRegexValue = regexes[1];
                    }
                }
            } else {
                eventRegexValue = existingCondition.getOption(); // old approach -> probably working with db before rhq 4.13
            }
        }

        TextItem eventDetailsRegex = new TextItem(EVENT_REGEX_ITEMNAME,
            MSG.view_alert_definition_condition_editor_common_regex());
        eventDetailsRegex.setRequired(false);
        eventDetailsRegex.setTooltip(MSG.view_alert_definition_condition_editor_event_regexTooltip());
        eventDetailsRegex.setHoverWidth(200);
        eventDetailsRegex.setWrapTitle(false);
        eventDetailsRegex.setShowIfCondition(ifFunc);
        if (editMode) {
            eventDetailsRegex.setDefaultValue(eventRegexValue);
        }
        formItems.add(eventDetailsRegex);
        
        TextItem eventSourcePathRegex = new TextItem(EVENT_SOURCE_PATH_REGEX_ITEMNAME,
            MSG.view_inventory_eventHistory_sourceLocation() + " "
                + MSG.view_alert_definition_condition_editor_common_regex());
        eventSourcePathRegex.setRequired(false);
        eventSourcePathRegex.setTooltip(MSG.view_alert_definition_condition_editor_common_regex_tooltip());
        eventSourcePathRegex.setHoverWidth(200);
        eventSourcePathRegex.setWrapTitle(false);
        eventSourcePathRegex.setShowIfCondition(ifFunc);
        if (editMode) {
            eventSourcePathRegex.setDefaultValue(eventSourcePathRegexValue);
        }
        formItems.add(eventSourcePathRegex);

        return formItems;
    }

    private ArrayList<FormItem> buildResourceConfigChangeFormItems(boolean editMode) {
        ArrayList<FormItem> formItems = new ArrayList<FormItem>();

        ShowIfCategoryFunction ifFunc = new ShowIfCategoryFunction(AlertConditionCategory.RESOURCE_CONFIG);

        String helpStr = MSG.view_alert_definition_condition_editor_resource_configuration_tooltip();
        StaticTextItem helpItem = buildHelpTextItem("changeConfigHelp", helpStr, ifFunc);
        formItems.add(helpItem);

        return formItems;
    }

    private ArrayList<FormItem> buildDriftFormItems(boolean editMode) {
        ArrayList<FormItem> formItems = new ArrayList<FormItem>();

        ShowIfCategoryFunction ifFunc = new ShowIfCategoryFunction(AlertConditionCategory.DRIFT);

        String helpStr = MSG.view_alert_definition_condition_editor_drift_tooltip();
        StaticTextItem helpItem = buildHelpTextItem("driftHelp", helpStr, ifFunc);
        formItems.add(helpItem);

        TextItem driftDefNameRegex = new TextItem(DRIFT_DEFNAME_REGEX_ITEMNAME,
            MSG.view_alert_definition_condition_editor_drift_configname_regex());
        driftDefNameRegex.setRequired(false);
        driftDefNameRegex.setTooltip(MSG.view_alert_definition_condition_editor_drift_configname_regex_tooltip());
        driftDefNameRegex.setHoverWidth(200);
        driftDefNameRegex.setWrapTitle(false);
        driftDefNameRegex.setShowIfCondition(ifFunc);

        TextItem driftPathNameRegex = new TextItem(DRIFT_PATHNAME_REGEX_ITEMNAME,
            MSG.view_alert_definition_condition_editor_drift_pathname_regex());
        driftPathNameRegex.setRequired(false);
        driftPathNameRegex.setTooltip(MSG.view_alert_definition_condition_editor_drift_pathname_regex_tooltip());
        driftPathNameRegex.setHoverWidth(200);
        driftPathNameRegex.setWrapTitle(false);
        driftPathNameRegex.setShowIfCondition(ifFunc);
        if (editMode) {
            driftDefNameRegex.setDefaultValue(existingCondition.getName());
            driftPathNameRegex.setDefaultValue(existingCondition.getOption());
        }

        formItems.add(driftDefNameRegex);
        formItems.add(driftPathNameRegex);
        return formItems;
    }

    private SelectItem buildMetricDropDownMenu(String itemName, boolean dynamicOnly, FormItemIfFunction ifFunc,
        boolean editMode) {

        // find out if this is the ALL - if it is, we can't have more than one conditional use the same metric (BZ 737565)
        Set<String> metricIdsToHide = new HashSet<String>();
        if (BooleanExpression.ALL.name().equals(this.conditionExpression.getValue().toString())) {
            for (AlertCondition condition : this.conditions) {
                if (editMode) { // do not hide the metric if it is part of the currently modified condition
                    AlertCondition conditionWithoutId = new AlertCondition(existingCondition); // newly created conditions don't have id yet
                    AlertCondition modifiedConditionWithoutId = new AlertCondition(condition);
                    if (conditionWithoutId.equals(modifiedConditionWithoutId)) {
                        continue;
                    }
                }
                if (condition.getMeasurementDefinition() != null) {
                    metricIdsToHide.add(String.valueOf(condition.getMeasurementDefinition().getId()));
                }
            }
        }

        LinkedHashMap<String, String> metricsMap = new LinkedHashMap<String, String>();
        Set<MeasurementDefinition> sortedDefs = new HashSet<MeasurementDefinition>(
            this.resourceType.getMetricDefinitions());

        for (MeasurementDefinition def : sortedDefs) {
            if (def.getDataType() == DataType.MEASUREMENT) {
                if (!dynamicOnly || def.getNumericType() == NumericType.DYNAMIC) {
                    String idString = String.valueOf(def.getId()); // use id, not name, for key; name is not unique when per-minute metric is also used
                    if (!metricIdsToHide.contains(idString)) {
                        metricsMap.put(idString, def.getDisplayName());
                    }
                }
            }
        }

        if (metricsMap.isEmpty()) {
            return null; // all metrics should be hidden
        }

        SelectItem metricSelection = new SortedSelectItem(itemName,
            MSG.view_alert_definition_condition_editor_metric_threshold_name());
        metricSelection.setValueMap(metricsMap);
        if (editMode) {
            metricSelection.setDefaultValue(String.valueOf(existingCondition.getMeasurementDefinition().getId()));
        } else {
            metricSelection.setDefaultToFirstOption(true);
        }
        metricSelection.setWidth("*");
        metricSelection.setRedrawOnChange(true);
        metricSelection.setShowIfCondition(ifFunc);
        return metricSelection;
    }

    private SelectItem buildCalltimeMetricDropDownMenu(String itemName, FormItemIfFunction ifFunc, boolean editMode) {

        LinkedHashMap<String, String> metricsMap = new LinkedHashMap<String, String>();
        for (MeasurementDefinition def : this.resourceType.getMetricDefinitions()) {
            if (def.getDataType() == DataType.CALLTIME) {
                metricsMap.put(String.valueOf(def.getId()), def.getDisplayName());
            }
        }

        SelectItem metricSelection = new SortedSelectItem(itemName,
            MSG.view_alert_definition_condition_editor_metric_calltime_common_name());
        metricSelection.setValueMap(metricsMap);
        if (editMode) {
            metricSelection.setDefaultValue(String.valueOf(existingCondition.getMeasurementDefinition().getId()));
        } else {
            metricSelection.setDefaultToFirstOption(true);
        }
        metricSelection.setWidth("*");
        metricSelection.setRedrawOnChange(true);
        metricSelection.setShowIfCondition(ifFunc);
        return metricSelection;
    }

    private SelectItem buildComparatorDropDownMenu(String itemName, FormItemIfFunction ifFunc, boolean editMode) {

        LinkedHashMap<String, String> comparators = new LinkedHashMap<String, String>(3);
        comparators.put("<", "< (" + MSG.view_alert_definition_condition_editor_metric_threshold_comparator_less()
            + ")");
        comparators.put("=", "= (" + MSG.view_alert_definition_condition_editor_metric_threshold_comparator_equal()
            + ")");
        comparators.put(">", "> (" + MSG.view_alert_definition_condition_editor_metric_threshold_comparator_greater()
            + ")");

        SelectItem comparatorSelection = new SortedSelectItem(itemName,
            MSG.view_alert_definition_condition_editor_metric_threshold_comparator());
        comparatorSelection.setValueMap(comparators);
        comparatorSelection.setDefaultValue(editMode ? existingCondition.getComparator() : "<");
        comparatorSelection
            .setTooltip(MSG.view_alert_definition_condition_editor_metric_threshold_comparator_tooltip());
        comparatorSelection.setHoverWidth(200);
        comparatorSelection.setShowIfCondition(ifFunc);
        return comparatorSelection;
    }

    private SelectItem buildCalltimeComparatorDropDownMenu(String itemName, FormItemIfFunction ifFunc, boolean editMode) {

        LinkedHashMap<String, String> comparators = new LinkedHashMap<String, String>(3);
        comparators.put("LO", MSG.view_alert_definition_condition_editor_metric_calltime_common_comparator_shrinks());
        comparators.put("CH", MSG.view_alert_definition_condition_editor_metric_calltime_common_comparator_changes());
        comparators.put("HI", MSG.view_alert_definition_condition_editor_metric_calltime_common_comparator_grows());

        SelectItem comparatorSelection = new SortedSelectItem(itemName,
            MSG.view_alert_definition_condition_editor_metric_calltime_common_comparator());
        comparatorSelection.setValueMap(comparators);
        comparatorSelection.setDefaultValue(editMode ? existingCondition.getComparator() : "CH");
        comparatorSelection.setTooltip(MSG
            .view_alert_definition_condition_editor_metric_calltime_common_comparator_tooltip());
        comparatorSelection.setHoverWidth(200);
        comparatorSelection.setShowIfCondition(ifFunc);
        return comparatorSelection;
    }

    private SelectItem buildRangeComparatorDropDownMenu(String itemName, FormItemIfFunction ifFunc, boolean editMode) {

        LinkedHashMap<String, String> comparators = new LinkedHashMap<String, String>(2);
        comparators.put("<", MSG.view_alert_definition_condition_editor_metric_range_comparator_inside_exclusive());
        comparators.put(">", MSG.view_alert_definition_condition_editor_metric_range_comparator_outside_exclusive());
        comparators.put("<=", MSG.view_alert_definition_condition_editor_metric_range_comparator_inside_inclusive());
        comparators.put(">=", MSG.view_alert_definition_condition_editor_metric_range_comparator_outside_inclusive());

        SelectItem comparatorSelection = new SortedSelectItem(itemName,
            MSG.view_alert_definition_condition_editor_metric_range_comparator());
        comparatorSelection.setValueMap(comparators);
        comparatorSelection.setDefaultValue(editMode ? existingCondition.getComparator() : "<");
        comparatorSelection.setTooltip(MSG.view_alert_definition_condition_editor_metric_range_comparator_tooltip());
        comparatorSelection.setHoverWidth(200);
        comparatorSelection.setShowIfCondition(ifFunc);
        return comparatorSelection;
    }

    private StaticTextItem buildBaseUnitsItem(final SelectItem metricDropDownMenu, FormItemIfFunction ifFunc,
        boolean editMode) {
        String baseUnits = MSG.view_alert_definition_condition_editor_common_baseUnits();
        final StaticTextItem baseUnitsItem = new StaticTextItem("baseUnits", baseUnits);
        baseUnitsItem.setHoverWidth(200);
        baseUnitsItem.setShowIfCondition(ifFunc);

        metricDropDownMenu.addChangedHandler(new ChangedHandler() {
            public void onChanged(ChangedEvent event) {
                MeasurementDefinition measDef = getMeasurementDefinition(metricDropDownMenu.getValueAsString());
                baseUnitsItem.setValue(measDef.getUnits() == MeasurementUnits.NONE ? MSG
                    .view_alert_definition_condition_editor_common_baseUnits_none() : measDef.getUnits().toString());
                List<MeasurementUnits> availableUnits = measDef.getUnits().getFamilyUnits();
                baseUnitsItem.setTooltip(MSG.view_alert_definition_condition_editor_common_baseUnits_availableUnits()
                    + (availableUnits.isEmpty() || availableUnits.get(0) == MeasurementUnits.NONE ? MSG
                        .view_alert_definition_condition_editor_common_baseUnits_none() : availableUnits));
            }
        });
        // initialize the field, the default will be the first entry in the value map
        MeasurementDefinition defaultMeasDef = getMeasurementDefinition((String) metricDropDownMenu
            .getAttributeAsMap("valueMap").keySet().iterator().next());
        MeasurementUnits units = defaultMeasDef.getUnits();
        if (editMode) {
            units = existingCondition.getMeasurementDefinition().getUnits();
        }
        baseUnitsItem.setValue(units == MeasurementUnits.NONE ? MSG.common_val_none() : units.toString());
        List<MeasurementUnits> availableUnits = units.getFamilyUnits();
        baseUnitsItem.setTooltip(MSG.view_alert_definition_condition_editor_common_baseUnits_availableUnits()
            + (availableUnits.isEmpty() || availableUnits.get(0) == MeasurementUnits.NONE ? MSG
                .view_alert_definition_condition_editor_common_baseUnits_none() : availableUnits));
        return baseUnitsItem;
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

    private MeasurementDefinition getMeasurementDefinition(String metricId) {
        int id = Integer.valueOf(metricId).intValue();
        for (MeasurementDefinition def : this.resourceType.getMetricDefinitions()) {
            if (id == def.getId()) {
                return def;
            }
        }
        CoreGUI.getErrorHandler().handleError(
            MSG.view_alert_definition_condition_editor_metric_common_definition_not_found());
        return null;
    }

    private class ShowIfCategoryFunction implements FormItemIfFunction {
        private final AlertConditionCategory category;
        private final String calltimeCategory;

        public ShowIfCategoryFunction(AlertConditionCategory category) {
            this.category = category;
            this.calltimeCategory = null;
        }

        public ShowIfCategoryFunction(String calltimeCategory) {
            this.category = null;
            this.calltimeCategory = calltimeCategory;
        }

        public boolean execute(FormItem item, Object value, DynamicForm form) {
            String conditionTypeString = form.getValue("conditionType").toString();
            if (category != null) {
                return category.name().equals(conditionTypeString);
            } else {
                return calltimeCategory.equals(conditionTypeString);
            }
        }
    }
}
