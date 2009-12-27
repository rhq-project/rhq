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
import java.io.FileOutputStream;
import java.io.InputStream;
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

    private static final String SINGLETON_RESOURCEKEY = "jdbctrace";
    private static final String SINGLETON_RESOURCENAME = "JDBC Tracer";
    private static final String SINGLETON_RESOURCEDESC = "Byte-injection rules that trace JDBC invocations";

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
                if (JdbcTracerComponent.DEFAULT_JDBC_TRACER_SCRIPT_NAME.equals(file.getName())) {
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
            String key = SINGLETON_RESOURCEKEY;
            String name = SINGLETON_RESOURCENAME;
            String description = SINGLETON_RESOURCEDESC;
            String version = determineJdbcScriptVersion(discoveredJdbcTraceScriptContent);

            Configuration pc = context.getDefaultPluginConfiguration();
            pc.put(new PropertySimple(JdbcTracerComponent.PLUGINCONFIG_ENABLED, "true"));
            pc.put(new PropertySimple(JdbcTracerComponent.PLUGINCONFIG_SCRIPTNAME,
                JdbcTracerComponent.DEFAULT_JDBC_TRACER_SCRIPT_NAME));

            DiscoveredResourceDetails resource = new DiscoveredResourceDetails(context.getResourceType(), key, name,
                version, description, pc, null);

            set.add(resource);
        }

        return set;
    }

    public DiscoveredResourceDetails discoverResource(Configuration pluginConfiguration,
        ResourceDiscoveryContext<BytemanAgentComponent> context) throws InvalidPluginConfigurationException {

        String scriptContent;
        try {
            String scriptName = pluginConfiguration.getSimpleValue(JdbcTracerComponent.PLUGINCONFIG_SCRIPTNAME,
                JdbcTracerComponent.DEFAULT_JDBC_TRACER_SCRIPT_NAME);
            BytemanAgentComponent bytemanAgentResource = context.getParentResourceComponent();
            File scriptFile = extractJdbcTraceRulesScriptFile(bytemanAgentResource, scriptName);
            scriptContent = new String(StreamUtil.slurp(new FileInputStream(scriptFile)));
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract jdbc trace script file", e);
        }

        String key = SINGLETON_RESOURCEKEY;
        String name = SINGLETON_RESOURCENAME;
        String description = SINGLETON_RESOURCEDESC;
        String version = determineJdbcScriptVersion(scriptContent);

        DiscoveredResourceDetails resource = new DiscoveredResourceDetails(context.getResourceType(), key, name,
            version, description, pluginConfiguration, null);

        return resource;
    }

    /**
     * Given the byteman agent resource where the JDBC tracer resource is hosted,
     * returns the file where the jdbc trace rules script file should exist.
     * 
     * This is static-package scoped so the {@link JdbcTracerComponent} can use this.
     * 
     * @param bytemanAgentComponent resource context of the byteman agent resource
     * @param scriptName the name of the jdbc rules script file (not path name, just the short file name)
     * @return the file where the script was extracted
     * 
     * @throws Exception
     */
    static File getJdbcTraceRulesScriptFile(BytemanAgentComponent bytemanAgentComponent, String scriptName) {
        File dataDir = bytemanAgentComponent.getResourceDataDirectory("jdbctrace");
        dataDir.mkdirs();
        File scriptFile = new File(dataDir, scriptName.replace('/', '-').replace('\\', '-')); // don't want it in subdirectory
        return scriptFile;
    }

    /**
     * Given the parent byteman agent resource where the JDBC tracer resource is hosted,
     * this will extract the jdbc trace rules script file and store it in a persisted data directory
     * 
     * This is static-package scoped so the {@link JdbcTracerComponent} can use this.
     * 
     * @param bytemanAgentComponent byteman agent resource
     * @param scriptName the name of the jdbc rules script file (not path name, just the short file name)
     * @return the file where the script was extracted
     * 
     * @throws Exception
     */
    static File extractJdbcTraceRulesScriptFile(BytemanAgentComponent bytemanAgentComponent, String scriptName)
        throws Exception {

        // extract the script file from our plugin jar into our parent byteman component's data directory
        File scriptFile = getJdbcTraceRulesScriptFile(bytemanAgentComponent, scriptName);

        InputStream resourceAsStream = JdbcTracerDiscoveryComponent.class.getResourceAsStream("/" + scriptName);
        if (resourceAsStream == null) {
            throw new Exception("Cannot find JDBC tracer rules file from classloader");
        }
        StreamUtil.copy(resourceAsStream, new FileOutputStream(scriptFile), true);

        LogFactory.getLog(JdbcTracerDiscoveryComponent.class).debug(
            "Extracted jdbc trace script file from plugin jar to [" + scriptFile.getAbsolutePath() + "]");

        return scriptFile;
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