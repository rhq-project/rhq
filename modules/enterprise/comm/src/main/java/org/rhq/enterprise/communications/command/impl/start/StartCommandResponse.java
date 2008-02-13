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
package org.rhq.enterprise.communications.command.impl.start;

import org.rhq.enterprise.communications.command.AbstractCommandResponse;
import org.rhq.enterprise.communications.command.Command;
import org.rhq.enterprise.communications.command.CommandResponse;

/**
 * The response of a start command that will indicate if the remote process instance was successfully started or not.
 *
 * <p>The results object (if command was successful) will be the exit code (as an Integer) of the process (if the
 * command has its {@link StartCommand#isWaitForExit()} set to <code>true</code>). The results object will be <code>
 * null</code> if that wait flag is <code>false</code>.</p>
 *
 * @author John Mazzitelli
 */
public class StartCommandResponse extends AbstractCommandResponse {
    /**
     * the UID to identify the serializable version of this class
     */
    private static final long serialVersionUID = 1L;

    /**
     * Creates a response, however, there is no associated exit code (due to the fact that the original command did not
     * request that the command service wait for the process to exit).
     *
     * @see AbstractCommandResponse#AbstractCommandResponse(Command)
     */
    public StartCommandResponse(Command command) {
        super(command);
    }

    /**
     * Creates a response that considers the command a success, with the <code>exitCode</code> passed as the result
     * object. The exit code is the process's exit code that it returned when it stopped.
     *
     * @param command  the command that succeeded
     * @param exitCode the exit code
     */
    public StartCommandResponse(Command command, Integer exitCode) {
        super(command, true, exitCode, null);
    }

    /**
     * Creates a response that considers the command a failure with the given exception stored in the response.
     *
     * @param command   the command that failed
     * @param exception that caused the failure
     */
    public StartCommandResponse(Command command, Throwable exception) {
        super(command, false, null, exception);
    }

    /**
     * Constructor for {@link StartCommandResponse} ;
     *
     * @see AbstractCommandResponse#AbstractCommandResponse(CommandResponse)
     */
    public StartCommandResponse(CommandResponse responseToTransform) {
        super(responseToTransform);
    }

    /**
     * Convienence method that provides a strongly-typed return value of the exit code. Note that if the command did not
     * tell the command service to wait for the process to exit, the returned value will be <code>null</code>. The
     * returned value will also be <code>null</code> if the process failed to start; in which case, get the
     * {@link AbstractCommandResponse#getException()} that caused the failure.
     *
     * @return the exit code of the process; <code>null</code> if the process failed to start or the command service
     *         didn't wait for it to exit
     */
    public Integer getExitCode() {
        return (Integer) getResults();
    }
}