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
package org.rhq.enterprise.communications.command.server.discovery;

import org.jboss.remoting.InvokerLocator;
import org.rhq.enterprise.communications.ServiceContainer;

/**
 * A listener interface that gets notified when the registry of known servers has changed (for example, when new servers
 * come online or old servers go down).
 *
 * @author John Mazzitelli
 * @see    ServiceContainer#addDiscoveryListener(AutoDiscoveryListener)
 */
public interface AutoDiscoveryListener {
    /**
     * Invoked when a new server has come online.
     *
     * @param locator the invoker locator of the new server that has been discovered
     */
    void serverOnline(InvokerLocator locator);

    /**
     * Invoked when an old server has gone offline.
     *
     * @param locator the invoker locator of the old server that has gone offline
     */
    void serverOffline(InvokerLocator locator);
}