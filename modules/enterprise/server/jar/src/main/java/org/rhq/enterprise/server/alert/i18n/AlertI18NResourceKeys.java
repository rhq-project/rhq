/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.enterprise.server.alert.i18n;

import mazz.i18n.annotation.I18NMessage;
import mazz.i18n.annotation.I18NMessages;
import mazz.i18n.annotation.I18NResourceBundle;

@I18NResourceBundle(baseName = "alert-messages", defaultLocale = "en")
public interface AlertI18NResourceKeys {

    @I18NMessages({ @I18NMessage("Availability goes DISABLED") })
    String ALERT_AVAILABILITY_GOES_DISABLED = "alert.condition.availability.disabled";

    @I18NMessages({ @I18NMessage("Avail goes DISABLED") })
    String ALERT_AVAILABILITY_GOES_DISABLED_SHORT = "alert.condition.availability.disabled.short";

    @I18NMessages({ @I18NMessage("Availability goes DOWN"),
        @I18NMessage(locale = "de", value = "Verf�gbarkeit wird NACH UNTEN") })
    String ALERT_AVAILABILITY_GOES_DOWN = "alert.condition.availability.down";

    @I18NMessages({ @I18NMessage("Avail goes DOWN"), @I18NMessage(locale = "de", value = "Verf. wird NACH UNTEN") })
    String ALERT_AVAILABILITY_GOES_DOWN_SHORT = "alert.condition.availability.down.short";

    @I18NMessages({ @I18NMessage("Availability goes NOT UP") })
    String ALERT_AVAILABILITY_GOES_NOT_UP = "alert.condition.availability.notup";

    @I18NMessages({ @I18NMessage("Avail goes NOT UP") })
    String ALERT_AVAILABILITY_GOES_NOT_UP_SHORT = "alert.condition.availability.notup.short";

    @I18NMessages({ @I18NMessage("Availability goes UP"), @I18NMessage(locale = "de", value = "Verf�gbarkeit wird AUF") })
    String ALERT_AVAILABILITY_GOES_UP = "alert.condition.availability.up";

    @I18NMessages({ @I18NMessage("Avail goes UP"), @I18NMessage(locale = "de", value = "Verf. wird AUF") })
    String ALERT_AVAILABILITY_GOES_UP_SHORT = "alert.condition.availability.up.short";

    @I18NMessages({ @I18NMessage("Availability goes UNKNOWN") })
    String ALERT_AVAILABILITY_GOES_UNKNOWN = "alert.condition.availability.unknown";

    @I18NMessages({ @I18NMessage("Avail goes UNKNOWN") })
    String ALERT_AVAILABILITY_GOES_UNKNOWN_SHORT = "alert.condition.availability.unknown.short";

    @I18NMessages({ @I18NMessage("Availability stays DOWN") })
    String ALERT_AVAILABILITY_DURATION_DOWN = "alert.condition.availability.duration.down";

    @I18NMessages({ @I18NMessage("Avail stays DOWN") })
    String ALERT_AVAILABILITY_DURATION_DOWN_SHORT = "alert.condition.availability.duration.down.short";

    @I18NMessages({ @I18NMessage("Availability stays NOT UP") })
    String ALERT_AVAILABILITY_DURATION_NOT_UP = "alert.condition.availability.duration.notup";

    @I18NMessages({ @I18NMessage("Avail stays NOT UP") })
    String ALERT_AVAILABILITY_DURATION_NOT_UP_SHORT = "alert.condition.availability.duration.notup.short";

    // Foo Prop > 10.0% of Baseline Mean Value
    @I18NMessages({ @I18NMessage("{0} {1} {2} of Baseline Mean Value") /*, @I18NMessage(locale = "de", value = "") */})
    String ALERT_BASELINE_MEAN = "alert.condition.baseline.mean";

    // Foo Prop > 10.0% bl mean
    @I18NMessages({ @I18NMessage("{0} {1} {2} bl mean") /*, @I18NMessage(locale = "de", value = "") */})
    String ALERT_BASELINE_MEAN_SHORT = "alert.condition.baseline.mean.short";

    @I18NMessages({ @I18NMessage("{0} {1} {2} of Baseline Minimum Value") /*, @I18NMessage(locale = "de", value = "") */})
    String ALERT_BASELINE_MIN = "alert.condition.baseline.min";

    @I18NMessages({ @I18NMessage("{0} {1} {2} bl min") /*, @I18NMessage(locale = "de", value = "") */})
    String ALERT_BASELINE_MIN_SHORT = "alert.condition.baseline.min.short";

    @I18NMessages({ @I18NMessage("{0} {1} {2} of Baseline Maximum Value") /*, @I18NMessage(locale = "de", value = "") */})
    String ALERT_BASELINE_MAX = "alert.condition.baseline.max";

    @I18NMessages({ @I18NMessage("{0} {1} {2} bl max") /*, @I18NMessage(locale = "de", value = "") */})
    String ALERT_BASELINE_MAX_SHORT = "alert.condition.baseline.max.short";

    // Calltime Metric Foo MAX > 1.0 with calltime destination matching "*.txt"
    @I18NMessages({ @I18NMessage("Calltime Metric {0} {1} {2} {3} with calltime destination matching \"{4}\"") /*, @I18NMessage(locale = "de", value = "") */})
    String ALERT_METRIC_CALLTIME_THRESHOLD_WITH_EXPR = "alert.condition.calltime-threshold-with-expr";

    // Foo MAX > 1.0 matching "*.txt"
    @I18NMessages({ @I18NMessage("{0} {1} {2} {3} matching \"{4}\"") /*, @I18NMessage(locale = "de", value = "") */})
    String ALERT_METRIC_CALLTIME_THRESHOLD_WITH_EXPR_SHORT = "alert.condition.calltime-threshold-with-expr.short";

    // Calltime Metric Foo MAX > 1.0
    @I18NMessages({ @I18NMessage("Calltime Metric {0} {1} {2} {3}") /*, @I18NMessage(locale = "de", value = "") */})
    String ALERT_METRIC_CALLTIME_THRESHOLD = "alert.condition.calltime-threshold";

    // Foo MAX > 1.0
    @I18NMessages({ @I18NMessage("{0} {1} {2} {3}") /*, @I18NMessage(locale = "de", value = "") */})
    String ALERT_METRIC_CALLTIME_THRESHOLD_SHORT = "alert.condition.calltime-threshold.short";

    @I18NMessages({ @I18NMessage("grows") /*, @I18NMessage(locale = "de", value = "") */})
    String ALERT_METRIC_CALLTIME_CHANGE_GROWS = "alert.condition.calltime-change.grows";

    @I18NMessages({ @I18NMessage("shrinks") /*, @I18NMessage(locale = "de", value = "") */})
    String ALERT_METRIC_CALLTIME_CHANGE_SHRINKS = "alert.condition.calltime-change.shrinks";

    @I18NMessages({ @I18NMessage("changes") /*, @I18NMessage(locale = "de", value = "") */})
    String ALERT_METRIC_CALLTIME_CHANGE_CHANGES = "alert.condition.calltime-change.changes";

    // Calltime Metric Foo MAX (grows/shrinks/changes) by at least 1% with calltime destination matching "*.txt"
    @I18NMessages({ @I18NMessage("Calltime Metric {0} {1} {2} by at least {3} with calltime destination matching \"{4}\"") /*, @I18NMessage(locale = "de", value = "") */})
    String ALERT_METRIC_CALLTIME_CHANGE_WITH_EXPR = "alert.condition.calltime-change-with-expr";

    // Foo MAX (grows/shrinks/changes) by 1% matching "*.txt"
    @I18NMessages({ @I18NMessage("{0} {1} {2} by {3} matching \"{4}\"") /*, @I18NMessage(locale = "de", value = "") */})
    String ALERT_METRIC_CALLTIME_CHANGE_WITH_EXPR_SHORT = "alert.condition.calltime-change-with-expr.short";

    // Calltime Metric Foo MAX (grows/shrinks/changes) by at least 1%
    @I18NMessages({ @I18NMessage("Calltime Metric {0} {1} {2} by at least {3}") /*, @I18NMessage(locale = "de", value = "") */})
    String ALERT_METRIC_CALLTIME_CHANGE = "alert.condition.calltime-change";

    // Foo MAX (grows/shrinks/changes) by 1%
    @I18NMessages({ @I18NMessage("{0} {1} {2} by {3}") /*, @I18NMessage(locale = "de", value = "") */})
    String ALERT_METRIC_CALLTIME_CHANGE_SHORT = "alert.condition.calltime-change.short";

    // Foo Value Changed
    @I18NMessages({ @I18NMessage("{0} Value Changed"),
        @I18NMessage(locale = "de", value = "{0} Der Wert hat sich ge�ndert") })
    String ALERT_METRIC_CHANGED = "alert.condition.metric.changed";

    @I18NMessages({ @I18NMessage("{0} Val Chg"), @I18NMessage(locale = "de", value = "{0} Wert�nd.") })
    String ALERT_METRIC_CHANGED_SHORT = "alert.condition.metric.changed.short";

    @I18NMessages({ @I18NMessage("{0} Value Changed Matching Expression \"{1}\"") /*, @I18NMessage(locale = "de", value = "") */})
    String ALERT_METRIC_CHANGED_WITH_EXPR = "alert.condition.metric.changed-with-expr";

    @I18NMessages({ @I18NMessage("{0} Val Chg Matching \"{1}\"") /*, @I18NMessage(locale = "de", value = "") */})
    String ALERT_METRIC_CHANGED_WITH_EXPR_SHORT = "alert.condition.metric.changed-with-expr.short";

    @I18NMessages({ @I18NMessage("Operation [{0}] has status=[{1}]") ,
            @I18NMessage(locale = "de", value = "Operation [{0}] hat den Status [{1}]") })
    String ALERT_OPERATION = "alert.condition.op";

    @I18NMessages({ @I18NMessage("Op [{0}]={1}") /*, @I18NMessage(locale = "de", value = "") */})
    String ALERT_OPERATION_SHORT = "alert.condition.op.short";

    @I18NMessages({ @I18NMessage("Resource Configuration Changed") /*, @I18NMessage(locale = "de", value = "") */})
    String ALERT_RESOURCECONFIGCHANGE = "alert.condition.resconfigchange";

    @I18NMessages({ @I18NMessage("Res Config Chg") /*, @I18NMessage(locale = "de", value = "") */})
    String ALERT_RESOURCECONFIGCHANGE_SHORT = "alert.condition.resconfigchange.short";

    @I18NMessages({ @I18NMessage("Event With Severity [{0}]") /*, @I18NMessage(locale = "de", value = "") */})
    String ALERT_EVENT = "alert.condition.event";

    // [WARN] Event
    @I18NMessages({ @I18NMessage("[{0}] Event") /*, @I18NMessage(locale = "de", value = "") */})
    String ALERT_EVENT_SHORT = "alert.condition.event.short";

    @I18NMessages({ @I18NMessage("Event With Severity [{0}] Matching Expression \"{1}\"") /*, @I18NMessage(locale = "de", value = "") */})
    String ALERT_EVENT_WITH_EXPR = "alert.condition.event-with-expr";

    @I18NMessages({ @I18NMessage("Event With Severity [{0}] with Details Matching Expression \"{1}\" With Event Source Matching Expression \"{2}\"") /*, @I18NMessage(locale = "de", value = "") */})
    String ALERT_EVENT_WITH_EXPR_WITH_SOURCE = "alert.condition.event-with-expr-with-source";

    @I18NMessages({ @I18NMessage("[{0}] Event Matching \"{1}\"") /*, @I18NMessage(locale = "de", value = "") */})
    String ALERT_EVENT_WITH_EXPR_SHORT = "alert.condition.event-with-expr.short";

    @I18NMessages({ @I18NMessage("[{0}] Event Matching \"{1}\" with Event Source Matching \"{2}\"") /*, @I18NMessage(locale = "de", value = "") */})
    String ALERT_EVENT_WITH_EXPR_WITH_SOURCE_SHORT = "alert.condition.event-with-expr-with-source.short";

    @I18NMessages({ @I18NMessage("Event With Severity [{0}] With Event Source Matching Expression \"{1}\"") /*, @I18NMessage(locale = "de", value = "") */})
    String ALERT_EVENT_WITH_SOURCE = "alert.condition.event-with-source-expr";

    @I18NMessages({ @I18NMessage("[{0}] Event With Event Source Matching \"{1}\"") /*, @I18NMessage(locale = "de", value = "") */})
    String ALERT_EVENT_WITH_SOURCE_SHORT = "alert.condition.event-with-source-expr.short";

    @I18NMessages({ @I18NMessage("Drift Detected") /*, @I18NMessage(locale = "de", value = "") */})
    String ALERT_DRIFT = "alert.condition.drift";

    @I18NMessages({ @I18NMessage("Drift!") /*, @I18NMessage(locale = "de", value = "") */})
    String ALERT_DRIFT_SHORT = "alert.condition.drift.short";

    @I18NMessages({ @I18NMessage("Drift detected for files that match \"{0}\" and for drift definition [{1}]") /*, @I18NMessage(locale = "de", value = "") */})
    String ALERT_DRIFT_CONFIGPATHS = "alert.condition.drift.configpaths";

    @I18NMessages({ @I18NMessage("Drift matching \"{0}\", config=[{1}]") /*, @I18NMessage(locale = "de", value = "") */})
    String ALERT_DRIFT_CONFIGPATHS_SHORT = "alert.condition.drift.configpaths.short";

    @I18NMessages({ @I18NMessage("Drift detected for drift definition [{0}]") /*, @I18NMessage(locale = "de", value = "") */})
    String ALERT_DRIFT_ONLYCONFIG = "alert.condition.drift.onlyconfig";

    @I18NMessages({ @I18NMessage("Drift! config=[{0}]") /*, @I18NMessage(locale = "de", value = "") */})
    String ALERT_DRIFT_ONLYCONFIG_SHORT = "alert.condition.drift.onlyconfig.short";

    @I18NMessages({ @I18NMessage("Drift detected for files that match \"{0}\"") /*, @I18NMessage(locale = "de", value = "") */})
    String ALERT_DRIFT_ONLYPATHS = "alert.condition.drift.onlypaths";

    @I18NMessages({ @I18NMessage("Drift matching \"{0}\"") /*, @I18NMessage(locale = "de", value = "") */})
    String ALERT_DRIFT_ONLYPATHS_SHORT = "alert.condition.drift.onlypaths.short";

    // Foo Value is Between 1.0B and 2.0B, Inclusive
    @I18NMessages({ @I18NMessage("{0} Value is Between {1} and {2}, Inclusive"),
        @I18NMessage(locale = "de", value = "{0} Der Wert ist zwischen {1} und {2}, inklusiv") })
    String ALERT_RANGE_INSIDE_INCL = "alert.condition.range.in.incl";

    @I18NMessages({ @I18NMessage("{0} Between {1} - {2}, incl"),
        @I18NMessage(locale = "de", value = "{0} zwischen {1} und {2}, inkl") })
    String ALERT_RANGE_INSIDE_INCL_SHORT = "alert.condition.range.in.incl.short";

    @I18NMessages({ @I18NMessage("{0} Value is Between {1} and {2}, Exclusive"),
        @I18NMessage(locale = "de", value = "{0} Der Wert zwischen {1} und {2}, exklusiv") })
    String ALERT_RANGE_INSIDE_EXCL = "alert.condition.range.in.excl";

    @I18NMessages({ @I18NMessage("{0} Between {1} - {2}, excl"),
        @I18NMessage(locale = "de", value = "{0} zwischen {1} - {2}, exkl.") })
    String ALERT_RANGE_INSIDE_EXCL_SHORT = "alert.condition.range.in.excl.short";

    @I18NMessages({ @I18NMessage("{0} Value is Outside {1} and {2}, Inclusive"),
        @I18NMessage(locale = "de", value = "{0} Der Wert außerhalb {1} und {2}, pauschal") })
    String ALERT_RANGE_OUTSIDE_INCL = "alert.condition.range.out.incl";

    @I18NMessages({ @I18NMessage("{0} Outside {1} - {2}, incl"),
        @I18NMessage(locale = "de", value = "{0} außerhalb {1} und {2}, paus") })
    String ALERT_RANGE_OUTSIDE_INCL_SHORT = "alert.condition.range.out.incl.short";

    @I18NMessages({ @I18NMessage("{0} Value is Outside {1} and {2}, Exclusive"),
        @I18NMessage(locale = "de", value = "{0} Der Wert außerhalb {1} und {2}, exklusiv") })
    String ALERT_RANGE_OUTSIDE_EXCL = "alert.condition.range.out.excl";

    @I18NMessages({ @I18NMessage("{0} Outside {1} - {2}, excl"),
        @I18NMessage(locale = "de", value = "{0} außerhalb {1} und {2}, exkl") })
    String ALERT_RANGE_OUTSIDE_EXCL_SHORT = "alert.condition.range.out.excl.short";

    @I18NMessages({
        @I18NMessage("\\  - Condition {0}: {1}\\n\\\n" + "\\  - Date/Time: {2}\\n\\\n" + "\\  - Details: {3}\\n\\\n"),
        @I18NMessage(locale = "de", value = "  - Bedingung {0}: {1}\\n\\\n  - Datum/Uhrzeit: {2}\\n\\\n"
            + "\\  - Details: {3}\\n\\\n") })
    String ALERT_EMAIL_CONDITION_LOG_FORMAT = "alert.email.condition.log.format";

    @I18NMessages({ @I18NMessage("\\  - Cond {0}: {1}\\n\\\n" + "\\  - Time: {2}\\n\\\n" + "\\  - Det: {3}\\n\\\n"),
        @I18NMessage(locale = "de", value = "  - Bed {0}: {1}\\n\\\n  - Zeit: {2}\\n\\\n" + "\\  - Det: {3}\\n\\\n") })
    String ALERT_EMAIL_CONDITION_LOG_FORMAT_SHORT = "alert.email.condition.log.format.short";

    // Needed for the AlertManagerBeanTest
    @I18NMessages({ @I18NMessage("Cond(?:ition)?"), @I18NMessage(locale = "de", value = "Bed(?:ingung)?")})
    String ALERT_CONDITION_PATTERN = "alert.condition.pattern";
}