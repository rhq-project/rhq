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

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.system.OperatingSystemType;
import org.rhq.core.system.SystemInfo;
import org.rhq.enterprise.agent.Version;

/**
 * This is the discovery component for the agent Java Service Wrapper.
 * Today, the JSW is only used on Windows platforms.
 *
 * @author John Mazzitelli
 */
public class AgentJavaServiceWrapperDiscoveryComponent implements ResourceDiscoveryComponent<AgentServerComponent> {
    private final Log log = LogFactory.getLog(AgentJavaServiceWrapperDiscoveryComponent.class);

    /**
     * The name of the plugin configuration simple property whose value is the JSW's configuration file path.
     * Package scoped so the component can use this.
     */
    static final String PLUGINCONFIG_CONF_FILE = "configurationFile";

    /**
     * The name of the plugin configuration simple property whose value is the JSW's environment file path.
     * Package scoped so the component can use this.
     */
    static final String PLUGINCONFIG_ENV_FILE = "environmentFile";

    /**
     * The name of the plugin configuration simple property whose value is the JSW's include file path.
     * Package scoped so the component can use this.
     */
    static final String PLUGINCONFIG_INC_FILE = "includeFile";

    /**
     * Simply returns the service resource.
     *
     * @see ResourceDiscoveryComponent#discoverResources(ResourceDiscoveryContext)
     */
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<AgentServerComponent> context) {
        log.info("Discovering RHQ Agent's JSW service...");

        HashSet<DiscoveredResourceDetails> set = new HashSet<DiscoveredResourceDetails>();

        try {
            String baseName = getBaseName(context.getSystemInformation());

            if (baseName != null) {
                EmsAttribute attrib = context.getParentResourceComponent().getAgentBean().getAttribute("Version");
                attrib.refresh();
                String version;
                if (attrib != null && attrib.getValue() != null) {
                    version = attrib.getValue().toString();
                } else {
                    version = Version.getProductVersion(); // just use the one we can get statically, its probably the correct version
                }

                // we know our agent plugin is running in the same process as our agent
                // so the conf file must be on the same box that we are running on.
                // try to find the file in one of several ways - but once we find it, stop (there is only ever one of them)
                if (!findInAgentHome(context, version, baseName, set)) {
                    log.warn("Could not find the agent's JSW anywhere");
                }
            }
        } catch (Exception e) {
            log.error("An error occurred while attempting to auto-discover the agent's JSW", e);
        }

        return set;
    }

    /**
     * Looks for the JSW relative to the agent home directory.
     * 
     * @param context
     * @param version
     * @param baseName
     * @param discoveries where the new details are stored if the JSW is discovered
     * 
     * @return <code>true</code> if this method discovers the JSW; <code>false</code> if not
     */
    private boolean findInAgentHome(ResourceDiscoveryContext<AgentServerComponent> context, String version,
        String baseName, HashSet<DiscoveredResourceDetails> discoveries) {

        try {
            EmsAttribute home = context.getParentResourceComponent().getAgentBean().getAttribute("AgentHomeDirectory");
            home.refresh();
            Object agentHome = home.getValue();
            if (agentHome != null) {
                File file = new File(agentHome.toString(), baseName);
                if (file.exists()) {
                    discoveries.add(createDetails(context, version, file));
                }
            }

            return discoveries.size() > 0;
        } catch (Exception e) {
            log.debug("Cannot use agent home to find JSW. Cause: " + e);
            return false;
        }
    }

    private DiscoveredResourceDetails createDetails(ResourceDiscoveryContext<AgentServerComponent> context,
        String version, File discoveredLocation) {

        String key = "jsw"; // this is a singleton resource; only ever one of these
        String name = "RHQ Agent Java Service Wrapper";
        String description = "A native wrapper that can start the agent and manage its lifecycle as a service";
        String location;
        try {
            location = discoveredLocation.getCanonicalPath(); // try to get the canonical path but...
        } catch (Exception e) {
            location = discoveredLocation.getAbsolutePath(); // ...if we can't, use absolute path
        }

        File conf = new File(location, "rhq-agent-wrapper.conf");
        File env = new File(location, "rhq-agent-wrapper.env");
        File inc = new File(location, "rhq-agent-wrapper.inc");

        DiscoveredResourceDetails details = new DiscoveredResourceDetails(context.getResourceType(), key, name,
            version, description, null, null);
        Configuration pc = details.getPluginConfiguration();
        // put the paths in the config, even if they don't exist on the file system
        pc.put(new PropertySimple(PLUGINCONFIG_CONF_FILE, conf.getAbsoluteFile()));
        pc.put(new PropertySimple(PLUGINCONFIG_ENV_FILE, env.getAbsoluteFile()));
        pc.put(new PropertySimple(PLUGINCONFIG_INC_FILE, inc.getAbsoluteFile()));
        return details;
    }

    /**
     * Returns the base filename where the JSW is to be found.
     * This is dependent on the platform we are running on.
     * Windows machines will return a non-<code>null</code> string but because the
     * JSW is not used by the agent on UNIX machines, <code>null</code> will be returned
     * when running on non-Windows machines.
     * 
     * @param sysInfo used to determine what platform we are on
     * 
     * @return the location of the JSW that we are trying to discover, or <code>null</code> if we
     *         should not try to discovery the JSW. The returned location is relative to agent home.
     */
    private String getBaseName(SystemInfo sysInfo) {
        String location = null;

        try {
            OperatingSystemType osType = sysInfo.getOperatingSystemType();

            if (osType == OperatingSystemType.JAVA) {
                String osName = System.getProperty("os.name", "").toLowerCase();
                osType = osName.contains("windows") ? OperatingSystemType.WINDOWS : OperatingSystemType.LINUX;
            }

            if (osType == OperatingSystemType.WINDOWS) {
                location = "bin\\wrapper";
            }
        } catch (Exception e) {
            // can't determine os type, assume unix
        }

        return location;
    }
}