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

import javax.jws.WebParam;
import javax.script.ScriptContext;
import javax.script.Bindings;
import java.util.*;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.lang.annotation.Annotation;

/**
 * A Contextual JavaScript interactive completor. Not perfect, but
 * handles a fair number of cases.
 *
 *
 * @author Greg Hinkle
 */
public class ServiceCompletor implements Completor {

    private Map<String,Object> services;

    private ScriptContext context;

    private String lastComplete;

    // Consecutive times this exact complete has been requested
    private int recomplete;


    public ServiceCompletor(Map<String, Object> services) {
        this.services = services;
    }

    public int complete(String s, int i, List list) {

        if (lastComplete != null && lastComplete.equals(s)) {
            recomplete++;
        } else {
            recomplete = 1;
        }

        lastComplete = s;


        String base = s;

        int rootLength = 0;

        if (s.indexOf('=') > 0) {
            base = s.substring(s.indexOf("=")+1).trim();
            rootLength = s.length() - base.length();
        }


        String[] call = base.split("\\.", 2);
        if (base.endsWith(".")) {
            call = new String[] { call[0], ""};
        }


        if (call.length == 1) {
            Map<String, Object> matches = getContextMatches(call[0]);
                if (matches.size() == 1 && matches.containsKey(call[0])) {
                    list.add(".");
                    return rootLength + call[0].length() + 1;
                
            }  else {
                    list.addAll(matches.keySet());
                }
        } else {
            Object rootObject = context.getAttribute(call[0]);
            if (rootObject != null) {
                return rootLength + call[0].length() + 1 + contextComplete(rootObject, call[1], i, list);
            }
        }

        return (list.size() == 0) ? (-1) : rootLength;
    }


    public int contextComplete(Object baseObject, String s, int i, List list) {
        if (s.contains(".")) {
            String[] call = s.split("\\.",2);

            Map<String, Object> matches = getContextMatches(baseObject, call[0]);
            Object rootObject = matches.get(call[0]);

            return call[0].length() + 1 + contextComplete(rootObject, call[1],i,list);
        } else {
            String[] call = s.split("\\(", 2);

            Map<String, Object> matches = getContextMatches(baseObject, call[0]);


            if (call.length == 2 && matches.containsKey(call[0]) && matches.get(call[0]) instanceof Method) {
                return call[0].length() + 1 + completeParameters(call[1], i, list, (Method)matches.get(call[0]));
            } else {
                if (matches.size() == 1 && matches.containsKey(call[0])) {
                    list.add("(");
                    return call[0].length() + 1;
                }

                list.addAll(matches.keySet());
                return 0;
            }
        }
    }





    public int completeParameters(String params, int i, List list, Method method) {


        String[] paramList = params.split(",");



        Class[] c = method.getParameterTypes();

        String lastParam = paramList[paramList.length -1];
        int paramIndex = paramList.length - 1;
        if (params.trim().endsWith(",")) {
            lastParam = "";
            paramIndex++;
        }

        int baseLength = 0;

        for (int x = 0; x < paramIndex; x++ ) {
            if (!c[x].isAssignableFrom(context.getAttribute(paramList[x]).getClass())) { 
                return -1;
            }
            baseLength += paramList[x].length() + 1;
        }


        if (paramIndex >= c.length) {
            list.add(params+")");
            return (params+")").length();
        } else {
            Map<String, Object> matches = getContextMatches(lastParam, c[paramIndex]);

            if (matches.size() == 1 && matches.containsKey(lastParam)) {
                list.add(paramIndex == c.length-1 ?")": ",");
                return baseLength + lastParam.length();
            } else {
                list.addAll(matches.keySet());
            }

            if (list.size() == 0 && recomplete == 2) {
                Annotation[][] annotations = method.getParameterAnnotations();

                if (annotations != null && annotations.length >= i) {
                    Annotation[] as = annotations[paramIndex];
                    for (Annotation a : as) {
                        if (a instanceof WebParam) {
                            list.add("name: " + ((WebParam) a).name());
                        }
                    }
                }
            }
            list.add("type: " + c[paramIndex].getSimpleName());

            return baseLength;
        }
    }


    private Map<String, Object> getContextMatches(String start) {
        Map<String, Object> found = new HashMap<String, Object>();
        if (context != null) {
            for (Integer scope : context.getScopes()) {
                Bindings bindings = context.getBindings(scope);
                for (String var : bindings.keySet()) {
                    if (var.startsWith(start)) {
                        found.put(var, bindings.get(var));
                    }
                }
            }
        }
        for (String var : services.keySet()) {
            if (var.startsWith(start)) {
                found.put(var, services.get(var));
            }
        }
        return found;
    }


     private Map<String, Object> getContextMatches(String start, Class typeFilter) {
        Map<String, Object> found = new HashMap<String, Object>();
        if (context != null) {
            for (Integer scope : context.getScopes()) {
                Bindings bindings = context.getBindings(scope);
                for (String var : bindings.keySet()) {
                    if (var.startsWith(start)) {
                        if (typeFilter.isAssignableFrom(bindings.get(var).getClass())) {
                            found.put(var, bindings.get(var));
                        }
                    }
                }
            }
        }
        return found;
    }

    private Map<String, Object> getContextMatches(Object baseObject, String start) {
        Map<String, Object> found = new HashMap<String, Object>();

        Class intf = baseObject.getClass().getInterfaces()[0];
        Method[] methods = baseObject.getClass().getDeclaredMethods();
        for (Method m : methods) {
            if (m.getName().startsWith(start)) {
                found.put(m.getName(), m);
            }
        }


        Field[] fields = baseObject.getClass().getDeclaredFields();
        for (Field f : fields) {
            if (f.getName().startsWith(start)) {
                found.put(f.getName(), f);
            }
        }

        return found;
    }




    private List<Method> getMethods(Object service) {
        Class intf = service.getClass().getInterfaces()[0];
        Method[] methods = intf.getMethods();
        Arrays.sort(methods, new Comparator<Method>() {
            public int compare(Method o1, Method o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        List<Method> methodList = new ArrayList<Method>();
        methodList.addAll(Arrays.asList(methods));
        return methodList;
    }


    private void addSignatures( String serviceName, Method method, List candidates) {
        Object service = services.get(serviceName);
        Class intf = service.getClass().getInterfaces()[0];
        candidates.add(getSignature(method));
        
//        Method[] methods = intf.getMethods();
//        for (Method m : methods) {
//            if (m.getName().equals(methodName)) {
//                candidates.add(serviceName + "." + methodName + getSignature(m));
//            }
//        }
    }

    private static String getSignature(Method m) {
        StringBuilder buf = new StringBuilder();
        Class[] params = m.getParameterTypes();
        Annotation[][] annotations = m.getParameterAnnotations();
        int i = 0;
        for (Class type : params) {
            if (buf.length() == 0) {
                buf.append("(");
            } else {
                buf.append(", ");
            }

            String name = null;

            if (annotations != null && annotations.length >= i) {
                Annotation[] as = annotations[i];
                for (Annotation a : as) {
                    if (a instanceof WebParam) {
                        name = ((WebParam)a).name();
                    }
                }

            }

            if (name == null) {
                name = type.getSimpleName();
            }

            buf.append(name);

            i++;
        }
        buf.append(")");
        return buf.toString();
    }


    public ScriptContext getContext() {
        return context;
    }

    public void setContext(ScriptContext context) {
        this.context = context;
    }
}
