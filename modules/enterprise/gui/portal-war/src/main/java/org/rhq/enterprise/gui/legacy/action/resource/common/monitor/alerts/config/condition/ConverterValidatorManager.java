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
package org.rhq.enterprise.gui.legacy.action.resource.common.monitor.alerts.config.condition;

import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMessage;

import org.rhq.core.domain.alert.AlertCondition;
import org.rhq.core.domain.alert.AlertConditionCategory;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.enterprise.gui.legacy.action.resource.common.monitor.alerts.config.ConditionBean;

public class ConverterValidatorManager {
    private static AvailabilityConverterValidator availabilityConverter;
    private static EventsConverterValidator eventsConverter;
    private static MeasurementConverterValidator measurementConverter;
    private static CallTimeDataConverterValidator calltimeConverter;
    private static OperationConverterValidator operationConverter;
    private static TraitConverterValidator traitConverter;
    private static ResourceConfigurationConverterValidator resourceConfigurationConverter;

    static {
        availabilityConverter = new AvailabilityConverterValidator();
        eventsConverter = new EventsConverterValidator();
        measurementConverter = new MeasurementConverterValidator();
        calltimeConverter = new CallTimeDataConverterValidator();
        operationConverter = new OperationConverterValidator();
        traitConverter = new TraitConverterValidator();
        resourceConfigurationConverter = new ResourceConfigurationConverterValidator();
    }

    public static AlertCondition exportProperties(Subject subject, ConditionBean fromBean) {
        AlertCondition toCondition = new AlertCondition();

        if (fromBean.getTrigger().equals(measurementConverter.getTriggerName())) {
            measurementConverter.exportProperties(subject, fromBean, toCondition);
        } else if (fromBean.getTrigger().equals(traitConverter.getTriggerName())) {
            traitConverter.exportProperties(subject, fromBean, toCondition);
        } else if (fromBean.getTrigger().equals(calltimeConverter.getTriggerName())) {
            calltimeConverter.exportProperties(subject, fromBean, toCondition);
        } else if (fromBean.getTrigger().equals(eventsConverter.getTriggerName())) {
            eventsConverter.exportProperties(subject, fromBean, toCondition);
        } else if (fromBean.getTrigger().equals(availabilityConverter.getTriggerName())) {
            availabilityConverter.exportProperties(subject, fromBean, toCondition);
        } else if (fromBean.getTrigger().equals(operationConverter.getTriggerName())) {
            operationConverter.exportProperties(subject, fromBean, toCondition);
        } else if (fromBean.getTrigger().equals(resourceConfigurationConverter.getTriggerName())) {
            resourceConfigurationConverter.exportProperties(subject, fromBean, toCondition);
        } else {
            throw new IllegalStateException(ConverterValidatorManager.class.getSimpleName()
                + " does not exporting for the trigger: " + fromBean.getTrigger());
        }

        return toCondition;
    }

    public static void importProperties(Subject subject, AlertCondition fromCondition, ConditionBean toBean) {
        AlertConditionCategory category = fromCondition.getCategory();

        if ((category == AlertConditionCategory.THRESHOLD) || (category == AlertConditionCategory.BASELINE)
            || (category == AlertConditionCategory.CHANGE)) {
            DataType measDataType = null;
            if (fromCondition.getMeasurementDefinition() != null)
                measDataType = fromCondition.getMeasurementDefinition().getDataType();
            if (DataType.CALLTIME == measDataType)
                calltimeConverter.importProperties(subject, fromCondition, toBean);
            else
                measurementConverter.importProperties(subject, fromCondition, toBean);
        } else if (category == AlertConditionCategory.TRAIT) {
            traitConverter.importProperties(subject, fromCondition, toBean);
        } else if (category == AlertConditionCategory.EVENT) {
            eventsConverter.importProperties(subject, fromCondition, toBean);
        } else if (category == AlertConditionCategory.AVAILABILITY) {
            availabilityConverter.importProperties(subject, fromCondition, toBean);
        } else if (category == AlertConditionCategory.CONTROL) {
            operationConverter.importProperties(subject, fromCondition, toBean);
        } else if (category == AlertConditionCategory.RESOURCE_CONFIG) {
            resourceConfigurationConverter.importProperties(subject, fromCondition, toBean);
        } else {
            throw new IllegalStateException(ConverterValidatorManager.class.getSimpleName()
                + " does not importing for the AlertConditionCategory: " + category);
        }
    }

    public static boolean validate(ConditionBean bean, ActionErrors errors, int index) {
        if (bean.getTrigger() == null) {
            ActionMessage err = new ActionMessage("alert.config.error.NoTriggerTypeSelected");
            errors.add("condition[" + index + "].trigger", err);
            return false;
        } else if (bean.getTrigger().equals(measurementConverter.getTriggerName())) {
            return measurementConverter.validate(bean, errors, index);
        } else if (bean.getTrigger().equals(calltimeConverter.getTriggerName())) {
            return calltimeConverter.validate(bean, errors, index);
        } else if (bean.getTrigger().equals(traitConverter.getTriggerName())) {
            return traitConverter.validate(bean, errors, index);
        } else if (bean.getTrigger().equals(eventsConverter.getTriggerName())) {
            return eventsConverter.validate(bean, errors, index);
        } else if (bean.getTrigger().equals(availabilityConverter.getTriggerName())) {
            return availabilityConverter.validate(bean, errors, index);
        } else if (bean.getTrigger().equals(operationConverter.getTriggerName())) {
            return operationConverter.validate(bean, errors, index);
        } else if (bean.getTrigger().equals(resourceConfigurationConverter.getTriggerName())) {
            return resourceConfigurationConverter.validate(bean, errors, index);
        } else {
            throw new IllegalStateException(ConverterValidatorManager.class.getSimpleName()
                + " does not validation for the trigger: " + bean.getTrigger());
        }
    }

    public static void setDefaults(ConditionBean bean) {
        bean.setThresholdType(MeasurementConverterValidator.TYPE_ABS);
    }
}