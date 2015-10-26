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

package org.rhq.scripting.javascript;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.MethodDescriptor;
import java.beans.PropertyDescriptor;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.script.Bindings;
import javax.script.ScriptContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.ScriptableObject;

import org.rhq.scripting.CodeCompletion;
import org.rhq.scripting.MetadataProvider;

/**
 * A Contextual JavaScript interactive completor. Not perfect, but
 * handles a fair number of cases.
 *
 * @author Greg Hinkle
 * @author Lukas Krejci
 */
public class JavascriptCompletor implements CodeCompletion {

    private static final Log LOG = LogFactory.getLog(JavascriptCompletor.class);

    private ScriptContext context;
    private MetadataProvider metadataProvider;

    private String lastComplete;

    // Consecutive times this exact complete has been requested
    private int recomplete;

    private static final Set<String> IGNORED_METHODS;
    static {
        IGNORED_METHODS = new HashSet<String>();
        IGNORED_METHODS.add("newProxyInstance");
        IGNORED_METHODS.add("hashCode");
        IGNORED_METHODS.add("equals");
        IGNORED_METHODS.add("getInvocationHandler");
        IGNORED_METHODS.add("setHandler");
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

    @Override
    @SuppressWarnings("unchecked")
    public int complete(PrintWriter output, String s, int i, @SuppressWarnings("rawtypes") List list) {
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

            String[] call = base.split("\\.");
            if (base.endsWith(".")) {
                String[] argPadded = new String[call.length + 1];
                System.arraycopy(call, 0, argPadded, 0, call.length);
                argPadded[call.length] = "";
                call = argPadded;
            }

            if (call.length == 1) {
                Map<String, Object> matches = getContextMatches(call[0]);
                if (matches.size() == 1 && matches.containsKey(call[0]) && !s.endsWith(".")) {
                    list.add(".");
                    return rootLength + call[0].length() + 1;

                } else {
                    list.addAll(matches.keySet());
                }
            } else {
                Object rootObject = context.getAttribute(call[0]);
                if (rootObject != null) {
                    String theRest = base.substring(call[0].length() + 1, base.length());
                    int matchIndex = contextComplete(output, rootObject, theRest, i, list);
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

    @Override
    public void setMetadataProvider(MetadataProvider metadataProvider) {
        this.metadataProvider = metadataProvider;
    }

    /**
     * Base Object can be an object where we're looking for methods on it, or an
     * interface. This recursively works off the completions left to right.
     *
     * Objects can be completed with fields or method calls.
     * method parameters are completed with type matching
     * method result chainings are completed based on declared return types
     *
     * e.g. have a Resource in context as myResource. Original string is
     * "myResource.name". This method would be called with a baseObject ==
     * to myResource and the string "name".
     *
     * Note: this method will not and should not execute methods, but will
     * read field properties to continue chained completions.
     *
     * @param output the output that can the completor use to convey info to the user
     * @param baseObject the context object or class to complete from
     * @param s the relative command string to check
     * @param i
     * @param list
     * @return location of relative completion
     */
    private int contextComplete(PrintWriter output, Object baseObject, String s, int i, List<String> list) {
        if (s.contains(".")) {
            String[] call = s.split("\\.", 2);

            String next = call[0];
            if (next.contains("(")) {
                next = next.substring(0, next.indexOf("("));
            }

            Map<String, List<Object>> matches = getContextMatches(output, baseObject, next);
            if (!matches.isEmpty()) {
                // BZ 871407 NPE on auto completion for javascript object
                List<Object> nextList = matches.get(next);
                if (nextList == null || nextList.isEmpty()) {
                    return -1;
                }
                Object rootObject = nextList.get(0);
                if (rootObject instanceof PropertyDescriptor && !(baseObject instanceof Class)) {
                    try {
                        Method readMethod = ((PropertyDescriptor) rootObject).getReadMethod();
                        //the read method might be null for for example indexed bean properties.
                        //Rhino doesn't interpret any more complex bean properties.
                        if (readMethod != null) {
                            rootObject = invoke(baseObject, readMethod);
                        } else {
                            return -1;
                        }
                    } catch (Exception e) {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Exception while reading a java bean property.", e);
                        }

                        return -1;
                    }
                } else if (rootObject instanceof Method) {
                    rootObject = ((Method) rootObject).getReturnType();
                }

                return call[0].length() + 1 + contextComplete(output, rootObject, call[1], i, list);
            } else {
                return -1;
            }
        } else {
            String[] call = s.split("\\(", 2);

            Map<String, List<Object>> matches = getContextMatches(output, baseObject, call[0]);

            if (call.length == 2 && matches.containsKey(call[0])) {

                int x = 0;
                for (String key : matches.keySet()) {

                    List<Object> matchList = matches.get(key);

                    if (recomplete == 2) {
                        List<Method> methods = new ArrayList<Method>();
                        for (Object match : matchList) {
                            if (match instanceof Method) {
                                methods.add((Method) match);
                            }
                        }
                        displaySignatures(output, baseObject, methods.toArray(new Method[methods.size()]));
                        return -1;
                    }

                    for (Object match : matchList) {

                        if (key.equals(call[0]) && match instanceof Method) {

                            int result = completeParameters(baseObject, call[1], i, list, (Method) match); // x should be the same for all calls
                            if (result > 0) {
                                x = result;
                            }
                        }
                    }
                }
                return call[0].length() + 1 + x;
            }

            if (matches.size() == 1 && matches.containsKey(call[0])) {
                Object obj = matches.get(call[0]).get(0);
                if (isMethod(obj)) {
                    boolean close = obj instanceof Method && ((Method) obj).getParameterTypes().length == 0;
                    list.add("(" + (close ? ")" : ""));
                }
                return call[0].length() + 1;
            }

            if (recomplete == 2) {
                List<Method> methods = new ArrayList<Method>();
                for (List<Object> matchList : matches.values()) {
                    for (Object val : matchList) {
                        if (val instanceof Method) {
                            methods.add((Method) val);
                        }
                    }
                }
                displaySignatures(output, baseObject, methods.toArray(new Method[methods.size()]));
            } else {
                if (matches.size() == 1 && matches.values().iterator().next().get(0) instanceof Method) {
                    list.add(matches.keySet().iterator().next()
                        + "("
                        + ((((Method) matches.values().iterator().next().get(0)).getParameterTypes().length == 0 ? ")"
                            : "")));
                } else {
                    list.addAll(matches.keySet());
                }
            }
            return 0;

        }
    }

    private void displaySignatures(PrintWriter output, Object object, Method... methods) {
        try {
            String[][] signatures = new String[methods.length][];
            int i = 0;
            for (Method m : methods) {
                signatures[i++] = getSignature(object, m).split(" ", 2);
            }

            int maxReturnLength = 0;
            for (String[] sig : signatures) {
                if (sig[0].length() > maxReturnLength)
                    maxReturnLength = sig[0].length();
            }

            output.println();
            output.println();

            for (String[] sig : signatures) {
                for (i = 0; i < (maxReturnLength - sig[0].length()); i++) {
                    output.print(" ");
                }

                output.print(sig[0]);
                output.print(" ");
                output.print(sig[1]);
                output.println();
            }
        } catch (Exception e) {
            e.printStackTrace(output);
        }
    }

    /**
     * Split apart the parameters to a method call and complete the last parameter. If the last
     * paramater has a valid value close that field with a "," for the next param or a ")" if is
     * the last parameter. Does all machting according to the type of the parameters of the
     * supplied method.
     *
     * @param baseObject
     * @param params
     * @param i
     * @param list
     * @param method
     * @return
     */
    public int completeParameters(Object baseObject, String params, int i, List<String> list, Method method) {

        String[] paramList = params.split(",");

        Class<?>[] c = method.getParameterTypes();

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
            if (params.endsWith(")")) {
                return -1;
            } else {
                list.add(")");
                return (params + ")").length();
            }
        } else {

            if (baseObject instanceof Map && method.getName().equals("get") && method.getParameterTypes().length == 1) {
                //unused Class<?> keyType = method.getParameterTypes()[0];
                for (Object key : ((Map<?, ?>) baseObject).keySet()) {
                    String lookupChoice = "\'" + String.valueOf(key) + "\'";
                    if (lookupChoice.startsWith(lastParam)) {
                        list.add(lookupChoice);
                    }
                }
                if (list.size() == 1) {
                    list.set(0, list.get(0) + ")");
                }

            } else {
                Class<?> parameterType = c[paramIndex];

                Map<String, Object> matches = getContextMatches(lastParam, parameterType);

                if (matches.size() == 1 && matches.containsKey(lastParam)) {

                    list.add(paramIndex == c.length - 1 ? ")" : ",");
                    return baseLength + lastParam.length();
                } else {
                    list.addAll(matches.keySet());
                }
            }

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

        //this was originally part of the code completor that lived in the CLI
        //I don't think we need it, because the services are present under the
        //same names in the context. This code can never add any new matches.
        /*
        if (services != null) {
            for (String var : services.keySet()) {
                if (var.startsWith(start)) {
                    found.put(var, services.get(var));
                }
            }
        }
        */

        return found;
    }

    /**
     * Look through all available contexts to find bindings that both start with
     * the supplied start and match the typeFilter.
     * @param start
     * @param typeFilter
     * @return
     */
    private Map<String, Object> getContextMatches(String start, Class<?> typeFilter) {
        Map<String, Object> found = new HashMap<String, Object>();
        if (context != null) {
            for (int scope : context.getScopes()) {
                Bindings bindings = context.getBindings(scope);
                for (String var : bindings.keySet()) {
                    if (var.startsWith(start)) {

                        if ((bindings.get(var) != null && typeFilter.isAssignableFrom(bindings.get(var).getClass()))
                            || recomplete == 3) {
                            found.put(var, bindings.get(var));
                        }
                    }
                }
            }

            if (typeFilter.isEnum()) {
                for (Object ec : typeFilter.getEnumConstants()) {
                    Enum<?> e = (Enum<?>) ec;
                    String code = typeFilter.getSimpleName() + "." + e.name();
                    if (code.startsWith(start)) {
                        found.put(typeFilter.getSimpleName() + "." + e.name(), e);
                    }
                }
            }
        }
        return found;
    }

    private Map<String, List<Object>> getContextMatches(PrintWriter output, Object baseObject, String start) {
        Map<String, List<Object>> found = new HashMap<String, List<Object>>();

        Class<?> baseObjectClass = null;
        if (baseObject instanceof Class) {
            baseObjectClass = (Class<?>) baseObject;
        } else {
            baseObjectClass = baseObject.getClass();
        }

        try {
            if (baseObjectClass.equals(Void.TYPE)) {
                return found;
            } else if (ScriptableObject.class.isAssignableFrom(baseObjectClass)) {
                return findJavascriptContextMatches((ScriptableObject) baseObject, start);
            } else {
                return findJavaBeanContextMatches(baseObject, baseObjectClass, start);
            }

        } catch (Exception e) {
            LOG.info("Failure during code completion", e);
            e.printStackTrace(output);
        }

        return found;
    }

    private Map<String, List<Object>> findJavascriptContextMatches(ScriptableObject object, String start) {
        // don't attempt ID completion on arrays.. While this would return the available indices in the array which
        // is a very useful completion hint, we currently only support code completion on dots. I.e. if "a" was an
        // array, code completion on "a." would return the indices of the array. The user could then be tempted to
        // to use such completed constructs even though they're not a valid javascript ("a.0" is not a valid
        // javascript expression).
        if (object instanceof NativeArray) {
            return Collections.emptyMap();
        }

        HashMap<String, List<Object>> ret = new HashMap<String, List<Object>>();

        for (Object o : object.getIds()) {
            String key = o.toString();

            if (start == null || start.isEmpty() || key.startsWith(start)) {
                Object target = object.get(key);
                ret.put(key, new ArrayList<Object>(Arrays.asList(target)));
            }
        }

        return ret;
    }

    private Map<String, List<Object>> findJavaBeanContextMatches(Object baseObject, Class<?> baseObjectClass,
        String start) throws IntrospectionException {

        Map<String, List<Object>> found = new HashMap<String, List<Object>>();

        BeanInfo info = null;
        if (baseObjectClass.isInterface() || baseObjectClass.equals(Object.class)) {
            info = Introspector.getBeanInfo(baseObjectClass);
        } else {
            info = Introspector.getBeanInfo(baseObjectClass, Object.class);
        }

        Set<Method> methodsCovered = new HashSet<Method>();

        PropertyDescriptor[] descriptors = info.getPropertyDescriptors();
        for (PropertyDescriptor desc : descriptors) {
            if (desc.getName().startsWith(start) && (!IGNORED_METHODS.contains(desc.getName()))) {

                List<Object> list = found.get(desc.getName());
                if (list == null) {
                    list = new ArrayList<Object>();
                    found.put(desc.getName(), list);
                }
                list.add(desc);

                methodsCovered.add(desc.getReadMethod());
                methodsCovered.add(desc.getWriteMethod());
            }
        }

        MethodDescriptor[] methods = info.getMethodDescriptors();
        for (MethodDescriptor desc : methods) {
            if (desc.getName().startsWith(start) && !methodsCovered.contains(desc.getMethod())
                && !desc.getName().startsWith("_d") && !IGNORED_METHODS.contains(desc.getName())) {

                Method m = desc.getMethod();

                List<Object> list = found.get(desc.getName());
                if (list == null) {
                    list = new ArrayList<Object>();
                    found.put(desc.getName(), list);
                }
                list.add(m);
            }
        }

        return found;
    }

    private String getSignature(Object object, Method m) {

        StringBuilder buf = new StringBuilder();
        Type[] params = m.getGenericParameterTypes();
        int i = 0;
        m = metadataProvider.getUnproxiedMethod(m);

        buf.append(metadataProvider.getTypeName(m.getGenericReturnType(), false));
        buf.append(" ");

        buf.append(m.getName());
        buf.append("(");
        boolean first = true;
        for (Type type : params) {
            if (!first) {
                buf.append(", ");
            } else {
                first = false;
            }

            String name = metadataProvider.getTypeName(type, false);
            String paramName = metadataProvider.getParameterName(m, i);
            if (paramName != null) {
                name += " " + paramName;
            }

            buf.append(name);

            i++;
        }
        buf.append(")");
        return buf.toString();
    }

    @Override
    public void setScriptContext(ScriptContext context) {
        this.context = context;
    }

    private static Object invoke(Object o, Method m) throws IllegalAccessException, InvocationTargetException {
        boolean access = m.isAccessible();
        m.setAccessible(true);
        try {
            return m.invoke(o);
        } finally {
            m.setAccessible(access);
        }
    }

    private static boolean isMethod(Object object) {
        return object != null && object instanceof Method || object instanceof Function;
    }
}
