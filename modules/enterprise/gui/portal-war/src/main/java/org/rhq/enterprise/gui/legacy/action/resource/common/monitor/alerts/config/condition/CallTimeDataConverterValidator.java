package org.rhq.enterprise.gui.legacy.action.resource.common.monitor.alerts.config.condition;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

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
import org.rhq.core.server.MeasurementConverter;
import org.rhq.core.server.MeasurementParser;
import org.rhq.core.util.NumberUtil;
import org.rhq.enterprise.gui.legacy.action.resource.common.monitor.alerts.config.ConditionBean;
import org.rhq.enterprise.server.measurement.MeasurementDefinitionManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class CallTimeDataConverterValidator implements ConditionBeanConverterValidator {

    public static final String DOUBLE_REGEX = "[0-9,.]+";

    public static final String TYPE_ABS = "absolute";
    public static final String TYPE_CHG = "changed";

    private MeasurementDefinitionManagerLocal definitionManager;

    public CallTimeDataConverterValidator() {
        definitionManager = LookupUtil.getMeasurementDefinitionManager();
    }

    public void exportProperties(Subject subject, ConditionBean fromBean, AlertCondition toCondition) {
        MeasurementDefinition definition = definitionManager.getMeasurementDefinition(subject, fromBean
            .getCallTimeMetricId());
        toCondition.setMeasurementDefinition(definition);

        if (fromBean.getThresholdType().equals(TYPE_ABS)) {
            MeasurementNumericValueAndUnits valueAndUnits = MeasurementParser.parse(fromBean.getCtAbsoluteValue(),
                definition.getUnits());

            toCondition.setThreshold(valueAndUnits.getValue());
            toCondition.setCategory(AlertConditionCategory.THRESHOLD);
            toCondition.setOption(fromBean.getCalltimeAbsOption());
            toCondition.setComparator(fromBean.getCalltimeComparator());
            toCondition.setName(fromBean.getCalltimeAbsPattern());

        } else if (fromBean.getThresholdType().equals(TYPE_CHG)) {
            MeasurementNumericValueAndUnits percentage = MeasurementParser.parse(fromBean.getCtPercentage(),
                MeasurementUnits.PERCENTAGE);

            toCondition.setThreshold(percentage.getValue());
            toCondition.setCategory(AlertConditionCategory.CHANGE);
            toCondition.setOption(fromBean.getCalltimeChgOption());
            toCondition.setName(fromBean.getCalltimeChgPattern());
            toCondition.setComparator(fromBean.getCalltimeChgOp());
        }
    }

    public void importProperties(Subject subject, AlertCondition fromCondition, ConditionBean toBean) {
        // shared measurement processing
        MeasurementDefinition definition = fromCondition.getMeasurementDefinition();
        toBean.setCallTimeMetricName(definition.getDisplayName());
        toBean.setCallTimeMetricId(definition.getId());

        toBean.setTrigger(getTriggerName());

        if (fromCondition.getCategory() == AlertConditionCategory.THRESHOLD) {
            toBean.setThresholdType(TYPE_ABS);
            toBean.setCalltimeAbsOption(fromCondition.getOption());
            toBean.setCalltimeAbsPattern(fromCondition.getName());

            try {
                // this is coming from the backing store, so
                String formattedValue = MeasurementConverter.format(fromCondition.getThreshold(),
                    definition.getUnits(), true);
                toBean.setCtAbsoluteValue(formattedValue);
            } catch (MeasurementConversionException mce) {
                toBean.setCtAbsoluteValue("Conversion Error");
            }

            toBean.setCalltimeComparator(fromCondition.getComparator());
            toBean.setPercentageComparator(null);
        } else if (fromCondition.getCategory() == AlertConditionCategory.CHANGE) {
            toBean.setThresholdType(TYPE_CHG);
            toBean.setCalltimeChgOption(fromCondition.getOption());
            toBean.setCalltimeChgPattern(fromCondition.getName());
            toBean.setCalltimeChgOp(fromCondition.getComparator());

            Double threshold = MeasurementUnits.scaleUp(fromCondition.getThreshold(), MeasurementUnits.PERCENTAGE);
            toBean.setCtPercentage(MeasurementConverter.format(threshold, MeasurementUnits.NONE, true));

            toBean.setCalltimeComparator(null);

        }

    }

    public boolean validate(ConditionBean bean, ActionErrors errors, int index) {
        if (bean.getCallTimeMetricId() <= 0) {
            // user didn't select a CallTime metric
            ActionMessage err = new ActionMessage("alert.config.error.NoMetricSelected");
            errors.add("condition[" + index + "].callTimeMetricId", err);
            return false;
        }

        if (bean.getThresholdType().equals(TYPE_ABS)) {
            if (!GenericValidator.matchRegexp(bean.getCtAbsoluteValue(), DOUBLE_REGEX)) {
                ActionMessage err = new ActionMessage("errors.double", "AbsoluteValue");
                errors.add("condition[" + index + "].ctAbsoluteValue", err);
                return false;
            } else // double
            {
                // do nothing
            }
            if (bean.getCalltimeAbsPattern() != null) {
                try {
                    Pattern p = Pattern.compile(bean.getCalltimeAbsPattern());
                } catch (PatternSyntaxException e) {
                    ActionMessage err = new ActionMessage("alert.config.error.CalltimePatternError");
                    errors.add("condition[" + index + "].calltimeAbsPattern", err);
                }
            }
        } else if (bean.getThresholdType().equals(TYPE_CHG)) // percentage
        {
            if (!GenericValidator.matchRegexp(bean.getCtPercentage(), DOUBLE_REGEX)) {
                ActionMessage err = new ActionMessage("errors.double", "Percentage");
                errors.add("condition[" + index + "].ctPercentage", err);
                return false;
            } else // double
            {
                // 0-100 range
                double percentage = NumberUtil.stringAsNumber(bean.getCtPercentage()).doubleValue();
                if (!GenericValidator.isInRange(percentage, 0d, 1000d)) {
                    ActionMessage err = new ActionMessage("errors.range", String.valueOf(percentage), String
                        .valueOf(0d), String.valueOf(1000d));
                    errors.add("condition[" + index + "].ctPercentage", err);
                    return false;
                }
            }
            if (bean.getCalltimeChgPattern() != null) {
                try {
                    Pattern p = Pattern.compile(bean.getCalltimeChgPattern());
                } catch (PatternSyntaxException e) {
                    ActionMessage err = new ActionMessage("alert.config.error.CalltimePatternError");
                    errors.add("condition[" + index + "].calltimeChgPattern", err);
                }
            }
        }

        return true;
    }

    public String getTriggerName() {
        return "onCallTime";
    }
}
