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

import org.testng.annotations.Test;

import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.util.FileUtils;
import org.rhq.modules.plugins.jbossas7.itest.AbstractServerComponentTest;
import org.rhq.test.arquillian.RunDiscovery;

/**
 * Test discovery and facets of the "JBossAS7 Standalone Server" Resource type.
 *
 * @author Ian Springer
 */
@Test(groups = "standalone", singleThreaded = true)
public class StandaloneServerComponentTest extends AbstractServerComponentTest {

    public static final ResourceType RESOURCE_TYPE = new ResourceType("JBossAS7 Standalone Server", PLUGIN_NAME, ResourceCategory.SERVER, null);
    // The key of an AS7 Standalone Server Resource is its JBOSS_HOME dir.
    public static final String RESOURCE_KEY = FileUtils.getCanonicalPath(System.getProperty("jboss.home"));

    private static final String SERVER_STATE_TRAIT_NAME = "server-state";
    private static final String RELEASE_CODENAME_TRAIT_NAME = "release-codename";
    private static final String PRODUCT_NAME_TRAIT_NAME = "product-name";
    private static final String PRODUCT_VERSION_TRAIT_NAME = "product-version";
    private static final String START_TIME_TRAIT_NAME = "startTime";

    private static final String RELOAD_OPERATION_NAME = "reload";
    private static final String SHUTDOWN_OPERATION_NAME = "shutdown";
    private static final String START_OPERATION_NAME = "start";
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
    @Test
    @RunDiscovery
    public void testAutoDiscovery() throws Exception {
        super.testAutoDiscovery();
    }

    // ******************************* TRAITS ******************************* //
    @Test(dependsOnMethods = "testAutoDiscovery")
    public void testServerStateTrait() throws Exception {
        System.out.println("\n\n********* Running " + getClass().getSimpleName() + ".testServerStateTrait...");
        collectTraitAndAssertNotNull(getServerResource(), SERVER_STATE_TRAIT_NAME);
    }

    @Test(dependsOnMethods = "testAutoDiscovery")
    public void testReleaseCodenameTrait() throws Exception {
        System.out.println("\n\n********* Running " + getClass().getSimpleName() + ".testReleaseCodenameTrait...");
        collectTraitAndAssertNotNull(getServerResource(), RELEASE_CODENAME_TRAIT_NAME);
    }

    @Test(dependsOnMethods = "testAutoDiscovery")
    public void testReleaseVersionTrait() throws Exception {
        super.testReleaseVersionTrait();
    }

    @Test(dependsOnMethods = "testAutoDiscovery")
    public void testProductNameTrait() throws Exception {
        System.out.println("\n\n********* Running " + getClass().getSimpleName() + ".testProductNameTrait...");
        collectTraitAndAssertNotNull(getServerResource(), PRODUCT_NAME_TRAIT_NAME);
    }

    @Test(dependsOnMethods = "testAutoDiscovery")
    public void testProductVersionTrait() throws Exception {
        System.out.println("\n\n********* Running " + getClass().getSimpleName() + ".testProductVersionTrait...");
        collectTraitAndAssertNotNull(getServerResource(), PRODUCT_VERSION_TRAIT_NAME);
    }

    @Test(dependsOnMethods = "testAutoDiscovery")
    public void testStartTimeTrait() throws Exception {
        System.out.println("\n\n********* Running " + getClass().getSimpleName() + ".testStartTimeTrait...");
        collectTraitAndAssertNotNull(getServerResource(), START_TIME_TRAIT_NAME);
    }

    // ******************************* OPERATIONS ******************************* //
    // TODO: Re-enable once fixed.
    @Test(dependsOnMethods = "testAutoDiscovery", enabled = false)
    public void testReloadOperation() throws Exception {
        System.out.println("\n\n********* Running " + getClass().getSimpleName() + ".testReloadOperation...");
        invokeOperationAndAssertSuccess(getServerResource(), RELOAD_OPERATION_NAME, null);
    }

    // TODO: Re-enable this once "shutdown" operation has been fixed.
    @Test(dependsOnMethods = "testAutoDiscovery", enabled = false)
    public void testShutdownOperation() throws Exception {
        System.out.println("\n\n********* Running " + getClass().getSimpleName() + ".testShutdownOperation...");
        invokeOperationAndAssertSuccess(getServerResource(), SHUTDOWN_OPERATION_NAME, null);
        // Restart the server, so the rest of the tests don't fail.
        testStartOperation();
    }

    // TODO: Re-enable this once "shutdown" operation has been fixed.
    @Test(dependsOnMethods = "testShutdownOperation", enabled = false)
    public void testStartOperation() throws Exception {
        System.out.println("\n\n********* Running " + getClass().getSimpleName() + ".testStartOperation...");
        invokeOperationAndAssertSuccess(getServerResource(), START_OPERATION_NAME, null);
    }

    // TODO: Re-enable once fixed.
    @Test(dependsOnMethods = "testAutoDiscovery", enabled = false)
    public void testRestartOperation() throws Exception {
        System.out.println("\n\n********* Running " + getClass().getSimpleName() + ".testRestartOperation...");
        invokeOperationAndAssertSuccess(getServerResource(), RESTART_OPERATION_NAME, null);
    }

}
