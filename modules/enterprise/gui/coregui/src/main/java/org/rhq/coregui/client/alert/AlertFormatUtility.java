/*
 * RHQ Management Platform
 * Copyright (C) 2010 Red Hat, Inc.
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
package org.rhq.coregui.client.alert;

import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.alert.AlertCondition;
import org.rhq.core.domain.alert.AlertConditionCategory;
import org.rhq.core.domain.alert.AlertConditionOperator;
import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.measurement.MeasurementUnits;
import org.rhq.core.domain.measurement.composite.MeasurementNumericValueAndUnits;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.Messages;
import org.rhq.coregui.client.util.MeasurementConverterClient;

/**
 * The methods in this class are ported from AlertDefUtil from portal-war and MeasurementFormatter from
 * server-jar.
 *
 * @author Ian Springer
 * @author John Mazzitelli
 */
public class AlertFormatUtility {
    static Messages MSG = CoreGUI.getMessages();

    private AlertFormatUtility() {
    }

    public static String formatAlertConditionForDisplay(AlertCondition condition) {
        StringBuilder str = new StringBuilder();

        AlertConditionCategory category = condition.getCategory();
        switch (category) {
        case AVAILABILITY: {
            str.append(MSG.common_title_availability());
            str.append(" [");
            AlertConditionOperator operator = AlertConditionOperator.valueOf(condition.getName().toUpperCase());
            switch (operator) {
            case AVAIL_GOES_DISABLED:
                str.append(MSG.view_alert_definition_condition_editor_operator_availability_goesDisabled());
                break;
            case AVAIL_GOES_DOWN:
                str.append(MSG.view_alert_definition_condition_editor_operator_availability_goesDown());
                break;
            case AVAIL_GOES_UNKNOWN:
                str.append(MSG.view_alert_definition_condition_editor_operator_availability_goesUnknown());
                break;
            case AVAIL_GOES_UP:
                str.append(MSG.view_alert_definition_condition_editor_operator_availability_goesUp());
                break;
            case AVAIL_GOES_NOT_UP:
                str.append(MSG.view_alert_definition_condition_editor_operator_availability_goesNotUp());
                break;
            default:
                str.append("*ERROR*");
            }
            str.append("]");

            break;
        }
        case AVAIL_DURATION: {
            str.append(MSG.view_alert_definition_condition_editor_availabilityDuration());
            str.append(" [");
            AlertConditionOperator operator = AlertConditionOperator.valueOf(condition.getName().toUpperCase());
            switch (operator) {
            case AVAIL_DURATION_DOWN:
                str.append(MSG.view_alert_definition_condition_editor_operator_availability_durationDown());
                break;
            case AVAIL_DURATION_NOT_UP:
                str.append(MSG.view_alert_definition_condition_editor_operator_availability_durationNotUp());
                break;
            default:
                str.append("*ERROR*");
            }
            str.append(" For ");

            long longValue = Long.valueOf(condition.getOption());
            MeasurementNumericValueAndUnits valueWithUnits;
            if (longValue % 3600 == 0) {
                if (longValue == 3600) {
                    valueWithUnits = new MeasurementNumericValueAndUnits(1.0d, MeasurementUnits.HOURS);
                } else {
                    valueWithUnits = MeasurementConverterClient.fit((double) longValue, MeasurementUnits.SECONDS,
                        MeasurementUnits.HOURS, MeasurementUnits.HOURS);
                }
            } else if (longValue == 60) {
                valueWithUnits = new MeasurementNumericValueAndUnits(1.0d, MeasurementUnits.MINUTES);
            } else {
                valueWithUnits = MeasurementConverterClient.fit((double) longValue, MeasurementUnits.SECONDS,
                        MeasurementUnits.MINUTES, MeasurementUnits.MINUTES);
            }
            String formatted = MeasurementConverterClient.format(String.valueOf(valueWithUnits.getValue()),
                valueWithUnits.getUnits());
            str.append(formatted);
            str.append("]");

            break;
        }
        case THRESHOLD: {
            double value = condition.getThreshold();
            MeasurementUnits units = condition.getMeasurementDefinition().getUnits();
            String formatted = MeasurementConverterClient.format(value, units, true);

            if (condition.getOption() == null) {
                str.append(MSG.view_alert_common_tab_conditions_type_metric_threshold());
                str.append(" [");
                str.append(condition.getName());
                str.append(" ");
                str.append(condition.getComparator());
                str.append(" ");
                str.append(formatted);
                str.append("]");
            } else {
                // this is a calltime threshold condition
                str.append(MSG.view_alert_common_tab_conditions_type_metric_calltime_threshold());
                str.append(" [");
                if (condition.getMeasurementDefinition() != null) {
                    str.append(condition.getMeasurementDefinition().getDisplayName());
                    str.append(" ");
                }
                str.append(condition.getOption()); // MIN, MAX, AVG (never null)
                str.append(" ");
                str.append(condition.getComparator()); // <, >, =
                str.append(" ");
                str.append(formatted);
                str.append("]");
                if (condition.getName() != null && condition.getName().length() > 0) {
                    str.append(" ");
                    str.append(MSG.view_alert_common_tab_conditions_type_metric_calltime_destination());
                    str.append(" '");
                    str.append(condition.getName());
                    str.append("'");
                }
            }
            break;
        }
        case BASELINE: {
            str.append(MSG.view_alert_common_tab_conditions_type_metric_baseline());
            str.append(" [");
            str.append(condition.getName());
            str.append(" ");
            str.append(condition.getComparator());
            str.append(" ");

            double value = condition.getThreshold();
            MeasurementUnits units = MeasurementUnits.PERCENTAGE;
            String formatted = MeasurementConverterClient.format(value, units, true);
            str.append(formatted);

            str.append(" ").append(MSG.view_alert_common_tab_conditions_type_metric_baseline_verb()).append(" ");
            str.append(condition.getOption());
            str.append("]");
            break;
        }
        case CHANGE: {
            if (condition.getOption() == null) {
                str.append(MSG.view_alert_common_tab_conditions_type_metric_change());
                str.append(" [");
                str.append(condition.getName());
                str.append("]");
            } else {
                // this is a calltime change condition
                str.append(MSG.view_alert_common_tab_conditions_type_metric_calltime_change());
                str.append(" [");
                if (condition.getMeasurementDefinition() != null) {
                    str.append(condition.getMeasurementDefinition().getDisplayName());
                    str.append(" ");
                }
                str.append(condition.getOption()); // MIN, MAX, AVG (never null)
                str.append(" ");
                str.append(getCalltimeChangeComparator(condition.getComparator())); // LO, HI, CH
                str.append(" ");
                str.append(MSG.view_alert_common_tab_conditions_type_metric_calltime_change_verb());
                str.append(" ");

                double value = condition.getThreshold();
                MeasurementUnits units = MeasurementUnits.PERCENTAGE;
                String formatted = MeasurementConverterClient.format(value, units, true);
                str.append(formatted);

                str.append("]");
                if (condition.getName() != null && condition.getName().length() > 0) {
                    str.append(" ");
                    str.append(MSG.view_alert_common_tab_conditions_type_metric_calltime_destination());
                    str.append(" '");
                    str.append(condition.getName());
                    str.append("'");
                }
            }
            break;
        }
        case TRAIT: {
            str.append(MSG.view_alert_common_tab_conditions_type_metric_trait_change());
            str.append(" [");
            str.append(condition.getName());
            str.append("]");
            if (condition.getOption() != null && condition.getOption().length() > 0) {
                str.append(" ");
                str.append(MSG.view_alert_common_tab_conditions_type_metric_trait_matching());
                str.append(" '");
                str.append(condition.getOption());
                str.append("'");
            }

            break;
        }
        case CONTROL: {
            str.append(MSG.view_alert_common_tab_conditions_type_operation());
            str.append(" [");
            str.append(condition.getName());
            str.append("] ");
            str.append(MSG.view_alert_common_tab_conditions_type_operation_status());
            str.append(" [");
            str.append(condition.getOption());
            str.append("]");
            break;
        }
        case RESOURCE_CONFIG: {
            str.append(MSG.view_alert_common_tab_conditions_type_resource_configuration());
            break;
        }
        case EVENT: {
            str.append(MSG.view_alert_common_tab_conditions_type_event());
            str.append(" [");
            str.append(condition.getName());
            str.append("]");
            if (condition.getOption() != null && condition.getOption().length() > 0) {
                String eventDetailsRegexValue = "", eventSourcePathRegexValue = "";
                if (condition.getOption().contains(AlertCondition.ADHOC_SEPARATOR)) {
                    String[] regexes = condition.getOption().split(AlertCondition.ADHOC_SEPARATOR);
                    if (regexes.length > 0) {
                        eventDetailsRegexValue = regexes[0];
                        if (regexes.length > 1) {
                            eventSourcePathRegexValue = regexes[1];
                        }
                    }
                } else {
                    eventDetailsRegexValue = condition.getOption(); // old approach -> probably working with db before rhq 4.13
                }
                if (!eventSourcePathRegexValue.isEmpty()) {
                    str.append(" ");
                    str.append(MSG.view_alert_common_tab_conditions_type_event_matching());
                    str.append(" '");
                    str.append(eventSourcePathRegexValue);
                    str.append("'");
                }
                if (!eventDetailsRegexValue.isEmpty()) {
                    str.append(" ");
                    str.append(MSG.view_alert_common_tab_conditions_type_event_details_matching());
                    str.append(" '");
                    str.append(eventDetailsRegexValue);
                    str.append("'");
                }
            }
            break;
        }
        case DRIFT: {
            String configNameRegex = condition.getName();
            String pathNameRegex = condition.getOption();
            if (configNameRegex == null || configNameRegex.length() == 0) {
                if (pathNameRegex == null || pathNameRegex.length() == 0) {
                    // neither a config name regex nor path regex was specified 
                    str.append(MSG.view_alert_common_tab_conditions_type_drift());
                } else {
                    // a path name regex was specified, but not a config name regex 
                    str.append(MSG.view_alert_common_tab_conditions_type_drift_onlypaths(pathNameRegex));
                }
            } else {
                if (pathNameRegex == null || pathNameRegex.length() == 0) {
                    // a config name regex was specified, but not a path name regex 
                    str.append(MSG.view_alert_common_tab_conditions_type_drift_onlyconfig(configNameRegex));
                } else {
                    // both a config name regex and a path regex was specified 
                    str.append(MSG.view_alert_common_tab_conditions_type_drift_configpaths(pathNameRegex,
                        configNameRegex));
                }
            }
            break;
        }
        case RANGE: {
            String metricName = condition.getName();
            MeasurementUnits units = condition.getMeasurementDefinition().getUnits();
            double loValue = condition.getThreshold();
            String formattedLoValue = MeasurementConverterClient.format(loValue, units, true);
            String formattedHiValue = condition.getOption();
            try {
                double hiValue = Double.parseDouble(formattedHiValue);
                formattedHiValue = MeasurementConverterClient.format(hiValue, units, true);
            } catch (Exception e) {
                formattedHiValue = "?[" + formattedHiValue + "]?"; // signify something is wrong with the value
            }

            // < means "inside the range", > means "outside the range" - exclusive
            // <= means "inside the range", >= means "outside the range" - inclusive

            if (condition.getComparator().equals("<")) {
                str.append(MSG.view_alert_common_tab_conditions_type_metric_range_inside_exclusive(metricName,
                    formattedLoValue, formattedHiValue));
            } else if (condition.getComparator().equals(">")) {
                str.append(MSG.view_alert_common_tab_conditions_type_metric_range_outside_exclusive(metricName,
                    formattedLoValue, formattedHiValue));
            } else if (condition.getComparator().equals("<=")) {
                str.append(MSG.view_alert_common_tab_conditions_type_metric_range_inside_inclusive(metricName,
                    formattedLoValue, formattedHiValue));
            } else if (condition.getComparator().equals(">=")) {
                str.append(MSG.view_alert_common_tab_conditions_type_metric_range_outside_inclusive(metricName,
                    formattedLoValue, formattedHiValue));
            } else {
                str.append("BAD COMPARATOR! Report this bug: " + condition.getComparator());
            }
            break;
        }
        default: {
            str.append(MSG.view_alert_common_tab_invalid_condition_category(category.name()));
            break;
        }
        }
        return str.toString();
    }

    private static String getCalltimeChangeComparator(String comparator) {
        if ("HI".equals(comparator)) {
            return MSG.view_alert_common_tab_conditions_type_metric_calltime_delta_grows();
        } else if ("LO".equals(comparator)) {
            return MSG.view_alert_common_tab_conditions_type_metric_calltime_delta_shrinks();
        } else { // CH
            return MSG.view_alert_common_tab_conditions_type_metric_calltime_delta_other();
        }
    }

    public static String getAlertRecoveryInfo(Alert alert) {
        String recoveryInfo;
        AlertDefinition recoveryAlertDefinition = alert.getRecoveryAlertDefinition();
        if (recoveryAlertDefinition != null && recoveryAlertDefinition.getId() != 0) {
            int resourceId = alert.getAlertDefinition().getResource().getId();
            String otherAlertDef = "<a href=\"/alerts/Config.do?mode=viewRoles&id=" + resourceId + "&ad="
                + recoveryAlertDefinition.getId() + "\">" + recoveryAlertDefinition.getName() + "</a>";
            recoveryInfo = MSG.view_alert_common_tab_conditions_recovery_enabled(otherAlertDef);
        } else if (alert.getWillRecover()) {
            recoveryInfo = MSG.view_alert_common_tab_conditions_recovery_disabled();
        } else {
            recoveryInfo = MSG.common_val_na();
        }
        return recoveryInfo;
    }
}
