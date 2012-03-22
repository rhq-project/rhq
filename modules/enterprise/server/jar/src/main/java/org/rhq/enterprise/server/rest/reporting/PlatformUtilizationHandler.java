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

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.interceptor.Interceptors;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;

import org.rhq.core.domain.resource.composite.PlatformMetricsSummary;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.resource.PlatformUtilizationManagerLocal;
import org.rhq.enterprise.server.rest.AbstractRestBean;
import org.rhq.enterprise.server.rest.SetCallerInterceptor;

/**
 * @author jsanda
 */
@Interceptors(SetCallerInterceptor.class)
@Stateless
public class PlatformUtilizationHandler extends AbstractRestBean implements PlatformUtilizationLocal {

    @EJB
    private PlatformUtilizationManagerLocal platformUtilizationMgr;

    @Override
    public StreamingOutput generateReport(UriInfo uriInfo, Request request, HttpHeaders headers) {
        return new StreamingOutput() {
            @Override
            public void write(OutputStream output) throws IOException, WebApplicationException {
                output.write((getHeader() + "\n").getBytes());
                PageList<PlatformMetricsSummary> summaries = platformUtilizationMgr.loadPlatformMetrics(caller);
                for (PlatformMetricsSummary summary : summaries) {
                    output.write((toCSV(summary) + "\n").getBytes());
                }
            }
        };
    }

    private String getHeader() {
        return "Name,Version,CPU,Memory,Swap";
    }

    private String toCSV(PlatformMetricsSummary summary) {
        if (summary.isMetricsAvailable()) {
            return summary.getResource().getName() + "," + summary.getResource().getVersion() + "," +
                calculateCPUUsage(summary) + "," + calculateMemoryUsage(summary) + "," + calculateSwapUsage(summary);

        }

        return summary.getResource().getName() + "," + summary.getResource().getVersion() + ",NA,NA,NA";
    }

    private double calculateCPUUsage(PlatformMetricsSummary summary) {
        Double systemCPU = (Double) summary.getSystemCPU().getValue();
        Double userCPU = (Double) summary.getUserCPU().getValue();
        return (systemCPU * userCPU) * 100;
    }

    private double calculateMemoryUsage(PlatformMetricsSummary summary) {
        Double totalMemory = (Double) summary.getTotalMemory().getValue();
        Double freeMemory = (Double) summary.getFreeMemory().getValue();
        Double usedMemory = totalMemory - freeMemory;
        return (usedMemory / totalMemory) * 100;
    }

    private double calculateSwapUsage(PlatformMetricsSummary summary) {
        Double totalSwap = (Double) summary.getTotalSwap().getValue();
        Double usedSwap = (Double) summary.getUsedSwap().getValue();
        return (usedSwap / totalSwap) * 100;
    }
}
