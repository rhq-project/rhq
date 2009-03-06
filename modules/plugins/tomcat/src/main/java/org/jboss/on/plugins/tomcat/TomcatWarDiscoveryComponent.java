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
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
                String filename = (docBaseFile.isAbsolute()) ? docBase : (deployDirectoryPath + File.separator + docBase);
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
                pluginConfiguration.put(new PropertySimple(TomcatWarComponent.PROPERTY_RESPONSE_TIME_LOG_FILE, getResponseTimeLogFile(parentComponent
                    .getInstallationPath(), host, contextRoot)));
                resource.setResourceName(resource.getResourceName().replace("{contextRoot}", (("/".equals(contextRoot)) ? docBase : contextRoot)));

                result.add(resource);
            } else {
                log.warn("Skipping discovered web application with unexpected name: " + name);
            }
        }

        // Find all deployed but unstarted applications IS THIS NECESSARY?
        // Set<DiscoveredResourceDetails> fileSystemResources = discoverFileSystem(context);

        // Merge. The addAll operation will only add items that are not already present, so resources discovered
        // by JMX will be used instead of those found by the file system scan.
        //jmxResources.addAll(fileSystemResources);

        return result;
    }

    /**
     * THIS IS NOT FULLY COOKED.  IT MAY NOT BE NECESSARY FOR TOMCAT AND WILL NEED TO BE FIXED UP IF USED.
     * 
     * Discovers applications that are deployed but did not start and are thus not discoverable through JMX.
     *
     * @param  context discovery context, will be used to determine what type of application (EAR, WAR) is being found
     *
     * @return set of all applications discovered on the file system; this should include at least some of the
     *         applications discovered through JMX as well
     */
    private Set<DiscoveredResourceDetails> discoverFileSystem(ResourceDiscoveryContext<TomcatVHostComponent> context) {
        Configuration defaultConfiguration = context.getDefaultPluginConfiguration();

        // Find the location of the deploy directory
        TomcatVHostComponent parentComponent = context.getParentResourceComponent();
        ApplicationServerComponent applicationServerComponent = (ApplicationServerComponent) parentComponent;

        String deployDirectoryPath = applicationServerComponent.getConfigurationPath().getPath();
        File deployDirectory = new File(deployDirectoryPath);

        // Set up filter for application type
        String extension = defaultConfiguration.getSimple("extension").getStringValue();
        FilenameFilter filter = new ApplicationFileFilter(extension);
        File[] files = deployDirectory.listFiles(filter);

        // For each file found, create a resource details instance for it
        ResourceType resourceType = context.getResourceType();

        Set<DiscoveredResourceDetails> resources = new HashSet<DiscoveredResourceDetails>(files.length);
        for (File file : files) {
            String resourceKey = determineResourceKey(defaultConfiguration, file.getName());
            String objectName = resourceKey;
            String resourceName = file.getName();
            String description = defaultConfiguration.getSimple(MBeanResourceDiscoveryComponent.PROPERTY_DESCRIPTION_TEMPLATE).getStringValue();

            DiscoveredResourceDetails resource = new DiscoveredResourceDetails(resourceType, resourceKey, resourceName, "", description, null, null);
            Configuration resourcePluginConfiguration = resource.getPluginConfiguration();
            resourcePluginConfiguration.put(new PropertySimple(TomcatWarComponent.PROPERTY_NAME, resourceName));
            resourcePluginConfiguration.put(new PropertySimple(MBeanResourceDiscoveryComponent.PROPERTY_OBJECT_NAME, objectName));
            resourcePluginConfiguration.put(new PropertySimple(TomcatWarComponent.PROPERTY_FILENAME, file.getAbsolutePath()));

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
     * @param  fileName             file name of the application found (e.g. rhq.ear)
     *
     * @return resource key to use for the indicated application file
     */
    private String determineResourceKey(Configuration defaultConfiguration, String fileName) {
        String template = defaultConfiguration.getSimple(MBeanResourceDiscoveryComponent.PROPERTY_OBJECT_NAME).getStringValue();
        String resourceKey = template.replaceAll("%name%", fileName);
        return resourceKey;
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
        String rtLogFileName = (isLocalhost(vHost) ? "" : vHost) + (isRoot(contextRoot) ? "/ROOT" : contextRoot) + RT_LOG_FILE_NAME_SUFFIX;
        rtLogFileName = rtLogFileName.substring(1);
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
    private static class ApplicationFileFilter implements FilenameFilter {
        // Attributes  --------------------------------------------

        private String applicationExtension;

        // Constructors  --------------------------------------------

        private ApplicationFileFilter(String applicationExtension) {
            this.applicationExtension = applicationExtension;
        }

        // FilenameFilter Implementation  --------------------------------------------

        public boolean accept(File dir, String name) {
            // TODO: If on Windows, this check should be case-insensitive.
            boolean result = name.endsWith(applicationExtension);
            return result;
        }
    }
}