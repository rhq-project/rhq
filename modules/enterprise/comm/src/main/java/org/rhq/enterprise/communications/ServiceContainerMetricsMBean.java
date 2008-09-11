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

import javax.management.ObjectName;

import org.jboss.mx.util.ObjectNameFactory;

import org.rhq.enterprise.communications.command.server.CommandProcessor;

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
     * Returns the total number of commands that were received but failed to be processed succesfully.
     *
     * @return count of failed commands
     *
     * @see    CommandProcessor#getNumberFailedCommands()
     */
    long getNumberFailedCommandsReceived();

    /**
     * Returns the total number of commands that were received but were dropped, usually due to a limit reached in the
     * server that prohibits more commands to be invoked until current invocations finish. This will always be equal to
     * or less than {@link #getNumberFailedCommandsReceived()} because a dropped command is also considered a failed
     * command.
     *
     * @return count of dropped commands
     *
     * @see    CommandProcessor#getNumberDroppedCommands()
     */
    long getNumberDroppedCommandsReceived();

    /**
     * Returns the total number of commands that were received but were not processed, usually due to global suspension of
     * command processing. This will always be equal to or less than {@link #getNumberFailedCommandsReceived()} because an
     * unprocessed command is also considered a failed command.
     *
     * @return count of dropped commands
     *
     * @see    CommandProcessor#getNumberNotProcessedCommands()
     */
    long getNumberNotProcessedCommandsReceived();

    /**
     * Returns the total number of commands that were received and processed successfully.
     *
     * @return count of commands successfully processed
     *
     * @see    CommandProcessor#getNumberSuccessfulCommands()
     */
    long getNumberSuccessfulCommandsReceived();

    /**
     * Returns the sum of {@link #getNumberSuccessfulCommandsReceived()} and {@link #getNumberFailedCommandsReceived()}.
     *
     * @return total number of commands received and processed
     */
    long getNumberTotalCommandsReceived();

    /**
     * Returns the average execution time (in milliseconds) it took to execute all
     * {@link #getNumberSuccessfulCommandsReceived() successful commands received}.
     *
     * @return average execute time for all successful commands.
     */
    long getAverageExecutionTimeReceived();
}