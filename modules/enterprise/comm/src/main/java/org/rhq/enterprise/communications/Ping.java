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

import org.rhq.enterprise.communications.command.impl.echo.EchoCommand;

/**
 * The interface to the Ping POJO - this is the interface that will be remotely exposed to clients.
 *
 * @author John Mazzitelli
 */
public interface Ping {
    /**
     * Pings the POJO to ensure connectivity. This simply returns the <code>echoMessage</code> back, prefixed with the
     * optional prefix message. This mimics the functionality of the {@link EchoCommand}.
     *
     * @param  echoMessage message to echo back to the caller
     * @param  prefix      the string to be prepended to the echo message before being returned (may be <code>
     *                     null</code>)
     *
     * @return the echo message, prepended with the prefix string
     */
    String ping(String echoMessage, String prefix);
}