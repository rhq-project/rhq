/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.util

import java.lang.reflect.Method
import org.jboss.seam.ScopeType
import org.jboss.seam.contexts.BasicContext
import org.jboss.seam.contexts.Context
import org.jboss.seam.core.Init
import org.rhq.enterprise.gui.util.LookupComponent.LookupUtilComponent
import org.rhq.enterprise.server.util.LookupUtil
import org.testng.AssertJUnit
import org.testng.annotations.BeforeClass
import org.testng.annotations.DataProvider
import org.testng.annotations.Test

public class LookupComponentTest {

    def lookupComponent
    def contextMap

    @BeforeClass
    void setUp() {
        contextMap = [:]

        lookupComponent = new LookupComponent()
        lookupComponent.init = new Init()
        lookupComponent.applicationContext = new BasicContext(ScopeType.APPLICATION, contextMap)

        lookupComponent.scanComponents()
    }

    @DataProvider(name="components")
    Object[][] createExpectedComponents() {
        // Note: these are just arbitrary - feel free to add to the list
        [
            [ "activeConditionProducer.component", "activeConditionProducer", getMethod("getActiveConditionProducer") ],
            [ "activeConditionConsumer.component", "activeConditionConsumer", getMethod("getActiveConditionConsumer") ],
            [ "agentManager.component", "agentManager", getMethod("getAgentManager") ],
            [ "alertConditionCacheManager.component", "alertConditionCacheManager", getMethod("getAlertConditionCacheManager") ],
            [ "alertConditionManager.component", "alertConditionManager", getMethod("getAlertConditionManager") ],
            [ "alertDefinitionManager.component", "alertDefinitionManager", getMethod("getAlertDefinitionManager") ],
            [ "alertSubsystemManager.component", "alertSubsystemManager", getMethod("getAlertSubsystemManager") ],
            [ "configurationManager.component", "configurationManager", getMethod("getConfigurationManager") ],
            [ "emailManagerBean.component", "emailManagerBean", getMethod("getEmailManagerBean") ],
            [ "resourceManager.component", "resourceManager", getMethod("getResourceManager") ],
        ]
    }

    def getMethod(String methodName) {
        return LookupUtil.class.getMethod(methodName)
    }

    @Test(dataProvider="components")
    void componentExists(def expectedComponentName, def expectedName, def expectedMethod) {
        def component = contextMap[expectedComponentName]

        AssertJUnit.assertNotNull("The component: '" + expectedComponentName + "' should be in the application context.", component)
    }

    @Test(dataProvider="components")
    void componentName(def expectedComponentName, def expectedName, def expectedMethod) {
        def component = contextMap[expectedComponentName]

        AssertJUnit.assertEquals("Created component should be named:  " + expectedName, expectedName, component.getName())
    }

    @Test(dataProvider="components")
    void componentMethod(def expectedComponentName, def expectedName, def expectedMethod) {
        def component = contextMap[expectedComponentName]

        AssertJUnit.assertEquals("The method:  " + expectedMethod.toGenericString() + " should be called to create component:  " + expectedName,
                expectedMethod, component.getCreationMethod())
    }

}