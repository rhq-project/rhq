/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.omg.CosNaming._BindingIteratorImplBase;

import org.rhq.modules.plugins.jbossas7.json.ComplexResult;
import org.rhq.modules.plugins.jbossas7.json.Operation;
import org.rhq.modules.plugins.jbossas7.json.PROPERTY_VALUE;

/**
 * Generate properties, metrics and operation templates for the
 * plugin descriptor from a domain dump (server can run in domain
 * or standalone mode).
 *
 * @author Heiko W. Rupp
 */
@SuppressWarnings("unchecked")
public class Domain2Descriptor {

    public static void main(String[] args) throws Exception {

        if (args.length<1) {
            usage();
            System.exit(1);
        }

        Domain2Descriptor d2d = new Domain2Descriptor();
        d2d.run(args);


    }

    private void run(String[] args) {

        D2DMode mode = null;
        int pos = 0;
        if (args[0].startsWith("-")) {
            if (args[0].equals("-m"))
                mode = D2DMode.METRICS;
            else if (args[0].equals("-p"))
                mode = D2DMode.PROPERTIES;
            else if (args[0].equals("-o"))
                mode = D2DMode.OPERATION;
            else {
                usage();
                return;
            }
            pos++;
        }

        String path = args[pos];
        String childType = null;
        if (args.length>pos+1)
            childType = args[pos+1];

        ASConnection conn = new ASConnection("localhost",9990);

        List<PROPERTY_VALUE> address = pathToAddress(path);
        Operation op = new Operation("read-resource-description",address);
        op.addAdditionalProperty("recursive","true");

        if (mode== D2DMode.OPERATION)
            op.addAdditionalProperty("operations",true);
        if (mode == D2DMode.METRICS)
            op.addAdditionalProperty("include-runtime",true);

        ComplexResult res = conn.executeComplex(op);
        if (!res.isSuccess()) {
            System.err.println("Failure: " + res.getFailureDescription());
            return;
        }


        Map<String,Object> resMap = res.getResult();
        String what;
        if (mode== D2DMode.OPERATION)
            what="operations";
        else
            what="attributes";

        Map<String,Object> attributesMap;
        if (childType!=null) {
            Map childMap = (Map) resMap.get("children");
            Map <String,Object> typeMap = (Map<String, Object>) childMap.get(childType);
            Map descriptionMap = (Map) typeMap.get("model-description");
            if (descriptionMap==null) {
                System.err.println("No model description found");
                return;
            }
            Map starMap = (Map) descriptionMap.get("*");
            attributesMap = (Map<String, Object>) starMap.get(what);
        }
        else {
            attributesMap = (Map<String, Object>) resMap.get(what);
        }

        if (mode==D2DMode.OPERATION) {
            for (Map.Entry<String,Object> entry : attributesMap.entrySet()) {
                if (entry.getKey().startsWith("read-"))
                    continue;
                if (entry.getKey().equals("write-attribute"))
                    continue;

                createOperation(entry.getKey(), (Map<String,Object>)entry.getValue());
            }
        }
        else
            createProperties(mode, attributesMap, 0);
//        }
    }

    private void createProperties(D2DMode mode, Map<String, Object> attributesMap, int indent) {
        if (attributesMap==null)
            return;

        for (Map.Entry<String,Object> entry : attributesMap.entrySet()) {

            Map<String,Object> props = (Map<String, Object>) entry.getValue();

            String entryName = entry.getKey();

            Type ptype = getTypeFromProps(props);
            String typeString = getTypeStringForTypeAndName(ptype, entryName);

            if (ptype == Type.OBJECT) {
                System.out.println("<c:map-property name=\"" + entryName +"\" description=\"" +
                        props.get("description") + "\" >");
                Map<String, Object> attributesMap1 = (Map<String, Object>) props.get(
                        "attributes");
                if (attributesMap1!=null)
                    createProperties(mode,
                        attributesMap1, indent+4);
                else {
                    for (Map.Entry<String,Object> emapEntry : props.entrySet()) {
                        String key = emapEntry.getKey();
                        if (key.equals("type") || key.equals("description") || key.equals("required"))
                            continue;

                        Map<String,Object> emapEntryValue = (Map<String, Object>) emapEntry.getValue();
                        String ts = getTypeStringForTypeAndName(getTypeFromProps(emapEntryValue),key);
                        StringBuilder sb = generateProperty(indent, emapEntryValue,ts,emapEntry.getKey(),getAccessType(emapEntryValue));
                        System.out.println(sb.toString());

                    }
                }

                System.out.println("</c:map-property>");

                continue;

            }

            if (ptype== Type.LIST && mode!= D2DMode.METRICS) {

                StringBuilder sb = new StringBuilder("<c:list-property name=\"");
                sb.append(entryName);
                sb.append("\"");
                String description = (String) props.get("description");
                appendDescription(sb,description);
                sb.append(" >\n");
                if (!props.containsKey("attributes"))
                    sb.append("    <c:simple-property name=\"").append(entryName).append("\" />\n");
                else {
                    doIndent(indent,sb);
                    sb.append("<c:map-property name=\"").append(entryName).append("\">\n");
                    System.out.println(sb.toString());
                    createProperties(mode, (Map<String, Object>) props.get("attributes"), indent + 4);
                    sb = new StringBuilder();
                    doIndent(indent,sb);
                    sb.append("</c:map-property>\n");
                }
                sb.append("</c:list-property>");

                System.out.println(sb.toString());


                continue;
            }

            String accessType = getAccessType(props);
            if (mode== D2DMode.METRICS) {
                if (!accessType.equals("metric"))
                    continue;

                StringBuilder sb = new StringBuilder();
                doIndent(indent,sb);
                sb.append("<metric property=\"");
                sb.append(entryName).append('"');
                if (ptype== Type.STRING)
                    sb.append(" dataType=\"trait\"");

                String description = (String) props.get("description");
                appendDescription(sb,description);
                sb.append("/>");
                System.out.println(sb.toString());

            }
            else { // configuration
                if (accessType.equals("metric"))
                    continue;

                StringBuilder sb = generateProperty(indent, props, typeString, entryName, accessType);

                System.out.println(sb.toString());
            }
        }
    }

    private void createOperation(String name, Map<String, Object> operationMap) {

        StringBuilder builder = new StringBuilder("<operation name=\"");

        builder.append(name).append('"');

        String description = (String) operationMap.get("description");
        appendDescription(builder, description);
        builder.append(">\n");

        Map<String,Object> reqMap = (Map<String, Object>) operationMap.get("request-properties");
        if (reqMap!=null && !reqMap.isEmpty()) {
            builder.append("  <parameters>\n");
            generatePropertiesForMap(builder, reqMap, true);
            builder.append("  </parameters>\n");

        }
        Map replyMap = (Map) operationMap.get("reply-properties");
        if (replyMap!=null && !replyMap.isEmpty()){
            builder.append("  <results>\n");
            generatePropertiesForMap(builder, replyMap, true);
            builder.append("  </results>\n");
        }


        builder.append("</operation>\n");
        System.out.println(builder.toString());
    }

    private void appendDescription(StringBuilder builder, String description) {
        if (description!=null && !description.isEmpty()) {
            if (builder.length()>120)
                builder.append("\n        ");
            builder.append(" description=\"");

            description = description.replace("<","&lt;");
            description = description.replace(">","&gt;");
            description = description.replace("\"","\\\"");

            builder.append(description);
            builder.append('"');
        }
    }

    private void generatePropertiesForMap(StringBuilder builder, Map<String, Object> map, boolean forceReadWrite) {
        for (Map.Entry<String,Object> entry : map.entrySet()) {

            Map<String, Object> entryValue = (Map<String, Object>) entry.getValue();
            String entryKey = entry.getKey();

            Type type = getTypeFromProps(entryValue);
            String typeString = getTypeStringForTypeAndName(type, entryKey);
            builder.append(generateProperty(4, entryValue,typeString, entryKey, null));
            builder.append('\n');
        }
    }

    private String getAccessType(Map<String, Object> props) {
        String accessType = (String) props.get("access-type");
        if (accessType==null)
            accessType = "read-only"; // default of as7
        return accessType;
    }

    private String getTypeStringForTypeAndName(Type ptype, String entryName) {
        String typeString;

        switch (ptype) {
        case INT:
            typeString = "integer"; break;
        case STRING:
            typeString = "string"; break;
        case BOOLEAN:
            typeString = "boolean"; break;
        case LONG:
            typeString = "long"; break;
        case BIG_DECIMAL:
            typeString = "long"; break; // TODO better float or double?
        case LIST:
            typeString = "-list-";
            break; // Handled below
        case OBJECT: // an embedded map
            typeString = "-object-";
            break; // Handled below
        default:
            typeString = "- unknown -";
            System.err.println("Unknown type " + ptype + " for " + entryName);
        }
        return typeString;
    }

    private StringBuilder generateProperty(int indent, Map<String, Object> props, String typeString, String entryName,
                                           String accessType) {
        StringBuilder sb = new StringBuilder();
        doIndent(indent,sb);
        sb.append("<c:simple-property name=\"");
        sb.append(entryName).append('"');

        Object required = props.get("required");
        if (required != null && (Boolean) required) {
            sb.append(" required=\"true\"");
        }
        else {
            sb.append(" required=\"false\"");
        }

        sb.append(" type=\"").append(typeString).append("\"");
        sb.append(" readOnly=\"");
        if (accessType!=null && accessType.equals("read-only")) // TODO if no access-type is given, the one from the parent applies
            sb.append("true");
        else
            sb.append("false");
        sb.append('"');

        Object defVal = props.get("default");
        if (defVal!=null) {
            sb.append(" defaultValue=\"").append(defVal).append('\"');
        }

        String description = (String) props.get("description");
        appendDescription(sb,description);
        sb.append("/>");
        return sb;
    }

    private void doIndent(int indent, StringBuilder sb) {
        for (int i = 0 ; i < indent ; i++)
            sb.append(' ');
    }

    private Type getTypeFromProps(Map<String, Object> props) {
        Map<String,String> tMap = (Map<String, String>) props.get("type");
        if (tMap==null)
            return Type.OBJECT;

        String type = tMap.get("TYPE_MODEL_VALUE");
        Type ret = Type.valueOf(type);

        return ret;
    }

    /**
      * Convert a path in the form key=value,key=value... to a List of properties.
      * @param path Path to translate
      * @return List of properties
      */
     public List<PROPERTY_VALUE> pathToAddress(String path) {
         if (path==null || path.isEmpty())
             return Collections.emptyList();

         List<PROPERTY_VALUE> result = new ArrayList<PROPERTY_VALUE>();
         String[] components = path.split(",");
         for (String component : components) {
             String tmp = component.trim();

             if (tmp.contains("=")) {
                 // strip / from the start of the key if it happens to be there
                 if (tmp.startsWith("/"))
                     tmp = tmp.substring(1);

                 String[] pair = tmp.split("=");
                 PROPERTY_VALUE valuePair = new PROPERTY_VALUE(pair[0], pair[1]);
                 result.add(valuePair);
             }
         }

         return result;
     }



    private static void usage() {
        System.out.println("Domain2Properties [-p|-m] path type");
        System.out.println("   path is of kind 'key=value[,key=value]+");
        System.out.println(" -p create properties (default)");
        System.out.println(" -m create metrics");
        System.out.println(" -o create operations");
    }

    public enum Type {
        STRING,
        INT,
        BOOLEAN,
        LONG,
        BIG_DECIMAL,
        OBJECT,
        LIST

        ;
    }

    private enum D2DMode {
        METRICS,
        PROPERTIES,
        OPERATION
        ;
    }

}
