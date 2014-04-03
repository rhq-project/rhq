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

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.install.remote.AgentInstall;
import org.rhq.core.domain.install.remote.AgentInstallInfo;
import org.rhq.core.domain.install.remote.RemoteAccessInfo;
import org.rhq.enterprise.server.authz.RequiredPermission;
import org.rhq.enterprise.server.core.AgentManagerLocal;

/**
 * Installs, starts and stops remote agents via SSH.
 *
 * @author Greg Hinkle
 * @author John Mazzitelli
 */
@Stateless
public class RemoteInstallManagerBean implements RemoteInstallManagerLocal, RemoteInstallManagerRemote {

    @EJB
    private AgentManagerLocal agentManager;

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public boolean agentInstallCheck(Subject subject, RemoteAccessInfo remoteAccessInfo, String agentInstallPath) {
        SSHInstallUtility sshUtil = getSSHConnection(remoteAccessInfo);
        try {
            return sshUtil.agentInstallCheck(agentInstallPath);
        } finally {
            sshUtil.disconnect();
        }
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public AgentInstallInfo installAgent(Subject subject, RemoteAccessInfo remoteAccessInfo, String parentPath) {
        return installAgent(subject, remoteAccessInfo, parentPath, false);
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public AgentInstallInfo installAgent(Subject subject, RemoteAccessInfo remoteAccessInfo, String parentPath,
        boolean overwriteExistingAgent) {

        boolean agentAlreadyInstalled = agentInstallCheck(subject, remoteAccessInfo, parentPath);
        if (agentAlreadyInstalled) {
            if (!overwriteExistingAgent) {
                throw new IllegalStateException("Agent appears to already be installed under: " + parentPath);
            } else {
                // we were asked to overwrite it; make sure we shut it down first before the install happens (which will remove it)
                stopAgent(subject, remoteAccessInfo, parentPath);
            }
        }

        // before we install, let's create a AgentInstall and pass its ID
        // as the install ID so the agent can link up with it when it registers.
        AgentInstall agentInstall = new AgentInstall();
        agentInstall.setSshHost(remoteAccessInfo.getHost());
        agentInstall.setSshPort(remoteAccessInfo.getPort());
        agentInstall.setSshUsername(remoteAccessInfo.getUser());
        agentInstall.setSshPassword(remoteAccessInfo.getPassword());

        AgentInstall ai = agentManager.updateAgentInstall(subject, agentInstall);
        SSHInstallUtility sshUtil = getSSHConnection(remoteAccessInfo);
        try {
            return sshUtil.installAgent(parentPath, String.valueOf(ai.getId()));
        } finally {
            sshUtil.disconnect();
        }
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public String startAgent(Subject subject, RemoteAccessInfo remoteAccessInfo, String agentInstallPath) {
        SSHInstallUtility sshUtil = getSSHConnection(remoteAccessInfo);
        try {
            return sshUtil.startAgent(agentInstallPath);
        } finally {
            sshUtil.disconnect();
        }
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public String stopAgent(Subject subject, RemoteAccessInfo remoteAccessInfo, String agentInstallPath) {
        SSHInstallUtility sshUtil = getSSHConnection(remoteAccessInfo);
        try {
            return sshUtil.stopAgent(agentInstallPath);
        } finally {
            sshUtil.disconnect();
        }
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public String agentStatus(Subject subject, RemoteAccessInfo remoteAccessInfo, String agentInstallPath) {
        SSHInstallUtility sshUtil = getSSHConnection(remoteAccessInfo);
        try {
            return sshUtil.agentStatus(agentInstallPath);
        } finally {
            sshUtil.disconnect();
        }
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public String findAgentInstallPath(Subject subject, RemoteAccessInfo remoteAccessInfo, String parentPath) {
        SSHInstallUtility sshUtil = getSSHConnection(remoteAccessInfo);
        try {
            return sshUtil.findAgentInstallPath(parentPath);
        } finally {
            sshUtil.disconnect();
        }
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public String[] remotePathDiscover(Subject subject, RemoteAccessInfo remoteAccessInfo, String parentPath) {
        SSHInstallUtility sshUtil = getSSHConnection(remoteAccessInfo);
        try {
            return sshUtil.pathDiscovery(parentPath);
        } finally {
            sshUtil.disconnect();
        }
    }

    private SSHInstallUtility getSSHConnection(RemoteAccessInfo remoteAccessInfo) {
        if (remoteAccessInfo.getHost() == null) {
            throw new RuntimeException("Enter a host");
        }
        SSHInstallUtility sshUtil = new SSHInstallUtility(remoteAccessInfo);
        return sshUtil;
    }
}
