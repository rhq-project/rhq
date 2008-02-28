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
package org.rhq.enterprise.gui.legacy.action.resource.common.monitor.alerts.config;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionErrors;

import org.rhq.core.domain.alert.AlertCondition;
import org.rhq.core.domain.auth.Subject;
import org.rhq.enterprise.gui.legacy.action.resource.common.monitor.alerts.config.condition.ConverterValidatorManager;

/**
 * Bean that holds alert condition info.
 */
public final class ConditionBean {
    private Integer id; // nullable
    private String trigger;
    private String thresholdType;
    private String absoluteComparator;
    private String percentageComparator;
    private Integer metricId;
    private String metricName;
    private String absoluteValue;
    private String percentage;
    private String baselineOption;
    private String controlAction;
    private String controlActionStatus;
    private String customProperty;
    private String eventSeverity;
    private String eventDetails;
    private String availability;
    private String availabilityStatus;

    private Integer traitId;
    private String traitName;

    public ConditionBean() {
        ConverterValidatorManager.setDefaults(this);
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTrigger() {
        return trigger;
    }

    public void setTrigger(String trigger) {
        this.trigger = trigger;
    }

    public String getThresholdType() {
        return thresholdType;
    }

    public void setThresholdType(String thresholdType) {
        this.thresholdType = thresholdType;
    }

    public String getAbsoluteComparator() {
        return absoluteComparator;
    }

    public void setAbsoluteComparator(String absoluteComparator) {
        this.absoluteComparator = absoluteComparator;
    }

    public String getPercentageComparator() {
        return percentageComparator;
    }

    public void setPercentageComparator(String percentageComparator) {
        this.percentageComparator = percentageComparator;
    }

    public Integer getMetricId() {
        return metricId;
    }

    public void setMetricId(Integer metricId) {
        this.metricId = metricId;
    }

    public String getMetricName() {
        return metricName;
    }

    public void setMetricName(String metricName) {
        this.metricName = metricName;
    }

    public String getPercentage() {
        return percentage;
    }

    public void setPercentage(String percentage) {
        this.percentage = percentage;
    }

    public String getAbsoluteValue() {
        return absoluteValue;
    }

    public void setAbsoluteValue(String absoluteValue) {
        this.absoluteValue = absoluteValue;
    }

    public String getBaselineOption() {
        return baselineOption;
    }

    public void setBaselineOption(String baselineOption) {
        this.baselineOption = baselineOption;
    }

    public String getControlAction() {
        return controlAction;
    }

    public void setControlAction(String controlAction) {
        this.controlAction = controlAction;
    }

    public String getControlActionStatus() {
        return controlActionStatus;
    }

    public void setControlActionStatus(String controlActionStatus) {
        this.controlActionStatus = controlActionStatus;
    }

    public String getCustomProperty() {
        return customProperty;
    }

    public void setCustomProperty(String customProperty) {
        this.customProperty = customProperty;
    }

    public String getEventSeverity() {
        return eventSeverity;
    }

    public void setEventSeverity(String eventSeverity) {
        this.eventSeverity = eventSeverity;
    }

    public String getEventDetails() {
        return eventDetails;
    }

    public void setEventDetails(String logMatch) {
        this.eventDetails = logMatch;
    }

    public String getAvailability() {
        return availability;
    }

    public void setAvailability(String availability) {
        this.availability = availability;
    }

    public String getAvailabilityStatus() {
        return availabilityStatus;
    }

    public void setAvailabilityStatus(String availabilityStatus) {
        this.availabilityStatus = availabilityStatus;
    }

    public Integer getTraitId() {
        return traitId;
    }

    public void setTraitId(Integer traitId) {
        this.traitId = traitId;
    }

    public String getTraitName() {
        return traitName;
    }

    public void setTraitName(String traitName) {
        this.traitName = traitName;
    }

    public void importProperties(AlertCondition cond, boolean isTypeAlert, Subject subject) throws Exception {
        ConverterValidatorManager.importProperties(subject, cond, this);
    }

    public AlertCondition exportProperties(HttpServletRequest request, Subject subject, boolean typeAlert)
        throws Exception {
        return ConverterValidatorManager.exportProperties(subject, this);
    }

    public boolean validate(HttpServletRequest request, ActionErrors errors, int index) {
        return ConverterValidatorManager.validate(this, errors, index);
    }
}