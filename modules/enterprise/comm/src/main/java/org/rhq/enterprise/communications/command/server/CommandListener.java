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
package org.rhq.enterprise.communications.command.server;

import org.rhq.enterprise.communications.command.Command;
import org.rhq.enterprise.communications.command.CommandResponse;

/**
 * A listener that will receive notifications when new commands are received and when they have completed their
 * processing.
 *
 * @author John Mazzitelli
 */
public interface CommandListener {
    /**
     * The listener callback method that is called when a new command has been received by a remote client.
     *
     * <p><i>Implementors of this method must ensure that it does not block and finishes very quickly; otherwise,
     * command throughput will suffer.</i></p>
     *
     * @param command the new command that was received
     */
    void receivedCommand(Command command);

    /**
     * The listener callback method that is called when a new command has been processed and is about to be returned to
     * the remote client.
     *
     * <p><i>Implementors of this method must ensure that it does not block and finishes very quickly; otherwise,
     * command throughput will suffer.</i></p>
     *
     * @param command  the command that was received and processed
     * @param response the results of the command - this is being sent back to the client
     */
    void processedCommand(Command command, CommandResponse response);
}