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
package org.rhq.modules.plugins.jbossas7.itest.domain;

import java.io.File;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.Test;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.configuration.ListPropertySimpleWrapper;
import org.rhq.modules.plugins.jbossas7.itest.AbstractServerComponentTest;
import org.rhq.test.arquillian.RunDiscovery;

/**
 * Test discovery and facets of the "JBossAS7 Host Controller" Resource type.
 *
 * @author Ian Springer
 */
@Test(groups = {"integration", "pc", "domain"}, singleThreaded = true)
public class DomainServerComponentTest extends AbstractServerComponentTest {

    public static final ResourceType RESOURCE_TYPE =
            new ResourceType("JBossAS7 Host Controller", PLUGIN_NAME, ResourceCategory.SERVER, null);
    // The key is the server's base dir.
    public static final String RESOURCE_KEY = new File(JBOSS_HOME, "domain").getPath();

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
        return "jboss.domain.bindAddress";
    }

    @Override
    protected String getPortOffsetSystemPropertyName() {
        return "jboss.domain.portOffset";
    }

    @Test(priority = 1000, groups = "discovery")
    @RunDiscovery
    public void testDomainServerDiscovery() throws Exception {
        super.testAutoDiscovery();
    }

    @Override
    protected void validatePluginConfiguration(Configuration pluginConfig) {
        super.validatePluginConfiguration(pluginConfig);

        // "startScript" prop
        String startScript = pluginConfig.getSimpleValue("startScript");
        String expectedStartScriptFileName = (File.separatorChar == '/') ? "domain.sh" : "domain.bat";
        String expectedStartScript = new File("bin", expectedStartScriptFileName).getPath();
        Assert.assertEquals(startScript, expectedStartScript);

        // "startScriptArgs" prop
        PropertySimple startScriptArgsProp = pluginConfig.getSimple("startScriptArgs");
        ListPropertySimpleWrapper startScriptArgsPropWrapper = new ListPropertySimpleWrapper(startScriptArgsProp);
        List<String> args = startScriptArgsPropWrapper.getValue();

        Assert.assertEquals(args.size(), 7, args.toString());

        Assert.assertEquals(args.get(0), "-Djboss.bind.address.management=127.0.0.1");
        Assert.assertEquals(args.get(1), "-Djboss.bind.address=127.0.0.1");
        Assert.assertEquals(args.get(2), "-Djboss.bind.address.unsecure=127.0.0.1");
        Assert.assertEquals(args.get(3), "-Djboss.socket.binding.port-offset=50000");
        Assert.assertEquals(args.get(4), "-Djboss.management.native.port=59999");
        Assert.assertEquals(args.get(5), "-Djboss.management.http.port=59990");
        Assert.assertEquals(args.get(6), "-Djboss.management.https.port=59943");
    }

    // ******************************* METRICS ******************************* //
    @Override
    @Test(priority = 1002, enabled = true)
    public void testReleaseVersionTrait() throws Exception {
        super.testReleaseVersionTrait();
    }

    // ******************************* OPERATIONS ******************************* //
    @Test(priority = 1003, enabled = true)
    public void testDomainServerShutdownAndStartOperations() throws Exception {
        super.testShutdownAndStartOperations();
    }

    @AfterSuite
    public void killServerProcesses() {
        super.killServerProcesses();
    }

    @Override
    protected int getPortOffset() {
        return 50000;
    }

}
