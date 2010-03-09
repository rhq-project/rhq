package org.rhq.enterprise.server.search.translation;

import org.rhq.enterprise.server.search.translation.antlr.RHQLComparisonOperator;

public abstract class AbstractSearchTranslator implements SearchTranslator {

    protected String getJPQLForString(String fragment, RHQLComparisonOperator operator, String value) {
        if (operator == RHQLComparisonOperator.EQUALS) {
            return lower(fragment) + " LIKE '%" + value.toLowerCase() + "%'";

        } else if (operator == RHQLComparisonOperator.EQUALS_STRICT) {
            return fragment + " LIKE '%" + value + "%'";

        } else if (operator == RHQLComparisonOperator.NOT_EQUALS) {
            return lower(fragment) + " LIKE '%" + value.toLowerCase() + "%'";

        } else if (operator == RHQLComparisonOperator.NOT_EQUALS_STRICT) {
            return fragment + " LIKE '%" + value + "%'";

        } else if (operator == RHQLComparisonOperator.NULL) {
            return fragment + " IS NULL";

        } else if (operator == RHQLComparisonOperator.NOT_NULL) {
            return fragment + " IS NOT NULL";

        } else {
            throw new IllegalArgumentException("Unsupported operator " + operator);
        }
    }

    protected String getJPQLForEnum(String fragment, RHQLComparisonOperator operator, String value,
        Class<? extends Enum<?>> enumClass, boolean useOrdinal) {
        if (operator == RHQLComparisonOperator.EQUALS) {
            return fragment + " = " + getEnum(enumClass, value, useOrdinal);

        } else if (operator == RHQLComparisonOperator.EQUALS_STRICT) {
            return fragment + " = " + getEnum(enumClass, value, useOrdinal);

        } else if (operator == RHQLComparisonOperator.NOT_EQUALS) {
            return fragment + " != " + getEnum(enumClass, value, useOrdinal);

        } else if (operator == RHQLComparisonOperator.NOT_EQUALS_STRICT) {
            return fragment + " != " + getEnum(enumClass, value, useOrdinal);

        } else if (operator == RHQLComparisonOperator.NULL) {
            return fragment + " IS NULL";

        } else if (operator == RHQLComparisonOperator.NOT_NULL) {
            return fragment + " IS NOT NULL";

        } else {
            throw new IllegalArgumentException("Unsupported operator " + operator);
        }
    }

    protected String getEnum(Class<? extends Enum<?>> enumClass, String value, boolean useOrdinal) {
        for (Enum<?> nextEnum : enumClass.getEnumConstants()) {
            if (nextEnum.name().toLowerCase().equals(value.toLowerCase())) {
                if (useOrdinal) {
                    return String.valueOf(nextEnum.ordinal());
                } else {
                    return nextEnum.name();
                }
            }
        }
        throw new IllegalArgumentException("No enum of type '" + enumClass.getSimpleName() + "' with name matching '"
            + value + "'");
    }

    protected String lower(String data) {
        return "LOWER(" + data + ")";
    }

    protected String quote(String data) {
        return "'" + data + "'";
    }

}
