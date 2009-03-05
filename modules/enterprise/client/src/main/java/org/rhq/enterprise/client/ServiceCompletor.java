/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.enterprise.client;

import jline.Completor;

import java.util.*;
import java.lang.reflect.Method;

/**
 * @author Greg Hinkle
 */
public class ServiceCompletor implements Completor {

    private Map<String,Object> services;

    public ServiceCompletor(Map<String, Object> services) {
        this.services = services;
    }

    public int complete(String s, int i, List list) {

        /*Pattern p = Pattern.compile("^(.*)([^(]*)$");
        Matcher m = p.matcher(s);
        String prefix = "";
        if (m.matches()) {
            prefix = m.group(1);
            s = m.group(2);
        }
        */
        String[] call = s.split("\\.");
        if (s.endsWith(".")) {
            call = new String[] { call[0], ""};
        }

        for (String key : services.keySet()) {
            if (key.equals(call[0])) {
                if (call.length == 2) {
                    for (String methodName : getMethods(services.get(key))) {
                        if (methodName.equals(call[1])) {
                            addSignatures(key, methodName, list);

                        } else if (methodName.startsWith(call[1])) {
                            list.add(/*prefix +*/ key + "." + methodName);

                        }
                    }
                }
            } else if (key.startsWith(call[0])) {
                list.add(key);
            }

        }

        return 0;
    }

    public List<String> getMethods(Object service) {
        Class intf = service.getClass().getInterfaces()[0];
        Method[] methods = intf.getMethods();
        Arrays.sort(methods, new Comparator<Method>() {
            public int compare(Method o1, Method o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        List<String> methodList = new ArrayList<String>();
        for (Method m : methods) {
            methodList.add(m.getName());
        }
        return methodList;
    }


    public void addSignatures(String serviceName, String methodName, List candidates) {
        Object service = services.get(serviceName);
        Class intf = service.getClass().getInterfaces()[0];
        Method[] methods = intf.getMethods();
        for (Method m : methods) {
            if (m.getName().equals(methodName)) {
                candidates.add(serviceName + "." + methodName + getSignature(m));
            }
        }
    }

    public static String getSignature(Method m) {
        StringBuilder buf = new StringBuilder();
        Class[] params = m.getParameterTypes();
        for (Class type : params) {
            if (buf.length() == 0) {
                buf.append("(");
            } else {
                buf.append(", ");
            }
            buf.append(type.getSimpleName());
        }
        buf.append(")");
        return buf.toString();
    }

}
