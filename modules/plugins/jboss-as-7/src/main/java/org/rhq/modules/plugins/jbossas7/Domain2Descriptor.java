/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.modules.plugins.jbossas7;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.rhq.modules.plugins.jbossas7.json.Address;
import org.rhq.modules.plugins.jbossas7.json.ComplexResult;
import org.rhq.modules.plugins.jbossas7.json.Operation;

/**
 * Generate properties, metrics and operation templates for the plugin
 * descriptor from a domain dump (server can run in domain or standalone mode).
 *
 * @author Heiko W. Rupp
 */
@SuppressWarnings("unchecked")
public class Domain2Descriptor {

    //Need to hard code until JIRA addressed: https://issues.jboss.org/browse/AS7-4384 
    private String[] properties = { "cpu", "mem", "heap", "sessions", "requests", "send-traffic", "receive-traffic",
        "busyness", "connection-pool" };
    private D2DMode mode = null;

    public static void main(String[] args) throws Exception {

        if (args.length < 1) {
            usage();
            System.exit(1);
        }

        Domain2Descriptor d2d = new Domain2Descriptor();
        d2d.run(args);
    }

    private void run(String[] args) {

        //process and populate command line args passed in and determine
        //operation modes.
        String user = null;
        String pass = null;

        int pos = 0;
        boolean optionFound = false;

        String arg;
        do {
            arg = args[pos];
            if (arg.startsWith("-")) {
                if (arg.equals("-m")) {
                    mode = D2DMode.METRICS;
                } else if (arg.equals("-p")) {
                    mode = D2DMode.PROPERTIES;
                } else if (arg.equals("-o")) {
                    mode = D2DMode.OPERATION;
                } else if (arg.equals("-r")) {
                    mode = D2DMode.RECURSIVE;
                } else if (arg.startsWith("-U")) {
                    String tmp = arg.substring(2);
                    if (!tmp.contains(":")) {
                        usage();
                    }
                    user = tmp.substring(0, tmp.indexOf(":"));
                    pass = tmp.substring(tmp.indexOf(":") + 1);
                } else {
                    usage();
                    return;
                }
                pos++;
                optionFound = true;
            } else {
                optionFound = false;
            }
        } while (optionFound);

        String path = arg;
        // pwd command

        //spinder Mar-29-2012: if additional child type info passed in then load it. What does this look like?
        String childType = null;
        if (args.length > pos + 1) {
            childType = args[pos + 1];
        }

        //create connection
        ASConnection conn = new ASConnection("localhost", 9990, user, pass);

        Address address = new Address(path);

        //create request to get metadata type information
        Operation op = new Operation("read-resource-description", address);
        //recurse down the tree.
        op.addAdditionalProperty("recursive", "true");

        //additionally request operation metadata
        if (mode == D2DMode.OPERATION) {
            op.addAdditionalProperty("operations", true);
        }
        //additionally request metric metadata
        if (mode == D2DMode.METRICS) {
            op.addAdditionalProperty("include-runtime", true);
        }

        ComplexResult res = conn.executeComplex(op);
        if (res == null) {
            System.err.println("Got no result");
            return;
        }
        if (!res.isSuccess()) {
            System.err.println("Failure: " + res.getFailureDescription());
            return;
        }

        //load json object hierarchy of response
        Map<String, Object> resMap = res.getResult();
        String what;
        if (mode == D2DMode.OPERATION) {
            what = "operations";
        } else {
            what = "attributes";
        }

        //Determine which attributes to focus on.
        Map<String, Object> attributesMap = null;

        //when will childtype is actually passed then...
        if (childType != null) {

            Map childMap = (Map) resMap.get("children");
            Map<String, Object> typeMap = (Map<String, Object>) childMap.get(childType);
            if (typeMap == null) {
                System.err.println("No child with type '" + childType + "' found");
                return;
            }
            Map descriptionMap = (Map) typeMap.get("model-description");
            if (descriptionMap == null) {
                System.err.println("No model description found");
                return;
            }
            Map starMap = (Map) descriptionMap.get("*");
            if (starMap != null) {
                attributesMap = (Map<String, Object>) starMap.get(what);
            } else {//when no *map is provided check for 'classic'
                Map classicMap = (Map) descriptionMap.get("classic");
                attributesMap = (Map<String, Object>) classicMap.get(what);
            }//spinder: What about 'jsapi'? This occurs on some nodes.
        } else {//no child type passed in just load typical map
            attributesMap = (Map<String, Object>) resMap.get(what);
        }

        if (mode == D2DMode.OPERATION) {
            //populate operations(each special map type) and sort them for ordered listing
            Set<String> strings = attributesMap.keySet();
            String[] keys = strings.toArray(new String[strings.size()]);
            Arrays.sort(keys);

            for (String key : keys) {
                //exclude typical 'read-' and 'write-attribute' operations typical to all types.
                if (key.startsWith("read-attribute")) {
                    continue;
                }
                if (key.startsWith("read-children")) {
                    continue;
                }
                if (key.startsWith("read-operation")) {
                    continue;
                }
                if (key.startsWith("read-resource")) {
                    continue;
                }
                if (key.equals("write-attribute")) {
                    continue;
                }
                //exclude a few more shared operations: whoami, undefine-attribute
                if (key.equals("whoami")) {
                    continue;
                }
                if (key.equals("undefine-attribute")) {
                    continue;
                }

                //for each custom operation found, retrieve child hierarchy and pass into
                Map<String, Object> value = (Map<String, Object>) attributesMap.get(key);
                createOperation(key, value);
            }
        } else if (mode == D2DMode.RECURSIVE) {// list the child nodes and properties
            StringBuilder tree = new StringBuilder(path + " -> \n");
            System.out.print(tree);
            listPropertiesAndChildren(3, resMap);
        } else {
            createProperties(mode, attributesMap, 0);
        }
    }

    /** Assume parent metadata type passed in with i)description ii)attributes iii)operations iv)children
     * 
     * @param indent
     * @param childMap
     */
    private void listPropertiesAndChildren(int indent, Map<String, Object> metaDataNode) {
        if (metaDataNode == null) {
            return;
        }

        //retrieve the attributes for the node.
        Map<String, Object> cAttributeMap = (Map<String, Object>) metaDataNode.get("attributes");
        if (cAttributeMap != null) {
            //retrieve keys and sort.
            String[] ckeys = cAttributeMap.keySet().toArray(new String[cAttributeMap.size()]);
            Arrays.sort(ckeys);
            for (String ckey : ckeys) {//list each of the attributes.
                StringBuilder sbc = new StringBuilder();
                doIndent(indent + 2, sbc);
                sbc.append("- " + ckey);
                System.out.println(sbc);
                }
        }

        //now check for children
        Object childrenType = ((Map<String, Object>) metaDataNode).get("children");
        if (childrenType != null) {
            Map<String, Object> childrenMap = (Map<String, Object>) childrenType;
            String[] childKeys = childrenMap.keySet().toArray(new String[childrenMap.size()]);
            Arrays.sort(childKeys);
            for (String ckey : childKeys) {
                StringBuilder sb2 = new StringBuilder();
                doIndent(indent + 2, sb2);
                sb2.append("[" + ckey + "]");
                System.out.println(sb2);
                //recurse
                Map<String, Object> child = (Map<String, Object>) childrenMap.get(ckey);
                Map<String, Object> retrievedType = locateMetaDataNodeFromMap(child);
                listPropertiesAndChildren(indent + 4, retrievedType);
                }
        }
    }

    private Map<String, Object> locateMetaDataNodeFromMap(Map<String, Object> child) {
        Map<String, Object> retrieved = null;
        if (child != null) {
            //model description
            Object modelDescription = child.get("model-description");
            if (modelDescription != null) {
                //*map
                Object starMap = ((Map<String, Object>) modelDescription).get("*");
                if (starMap != null) {
                    retrieved = (Map<String, Object>) starMap;
                } else {//check for classic
                    Object classic = ((Map<String, Object>) modelDescription).get("classic");
                    retrieved = (Map<String, Object>) classic;
                }//spinder: What about 'jsapi'? This occurs on some nodes.
            }
        }
        return retrieved;
    }

    private void createProperties(D2DMode mode, Map<String, Object> attributesMap, int indent) {
        if (attributesMap == null) {
            return;
        }

        String[] keys = attributesMap.keySet().toArray(new String[attributesMap.size()]);
        Arrays.sort(keys);

        for (String key : keys) {
            Object entry = attributesMap.get(key);
            Map<String, Object> props = (Map<String, Object>) entry;

            Type ptype = getTypeFromProps(props);
            if (ptype == Type.OBJECT && mode != D2DMode.METRICS) {
                StringBuilder requiredStatus = new StringBuilder();
                Object required = props.get("required");
                if (required != null && (Boolean) required) {
                    requiredStatus.append(" required=\"true\"");
                } else {
                    requiredStatus.append(" required=\"false\"");
                }
                System.out.println("<c:map-property name=\"" + key + "\"" + requiredStatus + " description=\""
                    + props.get("description") + "\" >");

                Map<String, Object> attributesMap1 = (Map<String, Object>) props.get("attributes");
                Map<String, Object> valueTypes = (Map<String, Object>) props.get("value-type");

                if (attributesMap1 != null) {
                    createProperties(mode, attributesMap1, indent + 4);
                } else if (valueTypes != null) {
                    for (Map.Entry<String, Object> myEntry : valueTypes.entrySet()) {
                        createSimpleProp(indent + 4, myEntry);
                    }
                } else {
                    for (Map.Entry<String, Object> emapEntry : props.entrySet()) {
                        String emapKey = emapEntry.getKey();
                        if (emapKey.equals("type") || emapKey.equals("description") || emapKey.equals("required")) {
                            continue;
                        }

                        if (emapEntry.getValue() instanceof Map) {
                            Map<String, Object> emapEntryValue = (Map<String, Object>) emapEntry.getValue();
                            Type ts = getTypeFromProps(emapEntryValue);
                            StringBuilder sb = generateProperty(indent, emapEntryValue, ts, emapEntry.getKey(),
                                getAccessType(emapEntryValue));
                            System.out.println(sb.toString());
                        } else {
                            System.out.println(emapEntry.getValue());
                        }

                    }
                }

                System.out.println("</c:map-property>");

                continue;

            }

            if (ptype == Type.LIST && mode != D2DMode.METRICS) {

                StringBuilder sb = new StringBuilder("<c:list-property name=\"");
                sb.append(key);
                sb.append("\"");
                //include required status on plugin entry
                Object required = props.get("required");
                if (required != null && (Boolean) required) {
                    sb.append(" required=\"true\"");
                } else {
                    sb.append(" required=\"false\"");
                }

                String description = (String) props.get("description");
                appendDescription(sb, description, null);

                sb.append(" >\n");
                if (!props.containsKey("attributes")) {
                    sb.append("    <c:simple-property name=\"").append(key).append("\" />\n");
                } else {
                    doIndent(indent, sb);
                    sb.append("<c:map-property name=\"").append(key).append("\">\n");
                    System.out.println(sb.toString());
                    createProperties(mode, (Map<String, Object>) props.get("attributes"), indent + 4);
                    sb = new StringBuilder();
                    doIndent(indent, sb);
                    sb.append("</c:map-property>\n");
                }
                sb.append("</c:list-property>");

                System.out.println(sb.toString());

                continue;
            }

            String accessType = getAccessType(props);
            if (mode == D2DMode.METRICS) {
                if (ptype == Type.OBJECT) {
                    HashMap<String, Object> myMap = (HashMap<String, Object>) props.get("value-type");
                    for (Map.Entry<String, Object> myEntry : myMap.entrySet()) {
                        if (myEntry.getValue() instanceof String && myEntry.getValue().equals("STRING")) {
                            createMetricEntry(indent, props, key + ":" + myEntry.getKey(), getTypeFromProps(myMap));
                        } else if (myEntry.getValue() instanceof Map<?, ?>) {
                            createMetricEntry(indent, (Map<String, Object>) myEntry.getValue(),
                                key + ":" + myEntry.getKey(), getTypeFromProps(myMap));
                        }
                    }
                } else {
                    if (!accessType.equals("metric")) {
                        continue;
                    }
                    createMetricEntry(indent, props, key, ptype);
                }

            } else { // configuration
                if (accessType.equals("metric")) {
                    continue;
                }

                StringBuilder sb = generateProperty(indent, props, ptype, key, accessType);

                System.out.println(sb.toString());
            }
        }
    }

    private void createMetricEntry(int indent, Map<String, Object> props, String entryName, Type ptype) {
        StringBuilder sb = new StringBuilder();
        doIndent(indent, sb);
        sb.append("<metric property=\"");
        sb.append(entryName).append('"');
        if (ptype == Type.STRING) {
            sb.append(" dataType=\"trait\"");
        }

        String description = (String) props.get("description");
        appendDescription(sb, description, null);
        sb.append("/>");
        System.out.println(sb.toString());
    }

    private void createSimpleProp(int indent, Map.Entry<String, Object> emapEntry) {
        StringBuilder sb;

        if (emapEntry.getValue() instanceof Map) {
            Map<String, Object> emapEntryValue = (Map<String, Object>) emapEntry.getValue();
            sb = generateProperty(indent, emapEntryValue, getTypeFromProps(emapEntryValue), emapEntry.getKey(),
                getAccessType(emapEntryValue));
        } else {
            sb = new StringBuilder();
            doIndent(indent, sb);
            sb.append(emapEntry.getValue().toString());
        }
        System.out.println(sb.toString());
    }

    /** Assumes custom operation for AS7 node.
     * 
     * @param name of custom operation.
     * @param operationMap Json node representation of operation details as Map<String,Object>.
     */
    private void createOperation(String name, Map<String, Object> operationMap) {
        if ((name == null) && (operationMap == null)) {
            return;
        }

        //container for flexible string concatenation and each operation
        StringBuilder builder = new StringBuilder("<operation name=\"");

        builder.append(name).append('"');

        //description attribute
        String description = (String) operationMap.get("description");
        appendDescription(builder, description, null);
        //close xml tag
        builder.append(">\n");

        //detect operation parameters if present.
        Map<String, Object> reqMap = (Map<String, Object>) operationMap.get("request-properties");
        if (reqMap != null && !reqMap.isEmpty()) {//if present build parameters segment for plugin descriptor.
            builder.append("  <parameters>\n");
            generatePropertiesForMap(builder, reqMap);
            builder.append("  </parameters>\n");

        }
        Map replyMap = (Map) operationMap.get("reply-properties");
        builder.append("  <results>\n");
        if (replyMap != null && !replyMap.isEmpty()) {
            generatePropertiesForMap(builder, replyMap);
        } else {
            builder.append("     <c:simple-property name=\"operationResult\" description=\"" + description + "\" />\n");
        }
        builder.append("  </results>\n");

        builder.append("</operation>\n");
        System.out.println(builder.toString());
    }

    /** Builds 'description' attribute for an xml node.
     * 
     * @param builder 
     * @param description
     * @param defaultValueText
     */
    private void appendDescription(StringBuilder builder, String description, String defaultValueText) {
        if (description != null && !description.isEmpty()) {
            //wrap onto a new line
            if (builder.length() > 120) {
                builder.append("\n        ");
            }
            builder.append(" description=\"");

            //trim period off of descriptions for consistency.
            if (defaultValueText != null) {
                if (description.charAt(description.length() - 1) != '.') {
                    description += ".";
                }
                defaultValueText = escapeHtmlCharacters(defaultValueText + "");
                description = description + " " + defaultValueText;
            }

            //replace problematic strings with correct escaped xml references.
            description = escapeHtmlCharacters(description);

            builder.append(description);

            builder.append('"');
        }
    }

    /** Escape all known problematic html characters returned to prevent
     *  xml or html parse issues.
     *  
     *  Currently includes a few popular and encountered escape characters. 
     *  We may want to add them all later.  
     * 
     * @param description
     * @return
     */
    private String escapeHtmlCharacters(String description) {
        description = description.replace("<", "&lt;");
        description = description.replace(">", "&gt;");
        description = description.replace("\"", "\'");
        description = description.replace("'", "&apos;");
        //add a few more related to regex
        description = description.replace("%", "&#37;");
        description = description.replace(":", "&#58;");
        description = description.replace("[", "&#91;");
        description = description.replace("]", "&#93;");
        description = description.replace("^", "&#94;");
        description = description.replace("$", "&#36;");
        description = description.replace("?", "&#63;");
        description = description.replace("+", "&#43;");
        description = description.replace("-", "&#45;");
        //TODO: spinder should we add the other escape elements as well?
        return description;
    }

    private void generatePropertiesForMap(StringBuilder builder, Map<String, Object> map) {

        for (Map.Entry<String, Object> entry : map.entrySet()) {

            Object o = entry.getValue();
            if (o instanceof Map) {// regular json type
                Map<String, Object> entryValue = (Map<String, Object>) o;
                String entryKey = entry.getKey();

                Type type = getTypeFromProps(entryValue);
                builder.append(generateProperty(4, entryValue, type, entryKey, null));
                builder.append('\n');
            } else {//do we list this as a comment because it's an as7 invalid type?
                doIndent(4, builder);
                builder.append("<!--").append(entry.getKey()).append("..").append(entry.getValue().toString())
                    .append("-->\n");
            }
        }
    }

    private String getAccessType(Map<String, Object> props) {
        String accessType = (String) props.get("access-type");
        if (accessType == null) {
            accessType = "read-only"; // default of as7
        }
        return accessType;
    }

    private StringBuilder generateProperty(int indent, Map<String, Object> props, Type type, String entryName,
        String accessType) {

        boolean expressionsAllowed = false;
        Boolean tmp = (Boolean) props.get("expressions-allowed");
        if (tmp != null && tmp) {
            expressionsAllowed = true;
        }

        StringBuilder sb = new StringBuilder();
        doIndent(indent, sb);
        sb.append("<c:simple-property name=\"");
        sb.append(entryName);
        if (expressionsAllowed && type.isNumeric()) {
            sb.append(":expr");
        }
        sb.append('"');

        Object required = props.get("required");
        if (required != null && (Boolean) required) {
            sb.append(" required=\"true\"");
        } else {
            sb.append(" required=\"false\"");
        }

        sb.append(" type=\"");
        if (expressionsAllowed && type.isNumeric()) {
            sb.append("string");
        } else if (type.rhqName.equalsIgnoreCase("-option-list-")
            || ((mode == D2DMode.OPERATION) && type.rhqName.equalsIgnoreCase("-object-"))) {
            sb.append("string");
        } else {
            if (type.rhqName.equalsIgnoreCase("-object-") || type.rhqName.equalsIgnoreCase("-list-")) {
                sb.append("string");
            } else {
                sb.append(type.rhqName);
            }
        }
        sb.append("\"");
        sb.append(" readOnly=\"");
        if (accessType != null && accessType.equals("read-only")) {
            sb.append("true");
        } else {
            sb.append("false");
        }
        sb.append('"');

        Object defVal = props.get("default");
        String defaultValueDescription = null;
        if (defVal != null) {
            sb.append(" defaultValue=\"").append(escapeHtmlCharacters(defVal + "")).append('\"');
            defaultValueDescription = "The default value is " + escapeHtmlCharacters(defVal + "") + ".";
        }

        String description = (String) props.get("description");
        appendDescription(sb, description, defaultValueDescription);
        //Detect whether type PROPERTY and insert known supported properties before close.
        if (type.rhqName.equalsIgnoreCase("-option-list-")) {//if default provided then set it and close tag
            sb.append(generateOptionList(indent, properties, properties[7]));
        } else if (((mode == D2DMode.OPERATION) || (mode == D2DMode.PROPERTIES))
            && ((type.rhqName.equalsIgnoreCase("-object-")) || (type.rhqName.equalsIgnoreCase("string")))) {
            //detect map type if present an build map instead
            if ((props.get("allowed") == null) && (props.get("value-type") != null)) {
                //<c:map-property name="filter" description="Defines a simple filter type." >
                StringBuilder mb = new StringBuilder();
                doIndent(indent, mb);
                mb.append("<c:map-property name=\"");
                mb.append(entryName);
                mb.append('"');
                if (required != null && (Boolean) required) {
                    mb.append(" required=\"true\"");
                } else {
                    mb.append(" required=\"false\"");
                }
                appendDescription(mb, description, defaultValueDescription);
                mb.append(">\n");
                //iterate over map children
                Map<String, Object> mapType = (Map<String, Object>) props.get("value-type");
                if (mapType != null) {
                    for (Map.Entry<String, Object> entry : mapType.entrySet()) {
                        Object o = entry.getValue();
                        if (o instanceof String && o.equals("STRING")) {
                            mb.append(generateProperty(indent + 4, mapType, getTypeFromProps(mapType), entry.getKey(),
                                null));
                        } else if (o instanceof Map<?, ?>) {
                            Map<String, Object> entryValue = (Map<String, Object>) o;
                            String entryKey = entry.getKey();
                            Type childType = getTypeFromProps(entryValue);
                            mb.append(generateProperty(indent + 4, entryValue, childType, entryKey, null));
                            mb.append("\n");
                        }
                    }
                    doIndent(indent, mb);
                    mb.append("</c:map-property>");
                    return mb;
                }
            } else {//contents of allowed defines the dropdown values.
                sb.append(useAvailableOptionsList(indent, props, null));
            }
        } else { //no default provided close tag.
            sb.append("/>");
        }
        return sb;
    }

    /** Assumes that -object- is a json type where allowable values are defined
     *  in the 'allowed' child. Generates the matching <c:property-options> entries.
     * 
     * @param indent
     * @param props
     * @param defaultSelection
     * @return
     */
    private String useAvailableOptionsList(int indent, Map<String, Object> props, String defaultSelection) {
        StringBuilder optionList = new StringBuilder();

        //if default provided append.
        if ((defaultSelection != null) && (!defaultSelection.trim().isEmpty())) {
            defaultSelection = escapeHtmlCharacters(defaultSelection + "");
            optionList.append(" defaultValue=\"" + defaultSelection + "\" ");
        }

        //see if 'allowed' is set
        ArrayList<String> options = (ArrayList<String>) props.get("allowed");
        if (options != null && !options.isEmpty()) {
            Collections.sort(options);
            //close simple-property with children
            optionList.append(">\n");
            doIndent(indent + 1, optionList);
            optionList.append("<c:property-options>\n");
            for (String prop : options) {
                doIndent(indent + 2, optionList);
                optionList.append("<c:option value=\"" + prop + "\" name=\"" + prop + "\"/>\n");
            }
            doIndent(indent + 1, optionList);//move indentation for end of property-options
            optionList.append("</c:property-options>\n");
            doIndent(indent, optionList);
            optionList.append("</c:simple-property>");
        } else {//no additional child elements to add
            optionList.append("/>\n");
        }
        return optionList.toString();
    }

    /** Generates hardcoded list of options not available because of JIRA
     *  https://issues.jboss.org/browse/AS7-4384
     * 
     * @param indent
     * @param properties
     * @param defaultSelection
     * @return
     */
    private String generateOptionList(int indent, String[] properties, String defaultSelection) {
        StringBuilder optionList = new StringBuilder();

        if ((defaultSelection != null) && (!defaultSelection.trim().isEmpty())) {
            defaultSelection = escapeHtmlCharacters(defaultSelection + "");
            optionList.append(" defaultValue=\"" + defaultSelection + "\" >\n");
        } else {//no default provided
            optionList.append(">\n");
        }

        doIndent(indent + 1, optionList);
        optionList.append("<c:property-options>\n");
        //spinder 3/28/12: There is not way to query this option list and process is not used much. Hardcoding for now. 
        //https://issues.jboss.org/browse/AS7-4384
        for (String prop : properties) {
            doIndent(indent + 2, optionList);
            optionList.append("<c:option value=\"" + prop + "\" name=\"" + prop + "\"/>\n");
        }
        doIndent(indent + 1, optionList);
        optionList.append("</c:property-options>\n");
        doIndent(indent, optionList);
        optionList.append("</c:simple-property>");
        return optionList.toString();
    }

    private void doIndent(int indent, StringBuilder sb) {
        for (int i = 0; i < indent; i++) {
            sb.append(' ');
        }
    }

    private Type getTypeFromProps(Map<String, Object> props) {
        Map<String, String> tMap = (Map<String, String>) props.get("type");
        if (tMap == null) {
            return Type.OBJECT;
        }

        String type = tMap.get("TYPE_MODEL_VALUE");
        Type ret = Type.valueOf(type);

        return ret;
    }

    private static void usage() {
        System.out.println("Domain2Properties [-U<user>:<pass>] [-p|-m|-o] path type");
        System.out.println("   path is of kind 'key=value[,key=value]+");
        System.out.println(" -p create properties (default)");
        System.out.println(" -m create metrics");
        System.out.println(" -o create operations");
        System.out.println(" -r recurse node and list children and properties.");
        System.out.println(" -U<user>:<pass>  - supply credentials to talk to AS7");
    }

    public enum Type {

        STRING(false, "string"), INT(true, "integer"), BOOLEAN(false, "boolean"), LONG(true, "long"), BIG_DECIMAL(true,
            "long"), OBJECT(false, "-object-"), LIST(false, "-list-"), DOUBLE(true, "long"), PROPERTY(false,
            "-option-list-");

        private boolean numeric;
        private String rhqName;

        private Type(boolean numeric, String rhqName) {
            this.numeric = numeric;
            this.rhqName = rhqName;
        }

        public boolean isNumeric() {
            return numeric;
        }
    }

    private enum D2DMode {
        METRICS, PROPERTIES, OPERATION, RECURSIVE
    }

}
