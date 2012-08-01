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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.servlet.annotation.WebServlet;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

import org.jboss.as.controller.client.ModelControllerClient;

import org.rhq.common.jbossas.client.controller.Address;
import org.rhq.common.jbossas.client.controller.JBossASClient;
import org.rhq.common.jbossas.client.controller.SecurityDomainJBossASClient;
import org.rhq.core.util.PropertiesFileUpdate;
import org.rhq.enterprise.gui.installer.client.gwt.InstallerGWTService;
import org.rhq.enterprise.gui.installer.client.shared.ServerDetails;
import org.rhq.enterprise.gui.installer.server.service.ManagementService;

/**
 * @author John Mazzitelli
 */
@WebServlet(value = "/org.rhq.enterprise.gui.installer.Installer/InstallerGWTService")
public class InstallerGWTServiceImpl extends RemoteServiceServlet implements InstallerGWTService {

    private static final long serialVersionUID = 1L;

    private static final String RHQ_SECURITY_DOMAIN = "RHQDSSecurityDomain";

    @Override
    public ArrayList<String> getServerNames(String connectionUrl, String username, String password) throws Exception {
        try {
            return ServerInstallUtil.getServerNames(connectionUrl, username, password);
        } catch (Exception e) {
            log("Could not get the list of registered server names", e);
            return null;
        }
    }

    @Override
    public ServerDetails getServerDetails(String connectionUrl, String username, String password, String serverName)
        throws Exception {
        try {
            return ServerInstallUtil.getServerDetails(connectionUrl, username, password, serverName);
        } catch (Exception e) {
            log("Could not get server details for [" + serverName + "]", e);
            return null;
        }
    }

    @Override
    public boolean isDatabaseSchemaExist(String connectionUrl, String username, String password) throws Exception {
        try {
            return ServerInstallUtil.isDatabaseSchemaExist(connectionUrl, username, password);
        } catch (Exception e) {
            log("Could not determine database existence", e);
            return false;
        }
    }

    @Override
    public String testConnection(String connectionUrl, String username, String password) throws Exception {
        String results = ServerInstallUtil.testConnection(connectionUrl, username, password);
        return results;
    }

    @Override
    public HashMap<String, String> getServerProperties() throws Exception {
        File serverPropertiesFile = getServerPropertiesFile();
        PropertiesFileUpdate propsFile = new PropertiesFileUpdate(serverPropertiesFile.getAbsolutePath());
        Properties props = propsFile.loadExistingProperties();

        // GWT can't handle Properties - convert to HashMap
        HashMap<String, String> map = new HashMap<String, String>(props.size());
        for (Object property : props.keySet()) {
            map.put(property.toString(), props.getProperty(property.toString()));
        }
        return map;
    }

    @Override
    public void saveServerProperties(HashMap<String, String> serverProperties) throws Exception {
        File serverPropertiesFile = getServerPropertiesFile();
        PropertiesFileUpdate propsFile = new PropertiesFileUpdate(serverPropertiesFile.getAbsolutePath());

        // GWT can't handle Properties - convert from HashMap
        Properties props = new Properties();
        for (Map.Entry<String, String> entry : serverProperties.entrySet()) {
            props.setProperty(entry.getKey(), entry.getValue());
        }

        propsFile.update(props);

        return;
    }

    @Override
    public String getAppServerVersion() throws Exception {
        JBossASClient client = new JBossASClient(getClient());
        String version = client.getStringAttribute("release-version", Address.root());
        return version;
    }

    @Override
    public String getOperatingSystem() throws Exception {
        JBossASClient client = new JBossASClient(getClient());
        String[] address = { "core-service", "platform-mbean", "type", "operating-system" };
        String osName = client.getStringAttribute("name", Address.root().add(address));
        return osName;
    }

    private String getAppServerHomeDir() throws Exception {
        JBossASClient client = new JBossASClient(getClient());
        String[] address = { "core-service", "server-environment" };
        String dir = client.getStringAttribute(true, "home-dir", Address.root().add(address));
        return dir;
    }

    private File getServerPropertiesFile() throws Exception {
        File appServerHomeDir = new File(getAppServerHomeDir());
        File serverPropertiesFile = new File(appServerHomeDir, "../bin/rhq-server.properties");
        return serverPropertiesFile;
    }

    private ModelControllerClient getClient() {
        ModelControllerClient client = ManagementService.getClient();
        return client;
    }

    private void createDatasourceSecurityDomain(String username, String password) throws Exception {
        final SecurityDomainJBossASClient client = new SecurityDomainJBossASClient(getClient());
        final String securityDomain = RHQ_SECURITY_DOMAIN;
        if (!client.isSecurityDomain(securityDomain)) {
            client.createNewSecureIdentitySecurityDomainRequest(securityDomain, username, password);
            log("Security domain [" + securityDomain + "] created");
        } else {
            log("Security domain [" + securityDomain + "] already exists, skipping the creation request");
        }
    }
}
