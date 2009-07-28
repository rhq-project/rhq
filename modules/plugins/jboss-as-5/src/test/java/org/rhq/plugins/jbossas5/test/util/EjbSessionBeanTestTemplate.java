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

package org.rhq.plugins.jbossas5.test.util;

import static org.testng.Assert.fail;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotSame;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.pluginapi.operation.OperationResult;

/**
 * The EJB2 and EJB3 session beans share common characteristics so we can setup a common
 * template shared by the "real" tests.
 * 
 * @author Lukas Krejci
 */
public abstract class EjbSessionBeanTestTemplate {

    /**
     * This method needs to be called from a @Before* method of the test so that
     * it can setup the bean for the test.
     */
    public void setupBean() {
        try {
            Object bean = getRemoteBean();

            //call some methods so that we get the stats back
            MethodArgDef[] args = getTestedMethodArgs();

            String beanName = getTestedBeanName();
            String methodName = getTestedMethodName();
            int nofTimes = getTestedMethodExpectedInvocationCount();
            
            System.out.println("Invoking method \"" + methodName + "\" on bean \"" + beanName + "\" " + nofTimes + " times.");
            
            for (int i = 0; i < nofTimes; ++i) {
                Object ret = AppServerUtils.invokeMethod(methodName, bean, args);
                System.out.print("Invocation " + i + " returned ");
                if (ret == null) {
                    System.out.println("null");
                } else {
                    System.out.println(ret.toString());
                }
            }
        } catch (Exception e) {
            fail("Failed to setup the remote EJB test bean.", e);
        }
    }
    
    /**
     * Validates the result of the operation on the resource.
     * Returns true if the result has been validated by this method.
     * 
     * @param name the name of the operation
     * @param result the result of the operation
     * @param resource the resource upon which the operation has been invoked
     * @return true if this method validated the operation result, 
     *         false if the validation should be performed by the caller
     */
    public boolean validateOperationResult(String name, OperationResult result, Resource resource) {
        if ("viewInvocationStats".equals(name) && resource.getResourceKey().contains(getExpectedResourceKey())) {
            //the method was invoked ten times in the setup. we should see it in the results here...
            Configuration resultValueConfig = result.getComplexResults();

            assertNotNull(resultValueConfig, "viewInvocationStats results shouldn't be null.");

            PropertyList propertyList = resultValueConfig.getList("methods");

            assertNotNull(propertyList, "viewInvocationStats must include a \"methods\" property list.");
            assertNotSame(propertyList.getList().size(), 0,
                "the viewInvocationStats should contain at least one method statistics");

            PropertyMap methodStat = (PropertyMap) propertyList.getList().iterator().next();

            assertEquals(methodStat.getSimpleValue("methodName", null), getExpectedMethodName(),
                "Couldn't find method stats for the tested method.");

            PropertySimple count = methodStat.getSimple("count");
            assertNotNull(count, "Could get to the 'count' method stat property. This should not happen.");

            assertEquals(count.getIntegerValue(), Integer.valueOf(getTestedMethodExpectedInvocationCount()),
                "The tested method call count doesn't match.");

            assertNotNull(methodStat.getSimple("totalTime"),
                "Couldn't find 'totalTime' method stat. This should not happen.");
            assertNotNull(methodStat.getSimple("minTime"),
                "Couldn't find 'minTime' method stat. This should not happen.");
            assertNotNull(methodStat.getSimple("maxTime"),
                "Couldn't find 'maxTime' method stat. This should not happen.");
            
            return true;
        }
        
        return false;
    }
    
    protected abstract String getTestedBeanName();
    
    protected abstract Object getRemoteBean() throws Exception;    

    protected abstract String getExpectedResourceKey();

    protected abstract String getTestedMethodName();

    protected String getExpectedMethodName() {
        return getTestedMethodName();
    }
    
    protected abstract MethodArgDef[] getTestedMethodArgs();

    protected int getTestedMethodExpectedInvocationCount() {
        return 10;
    }
}
