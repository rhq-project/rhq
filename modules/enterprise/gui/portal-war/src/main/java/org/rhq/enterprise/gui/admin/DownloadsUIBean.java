/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Provides data to the page that allows a user to download the agent update binary, client distribution,
 * connector binaries, or command-line bundle deployer.
 * 
 * @author Greg Hinkle
 */
public class DownloadsUIBean implements Serializable {
    private static final long serialVersionUID = 1L;

    public Properties getAgentVersionProperties() {
        try {
            File file = LookupUtil.getAgentManager().getAgentUpdateVersionFile();

            Properties props = new Properties();
            props.load(new FileInputStream(file));

            return props;

        } catch (Exception e) {
            throw new RuntimeException("Agent download information not available", e);
        }
    }

    public List<File> getConnectorDownloadFiles() throws Exception {
        File downloadDir = getConnectorDownloadsDir();
        List<File> files = getFiles(downloadDir);
        return files;
    }

    private File getConnectorDownloadsDir() throws Exception {
        File serverHomeDir = LookupUtil.getCoreServer().getJBossServerHomeDir();
        File downloadDir = new File(serverHomeDir, "deploy/rhq.ear/rhq-downloads/connectors");
        if (!downloadDir.exists()) {
            throw new FileNotFoundException("Missing connectors download directory at [" + downloadDir + "]");
        }
        return downloadDir;
    }

    private File getClientDownloadDir() throws Exception {
        File serverHomeDir = LookupUtil.getCoreServer().getJBossServerHomeDir();
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

    public String getBundleDeployerDownloadFileName() throws Exception {
        File downloadDir = getBundleDeployerDownloadDir();
        List<File> files = getFiles(downloadDir);
        File file = files.get(0);
        return file.getName();
    }

    private File getBundleDeployerDownloadDir() throws Exception {
        File serverHomeDir = LookupUtil.getCoreServer().getJBossServerHomeDir();
        File downloadDir = new File(serverHomeDir, "deploy/rhq.ear/rhq-downloads/bundle-deployer");
        if (!downloadDir.exists()) {
            throw new FileNotFoundException("Missing bundle deployer download directory at [" + downloadDir + "]");
        }
        return downloadDir;
    }

    private static List<File> getFiles(File downloadDir) {
        File[] filesArray = downloadDir.listFiles();
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
}
