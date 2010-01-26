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
package org.rhq.core.pc;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.clientapi.agent.metadata.PluginDependencyGraph;
import org.rhq.core.clientapi.server.discovery.InventoryReport;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pc.inventory.InventoryManager;
import org.rhq.core.pc.inventory.ResourceContainer;
import org.rhq.core.pc.plugin.CanonicalResourceKey;
import org.rhq.core.pc.plugin.ClassLoaderManager;
import org.rhq.core.pluginapi.operation.OperationResult;

/**
 * The management interface implementation for the {@link PluginContainer} itself.
 * 
 * @author John Mazzitelli
 */
public class PluginContainerMBeanImpl implements PluginContainerMBeanImplMBean {
    private static final Log log = LogFactory.getLog(PluginContainerMBeanImpl.class);

    private final PluginContainer pluginContainer;

    public PluginContainerMBeanImpl(PluginContainer pc) {
        this.pluginContainer = pc;
    }

    public void register() {
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        try {
            server.registerMBean(this, new ObjectName(OBJECT_NAME));
        } catch (Exception e) {
            log.error("Unable to register PluginContainerMBean", e);
        }
    }

    public void unregister() {
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        try {
            server.unregisterMBean(new ObjectName(OBJECT_NAME));
        } catch (Exception e) {
            log.warn("Unable to unregister PluginContainerMBean", e);
        }
    }

    public String executeDiscovery(Boolean detailedDiscovery) {

        StringBuilder results = new StringBuilder();

        InventoryReport report = this.pluginContainer.getInventoryManager().executeServerScanImmediately();
        results.append(generateInventoryReportString(report));

        if (detailedDiscovery != null && detailedDiscovery.booleanValue()) {
            report = this.pluginContainer.getInventoryManager().executeServiceScanImmediately();
            results.append('\n');
            results.append(generateInventoryReportString(report));
        }

        return results.toString();
    }

    public OperationResult retrievePluginDependencyGraph() {

        Map<String, List<String>> dependencies = new HashMap<String, List<String>>();
        List<String> deploymentOrder;

        ClassLoader originalCL = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
            ClassLoaderManager clm = this.pluginContainer.getPluginManager().getClassLoaderManager();
            PluginDependencyGraph graph = clm.getPluginDependencyGraph();
            deploymentOrder = graph.getDeploymentOrder();
            for (String pluginName : deploymentOrder) {
                List<String> deps = graph.getPluginDependencies(pluginName);
                if (deps == null || deps.size() == 0) {
                    deps = new ArrayList<String>(Arrays.asList("<none>"));
                }
                dependencies.put(pluginName, deps);
            }
        } finally {
            Thread.currentThread().setContextClassLoader(originalCL);
        }

        OperationResult info = new OperationResult();
        PropertyList list = new PropertyList("plugins");
        info.getComplexResults().put(list);

        for (String plugin : deploymentOrder) {
            PropertyMap map = new PropertyMap("plugin");
            map.put(new PropertySimple("name", plugin));
            map.put(new PropertySimple("dependencies", dependencies.get(plugin)));
            list.add(map);
        }

        return info;
    }

    public OperationResult retrievePluginClassLoaderInformation() {

        Map<String, ClassLoader> classloaders;

        ClassLoader originalCL = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
            ClassLoaderManager clm = this.pluginContainer.getPluginManager().getClassLoaderManager();
            classloaders = clm.getPluginClassLoaders();
        } finally {
            Thread.currentThread().setContextClassLoader(originalCL);
        }

        OperationResult info = new OperationResult();
        PropertySimple numClassLoaders = new PropertySimple("numberOfClassLoaders", String.valueOf(classloaders.size()));
        PropertyList list = new PropertyList("classloaders");
        info.getComplexResults().put(numClassLoaders);
        info.getComplexResults().put(list);

        for (Map.Entry<String, ClassLoader> entry : classloaders.entrySet()) {
            String pluginName = entry.getKey();
            ClassLoader classloader = entry.getValue();
            PropertyMap map = new PropertyMap("classloader");
            map.put(new PropertySimple("pluginName", pluginName));
            map.put(new PropertySimple("classloaderInfo", classloader));
            list.add(map);
        }

        return info;
    }

    public OperationResult retrieveDiscoveryClassLoaderInformation() {

        Map<String, ClassLoader> classloaders;

        ClassLoader originalCL = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
            ClassLoaderManager clm = this.pluginContainer.getPluginManager().getClassLoaderManager();
            classloaders = clm.getDiscoveryClassLoaders();
        } finally {
            Thread.currentThread().setContextClassLoader(originalCL);
        }

        OperationResult info = new OperationResult();
        PropertySimple numClassLoaders = new PropertySimple("numberOfClassLoaders", String.valueOf(classloaders.size()));
        PropertyList list = new PropertyList("classloaders");
        info.getComplexResults().put(numClassLoaders);
        info.getComplexResults().put(list);

        for (Map.Entry<String, ClassLoader> entry : classloaders.entrySet()) {
            String id = entry.getKey();
            ClassLoader classloader = entry.getValue();
            PropertyMap map = new PropertyMap("classloader");
            map.put(new PropertySimple("id", id));
            map.put(new PropertySimple("classloaderInfo", classloader));
            list.add(map);
        }

        return info;
    }

    public OperationResult retrieveAllResourceClassLoaderInformation() {

        Map<CanonicalResourceKey, ClassLoader> classloaders;
        Map<CanonicalResourceKey, String[]> canonicalIdMap = new HashMap<CanonicalResourceKey, String[]>(); // [0]=name, [1]=id, [2]=uuid

        ClassLoader originalCL = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
            ClassLoaderManager clm = this.pluginContainer.getPluginManager().getClassLoaderManager();
            InventoryManager im = this.pluginContainer.getInventoryManager();
            classloaders = clm.getResourceClassLoaders();
            for (CanonicalResourceKey canonicalId : classloaders.keySet()) {
                ResourceContainer container = im.getResourceContainer(canonicalId);
                String[] nameId = new String[3];
                if (container != null) {
                    nameId[0] = container.getResource().getName();
                    nameId[1] = Integer.toString(container.getResource().getId());
                    nameId[2] = container.getResource().getUuid();
                } else {
                    nameId[0] = "<unknown>";
                    nameId[1] = "<unknown>";
                    nameId[2] = "<unknown>";
                }
                canonicalIdMap.put(canonicalId, nameId);
            }
        } finally {
            Thread.currentThread().setContextClassLoader(originalCL);
        }

        OperationResult info = new OperationResult();
        PropertySimple numClassLoaders = new PropertySimple("numberOfResources", String.valueOf(classloaders.size()));
        PropertyList list = new PropertyList("classloaders");
        info.getComplexResults().put(numClassLoaders);
        info.getComplexResults().put(list);

        for (Entry<CanonicalResourceKey, ClassLoader> entry : classloaders.entrySet()) {
            CanonicalResourceKey canonicalId = entry.getKey();
            ClassLoader classloader = entry.getValue();
            String[] data = canonicalIdMap.get(canonicalId);
            PropertyMap map = new PropertyMap("classloader");
            map.put(new PropertySimple("resourceName", data[0]));
            map.put(new PropertySimple("resourceId", data[1]));
            map.put(new PropertySimple("resourceUuid", data[2]));
            map.put(new PropertySimple("canonicalId", canonicalId.toString()));
            map.put(new PropertySimple("classloaderInfo", classloader));
            list.add(map);
        }

        classloaders.clear(); // don't need this shallow copy anymore, help the GC clean up

        return info;
    }

    public OperationResult retrieveUniqueResourceClassLoaderInformation() {

        // keyed on resource classloader with the value being the number of resources assigned the classloader
        Map<ClassLoader, AtomicInteger> classloaderCounts = new HashMap<ClassLoader, AtomicInteger>();

        ClassLoader originalCL = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
            ClassLoaderManager clm = this.pluginContainer.getPluginManager().getClassLoaderManager();
            Map<CanonicalResourceKey, ClassLoader> classloaders = clm.getResourceClassLoaders();
            for (Entry<CanonicalResourceKey, ClassLoader> entry : classloaders.entrySet()) {
                AtomicInteger count = classloaderCounts.get(entry.getValue());
                if (count == null) {
                    count = new AtomicInteger(1);
                    classloaderCounts.put(entry.getValue(), count);
                } else {
                    count.incrementAndGet();
                }
            }
            classloaders.clear(); // don't need this shallow copy anymore, help the GC clean up
        } finally {
            Thread.currentThread().setContextClassLoader(originalCL);
        }

        OperationResult info = new OperationResult();
        PropertySimple numClassLoaders = new PropertySimple("numberOfClassLoaders", String.valueOf(classloaderCounts
            .size()));
        PropertyList list = new PropertyList("classloaders");
        info.getComplexResults().put(numClassLoaders);
        info.getComplexResults().put(list);

        for (Map.Entry<ClassLoader, AtomicInteger> entry : classloaderCounts.entrySet()) {
            ClassLoader classloader = entry.getKey();
            AtomicInteger count = entry.getValue();
            PropertyMap map = new PropertyMap("classloader");
            map.put(new PropertySimple("classloaderInfo", classloader));
            map.put(new PropertySimple("resourceCount", count.get()));
            list.add(map);
        }

        return info;
    }

    public int getNumberOfPluginClassLoaders() {
        return this.pluginContainer.getPluginManager().getClassLoaderManager().getNumberOfPluginClassLoaders();
    }

    public int getNumberOfDiscoveryClassLoaders() {
        return this.pluginContainer.getPluginManager().getClassLoaderManager().getNumberOfDiscoveryClassLoaders();
    }

    public int getNumberOfResourceClassLoaders() {
        return this.pluginContainer.getPluginManager().getClassLoaderManager().getNumberOfResourceClassLoaders();
    }

    private String generateInventoryReportString(InventoryReport report) {
        StringBuilder reportStr = new StringBuilder();
        if (report != null) {
            reportStr.append((report.isRuntimeReport() ? "*Service Scan" : "*Server Scan"));
            reportStr.append(" Inventory Report*\n");
            reportStr.append("Start Time: ").append(new Date(report.getStartTime())).append('\n');
            reportStr.append("Finish Time: ").append(new Date(report.getEndTime())).append('\n');
            reportStr.append("Resource Count: ").append(report.getResourceCount()).append('\n');
            reportStr.append("Error Count: ").append(report.getErrors().size());
        } else {
            reportStr.append("No inventory report available!");
        }
        return reportStr.toString();
    }
}
