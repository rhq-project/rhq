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
import org.jboss.dmr.ModelNode;

import org.rhq.enterprise.gui.installer.client.gwt.InstallerGWTService;
import org.rhq.enterprise.gui.installer.server.service.ManagementService;

/**
 * @author John Mazzitelli
 */
@WebServlet(value = "/org.rhq.enterprise.gui.installer.Installer/InstallerGWTService")
public class InstallerGWTServiceImpl extends RemoteServiceServlet implements InstallerGWTService {

    private static final long serialVersionUID = 1L;

    @Override
    public String getAppServerVersion() {
        ModelControllerClient client = ManagementService.getClient();

        // create the read operation we want to invoke on the root AS resource
        ModelNode op = new ModelNode();
        op.get("operation").set("read-attribute");
        op.get("name").set("release-version");

        String versionString;

        try {
            ModelNode results = client.execute(op);
            ModelNode version = results.get("result");
            versionString = version.asString();
        } catch (Exception e) {
            versionString = e.toString();
        }

        return versionString;
    }

    @Override
    public String getOperatingSystem() {
        ModelControllerClient client = ManagementService.getClient();

        // create the read operation we want to invoke on the platform MBean operating system resource
        ModelNode op = new ModelNode();
        op.get("operation").set("read-attribute");
        op.get("name").set("name");

        // provide the target address to the platform MBean operating system resource via ModelNode list
        ModelNode address = op.get("address");
        address.add("core-service", "platform-mbean");
        address.add("type", "operating-system");

        String osName;

        try {
            ModelNode results = client.execute(op);
            ModelNode version = results.get("result");
            osName = version.asString();
        } catch (Exception e) {
            osName = e.toString();
        }

        return osName;
    }

}
