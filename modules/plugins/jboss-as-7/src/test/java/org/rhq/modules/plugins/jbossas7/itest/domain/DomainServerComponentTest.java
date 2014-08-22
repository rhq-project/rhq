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

import static org.rhq.modules.plugins.jbossas7.test.util.Constants.DOMAIN_RESOURCE_KEY;
import static org.rhq.modules.plugins.jbossas7.test.util.Constants.DOMAIN_RESOURCE_TYPE;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.modules.plugins.jbossas7.itest.AbstractServerComponentTest;
import org.rhq.test.arquillian.RunDiscovery;

/**
 * Test discovery and facets of the "JBossAS7 Host Controller" Resource type.
 *
 * @author Ian Springer
 */
@Test(groups = { "integration", "pc", "domain" }, singleThreaded = true)
public class DomainServerComponentTest extends AbstractServerComponentTest {

    private Resource domainServer;

    @Override
    protected ResourceType getServerResourceType() {
        return DOMAIN_RESOURCE_TYPE;
    }

    @Override
    protected String getServerResourceKey() {
        return DOMAIN_RESOURCE_KEY;
    }

    @Override
    protected Resource getServerResource() {
        return domainServer;
    }

    @Override
    protected String getBindAddressSystemPropertyName() {
        return "jboss.domain.bindAddress";
    }

    @Override
    protected String getPortOffsetSystemPropertyName() {
        return "jboss.domain.portOffset";
    }

    @Test(priority = -10000)
    @RunDiscovery(discoverServers = true, discoverServices = false)
    public void initialDiscoveryTest() throws Exception {

        Resource platform = validatePlatform();
        domainServer = waitForResourceByTypeAndKey(platform, platform, DOMAIN_RESOURCE_TYPE, DOMAIN_RESOURCE_KEY);
    }

    @Test(priority = 1001)
    public void testDomainServerAttributeValidation() throws Exception {
        testServerAttributeValidation();
    }

    // ******************************* METRICS ******************************* //
    @Test(priority = 1003, enabled = true)
    public void testDomainReleaseVersionTrait() throws Exception {
        super.testReleaseVersionTrait();
    }

    // ******************************* OPERATIONS ******************************* //
    @Test(priority = 1004, enabled = true)
    public void testDomainServerShutdownAndStartOperations() throws Exception {
        super.testShutdownAndStartOperations();
    }

    @Override
    @Test(priority = 1005, enabled = true)
    public void testExecuteCliOperations() throws Exception {
        super.testExecuteCliOperations();
    }

    @Override
    protected String getExpectedStartScriptFileName() {
        return (File.separatorChar == '/') ? "domain.sh" : "domain.bat";
    }

    @Override
    protected List<String> getExpectedStartScriptArgs() {
        int portOffset = getPortOffset();
        String [] args = new String[] { //
            "-Djboss.bind.address.management=127.0.0.1", //
            "-Djboss.bind.address=127.0.0.1", //
            "-Djboss.bind.address.unsecure=127.0.0.1", //
            "-Djboss.socket.binding.port-offset=" + portOffset, //
            "-Djboss.management.native.port=" + (portOffset + 9999), //
            "-Djboss.management.http.port=" + (portOffset + 9990), //
            "-Djboss.management.https.port="  + (portOffset + 9943) //
        };
        return Arrays.asList(args);
    }

    @Override
    protected void validateStartScriptEnv(Map<String, String> env) {
        super.validateStartScriptEnv(env);

        // Only domain sets JBOSS_HOME, when not started via start script.
        String jbossHome = env.get("JBOSS_HOME");
        if (jbossHome != null) {
            Assert.assertTrue(new File(jbossHome).isDirectory());
        }
    }

}
