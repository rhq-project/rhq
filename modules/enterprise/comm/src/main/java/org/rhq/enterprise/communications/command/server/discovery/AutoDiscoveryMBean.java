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

/**
 * JMX interface to the AutoDiscovery MBean.
 *
 * @author John Mazzitelli
 */
public interface AutoDiscoveryMBean {
    /**
     * Returns the JMX <code>ObjectName</code> of the Network Registry that is used to perform auto-discovery.
     *
     * @return name of Network Registry MBean
     */
    String getNetworkRegistryName();

    /**
     * Sets the name (in String form of a JMX <code>ObjectName</code>) of the Network Registry that is used to perform
     * auto-discovery and begins listeningn to it. The caller must know what it is doing - this must be the name the
     * Network Registry is registered as, otherwise, auto-discovery will fail.
     *
     * <p>Note that if this object is already listening to a Network Registry, it will be unregistered before setting
     * the name.</p>
     *
     * @param name name the registered name of the Network Registry MBean
     */
    void setNetworkRegistryName(String name);
}