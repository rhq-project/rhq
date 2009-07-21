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
package org.rhq.plugins.script;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ProcessScanResult;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.system.ProcessExecutionResults;
import org.rhq.core.util.exception.ThrowableUtil;

/**
 * Discovery component that allows you to manually add your own CLI-managed resource, where
 * the CLI is some executable binary or script.
 * 
 * The CLI executable/script itself is not the managed resource, it is merely the management front end
 * to the actual managed resource.
 *
 * @author John Mazzitelli
 */
public class ScriptDiscoveryComponent implements ResourceDiscoveryComponent {
    private final Log log = LogFactory.getLog(ScriptDiscoveryComponent.class);

    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext context) {
        log.info("Processing discovered management script resources");

        HashSet<DiscoveredResourceDetails> details = new HashSet<DiscoveredResourceDetails>();

        // if subclasses defined one or more <process-scan>s, then see if the plugin container
        // auto-discovered processes using those process scan definitions.  Process all those that were found.
        List<ProcessScanResult> autoDiscoveryResults = context.getAutoDiscoveredProcesses();
        for (ProcessScanResult autoDiscoveryResult : autoDiscoveryResults) {
            DiscoveredResourceDetails autoDiscovered = processAutoDiscoveredResource(context, autoDiscoveryResult);
            if (autoDiscovered != null) {
                details.add(autoDiscovered);
            }
        }

        // pluginConfigs contain information on resources that were manually discovered/entered by the user;
        // take them and build details objects that represent that resources
        List<Configuration> pluginConfigs = context.getPluginConfigurations();
        for (Configuration pluginConfig : pluginConfigs) {
            DiscoveredResourceDetails manuallyAdded = processManuallyAddedResource(context, pluginConfig);
            if (manuallyAdded != null) {
                details.add(manuallyAdded);
            }
        }

        // We are done - this discovery component does not auto-discovery any resources on its own.
        // It will only deal with manually added resources or resources that were discovered via process scans.

        return details;
    }

    /**
     * Subclasses can override this method to process manually added resources.
     * This method is called for each resource that was manually added.
     * Implementors must return a details object that represents the new resource.
     * If this returns <code>null</code>, no resource will be discovered.
     *  
     * @param context the discovery context
     * @param pluginConfig information on the manually added resource
     * 
     * @return the details object that represents the new resource
     */
    protected DiscoveredResourceDetails processManuallyAddedResource(ResourceDiscoveryContext context,
        Configuration pluginConfig) {

        String executable = pluginConfig.getSimple(ScriptServerComponent.PLUGINCONFIG_EXECUTABLE).getStringValue();
        String version = determineVersion(context, pluginConfig);
        String description = determineDescription(context, pluginConfig);
        DiscoveredResourceDetails details = new DiscoveredResourceDetails(context.getResourceType(), executable,
            new File(executable).getName(), version, description, pluginConfig, null);
        return details;
    }

    /**
     * Subclasses should override this method if their plugin descriptors defined process scans.
     * This method is called for each process that was discovered that matched a process scan.
     * Implementors must return a details object that represents the discovered resource.
     * If this returns <code>null</code>, this process will be ignored and no resource will be discovered.
     * 
     * @param context the discovery context
     * @param autoDiscoveryResult information on the discovered process
     * 
     * @return the details object that represents the discovered resource
     */
    protected DiscoveredResourceDetails processAutoDiscoveredResource(ResourceDiscoveryContext context,
        ProcessScanResult autoDiscoveryResult) {

        return null; // this implementation is a no-op - nothing will be discovered
    }

    /**
     * Attempts to determine the description of the resource managed by the CLI.
     * 
     * @param context
     * @param pluginConfig
     *
     * @return the description or <code>null</code> if it could not be determined
     */
    protected String determineDescription(ResourceDiscoveryContext context, Configuration pluginConfig) {
        String description = null;
        try {
            PropertySimple descriptionProp = pluginConfig.getSimple(ScriptServerComponent.PLUGINCONFIG_FIXED_DESC);
            if (descriptionProp != null && descriptionProp.getStringValue() != null) {
                description = descriptionProp.getStringValue();
            } else {
                String args = pluginConfig.getSimpleValue(ScriptServerComponent.PLUGINCONFIG_DESC_ARGS, null);
                ProcessExecutionResults results = ScriptServerComponent.executeExecutable(context
                    .getSystemInformation(), pluginConfig, args, 5000L, true);
                if (results != null) {
                    if (results.getError() != null) {
                        log.warn("Failed to execute cli executable to get description. Cause: "
                            + ThrowableUtil.getAllMessages(results.getError()));
                    } else if (results.getCapturedOutput() != null) {
                        String regex = pluginConfig.getSimpleValue(ScriptServerComponent.PLUGINCONFIG_DESC_REGEX, null);
                        String output = results.getCapturedOutput();
                        if (regex == null) {
                            description = output;
                        } else {
                            Pattern pattern = Pattern.compile(regex);
                            Matcher match = pattern.matcher(output);
                            if (match.find()) {
                                if (match.groupCount() > 0) {
                                    description = match.group(1);
                                } else {
                                    description = output;
                                }
                            } else {
                                log.warn("Description regex [" + regex + "] did not match: " + truncateString(output));
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Cannot determine description of CLI server resource. Cause: " + e);
        }

        return description;
    }

    /**
     * Attempts to determine the version of the resource managed by the CLI.
     * 
     * @param context
     * @param pluginConfig
     *
     * @return the version or <code>null</code> if it could not be determined
     */
    protected String determineVersion(ResourceDiscoveryContext context, Configuration pluginConfig) {
        String version = null;
        try {
            PropertySimple versionProp = pluginConfig.getSimple(ScriptServerComponent.PLUGINCONFIG_FIXED_VERSION);
            if (versionProp != null && versionProp.getStringValue() != null) {
                version = versionProp.getStringValue();
            } else {
                String args = pluginConfig.getSimpleValue(ScriptServerComponent.PLUGINCONFIG_VERSION_ARGS, null);
                ProcessExecutionResults results = ScriptServerComponent.executeExecutable(context
                    .getSystemInformation(), pluginConfig, args, 5000L, true);
                if (results != null) {
                    if (results.getError() != null) {
                        log.warn("Failed to execute cli executable to get version. Cause: "
                            + ThrowableUtil.getAllMessages(results.getError()));
                    } else if (results.getCapturedOutput() != null) {
                        String regex = pluginConfig.getSimpleValue(ScriptServerComponent.PLUGINCONFIG_VERSION_REGEX,
                            null);
                        String output = results.getCapturedOutput();
                        if (regex == null) {
                            version = output;
                        } else {
                            Pattern pattern = Pattern.compile(regex);
                            Matcher match = pattern.matcher(output);
                            if (match.find()) {
                                if (match.groupCount() > 0) {
                                    version = match.group(1);
                                } else {
                                    version = output;
                                }
                            } else {
                                log.warn("Version regex [" + regex + "] did not match: " + truncateString(output));
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Cannot determine version of CLI server resource. Cause: " + e);
        }
        return version;
    }

    /**
     * Truncate a string so it is short, usually for display or logging purposes.
     * 
     * @param output the output to trim
     * @return the trimmed output
     */
    private String truncateString(String output) {
        String outputToLog = output;
        if (outputToLog != null && outputToLog.length() > 100) {
            outputToLog = outputToLog.substring(0, 100) + "...";
        }
        return outputToLog;
    }
}