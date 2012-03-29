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

import org.jetbrains.annotations.NotNull;
import org.rhq.core.clientapi.agent.PluginContainerException;
import org.rhq.core.clientapi.server.discovery.AutoDiscoveryRequest;
import org.rhq.core.clientapi.server.discovery.InventoryReport;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.ProcessScan;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.state.discovery.AutoDiscoveryScanType;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.PluginContainerConfiguration;
import org.rhq.core.pc.plugin.PluginComponentFactory;
import org.rhq.core.pc.plugin.PluginManager;
import org.rhq.core.pluginapi.inventory.ProcessScanResult;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.system.ProcessInfo;
import org.rhq.core.system.SystemInfo;
import org.rhq.core.system.SystemInfoFactory;
import org.rhq.core.system.pquery.ProcessInfoQuery;
import org.rhq.core.util.exception.ExceptionPackage;
import org.rhq.core.util.exception.Severity;

/**
 * Standard platform/server inventory detection execution. This is typically called in a non-blocking fashion, and the
 * report is returned asynchronously to the server. It is available for direct execution via a Future when running in an
 * embedded mode.
 *
 * @author Greg Hinkle
 * @author John Mazzitelli
 * @author Ian Springer
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

    public void run() {
        call();
    }

    @NotNull
    public InventoryReport call() {
        log.info("Executing server discovery scan...");
        InventoryReport report = new InventoryReport(inventoryManager.getAgent());

        try {
            report.setStartTime(System.currentTimeMillis());
            if ((autoDiscoveryRequest == null)
                || autoDiscoveryRequest.getScanTypes().contains(AutoDiscoveryScanType.Plugin)) {
                List<ProcessInfo> processInfos = getProcessInfos();
                pluginDiscovery(report, processInfos);
            }
            report.setEndTime(System.currentTimeMillis());

            if (report.getAddedRoots().size() == 1 &&
                report.getAddedRoots().iterator().next().getResourceType().getCategory() == ResourceCategory.PLATFORM) {
                Resource platform = report.getAddedRoots().iterator().next();
                log.info("Discovered new platform with " + platform.getChildResources().size() + " child server(s).");
            } else {
                log.info("Discovered " + report.getAddedRoots().size() + " new server(s).");
            }
            
            if (log.isDebugEnabled()) {
                log.debug("Server discovery scan took [" + (report.getEndTime() - report.getStartTime()) + "] ms.");
            }

            // TODO GH: This is principally valuable only until we work out the last of the data transfer situations
            if (log.isTraceEnabled()) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream(10000);
                ObjectOutputStream oos = new ObjectOutputStream(baos);
                oos.writeObject(report);
                log.trace("Server Discovery report for [" + report.getResourceCount() + "] resources with a size of ["
                    + baos.size() + "] bytes");
            }

            inventoryManager.handleReport(report);
        } catch (Exception e) {
            log.warn("Exception caught while executing server discovery scan.", e);
            report.addError(new ExceptionPackage(Severity.Warning, e));
        }

        return report;
    }

    private List<ProcessInfo> getProcessInfos() {
        SystemInfo systemInfo = SystemInfoFactory.createSystemInfo();
        log.debug("Retrieving process table...");
        long startTime = System.currentTimeMillis();
        List<ProcessInfo> processInfos = null;
        try {
            processInfos = systemInfo.getAllProcesses();
        } catch (UnsupportedOperationException uoe) {
            log.debug("Cannot perform process scan - not supported on this platform. (" + systemInfo.getClass() + ")");
        }
        long elapsedTime = System.currentTimeMillis() - startTime;
        log.debug("Retrieval of process table took " + elapsedTime + " ms.");
        return processInfos;
    }

    /**
     * Goes through server plugins running auto discovery
     *
     * @param report the inventory report to which to add the discovered servers
     * @param processInfos
     */
    @SuppressWarnings("unchecked")
    private void pluginDiscovery(InventoryReport report, List<ProcessInfo> processInfos) {
        inventoryManager.executePlatformScan();

        PluginManager pluginManager = PluginContainer.getInstance().getPluginManager();
        PluginComponentFactory factory = PluginContainer.getInstance().getPluginComponentFactory();

        Set<ResourceType> serverTypes = pluginManager.getMetadataManager().getTypesForCategory(ResourceCategory.SERVER);
        ResourceContainer platformContainer = inventoryManager.getResourceContainer(inventoryManager.getPlatform());
        Resource platformResource = platformContainer.getResource();

        for (ResourceType serverType : serverTypes) {
            if (!serverType.getParentResourceTypes().isEmpty()) {
                continue; // TODO GH: Need to stop discovering embedded tomcats here and other non-top level servers
            }

            try {
                ResourceDiscoveryComponent component = factory.getDiscoveryComponent(serverType, platformContainer);
                // TODO GH: Manage plugin component call

                /* TODO GH: Fixme
                 * if (!verifyComponentCompatibility(component,platformComponent)) { log.warn("Resource has parent
                 * resource with incompatible component " + serverType); continue; }
                 */

                if (platformContainer.getSynchronizationState() == ResourceContainer.SynchronizationState.NEW) {
                    report.addAddedRoot(platformResource);
                }

                // Perform auto-discovery PIQL queries now to see if we can auto-detect servers that are currently running.
                List<ProcessScanResult> scanResults = performProcessScans(processInfos, serverType);

                Set<Resource> discoveredServers = this.inventoryManager.executeComponentDiscovery(serverType,
                    component, platformContainer, scanResults);

                for (Resource discoveredServer : discoveredServers) {
                    Resource inventoriedResource = this.inventoryManager.mergeResourceFromDiscovery(discoveredServer,
                        platformResource);

                    if (inventoriedResource.getInventoryStatus() == InventoryStatus.NEW) {
                        // The resource is new to the Server inventory.
                        if (platformContainer.getSynchronizationState() == ResourceContainer.SynchronizationState.SYNCHRONIZED) {
                            // The Platform is already in Server inventory, so this'll be a report root. Otherwise,
                            // it'll get included in the report under the Platform.
                            report.addAddedRoot(inventoriedResource);
                        }
                    }
                }
            } catch (Throwable e) {
                report.getErrors().add(new ExceptionPackage(Severity.Severe, e));
                log.error("Error in auto discovery", e);
            }
        }

        // if we have nothing, our plugins didn't discovery anything, but we want to at least report the platform
        if (report.getAddedRoots().isEmpty()) {
            if (platformContainer.getSynchronizationState() == ResourceContainer.SynchronizationState.NEW) {
                report.addAddedRoot(platformResource);
            }
        }

        return;
    }

    private List<ProcessScanResult> performProcessScans(List<ProcessInfo> processInfos, ResourceType serverType) {
        if (processInfos == null || processInfos.isEmpty())
            return Collections.emptyList();
        List<ProcessScanResult> scanResults = new ArrayList<ProcessScanResult>();
        Set<ProcessScan> processScans = serverType.getProcessScans();
        if (processScans != null && !processScans.isEmpty()) {
            log.debug("Executing process scans for server type " + serverType + "...");
            ProcessInfoQuery piq = new ProcessInfoQuery(processInfos);
            for (ProcessScan processScan : processScans) {
                List<ProcessInfo> queryResults = piq.query(processScan.getQuery());
                if ((queryResults != null) && (queryResults.size() > 0)) {
                    for (ProcessInfo autoDiscoveredProcess : queryResults) {
                        scanResults.add(new ProcessScanResult(processScan, autoDiscoveredProcess));
                        if (log.isDebugEnabled()) {
                            log.debug("Process scan auto-detected potential new server Resource: scan=[" + processScan
                                + "], discovered-process=[" + autoDiscoveredProcess + "]");
                        }
                    }
                }
            }
        }
        return scanResults;
    }

    private boolean verifyComponentCompatibility(ResourceDiscoveryComponent component,
        ResourceComponent parentResourceComponent) throws PluginContainerException {
        Method discoveryCall;
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