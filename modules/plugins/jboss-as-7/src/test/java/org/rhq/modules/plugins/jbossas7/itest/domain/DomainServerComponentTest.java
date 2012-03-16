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

import org.testng.annotations.Test;

import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.modules.plugins.jbossas7.itest.AbstractServerComponentTest;
import org.rhq.test.arquillian.RunDiscovery;

/**
 * Test discovery and facets of the "JBossAS7 Host Controller" Resource type.
 *
 * @author Ian Springer
 */
@Test(groups = "domain", singleThreaded = true)
public class DomainServerComponentTest extends AbstractServerComponentTest {

    public static final ResourceType RESOURCE_TYPE = new ResourceType("JBossAS7 Host Controller", PLUGIN_NAME, ResourceCategory.SERVER, null);
    public static final String RESOURCE_KEY = "DomainController";

    private static final String RELEASE_CODENAME_TRAIT_NAME = "release-codename";
    private static final String PRODUCT_NAME_TRAIT_NAME = "product-name";
    private static final String PRODUCT_VERSION_TRAIT_NAME = "product-version";
    private static final String START_TIME_TRAIT_NAME = "startTime";

    private static final String SHUTDOWN_OPERATION_NAME = "shutdown";
    private static final String START_OPERATION_NAME = "start";

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

}
