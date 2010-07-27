package org.rhq.enterprise.server.search.common;

import org.rhq.enterprise.server.search.translation.antlr.RHQLComparisonOperator;
import org.rhq.enterprise.server.util.QueryUtility;

public class SearchQueryGenerationUtility {
    public enum ValueFilter {
        STARTS_WITH, //
        ENDS_WITH, //
        INDEX_OF, //
        EXACT_MATCH;
    }

    public static String getJPQLForString(String fragment, String value) {
        if (value == null) {
            value = "";
        }

        return getJPQLForString(fragment, RHQLComparisonOperator.EQUALS, value);
    }

    public static String getJPQLForString(String fragment, RHQLComparisonOperator operator, String value) {
        if (value == null) {
            value = "";
        }

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

    private static String getJPQLForString(String fragment, RHQLComparisonOperator operator, String value,
        ValueFilter filter) {
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

    private static String process(ValueFilter filter, String value) {
        if (filter == ValueFilter.STARTS_WITH) {
            return "'" + escape(value) + "%'";
        } else if (filter == ValueFilter.ENDS_WITH) {
            return "'%" + escape(value) + "'";
        } else if (filter == ValueFilter.INDEX_OF) {
            return "'%" + escape(value) + "%'";
        } else if (filter == ValueFilter.EXACT_MATCH) {
            return "'" + escape(value) + "'";
        } else {
            throw new IllegalArgumentException("Unsupported ValueFilter: " + filter);
        }
    }

    public static String lower(String data) {
        return "LOWER(" + data + ")";
    }

    public static String escape(String data) {
        return QueryUtility.escapeSearchParameter(data);
    }

}
