/*
 * RHQ Management Platform
 * Copyright (C) 2013 Red Hat, Inc.
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

package org.rhq.test.arquillian;

import java.util.Arrays;
import java.util.HashSet;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;

import org.rhq.core.clientapi.server.discovery.InventoryReport;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.pc.PluginContainerConfiguration;
import org.rhq.test.shrinkwrap.RhqAgentPluginArchive;

/**
 * @author Lukas Krejci
 */
public class CleanUpTest extends Arquillian {

    @Deployment(name = "1")
    @TargetsContainer("cleaned-up-pc")
    public static RhqAgentPluginArchive getDeepTestPlugin() {
        return ShrinkWrap.create(RhqAgentPluginArchive.class, "test-cleanup-deep-plugin-1.0.0.jar")
            .addClasses(TestDiscoveryComponent.class, TestResourceComponent.class)
            .setPluginDescriptor("test-deep-rhq-plugin.xml");
    }

    @Deployment(name = "2")
    @TargetsContainer("cleaned-up-pc")
    public static RhqAgentPluginArchive getTestPlugin() {
        return ShrinkWrap.create(RhqAgentPluginArchive.class, "test-cleanup-plugin-1.0.0.jar")
            .addClasses(TestDiscoveryComponent.class, TestResourceComponent.class)
            .setPluginDescriptor("test-rhq-plugin.xml");
    }

    @ArquillianResource
    private PluginContainerConfiguration pluginContainerConfig;

    @ArquillianResource
    private MockingServerServices serverServices;

    private FakeServerInventory fakeServerInventory;
    private FakeServerInventory.CompleteDiscoveryChecker discoveryCompleteChecker;

    private void setupDiscoveryServerMocks(int expectedDiscoveryDepth) throws Exception {
        serverServices.resetMocks();
        fakeServerInventory = new FakeServerInventory();
        discoveryCompleteChecker = fakeServerInventory.createAsyncDiscoveryCompletionChecker(expectedDiscoveryDepth);
        //autoimport everything
        when(serverServices.getDiscoveryServerService().mergeInventoryReport(any(InventoryReport.class))).then(
            fakeServerInventory.mergeInventoryReport(InventoryStatus.COMMITTED));
    }

    @BeforeDiscovery(testMethods = {"testCleanAll", "testClearingAfterTest", "checkDiscoveryCanRunFullBecauseInventoryClear"})
    public void setupDiscoveryMocksWithCleanInventory() throws Exception {
        setupDiscoveryServerMocks(3);
    }

    @BeforeDiscovery(testMethods = "testCleanAllButInventoryDat")
    public void setupDiscoveryMocksWithInventory() throws Exception {
        //do nothing here... we want the faked server to pretend the stuff was left in it.
        //we don't want the agent to think that it has obsolete resources which would cause it
        //to stop the resources from the persisted inventory, which would cause our marker files
        //to be put on the filesystem and fail the test.
    }

    @AfterDiscovery(testMethods = {"testCleanAll", "testClearingAfterTest", "checkDiscoveryCanRunFullBecauseInventoryClear"})
    public void waitForAsyncDiscoveries() throws Exception {
        if (discoveryCompleteChecker != null) {
            discoveryCompleteChecker.waitForDiscoveryComplete();
        }
    }

    @RunDiscovery
    @ClearPersistedData
    @Test
    public void testCleanAll() {
        String[] files = pluginContainerConfig.getDataDirectory().list();
        assertExpected(files, new String[]{}, "No other files should be in the data dir.");
    }

    @RunDiscovery
    @ClearPersistedData(ofInventory = false)
    @Test(dependsOnMethods = "testCleanAll")
    public void testCleanAllButInventoryDat() {
        String[] files = pluginContainerConfig.getDataDirectory().list();
        assertExpected(files, new String[]{"inventory.dat"}, "No other files should be in the data dir.");
    }

    @RunDiscovery
    @ClearPersistedData(ofPlugins = {})
    @Test(dependsOnMethods = "testCleanAllButInventoryDat")
    public void testCleanJustInventoryDat() {
        String[] files = pluginContainerConfig.getDataDirectory().list();

        assertExpected(files, new String[]{"testDeepPlugin", "testPlugin"},
            "No other files should be in the data dir.");
    }

    @ClearPersistedData(when = {When.AFTER_TEST})
    @Test(dependsOnMethods = "testCleanJustInventoryDat")
    public void testClearingAfterTest() {
        //nothing to be done here... we just want stuff to be cleared after this test
        //and actually check in the next test that it was successful
    }

    @RunDiscovery
    @Test(dependsOnMethods = "testClearingAfterTest")
    public void checkClearAfterTestWorked() {
        String[] files = pluginContainerConfig.getDataDirectory().list();
        assertExpected(files, new String[]{}, "No other files should be in the data dir.");
    }

    private void assertExpected(String[] actualFiles, String[] expectedFiles, String message) {
        HashSet<Object> sActual = new HashSet<Object>(Arrays.asList(actualFiles));
        HashSet<Object> sExpected = new HashSet<Object>(Arrays.asList(expectedFiles));

        //the "changesets" directory is going to always be there because it is eagerly created during PC startup.
        sExpected.add("changesets");

        //the "rc" directory is probably going to be there because it contains compacted resource configs
        sActual.remove("rc");

        assertEquals(sActual, sExpected, message);
    }
}
