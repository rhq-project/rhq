/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.enterprise.server.install.remote;

import javax.ejb.Remote;

/**
 * Future Remote API...
 * 
 * @author Greg Hinkle
 */
@Remote
public interface RemoteInstallManagerRemote {

    /* To Be Updated, commented :-) and Released in next version... 
     * 
        AgentInstallInfo agentInstallCheck(Subject subject, RemoteAccessInfo remoteAccessInfo);

        AgentInstallInfo installAgent(Subject subject, RemoteAccessInfo remoteAccessInfo, String path);

        String[] remotePathDiscover(Subject subject, RemoteAccessInfo remoteAccessInfo, String parentPath);

        String startAgent(Subject subject, RemoteAccessInfo remoteAccessInfo);

        String stopAgent(Subject subject, RemoteAccessInfo remoteAccessInfo);

        String agentStatus(Subject subject, RemoteAccessInfo remoteAccessInfo);
    */

}
