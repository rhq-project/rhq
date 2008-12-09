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
 * The resource component that represents the server communications subsystem.
 * 
 * @author John Mazzitelli
 */
public class CommunicationsResourceComponent extends MBeanResourceComponent {
    @Override
    public OperationResult invokeOperation(String name, Configuration parameters) throws Exception {
        if ("viewReceivedCallTimeData".equals(name)) {
            // we know our agent has the Calltime object in classpath - its in the comm module
            Map<String, Calltime> allData;
            allData = (Map<String, Calltime>) getEmsBean().getAttribute("CallTimeDataReceived").refresh();
            OperationResult result = new OperationResult();
            PropertyList commands = new PropertyList("commands");
            result.getComplexResults().put(commands);

            for (Map.Entry<String, Calltime> data : allData.entrySet()) {
                String commandName = data.getKey();
                Calltime calltime = data.getValue();
                PropertyMap command = new PropertyMap("command", //
                    new PropertySimple("command", commandName), //
                    new PropertySimple("totalCount", calltime.getCount()), //
                    new PropertySimple("successCount", calltime.getSuccesses()), //
                    new PropertySimple("failureCount", calltime.getFailures()), //
                    new PropertySimple("droppedCount", calltime.getDropped()), //
                    new PropertySimple("notProcessedCount", calltime.getNotProcessed()), //
                    new PropertySimple("executionMinTime", calltime.getMinimum()), //
                    new PropertySimple("executionMaxTime", calltime.getMaximum()), //
                    new PropertySimple("executionAvgTime", calltime.getAverage()));
                commands.add(command);
            }

            return result;
        }

        // isn't an operation we know about, must be an MBean operation that EMS can handle
        return super.invokeOperation(name, parameters);
    }
}
