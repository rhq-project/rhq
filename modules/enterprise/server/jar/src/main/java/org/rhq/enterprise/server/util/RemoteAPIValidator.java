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
package org.rhq.enterprise.server.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ejb.Local;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.alert.AlertDefinitionManagerBean;
import org.rhq.enterprise.server.alert.AlertManagerBean;
import org.rhq.enterprise.server.auth.SubjectManagerBean;
import org.rhq.enterprise.server.authz.RoleManagerBean;
import org.rhq.enterprise.server.configuration.ConfigurationManagerBean;
import org.rhq.enterprise.server.content.AdvisoryManagerBean;
import org.rhq.enterprise.server.content.ContentManagerBean;
import org.rhq.enterprise.server.content.DistributionManagerBean;
import org.rhq.enterprise.server.content.RepoManagerBean;
import org.rhq.enterprise.server.event.EventManagerBean;
import org.rhq.enterprise.server.measurement.AvailabilityManagerBean;
import org.rhq.enterprise.server.measurement.CallTimeDataManagerBean;
import org.rhq.enterprise.server.measurement.MeasurementBaselineManagerBean;
import org.rhq.enterprise.server.measurement.MeasurementDataManagerBean;
import org.rhq.enterprise.server.measurement.MeasurementDefinitionManagerBean;
import org.rhq.enterprise.server.measurement.MeasurementProblemManagerBean;
import org.rhq.enterprise.server.measurement.MeasurementScheduleManagerBean;
import org.rhq.enterprise.server.operation.OperationManagerBean;
import org.rhq.enterprise.server.report.DataAccessManagerBean;
import org.rhq.enterprise.server.resource.ResourceFactoryManagerBean;
import org.rhq.enterprise.server.resource.ResourceManagerBean;
import org.rhq.enterprise.server.resource.ResourceTypeManagerBean;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerBean;
import org.rhq.enterprise.server.support.SupportManagerBean;
import org.rhq.enterprise.server.system.SystemManagerBean;

/**
 * @author Joseph Marques
 */
public class RemoteAPIValidator {

    private static Class<?>[] beans = new Class[] { //
    AdvisoryManagerBean.class, //
        AlertDefinitionManagerBean.class, //
        AlertManagerBean.class,//
        AvailabilityManagerBean.class,//
        CallTimeDataManagerBean.class, //
        RepoManagerBean.class,//
        ConfigurationManagerBean.class, //
        ContentManagerBean.class, //
        //ContentSourceManagerBean.class, //
        DataAccessManagerBean.class, //
        DistributionManagerBean.class,//
        EventManagerBean.class, //
        MeasurementBaselineManagerBean.class,//
        MeasurementDataManagerBean.class,//
        MeasurementDefinitionManagerBean.class, //
        MeasurementProblemManagerBean.class,//
        MeasurementScheduleManagerBean.class, //
        OperationManagerBean.class, //
        //PerspectiveManagerBean.class, //
        ResourceFactoryManagerBean.class, //
        ResourceGroupManagerBean.class, //
        ResourceManagerBean.class, //
        ResourceTypeManagerBean.class, //
        RoleManagerBean.class, //
        SubjectManagerBean.class, //
        SupportManagerBean.class, //
        SystemManagerBean.class };

    private static Set<String> finderMethodExceptions = new HashSet<String>();
    static {
        /*
         * unless otherwise noted, all of the below methods return data considered to
         * be single, logical objects even though they are implemented as list structures
         */
        finderMethodExceptions.add("translateInstallationSteps");
        finderMethodExceptions.add("getResourceNameOptionItems");
        finderMethodExceptions.add("getResourceLineage");
        finderMethodExceptions.add("getResourceIdLineage");
        finderMethodExceptions.add("deleteResources"); // action method, just happens to returned deleted resourceIds
        finderMethodExceptions.add("deleteResource"); // action method, just happens to returned deleted resourceIds
        finderMethodExceptions.add("executeQuery");
        finderMethodExceptions.add("executeQueryWithPageControl");
    }

    public static void validateAll() {
        int classesInError = 0;
        int totalErrors = 0;
        for (Class<?> managerBean : beans) {
            List<String> errors = validate(managerBean);
            if (errors.isEmpty() == false) {
                classesInError++;
                totalErrors += errors.size();
                for (String error : errors) {
                    System.out.println(error);
                }
                System.out.println();
            }
        }
        System.out.println(String.valueOf(beans.length) + " classes checked, " + classesInError + " in error");
        System.out.println(String.valueOf(totalErrors) + " total errors");
    }

    public static List<String> validate(Class<?> managerBean) {
        List<String> errors = new ArrayList<String>();

        Class<?>[] interfaces = managerBean.getInterfaces();
        if (interfaces.length != 2) {
            errors.add(managerBean.getSimpleName() + " had " + interfaces.length + " interfaces, was expecting 2");
        }

        Class<?> localInterface = null;
        Class<?> remoteInterface = null;

        if (interfaces[0].getAnnotation(Local.class) != null) {
            localInterface = interfaces[0];
            remoteInterface = interfaces[1];
        } else {
            remoteInterface = interfaces[0];
            localInterface = interfaces[1];
        }

        WebService webService = remoteInterface.getAnnotation(WebService.class);
        if (webService == null) {
            errors.add(remoteInterface.getSimpleName() + ", missing @WebService class annotation");
        }

        Method[] remoteMethods = remoteInterface.getMethods();
        for (Method remoteMethod : remoteMethods) {
            WebMethod webMethod = remoteMethod.getAnnotation(WebMethod.class);
            if (webMethod == null) {
                errors.add(format(remoteMethod) + ", missing @WebMethod method annotation");
            }

            String methodName = remoteMethod.getName();
            Class<?>[] parameterTypes = remoteMethod.getParameterTypes();
            try {
                Method localMethod = localInterface.getMethod(methodName, parameterTypes);
                Method managerBeanMethod = managerBean.getMethod(methodName, parameterTypes);

                Class<?>[] localExceptions = localMethod.getExceptionTypes();
                Class<?>[] managerBeanExceptions = managerBeanMethod.getExceptionTypes();
                Class<?>[] remoteExceptions = remoteMethod.getExceptionTypes();

                for (Class<?> remoteException : remoteExceptions) {
                    // make sure local mirrors remote throws signature
                    boolean found = false;
                    for (Class<?> localException : localExceptions) {
                        if (remoteException.equals(localException)) {
                            found = true;
                            break;
                        }
                    }
                    if (found == false) {
                        errors.add(format(remoteMethod) + ", local method does not throw "
                            + remoteException.getSimpleName());
                    }

                    // make sure managerBean mirrors remote throws signature
                    for (Class<?> managerBeanException : managerBeanExceptions) {
                        if (remoteException.equals(managerBeanException)) {
                            found = true;
                            break;
                        }
                    }
                    if (found == false) {
                        errors.add(format(remoteMethod) + ", manager bean method does not throw "
                            + remoteException.getSimpleName());
                    }
                }
            } catch (NoSuchMethodException nsme) {
                errors.add(format(remoteMethod) + ", method not found in the local interface");
            }

            validateTypeParameter(remoteMethod, 0, Subject.class, "subject", errors);
            validateTypeParameter(remoteMethod, parameterTypes.length - 1, PageControl.class, "pageControl", errors);
        }

        Method[] managerBeanMethods = managerBean.getMethods();

        for (Method managerBeanMethod : managerBeanMethods) {
            String methodName = managerBeanMethod.getName();
            Class<?> returnType = managerBeanMethod.getReturnType();
            if (returnType.isAssignableFrom(List.class) || returnType.isAssignableFrom(PageList.class)) {
                if (methodName.startsWith("find") == false && finderMethodExceptions.contains(methodName) == false) {
                    errors.add(format(managerBeanMethod) + ", bean method returning a List must begin with 'find'");
                }
            }
        }

        return errors;
    }

    private static void validateTypeParameter(Method remoteMethod, int parameterIndex, Class<?> expectedParameterType,
        String namingConvention, List<String> errors) {
        int parameterCount = remoteMethod.getParameterTypes().length;
        if (parameterCount == 0) {
            return;
        }
        if (parameterIndex < 0 || parameterIndex > parameterCount - 1) {
            errors.add(format(remoteMethod) + ", parameterIndex was " + parameterIndex + " but only had "
                + parameterCount + " arguments");
        }

        Class<?> parameterType = remoteMethod.getParameterTypes()[parameterIndex];
        if (parameterType.equals(expectedParameterType) == false) {
            return;
        }
        Annotation[] parameterAnnotations = remoteMethod.getParameterAnnotations()[parameterIndex];
        WebParam webParam = null;
        for (int i = 0; i < parameterAnnotations.length; i++) {
            if (parameterAnnotations[i] instanceof WebParam) {
                webParam = (WebParam) parameterAnnotations[i];
                break;
            }
        }
        if (webParam == null) {
            errors.add(format(remoteMethod) + ", missing @WebParam parameter annotation for "
                + parameterType.getSimpleName());
            return;
        }

        String name = webParam.name();
        if (name.equals(namingConvention) == false) {
            errors.add(format(remoteMethod) + ", convention should be @WebParam(name = \"" + namingConvention + "\")");
        }
    }

    private static String format(Method method) {
        StringBuilder builder = new StringBuilder();
        builder.append(method.getDeclaringClass().getSimpleName()).append('.');
        builder.append(method.getName()).append("(");
        boolean first = true;
        for (Class<?> parameterType : method.getParameterTypes()) {
            if (first) {
                first = false;
            } else {
                builder.append(",");
            }
            builder.append(parameterType.getSimpleName());
        }
        builder.append(")");
        return builder.toString();
    }

    public static void main(String[] args) {
        RemoteAPIValidator.validateAll();
    }
}
