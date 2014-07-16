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
package org.rhq.plugins.jdbctrace;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jboss.byteman.agent.submit.Submit;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.plugins.byteman.BytemanAgentComponent;

/**
 * Component that can trace JDBC calls via byte-code maniuplation.
 *
 * @author John Mazzitelli
 */
public class JdbcTracerComponent implements ResourceComponent<BytemanAgentComponent>, OperationFacet,
    ConfigurationFacet {

    private final Log log = LogFactory.getLog(JdbcTracerComponent.class);

    private Configuration resourceConfiguration;
    private ResourceContext<BytemanAgentComponent> resourceContext;

    public void start(ResourceContext<BytemanAgentComponent> context) {
        this.resourceContext = context;

        try {
            prepareRules(true);
        } catch (Exception e) {
            log.warn("Failed to prepare the jdbc trace rules in the remote byteman agent", e);
        }

        return;
    }

    public void stop() {
        this.resourceContext = null;
    }

    public AvailabilityType getAvailability() {
        try {
            prepareRules(false);
            // the above always makes sure the remote byteman agent has the rules loaded; so we can always return UP here
            return AvailabilityType.UP;
        } catch (Exception e) {
            log.debug("Failed to prepare the jdbc trace rules in the remote byteman agent during avail check", e);
            return AvailabilityType.DOWN;
        }
    }

    public OperationResult invokeOperation(String name, Configuration configuration) {
        OperationResult result = new OperationResult();

        try {
            if ("refresh".equals(name)) {
                prepareRules(true);
            } else {
                throw new UnsupportedOperationException(name);
            }
        } catch (Exception e) {
            result.setErrorMessage(ThrowableUtil.getAllMessages(e));
        }

        return result;
    }

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

    public void updateResourceConfiguration(ConfigurationUpdateReport report) {
        // this simulates the plugin taking the new configuration and reconfiguring the managed resource
        resourceConfiguration = report.getConfiguration().deepCopy();

        report.setStatus(ConfigurationUpdateStatus.SUCCESS);
    }

    /**
     * Returns <code>true</code> if this component's plugin configuration tells us the JDBC
     * tracing is to be enabled. If <code>true</code>, the remote Byteman agent will (or should) have
     * the JDBC trace rules installed.
     *
     * @return <code>true</code> if this component's JDBC tracing is enabled
     */
    private boolean isEnabled() {
        Configuration pc = this.resourceContext.getPluginConfiguration();
        String enabledString = pc.getSimpleValue(JdbcTracerUtil.PLUGINCONFIG_ENABLED, "true");
        return Boolean.parseBoolean(enabledString);
    }

    /**
     * If this resource has been "enabled" (see its plugin configuration), this will ensure that the script
     * is added to the remote Byteman agent; if it is not enabled, the script will be deleted from the
     * remote Byteman agent.
     *
     * @param refresh if <code>true</code>, this will ensure the byteman agent has up to date rules. This means
     *                that even if the remote byteman agent already has the script installed, this method will
     *                delete the script and immediately re-install the current one. This parameter is meaningless
     *                if this resource has not been enabled (because, in that case, the script is deleted from
     *                the byteman agent so there is nothing to keep up to date)
     *
     * @throws Exception
     */
    private void prepareRules(boolean refresh) throws Exception {
        Configuration pc = this.resourceContext.getPluginConfiguration();
        String scriptName = pc.getSimpleValue(JdbcTracerUtil.PLUGINCONFIG_SCRIPTNAME,
            JdbcTracerUtil.DEFAULT_JDBC_TRACER_SCRIPT_NAME);
        String helperJarFileName = JdbcTracerUtil.DEFAULT_JDBC_TRACER_HELPER_JAR;

        JdbcTracerUtil jdbcTracerUtil = new JdbcTracerUtil();
        BytemanAgentComponent bytemanAgentResource = this.resourceContext.getParentResourceComponent();
        File script = jdbcTracerUtil.getJdbcTraceRulesScriptFile(bytemanAgentResource, scriptName);
        String scriptAbsolutePath = script.getAbsolutePath();
        File helper = jdbcTracerUtil.getHelperJarFile(bytemanAgentResource, helperJarFileName);
        String helperAbsolutePath = helper.getAbsolutePath();

        // see if there are already jdbc trace rules installed in the byteman agent
        // note that we talk directly to the remote byteman agent to get the info; our parent resource might not have our script yet in its cache
        Submit client = bytemanAgentResource.getBytemanClient();
        Map<String, String> allKnownScripts = client.getAllRules();
        String existingRules = null;
        if (allKnownScripts != null) {
            existingRules = allKnownScripts.get(scriptAbsolutePath);
        }

        // add or remove the rules as appropriate
        if (isEnabled()) {
            // if we are to refresh the script, remove the current script that is loaded (if one is loaded)
            if (existingRules != null && refresh) {
                Map<String, String> doomed = new HashMap<String, String>(1);
                doomed.put(scriptAbsolutePath, existingRules);
                client.deleteRules(doomed);
                existingRules = null;
            }

            if (refresh || !helper.exists()) {
                // the helper was deleted previously, we need to write it back out again
                jdbcTracerUtil.extractHelperJarFile(bytemanAgentResource, helperJarFileName);
            }
            if (refresh || !client.getLoadedSystemClassloaderJars().contains(helperAbsolutePath)) {
                client.addJarsToSystemClassloader(Arrays.asList(helperAbsolutePath));
            }

            if (existingRules == null) {
                if (!script.exists()) {
                    // the script was deleted previously, we need to write it back out again
                    jdbcTracerUtil.extractJdbcTraceRulesScriptFile(bytemanAgentResource, scriptName);
                }
                client.addRulesFromFiles(Arrays.asList(scriptAbsolutePath));
            }
        } else {
            if (existingRules != null) {
                Map<String, String> doomed = new HashMap<String, String>(1);
                doomed.put(scriptAbsolutePath, existingRules);
                client.deleteRules(doomed);
            }
        }

        return;
    }
}
