package org.rhq.plugins.apache.augeas;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.rhq.augeas.node.AugeasNode;

public class ApacheDirectiveRegExpression {

	private static Map<String,DirectiveMappingEnum> mappingType;
	
	static {
		mappingType = new HashMap<String,DirectiveMappingEnum>();
	    mappingType.put("Alias", DirectiveMappingEnum.DirectivePerMap);
	    mappingType.put("Listen", DirectiveMappingEnum.DirectivePerMap);
	    mappingType.put("ErrorDocument", DirectiveMappingEnum.DirectivePerMap);
	    mappingType.put("Options", DirectiveMappingEnum.ParamPerMap);
	    mappingType.put("ServerAlias", DirectiveMappingEnum.ParamPerMap);
	    mappingType.put("Allow", DirectiveMappingEnum.ParamPerMap);
	    mappingType.put("Deny", DirectiveMappingEnum.ParamPerMap);
	    mappingType.put("CustomLog", DirectiveMappingEnum.DirectivePerMapIndex);
	    mappingType.put("AllowOverride", DirectiveMappingEnum.DirectivePerMapIndex);
	}
	
	private static String word = "\"(?:[^\"\n]|\\\")*\"|'(?:[^'\n]|\\\')*'|[^'\" \t\n]+";
	private static String ws = "[ \t]*";
	private static final String wsMan = "[ \t]+";
	private static final String num = "[0-9]+";
	
	private static Map<String,String []> directiveRegex;
	static {
	     directiveRegex = new HashMap<String,String []>();
	     directiveRegex.put("Alias", new String[]{ ws+"("+ word +")"+ws+ "("+ word+")"+ws });
	     directiveRegex.put("CustomLog",  new String[]{ws+"("+ word +")"+"(?:" + wsMan+ "("+ word+")"+")?"+ws});
	     directiveRegex.put("ErrorDocument", new String[]{ws+"("+ num +")"+wsMan+"("+ word+")"+ws});
	     directiveRegex.put("Options",new String[]{"([+-])?"+"("+word+")"+ ws});	
	     directiveRegex.put("Allow", new String[]{"from", "(?:" + wsMan + "(" + word + "))"});
	     directiveRegex.put("Deny", new String[]{ "from", "(?:" + wsMan + "(" + word + "))"});
	     directiveRegex.put("Listen",  new String[]{"(?:((?:\\[[a-zA-Z0-9:]+\\])|(?:[0-9\\.]+)):)?([0-9]+)(?:" + wsMan + "(" + word + "))?"});
	     directiveRegex.put("ServerAlias", new String[]{ws+"("+word+")"});
	     //strange as it is, the order of the groups in the regex below has to be the same as the order of 
	     //corresponding simple props in the resource config
	     directiveRegex.put("AllowOverride", new String[]{"(All)|(None)|(AuthConfig)|(FileInfo)|(Indexes)|(Limit)|(Options)"});
	}
	
    public static List<String> getParams(AugeasNode parentNode){
    	StringBuilder value= new StringBuilder();
    	String nodeName = parentNode.getLabel();
    	List<String> result = new ArrayList<String>();
    	
    	List<AugeasNode> nodes = parentNode.getChildNodes();
    	for (AugeasNode node : nodes){
    		value.append(node.getValue());
    		value.append(" ");
    	}

    	if (value.length()>0){
    		value.deleteCharAt(value.length()-1);
    	}
    	
    	if (!directiveRegex.containsKey(nodeName)){
    		result.add(value.toString());
    		return result;
    	}
    	
    	List<Pattern> patterns = new ArrayList<Pattern>();
    	
    	for (String regexName : directiveRegex.get(nodeName)){
    		Pattern pat = Pattern.compile(regexName);
    		patterns.add(pat);
    	}
    	
    	int startIndex= 0;
    	
     	while (startIndex<value.length())
    		
        	for (Pattern pattern : patterns){
        		Matcher m = pattern.matcher(value); 
        		while (m.find(startIndex)){
        			for (int i=1;i<=m.groupCount();i++)
        			{
        				String val = m.group(i);   			  
        			    result.add(val);    			  
        			}
        			  startIndex = m.end();
        		}
        	}
    	
    	return result;
    }
    
    public static DirectiveMappingEnum getMappingType(String directiveName){
    	DirectiveMappingEnum map =  mappingType.get(directiveName);
    	if (map == null)
    		map = DirectiveMappingEnum.SimpleProp;
    	
    	return map;
    }
}
