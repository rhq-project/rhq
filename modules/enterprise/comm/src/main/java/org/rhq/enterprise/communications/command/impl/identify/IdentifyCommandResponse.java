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
package org.rhq.enterprise.communications.command.impl.identify;

import org.rhq.enterprise.communications.command.AbstractCommandResponse;
import org.rhq.enterprise.communications.command.Command;
import org.rhq.enterprise.communications.command.CommandResponse;

/**
 * Encapsulates the results of an {@link IdentifyCommand identify} command.
 *
 * @author John Mazzitelli
 */
public class IdentifyCommandResponse extends AbstractCommandResponse {
    /**
     * the Serializable UID
     */
    private static final long serialVersionUID = 1L;

    /**
     * Constructor for {@link IdentifyCommandResponse} that defines a successfully executed identify command.
     *
     * @param cmd   the {@link IdentifyCommand identify} command that was executed (may be <code>null</code>)
     * @param ident the identification information
     */
    public IdentifyCommandResponse(Command cmd, Identification ident) {
        super(cmd, true, ident, null);
    }

    /**
     * Constructor for {@link IdentifyCommandResponse} that defines a failed identify command.
     *
     * @param cmd       the {@link IdentifyCommand identify} command that was executed (may be <code>null</code>)
     * @param exception the exception that caused the failure, if available (may be <code>null</code>)
     */
    public IdentifyCommandResponse(Command cmd, Throwable exception) {
        super(cmd, false, null, exception);
    }

    /**
     * Constructor for {@link IdentifyCommandResponse}.
     *
     * @see AbstractCommandResponse#AbstractCommandResponse(CommandResponse)
     */
    public IdentifyCommandResponse(CommandResponse responseToTransform) {
        super(responseToTransform);
    }

    /**
     * Returns the remote server's identification information.
     *
     * @return identification of the remote server
     */
    public Identification getIdentification() {
        return (Identification) getResults();
    }
}