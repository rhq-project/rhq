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

package org.rhq.enterprise.communications.command.client;

import org.rhq.enterprise.communications.command.Command;
import org.rhq.enterprise.communications.command.CommandResponse;

/**
 * Callback that is informed when communication failures are detected.
 * Callbacks may also request that the failed message be retried.
 * 
 * @author John Mazzitelli
 */
public interface FailureCallback {

    /**
     * The callback method that is called when a failure is detected by a remote communicator.
     * Implementations can indicate if the request should be retried by returning <code>true</code>.
     * 
     * @param remoteCommunicator the communicator object that detected the failure
     * @param command the command that was attempted to be sent but failed
     * @param response the response of the command, if one was even received (this is normally <code>null</code>,
     *                 but under some circumstances this might actually be a non-<code>null</code> object) 
     * @param t the actual exception that describes the failure - if this is <code>null</code>, the exception
     *          should be found in the exception field of the response
     * 
     * @return <code>true</code> if the callback would like the communicator to retry the request; <code>false</code>
     *         if the failure should be considered unrecoverable and the communicator should continue with its
     *         error handling
     */
    boolean failureDetected(RemoteCommunicator remoteCommunicator, Command command, CommandResponse response,
        Throwable t);
}
