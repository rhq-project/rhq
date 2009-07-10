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
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.fail;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.testng.annotations.BeforeGroups;

/**
 * 
 * @author Lukas Krejci
 */
public abstract class AbstractSessionBeanTest extends AbstractEjb2ResourceTest {

    @BeforeGroups(groups = "as5-plugin")
    public void setupBean() {
        try {
            Object bean = createRemoteBean(getTestedBeanName(), getEjbCreateMethodArgs());

            //call some methods so that we get the stats back
            MethodArgDef[] args = getTestedMethodArgs();

            for (int i = 0; i < getTestedMethodExpectedInvocationCount(); ++i)
                invokeMethod(getTestedMethodName(), bean, args);
        } catch (Exception e) {
            fail("Failed to setup the remote EJB2 test bean.", e);
        }
    }

    @Override
    protected void validateOperationResult(String name, OperationResult result, Resource resource) {
        if ("viewInvocationStats".equals(name) && resource.getResourceKey().contains(getExpectedResourceKey())) {
            //the method was invoked ten times in the setup. we should see it in the results here...
            Configuration resultValueConfig = result.getComplexResults();

            assertNotNull(resultValueConfig, "viewInvocationStats results shouldn't be null.");

            PropertyList propertyList = resultValueConfig.getList("methods");

            assertNotNull(propertyList, "viewInvocationStats must include a \"methods\" property list.");
            assertEquals(propertyList.getList().size(), 1,
                "the viewInvocationStats should contain exactly one method statistics");

            PropertyMap methodStat = (PropertyMap) propertyList.getList().iterator().next();

            assertEquals(methodStat.getSimpleValue("methodName", null), getTestedMethodName(),
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
        } else {
            super.validateOperationResult(name, result, resource);
        }
    }

    protected Configuration getTestResourceConfiguration() {
        //there is no resource level configuration for session beans.
        return new Configuration();
    }

    protected abstract String getExpectedResourceKey();

    protected abstract String getTestedBeanName();

    protected abstract String getTestedMethodName();

    protected int getTestedMethodExpectedInvocationCount() {
        return 10;
    }

    protected abstract MethodArgDef[] getEjbCreateMethodArgs();

    protected abstract MethodArgDef[] getTestedMethodArgs();
}
