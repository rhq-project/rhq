/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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
package org.rhq.enterprise.gui.installer.server.servlet;

import javax.servlet.annotation.WebServlet;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

import org.jboss.as.controller.client.ModelControllerClient;

import org.rhq.common.jbossas.client.controller.Address;
import org.rhq.common.jbossas.client.controller.JBossASClient;
import org.rhq.enterprise.gui.installer.client.gwt.InstallerGWTService;
import org.rhq.enterprise.gui.installer.server.service.ManagementService;

/**
 * @author John Mazzitelli
 */
@WebServlet(value = "/org.rhq.enterprise.gui.installer.Installer/InstallerGWTService")
public class InstallerGWTServiceImpl extends RemoteServiceServlet implements InstallerGWTService {

    private static final long serialVersionUID = 1L;

    @Override
    public String getAppServerVersion() throws Exception {
        try {
            String version = getClient().getStringAttribute("release-version", Address.root());
        return version;
        } catch (Exception t) {
            t.printStackTrace();
            throw t;
        }
    }

    @Override
    public String getOperatingSystem() throws Exception {
        try {
        String[] address = { "core-service", "platform-mbean", "type", "operating-system" };
            String osName = getClient().getStringAttribute("name", Address.root().add(address));
        return osName;
        } catch (Exception t) {
            t.printStackTrace();
            throw t;
        }
    }

    private JBossASClient getClient() {
        ModelControllerClient client = ManagementService.getClient();
        return new JBossASClient(client);
    }
}
