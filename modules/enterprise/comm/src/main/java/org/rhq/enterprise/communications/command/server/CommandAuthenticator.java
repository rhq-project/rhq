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

/**
 * Implementations of this class are to perform specific tasks to authenticate an incoming command. These security
 * checks are performed above and beyond any transport layer security (such as SSL).
 *
 * @author John Mazzitelli
 */
public interface CommandAuthenticator {
    /**
     * The given command will be authenticated by some security mechanism determined by the implementor of this method.
     *
     * @param  command the command to authenticate
     *
     * @return <code>true</code> if the command passes the security checks and can be processed. <code>false</code> if
     *         the command failed to be authenticated - in this case, the command should be rejected and no further
     *         processing of the command should take place
     */
    boolean isAuthenticated(Command command);
}