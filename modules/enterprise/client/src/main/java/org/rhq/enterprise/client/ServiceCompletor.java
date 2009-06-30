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

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import jline.Completor;

/**
 * @author Greg Hinkle
 */
public class ServiceCompletor implements Completor {

    /** K=manager name V=remote proxy */
    private Map<String, Object> managers;

    public ServiceCompletor(Map<String, Object> managers) {
        this.managers = managers;
    }

    @SuppressWarnings("unchecked")
    public int complete(String s, int i, List list) {

        List<String> completions = getCompletions(s);

        // If we have only one possible manager, extend it out to the services
        if (1 == completions.size()) {
            String completion = completions.get(0);
            if (!completion.contains(".")) {
                completions = getCompletions(completion + ".");
            }
        }

        list.addAll(completions);

        return 0;
    }

    @SuppressWarnings("unchecked")
    private List<String> getCompletions(String input) {
        List<String> completions = new ArrayList<String>();

        String[] tokens = input.split("\\(");
        String paramString = (tokens.length == 2) ? tokens[1] : "";
        String[] call = tokens[0].split("\\.");
        if (input.endsWith(".")) {
            call = new String[] { call[0], "" };
        }

        for (String managerName : managers.keySet()) {
            if (managerName.equals(call[0])) {
                if (call.length == 2) {
                    for (String methodName : getServices(managers.get(managerName))) {
                        if (methodName.equals(call[1])) {
                            // addSignatures(managerName, methodName, completions);
                            addParameters(managerName, methodName, paramString, completions);
                        } else if (methodName.startsWith(call[1])) {
                            completions.add(/*prefix +*/managerName + "." + methodName);
                        }
                    }
                }
            } else if (managerName.startsWith(call[0])) {
                completions.add(managerName);
            }
        }

        return completions;
    }

    public List<String> getServices(Object managerRemoteProxy) {
        Class remoteInterface = managerRemoteProxy.getClass().getInterfaces()[0];
        Method[] services = remoteInterface.getMethods();
        Arrays.sort(services, new Comparator<Method>() {
            public int compare(Method o1, Method o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        List<String> methodList = new ArrayList<String>();
        for (Method m : services) {
            methodList.add(m.getName());
        }
        return methodList;
    }

    public void addParameters(String managerName, String methodName, String paramString, List<String> candidates) {
        Class<?>[] paramTypes = getParamTypes(managerName, methodName);

        paramString = paramString.trim();
        int currentParam = getParamCount(paramString);
        int lastComma = paramString.lastIndexOf(',');
        String setParamString = (-1 == lastComma) ? "" : paramString.substring(0, lastComma + 1);
        String currentParamString = (-1 == lastComma) ? paramString : paramString.substring(lastComma + 1);

        CliConfig cliConfig = CliConfig.getConfig(new File("./conf/cli.xml"));
        CliConfig.Service serviceCfg = cliConfig.getManagers(managerName).get(0).getServices(methodName).get(0);
        CliConfig.Parameter parameterCfg = serviceCfg.getParameter(currentParam);
        if (null != parameterCfg) {
            serviceCfg.getParameter(currentParam).getCandidates(managerName + "." + methodName + "(" + setParamString,
                currentParamString, candidates);
        } else {
            // tbd
        }
    }

    private int getParamCount(String paramString) {
        int count = 1;
        boolean doCount = true;

        for (int i = 0, length = paramString.length(); (i < length); ++i) {
            char ch = paramString.charAt(i);
            if (ch == '"') {
                doCount = !doCount;
            } else if ((ch == ',') && doCount) {
                ++count;
            }
        }
        return count;
    }

    // Remote API does not allow overloading, so at most one signature
    private Class<?>[] getParamTypes(String serviceName, String methodName) {
        Object service = managers.get(serviceName);
        Class<?> remoteInterface = service.getClass().getInterfaces()[0];
        Method[] methods = remoteInterface.getMethods();
        for (Method m : methods) {
            if (m.getName().equals(methodName)) {
                return m.getParameterTypes();
            }
        }
        return new Class<?>[0];
    }

    // may no longer be necessary
    //
    // Remote API does not allow overloading, so at most one signature
    public void addSignatures(String serviceName, String methodName, List candidates) {
        Object service = managers.get(serviceName);
        Class remoteInterface = service.getClass().getInterfaces()[0];
        Method[] methods = remoteInterface.getMethods();
        for (Method m : methods) {
            if (m.getName().equals(methodName)) {
                candidates.add(serviceName + "." + methodName + getSignature(m));
                break;
            }
        }
    }

    // ignore the first parameter if it is a Subject, we treat that as the implied sessionSubject
    public static String getSignature(Method m) {
        StringBuilder buf = new StringBuilder();
        Class<?>[] params = m.getParameterTypes();
        for (int i = 0; (i < params.length); ++i) {
            String typeName = params[i].getSimpleName();
            if ((0 == i) && typeName.equals("Subject")) {
                continue;
            }

            if (buf.length() == 0) {
                buf.append("(");
            } else {
                buf.append(", ");
            }
            buf.append(typeName);
        }
        buf.append(")");
        return buf.toString();
    }

}
