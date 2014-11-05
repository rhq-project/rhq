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

import java.util.LinkedHashMap;

import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.SpinnerItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;
import com.smartgwt.client.widgets.form.fields.events.ItemHoverEvent;
import com.smartgwt.client.widgets.form.fields.events.ItemHoverHandler;

import org.rhq.core.domain.alert.AlertDampening;
import org.rhq.core.domain.alert.AlertDampening.Category;
import org.rhq.core.domain.alert.AlertDampening.TimeUnits;
import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.Messages;

/**
 * @author John Mazzitelli
 */
public class DampeningAlertDefinitionForm extends DynamicForm implements EditAlertDefinitionForm {

    protected Messages MSG = CoreGUI.getMessages();

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

    private boolean updated;

    public DampeningAlertDefinitionForm() {
        this(null);
    }

    public DampeningAlertDefinitionForm(AlertDefinition alertDefinition) {
        super();
        this.alertDefinition = alertDefinition;
        this.updated = false;
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
                throw new IllegalStateException(MSG.view_alert_common_tab_invalid_dampening_category(category.name())); // should never happen
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
            throw new IllegalStateException(MSG.view_alert_common_tab_invalid_dampening_category(cat.name())); // should never happen
        }
        }

        markForRedraw();
    }

    @Override
    public void makeViewOnly() {
        updated = false;

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
            throw new IllegalStateException(MSG.view_alert_common_tab_invalid_dampening_category(cat.name())); // should never happen
        }
        }

        markForRedraw();
    }

    @Override
    public void saveAlertDefinition() {
        updated = false;

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
            throw new IllegalStateException(MSG.view_alert_common_tab_invalid_dampening_category(cat.name())); // should never happen
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

    @Override
    public boolean isResetMatching() {
        return updated;
    }

    private void buildForm() {
        if (!formBuilt) {
            dampeningRuleSelection = new SelectItem("dampeningRule", MSG.view_alert_common_tab_dampening());
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

            dampeningRuleStatic = new StaticTextItem("dampeningRuleStatic", MSG.view_alert_common_tab_dampening());
            dampeningRuleStatic.setWrapTitle(false);
            dampeningRuleStatic.setHoverWidth(300);

            // NONE
            // nothing to do - the none category has no ui components to render

            //  CONSECUTIVE_COUNT
            consecutiveOccurrencesSpinner = new SpinnerItem("consecutiveOccurrencesSpinner",
                MSG.view_alert_common_tab_dampening_consecutive_occurrences_label());
            consecutiveOccurrencesSpinner.setWrapTitle(false);
            consecutiveOccurrencesSpinner.setMin(1);
            consecutiveOccurrencesSpinner.setMax(999999);
            consecutiveOccurrencesSpinner.setStep(1);
            consecutiveOccurrencesSpinner.setDefaultValue(1);
            consecutiveOccurrencesSpinner.setHoverWidth(300);
            consecutiveOccurrencesSpinner.setTooltip(MSG
                .view_alert_common_tab_dampening_consecutive_occurrences_label_tooltip());
            consecutiveOccurrencesStatic = new StaticTextItem("consecutiveOccurrencesStatic",
                MSG.view_alert_common_tab_dampening_consecutive_occurrences_label());
            consecutiveOccurrencesStatic.setWrapTitle(false);

            consecutiveOccurrencesSpinner.addChangedHandler(new ChangedHandler() {
                public void onChanged(ChangedEvent event) {
                    updated = true;
                }
            });

            //  PARTIAL_COUNT
            partialOccurrencesSpinner = new SpinnerItem("partialOccurrencesSpinner",
                MSG.view_alert_common_tab_dampening_partial_occurrences_label());
            partialOccurrencesSpinner.setWrapTitle(false);
            partialOccurrencesSpinner.setMin(1);
            partialOccurrencesSpinner.setMax(999999);
            partialOccurrencesSpinner.setStep(1);
            partialOccurrencesSpinner.setDefaultValue(1);
            partialOccurrencesSpinner.setHoverWidth(300);
            partialOccurrencesSpinner.setTooltip(MSG
                .view_alert_common_tab_dampening_partial_occurrences_label_tooltip());
            partialOccurrencesStatic = new StaticTextItem("partialOccurrencesStatic",
                MSG.view_alert_common_tab_dampening_partial_occurrences_label());
            partialOccurrencesStatic.setWrapTitle(false);

            partialOccurrencesSpinner.addChangedHandler(new ChangedHandler() {
                public void onChanged(ChangedEvent event) {
                    updated = true;
                }
            });

            partialEvaluationsSpinner = new SpinnerItem("partialEvaluationsSpinner",
                MSG.view_alert_common_tab_dampening_partial_evalatuions_label());
            partialEvaluationsSpinner.setWrapTitle(false);
            partialEvaluationsSpinner.setMin(1);
            partialEvaluationsSpinner.setMax(999999);
            partialEvaluationsSpinner.setStep(1);
            partialEvaluationsSpinner.setDefaultValue(1);
            partialEvaluationsSpinner.setHoverWidth(300);
            partialEvaluationsSpinner.setTooltip(MSG
                .view_alert_common_tab_dampening_partial_evalatuions_label_tooltip());
            partialEvaluationsStatic = new StaticTextItem("partialEvaluationStatic",
                MSG.view_alert_common_tab_dampening_partial_evalatuions_label());
            partialEvaluationsStatic.setWrapTitle(false);

            partialEvaluationsSpinner.addChangedHandler(new ChangedHandler() {
                public void onChanged(ChangedEvent event) {
                    updated = true;
                }
            });

            //  DURATION_COUNT
            durationOccurrencesSpinner = new SpinnerItem("durationOccurrencesSpinner",
                MSG.view_alert_common_tab_dampening_duration_occurrences_label());
            durationOccurrencesSpinner.setWrapTitle(false);
            durationOccurrencesSpinner.setMin(1);
            durationOccurrencesSpinner.setMax(999999);
            durationOccurrencesSpinner.setStep(1);
            durationOccurrencesSpinner.setDefaultValue(1);
            durationOccurrencesSpinner.setHoverWidth(300);
            durationOccurrencesSpinner.setTooltip(MSG
                .view_alert_common_tab_dampening_duration_occurrences_label_tooltip());
            durationOccurrencesStatic = new StaticTextItem("durationOccurrencesStatic",
                MSG.view_alert_common_tab_dampening_duration_occurrences_label());
            durationOccurrencesStatic.setWrapTitle(false);

            durationOccurrencesSpinner.addChangedHandler(new ChangedHandler() {
                public void onChanged(ChangedEvent event) {
                    updated = true;
                }
            });

            durationTimePeriodSpinner = new SpinnerItem("durationTimePeriodSpinner",
                MSG.view_alert_common_tab_dampening_duration_period_label());
            durationTimePeriodSpinner.setWrapTitle(false);
            durationTimePeriodSpinner.setMin(1);
            durationTimePeriodSpinner.setMax(999999);
            durationTimePeriodSpinner.setStep(1);
            durationTimePeriodSpinner.setDefaultValue(1);
            durationTimePeriodSpinner.setHoverWidth(300);
            durationTimePeriodSpinner.setTooltip(MSG.view_alert_common_tab_dampening_duration_period_label_tooltip());
            durationTimePeriodStatic = new StaticTextItem("durationTimePeriodStatic",
                MSG.view_alert_common_tab_dampening_duration_period_label());
            durationTimePeriodStatic.setWrapTitle(false);

            durationTimePeriodSpinner.addChangedHandler(new ChangedHandler() {
                public void onChanged(ChangedEvent event) {
                    updated = true;
                }
            });

            durationTimeUnitsSelection = new SelectItem("durationTimeUnits", "");
            LinkedHashMap<String, String> units = new LinkedHashMap<String, String>(4);
            units.put(AlertDampening.TimeUnits.MINUTES.name(), getTimeUnitsTitle(AlertDampening.TimeUnits.MINUTES));
            units.put(AlertDampening.TimeUnits.HOURS.name(), getTimeUnitsTitle(AlertDampening.TimeUnits.HOURS));
            units.put(AlertDampening.TimeUnits.DAYS.name(), getTimeUnitsTitle(AlertDampening.TimeUnits.DAYS));
            units.put(AlertDampening.TimeUnits.WEEKS.name(), getTimeUnitsTitle(AlertDampening.TimeUnits.WEEKS));
            durationTimeUnitsSelection.setValueMap(units);
            durationTimeUnitsSelection.setDefaultValue(AlertDampening.TimeUnits.MINUTES.name());
            durationTimeUnitsStatic = new StaticTextItem("durationTimeUnitsStatic", "");

            durationTimeUnitsSelection.addChangedHandler(new ChangedHandler() {
                public void onChanged(ChangedEvent event) {
                    updated = true;
                }
            });

            dampeningRuleSelection.addChangedHandler(new ChangedHandler() {
                @Override
                public void onChanged(ChangedEvent event) {
                    updated = true;

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
                        throw new IllegalStateException(
                            MSG.view_alert_common_tab_invalid_dampening_category(cat.name())); // should never happen
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
            return MSG.common_val_none();
        }
        case CONSECUTIVE_COUNT: {
            return MSG.view_alert_common_tab_dampening_category_consecutive_count();
        }
        case PARTIAL_COUNT: {
            return MSG.view_alert_common_tab_dampening_category_partial_count();
        }
        case DURATION_COUNT: {
            return MSG.view_alert_common_tab_dampening_category_duration_count();
        }
        default: {
            throw new IllegalStateException(MSG.view_alert_common_tab_invalid_dampening_category(category.name())); // should never happen
        }
        }
    }

    private String getTimeUnitsTitle(AlertDampening.TimeUnits units) {
        switch (units) {
        case MINUTES: {
            return MSG.common_unit_minutes();
        }
        case HOURS: {
            return MSG.common_unit_hours();
        }
        case DAYS: {
            return MSG.common_unit_days();
        }
        case WEEKS: {
            return MSG.common_unit_weeks();
        }
        default: {
            throw new IllegalStateException(MSG.view_alert_common_tab_invalid_time_units(units.name())); // should never happen
        }
        }
    }

    private String getCategoryHelp(String categorySelection) {
        if (AlertDampening.Category.NONE.name().equals(categorySelection)) {
            return MSG.view_alert_common_tab_dampening_category_none_tooltip();
        } else if (AlertDampening.Category.CONSECUTIVE_COUNT.name().equals(categorySelection)) {
            return MSG.view_alert_common_tab_dampening_category_consecutive_count_tooltip();
        } else if (AlertDampening.Category.PARTIAL_COUNT.name().equals(categorySelection)) {
            return MSG.view_alert_common_tab_dampening_category_partial_count_tooltip();
        } else if (AlertDampening.Category.DURATION_COUNT.name().equals(categorySelection)) {
            return MSG.view_alert_common_tab_dampening_category_duration_count_tooltip();
        }

        return null; // should never happen
    }
}
