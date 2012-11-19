/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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

import java.util.List;

import org.rhq.core.domain.cloud.Server;
import org.rhq.core.domain.cloud.composite.ServerWithAgentCountComposite;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.gwt.CloudGWTService;
import org.rhq.enterprise.gui.coregui.server.util.SerialUtility;
import org.rhq.enterprise.server.cloud.CloudManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Jiri Kremser
 */
public class CloudGWTServiceImpl extends AbstractGWTServiceImpl implements CloudGWTService {

    private static final long serialVersionUID = 1L;

    private CloudManagerLocal cloudManager = LookupUtil.getCloudManager();

    @Override
    public PageList<ServerWithAgentCountComposite> getServers(PageControl pc) throws RuntimeException {
        try {
            return SerialUtility.prepare(cloudManager.getServerComposites(getSessionSubject(), pc), "CloudGWTServiceImpl.getServers");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public Server getServerById(int serverId) throws RuntimeException {
        try {
            return SerialUtility.prepare(cloudManager.getServerById(serverId), "CloudGWTServiceImpl.getServerById");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public List<Agent> getAgentsByServerName(String serverName) throws RuntimeException {
        try {
            return SerialUtility.prepare(cloudManager.getAgentsByServerName(serverName), "CloudGWTServiceImpl.getAgentsByServerName");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

}
