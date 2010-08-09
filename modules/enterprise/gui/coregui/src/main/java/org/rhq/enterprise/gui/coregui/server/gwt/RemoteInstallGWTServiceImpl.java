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
package org.rhq.enterprise.gui.coregui.server.gwt;

import org.rhq.core.domain.install.remote.AgentInstallInfo;
import org.rhq.core.domain.install.remote.RemoteAccessInfo;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.enterprise.gui.coregui.client.gwt.RemoteInstallGWTService;
import org.rhq.enterprise.gui.coregui.server.util.SerialUtility;
import org.rhq.enterprise.server.install.remote.RemoteInstallManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Greg Hinkle
 */
public class RemoteInstallGWTServiceImpl extends AbstractGWTServiceImpl implements RemoteInstallGWTService {
    private static final long serialVersionUID = 1L;

    private RemoteInstallManagerLocal remoteInstallManager = LookupUtil.getRemoteInstallManager();

    public boolean agentInstallCheck(RemoteAccessInfo remoteAccessInfo, String agentInstallPath) {
        try {
            return SerialUtility.prepare(remoteInstallManager.agentInstallCheck(getSessionSubject(), remoteAccessInfo,
                agentInstallPath), "RemoteInstallService.agentInstallCheck");
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    public AgentInstallInfo installAgent(RemoteAccessInfo remoteAccessInfo, String parentPath) {
        try {
            return SerialUtility.prepare(remoteInstallManager.installAgent(getSessionSubject(), remoteAccessInfo,
                parentPath), "RemoteInstallService.installAgent");
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    public String startAgent(RemoteAccessInfo remoteAccessInfo, String agentInstallPath) {
        try {
            return SerialUtility.prepare(remoteInstallManager.startAgent(getSessionSubject(), remoteAccessInfo,
                agentInstallPath), "RemoteInstallService.startAgent");
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    public String stopAgent(RemoteAccessInfo remoteAccessInfo, String agentInstallPath) {
        try {
            return SerialUtility.prepare(remoteInstallManager.stopAgent(getSessionSubject(), remoteAccessInfo,
                agentInstallPath), "RemoteInstallService.stopAgent");
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    public String agentStatus(RemoteAccessInfo remoteAccessInfo, String agentInstallPath) {
        try {
            return SerialUtility.prepare(remoteInstallManager.agentStatus(getSessionSubject(), remoteAccessInfo,
                agentInstallPath), "RemoteInstallService.agentStatus");
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    public String findAgentInstallPath(RemoteAccessInfo remoteAccessInfo, String parentPath) {
        try {
            return SerialUtility.prepare((remoteInstallManager.findAgentInstallPath(getSessionSubject(),
                remoteAccessInfo, parentPath)), "RemoteInstallService.findAgentInstallPath");
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    public String[] remotePathDiscover(RemoteAccessInfo remoteAccessInfo, String parentPath) {
        try {
            return SerialUtility.prepare((remoteInstallManager.remotePathDiscover(getSessionSubject(),
                remoteAccessInfo, parentPath)), "RemoteInstallService.remotePathDiscover");
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }
}
