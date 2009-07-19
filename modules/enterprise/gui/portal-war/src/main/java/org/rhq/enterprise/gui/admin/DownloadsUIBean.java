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
package org.rhq.enterprise.gui.admin;

import org.jboss.mx.util.MBeanServerLocator;
import org.jboss.system.server.ServerConfig;
import org.rhq.core.util.ObjectNameFactory;
import org.rhq.core.util.stream.StreamUtil;
import org.rhq.enterprise.server.util.LookupUtil;

import javax.management.MBeanServer;
import javax.management.MBeanServerInvocationHandler;
import javax.management.ObjectName;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.Serializable;
import java.util.Properties;
import java.util.List;
import java.util.ArrayList;

/**
 * @author Greg Hinkle
 */
public class DownloadsUIBean implements Serializable {


    public Properties getAgentVersionProperties() {

        try {
            File file = LookupUtil.getAgentManager().getAgentUpdateVersionFile();

            Properties p = new Properties();
            p.load(new FileInputStream(file));

            return p;

        } catch (Exception e) {
            throw new RuntimeException("Agent download information not available", e);
        }

    }

    private MBeanServer getMBeanServer() {
        return MBeanServerLocator.locateJBoss();
    }


    public List<File> getDownloadFiles() throws Exception {
        File downloadDir = getDownloadsDir();
        File[] filesArray = downloadDir.listFiles();

        // we only serve up files located in the flat rhq-downloads directory - no content from subdirectories
        List<File> files = new ArrayList<File>();
        if (filesArray != null) {
            for (File file : filesArray) {
                if (file.isFile()) {
                    files.add(file);
                }
            }
        }
        return files;
    }


    private File getDownloadsDir() throws Exception {
        MBeanServer mbs = getMBeanServer();
        ObjectName name = ObjectNameFactory.create("jboss.system:type=ServerConfig");
        Object mbean = MBeanServerInvocationHandler.newProxyInstance(mbs, name, ServerConfig.class, false);
        File serverHomeDir = ((ServerConfig) mbean).getServerHomeDir();
        File downloadDir = new File(serverHomeDir, "deploy/rhq.ear/rhq-downloads");
        if (!downloadDir.exists()) {
            throw new FileNotFoundException("Missing downloads directory at [" + downloadDir + "]");
        }
        return downloadDir;
    }



    private File getClientDownloadDir() throws Exception {
        MBeanServer mbs = getMBeanServer();
        ObjectName name = ObjectNameFactory.create("jboss.system:type=ServerConfig");
        Object mbean = MBeanServerInvocationHandler.newProxyInstance(mbs, name, ServerConfig.class, false);
        File serverHomeDir = ((ServerConfig) mbean).getServerHomeDir();
        File downloadDir = new File(serverHomeDir, "deploy/rhq.ear/rhq-downloads/rhq-client");
        if (!downloadDir.exists()) {
            throw new FileNotFoundException("Missing remote client download directory at [" + downloadDir + "]");
        }
        return downloadDir;
    }

    public Properties getClientVersionProperties() throws Exception {
        File versionFile = new File(getClientDownloadDir(), "rhq-client-version.properties");
        try {
            Properties p = new Properties();
            p.load(new FileInputStream(versionFile));
            return p;
        } catch (Exception e) {
            throw new RuntimeException("Unable to retrieve client version info", e);
        }
    }


}
