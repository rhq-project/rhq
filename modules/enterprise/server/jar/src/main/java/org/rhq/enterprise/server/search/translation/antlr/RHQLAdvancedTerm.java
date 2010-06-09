package org.rhq.enterprise.server.search.translation.antlr;

public class RHQLAdvancedTerm implements RHQLTerm {
    private final String lineage;
    private final String path;
    private final String param;

    private final RHQLComparisonOperator operator;

    private final String value;

    public RHQLAdvancedTerm(String lineage, String path, String param, RHQLComparisonOperator operator, String value) {
        this.lineage = lineage;
        this.path = path;
        this.param = param;
        this.operator = operator;
        this.value = value;
    }

    public String getLineage() {
        return lineage;
    }

    public String getPath() {
        return path;
    }

    public String getParam() {
        return param;
    }

    public RHQLComparisonOperator getOperator() {
        return operator;
    }

    public String getValue() {
        return value;
    }
}