package org.rhq.enterprise.server.search.translation.antlr;

public enum RHQLComparisonOperator {
    EQUALS(" = "), //
    EQUALS_STRICT(" = "), //
    NOT_EQUALS(" != "), //
    NOT_EQUALS_STRICT(" != "), //
    LESS_THAN(" < "), //
    GREATER_THAN(" > "), //
    NULL(" IS NULL "), //
    NOT_NULL(" IS NOT NULL ");

    private String defaultTranslation;

    private RHQLComparisonOperator(String defaultTranslation) {
        this.defaultTranslation = defaultTranslation;
    }

    public String getDefaultTranslation() {
        return defaultTranslation;
    }
}
