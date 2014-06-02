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
package org.rhq.coregui.server.gwt;

import javax.ejb.EJBException;

import org.rhq.core.domain.install.remote.AgentInstallInfo;
import org.rhq.core.domain.install.remote.CustomAgentInstallData;
import org.rhq.core.domain.install.remote.RemoteAccessInfo;
import org.rhq.core.domain.install.remote.SSHSecurityException;
import org.rhq.coregui.client.gwt.RemoteInstallGWTService;
import org.rhq.coregui.server.util.SerialUtility;
import org.rhq.enterprise.server.install.remote.RemoteInstallManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Greg Hinkle
 */
public class RemoteInstallGWTServiceImpl extends AbstractGWTServiceImpl implements RemoteInstallGWTService {
    private static final long serialVersionUID = 1L;

    private RemoteInstallManagerLocal remoteInstallManager = LookupUtil.getRemoteInstallManager();

    public void checkSSHConnection(RemoteAccessInfo remoteAccessInfo) throws SSHSecurityException, RuntimeException {
        try {
            remoteInstallManager.checkSSHConnection(getSessionSubject(), remoteAccessInfo);
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    public boolean agentInstallCheck(RemoteAccessInfo remoteAccessInfo, String agentInstallPath)
        throws SSHSecurityException, RuntimeException {
        try {
            return SerialUtility.prepare(remoteInstallManager.agentInstallCheck(getSessionSubject(), remoteAccessInfo,
                agentInstallPath), "RemoteInstallService.agentInstallCheck");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    public AgentInstallInfo installAgent(RemoteAccessInfo remoteAccessInfo, CustomAgentInstallData customData)
        throws SSHSecurityException, RuntimeException {
        try {
            return SerialUtility.prepare(
                remoteInstallManager.installAgent(getSessionSubject(), remoteAccessInfo, customData),
                "RemoteInstallService.installAgent");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    public String uninstallAgent(RemoteAccessInfo remoteAccessInfo) throws SSHSecurityException, RuntimeException {
        try {
            return SerialUtility.prepare(remoteInstallManager.uninstallAgent(getSessionSubject(), remoteAccessInfo),
                "RemoteInstallService.uninstallAgent");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    public String startAgent(RemoteAccessInfo remoteAccessInfo, String agentInstallPath) throws SSHSecurityException,
        RuntimeException {
        try {
            return SerialUtility.prepare(remoteInstallManager.startAgent(getSessionSubject(), remoteAccessInfo,
                agentInstallPath), "RemoteInstallService.startAgent");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    public String stopAgent(RemoteAccessInfo remoteAccessInfo, String agentInstallPath) throws SSHSecurityException,
        RuntimeException {
        try {
            return SerialUtility.prepare(remoteInstallManager.stopAgent(getSessionSubject(), remoteAccessInfo,
                agentInstallPath), "RemoteInstallService.stopAgent");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    public String agentStatus(RemoteAccessInfo remoteAccessInfo, String agentInstallPath) throws SSHSecurityException,
        RuntimeException {
        try {
            return SerialUtility.prepare(remoteInstallManager.agentStatus(getSessionSubject(), remoteAccessInfo,
                agentInstallPath), "RemoteInstallService.agentStatus");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    public String findAgentInstallPath(RemoteAccessInfo remoteAccessInfo, String parentPath)
        throws SSHSecurityException, RuntimeException {
        try {
            return SerialUtility.prepare((remoteInstallManager.findAgentInstallPath(getSessionSubject(),
                remoteAccessInfo, parentPath)), "RemoteInstallService.findAgentInstallPath");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    public String[] remotePathDiscover(RemoteAccessInfo remoteAccessInfo, String parentPath)
        throws SSHSecurityException, RuntimeException {
        try {
            return SerialUtility.prepare((remoteInstallManager.remotePathDiscover(getSessionSubject(),
                remoteAccessInfo, parentPath)), "RemoteInstallService.remotePathDiscover");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    protected RuntimeException getExceptionToThrowToClient(Throwable t) throws SSHSecurityException, RuntimeException {
        // if the SSH connection failed because of a bad or missing SSH key fingerprint, a SSHSecurityException will be thrown.
        // We want that SSHSecurityException sent back as-is to the GWT UI.
        if (t instanceof SSHSecurityException) {
            return (SSHSecurityException) t;
        } else if (t instanceof EJBException) {
            if (t.getCause() instanceof SSHSecurityException) {
                return (SSHSecurityException) t.getCause();
            }
        }
        return super.getExceptionToThrowToClient(t);
    }
}
