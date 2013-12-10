/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.modules.plugins.jbossas7;

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.measurement.calltime.CallTimeData;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.util.ResponseTimeConfiguration;
import org.rhq.core.pluginapi.util.ResponseTimeLogParser;
import org.rhq.modules.plugins.jbossas7.helper.ServerPluginConfiguration;

/**
 * The ResourceComponent for a "Web Runtime" Resource.
 *
 * @author Ian Springer
 */
public class WebRuntimeComponent extends BaseComponent<BaseComponent<?>> {

    private static final String RESPONSE_TIME_METRIC = "responseTime";

    private ResponseTimeLogParser responseTimeLogParser;

    @Override
    public void start(ResourceContext<BaseComponent<?>> resourceContext) throws InvalidPluginConfigurationException, Exception {
        super.start(resourceContext);

        Configuration pluginConfig = resourceContext.getPluginConfiguration();
        ResponseTimeConfiguration responseTimeConfig = new ResponseTimeConfiguration(pluginConfig);
        File logFile = responseTimeConfig.getLogFile();
        if (logFile == null) {
            logFile = findLogFile();
        }

        if (logFile != null) {
            if (getLog().isDebugEnabled()) {
                if (logFile.isFile()) {
                    getLog().debug("[" + resourceContext.getResourceKey() + "] is using the response time log file ["
                        + logFile + "]");
                } else {
                    getLog().debug("The response time log file [" + logFile + "] for ["
                        + resourceContext.getResourceKey() + "] does not exist yet.");
                }
            }

            this.responseTimeLogParser = new ResponseTimeLogParser(logFile);
            this.responseTimeLogParser.setExcludes(responseTimeConfig.getExcludes());
            this.responseTimeLogParser.setTransforms(responseTimeConfig.getTransforms());
        } else {
            if (getLog().isDebugEnabled()) {
                getLog().debug("Cannot monitor response time for [" + resourceContext.getResourceKey()
                    + "] - unknown log file location");
            }
        }
    }

    @Override
    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> origReqs) throws Exception {
        // make our own copy so, as we iterate, we can remove the item that we can process,
        // which will leave the rest for super.getValues() to process
        HashSet<MeasurementScheduleRequest> requests;
        requests = new HashSet<MeasurementScheduleRequest>(origReqs.size());
        requests.addAll(origReqs);

        // now process schedule requests
        for (Iterator<MeasurementScheduleRequest> iterator = requests.iterator(); iterator.hasNext(); ) {
            MeasurementScheduleRequest request = iterator.next();
            if (request.getName().equals(RESPONSE_TIME_METRIC)) {
                iterator.remove();
                if (this.responseTimeLogParser != null) {
                    try {
                        CallTimeData callTimeData = new CallTimeData(request);
                        this.responseTimeLogParser.parseLog(callTimeData);
                        report.addData(callTimeData);
                    } catch (Exception e) {
                        getLog().error("Failed to retrieve call-time metric '" + RESPONSE_TIME_METRIC + "' for "
                                + context.getResourceType() + " Resource with key [" + context.getResourceKey() + "].",
                                e);
                    }
                } else {
                    getLog().error("The '" + RESPONSE_TIME_METRIC + "' metric is enabled for " + context.getResourceType()
                            + " Resource with key [" + context.getResourceKey() + "], but no value is defined for the '"
                            + ResponseTimeConfiguration.RESPONSE_TIME_LOG_FILE_CONFIG_PROP + "' connection property.");
                    // TODO: Communicate this error back to the server for display in the GUI.
                }
                break;
            }
        }

        super.getValues(report, requests);
    }

    private File findLogFile() {
        File logFile = null;
        ServerPluginConfiguration serverPluginConfig = getServerComponent().getServerPluginConfiguration();
        File logDir = serverPluginConfig.getLogDir();
        if (logDir != null && logDir.isDirectory()) {
            try {
                String virtualHost = readAttribute("virtual-host");
                if (virtualHost != null) {
                    String contextRoot = readAttribute("context-root");
                    if (contextRoot != null) {
                        // RtFilter strips the initial '/' and replaces all other '/' with '_'.
                        // If the context is the top root context ("/"), then it uses "ROOT";
                        // see org.rhq.helpers.rtfilter.util.ServletUtility.getContextRootFromSpec25
                        if (contextRoot.startsWith("/")) {
                            if (contextRoot.equals("/")) {
                                contextRoot = "ROOT";
                            } else {
                                contextRoot = contextRoot.substring(1);
                            }
                        }
                        contextRoot = contextRoot.replace('/', '_'); // for context roots like foo/bar

                        // RtFilter doesn't prefix the filename with a vhost if there is none or its the default
                        if (virtualHost.equals("default-host")) {
                            virtualHost = "";
                        } else {
                            virtualHost = virtualHost + "_";
                        }

                        // RtFilter puts the log files in the rt subdirectory under the log directory.
                        // e.g. "rt/192.168.1.100_foo_rt.log" for foo.war deployed to 192.168.1.100 vhost
                        String logFileName = String.format("rt/%s%s_rt.log", virtualHost, contextRoot);
                        logFile = new File(logDir, logFileName);
                    } else {
                        if (getLog().isDebugEnabled()) {
                            getLog().debug("Unknown context root for: " + getAddress());
                        }
                    }
                } else {
                    if (getLog().isDebugEnabled()) {
                        getLog().debug("Unknown virtual host for: " + getAddress());
                    }
                }
            } catch (Exception e) {
                // ignore
            }
        }
        return logFile;
    }

}
