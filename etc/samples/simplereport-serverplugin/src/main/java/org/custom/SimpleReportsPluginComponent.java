/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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

package org.custom;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.measurement.ResourceAvailability;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.plugin.pc.ScheduledJobInvocationContext;
import org.rhq.enterprise.server.plugin.pc.ServerPluginComponent;
import org.rhq.enterprise.server.plugin.pc.ServerPluginContext;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * A sample lifecycle listener for the sample generic plugin. This listener will be
 * the main plugin component the server uses to start and stop the plugin.
 */
public class SimpleReportsPluginComponent implements ServerPluginComponent {
    private final Log log = LogFactory.getLog(SimpleReportsPluginComponent.class);

    private static final String PROP_REPORT_DIRECTORY = "reportDirectory";
    private static final String PROP_REPORT_HEADER = "reportHeader";

    private static final String JOBPROP_SHOW_PLATFORMS = "showPlatforms";
    private static final String JOBPROP_SHOW_SERVERS = "showServers";
    private static final String JOBPROP_SHOW_SERVICES = "showServices";

    private static final String JOBPROP_RESOURCE_IDS = "resourceIds";

    private ServerPluginContext context;
    private File reportDir = null;
    private String reportHeader = null;

    public void initialize(ServerPluginContext context) throws Exception {
        PropertySimple reportDirProp = context.getPluginConfiguration().getSimple(PROP_REPORT_DIRECTORY);
        if (reportDirProp == null || reportDirProp.getStringValue() == null) {
            throw new Exception("Simple report server plugin is misconfigured - report directory property is not set");
        }

        this.reportDir = new File(reportDirProp.getStringValue());
        this.reportDir.mkdirs();
        if (!this.reportDir.isDirectory()) {
            throw new Exception("Report directory does not exist and could not be created: " + this.reportDir);
        }

        PropertySimple reportHeaderProp = context.getPluginConfiguration().getSimple(PROP_REPORT_HEADER);
        if (reportHeaderProp != null) {
            this.reportHeader = reportHeaderProp.getStringValue();
        } else {
            this.reportHeader = null;
        }

        this.context = context;

        log.info("The simple report server plugin has been initialized!!!");
    }

    public void start() {
        log.info("The simple report server plugin has started!!!");
    }

    public void stop() {
        log.info("The simple report server plugin has stopped!!!");
    }

    public void shutdown() {
        log.info("The simple report server plugin has been shut down!!!");
    }

    public void generateResourceAvailabilityReport(ScheduledJobInvocationContext invocation) throws Exception {
        log.info("The simple report server plugin scheduled job [generateResourceAvailabilityReport] has triggered!!!");

        Properties data = invocation.getJobDefinition().getCallbackData();
        if (data == null) {
            throw new Exception("No callback data - cannot generate resource availability report");
        }

        // determine what resources we need to ping
        String resourceIdsStr = data.getProperty(JOBPROP_RESOURCE_IDS, "");
        String[] resourceIdsStrArray = resourceIdsStr.split(",");
        List<Integer> resourceIds = new ArrayList<Integer>(resourceIdsStrArray.length);
        for (String resourceIdStr : resourceIdsStrArray) {
            resourceIds.add(new Integer(resourceIdStr)); // throws exception if not a valid number, which halts job (which is good)
        }

        // ping the resources to get their availabilities
        List<ResourceAvailability> avails = new ArrayList<ResourceAvailability>(resourceIds.size());
        for (Integer id : resourceIds) {
            avails.add(pingResource(id.intValue()));
        }

        // dump the availability data to the report file
        File file = new File(this.reportDir, "availabilities.txt");
        PrintStream ps = new PrintStream(file);
        try {
            printHeader(ps);
            ps.println("  Report: AVAILABILITY");
            ps.println("    Date: " + new Date());
            for (ResourceAvailability avail : avails) {
                ps.printf("Resource: %6d | %4s | %s\n", //
                    avail.getResource().getId(), //
                    (avail.getAvailabilityType() == null) ? "???" : avail.getAvailabilityType().toString(), //
                    avail.getResource().getName());
            }
        } finally {
            ps.close();
        }
        return;
    }

    public void generateSimpleReports(ScheduledJobInvocationContext invocation) throws Exception {
        log.info("The simple report server plugin scheduled job [generateSimpleReport] has triggered!!!");

        try {
            Properties data = invocation.getJobDefinition().getCallbackData();
            boolean showPlatforms = Boolean.parseBoolean(data.getProperty(JOBPROP_SHOW_PLATFORMS, "true"));
            boolean showServers = Boolean.parseBoolean(data.getProperty(JOBPROP_SHOW_SERVERS, "true"));
            boolean showServices = Boolean.parseBoolean(data.getProperty(JOBPROP_SHOW_SERVICES, "true"));
            if (showPlatforms) {
                generatePlatformsReport();
            }
            if (showServers) {
                generateServersReport();
            }
            if (showServices) {
                generateServicesReport();
            }
        } catch (Exception e) {
            log.error("Failed to generate simple report", e);
        }
        return;
    }

    private void generatePlatformsReport() throws Exception {
        Subject user = getOverlord();
        ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();

        ResourceCategory category = ResourceCategory.PLATFORM;
        int committed = resourceManager.getResourceCountByCategory(user, category, InventoryStatus.COMMITTED);
        int ignored = resourceManager.getResourceCountByCategory(user, category, InventoryStatus.IGNORED);
        int deleted = resourceManager.getResourceCountByCategory(user, category, InventoryStatus.DELETED);
        int uninventoried = resourceManager.getResourceCountByCategory(user, category, InventoryStatus.UNINVENTORIED);

        File file = new File(this.reportDir, "platforms.txt");
        PrintStream ps = new PrintStream(file);
        try {
            printHeader(ps);
            ps.println("       Report: PLATFORM");
            ps.println("         Date: " + new Date());
            ps.println("    Committed: " + committed);
            ps.println("      Ignored: " + ignored);
            ps.println("      Deleted: " + deleted);
            ps.println("Uninventoried: " + uninventoried);
        } finally {
            ps.close();
        }
        return;
    }

    private void generateServersReport() throws Exception {
        Subject user = getOverlord();
        ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();

        ResourceCategory category = ResourceCategory.SERVER;
        int committed = resourceManager.getResourceCountByCategory(user, category, InventoryStatus.COMMITTED);
        int ignored = resourceManager.getResourceCountByCategory(user, category, InventoryStatus.IGNORED);
        int deleted = resourceManager.getResourceCountByCategory(user, category, InventoryStatus.DELETED);
        int uninventoried = resourceManager.getResourceCountByCategory(user, category, InventoryStatus.UNINVENTORIED);

        File file = new File(this.reportDir, "servers.txt");
        PrintStream ps = new PrintStream(file);
        try {
            printHeader(ps);
            ps.println("       Report: SERVER");
            ps.println("         Date: " + new Date());
            ps.println("    Committed: " + committed);
            ps.println("      Ignored: " + ignored);
            ps.println("      Deleted: " + deleted);
            ps.println("Uninventoried: " + uninventoried);
        } finally {
            ps.close();
        }
        return;
    }

    private void generateServicesReport() throws Exception {
        Subject user = getOverlord();
        ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();

        ResourceCategory category = ResourceCategory.SERVICE;
        int committed = resourceManager.getResourceCountByCategory(user, category, InventoryStatus.COMMITTED);
        int ignored = resourceManager.getResourceCountByCategory(user, category, InventoryStatus.IGNORED);
        int deleted = resourceManager.getResourceCountByCategory(user, category, InventoryStatus.DELETED);
        int uninventoried = resourceManager.getResourceCountByCategory(user, category, InventoryStatus.UNINVENTORIED);

        File file = new File(this.reportDir, "services.txt");
        PrintStream ps = new PrintStream(file);
        try {
            printHeader(ps);
            ps.println("       Report: SERVICE");
            ps.println("         Date: " + new Date());
            ps.println("    Committed: " + committed);
            ps.println("      Ignored: " + ignored);
            ps.println("      Deleted: " + deleted);
            ps.println("Uninventoried: " + uninventoried);
        } finally {
            ps.close();
        }
        return;
    }

    private void printHeader(PrintStream ps) {
        if (this.reportHeader != null) {
            ps.println(this.reportHeader);
        }
    }

    /**
     * Returns the availability for the given resource (e.g. UP or DOWN).
     * 
     * @param resourceId the ID of the resource whose availability is to be checked
     * @return the resource availability
     */
    private ResourceAvailability pingResource(int resourceId) {
        Subject user = getOverlord();
        ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();
        ResourceAvailability avail = resourceManager.getLiveResourceAvailability(user, resourceId);
        return avail;
    }

    private Subject getOverlord() {
        Subject user = LookupUtil.getSubjectManager().getOverlord();
        return user;
    }

    // currently not used, but this shows how you'd look up remote EJB SLSBs
    private static <T> T lookupRemote(Class<? super T> type) {
        try {
            return (T) new InitialContext().lookup((RHQConstants.EAR_NAME + "/" + type.getSimpleName() + "/remote"));
        } catch (NamingException e) {
            throw new RuntimeException("Failed to lookup remote interface to EJB: " + type, e);
        }
    }
}
