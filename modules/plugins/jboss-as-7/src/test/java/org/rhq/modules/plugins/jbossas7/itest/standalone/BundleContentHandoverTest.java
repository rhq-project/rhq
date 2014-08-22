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

package org.rhq.modules.plugins.jbossas7.itest.standalone;

import static java.lang.Boolean.TRUE;
import static org.rhq.modules.plugins.jbossas7.itest.domain.BundleContentHandoverTest.getFailureMessage;
import static org.rhq.modules.plugins.jbossas7.test.util.ASConnectionFactory.getStandaloneASConnection;
import static org.rhq.modules.plugins.jbossas7.test.util.Constants.STANDALONE_RESOURCE_KEY;
import static org.rhq.modules.plugins.jbossas7.test.util.Constants.STANDALONE_RESOURCE_TYPE;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.testng.annotations.Test;

import org.rhq.core.domain.resource.Resource;
import org.rhq.core.pc.util.ComponentUtil;
import org.rhq.core.pc.util.FacetLockType;
import org.rhq.core.pluginapi.bundle.BundleHandoverContext;
import org.rhq.core.pluginapi.bundle.BundleHandoverFacet;
import org.rhq.core.pluginapi.bundle.BundleHandoverRequest;
import org.rhq.core.pluginapi.bundle.BundleHandoverResponse;
import org.rhq.modules.plugins.jbossas7.itest.AbstractJBossAS7PluginTest;
import org.rhq.modules.plugins.jbossas7.json.Address;
import org.rhq.modules.plugins.jbossas7.json.Operation;
import org.rhq.modules.plugins.jbossas7.json.ReadAttribute;
import org.rhq.modules.plugins.jbossas7.json.ReadResource;
import org.rhq.modules.plugins.jbossas7.json.Result;
import org.rhq.test.arquillian.RunDiscovery;

/**
 * @author Thomas Segismont
 */
@Test(groups = { "integration", "pc", "standalone" }, singleThreaded = true)
public class BundleContentHandoverTest extends AbstractJBossAS7PluginTest {

    private Resource standaloneServerResource;

    @Test(priority = -10000)
    @RunDiscovery(discoverServers = true, discoverServices = false)
    public void initialDiscoveryTest() throws Exception {
        Resource platformResource = validatePlatform();
        standaloneServerResource = waitForResourceByTypeAndKey(platformResource, platformResource,
            STANDALONE_RESOURCE_TYPE, STANDALONE_RESOURCE_KEY);
    }

    @Test(dependsOnMethods = { "initialDiscoveryTest" })
    public void testExecuteCliAction() throws Exception {
        BundleHandoverFacet handoverFacet = getHandoverFacet();

        BundleHandoverContext.Builder contextBuilder = new BundleHandoverContext.Builder() //
            .setRevert(false);
        BundleHandoverRequest.Builder requestBuilder = new BundleHandoverRequest.Builder() //
            .setContent(getClass().getClassLoader().getResourceAsStream("itest/updateStandaloneEjb3Subsystem.cli")) //
            .setFilename("updateStandaloneEjb3Subsystem.cli") //
            .setAction("execute-script") //
            .setParams(Collections.<String, String> emptyMap()) //
            .setContext(contextBuilder.create());

        BundleHandoverResponse response = handoverFacet.handleContent(requestBuilder.createBundleHandoverRequest());

        assertTrue(response.isSuccess(), getFailureMessage(response));

        Address ejb3SubsystemAddress = new Address();
        ejb3SubsystemAddress.add("subsystem", "ejb3");
        Result readAttributeResult = getStandaloneASConnection().execute(
            new ReadAttribute(ejb3SubsystemAddress, "default-singleton-bean-access-timeout"));

        assertTrue(readAttributeResult.isSuccess(), readAttributeResult.getFailureDescription());

        Integer defaultSingletonBeanAccessTimeout = (Integer) readAttributeResult.getResult();

        assertEquals(defaultSingletonBeanAccessTimeout, Integer.valueOf(7777));
    }

    @Test(dependsOnMethods = { "initialDiscoveryTest" })
    public void testDeploymentAction() throws Exception {
        BundleHandoverFacet handoverFacet = getHandoverFacet();

        BundleHandoverContext.Builder contextBuilder = new BundleHandoverContext.Builder() //
            .setRevert(false);
        BundleHandoverRequest.Builder requestBuilder = new BundleHandoverRequest.Builder() //
            .setContent(getClass().getClassLoader().getResourceAsStream("itest/javaee6-test-app.war")) //
            .setFilename("javaee6-test-app.war") //
            .setAction("deployment") //
            .setParams(Collections.<String, String> emptyMap()) //
            .setContext(contextBuilder.create());

        BundleHandoverResponse response = handoverFacet.handleContent(requestBuilder.createBundleHandoverRequest());

        assertTrue(response.isSuccess(), getFailureMessage(response));

        Address deploymentAddress = new Address();
        deploymentAddress.add("deployment", "javaee6-test-app.war");
        Result readAttributeResult = getStandaloneASConnection().execute(new ReadResource(deploymentAddress));

        assertTrue(readAttributeResult.isSuccess(), readAttributeResult.getFailureDescription());

        @SuppressWarnings("unchecked")
        Map<String, ?> attributes = (Map<String, ?>) readAttributeResult.getResult();

        assertEquals(attributes.get("enabled"), TRUE);
        assertEquals(attributes.get("runtime-name"), "javaee6-test-app.war");

        Result removeDeploymentResult = getStandaloneASConnection().execute(new Operation("remove", deploymentAddress));

        assertTrue(removeDeploymentResult.isSuccess(), "Could not clean standalone deployment: "
            + removeDeploymentResult.getFailureDescription());
    }

    private BundleHandoverFacet getHandoverFacet() throws Exception {
        return ComponentUtil.getComponent(standaloneServerResource.getId(), BundleHandoverFacet.class,
            FacetLockType.WRITE, TimeUnit.MINUTES.toMillis(5), true, true, false);
    }
}
