/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.modules.plugins.jbossas7.itest.domain;

import java.util.ArrayList;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.Test;

import org.rhq.core.domain.resource.Resource;
import org.rhq.modules.plugins.jbossas7.itest.AbstractJBossAS7PluginTest;
import org.rhq.test.arquillian.RunDiscovery;

/**
 * @author Stefan Negrea
 */
@Test(groups = { "integration", "pc", "domain" }, singleThreaded = true)
public class ResourcesDomainServerTest extends AbstractJBossAS7PluginTest {
    private Resource platform;
    private Resource server;

    @Test(priority = 10, groups = "discovery")
    @RunDiscovery(discoverServices = true, discoverServers = true)
    public void initialDiscoveryTest() throws Exception {
        platform = validatePlatform();
        server = waitForResourceByTypeAndKey(platform, platform, DomainServerComponentTest.RESOURCE_TYPE,
            DomainServerComponentTest.RESOURCE_KEY);
        waitForAsyncDiscoveryToStabilize(server);
    }

    @Test(priority = 1031)
    public void loadUpdateResourceConfiguration() throws Exception {
        List<String> ignoredResources = new ArrayList<String>();

        //ignored because of differences between test plugin container and real application
        //works well with real agent
        ignoredResources.add("VHost (Profile)");
        ignoredResources.add("VHost (Managed Server)");

        //created JIRA AS7-5011
        //server is started with the configuration but unable to write it back as is
        //due to marshaling error
        ignoredResources.add("Network Interface");

        //created JIRA AS7-5012
        //default value for  is float but the resource only accepts integers
        ignoredResources.add("Domain Load Metric");

        //will revisit after BZ 826542 is resolved
        //ignoredResources.add("Authentication (Classic)");

        //tested separately
        ignoredResources.add("SocketBindingGroup");

        //Update requirements validate separately
        ignoredResources.add("Pooled Connection Factory (Profile)");
        ignoredResources.add("Connection Factory (Profile)");
        ignoredResources.add("Pooled Connection Factory (Managed Server)");
        ignoredResources.add("Connection Factory (Managed Server)");
        ignoredResources.add("Memory Pool");
        ignoredResources.add("Periodic Rotating File Handler");
        ignoredResources.add("Console Handler");

        // Datasources need a complex workflow, cannot be tested like this
        ignoredResources.add("DataSource (Profile)");
        ignoredResources.add("DataSource (Managed)");

        if (System.getProperty("as7.version").equals("6.1.0.Alpha")) {
            // HornetQ resource is broken on 6.1.0.Alpha. Operation fails with:
            // JBAS011673: The clustered attribute is deprecated.
            // To create a clustered HornetQ server, define at least one cluster-connection
            ignoredResources.add("HornetQ (Profile)");
        }

        if (System.getProperty("as7.version").startsWith("6.0") || System.getProperty("as7.version").startsWith("6.1")) {
            // Cluster Connection resources cannot be modified on EAP 6.0 or 6.1, only deleted/re-created
            // See Bug 1001612 https://bugzilla.redhat.com/show_bug.cgi?id=1001612#c0
            ignoredResources.add("Cluster Connection (Profile)");
        }

        int errorCount = loadUpdateConfigChildResources(server, ignoredResources);
        Assert.assertEquals(errorCount, 0);
    }

    @Test(priority = 1032)
    public void executeNoArgOperations() throws Exception {
        List<String> ignoredSubsystems = new ArrayList<String>();

        List<String> ignoredOperations = new ArrayList<String>();
        //ignored because there is no other server to fail-over to
        ignoredOperations.add("subsystem:force-failover");
        //ignored because this is not a true operation, it is handled
        //internally by a configuration property change
        ignoredOperations.add("enable");
        //ignored because the Osgi subsystem not configured out of box
        ignoredOperations.add("subsystem:activate");

        //ignored operations because they will stop the managed server and
        //the domain controller itself
        ignoredOperations.add("stop");
        ignoredOperations.add("restart");
        ignoredOperations.add("shutdown");
        ignoredOperations.add("stop-servers");
        ignoredOperations.add("restart-servers");

        executeNoArgOperations(server, ignoredSubsystems, ignoredOperations);
    }
}
