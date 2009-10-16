/*
 * Jopr Management Platform
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
package org.rhq.plugins.server;

import java.util.Map;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.enterprise.communications.command.server.CommandProcessorMetrics.Calltime;
import org.rhq.plugins.jmx.MBeanResourceComponent;

/**
 * The resource component that represents the remote API subsystem - remote clients are things like
 * the CLI that ask to invoke remote API calls. This does not involve agent communications.
 * 
 * @author John Mazzitelli
 */
public class RemoteAPIResourceComponent extends MBeanResourceComponent {
    @Override
    public OperationResult invokeOperation(String name, Configuration parameters) throws Exception {
        if ("viewReceivedCallTimeData".equals(name)) {
            Map<String, long[]> allData;
            allData = (Map<String, long[]>) getEmsBean().getAttribute("CallTimeDataAsPrimitives").refresh();
            OperationResult result = new OperationResult();
            PropertyList commands = new PropertyList("requests");
            result.getComplexResults().put(commands);

            for (Map.Entry<String, long[]> data : allData.entrySet()) {
                String api = data.getKey();
                long[] calltime = data.getValue();
                
                final int totalCountIndex = 0;
                final int successCountIndex = 1;
                final int failureCountIndex = 2;
                final int minExeTimeCountIndex = 3;
                final int maxExeTimeCountIndex = 4;
                final int avgExeTimeCountIndex = 5;

                // If successes is 0, the Callback values will have Long.MIN/MAX_VALUE for maximum and minimum times.
                // But if successes is 0, we know we can't have valid values anyway, so just zero them out.
                long minimum = 0;
                long maximum = 0;
                long average = 0;
                if (calltime[successCountIndex] > 0) {
                    minimum = calltime[minExeTimeCountIndex];
                    maximum = calltime[maxExeTimeCountIndex];
                    average = calltime[avgExeTimeCountIndex];                    
                }

                PropertyMap command = new PropertyMap("request", //
                    new PropertySimple("api", api), //
                    new PropertySimple("totalCount", calltime[totalCountIndex]), //
                    new PropertySimple("successCount", calltime[successCountIndex]), //
                    new PropertySimple("failureCount", calltime[failureCountIndex]), //
                    new PropertySimple("executionMinTime", minimum), //
                    new PropertySimple("executionMaxTime", maximum), //
                    new PropertySimple("executionAvgTime", average));
                commands.add(command);
            }

            return result;
        }

        // isn't an operation we know about, must be an MBean operation that EMS can handle
        return super.invokeOperation(name, parameters);
    }
}
