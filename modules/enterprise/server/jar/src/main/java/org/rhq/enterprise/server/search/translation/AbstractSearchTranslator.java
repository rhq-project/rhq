package org.rhq.enterprise.server.search.translation;

import org.rhq.enterprise.server.search.translation.antlr.RHQLComparisonOperator;

public abstract class AbstractSearchTranslator implements SearchTranslator {

    protected String getJPQLForEnum(String fragment, RHQLComparisonOperator operator, String value,
        Class<? extends Enum<?>> enumClass, boolean useOrdinal) {
        if (operator == RHQLComparisonOperator.NULL || //
            operator == RHQLComparisonOperator.NOT_NULL) {
            return fragment + operator.getDefaultTranslation();

        } else if (operator == RHQLComparisonOperator.EQUALS || //
            operator == RHQLComparisonOperator.EQUALS_STRICT || //
            operator == RHQLComparisonOperator.NOT_EQUALS || //
            operator == RHQLComparisonOperator.NOT_EQUALS_STRICT) {
            return fragment + operator.getDefaultTranslation() + getEnum(enumClass, value, useOrdinal);

        } else {
            throw new IllegalArgumentException("Unsupported operator " + operator);
        }
    }

    private String getEnum(Class<? extends Enum<?>> enumClass, String value, boolean useOrdinal) {
        value = value.toLowerCase();
        for (Enum<?> nextEnum : enumClass.getEnumConstants()) {
            if (nextEnum.name().toLowerCase().equals(value)) {
                if (useOrdinal) {
                    return String.valueOf(nextEnum.ordinal());
                } else {
                    return quote(nextEnum.name());
                }
            }
        }
        throw new IllegalArgumentException("No enum of type '" + enumClass.getSimpleName() + "' with name matching '"
            + value + "'");
    }

    protected String quote(String data) {
        return "'" + data + "'";
    }

    protected final String addFragmentIfParameterNotValue(String fragment, String parameter, String value) {
        if (!parameter.equalsIgnoreCase(value)) {
            return fragment;
        }
        return "";
    }

}
