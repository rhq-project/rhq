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
package org.rhq.coregui.client.gwt;

import com.google.gwt.user.client.rpc.RemoteService;

import org.rhq.core.domain.install.remote.AgentInstallInfo;
import org.rhq.core.domain.install.remote.CustomAgentInstallData;
import org.rhq.core.domain.install.remote.RemoteAccessInfo;
import org.rhq.core.domain.install.remote.SSHSecurityException;

/**
 * Provides methods to remotely install, start and stop agents over SSH.
 *
 * @author Greg Hinkle
 * @author John Mazzitelli
 */
public interface RemoteInstallGWTService extends RemoteService {

    boolean agentInstallCheck(RemoteAccessInfo remoteAccessInfo, String agentInstallPath) throws SSHSecurityException,
        RuntimeException;

    AgentInstallInfo installAgent(RemoteAccessInfo remoteAccessInfo, CustomAgentInstallData customData)
        throws SSHSecurityException, RuntimeException;

    String uninstallAgent(RemoteAccessInfo remoteAccessInfo) throws SSHSecurityException, RuntimeException;

    String startAgent(RemoteAccessInfo remoteAccessInfo, String agentInstallPath) throws SSHSecurityException,
        RuntimeException;

    String stopAgent(RemoteAccessInfo remoteAccessInfo, String agentInstallPath) throws SSHSecurityException,
        RuntimeException;

    String agentStatus(RemoteAccessInfo remoteAccessInfo, String agentInstallPath) throws SSHSecurityException,
        RuntimeException;

    String findAgentInstallPath(RemoteAccessInfo remoteAccessInfo, String parentPath) throws SSHSecurityException,
        RuntimeException;

    String[] remotePathDiscover(RemoteAccessInfo remoteAccessInfo, String parentPath) throws SSHSecurityException,
        RuntimeException;
}
