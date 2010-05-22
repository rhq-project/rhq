package org.rhq.enterprise.server.search.translation.antlr;

public class RHQLSimpleTerm implements RHQLTerm {
    private final String value;

    public RHQLSimpleTerm(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
