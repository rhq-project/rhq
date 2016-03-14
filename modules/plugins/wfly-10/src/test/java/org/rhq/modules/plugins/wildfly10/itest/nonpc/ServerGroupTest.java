/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.modules.plugins.wildfly10.itest.nonpc;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.resource.CreateResourceStatus;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.CreateResourceReport;
import org.rhq.modules.plugins.wildfly10.ASConnection;
import org.rhq.modules.plugins.wildfly10.HostControllerComponent;
import org.rhq.modules.plugins.wildfly10.json.Remove;
import org.rhq.modules.plugins.wildfly10.test.util.ASConnectionFactory;
import org.rhq.modules.plugins.wildfly10.test.util.Constants;

/**
 * Tests around server groups
 *
 * @author Heiko W. Rupp
 */
public class ServerGroupTest extends AbstractIntegrationTest {

    public void createServerGroupViaApi() throws Exception {
        ASConnection connection = ASConnectionFactory.getDomainControllerASConnection();
        HostControllerComponent hcc = new HostControllerComponent();
        hcc.setConnection(connection);

        Configuration rc = new Configuration();
        rc.put(new PropertySimple("profile", "default"));
        rc.put(new PropertySimple("socket-binding-group", "standard-sockets"));
        ResourceType rt = new ResourceType("ServerGroup", Constants.PLUGIN_NAME, ResourceCategory.SERVICE, null);

        String serverGroupName = "_test-sg";
        try {
            CreateResourceReport report = new CreateResourceReport(serverGroupName, rt, new Configuration(), rc, null);
            report = hcc.createResource(report);

            assert report != null : "Report was null.";
            assert report.getStatus() == CreateResourceStatus.SUCCESS : "Create was a failure: "
                + report.getErrorMessage();
        } finally {
            Remove r = new Remove("server-group", serverGroupName);
            connection.execute(r);
        }
    }

    public void badCreateServerGroupViaApi() throws Exception {
        ASConnection connection = ASConnectionFactory.getDomainControllerASConnection();
        HostControllerComponent hcc = new HostControllerComponent();
        hcc.setConnection(connection);

        Configuration rc = new Configuration();
        rc.put(new PropertySimple("profile", "luzibumpf")); // Does not exist op should fail
        rc.put(new PropertySimple("socket-binding-group", "standard-sockets"));
        ResourceType rt = new ResourceType("ServerGroup", Constants.PLUGIN_NAME, ResourceCategory.SERVICE, null);

        String serverGroupName = "_test-sg";
        try {
            CreateResourceReport report = new CreateResourceReport(serverGroupName, rt, new Configuration(), rc, null);
            report = hcc.createResource(report);

            assert report != null : "Report was null.";
            assert report.getStatus() == CreateResourceStatus.FAILURE : "Is AS7-1430 solved ?";
            assert report.getException() == null : report.getException();
        } finally {
            Remove r = new Remove("server-group", serverGroupName);
            connection.execute(r);
        }
    }

}
