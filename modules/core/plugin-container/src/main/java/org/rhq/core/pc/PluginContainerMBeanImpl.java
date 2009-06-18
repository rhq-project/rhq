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
import java.util.Date;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.discovery.InventoryReport;

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
