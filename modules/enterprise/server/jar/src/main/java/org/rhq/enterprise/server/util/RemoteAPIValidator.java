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

import java.lang.reflect.Method;

import javax.ejb.Local;

import org.rhq.enterprise.server.alert.AlertDefinitionManagerBean;
import org.rhq.enterprise.server.alert.AlertManagerBean;
import org.rhq.enterprise.server.auth.SubjectManagerBean;
import org.rhq.enterprise.server.authz.RoleManagerBean;
import org.rhq.enterprise.server.configuration.ConfigurationManagerBean;
import org.rhq.enterprise.server.content.ChannelManagerBean;
import org.rhq.enterprise.server.event.EventManagerBean;
import org.rhq.enterprise.server.measurement.AvailabilityManagerBean;
import org.rhq.enterprise.server.measurement.CallTimeDataManagerBean;
import org.rhq.enterprise.server.measurement.MeasurementBaselineManagerBean;
import org.rhq.enterprise.server.measurement.MeasurementDataManagerBean;
import org.rhq.enterprise.server.measurement.MeasurementDefinitionManagerBean;
import org.rhq.enterprise.server.measurement.MeasurementProblemManagerBean;
import org.rhq.enterprise.server.measurement.MeasurementScheduleManagerBean;
import org.rhq.enterprise.server.operation.OperationManagerBean;
import org.rhq.enterprise.server.resource.ResourceManagerBean;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerBean;

/**
 * @author Joseph Marques
 */
public class RemoteAPIValidator {

    private static Class<?>[] beans = new Class[] { //
    AlertDefinitionManagerBean.class, //
        AlertManagerBean.class,//
        AvailabilityManagerBean.class,//
        CallTimeDataManagerBean.class, //
        ChannelManagerBean.class,//
        ConfigurationManagerBean.class, //
        EventManagerBean.class, //
        MeasurementBaselineManagerBean.class,//
        MeasurementDataManagerBean.class,//
        MeasurementDefinitionManagerBean.class, //
        MeasurementProblemManagerBean.class,//
        MeasurementScheduleManagerBean.class, //
        OperationManagerBean.class, //
        ResourceGroupManagerBean.class, //
        ResourceManagerBean.class,//
        RoleManagerBean.class, //
        SubjectManagerBean.class };

    public static void validate() {
        for (Class<?> managerBean : beans) {
            Class<?>[] interfaces = managerBean.getInterfaces();
            if (interfaces.length != 2) {
                throw new ValidationException(managerBean.getSimpleName() + " had " + interfaces.length
                    + " interfaces, was expecting 2");
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

            Method[] remoteMethods = remoteInterface.getMethods();
            for (Method remoteMethod : remoteMethods) {
                String methodName = remoteMethod.getName();
                Class<?>[] parameterTypes = remoteMethod.getParameterTypes();
                try {
                    localInterface.getMethod(methodName, parameterTypes);
                } catch (NoSuchMethodException nsme) {
                    throw new ValidationException(managerBean.getSimpleName() + " had remote method '"
                        + format(remoteMethod) + "' which wasn't found in the local interface");
                }
            }
        }
    }

    private static String format(Method method) {
        StringBuilder builder = new StringBuilder();
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
        RemoteAPIValidator.validate();
    }
}

class ValidationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ValidationException(String message) {
        super(message);
    }
}