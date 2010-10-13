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

import java.util.LinkedHashMap;

import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.SpinnerItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;
import com.smartgwt.client.widgets.form.fields.events.ItemHoverEvent;
import com.smartgwt.client.widgets.form.fields.events.ItemHoverHandler;

import org.rhq.core.domain.alert.AlertDampening;
import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.alert.AlertDampening.Category;
import org.rhq.core.domain.alert.AlertDampening.TimeUnits;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;

/**
 * @author John Mazzitelli
 */
public class DampeningAlertDefinitionForm extends LocatableDynamicForm implements EditAlertDefinitionForm {

    private AlertDefinition alertDefinition;

    private boolean formBuilt = false;

    private SelectItem dampeningRuleSelection;
    private StaticTextItem dampeningRuleStatic;

    private SpinnerItem consecutiveOccurrencesSpinner;
    private StaticTextItem consecutiveOccurrencesStatic;

    private SpinnerItem partialOccurrencesSpinner;
    private StaticTextItem partialOccurrencesStatic;

    private SpinnerItem partialEvaluationsSpinner;
    private StaticTextItem partialEvaluationsStatic;

    private SpinnerItem durationOccurrencesSpinner;
    private StaticTextItem durationOccurrencesStatic;

    private SpinnerItem durationTimePeriodSpinner;
    private StaticTextItem durationTimePeriodStatic;

    private SelectItem durationTimeUnitsSelection;
    private StaticTextItem durationTimeUnitsStatic;

    public DampeningAlertDefinitionForm(String locatorId) {
        this(locatorId, null);
    }

    public DampeningAlertDefinitionForm(String locatorId, AlertDefinition alertDefinition) {
        super(locatorId);
        this.alertDefinition = alertDefinition;
    }

    @Override
    protected void onInit() {
        super.onInit();

        if (!formBuilt) {
            buildForm();
            setAlertDefinition(alertDefinition);
            makeViewOnly();
        }
    }

    @Override
    public AlertDefinition getAlertDefinition() {
        return alertDefinition;
    }

    @Override
    public void setAlertDefinition(AlertDefinition alertDef) {
        alertDefinition = alertDef;

        buildForm();

        if (alertDef == null) {
            clearFormValues();
        } else {
            clearFormValues();

            AlertDampening alertDampening = alertDef.getAlertDampening();
            if (alertDampening == null) {
                alertDampening = new AlertDampening(AlertDampening.Category.NONE);
                alertDefinition.setAlertDampening(alertDampening);
            }

            Category category = alertDampening.getCategory();
            dampeningRuleSelection.setValue(category.name());
            dampeningRuleStatic.setValue(getCategoryTitle(category));
            dampeningRuleStatic.setTooltip(getCategoryHelp(category.name()));

            switch (category) {
            case NONE: {
                break;
            }
            case CONSECUTIVE_COUNT: {
                consecutiveOccurrencesSpinner.setValue(alertDampening.getValue());
                consecutiveOccurrencesStatic.setValue(alertDampening.getValue());
                break;
            }
            case PARTIAL_COUNT: {
                partialOccurrencesSpinner.setValue(alertDampening.getValue());
                partialOccurrencesStatic.setValue(alertDampening.getValue());

                partialEvaluationsSpinner.setValue(alertDampening.getPeriod());
                partialEvaluationsStatic.setValue(alertDampening.getPeriod());
                break;
            }
            case DURATION_COUNT: {
                durationOccurrencesSpinner.setValue(alertDampening.getValue());
                durationOccurrencesStatic.setValue(alertDampening.getValue());

                durationTimePeriodSpinner.setValue(alertDampening.getPeriod());
                durationTimePeriodStatic.setValue(alertDampening.getPeriod());

                durationTimeUnitsSelection.setValue(alertDampening.getPeriodUnits().name());
                durationTimeUnitsStatic.setValue(getTimeUnitsTitle(alertDampening.getPeriodUnits()));
                break;
            }
            default: {
                throw new IllegalStateException("Invalid category - please report this as a bug: " + category); // should never happen
            }
            }
        }

        markForRedraw();
    }

    @Override
    public void makeEditable() {
        dampeningRuleSelection.show();
        dampeningRuleStatic.hide();

        AlertDampening.Category cat = AlertDampening.Category.valueOf(dampeningRuleSelection.getValue().toString());
        switch (cat) {
        case NONE: {
            consecutiveOccurrencesSpinner.hide();
            consecutiveOccurrencesStatic.hide();
            partialOccurrencesSpinner.hide();
            partialOccurrencesStatic.hide();
            partialEvaluationsSpinner.hide();
            partialEvaluationsStatic.hide();
            durationOccurrencesSpinner.hide();
            durationOccurrencesStatic.hide();
            durationTimePeriodSpinner.hide();
            durationTimePeriodStatic.hide();
            durationTimeUnitsSelection.hide();
            durationTimeUnitsStatic.hide();
            break;
        }
        case CONSECUTIVE_COUNT: {
            consecutiveOccurrencesSpinner.show();
            consecutiveOccurrencesStatic.hide();

            partialOccurrencesSpinner.hide();
            partialOccurrencesStatic.hide();
            partialEvaluationsSpinner.hide();
            partialEvaluationsStatic.hide();
            durationOccurrencesSpinner.hide();
            durationOccurrencesStatic.hide();
            durationTimePeriodSpinner.hide();
            durationTimePeriodStatic.hide();
            durationTimeUnitsSelection.hide();
            durationTimeUnitsStatic.hide();

            break;
        }
        case PARTIAL_COUNT: {
            partialOccurrencesSpinner.show();
            partialOccurrencesStatic.hide();

            partialEvaluationsSpinner.show();
            partialEvaluationsStatic.hide();

            consecutiveOccurrencesSpinner.hide();
            consecutiveOccurrencesStatic.hide();
            durationOccurrencesSpinner.hide();
            durationOccurrencesStatic.hide();
            durationTimePeriodSpinner.hide();
            durationTimePeriodStatic.hide();
            durationTimeUnitsSelection.hide();
            durationTimeUnitsStatic.hide();
            break;
        }
        case DURATION_COUNT: {
            durationOccurrencesSpinner.show();
            durationOccurrencesStatic.hide();

            durationTimePeriodSpinner.show();
            durationTimePeriodStatic.hide();

            durationTimeUnitsSelection.show();
            durationTimeUnitsStatic.hide();

            consecutiveOccurrencesSpinner.hide();
            consecutiveOccurrencesStatic.hide();
            partialOccurrencesSpinner.hide();
            partialOccurrencesStatic.hide();
            partialEvaluationsSpinner.hide();
            partialEvaluationsStatic.hide();
            break;
        }
        default: {
            throw new IllegalStateException("Bad dampening category - please report this bug: " + cat); // should never happen
        }
        }

        markForRedraw();
    }

    @Override
    public void makeViewOnly() {
        dampeningRuleSelection.hide();
        dampeningRuleStatic.show();

        AlertDampening.Category cat = AlertDampening.Category.valueOf(dampeningRuleSelection.getValue().toString());
        switch (cat) {
        case NONE: {
            consecutiveOccurrencesSpinner.hide();
            consecutiveOccurrencesStatic.hide();
            partialOccurrencesSpinner.hide();
            partialOccurrencesStatic.hide();
            partialEvaluationsSpinner.hide();
            partialEvaluationsStatic.hide();
            durationOccurrencesSpinner.hide();
            durationOccurrencesStatic.hide();
            durationTimePeriodSpinner.hide();
            durationTimePeriodStatic.hide();
            durationTimeUnitsSelection.hide();
            durationTimeUnitsStatic.hide();
            break;
        }
        case CONSECUTIVE_COUNT: {
            consecutiveOccurrencesSpinner.hide();
            consecutiveOccurrencesStatic.show();

            partialOccurrencesSpinner.hide();
            partialOccurrencesStatic.hide();
            partialEvaluationsSpinner.hide();
            partialEvaluationsStatic.hide();
            durationOccurrencesSpinner.hide();
            durationOccurrencesStatic.hide();
            durationTimePeriodSpinner.hide();
            durationTimePeriodStatic.hide();
            durationTimeUnitsSelection.hide();
            durationTimeUnitsStatic.hide();
            break;
        }
        case PARTIAL_COUNT: {
            partialOccurrencesSpinner.hide();
            partialOccurrencesStatic.show();

            partialEvaluationsSpinner.hide();
            partialEvaluationsStatic.show();

            consecutiveOccurrencesSpinner.hide();
            consecutiveOccurrencesStatic.hide();
            durationOccurrencesSpinner.hide();
            durationOccurrencesStatic.hide();
            durationTimePeriodSpinner.hide();
            durationTimePeriodStatic.hide();
            durationTimeUnitsSelection.hide();
            durationTimeUnitsStatic.hide();
            break;
        }
        case DURATION_COUNT: {
            durationOccurrencesSpinner.hide();
            durationOccurrencesStatic.show();

            durationTimePeriodSpinner.hide();
            durationTimePeriodStatic.show();

            durationTimeUnitsSelection.hide();
            durationTimeUnitsStatic.show();

            consecutiveOccurrencesSpinner.hide();
            consecutiveOccurrencesStatic.hide();
            partialOccurrencesSpinner.hide();
            partialOccurrencesStatic.hide();
            partialEvaluationsSpinner.hide();
            partialEvaluationsStatic.hide();
            break;
        }
        default: {
            throw new IllegalStateException("Bad dampening category - please report this bug: " + cat); // should never happen
        }
        }

        markForRedraw();
    }

    @Override
    public void saveAlertDefinition() {
        AlertDampening.Category cat = AlertDampening.Category.valueOf(dampeningRuleSelection.getValue().toString());
        AlertDampening alertDampening = new AlertDampening(cat);
        switch (cat) {
        case NONE: {
            // each time condition set is true
            alertDampening.setValue(0);
            alertDampening.setValueUnits(null);
            alertDampening.setPeriod(0);
            alertDampening.setPeriodUnits(null);
            break;
        }
        case CONSECUTIVE_COUNT: {
            // once every N times condition set is true consecutively
            alertDampening.setValue(Integer.valueOf(consecutiveOccurrencesSpinner.getValue().toString()));
            alertDampening.setValueUnits(null);
            alertDampening.setPeriod(0);
            alertDampening.setPeriodUnits(null);
            break;
        }
        case PARTIAL_COUNT: {
            // once every N times condition set is true during the last M evaluations
            alertDampening.setValue(Integer.valueOf(partialOccurrencesSpinner.getValue().toString()));
            alertDampening.setValueUnits(null);
            alertDampening.setPeriod(Integer.valueOf(partialEvaluationsSpinner.getValue().toString()));
            alertDampening.setPeriodUnits(null);
            break;
        }
        case DURATION_COUNT: {
            // once every N times condition set is true within a time period of M {mins, hours, days, weeks}
            alertDampening.setValue(Integer.valueOf(durationOccurrencesSpinner.getValue().toString()));
            alertDampening.setValueUnits(null);
            alertDampening.setPeriod(Integer.valueOf(durationTimePeriodSpinner.getValue().toString()));
            alertDampening.setPeriodUnits(TimeUnits.valueOf(durationTimeUnitsSelection.getValue().toString()));
            break;
        }
        default: {
            throw new IllegalStateException("Bad dampening category - please report this bug: " + cat); // should never happen
        }
        }
        alertDefinition.setAlertDampening(alertDampening);
    }

    @Override
    public void clearFormValues() {
        dampeningRuleSelection.clearValue();
        consecutiveOccurrencesSpinner.clearValue();
        partialOccurrencesSpinner.clearValue();
        partialEvaluationsSpinner.clearValue();
        durationOccurrencesSpinner.clearValue();
        durationTimePeriodSpinner.clearValue();
        durationTimeUnitsSelection.clearValue();

        dampeningRuleStatic.clearValue();
        consecutiveOccurrencesStatic.clearValue();
        partialOccurrencesStatic.clearValue();
        partialEvaluationsStatic.clearValue();
        durationOccurrencesStatic.clearValue();
        durationTimePeriodStatic.clearValue();
        durationTimeUnitsStatic.clearValue();

        markForRedraw();
    }

    private void buildForm() {
        if (!formBuilt) {
            dampeningRuleSelection = new SelectItem("dampeningRule", "Dampening Rule");
            LinkedHashMap<String, String> rules = new LinkedHashMap<String, String>(4);
            rules.put(AlertDampening.Category.NONE.name(), getCategoryTitle(AlertDampening.Category.NONE));
            rules.put(AlertDampening.Category.CONSECUTIVE_COUNT.name(),
                getCategoryTitle(AlertDampening.Category.CONSECUTIVE_COUNT));
            rules.put(AlertDampening.Category.PARTIAL_COUNT.name(),
                getCategoryTitle(AlertDampening.Category.PARTIAL_COUNT));
            rules.put(AlertDampening.Category.DURATION_COUNT.name(),
                getCategoryTitle(AlertDampening.Category.DURATION_COUNT));
            dampeningRuleSelection.setValueMap(rules);
            dampeningRuleSelection.setDefaultValue(AlertDampening.Category.NONE.name());
            dampeningRuleSelection.setWrapTitle(false);
            dampeningRuleSelection.setRedrawOnChange(true);
            dampeningRuleSelection.setHoverWidth(300);
            dampeningRuleSelection.addItemHoverHandler(new ItemHoverHandler() {
                @Override
                public void onItemHover(ItemHoverEvent event) {
                    String selection = dampeningRuleSelection.getValue().toString();
                    dampeningRuleSelection.setTooltip(getCategoryHelp(selection));
                }
            });

            dampeningRuleStatic = new StaticTextItem("dampeningRuleStatic", "Dampening Rule");
            dampeningRuleStatic.setWrapTitle(false);
            dampeningRuleStatic.setHoverWidth(300);

            // NONE
            // nothing to do - the none category has no ui components to render

            //  CONSECUTIVE_COUNT
            consecutiveOccurrencesSpinner = new SpinnerItem("consecutiveOccurrencesSpinner", "Occurrences");
            consecutiveOccurrencesSpinner.setWrapTitle(false);
            consecutiveOccurrencesSpinner.setMin(1);
            consecutiveOccurrencesSpinner.setMax(999999);
            consecutiveOccurrencesSpinner.setStep(1);
            consecutiveOccurrencesSpinner.setDefaultValue(1);
            consecutiveOccurrencesSpinner.setHoverWidth(300);
            consecutiveOccurrencesSpinner
                .setTooltip("The number of times the condition set must be consecutively true before the alert is triggered.");
            consecutiveOccurrencesStatic = new StaticTextItem("consecutiveOccurrencesStatic", "Occurrences");
            consecutiveOccurrencesStatic.setWrapTitle(false);

            //  PARTIAL_COUNT
            partialOccurrencesSpinner = new SpinnerItem("partialOccurrencesSpinner", "Occurrences");
            partialOccurrencesSpinner.setWrapTitle(false);
            partialOccurrencesSpinner.setMin(1);
            partialOccurrencesSpinner.setMax(999999);
            partialOccurrencesSpinner.setStep(1);
            partialOccurrencesSpinner.setDefaultValue(1);
            partialOccurrencesSpinner.setHoverWidth(300);
            partialOccurrencesSpinner
                .setTooltip("The number of times the condition set must be true during the last N evaluations before the alert is triggered.");
            partialOccurrencesStatic = new StaticTextItem("partialOccurrencesStatic", "Occurrences");
            partialOccurrencesStatic.setWrapTitle(false);

            partialEvaluationsSpinner = new SpinnerItem("partialEvaluationsSpinner", "Evaluations");
            partialEvaluationsSpinner.setWrapTitle(false);
            partialEvaluationsSpinner.setMin(1);
            partialEvaluationsSpinner.setMax(999999);
            partialEvaluationsSpinner.setStep(1);
            partialEvaluationsSpinner.setDefaultValue(1);
            partialEvaluationsSpinner.setHoverWidth(300);
            partialEvaluationsSpinner
                .setTooltip("The total number of times the condition set will be tested to see if the given number of occurrences are true.");
            partialEvaluationsStatic = new StaticTextItem("partialEvaluationStatic", "Evaluations");
            partialEvaluationsStatic.setWrapTitle(false);

            //  DURATION_COUNT
            durationOccurrencesSpinner = new SpinnerItem("durationOccurrencesSpinner", "Occurrences");
            durationOccurrencesSpinner.setWrapTitle(false);
            durationOccurrencesSpinner.setMin(1);
            durationOccurrencesSpinner.setMax(999999);
            durationOccurrencesSpinner.setStep(1);
            durationOccurrencesSpinner.setDefaultValue(1);
            durationOccurrencesSpinner.setHoverWidth(300);
            durationOccurrencesSpinner
                .setTooltip("The number of times the condition set must be true during the given time period before the alert is triggered.");
            durationOccurrencesStatic = new StaticTextItem("durationOccurrencesStatic", "Occurrences");
            durationOccurrencesStatic.setWrapTitle(false);

            durationTimePeriodSpinner = new SpinnerItem("durationTimePeriodSpinner", "Time Period");
            durationTimePeriodSpinner.setWrapTitle(false);
            durationTimePeriodSpinner.setMin(1);
            durationTimePeriodSpinner.setMax(999999);
            durationTimePeriodSpinner.setStep(1);
            durationTimePeriodSpinner.setDefaultValue(1);
            durationTimePeriodSpinner.setHoverWidth(300);
            durationTimePeriodSpinner
                .setTooltip("The time span in which the condition set will be tested to see if the given number of occurrences are true.");
            durationTimePeriodStatic = new StaticTextItem("durationTimePeriodStatic", "Time Period");
            durationTimePeriodStatic.setWrapTitle(false);

            durationTimeUnitsSelection = new SelectItem("durationTimeUnits", "");
            LinkedHashMap<String, String> units = new LinkedHashMap<String, String>(4);
            units.put(AlertDampening.TimeUnits.MINUTES.name(), getTimeUnitsTitle(AlertDampening.TimeUnits.MINUTES));
            units.put(AlertDampening.TimeUnits.HOURS.name(), getTimeUnitsTitle(AlertDampening.TimeUnits.HOURS));
            units.put(AlertDampening.TimeUnits.DAYS.name(), getTimeUnitsTitle(AlertDampening.TimeUnits.DAYS));
            units.put(AlertDampening.TimeUnits.WEEKS.name(), getTimeUnitsTitle(AlertDampening.TimeUnits.WEEKS));
            durationTimeUnitsSelection.setValueMap(units);
            durationTimeUnitsSelection.setDefaultValue(AlertDampening.TimeUnits.MINUTES.name());
            durationTimeUnitsStatic = new StaticTextItem("durationTimeUnitsStatic", "");

            dampeningRuleSelection.addChangedHandler(new ChangedHandler() {
                @Override
                public void onChanged(ChangedEvent event) {
                    Category cat = AlertDampening.Category.valueOf(event.getValue().toString());
                    switch (cat) {
                    case NONE: {
                        consecutiveOccurrencesSpinner.hide();
                        partialOccurrencesSpinner.hide();
                        partialEvaluationsSpinner.hide();
                        durationOccurrencesSpinner.hide();
                        durationTimePeriodSpinner.hide();
                        durationTimeUnitsSelection.hide();
                        break;
                    }
                    case CONSECUTIVE_COUNT: {
                        consecutiveOccurrencesSpinner.show();
                        partialOccurrencesSpinner.hide();
                        partialEvaluationsSpinner.hide();
                        durationOccurrencesSpinner.hide();
                        durationTimePeriodSpinner.hide();
                        durationTimeUnitsSelection.hide();
                        break;
                    }
                    case PARTIAL_COUNT: {
                        consecutiveOccurrencesSpinner.hide();
                        partialOccurrencesSpinner.show();
                        partialEvaluationsSpinner.show();
                        durationOccurrencesSpinner.hide();
                        durationTimePeriodSpinner.hide();
                        durationTimeUnitsSelection.hide();
                        break;
                    }
                    case DURATION_COUNT: {
                        consecutiveOccurrencesSpinner.hide();
                        partialOccurrencesSpinner.hide();
                        partialEvaluationsSpinner.hide();
                        durationOccurrencesSpinner.show();
                        durationTimePeriodSpinner.show();
                        durationTimeUnitsSelection.show();
                        break;
                    }
                    default: {
                        throw new IllegalStateException("Invalid category - please report this as a bug: " + cat); // should never happen
                    }
                    }
                    markForRedraw();
                }
            });

            // put all the fields in the form now
            setFields(dampeningRuleSelection, dampeningRuleStatic, consecutiveOccurrencesSpinner,
                consecutiveOccurrencesStatic, partialOccurrencesSpinner, partialOccurrencesStatic,
                partialEvaluationsSpinner, partialEvaluationsStatic, durationOccurrencesSpinner,
                durationOccurrencesStatic, durationTimePeriodSpinner, durationTimePeriodStatic,
                durationTimeUnitsSelection, durationTimeUnitsStatic);

            formBuilt = true;
        }
    }

    private String getCategoryTitle(AlertDampening.Category category) {
        switch (category) {
        case NONE: {
            return "None";
        }
        case CONSECUTIVE_COUNT: {
            return "Consecutive";
        }
        case PARTIAL_COUNT: {
            return "Last N Evaluations";
        }
        case DURATION_COUNT: {
            return "Time Period";
        }
        default: {
            throw new IllegalStateException("Invalid category - please report this as a bug: " + category); // should never happen
        }
        }
    }

    private String getTimeUnitsTitle(AlertDampening.TimeUnits units) {
        switch (units) {
        case MINUTES: {
            return "minutes";
        }
        case HOURS: {
            return "hours";
        }
        case DAYS: {
            return "days";
        }
        case WEEKS: {
            return "weeks";
        }
        default: {
            throw new IllegalStateException("Invalid time units - please report this as a bug: " + units); // should never happen
        }
        }
    }

    private String getCategoryHelp(String categorySelection) {
        if (AlertDampening.Category.NONE.name().equals(categorySelection)) {
            return "Dampening is disabled. Every time the condition set is true, an alert will be triggered.";
        } else if (AlertDampening.Category.CONSECUTIVE_COUNT.name().equals(categorySelection)) {
            return "An alert is triggered once every X occurrences the condition set is true consecutively.";
        } else if (AlertDampening.Category.PARTIAL_COUNT.name().equals(categorySelection)) {
            return "An alert is triggered once every X occurrences the condition set is true during the last N evaluations of the condition set.";
        } else if (AlertDampening.Category.DURATION_COUNT.name().equals(categorySelection)) {
            return "An alert is triggered once every X occurrences the condition set is true within a given time period.";
        }

        return null; // should never happen
    }
}
