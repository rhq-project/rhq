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
package org.rhq.enterprise.communications;

import java.util.Map;

import org.rhq.enterprise.communications.command.server.CommandProcessor;
import org.rhq.enterprise.communications.command.server.CommandProcessorMetrics.Calltime;

/**
 * MBean implementation that emits metrics from the server-side comm components.
 */
public class ServiceContainerMetrics implements ServiceContainerMetricsMBean {
    private CommandProcessor commandProcessor;
    private ServiceContainer serviceContainer; // useful for future, but not used yet

    /**
     * Creates a new {@link ServiceContainerMetrics} object.
     *
     * @param service_container the service container that houses the command processor
     * @param command_processor the actual command process object that receives commands emits the metrics
     */
    public ServiceContainerMetrics(ServiceContainer service_container, CommandProcessor command_processor) {
        serviceContainer = service_container;
        commandProcessor = command_processor;
    }

    public void clear() {
        commandProcessor.getCommandProcessorMetrics().clear();
    }

    public long getNumberSuccessfulCommandsReceived() {
        return commandProcessor.getCommandProcessorMetrics().getNumberSuccessfulCommands();
    }

    public long getNumberFailedCommandsReceived() {
        return commandProcessor.getCommandProcessorMetrics().getNumberFailedCommands();
    }

    public long getNumberDroppedCommandsReceived() {
        return commandProcessor.getCommandProcessorMetrics().getNumberDroppedCommands();
    }

    public long getNumberNotProcessedCommandsReceived() {
        return commandProcessor.getCommandProcessorMetrics().getNumberNotProcessedCommands();
    }

    public long getNumberTotalCommandsReceived() {
        // not truely atomic, but we aren't looking to provide exact, up-to-the-nanosecond, accuracy
        return getNumberSuccessfulCommandsReceived() + getNumberFailedCommandsReceived()
            + getNumberDroppedCommandsReceived() + getNumberNotProcessedCommandsReceived();
    }

    public long getAverageExecutionTimeReceived() {
        return commandProcessor.getCommandProcessorMetrics().getAverageExecutionTime();
    }

    public Map<String, Calltime> getCallTimeDataReceived() {
        Map<String, Calltime> callTimeData = commandProcessor.getCommandProcessorMetrics().getCallTimeData();
        return callTimeData;
    }
}