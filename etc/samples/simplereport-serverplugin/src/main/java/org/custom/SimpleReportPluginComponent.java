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
import java.util.Date;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.enterprise.server.plugin.pc.ScheduledJobInvocationContext;
import org.rhq.enterprise.server.plugin.pc.ServerPluginComponent;
import org.rhq.enterprise.server.plugin.pc.ServerPluginContext;

/**
 * A sample lifecycle listener for the sample generic plugin. This listener will be
 * the main plugin component the server uses to start and stop the plugin.
 */
public class SimpleReportPluginComponent implements ServerPluginComponent {
    private final Log log = LogFactory.getLog(SimpleReportPluginComponent.class);

    private static final String PROP_REPORT_DIRECTORY = "reportDirectory";
    private static final String PROP_REPORT_HEADER = "reportHeader";

    private static final String JOBPROP_SHOW_PLATFORMS = "showPlatforms";
    private static final String JOBPROP_SHOW_SERVERS = "showServers";
    private static final String JOBPROP_SHOW_SERVICES = "showServices";

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

    public void generateSimpleReport(ScheduledJobInvocationContext invocation) throws Exception {
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
        File file = new File(this.reportDir, "platforms.txt");
        PrintStream ps = new PrintStream(file);
        try {
            ps.println("=== PLATFORM REPORT ===");
            ps.println("Date Generated: " + new Date());
            printHeader(ps);

            // TODO: collect and print data
            ps.println("...data here...");
        } finally {
            ps.close();
        }
        return;
    }

    private void generateServersReport() throws Exception {
        File file = new File(this.reportDir, "servers.txt");
        PrintStream ps = new PrintStream(file);
        try {
            ps.println("=== SERVER REPORT ===");
            ps.println("Date Generated: " + new Date());
            printHeader(ps);

            // TODO: collect and print data
            ps.println("...data here...");
        } finally {
            ps.close();
        }
        return;
    }

    private void generateServicesReport() throws Exception {
        File file = new File(this.reportDir, "services.txt");
        PrintStream ps = new PrintStream(file);
        try {
            ps.println("=== SERVICE REPORT ===");
            ps.println("Date Generated: " + new Date());
            printHeader(ps);

            // TODO: collect and print data
            ps.println("...data here...");
        } finally {
            ps.close();
        }
        return;
    }

    private void printHeader(PrintStream ps) {
        if (this.reportHeader != null) {
            ps.println();
            ps.println(this.reportHeader);
            ps.println();
        } else {
            ps.println();
        }
    }
}
