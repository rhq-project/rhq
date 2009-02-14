/*
 * RHQ Management Platform
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
package org.rhq.core.pc.inventory;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.clientapi.agent.PluginContainerException;
import org.rhq.core.domain.discovery.InventoryReport;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.ProcessScan;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.state.discovery.AutoDiscoveryRequest;
import org.rhq.core.domain.state.discovery.AutoDiscoveryScanType;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.PluginContainerConfiguration;
import org.rhq.core.pc.plugin.PluginComponentFactory;
import org.rhq.core.pc.plugin.PluginManager;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ProcessScanResult;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.system.ProcessInfo;
import org.rhq.core.system.SystemInfo;
import org.rhq.core.system.SystemInfoFactory;
import org.rhq.core.system.pquery.ProcessInfoQuery;
import org.rhq.core.util.exception.ExceptionPackage;
import org.rhq.core.util.exception.Severity;

/**
 * Standard platform/server inventory detection execution. This is typically called in a non-blocking fashion and the
 * report is returned asynchronously to the server. It is available for direct execution via a Future when running in an
 * embedded mode.
 *
 * @author Greg Hinkle
 * @author John Mazzitelli
 */
public class AutoDiscoveryExecutor implements Runnable, Callable<InventoryReport> {
    private Log log = LogFactory.getLog(AutoDiscoveryExecutor.class);

    private AutoDiscoveryRequest autoDiscoveryRequest;

    private InventoryManager inventoryManager;

    private PluginContainerConfiguration configuration;

    public AutoDiscoveryExecutor(AutoDiscoveryRequest autoDiscoveryRequest, InventoryManager inventoryManager,
        PluginContainerConfiguration configuration) {
        this.autoDiscoveryRequest = autoDiscoveryRequest;
        this.inventoryManager = inventoryManager;
        this.configuration = configuration;
    }

    public AutoDiscoveryExecutor() {
        autoDiscoveryRequest = new AutoDiscoveryRequest();
        autoDiscoveryRequest.getScanTypes().add(AutoDiscoveryScanType.Plugin);
    }

    public void run() {
        call();
    }

    public InventoryReport call() {
        log.info("Executing server discovery scan...");
        InventoryReport report = new InventoryReport(inventoryManager.getAgent());

        try {
            report.setStartTime(System.currentTimeMillis());
            if ((autoDiscoveryRequest == null)
                || autoDiscoveryRequest.getScanTypes().contains(AutoDiscoveryScanType.Plugin)) {
                pluginDiscovery(report);
            }
            report.setEndTime(System.currentTimeMillis());
            log.debug(String.format("Server discovery scan took %d ms.", (report.getEndTime() - report.getStartTime())));

            // TODO GH: This is principally valuable only until we work out the last of the data transfer situations
            if (log.isTraceEnabled()) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream(10000);
                ObjectOutputStream oos = new ObjectOutputStream(baos);
                oos.writeObject(report);
                log.trace("Server Discovery report for " + report.getResourceCount() + " resources with a size of "
                    + baos.size() + " bytes");
            }

            inventoryManager.handleReport(report);
        } catch (Exception e) {
            log.warn("Exception caught while running server discovery", e);
            report.addError(new ExceptionPackage(Severity.Warning, e));
        }

        log.info("Found " + report.getAddedRoots().size() + " servers.");
        return report;
    }

    /**
     * Goes through server plugins running auto discovery
     *
     * @param report the inventory report to which to add the discovered servers
     */
    @SuppressWarnings("unchecked")
    private void pluginDiscovery(InventoryReport report) {
        inventoryManager.executePlatformScan();

        PluginManager pluginManager = PluginContainer.getInstance().getPluginManager();
        PluginComponentFactory factory = PluginContainer.getInstance().getPluginComponentFactory();

        Set<ResourceType> serverTypes = pluginManager.getMetadataManager().getTypesForCategory(ResourceCategory.SERVER);
        ResourceComponent platformComponent = inventoryManager.getResourceComponent(inventoryManager.getPlatform());

        for (ResourceType serverType : serverTypes) {
            if (!serverType.getParentResourceTypes().isEmpty()) {
                continue; // TODO GH: Need to stop discovering embedded tomcats here and other non-top level servers
            }

            try {
                ResourceDiscoveryComponent component = factory.getDiscoveryComponent(serverType);
                // TODO GH: Manage plugin component call

                /* TODO GH: Fixme
                 * if (!verifyComponentCompatibility(component,platformComponent)) { log.warn("Resource has parent
                 * resource with incompatible component " + serverType); continue; }
                 */

                ResourceContainer platformContainer = inventoryManager.getResourceContainer(inventoryManager
                    .getPlatform());

                if (platformContainer.getSynchronizationState() == ResourceContainer.SynchronizationState.NEW) {
                    report.addAddedRoot(platformContainer.getResource());
                }

                // perform auto-discovery PIQL queries now to see if we can auto-detect resources that are running now
                List<ProcessScanResult> scanResults = new ArrayList<ProcessScanResult>();
                SystemInfo systemInfo = SystemInfoFactory.createSystemInfo();

                try {
                    Set<ProcessScan> processScans = serverType.getProcessScans();
                    if (processScans != null && !processScans.isEmpty()) {
                        ProcessInfoQuery piq = new ProcessInfoQuery(systemInfo.getAllProcesses());
                        for (ProcessScan processScan : processScans) {
                            List<ProcessInfo> queryResults = piq.query(processScan.getQuery());
                            if ((queryResults != null) && (queryResults.size() > 0)) {
                                for (ProcessInfo autoDiscoveredProcess : queryResults) {
                                    scanResults.add(new ProcessScanResult(processScan, autoDiscoveredProcess));
                                    log.info("Process scan auto-detected new server resource: scan=[" + processScan
                                        + "], discovered-process=[" + autoDiscoveredProcess + "]");
                                }
                            }
                        }
                    }
                } catch (UnsupportedOperationException uoe) {
                    log.debug("Cannot perform process scan - not supported on this platform. (" + systemInfo.getClass()
                        + ")");
                }

                ResourceDiscoveryContext context = new ResourceDiscoveryContext(serverType, platformComponent,
                       platformContainer.getResourceContext(), systemInfo, scanResults, Collections.EMPTY_LIST, configuration.getContainerName());

                Set<DiscoveredResourceDetails> discoveredResources = null;

                try {
                    discoveredResources = component.discoverResources(context);
                } catch (Throwable e) {
                    log.warn("Plugin discovery failed - skipping", e);
                }

                if (discoveredResources != null) {
                    Resource platformResource = platformContainer.getResource();

                    for (DiscoveredResourceDetails discoveredResource : discoveredResources) {
                        Resource newResource = InventoryManager.createNewResource(discoveredResource);
                        newResource.setResourceType(serverType);
                        log.debug("Detected server resource " + newResource);
                        Resource inventoriedResource = inventoryManager.mergeResourceFromDiscovery(newResource,
                            platformResource);

                        if (inventoriedResource.getInventoryStatus() == InventoryStatus.NEW) {
                            // The resource is new to the Server inventory.
                            if (platformContainer.getSynchronizationState() == ResourceContainer.SynchronizationState.SYNCHRONIZED) {
                                // The Platform is already in Server inventory, so this'll be a report root. Otherwise, it'll get included in the report under the Platform.
                                report.addAddedRoot(inventoriedResource);
                            }
                        }
                    }
                }
            } catch (Throwable e) {
                report.getErrors().add(new ExceptionPackage(Severity.Severe, e));
                log.error("Error in discovery", e);
            }
        }
    }

    private boolean verifyComponentCompatibility(ResourceDiscoveryComponent component,
        ResourceComponent parentResourceComponent) throws PluginContainerException {
        Method discoveryCall = null;

        try {
            discoveryCall = component.getClass().getMethod("discoverResources", ResourceCategory.class);
        } catch (NoSuchMethodException e) {
            throw new PluginContainerException("Resource component doesn't implement resource component interface", e);
        }

        Class<?> componentParameterType = discoveryCall.getParameterTypes()[0];

        TypeVariable<? extends Class<?>>[] types = componentParameterType.getTypeParameters();

        if (types.length == 0) { // The component doesn't declare type and therefore doesn't care what its parent type is
            return true;
        }

        TypeVariable<? extends Class<?>> type = types[0];

        // TODO GH: Figure this out
        // if (type.getBounds())
        // can we use: parentResourceComponent.getClass().isAssignableFrom( type.getGenericDeclaration().getClass() )
        return true;
    }
}