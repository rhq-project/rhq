package org.rhq.plugins.apache.augeas;

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

    private static Map<String, DirectiveMappingEnum> mappingType;

    static {
        mappingType = new HashMap<String, DirectiveMappingEnum>();
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

    /**
     * This is the map of regular expressions for each individual directive
     * that needs "special treatment".
     * A directive can need multiple regexes to produce mappable values.
     */
    private static Map<String, Pattern[]> directiveRegex;
    static {
        directiveRegex = new HashMap<String, Pattern[]>();
        directiveRegex.put("Alias",
            new Pattern[] { Pattern.compile(ws + "(" + word + ")" + ws + "(" + word + ")" + ws) });
        directiveRegex.put("CustomLog", new Pattern[] { Pattern.compile(ws + "(" + word + ")" + "(?:" + wsMan + "("
            + word + ")" + ")?" + ws) });
        directiveRegex.put("ErrorDocument", new Pattern[] { Pattern.compile(ws + "(" + num + ")" + wsMan + "(" + word
            + ")" + ws) });
        directiveRegex.put("Options", new Pattern[] { Pattern.compile("([+-])?" + "(" + word + ")" + ws) });
        directiveRegex.put("Allow", new Pattern[] { Pattern.compile("from"),
            Pattern.compile("(?:" + wsMan + "(" + word + "))") });
        directiveRegex.put("Deny", new Pattern[] { Pattern.compile("from"),
            Pattern.compile("(?:" + wsMan + "(" + word + "))") });
        directiveRegex.put("Listen", new Pattern[] { Pattern
            .compile("(?:((?:\\[[a-zA-Z0-9:]+\\])|(?:[0-9\\.]+)):)?([0-9]+)(?:" + wsMan + "(" + word + "))?") });
        directiveRegex.put("ServerAlias", new Pattern[] { Pattern.compile(ws + "(" + word + ")") });
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
    public static DirectiveMappingEnum getMappingType(String directiveName) {
        DirectiveMappingEnum map = mappingType.get(directiveName);
        if (map == null)
            map = DirectiveMappingEnum.SimpleProp;

        return map;
    }
}
