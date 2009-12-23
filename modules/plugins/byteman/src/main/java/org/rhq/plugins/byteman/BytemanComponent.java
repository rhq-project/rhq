/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.plugins.byteman;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jboss.byteman.agent.submit.Submit;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.content.transfer.DeployPackageStep;
import org.rhq.core.domain.content.transfer.DeployPackagesResponse;
import org.rhq.core.domain.content.transfer.RemovePackagesResponse;
import org.rhq.core.domain.content.transfer.ResourcePackageDetails;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.content.ContentFacet;
import org.rhq.core.pluginapi.content.ContentServices;
import org.rhq.core.pluginapi.inventory.CreateChildResourceFacet;
import org.rhq.core.pluginapi.inventory.CreateResourceReport;
import org.rhq.core.pluginapi.inventory.DeleteResourceFacet;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.core.util.exception.ThrowableUtil;

/**
 * Component that represents the main Byteman agent listening for requests.
 * 
 * @author John Mazzitelli
 */
public class BytemanComponent implements ResourceComponent, MeasurementFacet, OperationFacet, ConfigurationFacet,
    ContentFacet, DeleteResourceFacet, CreateChildResourceFacet {
    private final Log log = LogFactory.getLog(BytemanComponent.class);

    private Configuration resourceConfiguration;
    private ResourceContext resourceContext;
    private Submit bytemanClient;

    public void start(ResourceContext context) {
        this.resourceContext = context;
        getBytemanClient(); // creates its
    }

    public void stop() {
        this.bytemanClient = null;
    }

    public AvailabilityType getAvailability() {
        try {
            getBytemanClient().getAgentVersion();
            return AvailabilityType.UP;
        } catch (Exception e) {
            return AvailabilityType.DOWN;
        }
    }

    /**
     * The plugin container will call this method when your resource component has been scheduled to collect some
     * measurements now. It is within this method that you actually talk to the managed resource and collect the
     * measurement data that is has emitted.
     *
     * @see MeasurementFacet#getValues(MeasurementReport, Set)
     */
    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> requests) {
        Submit client = getBytemanClient();

        for (MeasurementScheduleRequest request : requests) {
            String name = request.getName();

            try {
                if (name.equals("totalNumberOfRules")) {
                    int total = 0;
                    Map<String, String> allScripts = client.getAllScripts();
                    if (allScripts != null) {
                        for (String script : allScripts.values()) {
                            total += client.splitAllRulesFromScript(script).size();
                        }
                    }
                    report.addData(new MeasurementDataNumeric(request, Double.valueOf((double) total)));
                }
            } catch (Exception e) {
                log.error("Failed to obtain measurement [" + name + "]. Cause: " + e);
            }
        }

        return;
    }

    /**
     * The plugin container will call this method when it wants to invoke an operation on your managed resource. Your
     * plugin will connect to the managed resource and invoke the analogous operation in your own custom way.
     *
     * @see OperationFacet#invokeOperation(String, Configuration)
     */
    public OperationResult invokeOperation(String name, Configuration configuration) {

        OperationResult result = new OperationResult();
        Submit client = getBytemanClient();

        try {
            if ("getRule".equals(name)) {
                //
                // getRule == retrieves the rule definition for a given rule
                String ruleName = configuration.getSimpleValue("ruleName", null);
                if (ruleName == null || ruleName.length() == 0) {
                    throw new Exception("Did not specify the name of the rule to get");
                }
                Map<String, String> allScripts = client.getAllScripts();
                for (String script : allScripts.values()) {
                    List<String> rules = client.splitAllRulesFromScript(script);
                    for (String rule : rules) {
                        if (ruleName.equals(client.determineRuleName(rule))) {
                            Configuration resultConfig = result.getComplexResults();
                            resultConfig.put(new PropertySimple("ruleDefinition", rule));
                            return result;
                        }
                    }
                }
                throw new Exception("No rule was found with the name [" + ruleName + "]");
            } else if ("getClientVersion".equals(name)) {
                //
                // getClientVersion == return the version string of the client this plugin is using
                String clientVersion = client.getClientVersion();
                Configuration resultConfig = result.getComplexResults();
                resultConfig.put(new PropertySimple("version", (clientVersion == null) ? "<unknown>" : clientVersion));
                return result;
            } else if ("addJarsToSystemClasspath".equals(name)) {
                //
                // addJarsToSystemClasspath == adds a jar to the remote byteman agent's system classpath 
                String jarPaths = configuration.getSimpleValue("jarPaths", null);
                if (jarPaths == null || jarPaths.length() == 0) {
                    throw new Exception("Did not specify any jars to add");
                }
                String[] jarPathsArr = jarPaths.split(",");
                List<String> jarPathList = new ArrayList<String>();
                for (String jarPathString : jarPathsArr) {
                    jarPathList.add(jarPathString);
                }
                String response = client.addJarsToSystemClassloader(jarPathList);
                result.setSimpleResult(response);
                return result;
            } else if ("addJarsToBootClasspath".equals(name)) {
                //
                // addJarsToBootClasspath == adds a jar to the remote byteman agent's boot classpath 
                String jarPaths = configuration.getSimpleValue("jarPaths", null);
                if (jarPaths == null || jarPaths.length() == 0) {
                    throw new Exception("Did not specify any jars to add");
                }
                String[] jarPathsArr = jarPaths.split(",");
                List<String> jarPathList = new ArrayList<String>();
                for (String jarPathString : jarPathsArr) {
                    jarPathList.add(jarPathString);
                }
                String response = client.addJarsToBootClassloader(jarPathList);
                result.setSimpleResult(response);
                return result;
            } else {
                throw new UnsupportedOperationException(name);
            }
        } catch (Exception e) {
            result.setErrorMessage(ThrowableUtil.getAllMessages(e));
            return result;
        }
    }

    /**
     * The plugin container will call this method and it needs to obtain the current configuration of the managed
     * resource. Your plugin will obtain the managed resource's configuration in your own custom way and populate the
     * returned Configuration object with the managed resource's configuration property values.
     *
     * @see ConfigurationFacet#loadResourceConfiguration()
     */
    public Configuration loadResourceConfiguration() {
        // here we simulate the loading of the managed resource's configuration

        if (resourceConfiguration == null) {
            // for this example, we will create a simple dummy configuration to start with.
            // note that it is empty, so we're assuming there are no required configs in the plugin descriptor.
            resourceConfiguration = new Configuration();
        }

        Configuration config = resourceConfiguration;

        return config;
    }

    /**
     * The plugin container will call this method when it has a new configuration for your managed resource. Your plugin
     * will re-configure the managed resource in your own custom way, setting its configuration based on the new values
     * of the given configuration.
     *
     * @see ConfigurationFacet#updateResourceConfiguration(ConfigurationUpdateReport)
     */
    public void updateResourceConfiguration(ConfigurationUpdateReport report) {
        // this simulates the plugin taking the new configuration and reconfiguring the managed resource
        resourceConfiguration = report.getConfiguration().deepCopy();

        report.setStatus(ConfigurationUpdateStatus.SUCCESS);
    }

    /**
     * When this is called, the plugin is responsible for scanning its managed resource and look for content that need
     * to be managed for that resource. This method should only discover packages of the given package type.
     *
     * @see ContentFacet#discoverDeployedPackages(PackageType)
     */
    public Set<ResourcePackageDetails> discoverDeployedPackages(PackageType type) {
        return null;
    }

    /**
     * The plugin container calls this method when new packages need to be deployed/installed on resources.
     *
     * @see ContentFacet#deployPackages(Set, ContentServices)
     */
    public DeployPackagesResponse deployPackages(Set<ResourcePackageDetails> packages, ContentServices contentServices) {
        return null;
    }

    /**
     * When a remote client wants to see the actual data content for an installed package, this method will be called.
     * This method must return a stream of data containing the full content of the package.
     *
     * @see ContentFacet#retrievePackageBits(ResourcePackageDetails)
     */
    public InputStream retrievePackageBits(ResourcePackageDetails packageDetails) {
        return null;
    }

    /**
     * This is the method that is used when the component has to create the installation steps and their results.
     *
     * @see ContentFacet#generateInstallationSteps(ResourcePackageDetails)
     */
    public List<DeployPackageStep> generateInstallationSteps(ResourcePackageDetails packageDetails) {
        return null;
    }

    /**
     * This is called when the actual content of packages should be deleted from the managed resource.
     *
     * @see ContentFacet#removePackages(Set)
     */
    public RemovePackagesResponse removePackages(Set<ResourcePackageDetails> packages) {
        return null;
    }

    /**
     * When called, the plugin container is asking the plugin to create a new managed resource. The new resource's
     * details need to be added to the given report.
     *
     * @see CreateChildResourceFacet#createResource(CreateResourceReport)
     */
    public CreateResourceReport createResource(CreateResourceReport report) {
        return null;
    }

    /**
     * When called, the plugin container is asking the plugin to delete a managed resource.
     *
     * @see DeleteResourceFacet#deleteResource()
     */
    public void deleteResource() {
    }

    public Submit getBytemanClient() {
        if (this.bytemanClient == null) {
            Configuration pluginConfiguration = this.resourceContext.getPluginConfiguration();

            // get the address/port from the plugin config - defaults will be null to force NPEs which is OK, because nulls are error conditions
            String address = pluginConfiguration.getSimpleValue(BytemanDiscoveryComponent.PLUGIN_CONFIG_PROP_ADDRESS,
                null);
            String port = pluginConfiguration.getSimpleValue(BytemanDiscoveryComponent.PLUGIN_CONFIG_PROP_PORT, null);

            this.bytemanClient = new Submit(address, Integer.valueOf(port).intValue());
        }

        return this.bytemanClient;
    }
}
