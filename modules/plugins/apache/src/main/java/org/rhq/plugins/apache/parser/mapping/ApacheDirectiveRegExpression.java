/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.plugins.apache.parser.mapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.rhq.plugins.apache.parser.ApacheDirective;

/**
 * The httpd lens doesn't give us very detailed information about the directive
 * parameters.
 * It just supplies a list of "param" nodes each containing a value for one
 * parameter of a directive (how we wish it could be otherwise and the lens be more sophisticated).
 * <p>
 * In order to extract the useful information from this that we can use for mapping these
 * params to RHQ configuration properties, we define a series of regular expressions
 * that are designed to work with a space concatenated list of params supplied by Augeas.
 * <p>
 * In these regular expressions, each capturing group corresponds (in the exact order) to
 * the properties defined in the resource configurations.
 * 
 * @author Filip Drabek
 * @author Lukas Krejci
 */
public class ApacheDirectiveRegExpression {

    private static final Map<String, DirectiveMapping> MAPPING_TYPE;

    static {
        MAPPING_TYPE = new HashMap<String, DirectiveMapping>();
        MAPPING_TYPE.put("Alias", DirectiveMapping.DIRECTIVE_PER_MAP);
        MAPPING_TYPE.put("Listen", DirectiveMapping.DIRECTIVE_PER_MAP);
        MAPPING_TYPE.put("ErrorDocument", DirectiveMapping.DIRECTIVE_PER_MAP);
        MAPPING_TYPE.put("Options", DirectiveMapping.PARAM_PER_MAP);
        MAPPING_TYPE.put("ServerAlias", DirectiveMapping.PARAM_PER_MAP);
        MAPPING_TYPE.put("Allow", DirectiveMapping.PARAM_PER_MAP);
        MAPPING_TYPE.put("Deny", DirectiveMapping.PARAM_PER_MAP);
        MAPPING_TYPE.put("CustomLog", DirectiveMapping.DIRECTIVE_PER_MAP_INDEX);
        MAPPING_TYPE.put("AllowOverride", DirectiveMapping.PARAM_PER_MAP);
        MAPPING_TYPE.put("DirectoryIndex", DirectiveMapping.PARAM_PER_MAP);
        MAPPING_TYPE.put("NameVirtualHost", DirectiveMapping.DIRECTIVE_PER_MAP_INDEX);
        MAPPING_TYPE.put("IfModules",DirectiveMapping.POSITION_PROPERTY);
    }

    public static final String WORD = "\"(?:[^\"\n]|\\\")*\"|'(?:[^'\n]|\\\')*'|[^'\" \t\n]+";
    public static final String WS = "[ \t]*";
    public static final String WS_MAN = "[ \t]+";
    public static final String NUM = "[0-9]+";

    /**
     * This is the map of regular expressions for each individual directive
     * that needs "special treatment".
     * A directive can need multiple regexes to produce mappable values.
     */
    private static final Map<String, Pattern[]> DIRECTIVE_REGEX;
    static {
        DIRECTIVE_REGEX = new HashMap<String, Pattern[]>();
        DIRECTIVE_REGEX.put("Alias",
            new Pattern[] { Pattern.compile(WS + "(" + WORD + ")" + WS + "(" + WORD + ")" + WS) });
        DIRECTIVE_REGEX.put("CustomLog", new Pattern[] { Pattern.compile(WS + "(" + WORD + ")" + "(?:" + WS_MAN + "("
            + WORD + ")" + ")?" + WS) });
        DIRECTIVE_REGEX.put("ErrorDocument", new Pattern[] { Pattern.compile(WS + "(" + NUM + ")" + WS_MAN + "(" + WORD
            + ")" + WS) });
        DIRECTIVE_REGEX.put("Options", new Pattern[] {Pattern.compile(WS+"(Add|Remove|Set)"+WS+"([a-zA-Z]+)") });
        DIRECTIVE_REGEX.put("Allow", new Pattern[] { Pattern.compile("from"),
            Pattern.compile("(?:" + WS_MAN + "(" + WORD + "))") });
        DIRECTIVE_REGEX.put("Deny", new Pattern[] { Pattern.compile("from"),
            Pattern.compile("(?:" + WS_MAN + "(" + WORD + "))") });
        DIRECTIVE_REGEX.put("Listen", new Pattern[] { Pattern
            .compile("(?:((?:\\[[a-zA-Z0-9:]+\\])|(?:[0-9\\.]+)):)?([0-9]+)(?:" + WS_MAN + "(" + WORD + "))?") });
        DIRECTIVE_REGEX.put("ServerAlias", new Pattern[] { Pattern.compile(WS + "(" + WORD + ")") });
        DIRECTIVE_REGEX.put("AllowOverride", new Pattern[] { Pattern
            .compile(WS+"(All|None|AuthConfig|FileInfo|Indexes|Limit|Options)") });
        DIRECTIVE_REGEX.put("DirectoryIndex", new Pattern[] { Pattern.compile(WS+ "(" + WORD + ")" + WS) });
        DIRECTIVE_REGEX.put("NameVirtualHost", new Pattern[] { Pattern.compile(WS+ "(" + WORD + ")"+ WS) });
    }

    
    private static final Map<String, Pattern[]> DIRECTIVEREGEX_TO_AUGEAS;
    static {
            DIRECTIVEREGEX_TO_AUGEAS = new HashMap<String, Pattern[]>();
            DIRECTIVEREGEX_TO_AUGEAS.put("Options", new Pattern[] { Pattern.compile(WS+"([+-]?"+WS+"[a-zA-Z]+)"+WS) });
            DIRECTIVEREGEX_TO_AUGEAS.put("Allow", new Pattern[] { Pattern.compile("(?:" + WS_MAN + "(" + WORD + "))") });
            DIRECTIVEREGEX_TO_AUGEAS.put("Deny", new Pattern[] { Pattern.compile("(?:" + WS_MAN + "(" + WORD + "))") });
            DIRECTIVEREGEX_TO_AUGEAS.put("ServerAlias", new Pattern[] { Pattern.compile(WS_MAN + "(" + WORD + ")") });
            DIRECTIVEREGEX_TO_AUGEAS.put("DirectoryIndex", new Pattern[] { Pattern.compile(WS_MAN+ "(" + WORD + ")") });
            DIRECTIVEREGEX_TO_AUGEAS.put("Alias",
                new Pattern[] { Pattern.compile(WS_MAN + "(" + WORD + ")" + WS_MAN + "(" + WORD + ")" + WS) });
            DIRECTIVEREGEX_TO_AUGEAS.put("CustomLog", new Pattern[] { Pattern.compile(WS_MAN + "(" + WORD + ")" + "(?:" + WS_MAN + "("
                    + WORD + ")" + ")?") });
            DIRECTIVEREGEX_TO_AUGEAS.put("AllowOverride", new Pattern[] { Pattern
                    .compile(WS+"(All|None|AuthConfig|FileInfo|Indexes|Limit|Options)") });
            DIRECTIVEREGEX_TO_AUGEAS.put("Listen", new Pattern[] { Pattern.compile(WS+ "(" + WORD + ")")});
            DIRECTIVEREGEX_TO_AUGEAS.put("NameVirtualHost", new Pattern[] { Pattern.compile(WS+ "(" + WORD +")" + WS) });
            DIRECTIVEREGEX_TO_AUGEAS.put("ErrorDocument", new Pattern[] { Pattern.compile(WS_MAN + "(" + NUM + ")" + WS_MAN + "(" + WORD
                + ")") });
            
    }
    /**
     * Parses the parameters of the supplied node and returns a list
     * of strings, each containing a value for a simple property
     * corresponding to that parameter.
     * <p>
     * A null value means that the parameters for given property 
     * was not defined in the configuration file.
     * 
     * @param parentNode the node containing the "param" sub nodes to parse
     * @return the list of mappable parameters
     */
    public static List<String> getParams(ApacheDirective parentNode) {
        StringBuilder value = new StringBuilder();
        String nodeName = parentNode.getName();
        List<String> result = new ArrayList<String>();
        for (String str : parentNode.getValues())
           value.append(str+" ");
        if  (value.length()>0)
            value.deleteCharAt(value.length()-1);
        
        if (!DIRECTIVE_REGEX.containsKey(nodeName)) {
            result.add(value.toString());
            return result;
        }

        value = SpecificParams.prepareForConfiguration(nodeName, value);
        //each regex is applied as long as it matches something
        Pattern[] patterns = DIRECTIVE_REGEX.get(nodeName);

        int startIndex = 0;
        boolean updated = true;
        while (updated & startIndex < value.length())
            updated = false;
            for (Pattern pattern : patterns) {
                Matcher m = pattern.matcher(value);
                while (m.find(startIndex)) {
                    for (int i = 1; i <= m.groupCount(); i++) {
                        String val = m.group(i);
                        result.add(val);
                    }
                    updated = true;
                    startIndex = m.end();
                }
            }

        return result;
    }
    
    public static List<String> createParams(String params,String name){
            List<String> nodeParams = new ArrayList<String>();
            
        if (!DIRECTIVEREGEX_TO_AUGEAS.containsKey(name)) {
                nodeParams.add(params);
            return nodeParams;
        }
        
        if (name.equals("Allow") | name.equals("Deny")){
                nodeParams.add("from");
        }
        //each regex is applied as long as it matches something
        Pattern[] patterns = DIRECTIVEREGEX_TO_AUGEAS.get(name);

        int startIndex = 0;
        boolean updated =true;         
        
        params = SpecificParams.prepareForAugeas(name, params);
        
        while (updated  & startIndex < params.length())
            updated = false;
            for (Pattern pattern : patterns) {
                Matcher m = pattern.matcher(params);
                while (m.find(startIndex)) {
                    for (int i = 1; i <= m.groupCount(); i++) {
                        String val = m.group(i);
                        if (val!=null)
                           nodeParams.add(val);
                    }
                    updated = true;
                    startIndex = m.end();
                }
            }
 
        if (name.equals("Options")){
        	int i=0;
        	for (String param : nodeParams){
        		param = param.replaceAll(" ", "");
        		nodeParams.set(i, param);
        		i++;
        	}
        }
        
        return nodeParams;
    }

    /**
     * The properties in the resource configuration share a couple of common
     * patterns how they map to the augeas tree. This method returns the corresponding
     * mapping type for a directive.
     * 
     * @param directiveName the directive to find out the mapping type of.
     * @return the mapping type that can be used to perform the mapping to the property.
     */
    public static DirectiveMapping getMappingType(String directiveName) {
    	DirectiveMapping map = MAPPING_TYPE.get(directiveName);
        if (map == null)
            map = DirectiveMapping.SIMPLE_PROP;

        return map;
    }
    
}
