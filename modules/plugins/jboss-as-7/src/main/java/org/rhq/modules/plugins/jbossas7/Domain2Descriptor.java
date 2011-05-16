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

import org.rhq.modules.plugins.jbossas7.json.ComplexResult;
import org.rhq.modules.plugins.jbossas7.json.Operation;
import org.rhq.modules.plugins.jbossas7.json.PROPERTY_VALUE;

/**
 * Generate properties from a domain dump
 * @author Heiko W. Rupp
 */
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

        boolean doMetrics = false;
        int pos = 0;
        if (args[0].startsWith("-")) {
            if (args[0].equals("-m"))
                doMetrics = true;
            else if (args[0].equals("-p"))
                doMetrics = false;
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
        Operation op = new Operation("read-resource-description",address); // ,"operations",true);
        op.addAdditionalProperty("recursive","true");
        if (doMetrics)
            op.addAdditionalProperty("include-runtime",true);
        ComplexResult res = (ComplexResult) conn.execute(op,true);
        if (!res.isSuccess()) {
            System.err.println("Failure: " + res.getFailureDescription());
            return;
        }

        Map<String,Object> attributesMap;

        Map<String,Object> resMap = res.getResult();
        if (childType!=null) {
            Map childMap = (Map) resMap.get("children");
            Map <String,Object> typeMap = (Map<String, Object>) childMap.get(childType);
            Map descriptionMap = (Map) typeMap.get("model-description");
            if (descriptionMap==null) {
                System.err.println("No model description found");
                return;
            }
            Map starMap = (Map) descriptionMap.get("*");
            attributesMap = (Map<String, Object>) starMap.get("attributes");
        }
        else {
            attributesMap = (Map<String, Object>) resMap.get("attributes");
        }

        createProperties(doMetrics, attributesMap, 0);

    }

    private void createProperties(boolean doMetrcis, Map<String, Object> attributesMap, int indent) {
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
                    createProperties(doMetrcis,
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

            if (ptype== Type.LIST && !doMetrcis) {

                StringBuilder sb = new StringBuilder("<c:list-property name=\"");
                sb.append(entryName);
                sb.append("\"");
                if (props.containsKey("description")) {
                    sb.append(" description=\"");
                    sb.append(props.get("description"));
                    sb.append("\"");
                }
                sb.append(" >\n");
                if (!props.containsKey("attributes"))
                    sb.append("    <c:simple-property name=\"").append(entryName).append("\" />\n");
                else {
                    doIndent(indent,sb);
                    sb.append("<c:map-property name=\"").append(entryName).append("\">\n");
                    System.out.println(sb.toString());
                    createProperties(doMetrcis, (Map<String, Object>) props.get("attributes"), indent + 4);
                    sb = new StringBuilder();
                    doIndent(indent,sb);
                    sb.append("</c:map-property>\n");
                }
                sb.append("</c:list-property>");

                System.out.println(sb.toString());


                continue;
            }

            String accessType = getAccessType(props);
            if (doMetrcis) {
                if (!accessType.equals("metric"))
                    continue;

                StringBuilder sb = new StringBuilder();
                doIndent(indent,sb);
                sb.append("<metric property=\"");
                sb.append(entryName).append('"');
                if (ptype== Type.STRING)
                    sb.append(" dataType=\"trait\"");

                String description = (String) props.get("description");
                if (description!=null) {
                    if (sb.length()+description.length() > 120)
                        sb.append("\n        ");
                    sb.append(" description=\"").append(description).append('"');
                }
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
        if (description!=null) {
            if (sb.length()+description.length() > 120)
                sb.append("\n        ");
            sb.append(" description=\"").append(description).append('"');
        }
        sb.append("/>");
        return sb;
    }

    private void doIndent(int indent, StringBuilder sb) {
        for (int i = 0 ; i < indent ; i++)
            sb.append(' ');
    }

    private Type getTypeFromProps(Map<String, Object> props) {
        Map<String,String> tMap = (Map<String, String>) props.get("type");
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

}
