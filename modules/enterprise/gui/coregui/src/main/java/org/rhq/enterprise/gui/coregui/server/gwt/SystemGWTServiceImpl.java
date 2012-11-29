/*
 * RHQ Management Platform
 * Copyright (C) 2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.server.gwt;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.rhq.core.domain.common.ProductInfo;
import org.rhq.core.domain.common.ServerDetails;
import org.rhq.core.domain.common.composite.SystemSettings;
import org.rhq.enterprise.gui.coregui.client.gwt.SystemGWTService;
import org.rhq.enterprise.server.core.AgentManagerLocal;
import org.rhq.enterprise.server.system.SystemManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Provides access to the system manager that allows you to obtain information about
 * the server as well as allowing you to reconfigure parts of the server.
 *
 * @author John Mazzitelli
 * @author Ian Springer
 */
public class SystemGWTServiceImpl extends AbstractGWTServiceImpl implements SystemGWTService {

    private static final long serialVersionUID = 1L;

    private SystemManagerLocal systemManager = LookupUtil.getSystemManager();
    private AgentManagerLocal agentManager = LookupUtil.getAgentManager();

    @Override
    public ProductInfo getProductInfo() throws RuntimeException {
        try {
            return systemManager.getProductInfo(getSessionSubject());
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public ServerDetails getServerDetails() throws RuntimeException {
        try {
            return systemManager.getServerDetails(getSessionSubject());
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public SystemSettings getSystemSettings() throws RuntimeException {
        try {
            return systemManager.getSystemSettings(getSessionSubject());
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public void setSystemSettings(SystemSettings settings) throws RuntimeException {
        try {
            systemManager.setSystemSettings(getSessionSubject(), settings);
            systemManager.reconfigureSystem(getSessionSubject());
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public void dumpToLog() {
        systemManager.dumpSystemInfo(getSessionSubject());
    }

    @Override
    public HashMap<String, String> getAgentVersionProperties() throws RuntimeException {
        try {
            File file = agentManager.getAgentUpdateVersionFile();

            Properties props = new Properties();
            FileInputStream inStream = new FileInputStream(file);
            try {
                props.load(inStream);
            } finally {
                inStream.close();
            }

            return convertFromProperties(props);
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t, "Agent download information not available.");
        }
    }

    @Override
    public HashMap<String, String> getConnectorDownloads() throws RuntimeException {
        try {
            File downloadDir = getConnectorDownloadsDir();
            List<File> files = getFiles(downloadDir);
            if (files == null) {
                return new HashMap<String, String>(0);
            }
            HashMap<String, String> map = new HashMap<String, String>(files.size());
            for (File file : files) {
                // key is the filename, value is the relative URL to download the file from the server
                map.put(file.getName(), "/downloads/connectors/" + file.getName());
            }
            return map;
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }

    }

    @Override
    public HashMap<String, String> getCliAlertScriptDownloads() throws RuntimeException {
        try {
            File downloadsDir = getCliAlertScriptDownloadsDir();
            List<File> files = getFiles(downloadsDir);
            if (files == null) {
                return new HashMap<String, String>(0);
            } else {
                HashMap<String, String> ret = new HashMap<String, String>(files.size());

                for (File file : files) {
                    ret.put(file.getName(), "/downloads/cli-alert-scripts/" + file.getName());
                }
                return ret;
            }
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public HashMap<String, String> getScriptModulesDownloads() throws RuntimeException {
        try {
            File downloadsDir = getScriptModulesDownloadsDir();
            List<File> files = getFiles(downloadsDir);
            if (files == null) {
                return new HashMap<String, String>(0);
            } else {
                HashMap<String, String> ret = new HashMap<String, String>(files.size());

                for (File file : files) {
                    ret.put(file.getName(), "/downloads/script-modules/" + file.getName());
                }
                return ret;
            }
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public HashMap<String, String> getClientVersionProperties() throws RuntimeException {
        File versionFile = new File(getClientDataDownloadDir(), "rhq-client-version.properties");
        try {
            Properties p = new Properties();
            FileInputStream inStream = new FileInputStream(versionFile);
            try {
                p.load(inStream);
            } finally {
                inStream.close();
            }
            return convertFromProperties(p);
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t, "Unable to retrieve client version info.");
        }
    }

    @Override
    public HashMap<String, String> getBundleDeployerDownload() throws RuntimeException {
        try {
            File downloadDir = getBundleDeployerDownloadDir();
            List<File> files = getFiles(downloadDir);
            if (files.isEmpty()) {
                throw new RuntimeException("Missing bundle deployer download file");
            }
            File file = files.get(0);
            HashMap<String, String> ret = new HashMap<String, String>(1);
            ret.put(file.getName(), "/downloads/bundle-deployer/" + file.getName());
            return ret;
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }

    }

    @Override
    public Boolean isLdapAuthorizationEnabled() throws RuntimeException {
        try {
            return systemManager.isLdapAuthorizationEnabled();
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    private File getConnectorDownloadsDir() {
        File homeDir = getDownloadHomeDir();
        File downloadDir = new File(homeDir, "connectors");
        if (!downloadDir.isDirectory()) {
            throw new RuntimeException("Server is missing connectors download directory at [" + downloadDir + "]");
        }
        return downloadDir;
    }

    private File getCliAlertScriptDownloadsDir() {
        File homeDir = getDownloadHomeDir();
        File downloadDir = new File(homeDir, "cli-alert-scripts");
        if (!downloadDir.isDirectory()) {
            throw new RuntimeException("Server is missing connectors download directory at [" + downloadDir + "]");
        }
        return downloadDir;
    }

    private File getScriptModulesDownloadsDir() {
        File homeDir = getDownloadHomeDir();
        File downloadDir = new File(homeDir, "script-modules");
        if (!downloadDir.isDirectory()) {
            throw new RuntimeException("Server is missing connectors download directory at [" + downloadDir + "]");
        }
        return downloadDir;
    }

    private File getClientDataDownloadDir() {
        File earDir = LookupUtil.getCoreServer().getJBossServerDataDir();
        File downloadDir = new File(earDir, "rhq-downloads/rhq-client");
        if (!downloadDir.isDirectory()) {
            throw new RuntimeException("Server is missing client download directory at [" + downloadDir + "]");
        }
        return downloadDir;
    }

    private File getBundleDeployerDownloadDir() {
        File homeDir = getDownloadHomeDir();
        File downloadDir = new File(homeDir, "bundle-deployer");
        if (!downloadDir.isDirectory()) {
            throw new RuntimeException("Missing bundle deployer download directory at [" + downloadDir + "]");
        }
        return downloadDir;
    }

    private File getDownloadHomeDir() {
        File earDeployDir = LookupUtil.getCoreServer().getEarDeploymentDir();
        File downloadDir = new File(earDeployDir, "rhq-downloads");
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

    // GWT does not support java.util.Properties - we have to convert to/from Properties <-> HashMap
    @SuppressWarnings("unused")
    private Properties convertToProperties(HashMap<String, String> map) {
        Properties props = new Properties();
        if (map != null) {
            props.putAll(map);
        }
        return props;
    }

    private HashMap<String, String> convertFromProperties(Properties props) {
        if (props == null) {
            return new HashMap<String, String>(0);
        }
        HashMap<String, String> map = new HashMap<String, String>(props.size());
        for (Map.Entry<Object, Object> entry : props.entrySet()) {
            map.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
        }
        return map;
    }
}