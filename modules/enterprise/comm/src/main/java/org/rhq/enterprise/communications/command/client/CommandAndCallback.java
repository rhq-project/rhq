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
import org.rhq.enterprise.communications.command.Command;

/**
 * Just a simple class to encapsulate both a command and an optional callback object.
 *
 * @author John Mazzitelli
 */
public class CommandAndCallback implements Serializable {
    /**
     * The serial UID
     */
    private static final long serialVersionUID = 1L;

    private final Command m_command;
    private final CommandResponseCallback m_callback;

    /**
     * Creates a new {@link CommandAndCallback} object.
     *
     * @param command
     * @param callback
     */
    public CommandAndCallback(Command command, CommandResponseCallback callback) {
        m_command = command;
        m_callback = callback;
    }

    /**
     * Returns the callback.
     *
     * @return callback
     */
    public CommandResponseCallback getCallback() {
        return m_callback;
    }

    /**
     * Returns the command.
     *
     * @return command
     */
    public Command getCommand() {
        return m_command;
    }

    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return this.getClass().getName() + ": command=" + getCommand() + "; callback=" + getCallback();
    }
}