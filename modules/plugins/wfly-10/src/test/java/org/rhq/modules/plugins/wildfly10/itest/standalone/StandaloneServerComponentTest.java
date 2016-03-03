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

package org.rhq.modules.plugins.wildfly10.itest.standalone;

import static org.rhq.core.domain.measurement.AvailabilityType.UP;
import static org.testng.Assert.assertEquals;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.testng.annotations.Test;

import org.rhq.core.clientapi.agent.configuration.ConfigurationUpdateRequest;
import org.rhq.core.clientapi.server.configuration.ConfigurationUpdateResponse;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.modules.plugins.wildfly10.itest.AbstractServerComponentTest;
import org.rhq.modules.plugins.wildfly10.test.util.Constants;
import org.rhq.test.arquillian.RunDiscovery;

/**
 * Test discovery and facets of the "JBossAS7 Standalone Server" Resource type.
 *
 * @author Ian Springer
 */
@Test(groups = { "integration", "pc", "standalone" }, singleThreaded = true)
public class StandaloneServerComponentTest extends AbstractServerComponentTest {

    private static final String RELOAD_OPERATION_NAME = "reload";
    private static final String RESTART_OPERATION_NAME = "restart";

    private Resource standaloneServer;

    @Override
    protected ResourceType getServerResourceType() {
        return Constants.STANDALONE_RESOURCE_TYPE;
    }

    @Override
    protected String getServerResourceKey() {
        return Constants.STANDALONE_RESOURCE_KEY;
    }

    @Override
    protected Resource getServerResource() {
        return standaloneServer;
    }

    @Override
    protected String getBindAddressSystemPropertyName() {
        return "jboss.standalone.bindAddress";
    }

    @Override
    protected String getPortOffsetSystemPropertyName() {
        return "jboss.standalone.portOffset";
    }

    @Test(priority = -10000)
    @RunDiscovery(discoverServers = true, discoverServices = false)
    public void initialDiscoveryTest() throws Exception {
        Resource platform = validatePlatform();
        standaloneServer = waitForResourceByTypeAndKey(platform, platform, Constants.STANDALONE_RESOURCE_TYPE,
            Constants.STANDALONE_RESOURCE_KEY);
    }

    @Test(priority = 2)
    public void testStandaloneServerAttributeValidation() throws Exception {
        testServerAttributeValidation();
    }

    // ******************************* METRICS ******************************* //
    @Test(priority = 3, enabled = true)
    public void testStandaloneReleaseVersionTrait() throws Exception {
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
    @Test(priority = 6, dependsOnMethods = "testStandaloneServerShutdownAndStartOperations", enabled = false)
    public void testRestartOperation() throws Exception {
        // First make sure the server is up.
        AvailabilityType avail = getAvailability(getServerResource());
        assertEquals(avail, UP);

        // Make sure the server is back up.
        // TODO (ips): Check that the server is a different process now.
        invokeOperationAndAssertSuccess(getServerResource(), RESTART_OPERATION_NAME, null);
        avail = getAvailability(getServerResource());
        assertEquals(avail, UP);
    }

    @Override
    @Test(priority = 6, enabled = true)
    public void testExecuteCliOperations() throws Exception {
        super.testExecuteCliOperations();
    }

    @Test(priority = 7, enabled = true)
    public void testSystemPropertiesSettings() throws Exception {

        Configuration config = loadResourceConfiguration(getServerResource());
        PropertyList starList = (PropertyList) config.get("*2");
        PropertyMap newProp = new PropertyMap("*:name");
        newProp.put(new PropertySimple("name", "Hulla"));
        newProp.put(new PropertySimple("value", "Hopp"));
        starList.add(newProp);

        ConfigurationUpdateRequest request = new ConfigurationUpdateRequest(1, config, getServerResource().getId());
        ConfigurationUpdateResponse response = pluginContainer.getConfigurationManager()
            .executeUpdateResourceConfigurationImmediately(request);
        assert response != null;
        assert response.getErrorMessage() == null : "Adding a property resulted in this error: "
            + response.getErrorMessage();

        config = loadResourceConfiguration(getServerResource());
        starList = (PropertyList) config.get("*2");
        List<Property> propertyList = starList.getList();
        for (Property prop : propertyList) {
            assert prop instanceof PropertyMap;
            PropertyMap map = (PropertyMap) prop;
            PropertySimple key = map.getSimple("name");
            assert key != null;
            if ("Hulla".equals(key.getStringValue())) {
                PropertySimple val = map.getSimple("value");
                assert val != null;
                assert "Hopp".equals(val.getStringValue());
            }
        }

    }

    @Override
    protected String getExpectedStartScriptFileName() {
        return (File.separatorChar == '/') ? "standalone.sh" : "standalone.bat";
    }

    @Override
    protected List<String> getExpectedStartScriptArgs() {
        String [] args = new String[] { //
            "--server-config=standalone-full-ha.xml", //
            "-Djboss.bind.address.management=127.0.0.1", //
            "-Djboss.bind.address=127.0.0.1", //
            "-Djboss.bind.address.unsecure=127.0.0.1", //
            "-Djboss.socket.binding.port-offset=" + getPortOffset() //
        };
        return Arrays.asList(args);
    }

}
