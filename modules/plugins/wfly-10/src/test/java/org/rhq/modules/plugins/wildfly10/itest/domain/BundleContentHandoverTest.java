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

package org.rhq.modules.plugins.wildfly10.itest.domain;

import static java.lang.Boolean.TRUE;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.Collections;
import java.util.HashMap;
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
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.modules.plugins.wildfly10.itest.AbstractJBossAS7PluginTest;
import org.rhq.modules.plugins.wildfly10.json.Address;
import org.rhq.modules.plugins.wildfly10.json.Operation;
import org.rhq.modules.plugins.wildfly10.json.ReadAttribute;
import org.rhq.modules.plugins.wildfly10.json.ReadResource;
import org.rhq.modules.plugins.wildfly10.json.Result;
import org.rhq.modules.plugins.wildfly10.test.util.ASConnectionFactory;
import org.rhq.modules.plugins.wildfly10.test.util.Constants;
import org.rhq.test.arquillian.RunDiscovery;

/**
 * @author Thomas Segismont
 */
@Test(groups = { "integration", "pc", "domain" }, singleThreaded = true)
public class BundleContentHandoverTest extends AbstractJBossAS7PluginTest {

    private Resource domainServerResource;

    @Test(priority = -10000)
    @RunDiscovery(discoverServers = true, discoverServices = false)
    public void initialDiscoveryTest() throws Exception {
        Resource platformResource = validatePlatform();
        domainServerResource = waitForResourceByTypeAndKey(platformResource, platformResource, Constants.DOMAIN_RESOURCE_TYPE,
            Constants.DOMAIN_RESOURCE_KEY);
    }

    @Test(dependsOnMethods = { "initialDiscoveryTest" })
    public void testExecuteCliAction() throws Exception {
        BundleHandoverFacet handoverFacet = getHandoverFacet();

        BundleHandoverContext.Builder contextBuilder = new BundleHandoverContext.Builder() //
            .setRevert(false);
        BundleHandoverRequest.Builder requestBuilder = new BundleHandoverRequest.Builder() //
            .setContent(getClass().getClassLoader().getResourceAsStream("itest/updateDomainEjb3Subsystem.cli")) //
            .setFilename("updateDomainEjb3Subsystem.cli") //
            .setAction("execute-script") //
            .setParams(Collections.<String, String> emptyMap()) //
            .setContext(contextBuilder.create());

        BundleHandoverResponse response = handoverFacet.handleContent(requestBuilder.createBundleHandoverRequest());

        assertTrue(response.isSuccess(), getFailureMessage(response));

        Address ejb3SubsystemAddress = new Address();
        ejb3SubsystemAddress.add("profile", "full");
        ejb3SubsystemAddress.add("subsystem", "ejb3");
        Result readAttributeResult = ASConnectionFactory.getDomainControllerASConnection().execute(
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
        Map<String, String> params = new HashMap<String, String>();
        params.put("serverGroup", "main-server-group");
        BundleHandoverRequest.Builder requestBuilder = new BundleHandoverRequest.Builder() //
            .setContent(getClass().getClassLoader().getResourceAsStream("itest/javaee6-test-app.war")) //
            .setFilename("javaee6-test-app.war") //
            .setAction("deployment") //
            .setParams(params) //
            .setContext(contextBuilder.create());

        BundleHandoverResponse response = handoverFacet.handleContent(requestBuilder.createBundleHandoverRequest());

        assertTrue(response.isSuccess(), getFailureMessage(response));

        Address managedServerDeploymentAddress = new Address();
        managedServerDeploymentAddress.add("host", "master");
        managedServerDeploymentAddress.add("server", "server-one");
        managedServerDeploymentAddress.add("deployment", "javaee6-test-app.war");
        Result readAttributeResult = ASConnectionFactory.getDomainControllerASConnection().execute(
            new ReadResource(managedServerDeploymentAddress));

        assertTrue(readAttributeResult.isSuccess(), readAttributeResult.getFailureDescription());

        @SuppressWarnings("unchecked")
        Map<String, ?> attributes = (Map<String, ?>) readAttributeResult.getResult();

        assertEquals(attributes.get("enabled"), TRUE);
        assertEquals(attributes.get("runtime-name"), "javaee6-test-app.war");

        Address serverGroupDeploymentAddress = new Address();
        serverGroupDeploymentAddress.add("server-group", "main-server-group");
        serverGroupDeploymentAddress.add("deployment", "javaee6-test-app.war");
        Result removeDeploymentResult = ASConnectionFactory.getDomainControllerASConnection().execute(
            new Operation("remove", serverGroupDeploymentAddress));

        assertTrue(removeDeploymentResult.isSuccess(), "Could not clean domain controller deployment: "
            + removeDeploymentResult.getFailureDescription());
    }

    private BundleHandoverFacet getHandoverFacet() throws Exception {
        return ComponentUtil.getComponent(domainServerResource.getId(), BundleHandoverFacet.class, FacetLockType.WRITE,
            TimeUnit.MINUTES.toMillis(5), true, true, false);
    }

    public static String getFailureMessage(BundleHandoverResponse response) {
        return response.getFailureType() + ": " + response.getMessage() + " "
            + ThrowableUtil.getAllMessages(response.getThrowable());
    }
}
