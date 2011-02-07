package org.rhq.enterprise.gui.coregui.client.util;

/**
 * @author Ian Springer
 */
public class TypeConversionUtility {

    private TypeConversionUtility() {
    }

    public static Integer toInteger(Object object) {
        Integer integerValue;
        if (object instanceof String) {
            integerValue = Integer.valueOf((String)object);
        } else if (object instanceof Number) {
            integerValue = ((Number) object).intValue();
        } else if (object == null) {
            integerValue = null;
        } else {
            throw new IllegalArgumentException("Failed to convert " + object.getClass().getName() + " [" + object
                    + "] to an Integer.");
        }
        return integerValue;
    }

    public static Long toLong(Object object) {
        Long longValue;
        if (object instanceof String) {
            longValue = Long.valueOf((String)object);
        } else if (object instanceof Number) {
            longValue = ((Number) object).longValue();
        } else if (object == null) {
            longValue = null;
        } else {
            throw new IllegalArgumentException("Failed to convert " + object.getClass().getName() + " [" + object
                    + "] to a Long.");
        }
        return longValue;
    }

}
