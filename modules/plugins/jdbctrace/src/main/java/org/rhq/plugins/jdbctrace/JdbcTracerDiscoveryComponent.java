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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.StringReader;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ManualAddFacet;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.util.stream.StreamUtil;
import org.rhq.plugins.byteman.BytemanAgentComponent;

/**
 * Discovers a JDBC tracer. Usually, this just performs "manual add" discovery, but can auto-detect one if
 * a VM has the JDBC tracer rules already installed.
 *
 * The JDBC Trace component is designed to be a singleton - there really should never be more than one
 * of them under a single byteman agent parent resource.
 *
 * @author John Mazzitelli
 */
public class JdbcTracerDiscoveryComponent implements ResourceDiscoveryComponent<BytemanAgentComponent>,
    ManualAddFacet<BytemanAgentComponent> {

    private final Log log = LogFactory.getLog(JdbcTracerDiscoveryComponent.class);

    private static final String RESOURCE_KEY_PREFIX = "jdbctrace";
    private static final String RESOURCE_NAME = "JDBC Tracer";
    private static final String RESOURCE_DESC = "Byte-injection rules that trace JDBC invocations";

    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<BytemanAgentComponent> context) {
        log.info("Discovering JDBC tracer");

        HashSet<DiscoveredResourceDetails> set = new HashSet<DiscoveredResourceDetails>();

        Map<String, String> allKnownScripts = context.getParentResourceComponent().getAllKnownScripts();
        String discoveredJdbcTraceScriptName = null;
        String discoveredJdbcTraceScriptContent = null;
        if (allKnownScripts != null) {
            for (Map.Entry<String, String> entry : allKnownScripts.entrySet()) {
                String scriptName = entry.getKey();
                File file = new File(scriptName);
                if (JdbcTracerUtil.DEFAULT_JDBC_TRACER_SCRIPT_NAME.equals(file.getName())) {
                    // it looks like the Byteman agent already has the JDBC tracer script installed! Let's use it
                    discoveredJdbcTraceScriptName = scriptName;
                    discoveredJdbcTraceScriptContent = entry.getValue();
                    log.info("Auto-discovered an existing JDBC Tracer resource. script="
                        + discoveredJdbcTraceScriptName);
                    break;
                }
            }
        }

        if (discoveredJdbcTraceScriptName != null) {
            String key = RESOURCE_KEY_PREFIX + '-' + JdbcTracerUtil.DEFAULT_JDBC_TRACER_SCRIPT_NAME;
            String name = RESOURCE_NAME;
            String description = RESOURCE_DESC;
            String version = determineJdbcScriptVersion(discoveredJdbcTraceScriptContent);

            Configuration pc = context.getDefaultPluginConfiguration();
            pc.put(new PropertySimple(JdbcTracerUtil.PLUGINCONFIG_ENABLED, "true"));
            pc.put(new PropertySimple(JdbcTracerUtil.PLUGINCONFIG_SCRIPTNAME,
                JdbcTracerUtil.DEFAULT_JDBC_TRACER_SCRIPT_NAME));

            DiscoveredResourceDetails resource = new DiscoveredResourceDetails(context.getResourceType(), key, name,
                version, description, pc, null);

            set.add(resource);
        }

        return set;
    }

    public DiscoveredResourceDetails discoverResource(Configuration pluginConfiguration,
        ResourceDiscoveryContext<BytemanAgentComponent> context) throws InvalidPluginConfigurationException {

        String scriptName;
        String scriptContent;

        try {
            scriptName = pluginConfiguration.getSimpleValue(JdbcTracerUtil.PLUGINCONFIG_SCRIPTNAME,
                JdbcTracerUtil.DEFAULT_JDBC_TRACER_SCRIPT_NAME);
            BytemanAgentComponent bytemanAgentResource = context.getParentResourceComponent();
            File scriptFile = new JdbcTracerUtil().extractJdbcTraceRulesScriptFile(bytemanAgentResource, scriptName);
            scriptContent = new String(StreamUtil.slurp(new FileInputStream(scriptFile)));
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract jdbc trace script file", e);
        }

        String key = RESOURCE_KEY_PREFIX + '-' + scriptName;
        String name = RESOURCE_NAME;
        String description = RESOURCE_DESC;
        String version = determineJdbcScriptVersion(scriptContent);

        DiscoveredResourceDetails resource = new DiscoveredResourceDetails(context.getResourceType(), key, name,
            version, description, pluginConfiguration, null);

        return resource;
    }

    /**
     * Will examine the script content and determine the version of the script.
     * In order for this to determine a version, there must be a variable binding in
     * the script that defines a String variable named "_version". This _version
     * binding must be the first binding defined in the BIND statement.
     *
     * @param scriptContent
     * @return the script version if it could be determined - if not, a constant string will be returned
     *         to indicate an indeterminate version.
     */
    private String determineJdbcScriptVersion(String scriptContent) {
        try {
            Pattern ruleNamePattern = Pattern.compile("\\s*BIND\\s+_version:String\\s*=\\s*\"(.+)\"\\s*,?\\s*");
            Matcher matcher;
            String version = null;
            BufferedReader reader = new BufferedReader(new StringReader(scriptContent));
            String line = reader.readLine();
            while (line != null && version == null) {
                matcher = ruleNamePattern.matcher(line);
                if (matcher.matches()) {
                    version = matcher.group(1);
                }
                line = reader.readLine();
            }
            return (version != null) ? version : "unversioned";
        } catch (Exception e) {
            // should never really happen, just ignore it
            return "unknown";
        }
    }
}