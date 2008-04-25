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

import org.apache.commons.validator.GenericValidator;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMessage;

import org.rhq.core.domain.alert.AlertCondition;
import org.rhq.core.domain.alert.AlertConditionCategory;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementUnits;
import org.rhq.core.domain.measurement.composite.MeasurementNumericValueAndUnits;
import org.rhq.core.domain.measurement.util.MeasurementConversionException;
import org.rhq.core.domain.measurement.util.MeasurementConverter;
import org.rhq.core.util.NumberUtil;
import org.rhq.enterprise.gui.legacy.action.resource.common.monitor.alerts.config.ConditionBean;
import org.rhq.enterprise.server.measurement.MeasurementDefinitionManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

class MeasurementConverterValidator implements ConditionBeanConverterValidator {
    public static final String DOUBLE_REGEX = "[0-9,.]+";
    public static final String TYPE_ABS = "absolute";
    public static final String TYPE_PERC = "percentage";
    public static final String TYPE_CHG = "changed";

    private MeasurementDefinitionManagerLocal definitionManager;

    public MeasurementConverterValidator() {
        definitionManager = LookupUtil.getMeasurementDefinitionManager();
    }

    @SuppressWarnings("deprecation")
    public void exportProperties(Subject subject, ConditionBean fromBean, AlertCondition toCondition) {
        MeasurementDefinition definition = definitionManager.getMeasurementDefinitionById(subject, fromBean
            .getMetricId());
        toCondition.setMeasurementDefinition(definition);

        if (fromBean.getThresholdType().equals(TYPE_ABS)) {
            MeasurementNumericValueAndUnits valueAndUnits = MeasurementConverter.parse(fromBean.getAbsoluteValue(),
                definition.getUnits());

            toCondition.setCategory(AlertConditionCategory.THRESHOLD);
            toCondition.setThreshold(valueAndUnits.getValue());
            toCondition.setComparator(fromBean.getAbsoluteComparator());

        } else if (fromBean.getThresholdType().equals(TYPE_PERC)) {
            MeasurementNumericValueAndUnits threshold = MeasurementConverter.parse(fromBean.getPercentage(),
                MeasurementUnits.PERCENTAGE);

            toCondition.setCategory(AlertConditionCategory.BASELINE);
            toCondition.setThreshold(threshold.getValue());
            toCondition.setComparator(fromBean.getPercentageComparator());
            toCondition.setOption(fromBean.getBaselineOption());

        } else {
            toCondition.setCategory(AlertConditionCategory.CHANGE);
        }

        toCondition.setName(fromBean.getMetricName());
    }

    public void importProperties(Subject subject, AlertCondition fromCondition, ConditionBean toBean) {
        // shared measurement processing
        toBean.setMetricName(fromCondition.getName());
        toBean.setMetricId(fromCondition.getMeasurementDefinition().getId());

        toBean.setTrigger(getTriggerName());

        // category-specific processing
        if (fromCondition.getCategory() == AlertConditionCategory.THRESHOLD) {
            toBean.setThresholdType(TYPE_ABS);

            MeasurementDefinition definition = fromCondition.getMeasurementDefinition();
            toBean.setMetricId(definition.getId());

            try {
                // this is coming from the backing store, so
                String formattedValue = MeasurementConverter.format(fromCondition.getThreshold(),
                    definition.getUnits(), true);
                toBean.setAbsoluteValue(formattedValue);
            } catch (MeasurementConversionException mce) {
                toBean.setAbsoluteValue("Conversion Error");
            }

            toBean.setAbsoluteComparator(fromCondition.getComparator());
            toBean.setPercentageComparator(null);

        } else if (fromCondition.getCategory() == AlertConditionCategory.BASELINE) {
            toBean.setThresholdType(TYPE_PERC);
            toBean.setBaselineOption(fromCondition.getOption());

            // The percent sign is fixed in the GUI, so scale up manually and format without units
            Double threshold = MeasurementUnits.scaleUp(fromCondition.getThreshold(), MeasurementUnits.PERCENTAGE);
            toBean.setPercentage(MeasurementConverter.format(threshold, MeasurementUnits.NONE, true));

            toBean.setAbsoluteComparator(null);
            toBean.setPercentageComparator(fromCondition.getComparator());
        } else if (fromCondition.getCategory() == AlertConditionCategory.CHANGE) {
            toBean.setThresholdType(TYPE_CHG);

            toBean.setAbsoluteComparator(null);
            toBean.setPercentageComparator(null);
        }
    }

    public boolean validate(ConditionBean bean, ActionErrors errors, int index) {
        if (bean.getMetricId() <= 0) {
            // user didn't select a metric
            ActionMessage err = new ActionMessage("alert.config.error.NoMetricSelected");
            errors.add("condition[" + index + "].metricId", err);
            return false;
        }

        if (bean.getThresholdType().equals(TYPE_ABS)) {
            if (!GenericValidator.matchRegexp(bean.getAbsoluteValue(), DOUBLE_REGEX)) {
                ActionMessage err = new ActionMessage("errors.double", "AbsoluteValue");
                errors.add("condition[" + index + "].absoluteValue", err);
                return false;
            } else // double
            {
                // do nothing
            }
        } else if (bean.getThresholdType().equals(TYPE_PERC)) // percentage
        {
            if (!GenericValidator.matchRegexp(bean.getPercentage(), DOUBLE_REGEX)) {
                ActionMessage err = new ActionMessage("errors.double", "Percentage");
                errors.add("condition[" + index + "].percentage", err);
                return false;
            } else // double
            {
                // 0-100 range
                double percentage = NumberUtil.stringAsNumber(bean.getPercentage()).doubleValue();
                if (!GenericValidator.isInRange(percentage, 0d, 1000d)) {
                    ActionMessage err = new ActionMessage("errors.range", String.valueOf(percentage), String
                        .valueOf(0d), String.valueOf(1000d));
                    errors.add("condition[" + index + "].percentage", err);
                    return false;
                }
            }

            if ((null == bean.getBaselineOption()) || (bean.getBaselineOption().length() == 0)) {
                ActionMessage err = new ActionMessage("alert.config.error.NoBaselineOptionSelected");
                errors.add("condition[" + index + "].baselineOption", err);
                return false;
            } else {
                // some sort of error handling here?
            }
        }

        return true;
    }

    public String getTriggerName() {
        return "onMeasurement";
    }
}