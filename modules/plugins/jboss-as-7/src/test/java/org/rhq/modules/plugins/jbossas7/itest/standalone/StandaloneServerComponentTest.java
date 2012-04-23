/*
 * RHQ Management Platform
 * Copyright (C) 2012 Red Hat, Inc.
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

import java.io.File;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.Test;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.configuration.ListPropertySimpleWrapper;
import org.rhq.modules.plugins.jbossas7.itest.AbstractServerComponentTest;
import org.rhq.test.arquillian.RunDiscovery;

import static org.testng.Assert.*;

/**
 * Test discovery and facets of the "JBossAS7 Standalone Server" Resource type.
 *
 * @author Ian Springer
 */
@Test(groups = {"integration", "pc", "standalone"}, singleThreaded = true)
public class StandaloneServerComponentTest extends AbstractServerComponentTest {

    public static final ResourceType RESOURCE_TYPE =
            new ResourceType("JBossAS7 Standalone Server", PLUGIN_NAME, ResourceCategory.SERVER, null);
    // The key is the server's base dir.
    public static final String RESOURCE_KEY = new File(JBOSS_HOME, "standalone").getPath();

    private static final String RELOAD_OPERATION_NAME = "reload";
    private static final String RESTART_OPERATION_NAME = "restart";

    @Override
    protected ResourceType getServerResourceType() {
        return RESOURCE_TYPE;
    }

    @Override
    protected String getServerResourceKey() {
        return RESOURCE_KEY;
    }

    @Override
    protected String getBindAddressSystemPropertyName() {
        return "jboss.standalone.bindAddress";
    }

    @Override
    protected String getPortOffsetSystemPropertyName() {
        return "jboss.standalone.portOffset";
    }

    @Test(priority = 1, groups = "discovery")
    @RunDiscovery
    public void testStandaloneServerDiscovery() throws Exception {
        super.testAutoDiscovery();
    }

    @Override
    protected void validatePluginConfiguration(Configuration pluginConfig) {
        super.validatePluginConfiguration(pluginConfig);

        // "startScript" prop
        String startScript = pluginConfig.getSimpleValue("startScript");
        String expectedStartScriptFileName = (File.separatorChar == '/') ? "standalone.sh" : "standalone.bat";
        String expectedStartScript = new File("bin", expectedStartScriptFileName).getPath();
        Assert.assertEquals(startScript, expectedStartScript);

        // "startScriptArgs" prop
        PropertySimple startScriptArgsProp = pluginConfig.getSimple("startScriptArgs");
        ListPropertySimpleWrapper startScriptArgsPropWrapper = new ListPropertySimpleWrapper(startScriptArgsProp);
        List<String> args = startScriptArgsPropWrapper.getValue();

        Assert.assertEquals(args.size(), 5, args.toString());

        Assert.assertEquals(args.get(0), "--server-config=standalone-full.xml");
        Assert.assertEquals(args.get(1), "-Djboss.bind.address.management=127.0.0.1");
        Assert.assertEquals(args.get(2), "-Djboss.bind.address=127.0.0.1");
        Assert.assertEquals(args.get(3), "-Djboss.bind.address.unsecure=127.0.0.1");
        Assert.assertEquals(args.get(4), "-Djboss.socket.binding.port-offset=40000");
    }

    @Test(priority = 2)
    public void testStandaloneServerPluginConfiguration() throws Exception {
        return;
    }

    // ******************************* METRICS ******************************* //
    @Override
    @Test(priority = 3, enabled = true)
    public void testReleaseVersionTrait() throws Exception {
        super.testReleaseVersionTrait();
    }

    // ******************************* OPERATIONS ******************************* //
    @Test(priority = 4)
    public void testReloadOperation() throws Exception {
        invokeOperationAndAssertSuccess(getServerResource(), RELOAD_OPERATION_NAME, null);
    }

    @Test(priority = 5, enabled = true)
    public void testStandaloneServerShutdownAndStartOperations() throws Exception {
        super.testShutdownAndStartOperations();
    }

    // TODO: Re-enable once fixed.
    @Test(priority = 5, dependsOnMethods = "testStandaloneServerShutdownAndStartOperations", enabled = false)
    public void testRestartOperation() throws Exception {
        // First make sure the server is up.
        AvailabilityType avail = getAvailability(getServerResource());
        assertEquals(avail, AvailabilityType.UP);

        // Make sure the server is back up.
        // TODO (ips): Check that the server is a different process now.
        invokeOperationAndAssertSuccess(getServerResource(), RESTART_OPERATION_NAME, null);
        avail = getAvailability(getServerResource());
        assertEquals(avail, AvailabilityType.UP);
    }

    @AfterSuite
    public void killServerProcesses() {
        super.killServerProcesses();
    }

    @Override
    protected int getPortOffset() {
        return 40000;
    }

}
