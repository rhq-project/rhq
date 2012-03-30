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

import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.util.FileUtils;
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

    public static final ResourceType RESOURCE_TYPE = new ResourceType("JBossAS7 Standalone Server", PLUGIN_NAME, ResourceCategory.SERVER, null);
    // The key of an AS7 Standalone Server Resource is its JBOSS_HOME dir.
    public static final String RESOURCE_KEY = FileUtils.getCanonicalPath(System.getProperty("jboss7.home") + "/standalone");

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

    // ******************************* METRICS ******************************* //
    @Override
    @Test(priority = 2, enabled = true)
    public void testMetricsHaveNonNullValues() throws Exception {
        super.testMetricsHaveNonNullValues();
    }

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

    // TODO: Re-enable this once "shutdown" operation has been fixed.
    @Test(priority = 5, enabled = false)
    public void testStandaloneServerShutdownAndStartOperations() throws Exception {
        super.testShutdownAndStartOperations();
    }

    // TODO: Re-enable once fixed.
    @Test(priority = 5, dependsOnMethods = "testStandaloneServerShutdownAndStartOperations", enabled = false)
    public void testRestartOperation() throws Exception {
        AvailabilityType avail = getAvailability(getServerResource());
        assertEquals(avail, AvailabilityType.UP);
        invokeOperationAndAssertSuccess(getServerResource(), RESTART_OPERATION_NAME, null);
        avail = getAvailability(getServerResource());
        assertEquals(avail, AvailabilityType.UP);
    }

}
