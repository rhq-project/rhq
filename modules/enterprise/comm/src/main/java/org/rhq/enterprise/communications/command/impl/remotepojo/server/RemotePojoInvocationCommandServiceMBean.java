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
package org.rhq.enterprise.communications.command.impl.remotepojo.server;

import org.rhq.enterprise.communications.command.client.ClientRemotePojoFactory;
import org.rhq.enterprise.communications.command.server.CommandServiceMBean;

/**
 * The MBean interface to the remote POJO invocation command service. This interface introduces the ability to add a new
 * POJO so it can be remotely invoked and remove a remote POJO so it is no longer remoted.
 *
 * @author John Mazzitelli
 */
public interface RemotePojoInvocationCommandServiceMBean extends CommandServiceMBean {
    /**
     * Adds the given POJO so it becomes remoteable via the {@link ClientRemotePojoFactory}. The <code>
     * remoteInterfaceName</code> will be the name of the interface that is exposed to the remote clients.
     *
     * @param pojo                the POJO to add to the list of remoted POJOs
     * @param remoteInterfaceName name of the interface to expose to remote clients
     *
     * @see   #removePojo(String)
     * @see   #removePojo(Class)
     */
    void addPojo(Object pojo, String remoteInterfaceName);

    /**
     * Adds the given POJO so it becomes remoteable via the {@link ClientRemotePojoFactory}. The <code>
     * remoteInterface</code> will be the interface that is exposed to the remote clients.
     *
     * @param pojo            the POJO to add to the list of remoted POJOs
     * @param remoteInterface the interface to expose to remote clients
     *
     * @see   #removePojo(String)
     * @see   #removePojo(Class)
     */
    <T> void addPojo(T pojo, Class<T> remoteInterface);

    /**
     * Removes the POJO with the given remoted interface so it no longer is remoteable and cannot be invoked via remote
     * clients. The <code>remoteInterfaceName</code> must be the name of the interface that was exposed to the remote
     * clients.
     *
     * @param remoteInterfaceName name of the interface that was exposed to remote clients
     *
     * @see   #addPojo(Object, String)
     * @see   #addPojo(Object, Class)
     */
    void removePojo(String remoteInterfaceName);

    /**
     * Removes the POJO with the given remoted interface so it no longer is remoteable and cannot be invoked via remote
     * clients. The <code>remoteInterface</code> must be the interface that was exposed to the remote clients.
     *
     * @param remoteInterface the interface that was exposed to remote clients
     *
     * @see   #addPojo(Object, String)
     * @see   #addPojo(Object, Class)
     */
    void removePojo(Class<?> remoteInterface);
}