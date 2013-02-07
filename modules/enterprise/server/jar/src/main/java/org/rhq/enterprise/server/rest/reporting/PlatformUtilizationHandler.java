/*
 *
 *  * RHQ Management Platform
 *  * Copyright (C) 2005-2012 Red Hat, Inc.
 *  * All rights reserved.
 *  *
 *  * This program is free software; you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation version 2 of the License.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program; if not, write to the Free Software
 *  * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 */

package org.rhq.enterprise.server.rest.reporting;

import java.io.IOException;
import java.io.OutputStream;
import java.text.NumberFormat;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.interceptor.Interceptors;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.resource.composite.PlatformMetricsSummary;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.resource.PlatformUtilizationManagerLocal;
import org.rhq.enterprise.server.rest.AbstractRestBean;
import org.rhq.enterprise.server.rest.ReportsInterceptor;

/**
 * @author jsanda
 */
@Interceptors(ReportsInterceptor.class)
@Stateless
public class PlatformUtilizationHandler extends AbstractRestBean implements PlatformUtilizationLocal {

    private final Log log = LogFactory.getLog(PlatformUtilizationHandler.class);

    @EJB
    private PlatformUtilizationManagerLocal platformUtilizationMgr;

    public StreamingOutput generateReportInternal(HttpServletRequest request, Subject user) {
        this.caller = user;

        return generateReport(request);
    }
    @Override
    public StreamingOutput generateReport(HttpServletRequest request) {

        if (log.isDebugEnabled()) {
            log.debug("Received request to generate report for " + caller);
        }
        return new StreamingOutput() {
            private NumberFormat numberFormat;

            {
                numberFormat = NumberFormat.getPercentInstance();
                numberFormat.setMaximumFractionDigits(2);
            }

            @Override
            public void write(OutputStream output) throws IOException, WebApplicationException {
                CsvWriter<PlatformMetricsSummary> csvWriter = new CsvWriter<PlatformMetricsSummary>();
                csvWriter.setColumns("resource.name", "resource.version", "CPUUsage", "memoryUsage", "swapUsage");

                csvWriter.setPropertyConverter("CPUUsage", new PropertyConverter<PlatformMetricsSummary>() {
                    @Override
                    public Object convert(PlatformMetricsSummary summary, String propertyName) {
                        return calculateCPUUsage(summary);
                    }
                });

                csvWriter.setPropertyConverter("memoryUsage", new PropertyConverter<PlatformMetricsSummary>() {
                    @Override
                    public Object convert(PlatformMetricsSummary summary, String propertyName) {
                        return calculateMemoryUsage(summary);
                    }
                });

                csvWriter.setPropertyConverter("swapUsage", new PropertyConverter<PlatformMetricsSummary>() {
                    @Override
                    public Object convert(PlatformMetricsSummary summary, String propertyName) {
                        return calculateSwapUsage(summary);
                    }
                });

                output.write((getHeader() + "\n").getBytes());
                PageList<PlatformMetricsSummary> summaries = platformUtilizationMgr.loadPlatformMetrics(caller);
                for (PlatformMetricsSummary summary : summaries) {
                    csvWriter.write(summary, output);
                }
            }

            private String getHeader() {
                return "Name,Version,CPU,Memory,Swap";
            }

            private String calculateCPUUsage(PlatformMetricsSummary summary) {
                if (!summary.isMetricsAvailable()) {
                    return "NA";
                }
                Double systemCPU = (Double) summary.getSystemCPU().getValue();
                Double userCPU = (Double) summary.getUserCPU().getValue();
                return numberFormat.format((systemCPU + userCPU));
            }

            private String calculateMemoryUsage(PlatformMetricsSummary summary) {
                if (!summary.isMetricsAvailable()) {
                    return "NA";
                }
                Double totalMemory = (Double) summary.getTotalMemory().getValue();
                Double usedMemory = (Double) summary.getActualUsedMemory().getValue();
                return numberFormat.format((usedMemory / totalMemory));
            }

            private String calculateSwapUsage(PlatformMetricsSummary summary) {
                if (!summary.isMetricsAvailable()) {
                    return "NA";
                }
                Double totalSwap = (Double) summary.getTotalSwap().getValue();
                Double usedSwap = (Double) summary.getUsedSwap().getValue();
                return numberFormat.format((usedSwap / totalSwap));
            }
        };
    }

}
