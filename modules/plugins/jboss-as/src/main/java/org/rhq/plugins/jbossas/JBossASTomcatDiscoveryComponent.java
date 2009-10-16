 /*
  * Jopr Management Platform
  * Copyright (C) 2005-2008 Red Hat, Inc.
  * All rights reserved.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of the GNU General Public License, version 2, as
  * published by the Free Software Foundation, and/or the GNU Lesser
  * General Public License, version 2.1, also as published by the Free
  * Software Foundation.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
  * GNU General Public License and the GNU Lesser General Public License
  * for more details.
  *
  * You should have received a copy of the GNU General Public License
  * and the GNU Lesser General Public License along with this program;
  * if not, write to the Free Software Foundation, Inc.,
  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
  */
package org.rhq.plugins.jbossas;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.system.ProcessInfo;

/**
 * Discovers embedded Tomcat servers (that is, those embedded in JBossAS servers).
 *
 * @author Greg Hinkle
 * @author Ian Springer
 */
public class JBossASTomcatDiscoveryComponent implements ResourceDiscoveryComponent<JBossASServerComponent> {
    private final Log log = LogFactory.getLog(JBossASTomcatDiscoveryComponent.class);

    public static final String EMBEDDED_TOMCAT_PRE42_DIR = "jbossweb-tomcat";
    public static final String EMBEDDED_TOMCAT_42_DIR = "jboss-web.";
    private static final String SERVER_INFO_PROPERTIES_RESOURCE = "org/apache/catalina/util/ServerInfo.properties";

    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<JBossASServerComponent> context)
        throws InvalidPluginConfigurationException, Exception {
        log.debug("Discovering Tomcat servers embedded in JBossAS server...");

        File configDir = context.getParentResourceComponent().getConfigurationPath();
        File deployDir = new File(configDir, "deploy");

        File[] jbossWebDirs = deployDir.listFiles(new FileFilter() {
            public boolean accept(File file) {
                boolean pre42 = file.getName().startsWith(EMBEDDED_TOMCAT_PRE42_DIR);
                boolean is42 = file.getName().startsWith(EMBEDDED_TOMCAT_42_DIR);
                return file.isDirectory() && (pre42 || is42);
            }
        });

        if ((!deployDir.isDirectory()) || (null == jbossWebDirs)) {
            throw new InvalidPluginConfigurationException("Invalid deploy directory: " + deployDir.getAbsolutePath());
        }

        Set<DiscoveredResourceDetails> set = new HashSet<DiscoveredResourceDetails>();
        for (File jbossWebDir : jbossWebDirs) {
            String key = jbossWebDir.getName();

            // TODO GH: Get bound addresses and ports or something
            String hostname = context.getSystemInformation().getHostname();
            boolean pre42 = jbossWebDir.getName().startsWith(EMBEDDED_TOMCAT_PRE42_DIR);
            String serverName = (pre42) ? "Tomcat" : "JBossWeb";
            String bindAddress = context.getParentResourceComponent().getBindingAddress();
            String version = getVersion(jbossWebDir);
            String name = getResourceName(hostname, serverName, version, bindAddress);
            String description = "JBossAS-Embedded " + serverName + " Web Server (" + jbossWebDir.getName()
                + File.separator + ")";
            Configuration pluginConfig = null;
            ProcessInfo processInfo = null;
            DiscoveredResourceDetails resource = new DiscoveredResourceDetails(context.getResourceType(), key, name,
                version, description, pluginConfig, processInfo);
            set.add(resource);
        }

        return set;
    }

    private String getResourceName(String hostName, String serverName, String version, String bindAddress) {
        if ((bindAddress != null) && (!bindAddress.equals("") || (!bindAddress.equals("0.0.0.0")))) {
            bindAddress = " (" + bindAddress + ")";
        } else {
            bindAddress = "";
        }

        return ((hostName == null) ? "" : (hostName + " ")) + " Embedded " + serverName + " Server " + version + " "
            + bindAddress;
    }

    private String getVersion(File jbossWebDir) throws IOException {
        boolean pre42 = jbossWebDir.getName().startsWith(EMBEDDED_TOMCAT_PRE42_DIR);
        String jarFileName = (pre42) ? "catalina.jar" : "jbossweb.jar";
        File jarFile = new File(jbossWebDir, jarFileName);
        ClassLoader classLoader = new URLClassLoader(new URL[] { jarFile.toURL() });
        InputStream stream = classLoader.getResourceAsStream(SERVER_INFO_PROPERTIES_RESOURCE);
        String version = null;
        if (stream != null) {
            Properties serverInfo = new Properties();
            serverInfo.load(stream);
            stream.close();
            version = serverInfo.getProperty("server.number");
            if (version == null) {
                String info = serverInfo.getProperty("server.info");
                if (info != null) {
                    version = info.substring(info.indexOf('/') + 1);
                }
            }
        }

        if (version == null) {
            version = "?";
            log.error("Failed to determine version of Embedded Tomcat server located at '" + jbossWebDir + "'");
        }

        return version;
    }
}