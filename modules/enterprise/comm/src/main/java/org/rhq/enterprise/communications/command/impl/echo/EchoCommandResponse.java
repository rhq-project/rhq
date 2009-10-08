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
package org.rhq.enterprise.communications.command.impl.echo;

import org.rhq.enterprise.communications.command.AbstractCommandResponse;
import org.rhq.enterprise.communications.command.Command;
import org.rhq.enterprise.communications.command.CommandResponse;

/**
 * Encapsulates the results of an {@link EchoCommand echo}.
 *
 * @author John Mazzitelli
 */
public class EchoCommandResponse extends AbstractCommandResponse {
    /**
     * the Serializable UID
     */
    private static final long serialVersionUID = 1L;

    /**
     * Constructor for {@link EchoCommandResponse} that defines a successfully executed echo command.
     *
     * @param cmd         the {@link EchoCommand echo} command that was executed (may be <code>null</code>)
     * @param echoMessage the message being echoed back to the client
     */
    public EchoCommandResponse(Command cmd, String echoMessage) {
        super(cmd, true, echoMessage, null);
    }

    /**
     * Constructor for {@link EchoCommandResponse} that defines a failed echo command.
     *
     * @param cmd       the {@link EchoCommand echo} command that was executed (may be <code>null</code>)
     * @param exception the exception that caused the failure, if available (may be <code>null</code>)
     */
    public EchoCommandResponse(Command cmd, Throwable exception) {
        super(cmd, false, null, exception);
    }

    /**
     * Constructor for {@link EchoCommandResponse}.
     *
     * @see AbstractCommandResponse#AbstractCommandResponse(CommandResponse)
     */
    public EchoCommandResponse(CommandResponse responseToTransform) {
        super(responseToTransform);
    }

    /**
     * Provides a strong-typed method that clients can call to obtain the echo message. Clients are free to call
     * {@link AbstractCommandResponse#getResults()} and cast the return value to a <code>String</code>. This method
     * simply provides a way to help clients avoid that cast and to know exactly what type to expect.
     *
     * @return the echoed message with any prefix that was requested in the command
     */
    public String getEchoMessage() {
        return (String) getResults();
    }
}