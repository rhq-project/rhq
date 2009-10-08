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

import java.io.Serializable;
import org.rhq.enterprise.communications.command.CommandResponse;

/**
 * This defines the callback object that will be called when an asynchronous command has been sent. It allows the client
 * that asked to send the command to be notified of the results of the command.
 *
 * <p>NOTE: This class is serializable since it is possible for commands to have to be persisted to disk as part of a
 * guaranteed delivery mechanism. This might not be something we should expect - we might have to remove the
 * serialization requirement on this class and just make the callback transient in the CommandAndCallback class.</p>
 *
 * @author John Mazzitelli
 */
public interface CommandResponseCallback extends Serializable {
    /**
     * The callback method that indicates the command was sent. The <code>response</code> is the results of the command
     * execution as returned by the server.
     *
     * @param response the results of the command
     */
    void commandSent(CommandResponse response);
}