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
import jline.ConsoleReader;

import javax.jws.WebParam;
import javax.script.Bindings;
import javax.script.ScriptContext;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.MethodDescriptor;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Collections;

import org.rhq.enterprise.client.utility.ReflectionUtility;
import org.rhq.enterprise.client.utility.ResourceClientProxy;
import org.rhq.core.domain.auth.Subject;

/**
 * A Contextual JavaScript interactive completor. Not perfect, but
 * handles a fair number of cases.
 *
 * @author Greg Hinkle
 */
public class ServiceCompletor implements Completor {

    private Map<String, Object> services;

    private ScriptContext context;

    private String lastComplete;

    // Consecutive times this exact complete has been requested
    private int recomplete;
    private ConsoleReader consoleReader;

    private static final Set<String> IGNORED_METHODS;
    static {
        IGNORED_METHODS = new HashSet<String>();
        IGNORED_METHODS.add("newProxyInstance");
        IGNORED_METHODS.add("hashCode");
        IGNORED_METHODS.add("equals");
        IGNORED_METHODS.add("getInvocationHandler");
        IGNORED_METHODS.add("isProxyClass");
        IGNORED_METHODS.add("newProxyInstance");
        IGNORED_METHODS.add("getProxyClass");
        IGNORED_METHODS.add("main");
        IGNORED_METHODS.add("handler");
        IGNORED_METHODS.add("init");
        IGNORED_METHODS.add("initChildren");
        IGNORED_METHODS.add("initMeasurements");
        IGNORED_METHODS.add("initOperations");
    }

    public ServiceCompletor(ConsoleReader reader) {
        this.consoleReader = reader;
    }

    public void setServices(Map<String, Object> services) {
        this.services = services;
    }

    public int complete(String s, int i, List list) {
        try {
            if (lastComplete != null && lastComplete.equals(s)) {
                recomplete++;
            } else {
                recomplete = 1;
            }

            lastComplete = s;


            String base = s;

            int rootLength = 0;

            if (s.indexOf('=') > 0) {
                base = s.substring(s.indexOf("=") + 1).trim();
                rootLength = s.length() - base.length();
            }


            String[] call = base.split("\\.", 2);
            if (base.endsWith(".")) {
                call = new String[]{call[0], ""};
            }


            if (call.length == 1) {
                Map<String, Object> matches = getContextMatches(call[0]);
                if (matches.size() == 1 && matches.containsKey(call[0])) {
                    list.add(".");
                    return rootLength + call[0].length() + 1;

                } else {
                    list.addAll(matches.keySet());
                }
            } else {
                Object rootObject = context.getAttribute(call[0]);
                if (rootObject != null) {
                    int matchIndex = contextComplete(rootObject, call[1], i, list);
                    Collections.sort(list);
                    return rootLength + call[0].length() + 1 + matchIndex;
                }
            }

            Collections.sort(list);


            return (list.size() == 0) ? (-1) : rootLength;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }


    public int contextComplete(Object baseObject, String s, int i, List list) {
        if (!s.contains("(") && s.contains(".")) {
            String[] call = s.split("\\.", 2);

            Map<String, Object> matches = getContextMatches(baseObject, call[0]);
            Object rootObject = matches.get(call[0]);

            return call[0].length() + 1 + contextComplete(rootObject, call[1], i, list);
        } else {
            String[] call = s.split("\\(", 2);

            Map<String, Object> matches = getContextMatches(baseObject, call[0]);


            if (call.length == 2 && matches.containsKey(call[0])) {

                List<Object> exactMatches = getExactContextMatches(baseObject, call[0]);
                int x = 0;
                for (Object match : exactMatches) {
                    if (match instanceof Method) {

                        if (recomplete == 3) {
                            displaySignatures(baseObject, (Method)match);

                            return -1;
                        }

                        int result = completeParameters(call[1], i, list, (Method) match);  // x should be the same for all calls
                        if (result > 0) {
                            x = result;
                        }
                    }
                }
                return call[0].length() + 1 + x;

            }

            if (matches.size() == 1 && matches.containsKey(call[0])) {
                if (matches.get(call[0]) instanceof Method) {
                    list.add("(");
                }
                return call[0].length() + 1;
            }

            if (recomplete == 2) {
                List<Method> methods = new ArrayList<Method>();
                for (Object val : matches.values()) {
                    if (val instanceof Method) {
                        methods.add((Method) val);
                    }
                }
                displaySignatures(baseObject,methods.toArray(new Method[methods.size()]));
            } else {
                list.addAll(matches.keySet());
            }
            return 0;

        }
    }

    private void displaySignatures(Object object, Method... methods) {
        try {
            String start = this.consoleReader.getCursorBuffer().getBuffer().toString();
            while ((this.consoleReader.getCursorBuffer().cursor > 0)
                       && this.consoleReader.backspace()) {
                ;
            }
            this.consoleReader.printNewline();
            this.consoleReader.printNewline();
            for (Method m : methods) {
                this.consoleReader.printString(getSignature(object, m));
                this.consoleReader.printNewline();
            }
            this.consoleReader.drawLine();
            this.consoleReader.putString(start);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public int completeParameters(String params, int i, List list, Method method) {

        String[] paramList = params.split(",");


        Class[] c = method.getParameterTypes();

        String lastParam = paramList[paramList.length - 1];
        int paramIndex = paramList.length - 1;
        if (params.trim().endsWith(",")) {
            lastParam = "";
            paramIndex++;
        }

        int baseLength = 0;

        for (int x = 0; x < paramIndex; x++) {
            Object paramFound = context.getAttribute(paramList[x]);

            if (paramFound != null && !c[x].isAssignableFrom(paramFound.getClass())) {
                return -1;
            }
            baseLength += paramList[x].length() + 1;
        }


        if (paramIndex >= c.length) {
            list.add(params + ")");
            return (params + ")").length();
        } else {
            Map<String, Object> matches = getContextMatches(lastParam, c[paramIndex]);

            if (matches.size() == 1 && matches.containsKey(lastParam)) {
                list.add(paramIndex == c.length - 1 ? ")" : ",");
                return baseLength + lastParam.length();
            } else {
                list.addAll(matches.keySet());
            }

//            if (list.size() == 0 && recomplete == 3) {
//                Annotation[][] annotations = method.getParameterAnnotations();
//
//                if (annotations != null && annotations.length >= i) {
//                    Annotation[] as = annotations[paramIndex];
//                    for (Annotation a : as) {
//                        if (a instanceof WebParam) {
//                            list.add("name: " + ((WebParam) a).name());
//                        }
//                    }
//                }
//                list.add("type: " + c[paramIndex].getSimpleName());
//            }
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
        if (services != null) {
            for (String var : services.keySet()) {
                if (var.startsWith(start)) {
                    found.put(var, services.get(var));
                }
            }
        }
        return found;
    }


    private Map<String, Object> getContextMatches(String start, Class typeFilter) {
        Map<String, Object> found = new HashMap<String, Object>();
        if (context != null) {
            for (int scope : context.getScopes()) {
                Bindings bindings = context.getBindings(scope);
                for (String var : bindings.keySet()) {
                    if (var.startsWith(start)) {

                        if ((bindings.get(var) != null && typeFilter.isAssignableFrom(bindings.get(var).getClass()))
                                || recomplete == 2) {
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

        try {

            BeanInfo info = Introspector.getBeanInfo(baseObject.getClass(), Object.class);

            Set<Method> methodsCovered = new HashSet<Method>();

            PropertyDescriptor[] descriptors = info.getPropertyDescriptors();
            for (PropertyDescriptor desc : descriptors) {
                if (desc.getName().startsWith(start)) {
                    found.put(desc.getName(), desc);
                    methodsCovered.add(desc.getReadMethod());
                    methodsCovered.add(desc.getWriteMethod());
                }
            }

            MethodDescriptor[] methods = info.getMethodDescriptors();
            for (MethodDescriptor desc : methods) {
                if (desc.getName().startsWith(start)
                        && !methodsCovered.contains(desc.getMethod())
                        && !desc.getName().startsWith("_d")
                        && !IGNORED_METHODS.contains(desc.getName())) {

                    found.put(desc.getName(), desc.getMethod());
                }
            }


        } catch (IntrospectionException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        return found;
    }

    private List<Object> getExactContextMatches(Object baseObject, String attribute) {
        List<Object> found = new ArrayList<Object>();

        try {

            BeanInfo info = Introspector.getBeanInfo(baseObject.getClass(), Object.class);

            Set<Method> methodsCovered = new HashSet<Method>();

            PropertyDescriptor[] descriptors = info.getPropertyDescriptors();
            for (PropertyDescriptor desc : descriptors) {
                if (desc.getName().equals(attribute)) {
                    found.add(desc);
                    methodsCovered.add(desc.getReadMethod());
                    methodsCovered.add(desc.getWriteMethod());
                }
            }

            MethodDescriptor[] methods = info.getMethodDescriptors();
            for (MethodDescriptor desc : methods) {
                if (desc.getName().equals(attribute) && !methodsCovered.contains(desc.getMethod())) {
                    found.add(desc.getMethod());
                }
            }


        } catch (IntrospectionException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
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


    private static String getSignature(Object object, Method m) {

        // If its our service proxy lookup the original interface that has the annotations
        if (object instanceof Proxy) {
            if (Proxy.getInvocationHandler(object) instanceof RemoteClientProxy) {
                try {
                    m = ((RemoteClientProxy)Proxy.getInvocationHandler(object)).
                            getRemoteInterface().
                                getDeclaredMethod(m.getName(),m.getParameterTypes());
                } catch (NoSuchMethodException e) {
                    Class[] params = m.getParameterTypes();
                    Class[] newParams = new Class[params.length + 1];
                    System.arraycopy(params, 0, newParams, 1, params.length);
                    newParams[0] = Subject.class;

                    try {
                           m = ((RemoteClientProxy)Proxy.getInvocationHandler(object)).
                            getRemoteInterface().
                                getDeclaredMethod(m.getName(),newParams);
                    } catch (NoSuchMethodException nsme) {
                        
                    }
                }
            }
        }


        StringBuilder buf = new StringBuilder();
        Type[] params = m.getGenericParameterTypes();
        Annotation[][] annotations = m.getParameterAnnotations();
        int i = 0;

        buf.append(ReflectionUtility.getSimpleTypeString(m.getGenericReturnType()));
        buf.append(" ");

        buf.append(m.getName());
        buf.append("(");
        for (Type type : params) {
            if (i > 0) {
                buf.append(", ");
            }

            String name = null;

            if (annotations != null && annotations.length >= i) {
                Annotation[] as = annotations[i];
                for (Annotation a : as) {
                    if (a instanceof WebParam) {
                        name = ReflectionUtility.getSimpleTypeString(type) + " " + ((WebParam) a).name();
                    }
                }
            }

            if (name == null) {
                name = ReflectionUtility.getSimpleTypeString(type);
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
