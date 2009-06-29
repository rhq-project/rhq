/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.enterprise.server.support;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.management.MBeanServer;
import javax.management.MBeanServerInvocationHandler;
import javax.management.ObjectName;

import org.jboss.mx.util.MBeanServerLocator;
import org.jboss.system.server.ServerConfig;

import org.rhq.core.clientapi.agent.support.SupportAgentService;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.util.ObjectNameFactory;
import org.rhq.core.util.stream.StreamUtil;
import org.rhq.enterprise.server.agentclient.AgentClient;
import org.rhq.enterprise.server.authz.RequiredPermission;
import org.rhq.enterprise.server.core.AgentManagerLocal;

/**
 * Provides some methods that are useful for supporting managed resources. This includes being
 * able to take a snapshot report of a managed resource, such as its log files, data files, and anything
 * the managed resource wants to expose.
 * 
 * This Support subsystem provides things like snapshot reports that are potentially very sensitive and
 * gives the user access to far sweeping data, (e.g. they can contain configuration settings, data files
 * and logs - all of which can contain confidential information). Because of this, most of the methods
 * in this subsystem will require the user to be an "inventory manager" with {@link Permission#MANAGE_INVENTORY}
 * permissions.
 * 
 * @author John Mazzitelli
 */
@Stateless
public class SupportManagerBean implements SupportManagerLocal {

    @EJB
    private AgentManagerLocal agentManager;

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public InputStream getSnapshotReportStream(Subject subject, int resourceId, String name, String description)
        throws Exception {

        AgentClient agentClient = this.agentManager.getAgentClient(resourceId);
        SupportAgentService supportService = agentClient.getSupportAgentService();
        InputStream snapshotStream = supportService.getSnapshotReport(resourceId, name, description);

        return snapshotStream;
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public URL getSnapshotReport(Subject subject, int resourceId, String name, String description) throws Exception {

        InputStream snapshotStream = getSnapshotReportStream(subject, resourceId, name, description);

        // TODO: not sure what we should really do with it, for now, put it in the downloads location.
        // you can retrieve this by going to http://localhost:7080/downloads
        File dir = getDownloadsDir();
        File downloadFile = File.createTempFile(name + "-" + resourceId + "-", ".zip", dir);
        StreamUtil.copy(snapshotStream, new FileOutputStream(downloadFile));

        return downloadFile.toURI().toURL();
    }

    private File getDownloadsDir() throws Exception {
        MBeanServer mbs = MBeanServerLocator.locateJBoss();
        ObjectName name = ObjectNameFactory.create("jboss.system:type=ServerConfig");
        Object mbean = MBeanServerInvocationHandler.newProxyInstance(mbs, name, ServerConfig.class, false);
        File serverHomeDir = ((ServerConfig) mbean).getServerHomeDir();
        File downloadDir = new File(serverHomeDir, "deploy/rhq.ear/rhq-downloads");
        if (!downloadDir.exists()) {
            throw new FileNotFoundException("Missing downloads directory at [" + downloadDir + "]");
        }
        return downloadDir;
    }
}

