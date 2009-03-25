/** Jopr Management Platform
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
package org.jboss.on.plugins.tomcat;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.on.plugins.tomcat.helper.CreateResourceHelper;
import org.mc4j.ems.connection.EmsConnection;
import org.mc4j.ems.connection.bean.EmsBean;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.ApplicationServerComponent;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.plugins.jmx.MBeanResourceDiscoveryComponent;

/**
 * Discovery component used to discover web applications.
 *
 * @author Jay Shaughnessy
 * @author Jason Dobies
 */
public class TomcatWarDiscoveryComponent extends MBeanResourceDiscoveryComponent<TomcatVHostComponent> {

    public static final String PLUGIN_CONFIG_NAME = "name";

    private static final List<String> EMS_ATTRIBUTE_DOC_BASE = Arrays.asList(new String[] { "docBase" });
    private static final List<String> EMS_ATTRIBUTE_PATH = Arrays.asList(new String[] { "path" });
    /** The name MBean attribute for each application is of the form "Tomcat WAR (//vHost/contextRoot)". */
    private static final Pattern PATTERN_NAME = Pattern.compile("//(.*)(/.*)");
    private static final String RT_LOG_FILE_NAME_SUFFIX = "_rt.log";

    private final Log log = LogFactory.getLog(this.getClass());

    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<TomcatVHostComponent> context) {
        // Parent will discover deployed applications through JMX
        Set<DiscoveredResourceDetails> resources = super.discoverResources(context);
        Set<DiscoveredResourceDetails> result = new HashSet<DiscoveredResourceDetails>();

        TomcatVHostComponent parentComponent = context.getParentResourceComponent();
        ApplicationServerComponent applicationServerComponent = (ApplicationServerComponent) parentComponent;
        String deployDirectoryPath = applicationServerComponent.getConfigurationPath().getPath();
        String parentHost = parentComponent.getName();
        Matcher m = PATTERN_NAME.matcher("");

        for (DiscoveredResourceDetails resource : resources) {
            resource.setResourceKey(CreateResourceHelper.getCanonicalName(resource.getResourceKey()));
            Configuration pluginConfiguration = resource.getPluginConfiguration();
            String name = pluginConfiguration.getSimpleValue(PLUGIN_CONFIG_NAME, "");
            m.reset(name);
            if (m.matches()) {
                String host = m.group(1);
                // skip entries that are not for this vHost
                if (!host.equalsIgnoreCase(parentHost)) {
                    continue;
                }

                // get some info from the MBean (it seems awkward I have to query for the bean I'm basically dealing with)
                EmsConnection connection = context.getParentResourceComponent().getEmsConnection();
                EmsBean warBean = connection.getBean(resource.getResourceKey());
                // this refresh is important in case EMS is caching a stale version of this object. It can happen if
                // a user deletes and then recreates the same object.
                String contextRoot = (String) warBean.refreshAttributes(EMS_ATTRIBUTE_PATH).get(0).getValue();
                String docBase = (String) warBean.refreshAttributes(EMS_ATTRIBUTE_DOC_BASE).get(0).getValue();
                File docBaseFile = new File(docBase);
                String filename = (docBaseFile.isAbsolute()) ? docBase
                    : (deployDirectoryPath + File.separator + docBase);
                try {
                    filename = new File(filename).getCanonicalPath();
                } catch (IOException e) {
                    // leave path as is
                    log.warn("Unexpected discovered web application path: " + filename);
                }
                if ("".equals(contextRoot)) {
                    contextRoot = "/";
                }
                pluginConfiguration.put(new PropertySimple(TomcatWarComponent.PROPERTY_VHOST, host));
                pluginConfiguration.put(new PropertySimple(TomcatWarComponent.PROPERTY_CONTEXT_ROOT, contextRoot));
                pluginConfiguration.put(new PropertySimple(TomcatWarComponent.PROPERTY_FILENAME, filename));
                pluginConfiguration.put(new PropertySimple(TomcatWarComponent.PROPERTY_RESPONSE_TIME_LOG_FILE,
                    getResponseTimeLogFile(parentComponent.getInstallationPath(), host, contextRoot)));
                resource.setResourceName(resource.getResourceName().replace("{contextRoot}",
                    (("/".equals(contextRoot)) ? docBase : contextRoot)));

                result.add(resource);
            } else {
                log.warn("Skipping discovered web application with unexpected name: " + name);
            }
        }

        // Find apps in the deploy directory that have not been deployed. This can happen if the vhost is
        // not autodeploying
        Set<DiscoveredResourceDetails> undeployedWarResources = discoverUndeployed(context, result);

        // Merge. The addAll operation will only add items that are not already present, so resources discovered
        // by JMX will be used instead of those found by the file system scan.
        result.addAll(undeployedWarResources);

        return result;
    }

    /**
     * Discovers applications that are present in the docbase directory but are not deployed and
     * are thus not discoverable through JMX since they have no mbean.
     *
     * @param  context discovery context
     * @param  deployed the set of already deployed (discovered via JMX) apps,  used for filtering 
     *
     * @return set of all applications discovered on the file system; this should include at least some of the
     *         applications discovered through JMX as well
     */
    private Set<DiscoveredResourceDetails> discoverUndeployed(ResourceDiscoveryContext<TomcatVHostComponent> context,
        Set<DiscoveredResourceDetails> deployed) {
        Configuration defaultConfiguration = context.getDefaultPluginConfiguration();

        // Find the location of the deploy directory
        TomcatVHostComponent vhost = context.getParentResourceComponent();
        File deployDirectory = vhost.getConfigurationPath();

        // Set up filter for application type
        FileFilter filter = new WebAppFileFilter();
        File[] files = deployDirectory.listFiles(filter);

        // can be null if we have a remote install dir specified for a remote server
        if ((null == files) || (0 == files.length)) {
            return new HashSet<DiscoveredResourceDetails>(0);
        }

        // For each file found, create a resource details instance for it
        ResourceType resourceType = context.getResourceType();
        String vhostName = vhost.getName();

        Set<DiscoveredResourceDetails> resources = new HashSet<DiscoveredResourceDetails>(files.length);
        for (File file : files) {
            // skip over anything in this directory that is not an actual web app
            if (!vhost.isWebApplication(file)) {
                continue;
            }

            String resourceName = defaultConfiguration
                .getSimple(MBeanResourceDiscoveryComponent.PROPERTY_NAME_TEMPLATE).getStringValue();
            String description = defaultConfiguration.getSimple(
                MBeanResourceDiscoveryComponent.PROPERTY_DESCRIPTION_TEMPLATE).getStringValue();
            String fileName = file.getName();
            String contextRoot = ((file.isDirectory() ? fileName : fileName.substring(0, fileName.length() - 4)));
            contextRoot = "ROOT".equals(contextRoot) ? "/" : "/" + contextRoot;
            resourceName = resourceName.replace("{contextRoot}", contextRoot);
            String name = "//" + vhostName + contextRoot;
            String resourceKey = determineResourceKey(defaultConfiguration, name);
            String objectName = resourceKey;

            DiscoveredResourceDetails resource = new DiscoveredResourceDetails(resourceType, resourceKey, resourceName,
                "", description, null, null);

            // If this app has been deployed skip it
            if (deployed.contains(resource)) {
                continue;
            }

            Configuration pluginConfiguration = resource.getPluginConfiguration();

            pluginConfiguration.put(new PropertySimple(TomcatWarComponent.PROPERTY_NAME, name));
            pluginConfiguration
                .put(new PropertySimple(MBeanResourceDiscoveryComponent.PROPERTY_OBJECT_NAME, objectName));
            pluginConfiguration.put(new PropertySimple(TomcatWarComponent.PROPERTY_VHOST, vhost.getName()));
            pluginConfiguration.put(new PropertySimple(TomcatWarComponent.PROPERTY_CONTEXT_ROOT, contextRoot));
            try {
                pluginConfiguration.put(new PropertySimple(TomcatWarComponent.PROPERTY_FILENAME, file
                    .getCanonicalPath()));
            } catch (IOException e) {
                pluginConfiguration
                    .put(new PropertySimple(TomcatWarComponent.PROPERTY_FILENAME, file.getAbsolutePath()));
            }
            pluginConfiguration.put(new PropertySimple(TomcatWarComponent.PROPERTY_RESPONSE_TIME_LOG_FILE,
                getResponseTimeLogFile(vhost.getInstallationPath(), vhost.getName(), contextRoot)));

            resources.add(resource);
        }

        return resources;
    }

    /**
     * Creates the appropriate resource key. The resource key is generated by substituting the file name into the plugin
     * descriptor provided JMX object name template. This will only be run for applications that are not currently
     * deployed and accessible through JMX. This method mimics the resource key created by the super class handling of
     * JMX discovered applications.
     *
     * @param  defaultConfiguration default plugin configuration for the application's resource type
     * @param  contextRoot contextRoot of the web app
     *
     * @return resource key to use for the indicated application file
     */
    private String determineResourceKey(Configuration defaultConfiguration, String name) {
        String template = defaultConfiguration.getSimple(MBeanResourceDiscoveryComponent.PROPERTY_OBJECT_NAME)
            .getStringValue();
        String resourceKey = template.replaceAll("%name%", name);
        return CreateResourceHelper.getCanonicalName(resourceKey);
    }

    /**
     * The filename generated here must be the same as the filenames being generated by the Response Time
     * filter {see RHQ RtFilter} configured with the Tomcat Server.
     * @param installPath
     * @param vHost
     * @param contextRoot
     * @return
     */
    private String getResponseTimeLogFile(File installPath, String vHost, String contextRoot) {
        File logsDir = new File(installPath, "logs/rt");
        String rtLogFileName = null;
        if (isLocalhost(vHost)) {
            rtLogFileName = (isRoot(contextRoot) ? "ROOT" : contextRoot.substring(1)) + RT_LOG_FILE_NAME_SUFFIX;
        } else {
            rtLogFileName = vHost + (isRoot(contextRoot) ? "/ROOT" : contextRoot) + RT_LOG_FILE_NAME_SUFFIX;
        }
        rtLogFileName = rtLogFileName.replace('/', '_');
        File rtLogFile = new File(logsDir, rtLogFileName);
        String result;

        try {
            result = rtLogFile.getCanonicalPath();
        } catch (IOException e) {
            result = rtLogFile.getPath();
        }

        return result;
    }

    private boolean isLocalhost(String vHost) {
        return ("localhost".equals(vHost) || "127.0.0.1".equals(vHost));
    }

    private boolean isRoot(String contextRoot) {
        return ("/".equals(contextRoot));
    }

    // Inner Classes  --------------------------------------------

    /**
     * Filter used to find applications.
     */
    private static class WebAppFileFilter implements FileFilter {
        // Attributes  --------------------------------------------

        public WebAppFileFilter() {
        }

        public boolean accept(File pathname) {
            return pathname.isDirectory() || pathname.getPath().toLowerCase().endsWith(".war");
        }
    }
}