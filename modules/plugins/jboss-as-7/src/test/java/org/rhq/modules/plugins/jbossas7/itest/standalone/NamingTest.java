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

package org.rhq.modules.plugins.jbossas7.itest.standalone;


import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import org.testng.annotations.Test;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pc.inventory.InventoryManager;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.modules.plugins.jbossas7.itest.AbstractJBossAS7PluginTest;
import org.rhq.test.arquillian.RunDiscovery;

/**
 * Test stuff around naming subsystem
 * This could actually also run for domain mode
 * @author Heiko W. Rupp
 */
@Test(groups = {"integration", "pc", "standalone"}, singleThreaded = true)
public class NamingTest extends AbstractJBossAS7PluginTest {

    public static final ResourceType RESOURCE_TYPE = new ResourceType("Naming", PLUGIN_NAME, ResourceCategory.SERVICE, null);
    private static final String RESOURCE_KEY = "subsystem=naming";

    @Test(priority = 10,groups = "discovery")
    @RunDiscovery(discoverServices = true, discoverServers = true)
    public void doSomeDiscovery() throws Exception {
        Resource platform = this.pluginContainer.getInventoryManager().getPlatform();
        assertNotNull(platform);
        assertEquals(platform.getInventoryStatus(), InventoryStatus.COMMITTED);
    }

    @Test(priority = 11)
    public void runJndiView() throws Exception {
        OperationResult result = invokeOperationAndAssertSuccess(getResource(),"jndi-view",null);
        Configuration configuration = result.getComplexResults();
        assert configuration != null;
        assert !configuration.getProperties().isEmpty();
        assert configuration.getProperties().size()==2 : "Expected 2 groups, but got " + configuration.getProperties().size();
        PropertyList javaProps = configuration.getList("java-contexts");
        assert javaProps != null;
        assert !javaProps.getList().isEmpty();

        // That property is probably empty, as by default no application should be shipped
        PropertyList appProps = configuration.getList("applications");
        assert appProps !=null;

    }


    private Resource getResource() {

        InventoryManager im = pluginContainer.getInventoryManager();
        Resource platform = im.getPlatform();
        Resource server = getResourceByTypeAndKey(platform,StandaloneServerComponentTest.RESOURCE_TYPE,StandaloneServerComponentTest.RESOURCE_KEY);
        Resource bindings = getResourceByTypeAndKey(server,RESOURCE_TYPE,RESOURCE_KEY);
        return bindings;
    }

}
