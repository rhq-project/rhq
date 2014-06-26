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

import javax.management.ObjectName;

import org.rhq.core.util.ObjectNameFactory;
import org.rhq.enterprise.communications.command.server.CommandProcessorMetrics;
import org.rhq.enterprise.communications.command.server.CommandProcessorMetrics.Calltime;

/**
 * This is the interface to the MBean that emits metric information on the server-side comm components.
 *
 * @author John Mazzitelli
 */
public interface ServiceContainerMetricsMBean {
    /**
     * The name of this metric collector MBean as it will be registered under by the service container
     */
    ObjectName OBJECTNAME_METRICS = ObjectNameFactory.create(ServiceContainer.JMX_DOMAIN + ":type=CommServerMetrics");

    /**
     * Clears the metrics data, starting all values back to 0 as if starting fresh.
     */
    void clear();

    /**
     * Returns the total number of commands that were received but failed to be processed succesfully.
     * This does not count {@link #getNumberDroppedCommandsReceived() dropped} or
     * {@link #getNumberNotProcessedCommandsReceived() unprocessed} commands.
     *
     * @return count of failed commands
     *
     * @see CommandProcessorMetrics#getNumberFailedCommands()
     */
    long getNumberFailedCommandsReceived();

    /**
     * Returns the total number of commands that were received but were dropped, usually due to a limit reached in the
     * server that prohibits more commands to be invoked until current invocations finish.
     *
     * @return count of dropped commands
     *
     * @see CommandProcessorMetrics#getNumberDroppedCommands()
     */
    long getNumberDroppedCommandsReceived();

    /**
     * Returns the total number of commands that were received but were not processed, usually due to global suspension of
     * command processing.
     *
     * @return count of dropped commands
     *
     * @see CommandProcessorMetrics#getNumberNotProcessedCommands()
     */
    long getNumberNotProcessedCommandsReceived();

    /**
     * Returns the total number of commands that were received and processed successfully.
     *
     * @return count of commands successfully processed
     *
     * @see CommandProcessorMetrics#getNumberSuccessfulCommands()
     */
    long getNumberSuccessfulCommandsReceived();

    /**
     * Returns the sum of all commands received, successful or not.
     *
     * @return total number of commands received
     */
    long getNumberTotalCommandsReceived();

    /**
     * Returns the average execution time (in milliseconds) it took to execute all
     * {@link #getNumberSuccessfulCommandsReceived() successful commands received}.
     *
     * @return average execute time for all successful commands.
     *
     * @see CommandProcessorMetrics#getAverageExecutionTime()
     */
    long getAverageExecutionTimeReceived();

    /**
     * Returns a map of individual command types/pojo invocations and their metrics such
     * as number of times invoked, min/max/avg execution times.
     *
     * @return calltime data
     *
     * @see CommandProcessorMetrics#getCallTimeDataReceived()
     */
    public Map<String, Calltime> getCallTimeDataReceived();
}