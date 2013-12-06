/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.AfterSuite;
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
@Test(groups = {"integration", "pc", "domain"}, singleThreaded = true)
public class DomainServerComponentTest extends AbstractServerComponentTest {

    public static final ResourceType RESOURCE_TYPE =
            new ResourceType("JBossAS7 Host Controller", PLUGIN_NAME, ResourceCategory.SERVER, null);
    // The key is the server host config file
    // hostConfig: /tmp/jboss-as-6.0.0/domain/configuration/host.xml
    public static final String RESOURCE_KEY = "hostConfig: "
        + new File(JBOSS_HOME, "domain" + File.separator + "configuration" + File.separator + "host.xml")
            .getAbsolutePath();

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

    protected String getExpectedStartScriptFileName() {
        return (File.separatorChar == '/') ? "domain.sh" : "domain.bat";
    }

    @AfterSuite
    public void killServerProcesses() {
        super.killServerProcesses();
    }

    @Override
    protected List<String> getExpectedStartScriptArgs() {
        int portOffset = getPortOffset();
        String [] args = new String[] {
            "-Djboss.bind.address.management=127.0.0.1",
            "-Djboss.bind.address=127.0.0.1",
            "-Djboss.bind.address.unsecure=127.0.0.1",
            "-Djboss.socket.binding.port-offset=" + portOffset,
            "-Djboss.management.native.port=" + (portOffset + 9999),
            "-Djboss.management.http.port=" + (portOffset + 9990),
            "-Djboss.management.https.port="  + (portOffset + 9943)
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
