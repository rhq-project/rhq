/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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

package org.rhq.plugins.apache.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.resource.ProcessScan;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.PluginContainerDeployment;
import org.rhq.core.pluginapi.inventory.ProcessScanResult;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.system.ProcessInfo;
import org.rhq.core.system.SystemInfo;
import org.rhq.core.system.SystemInfoFactory;
import org.rhq.core.system.pquery.ProcessInfoQuery;
import org.rhq.plugins.apache.ApacheServerComponent;
import org.rhq.plugins.apache.ApacheServerDiscoveryComponent;
import org.rhq.plugins.apache.parser.ApacheDirectiveTree;
import org.rhq.plugins.platform.PlatformComponent;

/**
 * 
 *
 * @author Lukas Krejci
 */
public class ApacheExecutionUtil {

    private static final Log LOG = LogFactory.getLog(ApacheExecutionUtil.class);
    
    private ResourceType apacheServerResourceType;
    private ApacheServerComponent serverComponent;
    private ResourceContext<PlatformComponent> resourceContext;
    private String serverRootPath;
    private String exePath;
    private String httpdConfPath;
    private String snmpHost;
    private int snmpPort;
    private String pingUrl;

    public enum ExpectedApacheState {
        RUNNING, STOPPED
    };
    
    public ApacheExecutionUtil(ResourceType apacheServerResourceType, String serverRootPath, String exePath,
        String httpdConfPath, String pingUrl, String snmpHost, int snmpPort) {

        this.apacheServerResourceType = apacheServerResourceType;
        this.serverRootPath = serverRootPath;
        this.exePath = exePath;
        this.httpdConfPath = httpdConfPath;
        this.snmpHost = snmpHost;
        this.snmpPort = snmpPort;
        this.pingUrl = pingUrl;
    }

    public void init() throws Exception {
        ApacheServerDiscoveryComponent discoveryComponent = new ApacheServerDiscoveryComponent();

        SystemInfo systemInfo = SystemInfoFactory.createSystemInfo();

        ResourceDiscoveryContext<PlatformComponent> discoveryContext = new ResourceDiscoveryContext<PlatformComponent>(
            apacheServerResourceType, null, null, systemInfo, scanProcesses(systemInfo), null,
            PluginContainerDeployment.AGENT);

        Configuration config = discoveryContext.getDefaultPluginConfiguration();
        config.put(new PropertySimple(ApacheServerComponent.PLUGIN_CONFIG_PROP_SERVER_ROOT, serverRootPath));
        config.put(new PropertySimple(ApacheServerComponent.PLUGIN_CONFIG_PROP_EXECUTABLE_PATH, exePath));
        config.put(new PropertySimple(ApacheServerComponent.PLUGIN_CONFIG_PROP_HTTPD_CONF, httpdConfPath));
        config.put(new PropertySimple(ApacheServerComponent.PLUGIN_CONFIG_PROP_SNMP_AGENT_HOST, snmpHost));
        config.put(new PropertySimple(ApacheServerComponent.PLUGIN_CONFIG_PROP_SNMP_AGENT_PORT, snmpPort));
        config.put(new PropertySimple(ApacheServerComponent.PLUGIN_CONFIG_PROP_URL, pingUrl));

        DiscoveredResourceDetails result = discoveryComponent.discoverResource(config, discoveryContext);

        serverComponent = new ApacheServerComponent();

        Resource resource = new Resource(result.getResourceKey(), null, apacheServerResourceType);
        resource.setPluginConfiguration(config);
        resourceContext = new ResourceContext<PlatformComponent>(resource, null,
            discoveryComponent, systemInfo, null, null, null, null, null, null, null, null);

        serverComponent.start(resourceContext);
    }

    public void invokeOperation(ExpectedApacheState desiredState, String operation) throws Exception {
        int i = 0;
        while (i < 10) {
            serverComponent.invokeOperation(operation, new Configuration());
            
            //wait for max 30s for the operation to "express" itself                 
            int w = 0;
            ProcessInfo pi;
            while (w < 30) {
                pi = getResourceContext().getNativeProcess();
                
                switch (desiredState) {
                case RUNNING:
                    if (pi != null && pi.isRunning()) {
                        return;
                    }
                    break;
                case STOPPED:
                    if (pi == null || !pi.isRunning()) {
                        return;
                    }
                }
                
                Thread.sleep(1000);
                ++w;
            }
            
            ++i;
            
            LOG.warn("Could not detect the httpd process after invoking the start operation but the operation didn't throw any exception. I will retry at most ten times and then fail loudly. This has been attempt no. " + i);
        }
        
        throw new IllegalStateException("Failed to start the httpd process even after 10 retries without the apache component complaining. This is super strange.");
    }

    public ResourceContext<PlatformComponent> getResourceContext() {
        return resourceContext;
    }

    public ApacheDirectiveTree getRuntimeConfiguration() {
        ApacheDirectiveTree config = serverComponent.loadParser();
        return RuntimeApacheConfiguration.extract(config, resourceContext.getNativeProcess(), new ApacheBinaryInfo(
            exePath), serverComponent.getModuleNames(), true);
    }

    public ApacheServerComponent getServerComponent() {
        return serverComponent;
    }
    
    private List<ProcessScanResult> scanProcesses(SystemInfo systemInfo) {
        List<ProcessScanResult> scanResults = new ArrayList<ProcessScanResult>();
        Set<ProcessScan> processScans = apacheServerResourceType.getProcessScans();
        if (processScans != null && !processScans.isEmpty()) {
            ProcessInfoQuery piq = new ProcessInfoQuery(systemInfo.getAllProcesses());
            for (ProcessScan processScan : processScans) {
                List<ProcessInfo> queryResults = piq.query(processScan.getQuery());
                if ((queryResults != null) && (queryResults.size() > 0)) {
                    for (ProcessInfo autoDiscoveredProcess : queryResults) {
                        scanResults.add(new ProcessScanResult(processScan, autoDiscoveredProcess));
                    }
                }
            }
        }
        return scanResults;
    }
}
