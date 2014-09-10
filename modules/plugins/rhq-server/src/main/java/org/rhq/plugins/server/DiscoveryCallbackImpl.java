/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
package org.rhq.plugins.server;

import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryCallback;
import org.rhq.core.system.ProcessInfo;
import org.rhq.core.util.obfuscation.Obfuscator;

public class DiscoveryCallbackImpl implements ResourceDiscoveryCallback {

    private static final String PLUGIN_CONFIG_HOME_DIR = "homeDir";
    private static final String PLUGIN_CONFIG_PASSWORD = "password";
    private static final String PROP_SERVER_PROP_FILE = "../bin/rhq-server.properties";
    private static final String PROP_SERVER_MGMT_USER_PASSWORD = "rhq.server.management.password";

    private Log log = LogFactory.getLog(DiscoveryCallbackImpl.class);

    @Override
    public DiscoveryCallbackResults discoveredResources(DiscoveredResourceDetails discoveredDetails) throws Exception {

        ProcessInfo processInfo = discoveredDetails.getProcessInfo();
        DiscoveryCallbackResults result = DiscoveryCallbackResults.UNPROCESSED;

        // Do RHQ Server specific work
        if (!isRhqServer(processInfo)) {
            return result;
        }

        result = DiscoveryCallbackResults.PROCESSED;

        // Make the name reflect the fact that this is the RHQ Server resource
        String name = discoveredDetails.getResourceName();
        discoveredDetails.setResourceName(name + " RHQ Server");

        // Update the plugin config and set the proper management user password
        String homeDirStr = discoveredDetails.getPluginConfiguration().getSimpleValue(PLUGIN_CONFIG_HOME_DIR);

        if (null == homeDirStr || homeDirStr.isEmpty()) {
            log.warn("The configuration property [" + PLUGIN_CONFIG_HOME_DIR
                + "] is not set - will not be able to connect to the RHQ Server instance");
            return result;
        }

        File homeDirFile = new File(homeDirStr);
        File serverPropertiesFile = new File(homeDirFile, PROP_SERVER_PROP_FILE);
        if (!serverPropertiesFile.exists()) {
            log.warn("The server properties file [" + serverPropertiesFile.getAbsolutePath()
                + "] does not exist - will not be able to connect to the RHQ Server instance");
            return result;
        }

        try {
            Properties props = new Properties();
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(serverPropertiesFile);
                props.load(fis);
            } finally {
                if (null != fis) {
                    fis.close();
                }
            }

            String encodedPassword = props.getProperty(PROP_SERVER_MGMT_USER_PASSWORD);

            if (null == encodedPassword || encodedPassword.isEmpty()) {
                log.warn("The server property [" + PROP_SERVER_MGMT_USER_PASSWORD
                    + "] is not set - will not be able to connect to the RHQ Server instance");
                return result;
            }

            String password = Obfuscator.decode(encodedPassword);
            Configuration pluginConfig = discoveredDetails.getPluginConfiguration();
            pluginConfig.put(new PropertySimple(PLUGIN_CONFIG_PASSWORD, password));
            discoveredDetails.setPluginConfiguration(pluginConfig);

            pluginConfig.setSimpleValue("supportsPatching", "false");
        } catch (Throwable t) {
            log.warn(
                "Problem setting RHQ Server management password - will not be able to connect to the RHQ Server instance",
                t);
        }

        return result;
    }

    public static boolean isRhqServer(ProcessInfo process) {

        if (process == null) {
            return false;
        }

        // LINUX
        if (File.separatorChar == '/') {
            String prop = process.getEnvironmentVariable("JAVA_OPTS");
            return (null != prop && prop.contains("-Dapp.name=rhq-server"));
        }

        // Windows
        ProcessInfo parentProcess = process.getParentProcess();
        if (null != parentProcess) {
            String commandLine = Arrays.toString(parentProcess.getCommandLine());
            if (null != commandLine && commandLine.contains("rhq-server-wrapper.conf")) {
                return true;
            }
        }

        return false;
    }

}
