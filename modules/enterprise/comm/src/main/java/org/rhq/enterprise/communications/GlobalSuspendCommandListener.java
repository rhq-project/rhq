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
package org.rhq.enterprise.communications;

import org.rhq.enterprise.communications.command.Command;
import org.rhq.enterprise.communications.command.CommandResponse;
import org.rhq.enterprise.communications.command.client.CommandPreprocessor;
import org.rhq.enterprise.communications.command.server.CommandListener;
import org.rhq.enterprise.communications.util.NotProcessedException;

/**
 * This is a listener for commands coming into the {@link ServiceContainer}'s {@link CommandPreprocessor} and will
 * immediately deny processing of the command, returning a {@link NotProcessedException}.  This is used if for some reason
 * all command processing should stop temporarily.  Adding the listener will stop all commands and deleting the listener will
 * resume command processing.
 *
 * @author Jay Shaughnessy
 */
public class GlobalSuspendCommandListener implements CommandListener {

    private final String name;
    private final String reason;

    /**
     * @param reason The reason command processing is being suspended. If provided will be supplied to {@link NotProcessedException}
     */
    public GlobalSuspendCommandListener(String name, String reason) {
        this.name = name;
        this.reason = reason;
    }

    /**
     * This will be called for every command coming in. We'll simply deny service to the command by throwing a
     * {@link NotProcessedException}.
     *
     * @see CommandListener#receivedCommand(Command)
     */
    public void receivedCommand(Command command) {
        throw new NotProcessedException(reason);
    }

    /**
     * This is called for every command that finished. It does nothing since we always throw a {@link NotProcessedException}.
     *
     * @see CommandListener#processedCommand(Command, CommandResponse)
     */
    public void processedCommand(Command command, CommandResponse response) {
        // nothing to do
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof GlobalSuspendCommandListener))
            return false;

        GlobalSuspendCommandListener listener = (GlobalSuspendCommandListener) obj;

        return (this.name.equals(listener.name));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

}