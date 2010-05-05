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

import javax.ejb.Stateless;

import org.rhq.core.domain.install.remote.AgentInstallInfo;
import org.rhq.core.domain.install.remote.RemoteAccessInfo;
import org.rhq.enterprise.server.authz.RequiredPermission;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;

/**
 * @author Greg Hinkle
 */
@Stateless
public class RemoteInstallManagerBean implements RemoteInstallManagerLocal, RemoteInstallManagerRemote {


    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public AgentInstallInfo agentInstallCheck(Subject subject, RemoteAccessInfo remoteAccessInfo) {
        SSHInstallUtility sshUtil = new SSHInstallUtility(remoteAccessInfo);

        return sshUtil.installAgent();
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public AgentInstallInfo installAgent(Subject subject, RemoteAccessInfo remoteAccessInfo, String path) {
        SSHInstallUtility sshUtil = new SSHInstallUtility(remoteAccessInfo);

        return sshUtil.installAgent();
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public String[] remotePathDiscover(Subject subject, RemoteAccessInfo remoteAccessInfo, String parentPath) {
        SSHInstallUtility ssh = new SSHInstallUtility(remoteAccessInfo);
        ssh.connect();
        return ssh.pathDiscovery(parentPath);
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public String startAgent(Subject subject, RemoteAccessInfo remoteAccessInfo) {
        SSHInstallUtility sshUtil = new SSHInstallUtility(remoteAccessInfo);
        return sshUtil.agentStart();
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public String stopAgent(Subject subject, RemoteAccessInfo remoteAccessInfo) {
        SSHInstallUtility sshUtil = new SSHInstallUtility(remoteAccessInfo);
        return sshUtil.agentStop();
    }

    public String agentStatus(Subject subject, RemoteAccessInfo remoteAccessInfo) {
        if (remoteAccessInfo.getHost() == null) {
            return "Enter a host";
        }
        SSHInstallUtility sshUtil = new SSHInstallUtility(remoteAccessInfo);
        return sshUtil.agentStatus();
    }
}
