/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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

package org.rhq.plugins.apache.upgrade;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.io.StringReader;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.BeforeClass;

import org.rhq.core.clientapi.agent.metadata.PluginMetadataParser;
import org.rhq.core.clientapi.descriptor.AgentPluginDescriptorUtil;
import org.rhq.core.clientapi.descriptor.plugin.PluginDescriptor;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceError;
import org.rhq.core.domain.resource.ResourceErrorType;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.PluginContainerDeployment;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.system.SystemInfoFactory;
import org.rhq.core.util.stream.StreamUtil;
import org.rhq.plugins.apache.ApacheServerComponent;
import org.rhq.plugins.apache.ApacheServerDiscoveryComponent;
import org.rhq.plugins.apache.ApacheVirtualHostServiceComponent;
import org.rhq.plugins.apache.ApacheVirtualHostServiceDiscoveryComponent;
import org.rhq.plugins.apache.PluginLocation;
import org.rhq.plugins.apache.parser.ApacheDirectiveTree;
import org.rhq.plugins.apache.setup.ApacheTestConfiguration;
import org.rhq.plugins.apache.setup.ApacheTestSetup;
import org.rhq.plugins.apache.util.ApacheDeploymentUtil.DeploymentConfig;
import org.rhq.plugins.apache.util.ResourceTypes;
import org.rhq.plugins.apache.util.VHostSpec;
import org.rhq.plugins.apache.util.VirtualHostLegacyResourceKeyUtil;
import org.rhq.test.TokenReplacingReader;
import org.rhq.test.pc.PluginContainerTest;


/**
 * Base class for the upgrade test classes.
 *
 * @author Lukas Krejci
 */
public class UpgradeTestBase extends PluginContainerTest {

    private static final Log LOG = LogFactory.getLog(UpgradeTestBase.class);

    public enum ResourceKeyFormat {
        SNMP, RHQ3, RHQ4
    };

    protected static final String DEPLOYMENT_SIMPLE_WITH_RESOLVABLE_SERVERNAMES = "simpleWithResolvableServerNames";
    protected static final String DEPLOYMENT_SIMPLE_WITH_UNRESOLVABLE_SERVER_NAMES = "simpleWithUnresolvableServerNames";
    protected static final String DEPLOYMENT_SIMPLE_WITH_WILDCARD_LISTENS = "simpleWithWildcardListens";

    private ResourceTypes apacheResourceTypes;
    protected Resource platform;

    @BeforeClass
    public void parseResourceTypesFromApachePlugin() throws Exception {
        apacheResourceTypes = new ResourceTypes(PluginLocation.APACHE_PLUGIN);
        platform = discoverPlatform();
    }

    protected void testUpgrade(String testMethod, ApacheTestConfiguration testConfiguration) throws Throwable {
        String testId = this.getClass().getSimpleName() + "#" + testMethod;
        final ApacheTestSetup setup = new ApacheTestSetup(testId, testConfiguration.configurationName, context,
            apacheResourceTypes);
        boolean testFailed = false;
        try {

            String[] configFiles = Arrays.copyOf(testConfiguration.apacheConfigurationFiles, testConfiguration.apacheConfigurationFiles.length + 1);
            configFiles[testConfiguration.apacheConfigurationFiles.length] = "/snmpd.conf";

            setup.withInventoryFrom(testConfiguration.inventoryFile)
                .withPlatformResource(platform).withDefaultExpectations().withDefaultOverrides(testConfiguration.defaultOverrides)
                .withApacheSetup().withConfigurationFiles(configFiles)
                .withServerRoot(testConfiguration.serverRoot).withExePath(testConfiguration.binPath);

            testConfiguration.beforeTestSetup(setup);

            LOG.debug("---------------------------------------------------------- Starting the upgrade test for: "
                + testId);
            LOG.debug("Deployment configuration: " + setup.getDeploymentConfig());

            setup.setup();

            testConfiguration.beforePluginContainerStart(setup);

            startConfiguredPluginContainer();

            testConfiguration.beforeTests(setup);

            //ok, now we should see the resources upgraded in the fake server inventory.
            ResourceType serverResourceType = apacheResourceTypes.findByName("Apache HTTP Server");
            ResourceType vhostResourceType = apacheResourceTypes.findByName("Apache Virtual Host");

            Set<Resource> servers = setup.getFakeInventory().findResourcesByTypeAndStatus(serverResourceType, InventoryStatus.COMMITTED);

            assertEquals(servers.size(), 1, "There should be exactly one apache server discovered.");

            Resource server = servers.iterator().next();

            String expectedResourceKey = ApacheServerDiscoveryComponent.formatResourceKey(testConfiguration.serverRoot, testConfiguration.serverRoot
                + "/conf/httpd.conf");

            assertEquals(server.getResourceKey(), expectedResourceKey,
                "The server resource key doesn't seem to be upgraded.");

            Set<Resource> vhosts = setup.getFakeInventory().findResourcesByTypeAndStatus(vhostResourceType, InventoryStatus.COMMITTED);

            String[] expectedRKs = testConfiguration.getExpectedResourceKeysAfterUpgrade(setup);

            assertEquals(vhosts.size(), expectedRKs.length, "Unexpected number of vhosts discovered found");

            List<String> expectedResourceKeys = Arrays.asList(expectedRKs);

            for (Resource vhost : vhosts) {
                assertTrue(expectedResourceKeys.contains(vhost.getResourceKey()),
                    "Unexpected virtual host resource key: '" + vhost.getResourceKey() + "'. Only expecting " + expectedResourceKeys);
            }

            String[] expectedFailureRKs = testConfiguration.getExpectedResourceKeysWithFailures(setup);
            if (expectedFailureRKs != null && expectedFailureRKs.length > 0) {
                Set<Resource> failingResources = new HashSet<Resource>();

                for(String rk : expectedFailureRKs) {
                    for(Resource r : vhosts) {
                        if (rk.equals(r.getResourceKey())) {
                            failingResources.add(r);
                            break;
                        }
                    }
                }

                assertEquals(failingResources.size(), expectedFailureRKs.length, "Couldn't find all the resources that should have failed.");

                for(Resource failingResource : failingResources) {
                    List<ResourceError> errors = failingResource.getResourceErrors(ResourceErrorType.UPGRADE);
                    assertNotNull(errors, "The main vhost doesn't have any upgrade errors.");
                    assertEquals(errors.size(), 1, "There should be exactly one upgrade error on the main vhost.");
                }

                //check that all other vhosts were not upgraded but have no errors
                for(Resource r : vhosts) {
                    if (failingResources.contains(r)) {
                        continue;
                    }

                    assertEquals(r.getResourceErrors(ResourceErrorType.UPGRADE).size(), 0, "Unexpected number of resource upgrade errors on vhost " + r);
                }
            } else {
                for(Resource r : vhosts) {
                    assertEquals(r.getResourceErrors(ResourceErrorType.UPGRADE).size(), 0, "Unexpected number of resource upgrade errors on vhost " + r);
                }
            }
        } catch (AssertionError e) {
            throw e;
        } catch (Throwable t) {
            testFailed = true;
            LOG.error("Error during test upgrade execution.", t);
            throw t;
        } finally {
            try {
                setup.withApacheSetup().stopApache();
            } catch (Exception e) {
                if (testFailed) {
                    LOG.error("Failed to stop apache.", e);
                } else {
                    throw e;
                }
            }

            LOG.debug("---------------------------------------------------------- Finished the upgrade test for: "
                + testId);
        }
    }

    protected void defineRHQ3ResourceKeys(ApacheTestConfiguration testConfig, ApacheTestSetup setup) throws Exception {
        setup.withApacheSetup().init();
        ApacheServerComponent component = setup.withApacheSetup().getServerComponent();
        ApacheDirectiveTree config = component.parseRuntimeConfiguration(false);

        DeploymentConfig deployConfig = setup.getDeploymentConfig();

        VirtualHostLegacyResourceKeyUtil keyUtil = new VirtualHostLegacyResourceKeyUtil(component, config);

        Map<String, String> replacements = deployConfig.getTokenReplacements();

        testConfig.defaultOverrides.put("main.rhq3.resource.key", keyUtil.getRHQ3NonSNMPLegacyMainServerResourceKey());

        if (deployConfig.vhost1 != null) {
            testConfig.defaultOverrides.put("vhost1.rhq3.resource.key", keyUtil.getRHQ3NonSNMPLegacyVirtualHostResourceKey(deployConfig.vhost1.getVHostSpec(replacements)));
        }

        if (deployConfig.vhost2 != null) {
            testConfig.defaultOverrides.put("vhost2.rhq3.resource.key", keyUtil.getRHQ3NonSNMPLegacyVirtualHostResourceKey(deployConfig.vhost2.getVHostSpec(replacements)));
        }

        if (deployConfig.vhost3 != null) {
            testConfig.defaultOverrides.put("vhost3.rhq3.resource.key", keyUtil.getRHQ3NonSNMPLegacyVirtualHostResourceKey(deployConfig.vhost3.getVHostSpec(replacements)));
        }

        if (deployConfig.vhost4 != null) {
            testConfig.defaultOverrides.put("vhost4.rhq3.resource.key", keyUtil.getRHQ3NonSNMPLegacyVirtualHostResourceKey(deployConfig.vhost4.getVHostSpec(replacements)));
        }

        setup.withDefaultOverrides(testConfig.defaultOverrides);
    }

    protected String interpret(String string, Map<String, String> variables) {
        return StreamUtil.slurp(new TokenReplacingReader(new StringReader(string), variables));
    }

    protected static String[] getVHostRKs(ApacheTestSetup setup, int[] successfulUpgrades, int[] failedUpgrades, ResourceKeyFormat rkFormat) {
        int sucLen = successfulUpgrades == null ? 0 : successfulUpgrades.length;
        int failLen = failedUpgrades == null ? 0 : failedUpgrades.length;

        String[] ret = new String[sucLen + failLen];

        int retIdx = 0;

        Map<String, String> replacements = setup.getInventoryFileReplacements();

        for(int i = 0; i < sucLen; ++i, ++retIdx) {
            int vhostNum = successfulUpgrades[i];
            if (vhostNum == 0) {
                ret[retIdx] = ApacheVirtualHostServiceComponent.MAIN_SERVER_RESOURCE_KEY;
            } else {
                VHostSpec vhost = setup.getDeploymentConfig().getVHost(vhostNum).getVHostSpec(replacements);
                ret[retIdx] = ApacheVirtualHostServiceDiscoveryComponent.createResourceKey(vhost.serverName, vhost.hosts);
            }
        }

        for(int i = 0; i < failLen; ++i, ++retIdx) {
            String variableName = null;
            if (failedUpgrades[i] == 0) {
                if (rkFormat == ResourceKeyFormat.SNMP) {
                    variableName = "";
                } else {
                    variableName = "main.rhq";
                }
            } else {
                if (rkFormat == ResourceKeyFormat.SNMP) {
                    variableName += "vhost" + failedUpgrades[i] + ".";
                } else {
                    variableName = "vhost" + failedUpgrades[i] + ".rhq";
                }
            }

            switch (rkFormat) {
            case RHQ3:
                variableName += "3.resource.key";
                break;
            case RHQ4:
                variableName += "4.resource.key";
                break;
            case SNMP:
                variableName += "snmp.identifier";
                break;
            }

            ret[retIdx] = replacements.get(variableName);
        }

        return ret;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static Resource discoverPlatform() throws Exception {
        PluginDescriptor descriptor = AgentPluginDescriptorUtil.loadPluginDescriptorFromUrl(new URI(PluginLocation.PLATFORM_PLUGIN)
            .toURL());
        PluginMetadataParser parser = new PluginMetadataParser(descriptor,
            Collections.<String, PluginMetadataParser> emptyMap());

        List<ResourceType> platformTypes = parser.getAllTypes();

        //this is the default container name in case of no plugin explicit plugin configuration, which we don't have.
        String containerName = InetAddress.getLocalHost().getCanonicalHostName();

        for (ResourceType rt : platformTypes) {
            if (rt.getCategory() != ResourceCategory.PLATFORM) {
                continue;
            }

            Class discoveryClass = Class.forName(parser.getDiscoveryComponentClass(rt));

            ResourceDiscoveryComponent discoveryComponent = (ResourceDiscoveryComponent) discoveryClass.newInstance();

            ResourceDiscoveryContext context = new ResourceDiscoveryContext(rt, null, null,
                SystemInfoFactory.createSystemInfo(), Collections.emptyList(), Collections.emptyList(), containerName,
                PluginContainerDeployment.AGENT);

            Set<DiscoveredResourceDetails> results = discoveryComponent.discoverResources(context);

            if (!results.isEmpty()) {
                DiscoveredResourceDetails details = results.iterator().next();

                Resource platform = new Resource();

                platform.setDescription(details.getResourceDescription());
                platform.setResourceKey(details.getResourceKey());
                platform.setName(details.getResourceName());
                platform.setVersion(details.getResourceVersion());
                platform.setPluginConfiguration(details.getPluginConfiguration());
                platform.setResourceType(rt);
                platform.setUuid(UUID.randomUUID().toString());
                platform.setId(1);

                return platform;
            }
        }

        return null;
    }

    protected static String variableName(String prefix, String name) {
        StringBuilder bld = new StringBuilder();
        if (prefix != null && !prefix.isEmpty()) {
            bld.append(prefix).append(".");
        }

        bld.append(name);

        return bld.toString();
    }

    protected static InetAddress determineLocalhost() {
        try {
            return InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            try {
                return InetAddress.getByName("127.0.0.1");
            } catch (UnknownHostException ee) {
                //doesn't happen
                return null;
            }
        }
    }
}
