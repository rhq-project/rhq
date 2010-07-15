package org.rhq.enterprise.server.search.translation;

import org.rhq.enterprise.server.search.translation.antlr.RHQLComparisonOperator;
import org.rhq.enterprise.server.util.QueryUtility;

public abstract class AbstractSearchTranslator implements SearchTranslator {

    public enum ValueFilter {
        STARTS_WITH, //
        ENDS_WITH, //
        INDEX_OF, //
        EXACT_MATCH;
    }

    private String process(ValueFilter filter, String value) {
        if (filter == ValueFilter.STARTS_WITH) {
            return "'" + QueryUtility.escapeSearchParameter(value) + "%'";
        } else if (filter == ValueFilter.ENDS_WITH) {
            return "'%" + QueryUtility.escapeSearchParameter(value) + "'";
        } else if (filter == ValueFilter.INDEX_OF) {
            return "'%" + QueryUtility.escapeSearchParameter(value) + "%'";
        } else if (filter == ValueFilter.EXACT_MATCH) {
            return "'" + QueryUtility.escapeSearchParameter(value) + "'";
        } else {
            throw new IllegalArgumentException("Unsupported ValueFilter: " + filter);
        }
    }

    private String getJPQLForString(String fragment, RHQLComparisonOperator operator, String value, ValueFilter filter) {
        if (operator == RHQLComparisonOperator.EQUALS) {
            return lower(fragment) + " LIKE " + process(filter, value.toLowerCase());

        } else if (operator == RHQLComparisonOperator.EQUALS_STRICT) {
            return fragment + " LIKE " + process(filter, value);

        } else if (operator == RHQLComparisonOperator.NOT_EQUALS) {
            return lower(fragment) + " NOT LIKE " + process(filter, value.toLowerCase());

        } else if (operator == RHQLComparisonOperator.NOT_EQUALS_STRICT) {
            return fragment + " NOT LIKE " + process(filter, value);

        } else if (operator == RHQLComparisonOperator.NULL) {
            return fragment + " IS NULL";

        } else if (operator == RHQLComparisonOperator.NOT_NULL) {
            return fragment + " IS NOT NULL";

        } else {
            throw new IllegalArgumentException("Unsupported operator " + operator);
        }
    }

    protected String getJPQLForString(String fragment, RHQLComparisonOperator operator, String value) {
        int size = value.length();
        if (value.startsWith("^")) {
            if (value.endsWith("$")) {
                return getJPQLForString(fragment, operator, value.substring(1, size - 1), ValueFilter.EXACT_MATCH);
            } else {
                return getJPQLForString(fragment, operator, value.substring(1), ValueFilter.STARTS_WITH);
            }
        } else {
            if (value.endsWith("$")) {
                return getJPQLForString(fragment, operator, value.substring(0, size - 1), ValueFilter.ENDS_WITH);
            } else {
                return getJPQLForString(fragment, operator, value, ValueFilter.INDEX_OF);
            }
        }
    }

    protected String getJPQLForEnum(String fragment, RHQLComparisonOperator operator, String value,
        Class<? extends Enum<?>> enumClass, boolean useOrdinal) {
        if (operator == RHQLComparisonOperator.NULL || //
            operator == RHQLComparisonOperator.NOT_NULL) {
            return QueryUtility.escapeSearchParameter(fragment + operator.getDefaultTranslation());

        } else if (operator == RHQLComparisonOperator.EQUALS || //
            operator == RHQLComparisonOperator.EQUALS_STRICT || //
            operator == RHQLComparisonOperator.NOT_EQUALS || //
            operator == RHQLComparisonOperator.NOT_EQUALS_STRICT) {
            return QueryUtility.escapeSearchParameter(fragment + operator.getDefaultTranslation()
                + getEnum(enumClass, value, useOrdinal));

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

    protected String lower(String data) {
        return "LOWER(" + data + ")";
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
