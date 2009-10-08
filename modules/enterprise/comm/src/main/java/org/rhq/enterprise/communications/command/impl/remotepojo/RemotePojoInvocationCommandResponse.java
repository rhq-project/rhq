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
package org.rhq.enterprise.communications.command.impl.remotepojo;

import org.rhq.enterprise.communications.command.AbstractCommandResponse;
import org.rhq.enterprise.communications.command.Command;
import org.rhq.enterprise.communications.command.CommandResponse;

/**
 * Encapsulates the results of a remote POJO invocation.
 *
 * @author John Mazzitelli
 */
public class RemotePojoInvocationCommandResponse extends AbstractCommandResponse {
    /**
     * the Serializable UID
     */
    private static final long serialVersionUID = 1L;

    /**
     * Constructor for {@link RemotePojoInvocationCommandResponse} that defines a successfully invoked remote POJO
     * method.
     *
     * @param cmd     the command that was executed (may be <code>null</code>)
     * @param results
     */
    public RemotePojoInvocationCommandResponse(Command cmd, Object results) {
        super(cmd, true, results, null);
    }

    /**
     * Constructor for {@link RemotePojoInvocationCommandResponse} that defines a failed remote POJO invocation.
     *
     * @param cmd       the command that was executed (may be <code>null</code>)
     * @param exception the exception that describes the failure, if available (may be <code>null</code>)
     */
    public RemotePojoInvocationCommandResponse(Command cmd, Throwable exception) {
        super(cmd, false, null, exception);
    }

    /**
     * Constructor for {@link RemotePojoInvocationCommandResponse}.
     *
     * @see AbstractCommandResponse#AbstractCommandResponse(CommandResponse)
     */
    public RemotePojoInvocationCommandResponse(CommandResponse responseToTransform) {
        super(responseToTransform);
    }
}