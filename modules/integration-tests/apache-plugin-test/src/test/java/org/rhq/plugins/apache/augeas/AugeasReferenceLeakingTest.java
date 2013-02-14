/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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

package org.rhq.plugins.apache.augeas;

import static org.testng.Assert.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jmock.Expectations;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import org.jboss.byteman.contrib.bmunit.BMNGRunner;
import org.jboss.byteman.contrib.bmunit.BMRule;
import org.jboss.byteman.contrib.bmunit.BMRules;

import org.rhq.core.clientapi.agent.PluginContainerException;
import org.rhq.core.clientapi.server.discovery.InventoryReport;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.discovery.AvailabilityReport;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.ServerServices;
import org.rhq.core.pc.configuration.ConfigurationManager;
import org.rhq.core.pc.inventory.InventoryManager;
import org.rhq.core.pc.inventory.ResourceContainer;
import org.rhq.core.pc.upgrade.FakeServerInventory;
import org.rhq.plugins.apache.ApacheServerComponent;
import org.rhq.plugins.apache.PluginLocation;
import org.rhq.plugins.apache.setup.ApacheTestConfiguration;
import org.rhq.plugins.apache.setup.ApacheTestSetup;
import org.rhq.plugins.apache.upgrade.UpgradeTestBase;
import org.rhq.plugins.apache.util.ResourceTypes;
import org.rhq.test.pc.PluginContainerSetup;
import org.rhq.test.pc.PluginContainerTest;

/**
 *
 *
 * @author Lukas Krejci
 */
@Listeners(PluginContainerTest.class)
@Test
public class AugeasReferenceLeakingTest extends BMNGRunner {

    private ApacheTestSetup setup;

    @BeforeMethod
    public void clearCreateAndCloseTracker() {
        CreateAndCloseTracker.clear();
    }

    @SuppressWarnings("unchecked")
    @PluginContainerSetup(plugins = { PluginLocation.PLATFORM_PLUGIN, PluginLocation.AUGEAS_PLUGIN,
        PluginLocation.APACHE_PLUGIN })
    @BMRules(
        rules = {
            @BMRule(name = "increment reference count on Augeas init", targetClass = "net.augeas.Augeas",
                targetMethod = "<init>(String, String, int)",
                helper = "org.rhq.plugins.apache.augeas.CreateAndCloseTracker",
                action = "recordCreate($0, formatStack())"),
            @BMRule(name = "decrement reference count on Augeas close", targetClass = "net.augeas.Augeas",
                targetMethod = "close()", helper = "org.rhq.plugins.apache.augeas.CreateAndCloseTracker",
                action = "recordClose($0, formatStack())") })
    @Parameters({ "apache2.install.dir", "apache2.exe.path",
        "AugeasReferenceLeakingTest.configurationReadingInvocationCount" })
    public void testReadingConfigurationsDoesNotLeakAugeasReferences(final String installDir, final String exePath,
        int configurationReadingInvocationCount) throws Exception {
        final FakeServerInventory fakeInventory = new FakeServerInventory();
        PluginContainerTest.getCurrentMockContext().checking(new Expectations() {
            {
                ServerServices ss = PluginContainerTest.getCurrentPluginContainerConfiguration().getServerServices();

                allowing(ss.getDiscoveryServerService()).mergeInventoryReport(with(any(InventoryReport.class)));
                will(fakeInventory.mergeInventoryReport(InventoryStatus.COMMITTED));

                allowing(ss.getDiscoveryServerService()).getResources(with(any(Set.class)), with(any(boolean.class)));
                will(fakeInventory.getResources());

                allowing(ss.getDiscoveryServerService()).mergeAvailabilityReport(with(any(AvailabilityReport.class)));
                allowing(ss.getDiscoveryServerService()).postProcessNewlyCommittedResources(with(any(Set.class)));

                allowing(ss.getDiscoveryServerService()).setResourceEnablement(with(any(int.class)),
                    with(any(boolean.class)));

                ignoring(ss.getBundleServerService());
                ignoring(ss.getConfigurationServerService());
                ignoring(ss.getContentServerService());
                ignoring(ss.getCoreServerService());
                ignoring(ss.getEventServerService());
                ignoring(ss.getMeasurementServerService());
                ignoring(ss.getOperationServerService());
                ignoring(ss.getResourceFactoryServerService());
            }
        });

        boolean apacheStarted = false;
        try {
            apacheStarted = startApache(installDir, exePath);

            PluginContainerTest.startConfiguredPluginContainer();

            configureApacheServerToUseAugeas();

            PluginContainer pc = PluginContainer.getInstance();
            Resource apacheServer = findApacheServerResource().getResource();

            for (int i = 0; i < configurationReadingInvocationCount; ++i) {
                checkApacheServerConfigurationRecursively(apacheServer, pc.getConfigurationManager());
                Thread.sleep(10000);
            }

            //wait a couple of seconds for the loadConfig calls to finish
            Thread.sleep(60000);
        } finally {
            if (apacheStarted) {
                stopApache();
            }
        }
    }

    @AfterMethod
    public void checkForLeaksAndMultiCloses() {
        List<String> leaks = CreateAndCloseTracker.getCreateLocationsWithoutClose();
        Map<String, List<String>> multiCloses = CreateAndCloseTracker.getMultiplyClosingLocations();

        assertEquals(leaks.size(), 0,
            "There were Augeas instances without a close() call on them created at the following locations: " + leaks);

        assertEquals(
            multiCloses.size(),
            0,
            "Each key in the map is a location where Augeas was created and the values are the lists of locations where that instance was closed: "
                + multiCloses);
    }

    private boolean startApache(final String installDir, final String exePath) throws Exception {
        ApacheTestConfiguration apacheConfig = new ApacheTestConfiguration() {
            {
                serverRoot = installDir;
                binPath = exePath;
                configurationName = "augeas-leak-test-config";
                apacheConfigurationFiles = new String[] { "/augeas-leak-test-config/httpd.conf", "/snmpd.conf" };
                inventoryFile = null;
            }
        };

        setup = new ApacheTestSetup(this.getClass().getSimpleName()
            + "#testReadingConfigurationsDoesNotLeakAugeasReferences", apacheConfig.configurationName,
            PluginContainerTest.getCurrentMockContext(),
            new ResourceTypes(PluginLocation.APACHE_PLUGIN));

        Resource platform = UpgradeTestBase.discoverPlatform();

        setup.withInventoryFrom(apacheConfig.inventoryFile).withPlatformResource(platform)
            .withDefaultOverrides(apacheConfig.defaultOverrides).withApacheSetup()
            .withConfigurationFiles(apacheConfig.apacheConfigurationFiles).withServerRoot(apacheConfig.serverRoot)
            .withExePath(apacheConfig.binPath);

        setup.setup();

        return true;
    }

    private void stopApache() throws Exception {
        if (setup != null) {
            setup.withApacheSetup().stopApache();
            setup = null;
        }
    }

    private void checkApacheServerConfigurationRecursively(Resource resource, ConfigurationManager cm)
        throws PluginContainerException {
        if (resource.getResourceType().getResourceConfigurationDefinition() != null) {
            cm.loadResourceConfiguration(resource.getId());
        }

        for (Resource child : resource.getChildResources()) {
            checkApacheServerConfigurationRecursively(child, cm);
        }
    }

    private void configureApacheServerToUseAugeas() throws Exception {
        ResourceTypes resourceTypes = new ResourceTypes(PluginLocation.APACHE_PLUGIN);

        InventoryManager im = PluginContainer.getInstance().getInventoryManager();

        ResourceContainer apacheServer = findApacheServerResource();

        Configuration config = apacheServer.getResourceContext().getPluginConfiguration();

        config.getSimple("augeasEnabled").setValue("yes");

        im.updatePluginConfiguration(apacheServer.getResource().getId(), config);

        //and run discovery so that the new resources can go into inventory

        im.executeServiceScanImmediately();
    }

    private ResourceContainer findApacheServerResource() throws Exception {
        InventoryManager im = PluginContainer.getInstance().getInventoryManager();
        ResourceTypes resourceTypes = new ResourceTypes(PluginLocation.APACHE_PLUGIN);
        ResourceType apacheServerResourceType = resourceTypes.findByName("Apache HTTP Server");

        return findApacheServerResource(im, apacheServerResourceType, im.getPlatform());
    }

    private ResourceContainer findApacheServerResource(InventoryManager im, ResourceType rt, Resource root) {
        if (root.getResourceType().equals(rt)
            && root.getPluginConfiguration().getSimpleValue(ApacheServerComponent.PLUGIN_CONFIG_PROP_SERVER_ROOT)
                .equals(setup.getDeploymentConfig().serverRoot)) {
            return im.getResourceContainer(root);
        }

        for (Resource child : root.getChildResources()) {
            ResourceContainer rc = findApacheServerResource(im, rt, child);
            if (rc != null) {
                return rc;
            }
        }

        return null;
    }
}
