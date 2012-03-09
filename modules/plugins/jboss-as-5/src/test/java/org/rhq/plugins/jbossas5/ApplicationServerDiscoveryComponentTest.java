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
package org.rhq.plugins.jbossas5;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.importer.ZipImporter;
import org.jboss.shrinkwrap.resolver.api.DependencyResolvers;
import org.jboss.shrinkwrap.resolver.api.maven.MavenDependencyResolver;
import org.rhq.core.clientapi.server.discovery.InventoryReport;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.PluginContainerConfiguration;
import org.rhq.core.util.maven.MavenArtifactProperties;
import org.rhq.test.arquillian.BeforeDiscovery;
import org.rhq.test.arquillian.DiscoveredResources;
import org.rhq.test.arquillian.FakeServerInventory;
import org.rhq.test.arquillian.MockingServerServices;
import org.rhq.test.arquillian.RunDiscovery;
import org.rhq.test.shrinkwrap.RhqAgentPluginArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.util.Collection;
import java.util.Set;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

/**
 * An integration test for {@link ApplicationServerDiscoveryComponent}.
 * 
 * @author Ian Springer
 */
@Test(groups = "arquillian")
public class ApplicationServerDiscoveryComponentTest extends Arquillian {

    @Deployment(name = "jboss-as-5")
    public static RhqAgentPluginArchive getJBossAS5Plugin() throws Exception {
        MavenArtifactProperties jmxPluginPom = MavenArtifactProperties.getInstance("org.rhq", "rhq-jmx-plugin");
        String rhqVersion = jmxPluginPom.getVersion();
        System.out.println("version: " + rhqVersion);
        File pluginJarFile = new File("target/jopr-jboss-as-5-plugin-" + rhqVersion + ".jar");
        MavenDependencyResolver mavenDependencyResolver = DependencyResolvers.use(MavenDependencyResolver.class);
        Collection<RhqAgentPluginArchive> requiredPlugins = mavenDependencyResolver.loadEffectivePom("pom.xml")
                .artifact("org.rhq:rhq-jmx-plugin:jar:" + rhqVersion).resolveAs(RhqAgentPluginArchive.class);
        return ShrinkWrap.create(ZipImporter.class, pluginJarFile.getName())
            .importFrom(pluginJarFile)
            .as(RhqAgentPluginArchive.class)
            .withRequiredPluginsFrom(requiredPlugins);
    }

    @ArquillianResource
    private MockingServerServices serverServices;

    @ArquillianResource
    private PluginContainerConfiguration pluginContainerConfiguration;

    @ArquillianResource
    private PluginContainer pluginContainer;

    @ArquillianResource
    private Deployer pluginDeployer;

    private FakeServerInventory fakeServerInventory;

    @DiscoveredResources(plugin = "JBossAS5", resourceType = "JBossAS Server")
    private Set<Resource> discoveredServers;

    @BeforeDiscovery
    public void resetServerServices() throws Exception {
        // Set up our fake server discovery ServerService, which will auto-import all Resources in reports it receives.
        this.serverServices.resetMocks();
        this.fakeServerInventory = new FakeServerInventory();
        when(this.serverServices.getDiscoveryServerService().mergeInventoryReport(any(InventoryReport.class))).then(
            this.fakeServerInventory.mergeInventoryReport(InventoryStatus.COMMITTED));
    }

    /**
     * Tests that autodiscovery of "JBossAS Server" Resources works.
     *
     * @throws Exception if an error occurs
     */
    @RunDiscovery
    public void testAutoDiscovery() throws Exception {
        Resource platform = this.pluginContainer.getInventoryManager().getPlatform();
                Assert.assertNotNull(platform);
                Assert.assertEquals(platform.getInventoryStatus(), InventoryStatus.COMMITTED);

        Assert.assertEquals(this.discoveredServers.size(), 0);
    }

}
