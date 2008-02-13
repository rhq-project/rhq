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
package org.rhq.enterprise.communications.command.impl.stream;

import org.rhq.enterprise.communications.command.AbstractCommandResponse;
import org.rhq.enterprise.communications.command.Command;
import org.rhq.enterprise.communications.command.CommandResponse;

/**
 * Encapsulates the results of a remote stream invocation.
 *
 * @author John Mazzitelli
 */
public class RemoteInputStreamCommandResponse extends AbstractCommandResponse {
    /**
     * the Serializable UID
     */
    private static final long serialVersionUID = 1L;

    /**
     * Constructor for {@link RemoteInputStreamCommandResponse} that defines a successfully invoked remote stream
     * method. It is expected that when a read() method call has succeeded, this constructor is used with the actual
     * command that was executed. This is because the command contains the byte array parameter that was used to store
     * the read bytes. In order to simulate pass-by-reference, the command must be stored in this response so that byte
     * array can be sent back with the response.
     *
     * @param cmd     the command that was executed (may be <code>null</code>)
     * @param results the results of the InputStream method call
     */
    public RemoteInputStreamCommandResponse(Command cmd, Object results) {
        super(cmd, true, results, null);
    }

    /**
     * Constructor for {@link RemoteInputStreamCommandResponse} that defines a failed remote stream invocation.
     *
     * @param cmd       the command that was executed (may be <code>null</code>)
     * @param exception the exception that describes the failure, if available (may be <code>null</code>)
     */
    public RemoteInputStreamCommandResponse(Command cmd, Throwable exception) {
        super(cmd, false, null, exception);
    }

    /**
     * Constructor for {@link RemoteInputStreamCommandResponse}.
     *
     * @see AbstractCommandResponse#AbstractCommandResponse(CommandResponse)
     */
    public RemoteInputStreamCommandResponse(CommandResponse responseToTransform) {
        super(responseToTransform);
    }

    /**
     * Returns the actual bytes that were read off of the stream. This method only returns non-<code>null</code> if the
     * command was successful and it executed one of the InputStream read methods that resulted in the read bytes stored
     * in the byte[] parameter passed to that method.
     *
     * @return the bytes read from the stream or <code>null</code> if the command was not a "read" method invocation
     */
    public byte[] getBytesReadFromStream() {
        // this will throw an exception only if a remote input stream command was not passed to the constructor
        // this should never happen. if this ever happens, we want a runtime exception thrown so the developer can fix his bug
        RemoteInputStreamCommand cmd = (RemoteInputStreamCommand) getCommand();
        Object[] params = cmd.getNameBasedInvocation().getParameters();

        // we know we are only invoking the InputStream API and therefore we know we only need to look
        // for the byte[] parameter in the first position for its two read() methods.  We need to do this
        // to mimic parameter pass by reference (when in reality we are passing by value over the wire).
        byte[] read_bytes = null;

        if ((params != null) && (params[0] instanceof byte[])) {
            read_bytes = (byte[]) params[0];
        }

        return read_bytes;
    }
}