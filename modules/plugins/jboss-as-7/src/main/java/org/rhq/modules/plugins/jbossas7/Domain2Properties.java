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

import com.apple.java.Usage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.modules.plugins.jbossas7.json.ComplexResult;
import org.rhq.modules.plugins.jbossas7.json.Operation;
import org.rhq.modules.plugins.jbossas7.json.PROPERTY_VALUE;

/**
 * Generate properties from a domain dump
 * @author Heiko W. Rupp
 */
public class Domain2Properties {

    public static void main(String[] args) throws Exception {

        if (args.length<2) {
            usage();
            System.exit(1);
        }

        Domain2Properties dp = new Domain2Properties();
        dp.run(args);


    }

    private void run(String[] args) {

        String path = args[0];
        String type = args[1];

        ASConnection conn = new ASConnection("localhost",9990);

        List<PROPERTY_VALUE> address = pathToAddress(path);
        Operation op = new Operation("read-resource-description",address,"operations",true);
        op.addAdditionalProperty("recursive","true");
        ComplexResult res = (ComplexResult) conn.execute(op,true);
        if (!res.isSuccess()) {
            System.err.println("Failure: " + res.getFailureDescription());
            return;
        }


        Map<String,Object> resMap = res.getResult();
        Map childMap = (Map) resMap.get("children");
        Map <String,Object> typeMap = (Map<String, Object>) childMap.get(type);
        Map descriptionMap = (Map) typeMap.get("model-description");
        Map starMap = (Map) descriptionMap.get("*");
        Map<String,Object> attributesMap = (Map<String, Object>) starMap.get("attributes");



        for (Map.Entry<String,Object> entry : attributesMap.entrySet()) {

            Map<String,Object> props = (Map<String, Object>) entry.getValue();


            Type ptype = getTypeFromProps(props);
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
                typeString = "long"; break; // TODO bettter float or double?
            case LIST:
                typeString = "-list-";
                break; // Handled below
            default:
                typeString = "- unknown -";
                System.err.println("Unknown type " + ptype + " for " + entry.getKey());
            }

            if (ptype==Type.LIST) {

                System.out.println("<c:list-property name=\"" + entry.getKey() +"\" >");
                System.out.println("  <c:simple-property name=\"" + entry.getKey() + "\" />");
                System.out.println("</c:list-property>");



                continue;
            }

            StringBuilder sb = new StringBuilder("<c:simple-property name=\"");
            sb.append(entry.getKey()).append('"');

            if ((Boolean) props.get("required")) {
                sb.append(" required=\"true\"");
            }

            sb.append(" type=\"").append(typeString).append("\"");
            sb.append(" readOnly=\"");
            if (props.get("access-type").equals("read-only"))
                sb.append("true");
            else
                sb.append("false");
            sb.append('"');

            String description = (String) props.get("description");
            if (sb.length()+description.length() > 120)
                sb.append("\n        ");
            sb.append(" description=\"").append(description).append('"');
            sb.append("/>");

            System.out.println(sb.toString());
        }


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
        System.out.println("Domain2Properties path type");
        System.out.println("   path is of kind 'key=value[,key=value]+");
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
