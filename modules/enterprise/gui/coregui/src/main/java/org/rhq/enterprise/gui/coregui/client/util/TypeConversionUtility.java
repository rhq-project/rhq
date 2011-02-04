package org.rhq.enterprise.gui.coregui.client.util;

/**
 * @author Ian Springer
 */
public class TypeConversionUtility {

    private TypeConversionUtility() {
    }

    public static Integer toInteger(Object object) {
        Integer integer;
        if (object instanceof String) {
            integer = Integer.valueOf((String)object);
        } else if (object instanceof Number) {
            integer = ((Number) object).intValue();
        } else if (object == null) {
            integer = null;
        } else {
            throw new IllegalArgumentException("Failed to convert " + object.getClass().getName() + " [" + object
                    + "] to an Integer.");
        }
        return integer;
    }

}
