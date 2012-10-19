package org.rhq.plugins.apache.parser;

public interface ApacheParser {
    
    public void addDirective(ApacheDirective directive) throws Exception;
    public void startNestedDirective(ApacheDirective directive);
    public void endNestedDirective(ApacheDirective directive);    
    public void startParsing();
    public void endParsing();
}
