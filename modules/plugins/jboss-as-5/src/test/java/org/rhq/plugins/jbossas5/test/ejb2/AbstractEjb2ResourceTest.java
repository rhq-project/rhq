/*
 * Jopr Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */

package org.rhq.plugins.jbossas5.test.ejb2;

import static org.testng.Assert.fail;

import java.io.File;
import java.lang.reflect.Method;

import org.rhq.core.pc.PluginContainer;
import org.rhq.plugins.jbossas5.test.AbstractResourceTest;
import org.rhq.plugins.jbossas5.test.util.AppServerUtils;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.BeforeGroups;
import org.testng.annotations.Parameters;

/**
 * 
 * @author Lukas Krejci
 */
public abstract class AbstractEjb2ResourceTest extends AbstractResourceTest {

    protected static class MethodArgDef {
        private Class<?> type;
        private Object value;

        public MethodArgDef(Class<?> type, Object value) {
            this.type = type;
            this.value = value;
        }

        public Class<?> getType() {
            return type;
        }

        public Object getValue() {
            return value;
        }
    }

    @BeforeGroups(groups = "as5-plugin")
    @Parameters("ejb2.test.jars.path")
    public void deployEjb2TestJars(String jarPaths) {
        try {
            System.out.println("Deploying EJB2 test jars to AS...");

            for (String jarPath : jarPaths.split(";")) {
                File jarFile = new File(jarPath);
                AppServerUtils.deployFileToAS(jarFile.getName(), jarFile, false);
            }
            //and discover the resources
            PluginContainer.getInstance().getInventoryManager().executeServiceScanImmediately();
        } catch (Exception e) {
            fail("Failed to deploy EJB2 test jars.", e);
        }
    }

    @AfterGroups(groups = "as5-plugin")
    @Parameters("ejb2.test.jars.path")
    public void undeployEjb2TestJars(String jarPaths) {
        try {
            System.out.println("Undeploying EJB2 test jars from AS...");
            for (String jarPath : jarPaths.split(";")) {
                File jarFile = new File(jarPath);
                AppServerUtils.undeployFromAS(jarFile.getName());
            }
        } catch (Exception e) {
            fail("Failed to undeploy EJB2 test jars.", e);
        }
    }

    protected static Object createRemoteBean(String homeJndiName, MethodArgDef... createMethodArgs) throws Exception {
        Object home = AppServerUtils.getRemoteObject(homeJndiName, Object.class);
        return invokeMethod("create", home, createMethodArgs);
    }

    protected static Object invokeMethod(String methodName, Object instance, MethodArgDef... methodArgTypesAndValues)
        throws Exception {
        Class<?>[] argTypes = null;
        Object[] argValues = null;

        if (methodArgTypesAndValues != null) {
            argTypes = new Class<?>[methodArgTypesAndValues.length];
            argValues = new Object[methodArgTypesAndValues.length];

            for (int i = 0; i < methodArgTypesAndValues.length; ++i) {
                argTypes[i] = methodArgTypesAndValues[i].getType();
                argValues[i] = methodArgTypesAndValues[i].getValue();
            }
        }

        Method method = instance.getClass().getMethod(methodName, argTypes);

        return method.invoke(instance, argValues);
    }
}
