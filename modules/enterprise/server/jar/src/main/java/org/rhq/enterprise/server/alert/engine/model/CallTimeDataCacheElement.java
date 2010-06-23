package org.rhq.enterprise.server.alert.engine.model;

import java.text.DateFormat;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.rhq.core.domain.measurement.calltime.CallTimeDataValue;

/**
 *
 * @author fbrueseke
 */
public class CallTimeDataCacheElement extends AbstractCacheElement<CallTimeDataValue> {
    public enum CallTimeElementValue {
        MAX, MIN, AVG, COUNT;
    }

    private enum CalltimeChangeOp {
        LO //Shrinks
        , CH //Changes
        , HI;//Grows
    }

    private static CallTimeDataValue dummy = new CallTimeDataValue();

    private Double compareValue = null;
    private CalltimeChangeOp comparator;

    private ConcurrentHashMap<String, CallTimeDataValue> previous = new ConcurrentHashMap<String, CallTimeDataValue>();
    private Pattern callDestPattern = null;

    private String fixPattern(String regex) {
        boolean sw = regex.startsWith(".*");
        boolean ew = regex.endsWith(".*");
        return (!sw ? ".*" : "") + regex + (!ew ? ".*" : "");
    }

    public CallTimeDataCacheElement(AlertConditionOperator operator, CallTimeElementValue whichValue,
        String comparator, Double value, int conditionId, String callDestPattern) {
        super(operator, whichValue, dummy, conditionId);

        if (value == null)
            throw new InvalidCacheElementException("Invalid Cache Element: " + "condition with id=" + conditionId + " "
                + "and operator='" + operator.toString() + "' " + "requires a non-null value");

        if (whichValue == null)
            throw new InvalidCacheElementException("operator '" + operator.toString() + "'"
                + " requires an operator option; it can not be null");

        compareValue = value;
        if (callDestPattern != null)
            this.callDestPattern = Pattern.compile(fixPattern(callDestPattern));
        else
            this.callDestPattern = null;

        if (comparator != null)
            this.comparator = CalltimeChangeOp.valueOf(comparator);
    }

    @Override
    public AlertConditionOperator.Type getOperatorSupportsType(AlertConditionOperator operator) {
        if ((operator == AlertConditionOperator.GREATER_THAN) || (operator == AlertConditionOperator.EQUALS)
            || (operator == AlertConditionOperator.LESS_THAN) || (operator == AlertConditionOperator.CHANGES)) {
            return operator.getDefaultType();
        }

        return AlertConditionOperator.Type.NONE;
    }

    @Override
    public boolean matches(CallTimeDataValue providedValue, Object... extras) {

        if (compareValue == null || compareValue.isNaN() || compareValue.isInfinite()
            || this.alertConditionOperatorOption == null)
            return false;

        Double provided = getCallTimeElementValue(providedValue);
        if (provided == null || provided.isInfinite() || provided.isNaN())
            return false;

        String key = "";
        if (extras != null && extras[0] != null)
            key = (String) extras[0];
        if (callDestPattern != null && !callDestPattern.matcher(key).matches())
            return false;

        switch (alertConditionOperator) {
        case EQUALS:
            return (compareValue.compareTo(provided) == 0);

        case GREATER_THAN: // threshold < measurement
            return (compareValue.compareTo(provided) < 0);

        case LESS_THAN: //threshold > measurement
            return (compareValue.compareTo(provided) > 0);

        case CHANGES:
            // I assume that CallTimeDataValue objects are delivered to this method in chronological order!
            if (!previous.containsKey(key)) {
                previous.put(key, providedValue);
                return false;
            }

            CallTimeDataValue previousElem = previous.get(key);
            double compare = getCallTimeElementValue(previousElem);
            boolean result = computeChangeResult(provided, compare, comparator);
            if (log.isTraceEnabled())
                log.trace("Changes at least " + compareValue + "% [" + result + "]; current:" + providedValue
                    + "; previous:" + previousElem);

            previous.put(key, providedValue);
            return result;
        }
        return false;
    }

    private boolean computeChangeResult(Double provided, double compare, CalltimeChangeOp op) {
        switch (op) {
        case LO:
            return (provided < compare - compareValue * compare);
        case CH:
            return (provided < compare - compareValue * compare || provided > compare + compareValue * compare);
        case HI:
            return (provided > compare + compareValue * compare);
        default:
            return false;
        }
    }

    private Double getCallTimeElementValue(CallTimeDataValue struct) {
        switch ((CallTimeElementValue) alertConditionOperatorOption) {
        case MIN:
            return struct.getMinimum();
        case MAX:
            return struct.getMaximum();
        case AVG:
            return struct.getTotal() / struct.getCount();
        case COUNT:
            return (double) struct.getCount();
        default:
            return null;
        }
    }

    @Override
    public String convertValueToString(CallTimeDataValue value) {
        return "[From:"
            + DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM).format(value.getBeginTime())
            + ", To:" + DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM).format(value.getEndTime())
            + ", " + alertConditionOperatorOption + ":" + getCallTimeElementValue(value) + "]";
    }
}
