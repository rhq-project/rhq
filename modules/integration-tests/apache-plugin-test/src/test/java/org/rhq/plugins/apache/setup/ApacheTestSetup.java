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

package org.rhq.plugins.apache.setup;

import static org.testng.Assert.assertTrue;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jmock.Expectations;
import org.jmock.Mockery;

import org.rhq.core.clientapi.server.discovery.InventoryReport;
import org.rhq.core.domain.discovery.AvailabilityReport;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceError;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pc.ServerServices;
import org.rhq.core.pc.upgrade.FakeServerInventory;
import org.rhq.core.system.SystemInfoFactory;
import org.rhq.core.util.file.FileUtil;
import org.rhq.plugins.apache.ApacheServerComponent;
import org.rhq.plugins.apache.ApacheServerDiscoveryComponent;
import org.rhq.plugins.apache.ApacheVirtualHostServiceComponent;
import org.rhq.plugins.apache.ApacheVirtualHostServiceDiscoveryComponent;
import org.rhq.plugins.apache.parser.ApacheDirectiveTree;
import org.rhq.plugins.apache.util.ApacheBinaryInfo;
import org.rhq.plugins.apache.util.ApacheDeploymentUtil;
import org.rhq.plugins.apache.util.ApacheDeploymentUtil.DeploymentConfig;
import org.rhq.plugins.apache.util.ApacheExecutionUtil;
import org.rhq.plugins.apache.util.ApacheExecutionUtil.ExpectedApacheState;
import org.rhq.plugins.apache.util.HttpdAddressUtility;
import org.rhq.plugins.apache.util.ResourceTypes;
import org.rhq.plugins.apache.util.VHostSpec;
import org.rhq.test.ObjectCollectionSerializer;
import org.rhq.test.TokenReplacingReader;
import org.rhq.test.pc.PluginContainerTest;

public class ApacheTestSetup {
    private String configurationName;
    private FakeServerInventory fakeInventory = new FakeServerInventory();
    private String inventoryFile;
    private Resource platform;
    private ApacheSetup apacheSetup = new ApacheSetup();
    private DeploymentConfig deploymentConfig;
    private Map<String, String> defaultOverrides = new HashMap<String, String>();
    private Map<String, String> inventoryFileReplacements;
    private Mockery context;
    private ResourceTypes apacheResourceTypes;
    private String testId;

    public class ApacheSetup {
        private String serverRoot;
        private String exePath;
        private Collection<String> configurationFiles;
        private ApacheExecutionUtil execution;
        private boolean deploy = true;

        private ApacheSetup() {

        }

        public ApacheSetup withServerRoot(String serverRoot) {
            this.serverRoot = serverRoot;
            //auto-define the server root property as if it was passed on the commandline
            System.getProperties().put(configurationName + ".server.root", serverRoot);
            deploymentConfig = ApacheDeploymentUtil.getDeploymentConfigurationFromSystemProperties(configurationName, defaultOverrides);
            return this;
        }

        public ApacheSetup withExePath(String exePath) {
            this.exePath = exePath;
            return this;
        }

        public ApacheSetup withConfigurationFiles(String... classPathUris) {
            return withConfigurationFiles(Arrays.asList(classPathUris));
        }

        public ApacheSetup withConfigurationFiles(Collection<String> classPathUris) {
            this.configurationFiles = classPathUris;
            return this;
        }

        public ApacheSetup withDeploymentOnSetup() {
            this.deploy = true;
            return this;
        }

        public ApacheSetup withNoDeploymentOnSetup() {
            this.deploy = false;
            return this;
        }

        public void startApache() throws Exception {
            //clear the error log
            File errorLog = new File(new File(new File(serverRoot), "logs"), "error_log");
            errorLog.delete();

            getExecutionUtil().invokeOperation(ExpectedApacheState.RUNNING, "start");
        }

        public void stopApache() throws Exception {
            getExecutionUtil().invokeOperation(ExpectedApacheState.STOPPED, "stop");

            //save a copy of the error log
            File errorLog = new File(new File(new File(serverRoot), "logs"), "error_log");

            if (errorLog.exists() && errorLog.canRead()) {
                String copyName = testId + ".httpd.error_log";

                FileUtil.copyFile(errorLog, new File(new File("target"), copyName));
            }
        }

        public void reloadApache() {

        }

        public ApacheServerComponent getServerComponent() {
            return getExecutionUtil().getServerComponent();
        }

        public ApacheDirectiveTree getRuntimeConfiguration() {
            return getExecutionUtil().getRuntimeConfiguration();
        }

        public ApacheExecutionUtil getExecutionUtil() {
            return execution;
        }

        public void init() throws Exception {
            File serverRootDir = new File(serverRoot);

            assertTrue(serverRootDir.exists(), "The configured server root denotes a non-existant directory: '"
                + serverRootDir + "'.");

            File logsDir = new File(serverRootDir, "logs");

            assertTrue(logsDir.exists(), "The configured server root denotes a directory that doesn't have a 'logs' subdirectory. This is unexpected.");

            File confDir = new File(serverRootDir, "conf");

            assertTrue(confDir.exists(),
                "The configured server root denotes a directory that doesn't have a 'conf' subdirectory. This is unexpected.");

            String confFilePath = confDir.getAbsolutePath() + File.separatorChar + "httpd.conf";

            String snmpHost = null;
            int snmpPort = 0;
            String pingUrl = null;

            if (configurationName != null) {
                if (deploy) {
                    File binDir = new File(serverRootDir, "bin");
                    List<File> additionalFilesToProcess = Arrays.asList(
                        new File(binDir, "apachectl"),
                        new File(binDir, "envvars"),
                        new File(binDir, "envvars-std"));
                    ApacheDeploymentUtil.deployConfiguration(confDir, configurationFiles, additionalFilesToProcess, deploymentConfig);
                }

                //ok, now try to find the ping URL. The best thing is to actually invoke
                //the same code the apache server discovery does.
                ApacheDirectiveTree tree = ApacheServerDiscoveryComponent.parseRuntimeConfiguration(confFilePath, null, ApacheBinaryInfo.getInfo(exePath, SystemInfoFactory.createSystemInfo()));

                //XXX this hardcodes apache2 as the only option we have...
                HttpdAddressUtility.Address addrToUse = HttpdAddressUtility.APACHE_2_x.getMainServerSampleAddress(tree, null, -1);
                pingUrl = addrToUse.toString();

                snmpHost = deploymentConfig.snmpHost;
                snmpPort = deploymentConfig.snmpPort;
            }

            execution = new ApacheExecutionUtil(apacheResourceTypes.findByName("Apache HTTP Server"),
                serverRoot, exePath, confFilePath, pingUrl,
                snmpHost, snmpPort);
            execution.init();
        }

        private void doSetup() throws Exception {
            init();
            startApache();
        }

        public ApacheTestSetup setup() throws Exception {
            return ApacheTestSetup.this.setup();
        }
    }

    public ApacheTestSetup(String testId, String configurationName, Mockery context,
        ResourceTypes apacheResourceTypes) {
        this.testId = testId;
        this.configurationName = configurationName;
        this.context = context;
        this.apacheResourceTypes = apacheResourceTypes;
        deploymentConfig = ApacheDeploymentUtil.getDeploymentConfigurationFromSystemProperties(configurationName, defaultOverrides);
    }

    public ApacheTestSetup withInventoryFrom(String classPathUri) {
        inventoryFile = classPathUri;
        return this;
    }

    public ApacheTestSetup withDefaultOverrides(Map<String, String> defaultOverrides) {
        this.defaultOverrides = defaultOverrides == null ? new HashMap<String, String>() : defaultOverrides;
        deploymentConfig = ApacheDeploymentUtil.getDeploymentConfigurationFromSystemProperties(configurationName, this.defaultOverrides);
        return this;
    }

    public ApacheTestSetup withPlatformResource(Resource platform) {
        this.platform = platform;
        return this;
    }

    public ApacheSetup withApacheSetup() {
        return apacheSetup;
    }

    public ApacheTestSetup withDefaultExpectations() throws Exception {
        context.checking(new Expectations() {
            {
                addDefaultExceptations(this);
            }
        });

        return this;
    }

    @SuppressWarnings("unchecked")
    public void addDefaultExceptations(Expectations expectations) throws Exception {
        ServerServices ss = PluginContainerTest.getCurrentPluginContainerConfiguration().getServerServices();

        //only import the apache servers we actually care about - we can't assume another apache won't be present
        //on the machine running the test...
        final ResourceType serverResourceType = apacheResourceTypes.findByName("Apache HTTP Server");
        expectations.allowing(ss.getDiscoveryServerService()).mergeInventoryReport(
            expectations.with(Expectations.any(InventoryReport.class)));
        expectations.will(fakeInventory.mergeInventoryReport(new FakeServerInventory.InventoryStatusJudge() {
            @Override
            public InventoryStatus judge(Resource resource) {
                if (serverResourceType.equals(resource.getResourceType())) {
                    return deploymentConfig.serverRoot.equals(resource.getPluginConfiguration().getSimpleValue(
                        ApacheServerComponent.PLUGIN_CONFIG_PROP_SERVER_ROOT)) ? InventoryStatus.COMMITTED
                        : InventoryStatus.IGNORED;
                } else {
                    return InventoryStatus.COMMITTED;
                }
            }
        }));

        expectations.allowing(ss.getDiscoveryServerService()).upgradeResources(
            expectations.with(Expectations.any(Set.class)));
        expectations.will(fakeInventory.upgradeResources());

        expectations.allowing(ss.getDiscoveryServerService()).getResources(
            expectations.with(Expectations.any(Set.class)), expectations.with(Expectations.any(boolean.class)));
        expectations.will(fakeInventory.getResources());

        expectations.allowing(ss.getDiscoveryServerService()).setResourceError(expectations.with(Expectations.any(ResourceError.class)));
        expectations.will(fakeInventory.setResourceError());

        expectations.allowing(ss.getDiscoveryServerService()).mergeAvailabilityReport(
            expectations.with(Expectations.any(AvailabilityReport.class)));

        expectations.allowing(ss.getDiscoveryServerService()).postProcessNewlyCommittedResources(
            expectations.with(Expectations.any(Set.class)));

        expectations.allowing(ss.getDiscoveryServerService()).clearResourceConfigError(
            expectations.with(Expectations.any(int.class)));

        expectations.allowing(ss.getDiscoveryServerService()).setResourceEnablement(
            expectations.with(Expectations.any(int.class)), expectations.with(Expectations.any(boolean.class)));

        expectations.allowing(ss.getDiscoveryServerService()).updateResourceVersion(
            expectations.with(Expectations.any(int.class)), expectations.with(Expectations.any(String.class)));

        expectations.allowing(ss.getDriftServerService()).getDriftDefinitions(expectations.with(Expectations.any(Set.class)));
        expectations.will(Expectations.returnValue(Collections.emptyMap()));

        expectations.allowing(ss.getDiscoveryServerService()).getResourcesAsList(expectations.with(Expectations.any(Integer[].class)));
        expectations.will(fakeInventory.getResourcesAsList());

        expectations.ignoring(ss.getBundleServerService());
        expectations.ignoring(ss.getConfigurationServerService());
        expectations.ignoring(ss.getContentServerService());
        expectations.ignoring(ss.getCoreServerService());
        expectations.ignoring(ss.getEventServerService());
        expectations.ignoring(ss.getMeasurementServerService());
        expectations.ignoring(ss.getOperationServerService());
        expectations.ignoring(ss.getResourceFactoryServerService());
    }

    public FakeServerInventory getFakeInventory() {
        return fakeInventory;
    }

    public DeploymentConfig getDeploymentConfig() {
        return deploymentConfig;
    }

    public ApacheTestSetup setup() throws Exception {
        apacheSetup.doSetup();

        Map<String, String> replacements = deploymentConfig.getTokenReplacements();
        replacements.put("server.root", apacheSetup.serverRoot);
        replacements.put("exe.path", apacheSetup.exePath);

        ApacheDeploymentUtil.addDefaultVariables(replacements, null);

        HttpdAddressUtility addressUtility = apacheSetup.getServerComponent()
            .getAddressUtility();
        ApacheDirectiveTree runtimeConfig = apacheSetup.getRuntimeConfiguration();

        replacements.put("snmp.identifier",
            addressUtility.getHttpdInternalMainServerAddressRepresentation(runtimeConfig).toString(false, false));

        replacements.put("main.rhq4.resource.key", ApacheVirtualHostServiceComponent.MAIN_SERVER_RESOURCE_KEY);

        VHostSpec vhost1 = deploymentConfig.vhost1 == null ? null : deploymentConfig.vhost1.getVHostSpec(replacements);
        VHostSpec vhost2 = deploymentConfig.vhost2 == null ? null : deploymentConfig.vhost2.getVHostSpec(replacements);
        VHostSpec vhost3 = deploymentConfig.vhost3 == null ? null : deploymentConfig.vhost3.getVHostSpec(replacements);
        VHostSpec vhost4 = deploymentConfig.vhost4 == null ? null : deploymentConfig.vhost4.getVHostSpec(replacements);

        if (vhost1 != null) {
            replacements.put(
                "vhost1.snmp.identifier",
                addressUtility.getHttpdInternalVirtualHostAddressRepresentation(runtimeConfig, vhost1.hosts.get(0),
                    vhost1.serverName).toString(false, false));

            replacements.put(
                "vhost1.rhq4.resource.key",
                ApacheVirtualHostServiceDiscoveryComponent.createResourceKey(
                    vhost1.serverName, vhost1.hosts));
        }

        if (vhost2 != null) {
            replacements.put(
                "vhost2.snmp.identifier",
                addressUtility.getHttpdInternalVirtualHostAddressRepresentation(runtimeConfig, vhost2.hosts.get(0),
                    vhost2.serverName).toString(false, false));

            replacements.put(
                "vhost2.rhq4.resource.key",
                ApacheVirtualHostServiceDiscoveryComponent.createResourceKey(
                    vhost2.serverName, vhost2.hosts));
        }

        if (vhost3 != null) {
            replacements.put(
                "vhost3.snmp.identifier",
                addressUtility.getHttpdInternalVirtualHostAddressRepresentation(runtimeConfig, vhost3.hosts.get(0),
                    vhost3.serverName).toString(false, false));

            replacements.put(
                "vhost3.rhq4.resource.key",
                ApacheVirtualHostServiceDiscoveryComponent.createResourceKey(
                    vhost3.serverName, vhost3.hosts));
        }

        if (vhost4 != null) {
            replacements.put(
                "vhost4.snmp.identifier",
                addressUtility.getHttpdInternalVirtualHostAddressRepresentation(runtimeConfig, vhost4.hosts.get(0),
                    vhost4.serverName).toString(false, false));

            replacements.put(
                "vhost4.rhq4.resource.key",
                ApacheVirtualHostServiceDiscoveryComponent.createResourceKey(
                    vhost4.serverName, vhost4.hosts));
        }

        //let the user override everything we just did
        replacements.putAll(defaultOverrides);

        inventoryFileReplacements = replacements;

        if (inventoryFile != null) {
            InputStream dataStream = getClass().getResourceAsStream(inventoryFile);

            Reader rdr = new TokenReplacingReader(new InputStreamReader(dataStream), replacements);

            @SuppressWarnings("unchecked")
            List<Resource> inventory = (List<Resource>) new ObjectCollectionSerializer().deserialize(rdr);

            //fix up the parent relationships, because they might not be reconstructed correctly by
            //JAXB - we're missing XmlID and XmlIDRef annotations in our model
            fixupParent(null, inventory);

            fakeInventory.prepopulateInventory(platform, inventory);
        }
        return this;
    }

    /**
     * After the setup, this returns all the variables used to update the tokens in the inventory file.
     *
     * @return
     */
    public Map<String, String> getInventoryFileReplacements() {
        return inventoryFileReplacements;
    }

    private void fixupParent(Resource parent, Collection<Resource> children) {
        for (Resource child : children) {
            child.setParentResource(parent);
            if (child.getChildResources() != null) {
                fixupParent(child, child.getChildResources());
            }
        }
    }
}