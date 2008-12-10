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
package org.rhq.plugins.agent;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mc4j.ems.connection.bean.attribute.EmsAttribute;

import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.system.OperatingSystemType;
import org.rhq.core.system.SystemInfo;
import org.rhq.enterprise.agent.Version;

/**
 * This is the discovery component for the agent environment setup script.
 *
 * @author John Mazzitelli
 */
public class AgentEnvironmentScriptDiscoveryComponent implements ResourceDiscoveryComponent<AgentServerComponent> {
    private final Log log = LogFactory.getLog(AgentEnvironmentScriptDiscoveryComponent.class);

    /**
     * The name of the plugin configuration simple property whose value is the script's path.
     * Package scoped so the component can use this.
     */
    static final String PLUGINCONFIG_PATHNAME = "Pathname";

    /**
     * Simply returns the agent env script resource.
     *
     * @see ResourceDiscoveryComponent#discoverResources(ResourceDiscoveryContext)
     */
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<AgentServerComponent> context) {
        log.info("Discovering RHQ Agent's environment setup script...");

        HashSet<DiscoveredResourceDetails> set = new HashSet<DiscoveredResourceDetails>();

        try {
            String baseName = getScriptBaseName(context.getSystemInformation());
            EmsAttribute attrib = context.getParentResourceComponent().getAgentBean().getAttribute("Version");
            String version;
            if (attrib != null && attrib.getValue() != null) {
                version = attrib.getValue().toString();
            } else {
                version = Version.getProductVersion(); // just use the one we can get statically, its probably the correct version
            }

            // we know our agent plugin is running in the same process as our agent
            // so the env script file must be on the same box that we are running on.
            if (!findInAgentHome(context, version, baseName, set)) {
                log.warn("Could not find the agent environment setup script anywhere");
            }
        } catch (Exception e) {
            log.error("An error occurred while attempting to auto-discover the agent's environment setup script", e);
        }

        return set;
    }

    /**
     * Looks for the script relative to the agent home directory.
     * 
     * @param context
     * @param version
     * @param baseName
     * @param discoveries where the new details are stored if the script is discovered
     * 
     * @return <code>true</code> if this method discovers the script; <code>false</code> if not
     */
    private boolean findInAgentHome(ResourceDiscoveryContext<AgentServerComponent> context, String version,
        String baseName, HashSet<DiscoveredResourceDetails> discoveries) {

        try {
            EmsAttribute home = context.getParentResourceComponent().getAgentBean().getAttribute("AgentHomeDirectory");
            home.refresh();
            Object agentHome = home.getValue();
            if (agentHome != null) {
                File file = new File(agentHome.toString(), "bin/" + baseName);
                if (file.exists()) {
                    discoveries.add(createDetails(context, version, file));
                }
            }

            return discoveries.size() > 0;
        } catch (Exception e) {
            log.debug("Cannot use agent home to find environment script. Cause: " + e);
            return false;
        }
    }

    private DiscoveredResourceDetails createDetails(ResourceDiscoveryContext<AgentServerComponent> context,
        String version, File discoveredScript) {

        String key = "environment-setup-script"; // this is a singleton resource; only ever one of these
        String description = "RHQ Agent Environment Setup Script";
        String pathname;
        try {
            pathname = discoveredScript.getCanonicalPath(); // try to get the canonical path but...
        } catch (Exception e) {
            pathname = discoveredScript.getAbsolutePath(); // ...if we can't, use absolute path
        }

        DiscoveredResourceDetails details = new DiscoveredResourceDetails(context.getResourceType(), key,
            discoveredScript.getName(), version, description, null, null);
        details.getPluginConfiguration().put(new PropertySimple(PLUGINCONFIG_PATHNAME, pathname));
        return details;
    }

    /**
     * Returns the base filename of the script we are looking for. This
     * is dependent on the platform we are running on (e.g. Windows
     * machines will have a .bat extension on the file where UNIX
     * machines will have a .sh extension).
     * 
     * @param sysInfo used to determine what platform we are on
     * 
     * @return the script filename we are trying to discover
     */
    private String getScriptBaseName(SystemInfo sysInfo) {
        String extension;

        try {
            OperatingSystemType osType = sysInfo.getOperatingSystemType();

            if (osType == OperatingSystemType.JAVA) {
                String osName = System.getProperty("os.name", "").toLowerCase();
                osType = osName.contains("windows") ? OperatingSystemType.WINDOWS : OperatingSystemType.LINUX;
            }

            if (osType == OperatingSystemType.WINDOWS) {
                extension = "bat";
            } else {
                extension = "sh";
            }
        } catch (Exception e) {
            extension = ".sh"; // can't determine os type, assume unix
        }

        return "rhq-agent-env." + extension;
    }
}