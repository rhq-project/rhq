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

import static org.testng.Assert.assertEquals;

import javax.rmi.PortableRemoteObject;

import org.rhq.core.domain.resource.Resource;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.plugins.jbossas5.test.util.AppServerUtils;
import org.rhq.plugins.jbossas5.test.util.EjbSessionBeanTestTemplate;
import org.rhq.plugins.jbossas5.test.util.MethodArgDef;
import org.testng.annotations.BeforeGroups;
import org.testng.annotations.BeforeTest;

/**
 * 
 * @author Lukas Krejci
 */
public abstract class AbstractSessionBeanTest extends AbstractEjb2ResourceTest {

    private EjbSessionBeanTestTemplate testTemplate;

    protected static abstract class Ejb2SessionBeanTestTemplate extends EjbSessionBeanTestTemplate {

        protected abstract MethodArgDef[] getEjbCreateMethodArgs();

        protected abstract Class<?> getHomeInterface();
        
        protected abstract String getHomeInterfaceJndiName();
        
        protected Object getRemoteBean() throws Exception {
            return createRemoteBean(getHomeInterfaceJndiName(), getHomeInterface(), getEjbCreateMethodArgs());
        }
        
        protected static Object createRemoteBean(String homeJndiName, Class<?> homeInterface, MethodArgDef... createMethodArgs) throws Exception {
            Object home = AppServerUtils.getRemoteObject(homeJndiName, Object.class);
            
            Object narrowed = PortableRemoteObject.narrow(home, homeInterface);
            
            return AppServerUtils.invokeMethod("create", narrowed, createMethodArgs);
        }            
    }
    
    protected AbstractSessionBeanTest(EjbSessionBeanTestTemplate testTemplate) {
        this.testTemplate = testTemplate;
    }

    protected void setupBean() {
        testTemplate.setupBean();
    }
    
    @Override
    protected void validateOperationResult(String name, OperationResult result, Resource resource) {
        if (!testTemplate.validateOperationResult(name, result, resource)) {
            super.validateOperationResult(name, result, resource);
        }
    }

    @Override
    protected void validateNumericMetricValue(String metricName, Double value, Resource resource) {
        if ("CreateCount".equals(metricName)) {
            assertEquals(value, Double.valueOf(1), "Unexpected Session Bean CreateCount.");
        } else {
            super.validateNumericMetricValue(metricName, value, resource);
        }
    }
    
    
}
