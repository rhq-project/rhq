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

import java.util.Map;
import org.rhq.enterprise.communications.command.Command;
import org.rhq.enterprise.communications.command.client.AbstractCommandClient;
import org.rhq.enterprise.communications.command.client.CommandClient;
import org.rhq.enterprise.communications.command.client.RemoteCommunicator;

/**
 * Provides a client API for the {@link EchoCommand echo command}.
 *
 * @author John Mazzitelli
 */
public class EchoCommandClient extends AbstractCommandClient {
    /**
     * @see AbstractCommandClient#AbstractCommandClient()
     */
    public EchoCommandClient() {
        super();
    }

    /**
     * @see AbstractCommandClient#AbstractCommandClient(RemoteCommunicator)
     */
    public EchoCommandClient(RemoteCommunicator communicator) {
        super(communicator);
    }

    /**
     * @see CommandClient#createNewCommand(Map)
     */
    public Command createNewCommand(Map<String, Object> params) {
        return new EchoCommand(params);
    }
}