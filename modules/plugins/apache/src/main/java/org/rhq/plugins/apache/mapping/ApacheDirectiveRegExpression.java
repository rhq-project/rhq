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
package org.rhq.plugins.apache.mapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.rhq.augeas.node.AugeasNode;

/**
 * The httpd lens doesn't give us very detailed information about the directive
 * parameters.
 * It just supplies a list of "param" nodes each containing a value for one
 * parameter of a directive (how we wish it could be otherwise and the lens be more sophisticated).
 * <p>
 * In order to extract the useful information from these that we can use for mapping the
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
        MAPPING_TYPE.put("AllowOverride", DirectiveMapping.DIRECTIVE_PER_MAP_INDEX);
    }

    private static String WORD = "\"(?:[^\"\n]|\\\")*\"|'(?:[^'\n]|\\\')*'|[^'\" \t\n]+";
    private static String WS = "[ \t]*";
    private static final String WS_MAN = "[ \t]+";
    private static final String NUM = "[0-9]+";

    /**
     * This is the map of regular expressions for each individual directive
     * that needs "special treatment".
     * A directive can need multiple regexes to produce mappable values.
     */
    private static Map<String, Pattern[]> directiveRegex;
    static {
        directiveRegex = new HashMap<String, Pattern[]>();
        directiveRegex.put("Alias",
            new Pattern[] { Pattern.compile(WS + "(" + WORD + ")" + WS + "(" + WORD + ")" + WS) });
        directiveRegex.put("CustomLog", new Pattern[] { Pattern.compile(WS + "(" + WORD + ")" + "(?:" + WS_MAN + "("
            + WORD + ")" + ")?" + WS) });
        directiveRegex.put("ErrorDocument", new Pattern[] { Pattern.compile(WS + "(" + NUM + ")" + WS_MAN + "(" + WORD
            + ")" + WS) });
        directiveRegex.put("Options", new Pattern[] { Pattern.compile("([+-])?" + "(" + WORD + ")" + WS) });
        directiveRegex.put("Allow", new Pattern[] { Pattern.compile("from"),
            Pattern.compile("(?:" + WS_MAN + "(" + WORD + "))") });
        directiveRegex.put("Deny", new Pattern[] { Pattern.compile("from"),
            Pattern.compile("(?:" + WS_MAN + "(" + WORD + "))") });
        directiveRegex.put("Listen", new Pattern[] { Pattern
            .compile("(?:((?:\\[[a-zA-Z0-9:]+\\])|(?:[0-9\\.]+)):)?([0-9]+)(?:" + WS_MAN + "(" + WORD + "))?") });
        directiveRegex.put("ServerAlias", new Pattern[] { Pattern.compile(WS + "(" + WORD + ")") });
        directiveRegex.put("AllowOverride", new Pattern[] { Pattern
            .compile("(All)|(None)|(AuthConfig)|(FileInfo)|(Indexes)|(Limit)|(Options)") });
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
    public static List<String> getParams(AugeasNode parentNode) {
        StringBuilder value = new StringBuilder();
        String nodeName = parentNode.getLabel();
        List<String> result = new ArrayList<String>();

        List<AugeasNode> nodes = parentNode.getChildNodes();
        for (AugeasNode node : nodes) {
            value.append(node.getValue());
            value.append(" ");
        }

        if (value.length() > 0) {
            value.deleteCharAt(value.length() - 1);
        }

        if (!directiveRegex.containsKey(nodeName)) {
            result.add(value.toString());
            return result;
        }

        //each regex is applied as long as it matches something
        Pattern[] patterns = directiveRegex.get(nodeName);

        int startIndex = 0;

        while (startIndex < value.length())

            for (Pattern pattern : patterns) {
                Matcher m = pattern.matcher(value);
                while (m.find(startIndex)) {
                    for (int i = 1; i <= m.groupCount(); i++) {
                        String val = m.group(i);
                        result.add(val);
                    }
                    startIndex = m.end();
                }
            }

        return result;
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
