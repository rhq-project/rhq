package org.rhq.plugins.apache.augeas;

import java.util.List;

import org.rhq.augeas.node.AugeasNode;

public class ApacheDirectiveRegExpression {

    public List<String> getParams(AugeasNode parentNodde){
    	return null;
    }
    
    public DirectiveMappingEnum getMappingType(String directiveName){
    	return DirectiveMappingEnum.DirectivePerMap;
    }
}
